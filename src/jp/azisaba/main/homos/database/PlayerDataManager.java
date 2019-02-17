package jp.azisaba.main.homos.database;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

		boolean success = sql.executeCommand(cmd);

		return success;
	}

	public static List<PlayerData> getPlayerDataListBefore30Days() {

		SQLHandler sql = SQLManager.getProtectedSQL();

		List<PlayerData> playerDataList = new ArrayList<>();
		String cmd = "select * from " + sql.getTicketTableName() + " where lastjoin > "
				+ (System.currentTimeMillis() - (1000L/**millis*/
						* 60L/**seconds*/
						* 60L/**minutes*/
						* 24L/**hours*/
						* 30L/**days*/
				));

		Statement stm = sql.createStatement();

		try {

			ResultSet set = stm.executeQuery(cmd);

			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));

				PlayerData data = new PlayerData(uuid, BigInteger.ZERO);
				setUpPlayerData(data);

				playerDataList.add(data);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			SQLHandler.closeStatement(stm);
		}

		return playerDataList;
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

			String moneyDataCmd = "select (" + String.join(", ", SQLManager.getColumnsFromTicketValueData()) + ") from "
					+ sql.getMoneyTableName() + " where uuid='" + data.getUuid().toString() + "';";
			ResultSet moneyDataSet = stm.executeQuery(moneyDataCmd);

			if (moneyDataSet.next()) {

				List<String> columnList = SQLManager.getColumnsFromTicketValueData();

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
