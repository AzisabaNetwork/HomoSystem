package jp.azisaba.main.homos.database;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.classes.TicketValueData;

public class TicketManager {

	public static boolean addTicket(UUID uuid, BigInteger value) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		String addTicket = "INSERT INTO ticketdata (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value.toString() + ") ON DUPLICATE KEY UPDATE tickets=tickets+VALUES(tickets);";

		boolean success1 = sql.executeCommand(addTicket);
		boolean success2 = addMoney(uuid, value);

		return success1 && success2;
	}

	public static boolean removeTicket(UUID uuid, BigInteger value) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		if (sql.getTickets(uuid).subtract(value).compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("Value must be greater than the player has.");
		}

		String removeTicket = "INSERT INTO " + sql.getTicketTableName() + " (uuid, tickets) VALUES ('" + uuid.toString()
				+ "', " + value.toString() + ") ON DUPLICATE KEY UPDATE tickets=tickets-VALUES(tickets);";

		boolean success2 = removeMoney(uuid, value);
		boolean success1 = sql.executeCommand(removeTicket);

		return success1 && success2;
	}

	public static BigDecimal valueOfTickets(UUID uuid, String server, BigInteger value) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		BigInteger money = data.getMoney(server);

		if (money.compareTo(BigInteger.ZERO) < 0) {

			if (!SQLManager.getColumnsFromTicketValueData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return BigDecimal.valueOf(-1);
		}

		BigDecimal ticketValue = new BigDecimal(money)
				.divide(new BigDecimal(data.getTickets()), 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(value));

		return ticketValue;
	}

	public static BigDecimal valueOfTicketsToConvertMoney(UUID uuid, String server, BigInteger amount) {
		PlayerData data = PlayerDataManager.getPlayerData(uuid);

		BigDecimal money;
		if (server != null) {
			money = new BigDecimal(data.getMoney(server));
		} else {
			money = new BigDecimal(data.getMoney());
		}

		if (money.compareTo(BigDecimal.ZERO) < 0) {

			if (!SQLManager.getColumnsFromTicketValueData().contains(server)) {
				throw new IllegalArgumentException("There is no server called \"" + server + "\"");
			}

			return BigDecimal.valueOf(-1);
		}

		BigDecimal tickets = new BigDecimal(data.getTickets());

		if (tickets.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal value = money.divide(tickets, 2, BigDecimal.ROUND_HALF_UP);
		value = value.multiply(new BigDecimal(amount)).multiply(BigDecimal.valueOf(0.9));

		return value;
	}

	private static boolean addMoney(UUID uuid, BigInteger value) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> colmnList = SQLManager.getColumnsFromTicketValueData();
		List<TicketValueData> values = sql.getTicketValueData(colmnList.toArray(new String[colmnList.size()]));

		List<String> addValueList = new ArrayList<>();
		for (TicketValueData med : values) {
			addValueList.add("" + med.getTicketValue().multiply(value).toString());
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

			BigDecimal valueOfTickets = valueOfTickets(uuid, med.getServerName(), value);

			if (valueOfTickets.compareTo(BigDecimal.ZERO) < 0) {
				continue;
			}

			valueMap.put(med.getServerName(), valueOfTickets.setScale(0, BigDecimal.ROUND_DOWN).toString());
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
		return addTicket(p.getUniqueId(), value);
	}

	public static boolean removeTicket(Player p, BigInteger value) {
		return removeTicket(p.getUniqueId(), value);
	}

	public static BigDecimal valueOfTickets(Player p, String server, BigInteger amount) {
		return valueOfTickets(p.getUniqueId(), server, amount);
	}
}
