package jp.azisaba.main.homos.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerTicketManager;

public class TestListener implements Listener {

	@SuppressWarnings("unused")
	private HOMOs plugin;

	public TestListener(HOMOs plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onClock(PlayerInteractEvent e) {
		Player p = e.getPlayer();

		if (e.getAction().toString().startsWith("LEFT_CLICK_")) {

			if (p.isSneaking()) {
				PlayerTicketManager.addTicket(p, 1);
			} else {
				PlayerTicketManager.addTicket(p, (int) (Math.random() * 100));
			}
		} else if (e.getAction().toString().startsWith("RIGHT_CLICK_")) {
			if (p.isSneaking()) {
				PlayerTicketManager.removeTicket(p, 1);
			} else {
				PlayerTicketManager.removeTicket(p, (int) (Math.random() * 100));
			}
		}

		PlayerData data = PlayerTicketManager.getPlayerData(p);

		Bukkit.broadcastMessage("Tickets: " + data.getTickets());
		Bukkit.broadcastMessage("Money: " + data.getMoney());
	}
}
