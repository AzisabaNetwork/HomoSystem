package jp.azisaba.main.homos;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

import jp.azisaba.main.homos.commands.HomoCommand;
import jp.azisaba.main.homos.database.SQLManager;
import jp.azisaba.main.homos.listeners.JoinListener;
import jp.azisaba.main.homos.listeners.TicketValueUpdateListener;
import jp.azisaba.main.homos.ticketvalue.TicketValueManager;
import jp.azisaba.main.homos.ticketvalue.TicketValueUpdateTask;

public class HomoSystem extends JavaPlugin {

    private static HomoSystemConfig config;
    private static TicketValueManager ticketValueManager;
    private static Economy econ = null;
    private static BukkitTask task;
    private static HomoSystem instance;

    @Override
    public void onEnable() {

        HomoSystem.config = new HomoSystemConfig(this);
        HomoSystem.config.loadConfig();

        if ( config.serverName.equalsIgnoreCase("Unknown") && !config.useTicketOnly ) {
            getLogger().warning("サーバー名は 'Unknown' 以外である必要があります。Pluginは無効化されます...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if ( config.hasEconomy ) {
            if ( !setupEconomy() ) {
                getLogger().warning("経済の取得に失敗しました。");
            } else {
                getLogger().info("経済と連携しました。");
            }
        }

        SQLManager.init(this);
        ticketValueManager = new TicketValueManager(this);

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TicketValueUpdateListener(this), this);

        Bukkit.getPluginCommand("homo").setExecutor(new HomoCommand());
        Bukkit.getPluginCommand("homo")
                .setPermissionMessage(ChatColor.GREEN + "おっと！ " + ChatColor.RED + "あなたには権限がありません！");

        if ( config.hasEconomy && !config.useTicketOnly ) {
            HomoSystem.task = new TicketValueUpdateTask().runTaskTimerAsynchronously(this, 20,
                    (long) (20 * config.updateTicketValueSeconds));
        }

        if ( !config.useTicketOnly ) {
            List<String> columns = SQLManager.getColumnsFromMoneyData();
            if ( !columns.contains(config.serverName) ) {
                SQLManager.addMoneyDataColmun(config.serverName);
            }

            columns = SQLManager.getColumnsFromLastjoin();
            if ( !columns.contains(config.serverName) ) {
                SQLManager.addLastjoinColmun(config.serverName);
            }
        }

        if ( config.useTicketOnly ) {
            getLogger().info("*** HOMOS IS NOW USE TICKET ONLY MODE ***");
        }
        instance = this;

        Bukkit.getLogger().info(getName() + " enabled.");
    }

    public static TicketValueManager getTicketValueManager() {
        return ticketValueManager;
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info(getName() + " disabled.");
    }

    public static void reloadPlugin() {

        instance.reloadConfig();

        HomoSystem.config = new HomoSystemConfig(instance);
        HomoSystem.config.loadConfig();

        SQLManager.closeAll();
        SQLManager.init(instance);

        if ( HomoSystem.task != null ) {
            HomoSystem.task.cancel();
            HomoSystem.task = null;
        }

        if ( config.hasEconomy && !config.useTicketOnly ) {
            HomoSystem.task = new TicketValueUpdateTask().runTaskTimerAsynchronously(instance, 0, 60 * 20);
        }

        if ( config.useTicketOnly ) {
            instance.getLogger().info("*** HOMOS IS NOW USE TICKET ONLY MODE ***");
        }
    }

    public static HomoSystemConfig getPluginConfig() {
        return HomoSystem.config;
    }

    public static Economy getEconomy() {
        return econ;
    }

    private static boolean setupEconomy() {
        if ( Bukkit.getPluginManager().getPlugin("Vault") == null ) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if ( rsp == null ) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
