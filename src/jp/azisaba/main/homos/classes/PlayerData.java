package jp.azisaba.main.homos.classes;

import java.util.UUID;

public class PlayerData {

	private UUID uuid;
	private String name;
	private int tickets;
	private long money;
	private long lastJoin;

	public PlayerData(UUID uuid, int tickets) {
		this.uuid = uuid;
		this.tickets = tickets;
	}

	public PlayerData(String name, int tickets) {
		this.name = name;
		this.tickets = tickets;
	}

	public PlayerData(UUID uuid, String name, int tickets, long lastjoin) {
		this.uuid = uuid;
		this.name = name;
		this.tickets = tickets;
		this.lastJoin = lastjoin;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTickets() {
		return tickets;
	}

	public void setTickets(int tickets) {
		this.tickets = tickets;
	}

	public long getLastJoin() {
		return lastJoin;
	}

	public void setLastJoin(long lastJoin) {
		this.lastJoin = lastJoin;
	}

	public long getMoney() {
		return money;
	}

	public void setMoney(long money) {
		this.money = money;
	}
}
