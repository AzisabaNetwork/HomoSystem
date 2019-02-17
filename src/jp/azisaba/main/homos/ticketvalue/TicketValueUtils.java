package jp.azisaba.main.homos.ticketvalue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerDataManager;
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

	private static boolean isTargetValue(BigDecimal value) {
		if (value.compareTo(BigDecimal.valueOf(500)) <= 0) {
			return false;
		}
		return true;
	}
}
