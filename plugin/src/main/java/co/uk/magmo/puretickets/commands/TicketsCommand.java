package co.uk.magmo.puretickets.commands;

import ai.broccol.corn.core.Lists;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.uk.magmo.puretickets.exceptions.PureException;
import co.uk.magmo.puretickets.locale.MessageNames;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.storage.TimeAmount;
import co.uk.magmo.puretickets.ticket.FutureTicket;
import co.uk.magmo.puretickets.ticket.Ticket;
import co.uk.magmo.puretickets.ticket.TicketStatus;
import co.uk.magmo.puretickets.user.User;
import co.uk.magmo.puretickets.utilities.Constants;
import co.uk.magmo.puretickets.utilities.generic.ReplacementUtilities;
import co.uk.magmo.puretickets.utilities.generic.UserUtilities;
import org.bukkit.OfflinePlayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@CommandAlias("tickets|tis")
@CommandPermission(Constants.STAFF_PERMISSION)
public class TicketsCommand extends PureBaseCommand {
    @Subcommand("%show")
    @CommandCompletion("@TicketHolders @TargetIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".show")
    @Description("Show a ticket")
    @Syntax("<Player> [Index]")
    public void onShow(User user, OfflinePlayer offlinePlayer, @Optional FutureTicket future) {
        processShowCommand(user, future);
    }

    @Subcommand("%pick")
    @CommandCompletion("@TicketHolders:status=OPEN @TargetIds:status=OPEN")
    @CommandPermission(Constants.STAFF_PERMISSION + ".pick")
    @Description("Pick a ticket")
    @Syntax("<Player> [Index]")
    public void onPick(User user, OfflinePlayer offlinePlayer, @Optional @AutoStatuses("OPEN") FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    try {
                        Ticket edited = ticketManager.pick(user.getUniqueId(), ticket);

                        notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.PICK_TICKET, ticket,
                                fields -> fields.put("PICKER", user.getName()));
                    } catch (PureException e) {
                        user.message(e.getMessageKey(), true, e.getReplacements());
                    }
                })
                .execute();
    }

    @Subcommand("%assign")
    @CommandCompletion("@Players @TicketHolders:status=OPEN @TargetIds:parameter=2,status=OPEN")
    @CommandPermission(Constants.STAFF_PERMISSION + ".assign")
    @Description("Assign a ticket to a staff member")
    @Syntax("<TargetPlayer> <Player> [Index]")
    public void onAssign(User user, OfflinePlayer target, OfflinePlayer offlinePlayer, @Optional @AutoStatuses("OPEN") FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    try {
                        Ticket edited = ticketManager.pick(target.getUniqueId(), ticket);

                        notificationManager.send(user, target.getUniqueId(), MessageNames.ASSIGN_TICKET, ticket, fields -> {
                            fields.put("ASSIGNER", user.getName());
                            fields.put("ASSIGNEE", target.getName() == null ? "" : target.getName());
                        });
                    } catch (PureException e) {
                        user.message(e.getMessageKey(), true, e.getReplacements());
                    }
                })
                .execute();
    }

    @Subcommand("%done")
    @CommandCompletion("@TicketHolders:status=PICK @TargetIds:status=PICK")
    @CommandPermission(Constants.STAFF_PERMISSION + ".done")
    @Description("Done-mark a ticket")
    @Syntax("<Player> [Index]")
    public void onDone(User user, OfflinePlayer offlinePlayer, @Optional @AutoStatuses("PICKED") FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    try {
                        Ticket edited = ticketManager.done(user.getUniqueId(), ticket);

                        notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.DONE_TICKET, ticket, null);
                    } catch (PureException e) {
                        user.message(e.getMessageKey(), true, e.getReplacements());
                    }
                })
                .execute();
    }

    @Subcommand("%yield")
    @CommandCompletion("@TicketHolders:status=PICK @TargetIds:status=PICK")
    @CommandPermission(Constants.STAFF_PERMISSION + ".yield")
    @Description("Yield a ticket")
    @Syntax("<Player> [Index]")
    public void onYield(User user, OfflinePlayer offlinePlayer, @Optional @AutoStatuses("PICKED") FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    try {
                        Ticket edited = ticketManager.yield(user.getUniqueId(), ticket);

                        notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.YIELD_TICKET, ticket, null);
                    } catch (PureException e) {
                        user.message(e.getMessageKey(), true, e.getReplacements());
                    }
                })
                .execute();
    }

    @Subcommand("%note")
    @CommandCompletion("@TicketHolders @TargetIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".note")
    @Description("Make a note on a ticket")
    @Syntax("<Player> <Index> <Message>")
    public void onNote(User user, OfflinePlayer offlinePlayer, FutureTicket future, String message) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    Ticket edited = ticketManager.note(user.getUniqueId(), ticket, message);

                    notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.NOTE_TICKET, ticket,
                            fields -> fields.put("NOTE", message)
                    );
                })
                .execute();
    }

    @Subcommand("%reopen")
    @CommandCompletion("@TicketHolders:status=CLOSED @TargetIds:status=CLOSED")
    @CommandPermission(Constants.STAFF_PERMISSION + ".reopen")
    @Description("Reopen a ticket")
    @Syntax("<Player> [Index]")
    public void onReopen(User user, OfflinePlayer offlinePlayer, @Optional @AutoStatuses("CLOSED") FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) -> {
                    try {
                        Ticket edited = ticketManager.reopen(user.getUniqueId(), ticket);

                        notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.REOPEN_TICKET, ticket, null);
                    } catch (PureException e) {
                        user.message(e.getMessageKey(), true, e.getReplacements());
                    }
                })
                .execute();
    }

    @Subcommand("%teleport")
    @CommandCompletion("@TicketHolders @TargetIds:parameter=1")
    @CommandPermission(Constants.STAFF_PERMISSION + ".teleport")
    @Description("Teleport to a ticket creation location")
    @Syntax("<Player> [Index]")
    public void onTeleport(User user, OfflinePlayer offlinePlayer, @Optional FutureTicket future) {
        taskManager.use()
                .future(future)
                .abortIfNull()
                .asyncLast((ticket) ->
                        notificationManager.send(user, offlinePlayer.getUniqueId(), MessageNames.TELEPORT_TICKET, ticket, null)
                )
                .future(future)
                .sync((ticket) -> user.asPlayer().teleport(ticket.getLocation()))
                .execute();
    }

    @Subcommand("%log")
    @CommandCompletion("@TicketHolders @TargetIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".log")
    @Description("Log tickets messages")
    @Syntax("<Player> [Index]")
    public void onLog(User user, OfflinePlayer offlinePlayer, @Optional FutureTicket future) {
        processLogCommand(user, future);
    }

    @Subcommand("%list")
    @CommandCompletion("@TicketHolders @TicketStatus")
    @CommandPermission(Constants.STAFF_PERMISSION + ".list")
    @Description("List all tickets")
    @Syntax("[Player]")
    public void onList(User user, @Optional OfflinePlayer offlinePlayer, @Optional TicketStatus status) {
        taskManager.use()
                .async(() -> {
                    if (offlinePlayer != null) {
                        List<Ticket> tickets = ticketManager.getAll(offlinePlayer.getUniqueId(), status);

                        user.message(Messages.TITLES__SPECIFIC_TICKETS, false, "%player%", offlinePlayer.getName());

                        tickets.forEach((ticket -> {
                            String[] replacements = ReplacementUtilities.ticketReplacements(ticket);
                            user.message(Messages.GENERAL__LIST_FORMAT, false, replacements);
                        }));
                    } else {
                        user.message(Messages.TITLES__ALL_TICKETS, false);

                        Lists.group(ticketManager.all(status), Ticket::getPlayerUUID).forEach((uuid, tickets) -> {
                            user.message(Messages.GENERAL__LIST_HEADER_FORMAT, false, "%name%",
                                    UserUtilities.nameFromUUID(uuid));

                            tickets.forEach(ticket -> {
                                String[] replacements = ReplacementUtilities.ticketReplacements(ticket);
                                user.message(Messages.GENERAL__LIST_FORMAT, false, replacements);
                            });
                        });
                    }
                })
                .execute();
    }

    @Subcommand("%status")
    @CommandCompletion("@TicketHolders")
    @CommandPermission(Constants.STAFF_PERMISSION + ".status")
    @Description("View amount of tickets in")
    @Syntax("[Player]")
    public void onStatus(User user, @Optional OfflinePlayer offlinePlayer) {
        taskManager.use()
                .async(() -> {
                    if (offlinePlayer != null) {
                        user.message(Messages.TITLES__SPECIFIC_STATUS, false, "%player%", offlinePlayer.getName());
                    } else {
                        user.message(Messages.TITLES__TICKET_STATUS, false);
                    }

                    EnumMap<TicketStatus, Integer> data = ticketManager.stats(offlinePlayer != null ? offlinePlayer.getUniqueId() : null);

                    data.forEach((status, amount) -> {
                        if (amount != 0) {
                            user.asSender().sendMessage(amount.toString() + " " + status.name().toLowerCase());
                        }
                    });
                })
                .execute();
    }

    @Subcommand("%highscore")
    @CommandCompletion("@TimeAmounts")
    @CommandPermission(Constants.STAFF_PERMISSION + ".highscore")
    @Description("View highscores of ticket completions")
    public void onHighscore(User user, TimeAmount amount) {
        taskManager.use()
                .async(() -> {
                    HashMap<UUID, Integer> highscores = ticketManager.highscores(amount);
                    user.message(Messages.TITLES__HIGHSCORES, false);

                    highscores.forEach((uuid, number) ->
                            user.message(Messages.GENERAL__HS_FORMAT, false, "%target%", UserUtilities.nameFromUUID(uuid), "%amount%", number.toString())
                    );
                })
                .execute();
    }
}
