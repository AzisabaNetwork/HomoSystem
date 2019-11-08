package jp.azisaba.main.homos.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import jp.azisaba.main.homos.HomoSystem;
import jp.azisaba.main.homos.database.PlayerDataManager;

public class JoinListener implements Listener {

    private final HomoSystem plugin;

    public JoinListener(HomoSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

        if ( HomoSystem.getPluginConfig().useTicketOnly ) {
            return;
        }

        Player p = e.getPlayer();

        new Thread() {
            public void run() {
                long start = System.currentTimeMillis();

                boolean success = PlayerDataManager.updatePlayerData(p.getUniqueId(), p.getName(),
                        System.currentTimeMillis());
                if ( !success ) {
                    plugin.getLogger().warning("プレイヤーデータの更新に失敗しました");
                } else {
                    long end = System.currentTimeMillis();
                    plugin.getLogger().info(p.getName() + "のプレイヤーデータを更新しました。 (" + (end - start) + "ms)");
                }
            }
        }.start();
    }
}
