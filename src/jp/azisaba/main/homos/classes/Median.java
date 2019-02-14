package jp.azisaba.main.homos.classes;

public class Median {

	private String serverName;
	private int median;
	private boolean locked;
	private boolean boost;

	public Median(String serverName, int median, boolean locked, boolean boost) {
		this.serverName = serverName;
		this.median = median;
		this.locked = locked;
		this.boost = boost;
	}

	public String getServerName() {
		return serverName;
	}

	public int getMedian() {
		return median;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isBoosting() {
		return boost;
	}
}
