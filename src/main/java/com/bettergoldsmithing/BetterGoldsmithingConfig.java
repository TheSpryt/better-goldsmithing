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
		name = "Lock gloves until smelted",
		description = "After depositing gold ore with gauntlets on, block equipping/removing gloves until the whole batch has finished smelting (all of its bonus XP credited).",
		position = 2
	)
	default boolean lockGloves()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLockIcon",
		name = "Show lock icon on gloves",
		description = "While the glove slot is locked, draw a small 🚫 mark on any gloves in your inventory so the lock is obvious at a glance.",
		position = 3
	)
	default boolean showLockIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideEmptyDispenser",
		name = "Hide empty dispenser option",
		description = "Remove the bar dispenser's left-click option while it's empty (shows \"Check\" instead of \"Take\"), so you can't waste a click collecting nothing.",
		position = 4
	)
	default boolean hideEmptyDispenser()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showChatMessages",
		name = "Show chat warnings",
		description = "Print a game message when a deposit or glove swap is blocked.",
		position = 5
	)
	default boolean showChatMessages()
	{
		return true;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "maxLockTicks",
		name = "Safety auto-unlock (ticks)",
		description = "Release the glove lock if there is no smelting progress for this many game ticks, so you can never get stuck (the timer resets while bars are still smelting). 0 disables the safety net.",
		position = 6
	)
	default int maxLockTicks()
	{
		return 50;
	}
}
