package jp.azisaba.main.homos;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import jp.azisaba.main.homos.commands.HomoCommand;
import jp.azisaba.main.homos.database.SQLManager;
import jp.azisaba.main.homos.listeners.JoinListener;
import jp.azisaba.main.homos.listeners.MedianUpdateListener;
import jp.azisaba.main.homos.median.MedianManager;
import jp.azisaba.main.homos.median.MedianUpdateTask;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class Homos extends JavaPlugin {

	public static HOMOsConfig config;
	private static MedianManager medianManager;
	private BukkitTask task;
	private static Economy econ;

	@Override
	public void onEnable() {

		Homos.config = new HOMOsConfig(this);
		Homos.config.loadConfig();

		if (config.hasEconomy) {
			if (!setupEconomy()) {
				getLogger().warning("経済の取得に失敗しました。");
			} else {
				getLogger().info("経済と連携しました。");
			}
		}

		SQLManager.init(this);
		medianManager = new MedianManager(this);

		Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
		Bukkit.getPluginManager().registerEvents(new MedianUpdateListener(this), this);

		Bukkit.getPluginCommand("homo").setExecutor(new HomoCommand());
		Bukkit.getPluginCommand("homo")
				.setPermissionMessage(ChatColor.GREEN + "おっと！ " + ChatColor.RED + "あなたには権限がありません！");

		if (config.hasEconomy) {
			this.task = new MedianUpdateTask().runTaskTimerAsynchronously(this, 20,
					(long) (20 * config.updateMedianSeconds));
		}

		Bukkit.getLogger().info(getName() + " enabled.");

		List<String> columns = SQLManager.getColumnsFromMoneyData();

		if (!columns.contains(config.serverName) && !config.serverName.equalsIgnoreCase("unknown")) {
			SQLManager.addMoneyDataColmun(config.serverName);
		}
	}

	public static MedianManager getMedianManager() {
		return medianManager;
	}

	@Override
	public void onDisable() {
		Bukkit.getLogger().info(getName() + " disabled.");
	}

	public void reloadPlugin() {

		reloadConfig();

		getConfig().set("SystemData.LockMedian", medianManager.isLocked());
		saveConfig();

		Homos.config = new HOMOsConfig(this);
		Homos.config.loadConfig();

		SQLManager.closeAll();
		SQLManager.init(this);

		if (this.task != null) {
			this.task.cancel();
			this.task = null;
		}

		if (config.hasEconomy) {
			this.task = new MedianUpdateTask().runTaskTimerAsynchronously(this, 0, 60 * 20);
		}
	}

	public static Economy getEconomy() {
		return econ;
	}

	private static boolean setupEconomy() {
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
}
