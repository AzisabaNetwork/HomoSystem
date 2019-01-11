package jp.azisaba.main.homos.median;

import org.bukkit.Bukkit;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.events.TicketToggleBoostEvent;
import jp.azisaba.main.homos.events.TicketValueUpdateEvent;

public class MedianManager {

	@SuppressWarnings("unused")
	private HOMOs plugin;

	private int currentMedian = -1;
	private boolean boostMode = false;

	private boolean lock = false;

	public MedianManager(HOMOs plugin) {
		this.plugin = plugin;
	}

	public boolean updateMedian(int value, boolean fireEvent) {

		if (fireEvent) {
			TicketValueUpdateEvent event = new TicketValueUpdateEvent(value);
			Bukkit.getPluginManager().callEvent(event);

			if (event.isCancelled()) {
				return false;
			}
		}

		this.currentMedian = value;
		return true;
	}

	public void setBoostMode(boolean enable, boolean fireEvent) {

		if (enable == boostMode) {
			return;
		}

		this.boostMode = enable;

		if (fireEvent) {
			TicketToggleBoostEvent event = new TicketToggleBoostEvent(enable);
			Bukkit.getPluginManager().callEvent(event);
		}
	}

	public void lock(boolean lock) {
		this.lock = lock;
	}

	public boolean isLocked() {
		return lock;
	}

	public boolean isBoostMode() {
		return boostMode;
	}

	public void updateMedian() {
		MedianUtils.update();
	}

	public int getCurrentMedian() {
		return currentMedian;
	}
}
