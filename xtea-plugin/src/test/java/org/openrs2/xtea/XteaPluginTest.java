package org.openrs2.xtea;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class XteaPluginTest {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(XteaPlugin.class);
		RuneLite.main(args);
	}

	private XteaPluginTest() {
		// empty
	}
}
