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
					"jdbc:mysql://" + ip + ":" + port + "/" + database
							+ "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true",
					user,
					password);
		} catch (SQLException e) {
			plugin.getLogger().warning("Failed to connect to SQL database.");
			e.printStackTrace();
			return;
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

	public synchronized ResultSet executeQuery(String str) {
		ResultSet set = null;
		Statement stm = null;

		try {
			stm = con.createStatement();
			set = stm.executeQuery(str);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return set;
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

	public boolean addMoneyDataColmun(String name) {
		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` " +
				"ADD COLUMN `" + name + "` BIGINT(20) NULL DEFAULT 0;");
	}

	public boolean removeMoneyDataColmun(String name) {
		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` DROP COLUMN `" + name + "`;");
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
