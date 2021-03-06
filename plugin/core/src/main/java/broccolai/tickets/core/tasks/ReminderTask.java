package broccolai.tickets.core.tasks;

import broccolai.tickets.core.locale.Message;
import broccolai.tickets.core.ticket.TicketManager;
import broccolai.tickets.core.ticket.TicketStatus;
import broccolai.tickets.core.user.PlayerSoul;
import broccolai.tickets.core.user.UserManager;
import broccolai.tickets.core.utilities.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Template;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ReminderTask implements Runnable {

    private final UserManager<?, ?, ?> userManager;
    private final TicketManager ticketManager;

    /**
     * Create a reminder task
     *
     * @param userManager   User manager
     * @param ticketManager Ticket manager
     */
    public ReminderTask(final @NonNull UserManager<?, ?, ?> userManager, final @NonNull TicketManager ticketManager) {
        this.userManager = userManager;
        this.ticketManager = ticketManager;
    }

    @Override
    public void run() {
        int amount = ticketManager.countTickets(TicketStatus.OPEN);

        if (amount == 0) {
            return;
        }

        for (PlayerSoul<?, ?> soul : userManager.getAllOnlinePlayer()) {
            if (!soul.hasPermission(Constants.STAFF_PERMISSION + ".remind")) {
                continue;
            }

            Component component = Message.FORMAT__REMINDER.use(Template.of("amount", String.valueOf(amount)));
            soul.sendMessage(component);
        }
    }

}
