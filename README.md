# Better Goldsmithing

A [RuneLite](https://runelite.net/) plugin for Old School RuneScape that helps you
train Smithing at the Blast Furnace with the **Gauntlets of goldsmithing**, without
accidentally losing the bonus experience they give on gold bars.

## What it is for

The Gauntlets of goldsmithing grant a large bonus to the Smithing experience you
get from smelting gold ore into gold bars (56.2 xp per bar instead of 22.5). At
the Blast Furnace there are two easy ways to lose that bonus:

1. **Depositing gold ore without the gauntlets on.** If you forget to equip them,
   or swapped to ice gloves to handle hot bars, the whole batch is smelted at the
   base rate with no bonus.
2. **Swapping the gauntlets off too early.** The bonus is only credited as each
   gold bar smelts, which happens over a few seconds after you deposit. The
   gauntlets must be worn at the moment each bar's experience is granted, so
   switching to ice gloves before the batch has fully smelted loses the bonus on
   the bars that were still cooking.

Better Goldsmithing closes both gaps, so you can run the gold method (and still
swap to ice gloves for the bars) without ever silently losing experience.

## Configuration

All options live under the **Better Goldsmithing** section in the RuneLite plugin
settings, and each can be toggled independently.

### Block deposit without gauntlets
*Default: on*

While you are holding gold ore and do **not** have the Gauntlets of goldsmithing
equipped, the conveyor belt's deposit option is removed. A left click on the
conveyor then just walks you to it instead of depositing, so you cannot dump a
batch of gold ore without the bonus. The option returns the instant you equip the
gauntlets. Depositing other ores is unaffected, since this only triggers while
gold ore is in your inventory.

### Lock gloves until smelted
*Default: on*

After you deposit gold ore with the gauntlets on, the glove slot is locked: you
cannot equip or remove any gloves until the **whole batch has finished smelting**,
guaranteeing the gauntlets are worn for every bar's bonus experience. The lock:

- **Arms only when the gold ore actually leaves your inventory** (the deposit
  completes), not when you click. You stay free to adjust gear while walking to
  the conveyor.
- **Holds until the entire batch has smelted**, tracked by the game's own count of
  unsmelted gold ore in the furnace, then releases automatically. After that you
  can freely swap to ice gloves to collect the bars.
- **Covers every kind of glove** (ice gloves, Smiths gloves (i), and any gloves
  added in future), because it checks the equipment slot rather than a fixed list
  of item ids.

### Show lock icon on gloves
*Default: on*

While the glove slot is locked, a small semi-transparent 🚫 mark is drawn on the
corner of any gloves in your inventory, so you can see at a glance that they are
locked. The mark disappears the moment the lock releases.

### Hide empty dispenser option
*Default: on*

The bar dispenser shows **Check** when it is empty and **Take** when bars are
ready to collect. This option removes the **Check** entry, so an empty dispenser
has no left click action and you cannot waste a click on it. As soon as bars are
ready and the option becomes **Take**, it is left untouched and works normally.

### Show chat warnings
*Default: on*

Prints a short message in your chat box when an action is blocked (for example,
trying to equip gloves while the slot is locked). Turn this off for silent
operation.

### Safety auto-unlock (ticks)
*Default: 50*

A safety net for the glove lock. If there is no smelting progress for this many
game ticks (for example, you walk away from the furnace), the glove slot unlocks
automatically so you can never get stuck. The timer resets while bars are still
smelting, so it never fires mid-batch. One game tick is 0.6 seconds, so the
default of 50 is roughly 30 seconds of inactivity. Set it to **0** to disable the
safety net.

## How to use

1. Enable the plugin and equip your Gauntlets of goldsmithing.
2. Smith gold at the Blast Furnace as normal.
3. Deposit gold ore with the gauntlets on. Your glove slot locks and the gloves in
   your inventory show a 🚫 mark.
4. Once the whole batch has finished smelting, the lock releases and you can swap
   to ice gloves to collect the bars if you wish.

## Notes

- The plugin only ever blocks or redirects **your own clicks**. It never performs
  actions or moves your character on its own.
- A blocked conveyor click falls through to an ordinary walk, so interacting with
  the belt always feels responsive.

## Building

This is a standard RuneLite external plugin built with Gradle (Java 11):

```
./gradlew run
```
