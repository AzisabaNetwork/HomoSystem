package jp.azisaba.main.homos.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerTicketManager;

public class JoinListener implements Listener {

	private HOMOs plugin;

	public JoinListener(HOMOs plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {

		if (!HOMOs.config.recordJoin) {
			return;
		}

		Player p = e.getPlayer();

		PlayerData data = PlayerTicketManager.getPlayerData(p);
		data.setLastJoin(System.currentTimeMillis());
		boolean success = PlayerTicketManager.updatePlayerData(data);

		if (!success) {
			plugin.getLogger().warning("プレイヤーデータの更新に失敗しました");
		}
	}
}
