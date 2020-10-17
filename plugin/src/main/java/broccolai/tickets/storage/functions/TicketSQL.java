package broccolai.tickets.storage.functions;

import broccolai.tickets.storage.TimeAmount;
import broccolai.tickets.storage.platforms.Platform;
import broccolai.tickets.ticket.Ticket;
import broccolai.tickets.ticket.TicketStatus;
import broccolai.tickets.utilities.UserUtilities;
import cloud.commandframework.types.tuples.Pair;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.google.common.collect.ObjectArrays;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TicketSQL {

    private static Platform platform;

    private TicketSQL() {
    }

    /**
     * Initialise TicketSQL
     *
     * @param platformInstance Platform instance
     */
    public static void setup(@NonNull final Platform platformInstance) {
        platform = platformInstance;
    }

    /**
     * Retrieve a Ticket from the Database
     *
     * @param id Tickets id
     * @return Constructed ticket
     */
    @Nullable
    public static Ticket select(final int id) {
        DbRow row;

        try {
            row = DB.getFirstRow("SELECT id, uuid, status, picker, location from puretickets_ticket WHERE id = ?", id);
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        return row != null ? HelpersSQL.buildTicket(row) : null;
    }

    /**
     * Retrieve a Ticket from the Database
     *
     * @param id   Ticket id
     * @param uuid User unique id
     * @return Constructed ticket
     */
    @Nullable
    public static Ticket select(final int id, @NonNull final UUID uuid) {
        DbRow row;

        try {
            row = DB.getFirstRow(
                    "SELECT id, uuid, status, picker, location from puretickets_ticket WHERE id = ? AND uuid = ?",
                    id,
                    uuid
            );
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        return row != null ? HelpersSQL.buildTicket(row) : null;
    }

    /**
     * Retrieves tickets with the given optional status
     *
     * @param status Optional status to filter with
     * @return List of the retrieved tickets
     */
    @NonNull
    public static List<Ticket> selectAll(@Nullable final TicketStatus status) {
        List<DbRow> results;

        try {
            if (status == null) {
                results = DB.getResults(
                        "SELECT id, uuid, status, picker, location FROM puretickets_ticket WHERE status <> ?",
                        TicketStatus.CLOSED.name()
                );
            } else {
                results = DB.getResults(
                        "SELECT id, uuid, status, picker, location FROM puretickets_ticket WHERE status = ?",
                        status.name()
                );
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        return results
                .stream()
                .map(HelpersSQL::buildTicket)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves tickets with a given players unique id and an optional status
     *
     * @param uuid   Players unique id to filter with
     * @param status Optional status to filter with
     * @return List of the retrieved tickets
     */
    @NonNull
    public static List<Ticket> selectAll(@NonNull final UUID uuid, @Nullable final TicketStatus status) {
        List<DbRow> results;

        String sql = "SELECT id, uuid, status, picker, location from puretickets_ticket WHERE uuid = ?";

        try {
            if (status == null) {
                results = DB.getResults(sql + " AND status <> ?", uuid.toString(), TicketStatus.CLOSED.name());
            } else {
                results = DB.getResults(sql + " AND status = ?", uuid.toString(), status.name());
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        return results
                .stream()
                .map(HelpersSQL::buildTicket)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves ticket ids with a given players unique id and an optional status
     *
     * @param uuid     Players unique id to filter with
     * @param statuses Optional status to filter with
     * @return List of the retrieved tickets
     */
    @NonNull
    public static List<Integer> selectIds(@NonNull final UUID uuid, @Nullable final TicketStatus... statuses) {
        String sql = "SELECT id from puretickets_ticket WHERE uuid = ?";
        Pair<String, Object[]> extensions = buildWhereExtension(true, statuses);
        Object[] replacements = ObjectArrays.concat(uuid.toString(), extensions.getSecond());

        try {
            return DB.getFirstColumnResults(sql + extensions.getFirst(), replacements);
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Retrieve the last ticket with a players unique id and optionally multiple statues
     *
     * @param uuid     Players unique id to filter with
     * @param statuses Optional statuses to filter with
     * @return Most recent ticket or if non are eligible null
     */
    @Nullable
    public static Ticket selectLastTicket(@NonNull final UUID uuid, @Nullable final TicketStatus... statuses) {
        String sql = "SELECT max(id) AS 'id', uuid, status, picker, location FROM puretickets_ticket WHERE uuid = ?";
        Pair<String, Object[]> extensions = buildWhereExtension(true, statuses);
        Object[] replacements = ObjectArrays.concat(uuid.toString(), extensions.getSecond());

        try {
            DbRow row = DB.getFirstRow(sql + extensions.getFirst(), replacements);
            return row.get("id") == null ? null : HelpersSQL.buildTicket(row);
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Retrieves the names of all ticket holders, optionally filtered by a status
     *
     * @param statuses Optional statuses to filter by
     * @return List of player names
     */
    @NonNull
    public static Set<String> selectNames(@Nullable final TicketStatus... statuses) {
        List<String> results;

        String sql = "SELECT uuid from puretickets_ticket";
        Pair<String, Object[]> extensions = buildWhereExtension(false, statuses);

        try {
            results = DB.getFirstColumnResults(sql + extensions.getFirst(), extensions.getSecond());
        } catch (final SQLException e) {
            throw new IllegalArgumentException();
        }

        return results.stream()
                .map(result -> UserUtilities.nameFromUUID(UUID.fromString(result)))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieve a map of ticket stats, optionally filtered by a players unique id
     *
     * @param uuid Optional players unique id to filter with
     * @return Enum map of the ticket stats
     */
    @NonNull
    public static EnumMap<TicketStatus, Integer> selectTicketStats(@Nullable final UUID uuid) {
        DbRow row;

        String sql = "SELECT "
                + "SUM(Status LIKE 'OPEN') AS open, "
                + "SUM(Status LIKE 'PICKED') AS picked, "
                + "SUM(status LIKE 'CLOSED') AS closed "
                + "from puretickets_ticket ";

        try {
            if (uuid == null) {
                row = DB.getFirstRow(sql);
            } else {
                row = DB.getFirstRow(sql + " WHERE uuid = ?", uuid.toString());
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        EnumMap<TicketStatus, Integer> results = new EnumMap<>(TicketStatus.class);

        results.put(TicketStatus.OPEN, row.getInt("open"));
        results.put(TicketStatus.PICKED, row.getInt("picked"));
        results.put(TicketStatus.CLOSED, row.getInt("closed"));

        return results;
    }

    /**
     * Checks if a ticket exists using a given id
     *
     * @param id Tickets id to filter with
     * @return Boolean
     */
    public static boolean exists(final int id) {
        try {
            Integer value = DB.getFirstColumn("SELECT EXISTS(SELECT 1 from puretickets_ticket WHERE id = ?)", id);

            return value == 1;
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Count the amount of tickets with a given unique id and status.
     *
     * @param uuid   Players unique id to filter with
     * @param status Status to filter with
     * @return Number of tickets
     */
    public static int count(@NonNull final UUID uuid, @NonNull final TicketStatus status) {
        try {
            return platform.getPureInteger(DB.getFirstColumn(
                    "SELECT COUNT(id) FROM puretickets_ticket WHERE uuid = ? AND status = ?",
                    uuid.toString(),
                    status.name()
            ));
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }
    }


    /**
     * Count the amount of tickets with a given optional status
     *
     * @param status Optional status to filter with
     * @return Number of tickets
     */
    public static int count(@Nullable final TicketStatus status) {
        String sql = "SELECT COUNT(id) FROM puretickets_ticket";

        try {
            if (status == null) {
                return platform.getPureInteger(DB.getFirstColumn(sql));
            } else {
                return platform.getPureInteger(DB.getFirstColumn(sql + " WHERE status = ?", status.name()));
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Insert a ticket into the Database and retrieve it's ticket id
     *
     * @param uuid     Players unique id
     * @param status   Tickets status
     * @param picker   Pickers unique id
     * @param location Ticket creation location
     * @return Tickets id
     */
    public static int insert(
            @NonNull final UUID uuid,
            @NonNull final TicketStatus status,
            @Nullable final UUID picker,
            @NonNull final Location location
    ) {
        Integer index;

        try {
            index = DB.getFirstColumn("SELECT max(id) from puretickets_ticket");

            if (index == null) {
                index = 1;
            } else {
                index += 1;
            }

            DB.executeInsert("INSERT INTO puretickets_ticket(id, uuid, status, picker, location) VALUES(?, ?, ?, ?, ?)",
                    index, uuid.toString(), status.name(), picker, HelpersSQL.serializeLocation(location)
            );
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        return index;
    }

    /**
     * Updates a tickets entry in the Database
     *
     * @param ticket Ticket to use
     */
    public static void update(@NonNull final Ticket ticket) {
        UUID pickerUUID = ticket.getPickerUUID();
        String picker;

        if (pickerUUID == null) {
            picker = null;
        } else {
            picker = pickerUUID.toString();
        }

        DB.executeUpdateAsync("UPDATE puretickets_ticket SET status = ?, picker = ? WHERE id = ?",
                ticket.getStatus().name(), picker, ticket.getId()
        );
    }

    /**
     * Retrieve the ticket completions grouped by player within an option time span
     *
     * @param span Optional span to check within
     * @return Map of players and their ticket completions.
     */
    public static Map<UUID, Integer> highscores(@NonNull final TimeAmount span) {
        Map<UUID, Integer> data = new HashMap<>();
        long length;

        if (span.getLength() == null) {
            length = 0;
        } else {
            length = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() - span.getLength();
        }

        String sql = "SELECT picker, COUNT(*) AS `num` "
                + "FROM puretickets_ticket "
                + "WHERE status = ? "
                + "AND picker IS NOT NULL "
                + "and id in (SELECT DISTINCT ticket FROM puretickets_message WHERE date > ?) "
                + "GROUP BY picker";

        List<DbRow> results;

        try {
            results = DB.getResults(sql, TicketStatus.CLOSED.name(), length);
        } catch (SQLException e) {
            throw new IllegalArgumentException();
        }

        results.forEach(result -> data.put(HelpersSQL.getUUID(result, "picker"), result.getInt("num")));

        return data;
    }

    @NonNull
    private static Pair<String, Object[]> buildWhereExtension(final boolean hasWhere, final TicketStatus... statuses) {
        final Object[] replacements = new String[statuses.length];
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < statuses.length; i++) {
            if (i == 0) {
                sb.append(hasWhere ? " AND (status = ?" : " WHERE status = ?");
            } else {
                sb.append(" OR status = ?");
            }

            replacements[i] = statuses[i].name();
        }

        if (hasWhere && statuses.length > 0) {
            sb.append(")");
        }

        return Pair.of(sb.toString(), replacements);
    }

}
