package jp.azisaba.main.homos.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.Median;
import jp.azisaba.main.homos.classes.PlayerData;

public class SQLDataManager {

	@SuppressWarnings("unused")
	private static Homos plugin;

	private static SQLHandler sql = null;

	public static void init(Homos plugin) {

		SQLDataManager.plugin = plugin;

		String ip = Homos.config.sqlIp;
		int port = Homos.config.sqlPort;
		String schema = Homos.config.sqlSchema;
		String user = Homos.config.sqlUser;
		String password = Homos.config.sqlPassword;

		sql = new SQLHandler(plugin, ip, port, schema, user, password);

		plugin.getLogger().info("SQL Setup done.");
		plugin.getLogger().info("Complete.");
	}

	public static PlayerData getPlayerData(Player p) {
		PlayerData data = new PlayerData(p.getUniqueId(), 0);

		sql.setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(UUID uuid) {
		PlayerData data = new PlayerData(uuid, 0);

		sql.setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(String name) {
		PlayerData data = new PlayerData(name, 0);

		sql.setUpPlayerData(data);
		return data;
	}

	public static boolean addTicket(Player p, int value) {
		return addTicket(p.getUniqueId(), value);
	}

	public static boolean addTicket(UUID uuid, int value) {
		String addTicket = "INSERT INTO ticketdata (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value + ") ON DUPLICATE KEY UPDATE tickets=tickets+VALUES(tickets);";

		boolean success1 = sql.executeCommand(addTicket);
		boolean success2 = addMoney(uuid, value);

		return success1 && success2;
	}

	private static boolean addMoney(UUID uuid, int ticketAmount) {
		List<String> colmnList = sql.getColumnsFromMedianData();
		List<Median> medians = sql.getMedianData(colmnList.toArray(new String[colmnList.size()]));

		List<String> addValueList = new ArrayList<>();
		for (Median med : medians) {
			addValueList.add("" + (med.getMedian() * ticketAmount));
		}

		String uuidStr = "'" + uuid.toString() + "'";
		StringBuilder builder = new StringBuilder("INSERT INTO " + sql.getMoneyTableName() + " "); // insert
		builder.append("(uuid, " + String.join(", ", colmnList) + ") "); // value define
		builder.append("VALUES (" + uuidStr + ", " + String.join(", ", addValueList) + ") "); // values
		builder.append("ON DUPLICATE KEY UPDATE "); // duplicate

		for (Median med : medians) {
			builder.append(med.getServerName() + "=" + med.getServerName() + "+VALUES(" + med.getServerName() + ") "); // updates
		}

		boolean success = sql.executeCommand(builder.toString() + ";");
		return success;
	}

	public static boolean removeTicket(Player p, int value) {
		return removeTicket(p.getUniqueId(), value);
	}

	public static boolean removeTicket(UUID uuid, int value) {

		if (sql.getTickets(uuid) - value < 0) {
			throw new IllegalArgumentException("Value must be greater than the player has.");
		}

		String removeTicket = "INSERT INTO " + sql.getTicketTableName() + " (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value + ") ON DUPLICATE KEY UPDATE tickets=tickets-VALUES(tickets);";

		boolean success2 = removeMoney(uuid, value);
		boolean success1 = sql.executeCommand(removeTicket);

		return success1 && success2;
	}

	private static boolean removeMoney(UUID uuid, int ticketAmount) {
		List<String> colmnList = sql.getColumnsFromMedianData();
		List<Median> medians = sql.getMedianData(colmnList.toArray(new String[colmnList.size()]));

		HashMap<String, String> valueMap = new HashMap<>();
		for (Median med : medians) {

			double valueOfTickets = valueOfTickets(uuid, med.getServerName(), ticketAmount);

			if (valueOfTickets < 0) {
				continue;
			}

			valueMap.put(med.getServerName(), String.valueOf((int) Math.ceil(valueOfTickets)));
		}

		String uuidStr = "'" + uuid.toString() + "'";
		StringBuilder builder = new StringBuilder("INSERT INTO " + sql.getMoneyTableName() + " "); // insert
		builder.append("(uuid, " + String.join(", ", colmnList) + ") "); // value define
		builder.append("VALUES (" + uuidStr + ", " + String.join(", ", valueMap.values()) + ") "); // values
		builder.append("ON DUPLICATE KEY UPDATE "); // duplicate

		for (Median med : medians) {
			builder.append(
					"{SERVER}=(CASE WHEN {SERVER}-VALUES({SERVER})<0 THEN 0 ELSE {SERVER}-VALUES({SERVER}) END), "
							.replace("{SERVER}", med.getServerName())); // updates
		}

		boolean success = sql
				.executeCommand(builder.toString().substring(0, builder.toString().lastIndexOf(",")) + ";");
		return success;
	}

	public static double valueOfTickets(Player p, String server, int amount) {
		return valueOfTickets(p.getUniqueId(), server, amount);
	}

	public static double valueOfTickets(UUID uuid, String server, int amount) {
		PlayerData data = getPlayerData(uuid);

		long money = data.getMoney(server);

		if (money < 0) {

			if (!sql.getColumnsFromMedianData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return -1d;
		}

		double value = (money / data.getTickets()) * amount;
		return value;
	}

	public static double valueOfTicketsToConvertMoney(UUID uuid, String server, int amount) {
		PlayerData data = getPlayerData(uuid);

		long money = data.getMoney(server);

		if (money < 0) {

			if (!sql.getColumnsFromMedianData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return -1d;
		}

		double value = money / data.getTickets();
		value = value * amount * 0.9;

		return value;
	}

	public static boolean updatePlayerData(UUID uuid, String name, long lastjoin) {

		String cmd = "INSERT INTO " + sql.getTicketTableName() + " (uuid, name, lastjoin) VALUES ('" + uuid.toString()
				+ "', '" + name + "', " + lastjoin
				+ ") ON DUPLICATE KEY UPDATE name=VALUES(name), lastjoin=VALUES(lastjoin);";

		boolean success = sql.executeCommand(cmd);

		return success;
	}

	public static List<PlayerData> getPlayerDataListBefore30Days() {
		return sql.getPlayerDataListBefore30Days();
	}

	public static SQLHandler getSQL() {
		return sql;
	}

	public static void closeAll() {
		sql.closeConnection();
	}
}
