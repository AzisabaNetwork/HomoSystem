package jp.azisaba.main.homos.database;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.classes.PlayerData;

public class PlayerTicketManager {

	@SuppressWarnings("unused")
	private static HOMOs plugin;

	private static SQLHandler sql = null;

	public static void init(HOMOs plugin) {

		PlayerTicketManager.plugin = plugin;

		String ip = HOMOs.config.sqlIp;
		int port = HOMOs.config.sqlPort;
		String schema = HOMOs.config.sqlSchema;
		String user = HOMOs.config.sqlUser;
		String password = HOMOs.config.sqlPassword;

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
		return sql.executeCommand(
				"INSERT INTO " + sql.getTableName() + " (uuid, name, tickets, money, lastjoin) VALUES ('"
						+ uuid.toString()
						+ "', NULL, " + value + ", " + HOMOs.getMedianManager().getCurrentMedian() * value + ", "
						+ System.currentTimeMillis()
						+ ") ON DUPLICATE KEY UPDATE tickets=tickets+VALUES(tickets), money=money+VALUES(money)");
	}

	public static boolean removeTicket(Player p, int value) {
		return removeTicket(p.getUniqueId(), value);
	}

	public static boolean removeTicket(UUID uuid, int value) {

		if (sql.getTickets(uuid) - value < 0) {
			throw new IllegalArgumentException("Value must be greater than the player has.");
		}

		return sql.executeCommand(
				"INSERT INTO " + sql.getTableName() + " (uuid, name, tickets, money, lastjoin) VALUES ('"
						+ uuid.toString()
						+ "', NULL, " + value + ", " + (int) (Math.ceil(valueOfTickets(uuid, value))) + ", "
						+ System.currentTimeMillis()
						+ ") ON DUPLICATE KEY UPDATE tickets=tickets-VALUES(tickets), money=(CASE WHEN money-VALUES(money)<0 THEN 0 ELSE money-VALUES(money) END)");
	}

	public static double valueOfTickets(Player p, int amount) {
		return valueOfTickets(p.getUniqueId(), amount);
	}

	public static double valueOfTickets(UUID uuid, int amount) {
		PlayerData data = getPlayerData(uuid);

		double value = data.getMoney() / data.getTickets();
		return value * amount;
	}

	public static double valueOfTicketsToConvertMoney(UUID uuid, int amount) {
		PlayerData data = getPlayerData(uuid);
		double value = data.getMoney() / data.getTickets();
		value = value * amount;

		value = value * 0.9;

		return value;
	}

	public static boolean updatePlayerData(PlayerData data) {
		return sql.executeCommand(
				"INSERT INTO " + sql.getTableName() + " (uuid, name, tickets, money, lastjoin) VALUES ('"
						+ data.getUuid().toString() + "', '" + data.getName() + "', " + data.getTickets() + ", "
						+ data.getMoney() + ", " + data.getLastJoin()
						+ ") ON DUPLICATE KEY UPDATE name=VALUES(name), tickets=VALUES(tickets), money=VALUES(money), lastjoin=VALUES(lastjoin)");
	}

	public static List<PlayerData> getPlayerDataListBefore30Days() {
		return sql.getPlayerDataListBefore30Days();
	}

	public static void closeAll() {
		sql.closeConnection();
	}
}
