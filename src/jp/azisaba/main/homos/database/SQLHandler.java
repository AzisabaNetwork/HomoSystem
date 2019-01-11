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

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.classes.PlayerData;

/**
 * @author siloneco
 *
 */
public class SQLHandler {

	@SuppressWarnings("unused")
	private HOMOs plugin;

	private Connection con;

	private final String TABLE_NAME = "ticketdata";

	protected SQLHandler(HOMOs plugin, String ip, int port, String database, String user, String password) {

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

		String createTable = "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME
				+ "` (`uuid` VARCHAR(36) NOT NULL, `name` TEXT, `tickets` INT NOT NULL DEFAULT 0,"
				+ " `money` BIGINT NOT NULL DEFAULT 0, `lastjoin` BIGINT NOT NULL DEFAULT 0,"
				+ " PRIMARY KEY (`uuid`), UNIQUE INDEX `uuid_UNIQUE` (`uuid` ASC) VISIBLE);";

		Statement stm = null;

		// Create table if not exists.
		try {
			stm = con.createStatement();
			stm.executeUpdate(createTable);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}
	}

	public synchronized boolean executeCommand(String str) {

		str = str.replace("%table%", TABLE_NAME);

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
					.executeQuery("select tickets from " + TABLE_NAME + " where uuid = '" + uuid.toString() + "'");

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
					.executeQuery("select lastjoin from " + TABLE_NAME + " where uuid = '" + uuid.toString() + "'");

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
		long money = 0;

		Statement stm = null;

		try {
			stm = con.createStatement();

			ResultSet set = null;

			if (data.getUuid() != null) {
				set = stm.executeQuery("select name, tickets, money, lastjoin from " + TABLE_NAME + " where uuid = '"
						+ data.getUuid().toString() + "'");
			} else if (data.getName() != null) {
				set = stm.executeQuery("select uuid, tickets, money, lastjoin from " + TABLE_NAME + " where name = '"
						+ data.getName() + "' LIMIT 0, 1");
			}

			if (set.next()) {

				updated = true;

				if (data.getUuid() != null) {
					name = set.getString("name");
					tickets = set.getInt("tickets");
					lastjoin = set.getLong("lastjoin");
					money = set.getLong("money");
				} else if (data.getName() != null) {
					uuid = UUID.fromString(set.getString("uuid"));
					tickets = set.getInt("tickets");
					lastjoin = set.getLong("lastjoin");
					money = set.getLong("money");
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
		data.setMoney(money);

		return true;
	}

	public List<PlayerData> getPlayerDataListBefore30Days() {

		Statement stm = null;
		List<PlayerData> playerDataList = new ArrayList<>();

		try {
			stm = con.createStatement();

			String cmd = "select * from " + TABLE_NAME + " where lastjoin > "
					+ (System.currentTimeMillis() - (1000L * 60L * 60L * 24L * 30L));

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

	public void closeConnection() {
		try {
			if (con != null && !con.isClosed()) {
				con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getTableName() {
		return TABLE_NAME;
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
