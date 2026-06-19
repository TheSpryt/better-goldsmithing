package com.bettergoldsmithing;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Arrays;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Better Goldsmithing",
	description = "Stops you depositing gold ore without goldsmith gauntlets, and locks your glove slot until the Smithing XP lands so you never lose the bonus.",
	tags = {"blast", "furnace", "goldsmith", "gauntlets", "smithing", "gold"}
)
public class BetterGoldsmithingPlugin extends Plugin
{
	private static final int GLOVE_SLOT = EquipmentInventorySlot.GLOVES.getSlotIdx();

	// Menu option text for putting on / taking off worn equipment.
	private static final Set<String> EQUIP_OPTIONS = Set.of("wear", "wield", "equip");
	private static final Set<String> REMOVE_OPTIONS = Set.of("remove", "take-off");

	// Object actions that can carry the conveyor "Put-ore" interaction.
	private static final Set<MenuAction> OBJECT_ACTIONS = Set.of(
		MenuAction.GAME_OBJECT_FIRST_OPTION,
		MenuAction.GAME_OBJECT_SECOND_OPTION,
		MenuAction.GAME_OBJECT_THIRD_OPTION,
		MenuAction.GAME_OBJECT_FOURTH_OPTION,
		MenuAction.GAME_OBJECT_FIFTH_OPTION
	);

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private BetterGoldsmithingConfig config;

	// True while we are protecting the glove slot, i.e. between depositing gold
	// ore with gauntlets on and the resulting Smithing XP being credited.
	private boolean glovesLocked;
	private int smithingXpAtDeposit;
	private int lockTicksRemaining;

	// Set when a deposit is clicked with the gauntlets on; the lock only arms
	// once the gold ore has actually left the inventory (the deposit completes).
	private boolean pendingDeposit;

	@Override
	protected void startUp()
	{
		clearLock();
	}

