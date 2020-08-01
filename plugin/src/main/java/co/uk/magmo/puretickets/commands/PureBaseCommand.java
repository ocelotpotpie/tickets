package co.uk.magmo.puretickets.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.HelpCommand;
import co.uk.magmo.corn.spigot.locale.LocaleManager;
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

                    user.message(Messages.TITLES__SHOW_TICKET, replacements);
                    issuer.sendInfo(Messages.SHOW__SENDER, replacements);
                    issuer.sendInfo(Messages.SHOW__MESSAGE, replacements);

                    if (ticket.getStatus() != TicketStatus.PICKED) {
                        issuer.sendInfo(Messages.SHOW__UNPICKED);
                    } else {
                        issuer.sendInfo(Messages.SHOW__PICKER, replacements);
                    }
                })
                .execute();
    }

    protected void processLogCommand(CommandIssuer issuer, FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    String[] replacements = ReplacementUtilities.ticketReplacements(ticket);

                    issuer.sendInfo(Messages.TITLES__TICKET_LOG, replacements);

                    ticket.getMessages().forEach(message -> {
                        String suffix = message.getData() != null ? message.getData() : UserUtilities.nameFromUUID(message.getSender());

                        issuer.sendInfo(Messages.GENERAL__LOG_FORMAT, "%reason%", message.getReason().name(),
                                "%date%", TimeUtilities.formatted(message.getDate()), "%suffix%", suffix);
                    });
                })
                .execute();
    }
}
