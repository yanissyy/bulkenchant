package com.bulkenchant;

import com.bulkenchant.api.DisenchantAllHandler;
import com.bulkenchant.api.EnchantAllHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class BulkEnchantNetworking {
	private BulkEnchantNetworking() {
	}

	public record EnchantAllPayload(int syncId, int buttonId) implements CustomPayload {
		public static final CustomPayload.Id<EnchantAllPayload> ID =
				new CustomPayload.Id<>(Identifier.of(BulkEnchantMod.MOD_ID, "enchant_all"));
		public static final PacketCodec<PacketByteBuf, EnchantAllPayload> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, EnchantAllPayload::syncId,
				PacketCodecs.VAR_INT, EnchantAllPayload::buttonId,
				EnchantAllPayload::new
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public record DisenchantAllPayload(int syncId) implements CustomPayload {
		public static final CustomPayload.Id<DisenchantAllPayload> ID =
				new CustomPayload.Id<>(Identifier.of(BulkEnchantMod.MOD_ID, "disenchant_all"));
		public static final PacketCodec<PacketByteBuf, DisenchantAllPayload> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, DisenchantAllPayload::syncId,
				DisenchantAllPayload::new
		);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	public static void registerServer() {
		PayloadTypeRegistry.playC2S().register(EnchantAllPayload.ID, EnchantAllPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DisenchantAllPayload.ID, DisenchantAllPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(EnchantAllPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player.currentScreenHandler.syncId == payload.syncId()
					&& player.currentScreenHandler instanceof EnchantmentScreenHandler handler) {
				((EnchantAllHandler) handler).bulkenchant$enchantAll(player, payload.buttonId());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(DisenchantAllPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player.currentScreenHandler.syncId == payload.syncId()
					&& player.currentScreenHandler instanceof GrindstoneScreenHandler handler) {
				((DisenchantAllHandler) handler).bulkenchant$disenchantAll(player);
			}
		});
	}
}
