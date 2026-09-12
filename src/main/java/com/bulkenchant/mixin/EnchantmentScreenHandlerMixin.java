package com.bulkenchant.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.bulkenchant.api.EnchantAllHandler;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Adds a "bulk enchant" action to the enchanting table: applies the enchantment
 * package chosen by the player (one of the 3 vanilla options) to every
 * enchantable item in their main inventory, at once, scaling the lapis/XP cost
 * with the number of items enchanted.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin implements EnchantAllHandler {

	@Shadow
	@Final
	private Inventory inventory;

	@Shadow
	@Final
	private ScreenHandlerContext context;

	@Shadow
	@Final
	private Property seed;

	@Shadow
	@Final
	public int[] enchantmentPower;

	@Shadow
	private List<EnchantmentLevelEntry> generateEnchantments(DynamicRegistryManager registryManager, ItemStack stack, int slot, int level) {
		throw new AssertionError("mixin shadow");
	}

	@Shadow
	public abstract void onContentChanged(Inventory inventory);

	@Override
	@Unique
	public void bulkenchant$enchantAll(ServerPlayerEntity player, int buttonId) {
		if (buttonId < 0 || buttonId >= this.enchantmentPower.length || this.enchantmentPower[buttonId] <= 0) {
			return;
		}

		ItemStack tableItem = this.inventory.getStack(0);
		boolean tableItemEligible = !tableItem.isEmpty() && tableItem.isEnchantable();

		PlayerInventory playerInventory = player.getInventory();
		List<Integer> mainIndices = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			ItemStack stack = playerInventory.getStack(i);
			if (!stack.isEmpty() && stack.isEnchantable()) {
				mainIndices.add(i);
			}
		}

		int count = (tableItemEligible ? 1 : 0) + mainIndices.size();
		if (count == 0) {
			player.sendMessage(Text.translatable("bulkenchant.message.no_items"), true);
			return;
		}

		int costPerItem = buttonId + 1;
		ItemStack lapisStack = this.inventory.getStack(1);

		if (!player.isInCreativeMode()) {
			if (lapisStack.isEmpty() || lapisStack.getCount() < costPerItem * count) {
				player.sendMessage(Text.translatable("bulkenchant.message.not_enough_lapis"), true);
				return;
			}

			if (player.experienceLevel < this.enchantmentPower[buttonId] || player.experienceLevel < costPerItem * count) {
				player.sendMessage(Text.translatable("bulkenchant.message.not_enough_levels"), true);
				return;
			}
		}

		this.context.run((world, pos) -> {
			int enchantedCount = 0;

			if (tableItemEligible) {
				ItemStack result = this.bulkenchant$enchantSingle(world, tableItem, buttonId, player, costPerItem);
				if (result != null) {
					this.inventory.setStack(0, result);
					enchantedCount++;
				}
			}

			for (int index : mainIndices) {
				ItemStack stack = playerInventory.getStack(index);
				ItemStack result = this.bulkenchant$enchantSingle(world, stack, buttonId, player, costPerItem);
				if (result != null) {
					playerInventory.setStack(index, result);
					enchantedCount++;
				}
			}

			if (enchantedCount > 0) {
				lapisStack.decrementUnlessCreative(costPerItem * enchantedCount, player);
				if (lapisStack.isEmpty()) {
					this.inventory.setStack(1, ItemStack.EMPTY);
				}

				player.incrementStat(Stats.ENCHANT_ITEM);
				this.inventory.markDirty();
				this.seed.set(player.getEnchantingTableSeed());
				this.onContentChanged(this.inventory);
				world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
			}
		});
	}

	/**
	 * Generates and applies enchantments to a single stack, mirroring vanilla's
	 * {@code onButtonClick} logic. Returns the resulting stack (which may be a
	 * different instance, e.g. book -> enchanted book), or {@code null} if the
	 * roll produced no enchantment for this particular item.
	 */
	@Unique
	private ItemStack bulkenchant$enchantSingle(World world, ItemStack stack, int buttonId, ServerPlayerEntity player, int cost) {
		List<EnchantmentLevelEntry> list = this.generateEnchantments(world.getRegistryManager(), stack, buttonId, this.enchantmentPower[buttonId]);
		if (list.isEmpty()) {
			return null;
		}

		ItemStack result = stack;
		if (result.isOf(Items.BOOK)) {
			result = result.withItem(Items.ENCHANTED_BOOK);
		}

		for (EnchantmentLevelEntry entry : list) {
			result.addEnchantment(entry.enchantment(), entry.level());
		}

		player.applyEnchantmentCosts(result, cost);
		Criteria.ENCHANTED_ITEM.trigger(player, result, cost);
		return result;
	}
}
