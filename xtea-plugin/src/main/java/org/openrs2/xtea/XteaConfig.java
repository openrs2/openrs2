package org.openrs2.xtea;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("openrs2xtea")
public interface XteaConfig extends Config {
	@ConfigItem(
		keyName = "endpoint",
		name = "API Endpoint",
		description = "The URL the XTEA keys are submitted to"
	)
	default String endpoint() {
		return "https://archive.openrs2.org/keys";
	}
}
