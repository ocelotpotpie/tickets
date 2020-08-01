package co.uk.magmo.puretickets.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageType;
import co.aikar.commands.PaperCommandManager;
import co.uk.magmo.corn.core.Lists;
import co.uk.magmo.corn.spigot.locale.LocaleManager;
import co.uk.magmo.corn.spigot.locale.LocaleUtils;
import co.uk.magmo.puretickets.configuration.Config;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.storage.TimeAmount;
import co.uk.magmo.puretickets.ticket.FutureTicket;
import co.uk.magmo.puretickets.ticket.Message;
import co.uk.magmo.puretickets.ticket.MessageReason;
import co.uk.magmo.puretickets.ticket.Ticket;
import co.uk.magmo.puretickets.ticket.TicketManager;
import co.uk.magmo.puretickets.ticket.TicketStatus;
import co.uk.magmo.puretickets.user.User;
import co.uk.magmo.puretickets.utilities.generic.NumberUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandManager extends PaperCommandManager {
    private final LocaleManager localeManager;

    public CommandManager(Plugin plugin, Config config, TicketManager ticketManager) {
        super(plugin);

        Map<Locale, FileConfiguration> locales = LocaleUtils.saveLocales(plugin,
                new File(plugin.getDataFolder(), "locales"), "/locales");

        localeManager = new LocaleManager(locales.get(Locale.ENGLISH), Messages.PREFIX);

        localeManager.registerKeys(Messages.class);

        //noinspection deprecation
        enableUnstableAPI("help");
        getLocales().setDefaultLocale(Locale.forLanguageTag(config.LOCALE));

        // Colours
        setFormat(MessageType.HELP, ChatColor.WHITE, ChatColor.AQUA, ChatColor.DARK_GRAY);
        setFormat(MessageType.INFO, ChatColor.WHITE, ChatColor.AQUA, ChatColor.DARK_GRAY);

        getCommandContexts().registerIssuerOnlyContext(User.class, c -> new User(c.getSender()));

        // Contexts
        getCommandContexts().registerOptionalContext(FutureTicket.class, c -> {
            FutureTicket future = new FutureTicket();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String input = c.popFirstArg();

                if (input != null) {
                    Ticket ticket = ticketManager.get(Integer.parseInt(input));

                    if (ticket == null || (c.hasFlag("issuer") && !ticket.getPlayerUUID().equals(c.getPlayer().getUniqueId()))) {
                        future.complete(null);
                        new User(c.getSender()).message(localeManager.composeMessage(Messages.EXCEPTIONS__TICKET_NOT_FOUND), true);
                        return;
                    }

                    future.complete(ticket);
                    return;
                }

                OfflinePlayer player;

                if (c.hasFlag("issuer")) {
                    player = (OfflinePlayer) c.getResolvedArg("player");
                } else {
                    player = (OfflinePlayer) c.getResolvedArg("offlinePlayer");
                }

                List<TicketStatus> statuses = new ArrayList<>();

                if (c.hasAnnotation(AutoStatuses.class)) {
                    String value = c.getAnnotationValue(AutoStatuses.class);
                    String[] values = value.split(",");

                    for (String s : values) {
                        statuses.add(TicketStatus.valueOf(s));
                    }
                }

                Ticket potentialTicket = ticketManager.getLatestTicket(player.getUniqueId(), statuses.toArray(new TicketStatus[0]));

                if (potentialTicket == null) {
                    future.complete(null);
                    new User(c.getSender()).message(localeManager.composeMessage(Messages.EXCEPTIONS__TICKET_NOT_FOUND), true);
                    return;
                }

                future.complete(potentialTicket);
            });

            return future;
        });

        getCommandContexts().registerContext(Message.class, c ->
                new Message(MessageReason.MESSAGE, LocalDateTime.now(), c.joinArgs())
        );

        getCommandContexts().registerContext(TicketStatus.class, c ->
                TicketStatus.from(c.popFirstArg())
        );

        getCommandContexts().registerContext(TimeAmount.class, c -> {
            try {
                return TimeAmount.valueOf(c.popFirstArg().toUpperCase());
            } catch (Exception e) {
                throw new InvalidCommandArgument();
            }
        });

        // Replacements
        getCommandReplacements().addReplacement("create", config.ALIAS__CREATE);
        getCommandReplacements().addReplacement("update", config.ALIAS__UPDATE);
        getCommandReplacements().addReplacement("close", config.ALIAS__CLOSE);
        getCommandReplacements().addReplacement("show", config.ALIAS__SHOW);
        getCommandReplacements().addReplacement("pick", config.ALIAS__PICK);
        getCommandReplacements().addReplacement("assign", config.ALIAS__ASSIGN);
        getCommandReplacements().addReplacement("done", config.ALIAS__DONE);
        getCommandReplacements().addReplacement("yield", config.ALIAS__YIELD);
        getCommandReplacements().addReplacement("note", config.ALIAS__NOTE);
        getCommandReplacements().addReplacement("reopen", config.ALIAS__REOPEN);
        getCommandReplacements().addReplacement("teleport", config.ALIAS__TELEPORT);
        getCommandReplacements().addReplacement("log", config.ALIAS__LOG);
        getCommandReplacements().addReplacement("list", config.ALIAS__LIST);
        getCommandReplacements().addReplacement("status", config.ALIAS__STATUS);
        getCommandReplacements().addReplacement("highscore", config.ALIAS__HIGHSCORE);
    }

    public void registerCompletions(TicketManager ticketManager) {
        getCommandCompletions().registerAsyncCompletion("TicketHolders", c ->
                ticketManager.allNames(TicketStatus.from(c.getConfig("status")))
        );

        getCommandCompletions().registerAsyncCompletion("TargetIds", c -> {
            try {
                OfflinePlayer target = c.getContextValue(OfflinePlayer.class, NumberUtilities.valueOfOrNull(c.getConfig("parameter")));
                TicketStatus status = TicketStatus.from(c.getConfig("status"));

                return Lists.map(ticketManager.getIds(target.getUniqueId(), status), Object::toString);
            } catch (Exception e) {
                return null;
            }
        });

        getCommandCompletions().registerAsyncCompletion("IssuerIds", c -> {
            try {
                TicketStatus status = TicketStatus.from(c.getConfig("status"));

                return Lists.map(ticketManager.getIds(c.getIssuer().getUniqueId(), status), Object::toString);
            } catch (Exception e) {
                return null;
            }
        });

        getCommandCompletions().registerStaticCompletion("TicketStatus", Lists.map(Arrays.asList(TicketStatus.values()), value ->
                value.name().toLowerCase()
        ));

        getCommandCompletions().registerStaticCompletion("TimeAmounts", Lists.map(Arrays.asList(TimeAmount.values()), value ->
                value.name().toLowerCase()
        ));
    }

    public void registerInjections(Object... inputs) {
        for (Object input : inputs) {
            registerDependency(input.getClass(), input);
        }
    }

    public void registerCommands() {
        registerCommand(new TicketCommand());
        registerCommand(new TicketsCommand());
        registerCommand(new PureTicketsCommand());
    }
}