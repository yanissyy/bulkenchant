package com.bulkenchant.client;

import java.util.ArrayDeque;
import java.util.Deque;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Purely client-side "macro": automates the exact same slot/button clicks a
 * player would do by hand, one item per client tick, using only vanilla
 * network actions. Works on any server (vanilla or modded) since the server
 * never needs to know about this mod - it just looks like fast manual play.
 *
 * Slot layout assumed (standard vanilla screens, player inventory untouched):
 * - Enchanting table: slot 0 = item, slot 1 = lapis, slots 2-28 = the 27 main
 *   inventory slots, slots 29-37 = hotbar (0-8).
 * - Grindstone: slot 0/1 = inputs, slot 2 = result, slots 3-29 = the 27 main
 *   inventory slots, slots 30-38 = hotbar.
 */
@Environment(EnvType.CLIENT)
public final class BulkEnchantMacro {
	private static final int ENCHANT_ITEM_SLOT = 0;
	private static final int ENCHANT_LAPIS_SLOT = 1;
	private static final int ENCHANT_ITEM_SLOTS_START = 2;
	private static final int ENCHANT_ITEM_SLOTS_END = 28;
	// Hotbar indices 2-8 (screen slots 29 + 2 .. 29 + 8), as requested: hotbar
	// slots 0-1 are left alone (e.g. for a currently held tool).
	private static final int ENCHANT_LAPIS_SOURCE_START = 31;
	private static final int ENCHANT_LAPIS_SOURCE_END = 37;

	private static final int GRIND_INPUT_SLOT = 0;
	private static final int GRIND_OUTPUT_SLOT = 2;
	private static final int GRIND_ITEM_SLOTS_START = 3;
	private static final int GRIND_ITEM_SLOTS_END = 29;

	private static final Deque<Runnable> QUEUE = new ArrayDeque<>();
	private static boolean registered = false;

	private BulkEnchantMacro() {
	}

	public static void init() {
		if (registered) {
			return;
		}
		registered = true;
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Runnable step = QUEUE.poll();
			if (step != null) {
				step.run();
			}
		});
	}

	public static void startEnchantAll(int syncId, int buttonId) {
		QUEUE.clear();
		for (int slot = ENCHANT_ITEM_SLOTS_START; slot <= ENCHANT_ITEM_SLOTS_END; slot++) {
			int itemSlot = slot;
			QUEUE.add(() -> enchantOne(syncId, buttonId, itemSlot));
		}
	}

	public static void startDisenchantAll(int syncId) {
		QUEUE.clear();
		for (int slot = GRIND_ITEM_SLOTS_START; slot <= GRIND_ITEM_SLOTS_END; slot++) {
			int itemSlot = slot;
			QUEUE.add(() -> disenchantOne(syncId, itemSlot));
		}
	}

	private static boolean screenStillValid(Class<? extends Screen> expectedScreenType, int syncId) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || !expectedScreenType.isInstance(client.currentScreen)) {
			return false;
		}
		return player.currentScreenHandler.syncId == syncId;
	}

	private static void enchantOne(int syncId, int buttonId, int itemSlot) {
		if (!screenStillValid(EnchantmentScreen.class, syncId)) {
			QUEUE.clear();
			return;
		}

		ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
		ItemStack stack = handler.getSlot(itemSlot).getStack();
		if (stack.isEmpty()) {
			return;
		}

		ensureLapis(syncId, buttonId + 1, ENCHANT_LAPIS_SLOT, ENCHANT_LAPIS_SOURCE_START, ENCHANT_LAPIS_SOURCE_END);

		click(syncId, itemSlot, SlotActionType.PICKUP);
		click(syncId, ENCHANT_ITEM_SLOT, SlotActionType.PICKUP);
		if (!handler.getCursorStack().isEmpty()) {
			// leftover from a stack bigger than 1 (the enchanting slot only holds 1 item)
			click(syncId, itemSlot, SlotActionType.PICKUP);
		}

		MinecraftClient.getInstance().interactionManager.clickButton(syncId, buttonId);

		click(syncId, ENCHANT_ITEM_SLOT, SlotActionType.PICKUP);
		click(syncId, itemSlot, SlotActionType.PICKUP);
	}

	private static void disenchantOne(int syncId, int itemSlot) {
		if (!screenStillValid(GrindstoneScreen.class, syncId)) {
			QUEUE.clear();
			return;
		}

		ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
		ItemStack stack = handler.getSlot(itemSlot).getStack();
		if (stack.isEmpty()) {
			return;
		}

		click(syncId, itemSlot, SlotActionType.PICKUP);
		click(syncId, GRIND_INPUT_SLOT, SlotActionType.PICKUP);
		if (!handler.getCursorStack().isEmpty()) {
			click(syncId, itemSlot, SlotActionType.PICKUP);
		}

		click(syncId, GRIND_OUTPUT_SLOT, SlotActionType.PICKUP);
		click(syncId, itemSlot, SlotActionType.PICKUP);
	}

	private static void ensureLapis(int syncId, int needed, int lapisSlot, int sourceStart, int sourceEnd) {
		ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
		if (handler.getSlot(lapisSlot).getStack().getCount() >= needed) {
			return;
		}

		for (int source = sourceStart; source <= sourceEnd; source++) {
			if (handler.getSlot(lapisSlot).getStack().getCount() >= needed) {
				return;
			}

			ItemStack sourceStack = handler.getSlot(source).getStack();
			if (sourceStack.isEmpty()) {
				continue;
			}

			click(syncId, source, SlotActionType.PICKUP);
			click(syncId, lapisSlot, SlotActionType.PICKUP);
			if (!handler.getCursorStack().isEmpty()) {
				click(syncId, source, SlotActionType.PICKUP);
			}
		}
	}

	private static void click(int syncId, int slot, SlotActionType type) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.interactionManager.clickSlot(syncId, slot, 0, type, client.player);
	}
}
