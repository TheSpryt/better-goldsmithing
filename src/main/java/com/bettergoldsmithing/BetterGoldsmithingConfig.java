package com.bettergoldsmithing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(BetterGoldsmithingConfig.GROUP)
public interface BetterGoldsmithingConfig extends Config
{
	String GROUP = "better-goldsmithing";

	@ConfigItem(
		keyName = "blockDepositWithoutGauntlets",
		name = "Block deposit without gauntlets",
		description = "Prevent putting gold ore on the Blast Furnace conveyor unless Gauntlets of goldsmithing are equipped.",
		position = 1
	)
	default boolean blockDepositWithoutGauntlets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lockGloves",
		name = "Lock gloves until XP lands",
		description = "After depositing gold ore with gauntlets on, block equipping/removing gloves until the Smithing XP for that deposit is credited.",
		position = 2
	)
	default boolean lockGloves()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideEmptyDispenser",
		name = "Hide empty dispenser option",
		description = "Remove the bar dispenser's left-click option while it's empty (shows \"Check\" instead of \"Take\"), so you can't waste a click collecting nothing.",
		position = 3
	)
	default boolean hideEmptyDispenser()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showChatMessages",
		name = "Show chat warnings",
		description = "Print a game message when a deposit or glove swap is blocked.",
		position = 4
	)
	default boolean showChatMessages()
	{
		return true;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "maxLockTicks",
		name = "Safety auto-unlock (ticks)",
		description = "Automatically release the glove lock after this many game ticks if no Smithing XP arrives, so you can never get stuck. 0 disables the safety net (lock only releases on XP).",
		position = 5
	)
	default int maxLockTicks()
	{
		return 50;
	}
}
