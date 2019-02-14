package jp.azisaba.main.homos.median;

import org.bukkit.scheduler.BukkitRunnable;

public class MedianUpdateTask extends BukkitRunnable {

	@Override
	public void run() {
		MedianUtils.update();
	}
}
