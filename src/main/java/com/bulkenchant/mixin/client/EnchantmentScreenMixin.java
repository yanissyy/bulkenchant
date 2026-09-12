package com.bulkenchant.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bulkenchant.client.BulkEnchantMacro;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;

/**
 * Shift-clicking one of the 3 enchantment options in the enchanting table
 * triggers a "bulk enchant" instead of the normal single-item enchant: the
 * chosen enchantment package is applied to every eligible item in the
 * player's inventory at once.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends HandledScreen<EnchantmentScreenHandler> {

	private EnchantmentScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void bulkenchant$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (!Screen.hasShiftDown()) {
			return;
		}

		int i = (this.width - this.backgroundWidth) / 2;
		int j = (this.height - this.backgroundHeight) / 2;

		for (int k = 0; k < 3; k++) {
			double d = mouseX - (i + 60);
			double e = mouseY - (j + 14 + 19 * k);
			if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
				if (this.handler.enchantmentPower[k] > 0) {
					BulkEnchantMacro.startEnchantAll(this.handler.syncId, k);
				}
				cir.setReturnValue(true);
				return;
			}
		}
	}
}
