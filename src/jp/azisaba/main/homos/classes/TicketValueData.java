package jp.azisaba.main.homos.classes;

import java.math.BigInteger;

public class TicketValueData {

	private String serverName;
	private BigInteger median;
	private boolean locked;
	private boolean boost;

	public TicketValueData(String serverName, BigInteger median, boolean locked, boolean boost) {
		this.serverName = serverName;
		this.median = median;
		this.locked = locked;
		this.boost = boost;
	}

	public String getServerName() {
		return serverName;
	}

	public BigInteger getTicketValue() {
		return median;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isBoosting() {
		return boost;
	}
}
