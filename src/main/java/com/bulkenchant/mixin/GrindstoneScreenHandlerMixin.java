package com.bulkenchant.mixin;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.bulkenchant.api.DisenchantAllHandler;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

/**
 * Adds a "bulk disenchant" action to the grindstone: strips the enchantments
 * (except curses, same as vanilla grinding) from every enchanted item in the
 * player's main inventory in one go, granting experience for all of them.
 */
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin implements DisenchantAllHandler {

	@Shadow
	@Final
	Inventory input;

	@Shadow
	@Final
	private ScreenHandlerContext context;

	@Shadow
	private ItemStack grind(ItemStack item) {
		throw new AssertionError("mixin shadow");
	}

	@Override
	@Unique
	public void bulkenchant$disenchantAll(ServerPlayerEntity player) {
		PlayerInventory playerInventory = player.getInventory();
		List<Integer> mainIndices = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			ItemStack stack = playerInventory.getStack(i);
			if (!stack.isEmpty() && EnchantmentHelper.hasEnchantments(stack)) {
				mainIndices.add(i);
			}
		}

		ItemStack slot0 = this.input.getStack(0);
		ItemStack slot1 = this.input.getStack(1);
		boolean slot0Eligible = !slot0.isEmpty() && EnchantmentHelper.hasEnchantments(slot0);
		boolean slot1Eligible = !slot1.isEmpty() && EnchantmentHelper.hasEnchantments(slot1);

		if (mainIndices.isEmpty() && !slot0Eligible && !slot1Eligible) {
			player.sendMessage(Text.translatable("bulkenchant.message.no_items"), true);
			return;
		}

		this.context.run((world, pos) -> {
			int totalXp = 0;

			if (slot0Eligible) {
				totalXp += this.bulkenchant$experienceValue(slot0, world);
				this.input.setStack(0, this.grind(slot0));
			}

			if (slot1Eligible) {
				totalXp += this.bulkenchant$experienceValue(slot1, world);
				this.input.setStack(1, this.grind(slot1));
			}

			for (int index : mainIndices) {
				ItemStack stack = playerInventory.getStack(index);
				totalXp += this.bulkenchant$experienceValue(stack, world);
				playerInventory.setStack(index, this.grind(stack));
			}

			if (totalXp > 0 && world instanceof ServerWorld serverWorld) {
				ExperienceOrbEntity.spawn(serverWorld, Vec3d.ofCenter(pos), totalXp);
			}

			this.input.markDirty();
			world.syncWorldEvent(WorldEvents.GRINDSTONE_USED, pos, 0);
		});
	}

	/**
	 * Same formula as the vanilla grindstone's output slot: sum the min-power of
	 * every non-curse enchantment, then halve and randomize it, exactly like a
	 * single item ground alone (i.e. with no second item combined in).
	 */
	@Unique
	private int bulkenchant$experienceValue(ItemStack stack, World world) {
		int power = 0;
		for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries()) {
			RegistryEntry<Enchantment> enchantment = entry.getKey();
			if (!enchantment.isIn(EnchantmentTags.CURSE)) {
				power += enchantment.value().getMinPower(entry.getIntValue());
			}
		}

		if (power <= 0) {
			return 0;
		}

		int half = (int) Math.ceil(power / 2.0);
		return half + world.random.nextInt(half);
	}
}
