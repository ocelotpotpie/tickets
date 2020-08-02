package co.uk.magmo.puretickets.commands;

import ai.broccol.corn.spigot.locale.LocaleManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.HelpCommand;
import co.uk.magmo.puretickets.configuration.Config;
import co.uk.magmo.puretickets.interactions.NotificationManager;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.tasks.TaskManager;
import co.uk.magmo.puretickets.ticket.FutureTicket;
import co.uk.magmo.puretickets.ticket.TicketManager;
import co.uk.magmo.puretickets.ticket.TicketStatus;
import co.uk.magmo.puretickets.user.User;
import co.uk.magmo.puretickets.utilities.generic.ReplacementUtilities;
import co.uk.magmo.puretickets.utilities.generic.TimeUtilities;
import co.uk.magmo.puretickets.utilities.generic.UserUtilities;
import org.bukkit.command.CommandSender;

public class PureBaseCommand extends BaseCommand {
    @Dependency
    protected Config config;

    @Dependency
    protected NotificationManager notificationManager;

    @Dependency
    protected TicketManager ticketManager;

    @Dependency
    protected TaskManager taskManager;

    @Dependency
    protected LocaleManager localeManager;

    @Default
    @HelpCommand
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.getHelpEntries().remove(0);
        help.showHelp();
    }

    protected void processShowCommand(User user, FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    String[] replacements = ReplacementUtilities.ticketReplacements(ticket);
                    localeManager.composeMessage(Messages.TITLES__SHOW_TICKET, replacements);

                    user.message(Messages.TITLES__SHOW_TICKET, false, replacements);
                    user.message(Messages.SHOW__SENDER, false, replacements);
                    user.message(Messages.SHOW__MESSAGE, false, replacements);

                    if (ticket.getStatus() != TicketStatus.PICKED) {
                        user.message(Messages.SHOW__UNPICKED, false);
                    } else {
                        user.message(Messages.SHOW__PICKER, false, replacements);
                    }
                })
                .execute();
    }

    protected void processLogCommand(User user, FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    String[] replacements = ReplacementUtilities.ticketReplacements(ticket);

                    user.message(Messages.TITLES__TICKET_LOG, false, replacements);

                    ticket.getMessages().forEach(message -> {
                        String suffix = message.getData() != null ? message.getData() : UserUtilities.nameFromUUID(message.getSender());

                        user.message(Messages.GENERAL__LOG_FORMAT, false, "%reason%", message.getReason().name(),
                                "%date%", TimeUtilities.formatted(message.getDate()), "%suffix%", suffix);
                    });
                })
                .execute();
    }
}
