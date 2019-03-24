package jp.azisaba.main.homos.database;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.HomoSystem;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.classes.TicketValueData;

public class TicketManager {

	public static boolean addTicket(UUID uuid, BigInteger value) {

		if (HomoSystem.getPluginConfig().useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		SQLHandler sql = SQLManager.getProtectedSQL();

		String addTicket = "INSERT INTO ticketdata (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value.toString() + ") ON DUPLICATE KEY UPDATE tickets=tickets+VALUES(tickets);";

		boolean success1 = sql.executeCommand(addTicket);
		boolean success2 = addMoney(uuid, value);

		return success1 && success2;
	}

	public static boolean removeTicket(UUID uuid, BigInteger value) {

		SQLHandler sql = SQLManager.getProtectedSQL();

		int compare = sql.getTickets(uuid).subtract(value).compareTo(BigInteger.ZERO);
		if (compare < 0) {
			throw new IllegalArgumentException("Value must be greater than the player has.");
		} else if (compare == 0) {
			value = sql.getTickets(uuid);
		}

		String removeTicket = "INSERT INTO " + sql.getTicketTableName() + " (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value.toString() + ") ON DUPLICATE KEY UPDATE tickets=tickets-VALUES(tickets);";

		boolean success2 = removeMoney(uuid, value);
		boolean success1 = sql.executeCommand(removeTicket);

		return success1 && success2;
	}

	public static BigInteger valueOfTickets(UUID uuid, String server, BigInteger value) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		BigInteger money = data.getMoney(server);

		if (money.compareTo(BigInteger.ZERO) < 0) {

			if (!SQLManager.getColumnsFromTicketValueData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return BigInteger.valueOf(-1);
		}

		BigDecimal ticketValue = new BigDecimal(money)
				.divide(new BigDecimal(data.getTickets()), 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(value));

		return ticketValue.toBigInteger();
	}

	public static BigInteger valueOfTicketsToConvertMoney(UUID uuid, String server, BigInteger amount) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		BigInteger money;
		if (server != null) {
			money = data.getMoney(server);
		} else {
			money = data.getMoney();
		}

		if (money.compareTo(BigInteger.ZERO) < 0) {

			if (!SQLManager.getColumnsFromTicketValueData().contains(server)) {
				if (server == null) {
					throw new IllegalArgumentException(
							"There is no server data called null(\"" + HomoSystem.getPluginConfig().serverName + "\")");
				} else {
					throw new IllegalArgumentException(
							"There is no server data called \"" + server + "\"");
				}
			}

			return BigInteger.valueOf(-1);
		}

		BigInteger tickets = data.getTickets();

		if (tickets.compareTo(BigInteger.ZERO) <= 0) {
			return BigInteger.ZERO;
		}

		BigInteger value = money.divide(tickets);
		value = (new BigDecimal(value.multiply(amount)).multiply(BigDecimal.valueOf(0.9))).toBigInteger();

		return value;
	}

	private static boolean addMoney(UUID uuid, BigInteger value) {

		if (HomoSystem.getPluginConfig().useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> colmnList = SQLManager.getColumnsFromTicketValueData();
		List<TicketValueData> values = sql.getTicketValueData(colmnList.toArray(new String[colmnList.size()]));

		List<String> addValueList = new ArrayList<>();
		for (TicketValueData med : values) {
			addValueList.add(med.getTicketValue().multiply(value).toString());
		}

		String uuidStr = "'" + uuid.toString() + "'";
		StringBuilder builder = new StringBuilder("INSERT INTO " + sql.getMoneyTableName() + " "); // insert
		builder.append("(uuid, " + String.join(", ", colmnList) + ") "); // value define
		builder.append("VALUES (" + uuidStr + ", " + String.join(", ", addValueList) + ") "); // values
		builder.append("ON DUPLICATE KEY UPDATE "); // duplicate

		for (TicketValueData med : values) {
			builder.append(med.getServerName() + "=" + med.getServerName() + "+VALUES(" + med.getServerName() + ") "); // updates
		}

		boolean success = sql.executeCommand(builder.toString() + ";");
		return success;
	}

	private static boolean removeMoney(UUID uuid, BigInteger value) {

		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> colmnList = SQLManager.getColumnsFromTicketValueData();
		List<TicketValueData> values = sql.getTicketValueData(colmnList.toArray(new String[colmnList.size()]));

		HashMap<String, String> valueMap = new HashMap<>();
		for (TicketValueData med : values) {

			BigInteger valueOfTickets = valueOfTickets(uuid, med.getServerName(), value);

			if (valueOfTickets.compareTo(BigInteger.ZERO) < 0) {
				continue;
			}

			valueMap.put(med.getServerName(), valueOfTickets.toString());
		}

		String uuidStr = "'" + uuid.toString() + "'";
		StringBuilder builder = new StringBuilder("INSERT INTO " + sql.getMoneyTableName() + " "); // insert
		builder.append("(uuid, " + String.join(", ", colmnList) + ") "); // value define
		builder.append("VALUES (" + uuidStr + ", " + String.join(", ", valueMap.values()) + ") "); // values
		builder.append("ON DUPLICATE KEY UPDATE "); // duplicate

		for (TicketValueData med : values) {
			builder.append(
					"{SERVER}=(CASE WHEN {SERVER}-VALUES({SERVER})<0 THEN 0 ELSE {SERVER}-VALUES({SERVER}) END), "
							.replace("{SERVER}", med.getServerName())); // updates
		}

		boolean success = sql
				.executeCommand(builder.toString().substring(0, builder.toString().lastIndexOf(",")) + ";");
		return success;
	}

	public static boolean addTicket(Player p, BigInteger value) {

		if (HomoSystem.getPluginConfig().useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		return addTicket(p.getUniqueId(), value);
	}

	public static boolean removeTicket(Player p, BigInteger value) {
		return removeTicket(p.getUniqueId(), value);
	}

	public static BigInteger valueOfTickets(Player p, String server, BigInteger amount) {
		return valueOfTickets(p.getUniqueId(), server, amount);
	}
}
