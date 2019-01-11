package jp.azisaba.main.homos.median;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import jp.azisaba.main.homos.HOMOs;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerTicketManager;
import net.milkbowl.vault.economy.Economy;

public class MedianUtils {

	private static Economy econ = null;

	protected static void update() {

		if (HOMOs.getMedianManager().isLocked()) {
			return;
		}

		boolean boost = false;

		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			Bukkit.getLogger()
					.warning(
							"[HOMOs] このサーバーにはVaultが入っていないため所持金が取得できません。このサーバーに通常経済を導入しない場合はconfigからUpdateMedianを無効化してください。");
			return;
		}

		if (econ == null) {
			setupEconomy();

			if (econ == null) {
				return;
			}
		}

		double median = -1d;

		List<PlayerData> playerDataList = PlayerTicketManager.getPlayerDataListBefore30Days();
		List<Double> moneyList = new ArrayList<>();

		for (PlayerData data : playerDataList) {

			OfflinePlayer p = Bukkit.getOfflinePlayer(data.getUuid());

			if (!econ.hasAccount(p)) {
				continue;
			}

			double value = econ.getBalance(p);

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

			median = median / 1000;
		} else {
			median = 100000;
			boost = true;
		}

		if (median <= 1000) {
			median = 100000;
			boost = true;
		}

		if (HOMOs.getMedianManager().getCurrentMedian() == median) {
			return;
		}

		if (HOMOs.getMedianManager().isBoostMode()) {
			boost = median < 5000;
		}

		HOMOs.getMedianManager().updateMedian((int) Math.floor(median), true);
		HOMOs.getMedianManager().setBoostMode(boost, true);

	}

	private static boolean isTargetValue(double value) {
		if (value <= 500) {
			return false;
		}
		return true;
	}

	private static boolean setupEconomy() {
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
}
