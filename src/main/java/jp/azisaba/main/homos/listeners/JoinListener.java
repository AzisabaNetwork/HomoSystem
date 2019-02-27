package jp.azisaba.main.homos.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.database.PlayerDataManager;

public class JoinListener implements Listener {

	private Homos plugin;

	public JoinListener(Homos plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {

		if (Homos.config.readOnly) {
			return;
		}

		Player p = e.getPlayer();
		boolean success = PlayerDataManager.updatePlayerData(p.getUniqueId(), p.getName(), System.currentTimeMillis());

		if (!success) {
			plugin.getLogger().warning("プレイヤーデータの更新に失敗しました");
		} else {
			plugin.getLogger().info("updated.");
		}
	}
}
