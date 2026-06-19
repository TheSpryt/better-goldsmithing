package com.bettergoldsmithing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterGoldsmithingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterGoldsmithingPlugin.class);
		RuneLite.main(args);
	}
}
