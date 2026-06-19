package com.bettergoldsmithing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Draws a small, semi-transparent "no entry" mark on every glove in the
 * inventory while the glove slot is locked, so the lock is obvious at a glance.
 */
class GloveLockOverlay extends WidgetItemOverlay
{
	private final BetterGoldsmithingPlugin plugin;
	private final BetterGoldsmithingConfig config;

	@Inject
	GloveLockOverlay(BetterGoldsmithingPlugin plugin, BetterGoldsmithingConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.showLockIcon() || !plugin.isGloveLockActive() || !plugin.isGloveSlotItem(itemId))
		{
			return;
		}

		final Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null)
		{
			return;
		}

		final int d = 13;
		final int x = bounds.x + bounds.width - d - 1;
		final int y = bounds.y + 1;
		final double inset = d * 0.15;

		final Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
			g.setStroke(new BasicStroke(2f));
			g.setColor(Color.RED);
			g.drawOval(x, y, d, d);
			g.drawLine(
				(int) Math.round(x + inset), (int) Math.round(y + inset),
				(int) Math.round(x + d - inset), (int) Math.round(y + d - inset));
		}
		finally
		{
			g.dispose();
		}
	}
}
