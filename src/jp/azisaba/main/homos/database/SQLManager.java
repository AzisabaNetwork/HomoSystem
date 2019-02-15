package jp.azisaba.main.homos.database;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jp.azisaba.main.homos.Homos;

public class SQLManager {

	@SuppressWarnings("unused")
	private static Homos plugin;

	private static SQLHandler sql = null;

	public static void init(Homos plugin) {

		SQLManager.plugin = plugin;

		String ip = Homos.config.sqlIp;
		int port = Homos.config.sqlPort;
		String schema = Homos.config.sqlSchema;
		String user = Homos.config.sqlUser;
		String password = Homos.config.sqlPassword;

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
				"  `tickets` int(11) NOT NULL DEFAULT '0'," +
				"  `lastjoin` bigint(20) NOT NULL DEFAULT '0'," +
				"  PRIMARY KEY (`uuid`)," +
				"  UNIQUE KEY `uuid_UNIQUE` (`uuid`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

		String createMedian = "CREATE TABLE IF NOT EXISTS `median` (" +
				"  `server` varchar(32) NOT NULL," +
				"  `median` bigint(20) DEFAULT '1000'," +
				"  `locked` tinyint(1) DEFAULT '0'," +
				"  `boost` tinyint(1) DEFAULT '0'," +
				"  PRIMARY KEY (`server`)," +
				"  UNIQUE KEY `server_UNIQUE` (`server`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

		String createMoneyData = "CREATE TABLE IF NOT EXISTS `moneydata` (" +
				"  `uuid` varchar(36) NOT NULL," +
				"  PRIMARY KEY (`uuid`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

		boolean success1 = sql.executeCommand(createTicketData);
		boolean success2 = sql.executeCommand(createMedian);
		boolean success3 = sql.executeCommand(createMoneyData);

		return success1 && success2 && success3;
	}

	public static boolean addMoneyDataColmun(String name) {
		return sql.executeCommand("ALTER TABLE `homos`.`" + sql.getMoneyTableName() + "` " +
				"ADD COLUMN `" + name + "` BIGINT(20) NULL DEFAULT 0;");
	}

	public static List<String> getColumnsFromMedianData() {

		List<String> colmnList = new ArrayList<>();

		try {
			String cmd = "select server from " + sql.getMedianTableName() + ";";
			ResultSet set = sql.executeQuery(cmd);

			while (set.next()) {
				String server = set.getString("server");
				colmnList.add(server);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return colmnList;
	}

	public static List<String> getColumnsFromMoneyData() {

		List<String> colmnList = new ArrayList<>();
		try {

			String cmd = "show columns from " + sql.getMoneyTableName() + ";";
			ResultSet set = sql.executeQuery(cmd);

			while (set.next()) {
				String column = set.getString("Field");
				String type = set.getString("Type");

				if (!type.equals("bigint(20)")) {
					continue;
				}

				colmnList.add(column);
			}

		} catch (Exception e) {
			e.printStackTrace();
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