package co.uk.magmo.puretickets.storage.functions;

import co.aikar.idb.DbRow;
import co.uk.magmo.puretickets.storage.platforms.Platform;
import co.uk.magmo.puretickets.ticket.Message;
import co.uk.magmo.puretickets.ticket.MessageReason;
import co.uk.magmo.puretickets.ticket.Ticket;
import co.uk.magmo.puretickets.ticket.TicketStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class HelpersSQL {
    private Platform platform;
    private MessageSQL messageSQL;

    public void setup(Platform platform, MessageSQL messageSQL) {
        this.platform = platform;
        this.messageSQL = messageSQL;
    }

    @Nullable UUID getUUID(DbRow row, String column) {
        String raw = row.getString(column);

        if (raw == null || raw.equals("null")) {
            return null;
        } else {
            return UUID.fromString(raw);
        }
    }

    Location getLocation(DbRow row, String column) {
        String raw = row.getString(column);
        String[] split = raw.split("\\|");
        World world = Bukkit.getWorld(split[0]);

        return new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }

    String serializeLocation(Location location) {
        return location.getWorld().getName() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
    }

    LocalDateTime getDate(DbRow row, String column) {
        Instant instant = Instant.ofEpochSecond(platform.getPureLong(row, column));

        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    Long serializeLocalDateTime(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    <T extends Enum<T>> T getEnumValue(DbRow row, Class<T> clazz, String column) {
        T[] enumConstants = clazz.getEnumConstants();
        String raw = row.getString(column);

        for (T enumConstant : enumConstants) {
            if (raw.equals(enumConstant.name())) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException();
    }

    Ticket buildTicket(DbRow row) {
        Integer id = row.getInt("id");
        UUID player = getUUID(row, "uuid");
        List<Message> messages = messageSQL.selectAll(row.getInt("id"));
        TicketStatus status = getEnumValue(row, TicketStatus.class, "status");
        Location location = getLocation(row, "location");
        UUID picker = getUUID(row, "picker");

        return new Ticket(id, player, messages, location, status, picker);
    }

    Message buildMessage(DbRow row) {
        MessageReason reason = getEnumValue(row, MessageReason.class, "reason");
        LocalDateTime date = getDate(row, "date");

        String data = row.getString("data");
        UUID sender = getUUID(row, "sender");

        return new Message(reason, date, data, sender);
    }
}
