package jp.azisaba.main.homos.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.classes.Median;
import jp.azisaba.main.homos.classes.PlayerData;

public class TicketManager {

	public static boolean addTicket(UUID uuid, int value) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		String addTicket = "INSERT INTO ticketdata (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value + ") ON DUPLICATE KEY UPDATE tickets=tickets+VALUES(tickets);";

		boolean success1 = sql.executeCommand(addTicket);
		boolean success2 = addMoney(uuid, value);

		return success1 && success2;
	}

	public static boolean removeTicket(UUID uuid, int value) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		if (sql.getTickets(uuid) - value < 0) {
			throw new IllegalArgumentException("Value must be greater than the player has.");
		}

		String removeTicket = "INSERT INTO " + sql.getTicketTableName() + " (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value + ") ON DUPLICATE KEY UPDATE tickets=tickets-VALUES(tickets);";

		boolean success2 = removeMoney(uuid, value);
		boolean success1 = sql.executeCommand(removeTicket);

		return success1 && success2;
	}

	public static double valueOfTickets(UUID uuid, String server, int amount) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		long money = data.getMoney(server);

		if (money < 0) {

			if (!SQLManager.getColumnsFromMedianData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return -1d;
		}

		double value = (money / data.getTickets()) * amount;
		return value;
	}

	public static double valueOfTicketsToConvertMoney(UUID uuid, String server, int amount) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		long money = data.getMoney(server);

		if (money < 0) {

			if (!SQLManager.getColumnsFromMedianData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return -1d;
		}

		double value = money / data.getTickets();
		value = value * amount * 0.9;

		return value;
	}

	private static boolean addMoney(UUID uuid, int ticketAmount) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> colmnList = SQLManager.getColumnsFromMedianData();
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

	private static boolean removeMoney(UUID uuid, int ticketAmount) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> colmnList = SQLManager.getColumnsFromMedianData();
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

	public static boolean addTicket(Player p, int value) {
		return addTicket(p.getUniqueId(), value);
	}

	public static boolean removeTicket(Player p, int value) {
		return removeTicket(p.getUniqueId(), value);
	}

	public static double valueOfTickets(Player p, String server, int amount) {
		return valueOfTickets(p.getUniqueId(), server, amount);
	}
}
