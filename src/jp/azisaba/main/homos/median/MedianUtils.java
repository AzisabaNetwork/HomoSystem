package jp.azisaba.main.homos.median;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerDataManager;
import net.milkbowl.vault.economy.Economy;

public class MedianUtils {

	protected static void update() {

		if (Homos.getMedianManager().isLocked()) {
			return;
		}

		boolean boost = false;
		double median = -1d;
		Economy econ = Homos.getEconomy();

		if (econ == null) {
			return;
		}

		List<PlayerData> playerDataList = PlayerDataManager.getPlayerDataListBefore30Days();
		List<Double> moneyList = new ArrayList<>();

		for (PlayerData data : playerDataList) {

			@SuppressWarnings("deprecation")
			boolean hasAccount = econ.hasAccount(data.getName());

			if (!hasAccount) {
				continue;
			}

			@SuppressWarnings("deprecation")
			double value = econ.getBalance(data.getName());

			if (!isTargetValue(value)) {
				continue;
			}

			moneyList.add(value);
		}

		if (moneyList.size() > 10) {
			Collections.sort(moneyList);

			if (moneyList.size() % 2 == 0) {
				double before = moneyList.get(moneyList.size() / 2);
				double after = moneyList.get((moneyList.size() / 2) + 1);

				median = (before + after) / 2;
			} else {
				median = moneyList.get((int) (moneyList.size() / 2 + 0.5));
			}
		} else {
			median = 100000;
			boost = true;
		}

		if (median <= 1000) {
			median = 100000;
			boost = true;
		}

		median = median / 1000;

		if (new BigDecimal(Homos.getMedianManager().getCurrentMedian()).compareTo(BigDecimal.valueOf(median)) == 0) {
			return;
		}

		if (Homos.getMedianManager().isBoostMode()) {
			boost = median < 5000;
		}

		Homos.getMedianManager().updateMedian(BigInteger.valueOf((int) Math.floor(median)), true);
		Homos.getMedianManager().setBoostMode(boost, true);

	}

	private static boolean isTargetValue(double value) {
		if (value <= 500) {
			return false;
		}
		return true;
	}
}
