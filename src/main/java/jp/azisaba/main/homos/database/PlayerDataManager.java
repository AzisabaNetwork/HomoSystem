package jp.azisaba.main.homos.database;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;

import jp.azisaba.main.homos.HomoSystem;
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

		if (HomoSystem.getPluginConfig().useTicketOnly) {
			throw new IllegalStateException(
					"This plugin is now USE TICKET ONLY MODE. You can turn off read only mode in the config.");
		}

		SQLHandler sql = SQLManager.getProtectedSQL();

		String cmd = "INSERT INTO " + sql.getTicketTableName() + " (uuid, name) VALUES ('" + uuid.toString()
				+ "', '" + name + "') ON DUPLICATE KEY UPDATE name=VALUES(name);";

		List<String> columns = SQLManager.getColumnsFromMoneyData();

		String moneyCmd = "INSERT INTO " + sql.getMoneyTableName() + " (uuid, "
				+ String.join(", ", SQLManager.getColumnsFromMoneyData())
				+ ") VALUES ('" + uuid.toString() + "'" + StringUtils.repeat(", 0", columns.size())
				+ ") ON DUPLICATE KEY UPDATE uuid=uuid;";

		String lastJoinCmd = "INSERT INTO " + sql.getLastjoinTableName() + " (uuid, "
				+ HomoSystem.getPluginConfig().serverName
				+ ") VALUES ('" + uuid.toString() + "', " + lastjoin + ") ON DUPLICATE KEY UPDATE "
				+ HomoSystem.getPluginConfig().serverName + "=VALUES(" + HomoSystem.getPluginConfig().serverName + ");";

		boolean success = sql.executeCommand(cmd);
		boolean success2 = sql.executeCommand(moneyCmd);
		boolean success3 = sql.executeCommand(lastJoinCmd);

		return success && success2 && success3;
	}

	public static List<PlayerData> getPlayerDataListBefore30Days() {

		SQLHandler sql = SQLManager.getProtectedSQL();

		String cmd = "select uuid from " + sql.getLastjoinTableName() + " where " + HomoSystem.getPluginConfig().serverName + " > "
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

		List<PlayerData> dataList = new ArrayList<>();
		StringBuilder basicCmd = new StringBuilder("select * from " + sql.getTicketTableName() + " where uuid = ''");

		for (UUID uuid : uuidList) {
			basicCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		basicCmd.append(";");

		Statement stm = sql.createStatement();
		try {

			HashMap<UUID, HashMap<String, BigInteger>> moneyMap = getMoneyMap(uuidList);
			HashMap<UUID, HashMap<String, Long>> lastJoinMap = getLastjoinMap(uuidList);

			ResultSet set = stm.executeQuery(basicCmd.toString());

			UUID uuid;
			String name;
			BigInteger tickets;

			while (set.next()) {
				uuid = UUID.fromString(set.getString("uuid"));
				name = set.getString("name");
				tickets = new BigInteger(set.getString("tickets"));

				PlayerData data = new PlayerData(uuid, name, tickets);

				if (moneyMap.containsKey(uuid)) {
					HashMap<String, BigInteger> moneyServerMap = moneyMap.get(uuid);

					for (String key : moneyServerMap.keySet()) {
						data.setMoney(key, moneyServerMap.get(key));
					}
				}

				if (lastJoinMap.containsKey(uuid)) {
					HashMap<String, Long> lastJoinServerMap = lastJoinMap.get(uuid);

					for (String key : lastJoinServerMap.keySet()) {
						data.setLastJoin(key, lastJoinServerMap.get(key));
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

		Statement stm = sql.createStatement();

		try {

			ResultSet playerDataSet = null;

			if (data.getUuid() != null) {

				String cmd = "select name, tickets from " + sql.getTicketTableName() + " where uuid = '"
						+ data.getUuid().toString() + "'";
				playerDataSet = stm.executeQuery(cmd);
			} else if (data.getName() != null) {

				String playerDataCmd = "SELECT uuid, tickets from " + sql.getTicketTableName()
						+ " where name='"
						+ data.getName() + "' LIMIT 0, 1";
				playerDataSet = stm.executeQuery(playerDataCmd);
			}

			if (playerDataSet.next()) {

				updated = true;

				if (data.getUuid() != null) {
					name = playerDataSet.getString("name");
					tickets = new BigInteger(playerDataSet.getString("tickets"));
				} else if (data.getName() != null) {
					uuid = UUID.fromString(playerDataSet.getString("uuid"));
					tickets = new BigInteger(playerDataSet.getString("tickets"));
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

		HashMap<UUID, HashMap<String, Long>> lastJoinMap = getLastjoinMap(Arrays.asList(data.getUuid()));
		if (lastJoinMap == null || lastJoinMap.size() <= 0)
			return true;

		HashMap<String, Long> lastJoinMapServer = lastJoinMap.get(data.getUuid());
		for (String server : lastJoinMapServer.keySet()) {
			data.setLastJoin(server, lastJoinMapServer.get(server));
		}

		return true;
	}

	private static HashMap<UUID, HashMap<String, BigInteger>> getMoneyMap(List<UUID> uuidList) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		StringBuilder moneyCmd = new StringBuilder("select * from " + sql.getMoneyTableName() + " where uuid = ''");
		for (UUID uuid : uuidList) {
			moneyCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		moneyCmd.append(";");

		List<String> columnList = SQLManager.getColumnsFromMoneyData();

		Statement stm = sql.createStatement();

		HashMap<UUID, HashMap<String, BigInteger>> moneyMap = new HashMap<>();

		try {

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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return moneyMap;
	}

	private static HashMap<UUID, HashMap<String, Long>> getLastjoinMap(List<UUID> uuidList) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		StringBuilder moneyCmd = new StringBuilder("select * from " + sql.getLastjoinTableName() + " where uuid = ''");
		for (UUID uuid : uuidList) {
			moneyCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		moneyCmd.append(";");

		List<String> columnList = SQLManager.getColumnsFromLastjoin();

		Statement stm = sql.createStatement();

		HashMap<UUID, HashMap<String, Long>> moneyMap = new HashMap<>();

		try {

			ResultSet moneySet = stm.executeQuery(moneyCmd.toString());

			while (moneySet.next()) {

				HashMap<String, Long> moneyServerMap = new HashMap<>();

				for (String column : columnList) {
					long value = moneySet.getLong(column);

					moneyServerMap.put(column, value);
				}

				moneyMap.put(UUID.fromString(moneySet.getString("uuid")), moneyServerMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return moneyMap;
	}

	@SuppressWarnings("unused")
	private static HashMap<UUID, Long> getLastjoinMap(List<UUID> uuidList, String serverName) {
		SQLHandler sql = SQLManager.getProtectedSQL();

		if (!SQLManager.getColumnsFromLastjoin().contains(serverName)) {
			throw new IllegalArgumentException(
					"There is no column called '" + serverName + "' in '" + sql.getLastjoinTableName() + "' table.");
		}

		StringBuilder moneyCmd = new StringBuilder("select * from " + sql.getLastjoinTableName() + " where uuid = ''");
		for (UUID uuid : uuidList) {
			moneyCmd.append(" OR uuid = '" + uuid.toString() + "'");
		}
		moneyCmd.append(";");

		Statement stm = sql.createStatement();

		HashMap<UUID, Long> moneyMap = new HashMap<>();

		try {

			ResultSet moneySet = stm.executeQuery(moneyCmd.toString());

			while (moneySet.next()) {

				UUID uuid = UUID.fromString(moneySet.getString("uuid"));
				long value = moneySet.getLong(serverName);

				moneyMap.put(uuid, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return moneyMap;
	}
}
