package jp.azisaba.main.homos.ticketvalue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerDataManager;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class TicketValueUtils {

	protected static void update() {

		if (Homos.getTicketValueManager().isLocked()) {
			return;
		}

		boolean boost = false;
		BigDecimal median = BigDecimal.valueOf(-1);
		Economy econ = Homos.getEconomy();

		if (econ == null) {
			return;
		}

		List<PlayerData> playerDataList = PlayerDataManager.getPlayerDataListBefore30Days();
		List<BigDecimal> moneyList = new ArrayList<>();

		for (PlayerData data : playerDataList) {

			@SuppressWarnings("deprecation")
			boolean hasAccount = econ.hasAccount(data.getName());

			if (!hasAccount) {
				continue;
			}

			@SuppressWarnings("deprecation")
			double value = econ.getBalance(data.getName());

			BigDecimal money = BigDecimal.valueOf(value);
			BigInteger ticketMoney = data.getMoney();

			BigDecimal total = money.add(new BigDecimal(ticketMoney));

			if (!isTargetValue(total)) {
				continue;
			}

			moneyList.add(total);
		}

		if (moneyList.size() > 10) {
			Collections.sort(moneyList, Collections.reverseOrder());

			if (moneyList.size() % 2 == 0) {
				BigDecimal before = moneyList.get(moneyList.size() / 2);
				BigDecimal after = moneyList.get((moneyList.size() / 2) + 1);

				median = (before.add(after)).divide(BigDecimal.valueOf(2));
			} else {
				median = moneyList.get((int) (moneyList.size() / 2 + 0.5));
			}
		} else {
			median = BigDecimal.valueOf(100000);
			boost = true;
		}

		if (median.compareTo(BigDecimal.valueOf(1000)) <= 0) {
			median = BigDecimal.valueOf(100000);
			boost = true;
		}

		BigDecimal ticketValue = median.divide(BigDecimal.valueOf(1000));

		if (new BigDecimal(Homos.getTicketValueManager().getCurrentTicketValue())
				.compareTo(ticketValue) == 0) {
			return;
		}

		if (Homos.getTicketValueManager().isBoostMode()) {
			boost = ticketValue.compareTo(BigDecimal.valueOf(5000)) < 0;
		}

		Homos.getTicketValueManager().updateTicketValue(ticketValue.setScale(0, BigDecimal.ROUND_DOWN).toBigInteger(),
				true);
		Homos.getTicketValueManager().setBoostMode(boost, true);

	}

	public static Entry<String, BigDecimal> getMedianPlayer() {
		String player = ChatColor.GRAY + "System";
		BigDecimal median = BigDecimal.valueOf(-1);
		Economy econ = Homos.getEconomy();

		if (econ == null) {
			return null;
		}

		List<PlayerData> playerDataList = PlayerDataManager.getPlayerDataListBefore30Days();
		HashMap<String, BigDecimal> moneyMap = new HashMap<String, BigDecimal>();

		for (PlayerData data : playerDataList) {

			@SuppressWarnings("deprecation")
			boolean hasAccount = econ.hasAccount(data.getName());

			if (!hasAccount) {
				continue;
			}

			@SuppressWarnings("deprecation")
			double value = econ.getBalance(data.getName());

			BigDecimal money = BigDecimal.valueOf(value);
			BigInteger ticketMoney = data.getMoney();

			BigDecimal total = money.add(new BigDecimal(ticketMoney));

			if (!isTargetValue(total)) {
				continue;
			}

			moneyMap.put(data.getName(), total);
		}

		List<Entry<String, BigDecimal>> entryList = new ArrayList<Entry<String, BigDecimal>>(moneyMap.entrySet());

		Collections.sort(entryList, new Comparator<Entry<String, BigDecimal>>() {
			public int compare(Entry<String, BigDecimal> obj1, Entry<String, BigDecimal> obj2) {
				// 4. 昇順
				return obj2.getValue().compareTo(obj1.getValue());
			}
		});

		if (entryList.size() > 10) {

			if (entryList.size() % 2 == 0) {
				Entry<String, BigDecimal> before = entryList.get(entryList.size() / 2);
				Entry<String, BigDecimal> after = entryList.get(entryList.size() / 2 + 1);

				median = before.getValue().add(after.getValue()).divide(BigDecimal.valueOf(2), 1,
						BigDecimal.ROUND_HALF_DOWN);
				player = before.getKey() + "," + after.getKey();

				Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
				map.put(player, median);
				return map.entrySet().iterator().next();
			} else {
				Entry<String, BigDecimal> v = entryList.get((int) (entryList.size() / 2 + 0.5));
				return v;
			}
		} else {
			median = BigDecimal.valueOf(100000);
		}

		if (median.compareTo(BigDecimal.valueOf(1000)) <= 0) {
			median = BigDecimal.valueOf(100000);
		}

		Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
		map.put(player, median);
		return map.entrySet().iterator().next();
	}

	private static boolean isTargetValue(BigDecimal value) {
		if (value.compareTo(BigDecimal.valueOf(500)) <= 0) {
			return false;
		}
		return true;
	}
}
