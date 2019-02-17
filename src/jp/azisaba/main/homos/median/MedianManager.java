package jp.azisaba.main.homos.median;

import java.math.BigInteger;

import org.bukkit.Bukkit;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.Median;
import jp.azisaba.main.homos.database.SQLHandler;
import jp.azisaba.main.homos.database.SQLManager;
import jp.azisaba.main.homos.events.TicketValueUpdateEvent;

public class MedianManager {

	@SuppressWarnings("unused")
	private Homos plugin;

	public MedianManager(Homos plugin) {
		this.plugin = plugin;
	}

	public boolean updateMedian(BigInteger value, boolean fireEvent) {

		String serverName = Homos.config.serverName;
		if (serverName.equalsIgnoreCase("unknown")) {
			throw new IllegalStateException("Server name mustn't be \"" + serverName + "\"");
		}

		if (getCurrentMedian().compareTo(value) == 0) {
			return false;
		}

		if (fireEvent) {
			TicketValueUpdateEvent event = new TicketValueUpdateEvent(value);
			Bukkit.getPluginManager().callEvent(event);

			if (event.isCancelled()) {
				return false;
			}
		}

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();
		boolean success = sql.executeCommand(
				"INSERT INTO " + sql.getMedianTableName() + " (server, median) VALUES ('" + serverName + "', "
						+ value.toString() + ") ON DUPLICATE KEY UPDATE median=VALUES(median);");

		return success;
	}

	public boolean setBoostMode(boolean enable, boolean fireEvent) {
		String serverName = Homos.config.serverName;
		if (serverName.equalsIgnoreCase("unknown")) {
			throw new IllegalStateException("Server name mustn't be \"" + serverName + "\"");
		}

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		int num = 0;
		if (enable) {
			num = 1;
		}

		boolean success = sql.executeCommand(
				"INSERT INTO " + sql.getMedianTableName() + " (server, boost) VALUES ('" + serverName + "', "
						+ num + ") ON DUPLICATE KEY UPDATE boost=VALUES(boost);");
		return success;
	}

	public boolean lock(boolean lock) {
		String serverName = Homos.config.serverName;
		if (serverName.equalsIgnoreCase("unknown")) {
			throw new IllegalStateException("Server name mustn't be \"" + serverName + "\"");
		}

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		int num = 0;
		if (lock) {
			num = 1;
		}

		boolean success = sql.executeCommand(
				"INSERT INTO " + sql.getMedianTableName() + " (server, locked) VALUES ('" + serverName + "', "
						+ num + ") ON DUPLICATE KEY UPDATE locked=VALUES(locked);");
		return success;
	}

	public boolean isLocked() {
		return getMedianData().isLocked();
	}

	public boolean isBoostMode() {
		return getMedianData().isBoosting();
	}

	public synchronized void updateMedian() {
		MedianUtils.update();
	}

	public BigInteger getCurrentMedian() {
		return getMedianData().getMedian();
	}

	private Median getMedianData() {
		String serverName = Homos.config.serverName;
		if (serverName.equalsIgnoreCase("unknown")) {
			throw new IllegalStateException("Server name mustn't be \"" + serverName + "\"");
		}

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		return sql.getMedianData(serverName);
	}
}
