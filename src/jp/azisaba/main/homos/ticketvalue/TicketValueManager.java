package jp.azisaba.main.homos.ticketvalue;

import java.math.BigInteger;

import org.bukkit.Bukkit;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.TicketValueData;
import jp.azisaba.main.homos.database.SQLHandler;
import jp.azisaba.main.homos.database.SQLManager;
import jp.azisaba.main.homos.events.TicketValueUpdateEvent;

public class TicketValueManager {

	@SuppressWarnings("unused")
	private Homos plugin;

	public TicketValueManager(Homos plugin) {
		this.plugin = plugin;
	}

	public boolean updateTicketValue(BigInteger value, boolean fireEvent) {

		String serverName = Homos.config.serverName;
		if (getCurrentTicketValue().compareTo(value) == 0) {
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
				"INSERT INTO " + sql.getTicketValueTableName() + " (server, value) VALUES ('" + serverName + "', "
						+ value.toString() + ") ON DUPLICATE KEY UPDATE value=VALUES(value);");

		return success;
	}

	public boolean setBoostMode(boolean enable, boolean fireEvent) {
		String serverName = Homos.config.serverName;

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		int num = 0;
		if (enable) {
			num = 1;
		}

		boolean success = sql.executeCommand(
				"INSERT INTO " + sql.getTicketValueTableName() + " (server, boost) VALUES ('" + serverName + "', "
						+ num + ") ON DUPLICATE KEY UPDATE boost=VALUES(boost);");
		return success;
	}

	public boolean lock(boolean lock) {
		String serverName = Homos.config.serverName;

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		int num = 0;
		if (lock) {
			num = 1;
		}

		boolean success = sql.executeCommand(
				"INSERT INTO " + sql.getTicketValueTableName() + " (server, locked) VALUES ('" + serverName + "', "
						+ num + ") ON DUPLICATE KEY UPDATE locked=VALUES(locked);");
		return success;
	}

	public boolean isLocked() {
		return getTicketValueData().isLocked();
	}

	public boolean isBoostMode() {
		return getTicketValueData().isBoosting();
	}

	public synchronized void updateTicketValue() {
		TicketValueUtils.update();
	}

	public BigInteger getCurrentTicketValue() {
		return getTicketValueData().getTicketValue();
	}

	private TicketValueData getTicketValueData() {
		String serverName = Homos.config.serverName;

		@SuppressWarnings("deprecation")
		SQLHandler sql = SQLManager.getSQL();

		return sql.getTicketValueData(serverName);
	}
}
