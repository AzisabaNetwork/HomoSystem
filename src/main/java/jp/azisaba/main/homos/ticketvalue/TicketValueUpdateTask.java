package jp.azisaba.main.homos.ticketvalue;

import org.bukkit.scheduler.BukkitRunnable;

public class TicketValueUpdateTask extends BukkitRunnable {

    @Override
    public void run() {
        TicketValueUtils.update();
    }
}
