package jp.azisaba.main.homos.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import jp.azisaba.main.homos.classes.PlayerData;

public class PlayerDataManager {

	public static PlayerData getPlayerData(Player p) {
		PlayerData data = new PlayerData(p.getUniqueId(), 0);

		setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(UUID uuid) {
		PlayerData data = new PlayerData(uuid, 0);

		setUpPlayerData(data);
		return data;
	}

	public static PlayerData getPlayerData(String name) {
		PlayerData data = new PlayerData(name, 0);

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

		ResultSet set = sql.executeQuery(cmd);

		try {

			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				String name = set.getString("name");
				int tickets = set.getInt("tickets");
				long lastjoin = set.getLong("lastjoin");

				PlayerData data = new PlayerData(uuid, name, tickets, lastjoin);

				playerDataList.add(data);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return playerDataList;
	}

	public static boolean setUpPlayerData(PlayerData data) {

		SQLHandler sql = SQLManager.getProtectedSQL();

		boolean updated = false;

		UUID uuid = null;
		String name = null;
		int tickets = -1;
		long lastjoin = -1;

		try {

			ResultSet playerDataSet = null;

			if (data.getUuid() != null) {
				playerDataSet = sql
						.executeQuery(
								"select name, tickets, lastjoin from " + sql.getTicketTableName() + " where uuid = '"
										+ data.getUuid().toString() + "'");
			} else if (data.getName() != null) {

				String playerDataCmd = "SELECT (uuid, tickets, lastjoin) from " + sql.getTicketTableName()
						+ " where name='"
						+ data.getName() + "' LIMIT 0, 1";
				playerDataSet = sql.executeQuery(playerDataCmd);
			}

			if (playerDataSet.next()) {

				updated = true;

				if (data.getUuid() != null) {
					name = playerDataSet.getString("name");
					tickets = playerDataSet.getInt("tickets");
					lastjoin = playerDataSet.getLong("lastjoin");
				} else if (data.getName() != null) {
					uuid = UUID.fromString(playerDataSet.getString("uuid"));
					tickets = playerDataSet.getInt("tickets");
					lastjoin = playerDataSet.getLong("lastjoin");
				}
			}

			String moneyDataCmd = "select (" + String.join(", ", SQLManager.getColumnsFromMedianData()) + ") from "
					+ sql.getMoneyTableName() + " where uuid='" + data.getUuid().toString() + "';";
			ResultSet moneyDataSet = sql.executeQuery(moneyDataCmd);

			if (moneyDataSet.next()) {

				List<String> columnList = SQLManager.getColumnsFromMedianData();

				for (String str : columnList) {
					long value = moneyDataSet.getLong(str);

					if (value > 0) {
						data.setMoney(str, value);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
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
