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
	private HashMap<String, Long> lastJoin = new HashMap<>();

	public PlayerData(UUID uuid, BigInteger tickets) {
		this.uuid = uuid;
		this.tickets = tickets;
	}

	public PlayerData(String name, BigInteger tickets) {
		this.name = name;
		this.tickets = tickets;
	}

	public PlayerData(UUID uuid, String name, BigInteger tickets) {
		this.uuid = uuid;
		this.name = name;
		this.tickets = tickets;
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

	public long getLastJoin(String server) {
		if (lastJoin.containsKey(server)) {
			return lastJoin.get(server);
		}

		return 0L;
	}

	public long getLastJoin() {
		String serverName = Homos.getPluginConfig().serverName;
		return getLastJoin(serverName);
	}

	public void setLastJoin(String server, long value) {
		this.lastJoin.put(server, value);
	}

	public BigInteger getMoney(String server) {

		if (server == null) {

			if (Homos.getPluginConfig().serverName != null) {
				return getMoney(Homos.getPluginConfig().serverName);
			}

			return BigInteger.valueOf(-1);
		}

		if (moneyMap.containsKey(server)) {
			return moneyMap.get(server);
		}

		return BigInteger.valueOf(-1);
	}

	public BigInteger getMoney() {
		String serverName = Homos.getPluginConfig().serverName;
		return getMoney(serverName);
	}

	public void setMoney(String server, BigInteger money) {
		this.moneyMap.put(server, money);
	}
}
