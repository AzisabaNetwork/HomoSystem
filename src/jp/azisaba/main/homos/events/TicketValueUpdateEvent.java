package jp.azisaba.main.homos.events;

import java.math.BigInteger;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TicketValueUpdateEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private BigInteger value;

	public TicketValueUpdateEvent(BigInteger value) {
		this.value = value;
	}

	public BigInteger getValue() {
		return value;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private boolean cancel = false;

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
}