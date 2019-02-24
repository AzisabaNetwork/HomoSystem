package jp.azisaba.main.homos.database;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;

import jp.azisaba.main.homos.classes.PlayerData;

public class PlayerDataManager {

	public static PlayerData getPlayerData(Player p) {
		PlayerData data = new PlayerData(p.getUniqueId(), BigInteger.ZERO);

		setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(UUID uuid) {
		PlayerData data = new PlayerData(uuid, BigInteger.ZERO);

		setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(String name) {
		PlayerData data = new PlayerData(name, BigInteger.ZERO);

		setUpPlayerData(data);
		return data;
	}

	public static boolean updatePlayerData(UUID uuid, String name, long lastjoin) {

		SQLHandler sql = SQLManager.getProtectedSQL();

		String cmd = "INSERT INTO " + sql.getTicketTableName() + " (uuid, name, lastjoin) VALUES ('" + uuid.toString()
				+ "', '" + name + "', " + lastjoin
				+ ") ON DUPLICATE KEY UPDATE name=VALUES(name), lastjoin=VALUES(lastjoin);";

		List<String> columns = SQLManager.getColumnsFromMoneyData();

		String moneyCmd = "INSERT INTO " + sql.getMoneyTableName() + " (uuid, "
				+ String.join(", ", SQLManager.getColumnsFromMoneyData())
				+ ") VALUES ('" + uuid.toString() + "'" + StringUtils.repeat(", 0", columns.size())
				+ ") ON DUPLICATE KEY UPDATE uuid=uuid;";

		boolean success = sql.executeCommand(cmd);
		boolean success2 = sql.executeCommand(moneyCmd);

		return success && success2;
	}

	public static List<PlayerData> getPlayerDataListBefore30Days() {

		SQLHandler sql = SQLManager.getProtectedSQL();

		String cmd = "select * from " + sql.getTicketTableName() + " where lastjoin > "
				+ (System.currentTimeMillis() - (1000L/**millis*/
						* 60L/**seconds*/
						* 60L/**minutes*/
						* 24L/**hours*/
						* 30L/**days*/
				));

		List<UUID> uuidList = new ArrayList<>();

		Statement stm = sql.createStatement();

		try {

			ResultSet set = stm.executeQuery(cmd);

			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				uuidList.add(uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return getPlayerDataListByUUIDList(uuidList);
	}

	public static List<PlayerData> getPlayerDataListByUUIDList(List<UUID> uuidList) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		List<String> columnList = SQLManager.getColumnsFromMoneyData();

		List<PlayerData> dataList = new ArrayList<>();
		StringBuilder basicCmd = new StringBuilder("select * from " + sql.getTicketTableName() + " where uuid = ''");
		StringBuilder moneyCmd = new StringBuilder("select * from " + sql.getMoneyTableName() + " where uuid = ''");

		for (UUID uuid : uuidList) {
			basicCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		basicCmd.append(";");

		for (UUID uuid : uuidList) {
			moneyCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		moneyCmd.append(";");

		Statement stm = sql.createStatement();
		try {

			HashMap<UUID, HashMap<String, BigInteger>> moneyMap = new HashMap<>();

			ResultSet moneySet = stm.executeQuery(moneyCmd.toString());

			while (moneySet.next()) {

				HashMap<String, BigInteger> moneyServerMap = new HashMap<>();

				for (String column : columnList) {
					String value = moneySet.getString(column);
					if (value == null)
						continue;

					moneyServerMap.put(column, new BigInteger(value));
				}

				moneyMap.put(UUID.fromString(moneySet.getString("uuid")), moneyServerMap);
			}

			ResultSet set = stm.executeQuery(basicCmd.toString());

			UUID uuid;
			String name;
			BigInteger tickets;
			long lastjoin;

			while (set.next()) {
				uuid = UUID.fromString(set.getString("uuid"));
				name = set.getString("name");
				tickets = new BigInteger(set.getString("tickets"));
				lastjoin = set.getLong("lastjoin");

				PlayerData data = new PlayerData(uuid, name, tickets, lastjoin);

				if (moneyMap.containsKey(uuid)) {
					HashMap<String, BigInteger> moneyServerMap = moneyMap.get(uuid);

					for (String key : moneyServerMap.keySet()) {
						data.setMoney(key, moneyServerMap.get(key));
					}
				}

				dataList.add(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return dataList;
	}

	public static boolean setUpPlayerData(PlayerData data) {

		SQLHandler sql = SQLManager.getProtectedSQL();

		boolean updated = false;

		UUID uuid = null;
		String name = null;
		BigInteger tickets = BigInteger.valueOf(-1);
		long lastjoin = -1;

		Statement stm = sql.createStatement();

		try {

			ResultSet playerDataSet = null;

			if (data.getUuid() != null) {

				String cmd = "select name, tickets, lastjoin from " + sql.getTicketTableName() + " where uuid = '"
						+ data.getUuid().toString() + "'";
				playerDataSet = stm.executeQuery(cmd);
			} else if (data.getName() != null) {

				String playerDataCmd = "SELECT uuid, tickets, lastjoin from " + sql.getTicketTableName()
						+ " where name='"
						+ data.getName() + "' LIMIT 0, 1";
				playerDataSet = stm.executeQuery(playerDataCmd);
			}

			if (playerDataSet.next()) {

				updated = true;

				if (data.getUuid() != null) {
					name = playerDataSet.getString("name");
					tickets = new BigInteger(playerDataSet.getString("tickets"));
					lastjoin = playerDataSet.getLong("lastjoin");
				} else if (data.getName() != null) {
					uuid = UUID.fromString(playerDataSet.getString("uuid"));
					tickets = new BigInteger(playerDataSet.getString("tickets"));
					lastjoin = playerDataSet.getLong("lastjoin");
				}
			}

			if (uuid == null) {
				uuid = data.getUuid();
			}

			String moneyDataCmd = "select " + String.join(", ", SQLManager.getColumnsFromMoneyData()) + " from "
					+ sql.getMoneyTableName() + " where uuid='" + uuid.toString() + "';";
			ResultSet moneyDataSet = stm.executeQuery(moneyDataCmd);

			if (moneyDataSet.next()) {

				List<String> columnList = SQLManager.getColumnsFromMoneyData();

				for (String str : columnList) {

					BigInteger value = new BigInteger(moneyDataSet.getString(str));

					if (value.compareTo(BigInteger.ZERO) >= 0) {
						data.setMoney(str, value);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			SQLHandler.closeStatement(stm);
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
}