	@Override
	protected void shutDown()
	{
		clearLock();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// Order matters: a deposit click should arm the lock, not be blocked by it.
		handleConveyorDeposit(event);

		if (!event.isConsumed())
		{
			handleGloveLock(event);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final String option = event.getOption();

		// An empty bar dispenser offers "Check"; once bars are ready it becomes
		// "Take". Drop the "Check" entry so the empty dispenser isn't clickable
		// and you can't waste a tick collecting nothing.
		if (config.hideEmptyDispenser()
			&& "Check".equalsIgnoreCase(option)
			&& "Bar dispenser".equalsIgnoreCase(Text.removeTags(event.getTarget())))
		{
			removeLastMenuEntry();
			return;
		}

		// Remove the conveyor's deposit ("Put-ore") option while gold ore is in
		// the inventory without the gauntlets on. The conveyor only has the one
		// object interaction, so we match on the action type rather than the
		// option text. Left-click then falls through to "Walk here", so a click
		// just walks you to the belt instead of depositing the gold and losing
		// the bonus XP.
		if (config.blockDepositWithoutGauntlets()
			&& event.getIdentifier() == ObjectID.BLAST_FURNACE_CONVEYER_BELT_CLICKABLE
			&& OBJECT_ACTIONS.contains(event.getMenuEntry().getType())
			&& inventoryHasGoldOre()
			&& !goldsmithGauntletsEquipped())
		{
			removeLastMenuEntry();
		}
	}

	// onMenuEntryAdded fires right after the entry is appended, so the entry we
	// want to drop is the last element of the current menu.
	private void removeLastMenuEntry()
	{
		final MenuEntry[] entries = client.getMenuEntries();
		if (entries.length > 0)
		{
			client.setMenuEntries(Arrays.copyOf(entries, entries.length - 1));
		}
	}

	private void handleConveyorDeposit(MenuOptionClicked event)
	{
		if (!OBJECT_ACTIONS.contains(event.getMenuAction()))
		{
			return;
		}

		if (event.getId() != ObjectID.BLAST_FURNACE_CONVEYER_BELT_CLICKABLE)
		{
			return;
		}

		// The conveyor only deposits ore; "Put-ore" empties all ores from the
		// inventory. We only care when that batch includes gold ore.
		if (!inventoryHasGoldOre())
		{
			return;
		}

		final boolean gauntlets = goldsmithGauntletsEquipped();

		if (!gauntlets)
		{
			// The "Put-ore" option is normally stripped in onMenuEntryAdded, so
			// this is just a backstop in case a deposit click slips through.
			if (config.blockDepositWithoutGauntlets())
			{
				event.consume();
			}
			return;
		}

		// Gauntlets are on. Don't lock yet — we may still be walking to the belt.
		// Just flag the intent; the lock arms in onItemContainerChanged once the
		// gold ore actually leaves the inventory.
		if (config.lockGloves())
		{
			pendingDeposit = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!pendingDeposit || event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		// The conveyor empties every ore at once, so the deposit is done the
		// moment the gold ore is gone from the inventory — now it's safe to lock.
		if (!containerHasGoldOre(event.getItemContainer()))
		{
			pendingDeposit = false;
			armLock();
		}
	}

	// While locked, block equipping OR removing anything that occupies the glove
	// slot. This is deliberately not a hardcoded ID list: it covers ice gloves,
	// Smiths gloves (i), and any future gloves Jagex add, since the slot is read
	// from the client's own item data (see isGloveSlotItem).
	private void handleGloveLock(MenuOptionClicked event)
	{
		if (!glovesLocked)
		{
			return;
		}

		final String option = event.getMenuOption() == null
			? ""
			: event.getMenuOption().toLowerCase();

		final boolean equipping = EQUIP_OPTIONS.contains(option);
		final boolean removing = REMOVE_OPTIONS.contains(option);
		if (!equipping && !removing)
		{
			return;
		}

		final int itemId = event.getItemId();
		if (itemId <= 0 || !isGloveSlotItem(itemId))
		{
			return;
		}

		event.consume();
		warn("Gloves are locked until the Smithing XP for your deposited gold lands.");
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!glovesLocked || event.getSkill() != Skill.SMITHING)
		{
			return;
		}

		// The bonus XP for the deposited gold has been credited with the
		// gauntlets still on — the swap window is now safe.
		if (event.getXp() > smithingXpAtDeposit)
		{
			releaseLock("smithing XP landed");
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!glovesLocked || config.maxLockTicks() <= 0)
		{
			return;
		}

		if (--lockTicksRemaining <= 0)
		{
			releaseLock("safety timeout reached without an XP drop");
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN
			|| state == GameState.HOPPING
			|| state == GameState.CONNECTION_LOST)
		{
			clearLock();
		}
	}

	private void armLock()
	{
		glovesLocked = true;
		smithingXpAtDeposit = client.getSkillExperience(Skill.SMITHING);
		lockTicksRemaining = config.maxLockTicks();
	}

	private void releaseLock(String reason)
	{
		clearLock();
		// Diagnostic only — releasing the lock is expected and must not spam chat.
		log.debug("Glove lock released: {}", reason);
	}

	private void clearLock()
	{
		glovesLocked = false;
		smithingXpAtDeposit = 0;
		lockTicksRemaining = 0;
		pendingDeposit = false;
	}

	private boolean inventoryHasGoldOre()
	{
		return containerHasGoldOre(client.getItemContainer(InventoryID.INV));
	}

	private boolean containerHasGoldOre(ItemContainer container)
	{
		if (container == null)
		{
			return false;
		}

		for (final Item item : container.getItems())
		{
			if (item.getId() == ItemID.GOLD_ORE)
			{
				return true;
			}
		}
		return false;
	}

	private boolean goldsmithGauntletsEquipped()
	{
		final ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment == null)
		{
			return false;
		}

		final Item gloves = equipment.getItem(GLOVE_SLOT);
		return gloves != null && gloves.getId() == ItemID.GAUNTLETS_OF_GOLDSMITHING;
	}

	// Returns true for any item worn in the glove slot, determined from the
	// client's item database rather than a fixed list — so new gloves are
	// covered automatically.
	private boolean isGloveSlotItem(int itemId)
	{
		final ItemStats stats = itemManager.getItemStats(itemId);
		return stats != null
			&& stats.getEquipment() != null
			&& stats.getEquipment().getSlot() == GLOVE_SLOT;
	}

	private void warn(String message)
	{
		if (!config.showChatMessages())
		{
			return;
		}

		client.addChatMessage(
			ChatMessageType.GAMEMESSAGE,
			"",
			ColorUtil.wrapWithColorTag("[Better Goldsmithing] " + message, Color.RED),
			null);
	}

	@Provides
	BetterGoldsmithingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterGoldsmithingConfig.class);
	}
}
