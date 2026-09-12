package com.bulkenchant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class BulkEnchantClientMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BulkEnchantMacro.init();

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof GrindstoneScreen grindstoneScreen) {
				int i = (screen.width - 176) / 2;
				int j = (screen.height - 166) / 2;
				int syncId = grindstoneScreen.getScreenHandler().syncId;

				Screens.getButtons(screen).add(ButtonWidget.builder(Text.translatable("bulkenchant.button.disenchant_all"), button ->
						BulkEnchantMacro.startDisenchantAll(syncId))
						.dimensions(i + 8, j + 61, 160, 16)
						.tooltip(Tooltip.of(Text.translatable("bulkenchant.button.disenchant_all.tooltip")))
						.build());
			}

			if (screen instanceof EnchantmentScreen) {
				int i = (screen.width - 176) / 2;
				int j = (screen.height - 166) / 2;

				ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) ->
						drawContext.drawCenteredTextWithShadow(
								client.textRenderer,
								Text.translatable("bulkenchant.hint.shift_click").formatted(Formatting.GRAY),
								i + 88, j + 73, 0xFFFFFF));
			}
		});
	}
}
