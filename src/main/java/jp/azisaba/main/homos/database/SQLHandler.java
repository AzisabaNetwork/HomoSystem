/**
 *
 */
package jp.azisaba.main.homos.database;

import java.math.BigInteger;
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
import jp.azisaba.main.homos.classes.TicketValueData;

/**
 * @author siloneco
 *
 */
public class SQLHandler {

	@SuppressWarnings("unused")
	private Homos plugin;

	private Connection con;

	private final String ticketdata_table = "ticketdata";
	private final String ticketvalue_table = "ticketvalue";
	private final String moneydata_table = "moneydata";
	private final String lastjoin_table = "lastjoin";

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

	@Deprecated
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

	protected Statement createStatement() {
		try {
			return con.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public BigInteger getTickets(UUID uuid) {
		BigInteger tickets = BigInteger.valueOf(-1);
		Statement stm = null;

		try {
			stm = con.createStatement();

			ResultSet set = stm
					.executeQuery(
							"select tickets from " + ticketdata_table + " where uuid = '" + uuid.toString() + "'");

			if (set.next()) {
				tickets = new BigInteger(set.getString("tickets"));
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

		if (Homos.config.useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` " +
				"ADD COLUMN `" + name + "` BIGINT(20) NULL DEFAULT 0;");
	}

	public boolean removeMoneyDataColmun(String name) {

		if (Homos.config.useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		return executeCommand("ALTER TABLE `homos`.`" + getMoneyTableName() + "` DROP COLUMN `" + name + "`;");
	}

	public TicketValueData getTicketValueData(String serverName) {
		Statement stm = null;
		TicketValueData value = null;

		try {
			stm = con.createStatement();

			ResultSet set = stm
					.executeQuery("select * from " + getTicketValueTableName() + " where server='" + serverName + "';");

			if (set.next()) {
				String exactServerName = set.getString("server");
				BigInteger ticketValueNum = new BigInteger(set.getString("value"));
				boolean locked = set.getInt("locked") > 0;
				boolean boost = set.getInt("boost") > 0;

				value = new TicketValueData(exactServerName, ticketValueNum, locked, boost);
			} else {
				stm.executeUpdate("INSERT INTO " + getTicketValueTableName() + " (server, value) VALUES ('" + serverName
						+ "', 1000) ON DUPLICATE KEY UPDATE value=VALUES(value);");

				value = new TicketValueData(serverName, BigInteger.valueOf(1000), false, true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return value;
	}

	public List<TicketValueData> getTicketValueData(String... servers) {
		Statement stm = null;
		List<TicketValueData> valueList = new ArrayList<>();

		try {
			stm = con.createStatement();

			String where = "server='" + String.join("', OR server='", servers) + "'";

			ResultSet set = stm
					.executeQuery("select * from " + getTicketValueTableName() + " where " + where + ";");

			while (set.next()) {
				String exactServerName = set.getString("server");
				BigInteger valueNum = new BigInteger(set.getString("value"));
				boolean locked = set.getInt("locked") > 0;
				boolean boost = set.getInt("boost") > 0;

				valueList.add(new TicketValueData(exactServerName, valueNum, locked, boost));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeStatement(stm);
		}

		return valueList;
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

	public String getTicketValueTableName() {
		return ticketvalue_table;
	}

	public String getMoneyTableName() {
		return moneydata_table;
	}

	public String getLastjoinTableName() {
		return lastjoin_table;
	}

	public static boolean closeStatement(Statement stm) {
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
