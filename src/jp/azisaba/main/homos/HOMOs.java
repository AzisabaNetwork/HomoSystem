package jp.azisaba.main.homos;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import jp.azisaba.main.homos.commands.HomoCommand;
import jp.azisaba.main.homos.database.PlayerTicketManager;
import jp.azisaba.main.homos.listeners.JoinListener;
import jp.azisaba.main.homos.listeners.MedianUpdateListener;
import jp.azisaba.main.homos.listeners.TestListener;
import jp.azisaba.main.homos.median.MedianManager;
import jp.azisaba.main.homos.median.MedianUpdateTask;
import net.md_5.bungee.api.ChatColor;

public class HOMOs extends JavaPlugin {

	public static HOMOsConfig config;

	private static MedianManager medianManager;

	private BukkitTask task;

	@Override
	public void onEnable() {

		HOMOs.config = new HOMOsConfig(this);
		HOMOs.config.loadConfig();

		PlayerTicketManager.init(this);
		medianManager = new MedianManager(this);

		Bukkit.getPluginManager().registerEvents(new TestListener(this), this);
		Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
		Bukkit.getPluginManager().registerEvents(new MedianUpdateListener(this), this);

		Bukkit.getPluginCommand("homo").setExecutor(new HomoCommand());
		Bukkit.getPluginCommand("homo")
				.setPermissionMessage(ChatColor.GREEN + "おっと！ " + ChatColor.RED + "あなたには権限がありません！");

		if (config.updateMedian) {
			this.task = new MedianUpdateTask().runTaskTimerAsynchronously(this, 20,
					(long) (20 * config.updateMedianSeconds));
		}

		medianManager.lock(getConfig().getBoolean("SystemData.LockMedian", false));
		medianManager.updateMedian(getConfig().getInt("SystemData.Median", -1), false);

		Bukkit.getLogger().info(getName() + " enabled.");
	}

	public static MedianManager getMedianManager() {
		return medianManager;
	}

	@Override
	public void onDisable() {

		reloadConfig();

		getConfig().set("SystemData.LockMedian", medianManager.isLocked());
		getConfig().set("SystemData.Median", medianManager.getCurrentMedian());
		saveConfig();

		Bukkit.getLogger().info(getName() + " disabled.");
	}

	public void reloadPlugin() {

		reloadConfig();

		getConfig().set("SystemData.LockMedian", medianManager.isLocked());
		saveConfig();

		HOMOs.config = new HOMOsConfig(this);
		HOMOs.config.loadConfig();

		PlayerTicketManager.closeAll();
		PlayerTicketManager.init(this);

		if (this.task != null) {
			this.task.cancel();
			this.task = null;
		}

		if (config.updateMedian) {
			this.task = new MedianUpdateTask().runTaskTimerAsynchronously(this, 0, 60 * 20);
		}

		medianManager.lock(getConfig().getBoolean("SystemData.LockMedian", false));
		medianManager.updateMedian(getConfig().getInt("SystemData.Median", -1), false);
	}
}
