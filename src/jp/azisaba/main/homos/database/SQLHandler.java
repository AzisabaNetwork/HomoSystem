/**
 *
 */
package jp.azisaba.main.homos.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.Median;
import jp.azisaba.main.homos.classes.PlayerData;

/**
 * @author siloneco
 *
 */
public class SQLHandler {

	@SuppressWarnings("unused")
	private Homos plugin;

	private Connection con;

	private final String ticketdata_table = "ticketdata";
	private final String median_table = "median";
	private final String moneydata_table = "moneydata";

	protected SQLHandler(Homos plugin, String ip, int port, String database, String user, String password) {

		this.plugin = plugin;

		plugin.getLogger().info("Connecting MySQL...");
		plugin.getLogger().info("IP: " + ip);
		plugin.getLogger().info("User: " + user);
		plugin.getLogger().info("Port: " + port);
		plugin.getLogger().info("Database: " + database);
		plugin.getLogger().info("Password: " + StringUtils.repeat("*", password.length()));

		try {
			con = DriverManager.getConnection(
					"jdbc:mysql://" + ip + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false", user,
					password);
		} catch (SQLException e) {
			plugin.getLogger().warning("Failed to connect to SQL database.");
			e.printStackTrace();
			return;
		}

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

		Statement stm = null;

		// Create table if not exists.
		try {
			stm = con.createStatement();
			stm.executeUpdate(createTicketData);
			stm.executeUpdate(createMedian);
			stm.executeUpdate(createMoneyData);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}
	}

