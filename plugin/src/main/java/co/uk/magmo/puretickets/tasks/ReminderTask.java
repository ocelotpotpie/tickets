package co.uk.magmo.puretickets.tasks;

import ai.broccol.corn.spigot.locale.LocaleManager;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.ticket.TicketManager;
import co.uk.magmo.puretickets.ticket.TicketStatus;
import co.uk.magmo.puretickets.user.User;
import co.uk.magmo.puretickets.utilities.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ReminderTask extends BukkitRunnable {
    private final LocaleManager localeManager;
    private final TicketManager ticketManager;

    public ReminderTask(LocaleManager localeManager, TicketManager ticketManager) {
        this.localeManager = localeManager;
        this.ticketManager = ticketManager;
    }

    @Override
    public void run() {
        Integer amount = ticketManager.count(TicketStatus.OPEN);

        if (amount == 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(Constants.STAFF_PERMISSION + ".remind")) {
                continue;
            }

            User user = new User(localeManager, player);
            user.message(Messages.OTHER__REMINDER, true, "%amount%", amount.toString());
        }
    }
}