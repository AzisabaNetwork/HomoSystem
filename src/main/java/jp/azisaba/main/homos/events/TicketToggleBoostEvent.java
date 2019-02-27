package jp.azisaba.main.homos.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketToggleBoostEvent extends Event {

	private boolean enable = false;

	public TicketToggleBoostEvent(boolean enable) {
		this.enable = enable;
	}

	public boolean isBoostModeEnable() {
		return enable;
	}

	private static HandlerList handlerlist = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlerlist;
	}

	@Override
	public HandlerList getHandlers() {
		return handlerlist;
	}
}
