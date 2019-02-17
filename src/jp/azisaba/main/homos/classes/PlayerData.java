package jp.azisaba.main.homos.classes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.UUID;

import jp.azisaba.main.homos.Homos;

public class PlayerData {

	private UUID uuid;
	private String name;
	private BigInteger tickets;
	private HashMap<String, BigInteger> moneyMap = new HashMap<>();
	private long lastJoin;

	public PlayerData(UUID uuid, BigInteger tickets) {
		this.uuid = uuid;
		this.tickets = tickets;
	}

	public PlayerData(String name, BigInteger tickets) {
		this.name = name;
		this.tickets = tickets;
	}

	public PlayerData(UUID uuid, String name, BigInteger tickets, long lastjoin) {
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

	public BigInteger getTickets() {
		return tickets;
	}

	public void setTickets(BigInteger tickets) {
		this.tickets = tickets;
	}

	public long getLastJoin() {
		return lastJoin;
	}

	public void setLastJoin(long lastJoin) {
		this.lastJoin = lastJoin;
	}

	public BigInteger getMoney(String server) {

		if (server == null) {

			if (Homos.config.serverName != null) {
				return getMoney(Homos.config.serverName);
			}

			return BigInteger.valueOf(-1);
		}

		if (moneyMap.containsKey(server)) {
			return moneyMap.get(server);
		}

		return BigInteger.valueOf(-1);
	}

	public BigInteger getMoney() {
		String serverName = Homos.config.serverName;
		return getMoney(serverName);
	}

	public void setMoney(String server, BigInteger money) {
		this.moneyMap.put(server, money);
	}
}
