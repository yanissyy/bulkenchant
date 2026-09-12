package com.bulkenchant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkEnchantMod implements ModInitializer {
	public static final String MOD_ID = "bulkenchant";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Bulk Enchant initialise");
		BulkEnchantNetworking.registerServer();
	}
}
