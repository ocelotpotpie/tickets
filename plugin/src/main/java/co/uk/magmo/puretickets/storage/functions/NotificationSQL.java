package co.uk.magmo.puretickets.storage.functions;

import ai.broccol.corn.spigot.locale.ComposedMessage;
import ai.broccol.corn.spigot.locale.LocaleManager;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.uk.magmo.puretickets.locale.Messages;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class NotificationSQL {
    private final HelpersSQL helpers;
    private final LocaleManager localeManager;

    public NotificationSQL(HelpersSQL helpers, LocaleManager localeManager) {
        this.helpers = helpers;
        this.localeManager = localeManager;
    }

    public Multimap<UUID, ComposedMessage> selectAllAndClear() {
        Multimap<UUID, ComposedMessage> output = ArrayListMultimap.create();
        List<DbRow> results;

        try {
            results = DB.getResults("SELECT uuid, message, replacements from puretickets_notification");
            DB.executeUpdate("DELETE from puretickets_notification");
        } catch (SQLException e) {
            return output;
        }

        for (DbRow result : results) {
            UUID uuid = helpers.getUUID(result, "uuid");

            Messages key = helpers.getEnumValue(result, Messages.class, "message");
            String[] replacements = result.getString("replacements").split("\\|");

            ComposedMessage message = localeManager.composeMessage(key, replacements);

            output.put(uuid, message);
        }

        return output;
    }

    public void insertAll(Multimap<UUID, ComposedMessage> notifications) {
        notifications.forEach(((uuid, notification) -> {
            String message = notification.getKey().getName();
            String replacements = String.join("|", notification.getReplacements());

            try {
                DB.executeInsert("INSERT INTO puretickets_notification(uuid, message, replacements) VALUES(?, ?, ?)",
                        uuid.toString(), message, replacements);
            } catch (SQLException e) {
                Bukkit.getLogger().warning("Failed to insert notification " + message);
            }
        }));
    }
}

