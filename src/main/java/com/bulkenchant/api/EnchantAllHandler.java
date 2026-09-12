package com.bulkenchant.api;

import net.minecraft.server.network.ServerPlayerEntity;

public interface EnchantAllHandler {
	void bulkenchant$enchantAll(ServerPlayerEntity player, int buttonId);
}
