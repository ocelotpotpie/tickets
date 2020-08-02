package co.uk.magmo.puretickets.interactions;

import ai.broccol.corn.spigot.locale.ComposedMessage;
import ai.broccol.corn.spigot.locale.LocaleManager;
import co.uk.magmo.puretickets.configuration.Config;
import co.uk.magmo.puretickets.integrations.DiscordManager;
import co.uk.magmo.puretickets.locale.MessageNames;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.locale.TargetType;
import co.uk.magmo.puretickets.storage.SQLManager;
import co.uk.magmo.puretickets.tasks.ReminderTask;
import co.uk.magmo.puretickets.tasks.TaskManager;
import co.uk.magmo.puretickets.ticket.Ticket;
import co.uk.magmo.puretickets.ticket.TicketManager;
import co.uk.magmo.puretickets.user.User;
import co.uk.magmo.puretickets.user.UserManager;
import co.uk.magmo.puretickets.utilities.Constants;
import co.uk.magmo.puretickets.utilities.generic.ReplacementUtilities;
import co.uk.magmo.puretickets.utilities.generic.TimeUtilities;
import co.uk.magmo.puretickets.utilities.generic.UserUtilities;
import com.google.common.collect.Multimap;
import com.google.common.collect.ObjectArrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class NotificationManager implements Listener {
    private final SQLManager sqlManager;
    private final LocaleManager localeManager;
    private final UserManager userManager;
    private final DiscordManager discordManager;

    private final Multimap<UUID, ComposedMessage> awaiting;

    public NotificationManager(Config config, SQLManager sqlManager, TaskManager taskManager, LocaleManager localeManager,
                               UserManager userManager, DiscordManager discordManager, TicketManager ticketManager) {
        this.sqlManager = sqlManager;
        this.localeManager = localeManager;
        this.userManager = userManager;
        this.discordManager = discordManager;

        awaiting = sqlManager.getNotification().selectAllAndClear();

        taskManager.addRepeatingTask(new ReminderTask(localeManager, ticketManager),
                TimeUtilities.minuteToLong(config.REMINDER__DELAY), TimeUtilities.minuteToLong(config.REMINDER__REPEAT));
    }

    public void send(User user, UUID target, MessageNames names, Ticket ticket, Consumer<HashMap<String, String>> addFields) {
        String[] specificReplacements = {"%user%", user.getName(), "%target%", UserUtilities.nameFromUUID(target)};
        String[] genericReplacements = ReplacementUtilities.ticketReplacements(ticket);

        String[] replacements = ObjectArrays.concat(specificReplacements, genericReplacements, String.class);

        for (TargetType targetType : names.getTargets()) {
            Messages message = Messages.retrieve(targetType, names);

            switch (targetType) {
                case SENDER:
                    user.message(message, true, replacements);
                    break;

                case NOTIFICATION:
                    OfflinePlayer op = Bukkit.getOfflinePlayer(target);

                    if (op.isOnline()) {
                        user.message(message, true, replacements);
                    } else {
                        awaiting.put(target, localeManager.composeMessage(message, replacements));
                    }

                    break;

                case ANNOUNCEMENT:
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!player.hasPermission(Constants.STAFF_PERMISSION + ".announce")) continue;
                        if (!userManager.get(player.getUniqueId()).getAnnouncements()) continue;

                        UUID uuid = user.getUniqueId();

                        if (uuid != null && uuid == player.getUniqueId()) continue;

                        user.message(message, true, replacements);
                    }

                    break;

                case DISCORD:
                    HashMap<String, String> fields = new HashMap<>();

                    if (addFields != null) {
                        addFields.accept(fields);
                    }

                    String action = ChatColor.stripColor(localeManager.valueOf(message));

                    discordManager.sendInformation(ticket.getStatus().getPureColor().getHex(),
                            UserUtilities.nameFromUUID(ticket.getPlayerUUID()), ticket.getPlayerUUID(), ticket.getId(), action, fields);
            }
        }
    }

    public void save() {
        sqlManager.getNotification().insertAll(awaiting);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        if (!awaiting.containsKey(uuid)) {
            return;
        }

        User user = new User(localeManager, uuid);
        awaiting.get(uuid).forEach(n -> user.message(n, true));

        awaiting.removeAll(uuid);
    }
}