	public synchronized boolean executeCommand(String str) {

		str = str.replace("%table%", ticketdata_table);

		boolean success = false;
		Statement stm = null;

		try {
			stm = con.createStatement();

			stm.executeUpdate(str);

			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return success;
	}

	public int getTickets(UUID uuid) {
		int tickets = -1;
		Statement stm = null;

		try {
			stm = con.createStatement();

			ResultSet set = stm
					.executeQuery(
							"select tickets from " + ticketdata_table + " where uuid = '" + uuid.toString() + "'");

			if (set.next()) {
				tickets = set.getInt("tickets");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return tickets;
	}

	public long getLastJoin(UUID uuid) {
		long lastjoin = -1;
		Statement stm = null;

		try {
			stm = con.createStatement();

			ResultSet set = stm
					.executeQuery(
							"select lastjoin from " + ticketdata_table + " where uuid = '" + uuid.toString() + "'");

			if (set.next()) {
				lastjoin = set.getInt("lastjoin");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return lastjoin;
	}

	public boolean setUpPlayerData(PlayerData data) {

		boolean updated = false;

		UUID uuid = null;
		String name = null;
		int tickets = -1;
		long lastjoin = -1;

		Statement stm = null;

		try {
			stm = con.createStatement();

			ResultSet playerDataSet = null;

			if (data.getUuid() != null) {
				playerDataSet = stm
						.executeQuery("select name, tickets, lastjoin from " + ticketdata_table + " where uuid = '"
								+ data.getUuid().toString() + "'");
			} else if (data.getName() != null) {

				String playerDataCmd = "SELECT (uuid, tickets, lastjoin) from " + ticketdata_table + " where name='"
						+ data.getName() + "' LIMIT 0, 1";
				playerDataSet = stm.executeQuery(playerDataCmd);
			}

			if (playerDataSet.next()) {

				updated = true;

				if (data.getUuid() != null) {
					name = playerDataSet.getString("name");
					tickets = playerDataSet.getInt("tickets");
					lastjoin = playerDataSet.getLong("lastjoin");
				} else if (data.getName() != null) {
					uuid = UUID.fromString(playerDataSet.getString("uuid"));
					tickets = playerDataSet.getInt("tickets");
					lastjoin = playerDataSet.getLong("lastjoin");
				}
			}

			String moneyDataCmd = "select (" + String.join(", ", getColumnsFromMedianData()) + ") from "
					+ moneydata_table + " where uuid='" + data.getUuid().toString() + "';";
			ResultSet moneyDataSet = stm.executeQuery(moneyDataCmd);

			if (moneyDataSet.next()) {

				List<String> columnList = getColumnsFromMedianData();

				for (String str : columnList) {
					long value = moneyDataSet.getLong(str);

					if (value > 0) {
						data.setMoney(str, value);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		if (!updated) {
			return false;
		}

		if (data.getUuid() == null)
			data.setUuid(uuid);

		if (data.getName() == null)
			data.setName(name);

		data.setTickets(tickets);
		data.setLastJoin(lastjoin);

		return true;
	}

	public List<PlayerData> getPlayerDataListBefore30Days() {

		Statement stm = null;
		List<PlayerData> playerDataList = new ArrayList<>();

		try {
			stm = con.createStatement();

			String cmd = "select * from " + ticketdata_table + " where lastjoin > "
					+ (System.currentTimeMillis() - (1000L/**millis*/
							* 60L/**seconds*/
							* 60L/**minutes*/
							* 24L/**hours*/
							* 30L/**days*/
					));

			ResultSet set = stm.executeQuery(cmd);

			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				String name = set.getString("name");
				int tickets = set.getInt("tickets");
				long lastjoin = set.getLong("lastjoin");

				PlayerData data = new PlayerData(uuid, name, tickets, lastjoin);

				playerDataList.add(data);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return playerDataList;
	}

	public boolean addMoneyDataColmun(String name) {
		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` " +
				"ADD COLUMN `" + name + "` BIGINT(20) NULL DEFAULT 0;");
	}

	public boolean removeMoneyDataColmun(String name) {
		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` DROP COLUMN `" + name + "`;");
	}

	public List<String> getColumnsFromMedianData() {

		Statement stm = null;
		List<String> colmnList = new ArrayList<>();
		try {
			stm = con.createStatement();

			String cmd = "select server from " + getMedianTableName() + ";";
			ResultSet set = stm.executeQuery(cmd);

			while (set.next()) {
				String server = set.getString("server");
				colmnList.add(server);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return colmnList;
	}

	public List<String> getColumnsFromMoneyData() {

		Statement stm = null;
		List<String> colmnList = new ArrayList<>();
		try {
			stm = con.createStatement();

			String cmd = "show columns from " + getMoneyTableName() + ";";
			ResultSet set = stm.executeQuery(cmd);

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
		} finally {
			closeStatement(stm);
		}

		return colmnList;
	}

	public Median getMedianData(String serverName) {
		Statement stm = null;
		Median median = null;

		try {
			stm = con.createStatement();

			ResultSet set = stm
					.executeQuery("select * from " + getMedianTableName() + " where server='" + serverName + "';");

			if (set.next()) {
				String exactServerName = set.getString("server");
				int medianNum = set.getInt("median");
				boolean locked = set.getInt("locked") > 0;
				boolean boost = set.getInt("boost") > 0;

				median = new Median(exactServerName, medianNum, locked, boost);
			} else {
				stm.executeUpdate("INSERT INTO " + getMedianTableName() + " (server, median) VALUES ('" + serverName
						+ "', 1000) ON DUPLICATE KEY UPDATE median=VALUES(median);");

				median = new Median(serverName, 1000, false, true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return median;
	}

	public List<Median> getMedianData(String... servers) {
		Statement stm = null;
		List<Median> medianList = new ArrayList<>();

		try {
			stm = con.createStatement();

			String where = "server='" + String.join("', OR server='", servers) + "'";

			ResultSet set = stm
					.executeQuery("select * from " + getMedianTableName() + " where " + where + ";");

			while (set.next()) {
				String exactServerName = set.getString("server");
				int medianNum = set.getInt("median");
				boolean locked = set.getInt("locked") > 0;
				boolean boost = set.getInt("boost") > 0;

				medianList.add(new Median(exactServerName, medianNum, locked, boost));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return medianList;
	}

	public void closeConnection() {
		try {
			if (con != null && !con.isClosed()) {
				con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getTicketTableName() {
		return ticketdata_table;
	}

	public String getMedianTableName() {
		return median_table;
	}

	public String getMoneyTableName() {
		return moneydata_table;
	}

	private boolean closeStatement(Statement stm) {
		try {
			if (stm != null && !stm.isClosed()) {
				stm.close();
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
