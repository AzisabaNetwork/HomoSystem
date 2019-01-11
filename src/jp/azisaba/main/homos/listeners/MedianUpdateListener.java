package jp.azisaba.main.homos.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.events.TicketToggleBoostEvent;
import jp.azisaba.main.homos.events.TicketValueUpdateEvent;

public class MedianUpdateListener implements Listener {

	private HOMOs plugin;

	public MedianUpdateListener(HOMOs plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onUpdate(TicketValueUpdateEvent e) {
		plugin.getLogger().info("Ticket value updated. new value is " + e.getValue());
	}

	@EventHandler
	public void onBoostToggle(TicketToggleBoostEvent e) {

		if (e.isBoostModeEnable()) {
			plugin.getLogger().info("Boost mode enabled!!");
		} else {
			plugin.getLogger().info("Boost mode disabled.");
		}
	}
}
