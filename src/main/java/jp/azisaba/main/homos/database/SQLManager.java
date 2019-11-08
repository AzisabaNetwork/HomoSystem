package jp.azisaba.main.homos.database;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jp.azisaba.main.homos.HomoSystem;

public class SQLManager {

    @SuppressWarnings("unused")
    private static HomoSystem plugin;

    private static SQLHandler sql = null;

    public static void init(HomoSystem plugin) {

        SQLManager.plugin = plugin;

        String ip = HomoSystem.getPluginConfig().sqlIp;
        int port = HomoSystem.getPluginConfig().sqlPort;
        String schema = HomoSystem.getPluginConfig().sqlSchema;
        String user = HomoSystem.getPluginConfig().sqlUser;
        String password = HomoSystem.getPluginConfig().sqlPassword;

        sql = new SQLHandler(plugin, ip, port, schema, user, password);

        plugin.getLogger().info("SQL Setup done.");

        plugin.getLogger().info("Checking tables...");
        createTables();
        plugin.getLogger().info("All Complete.");
    }

    private static boolean createTables() {
        String createTicketData = "CREATE TABLE IF NOT EXISTS `ticketdata` (" +
                "  `uuid` varchar(36) NOT NULL," +
                "  `name` text," +
                "  `tickets` bigint(20) NOT NULL DEFAULT '0'," +
                "  `lastjoin` bigint(20) NOT NULL DEFAULT '0'," +
                "  PRIMARY KEY (`uuid`)," +
                "  UNIQUE KEY `uuid_UNIQUE` (`uuid`)" +
                ") ENGINE=InnoDB;";

        String createTicketValue = "CREATE TABLE IF NOT EXISTS `ticketvalue` (" +
                "  `server` varchar(32) NOT NULL," +
                "  `value` bigint(20) DEFAULT '1000'," +
                "  `locked` tinyint(1) DEFAULT '0'," +
                "  `boost` tinyint(1) DEFAULT '0'," +
                "  PRIMARY KEY (`server`)," +
                "  UNIQUE KEY `server_UNIQUE` (`server`)" +
                ") ENGINE=InnoDB;";

        String createMoneyData = "CREATE TABLE IF NOT EXISTS `moneydata` (" +
                "  `uuid` varchar(36) NOT NULL," +
                "  PRIMARY KEY (`uuid`)" +
                ") ENGINE=InnoDB;";

        String createLastjoinData = "CREATE TABLE IF NOT EXISTS `lastjoin` (" +
                "  `uuid` varchar(36) NOT NULL," +
                "  PRIMARY KEY (`uuid`)" +
                ") ENGINE=InnoDB;";

        boolean success1 = sql.executeCommand(createTicketData);
        boolean success2 = sql.executeCommand(createTicketValue);
        boolean success3 = sql.executeCommand(createMoneyData);
        boolean success4 = sql.executeCommand(createLastjoinData);

        return success1 && success2 && success3 && success4;
    }

    public static boolean addMoneyDataColmun(String name) {

        if ( HomoSystem.getPluginConfig().useTicketOnly ) {
            throw new IllegalStateException(
                    "This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
        }

        return sql.executeCommand("ALTER TABLE `" + sql.getMoneyTableName() + "` " +
                "ADD COLUMN `" + name + "` BIGINT(20) NULL DEFAULT 0;");
    }

    public static boolean addLastjoinColmun(String name) {

        if ( HomoSystem.getPluginConfig().useTicketOnly ) {
            throw new IllegalStateException(
                    "This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
        }

        return sql.executeCommand("ALTER TABLE `" + sql.getLastjoinTableName() + "` ADD COLUMN `" + name
                + "` BIGINT(20) NULL DEFAULT '0'");
    }

    public static List<String> getColumnsFromTicketValueData() {

        List<String> colmnList = new ArrayList<>();

        Statement stm = sql.createStatement();

        try {
            String cmd = "select server from " + sql.getTicketValueTableName() + ";";
            ResultSet set = stm.executeQuery(cmd);

            while ( set.next() ) {
                String server = set.getString("server");
                colmnList.add(server);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            SQLHandler.closeStatement(stm);
        }

        return colmnList;
    }

    public static List<String> getColumnsFromMoneyData() {

        List<String> colmnList = new ArrayList<>();
        Statement stm = sql.createStatement();

        try {

            String cmd = "show columns from " + sql.getMoneyTableName() + ";";
            ResultSet set = stm.executeQuery(cmd);

            while ( set.next() ) {
                String column = set.getString("Field");
                String type = set.getString("Type");

                if ( !type.equals("bigint(20)") ) {
                    continue;
                }

                colmnList.add(column);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            SQLHandler.closeStatement(stm);
        }

        return colmnList;
    }

    public static List<String> getColumnsFromLastjoin() {

        List<String> colmnList = new ArrayList<>();
        Statement stm = sql.createStatement();

        try {

            String cmd = "show columns from " + sql.getLastjoinTableName() + ";";
            ResultSet set = stm.executeQuery(cmd);

            while ( set.next() ) {
                String column = set.getString("Field");
                String type = set.getString("Type");

                if ( !type.equals("bigint(20)") ) {
                    continue;
                }

                colmnList.add(column);
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            SQLHandler.closeStatement(stm);
        }

        return colmnList;
    }

    public static void closeAll() {
        sql.closeConnection();
    }

    @Deprecated
    public static SQLHandler getSQL() {
        return sql;
    }

    protected static SQLHandler getProtectedSQL() {
        return sql;
    }
}
