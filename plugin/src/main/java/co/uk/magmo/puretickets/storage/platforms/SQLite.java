package co.uk.magmo.puretickets.storage.platforms;

import co.aikar.idb.*;
import co.uk.magmo.puretickets.configuration.Config;
import co.uk.magmo.puretickets.storage.FunctionInterfaces.PlatformFunctions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class SQLite implements PlatformFunctions {
    @Override
    public void setup(Plugin plugin, Config config) {
        int version = 0;
        File file = new File(plugin.getDataFolder(), "tickets.db");

        try {
            file.createNewFile();
        } catch (IOException e) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

        DatabaseOptions options = DatabaseOptions.builder().sqlite(file.toString()).build();
        HikariPooledDatabase database = PooledDatabaseOptions.builder().options(options).createHikariDatabase();

        DB.setGlobalDatabase(database);

        try {
            DB.executeUpdate("CREATE TABLE IF NOT EXISTS ticket(id INTEGER, uuid TEXT, status TEXT, picker TEXT)");
            DB.executeUpdate("CREATE TABLE IF NOT EXISTS message(ticket INTEGER, reason TEXT, data TEXT, sender TEXT, date TEXT)");
            DB.executeUpdate("CREATE TABLE IF NOT EXISTS notification(uuid TEXT, message TEXT, replacements TEXT)");
            DB.executeUpdate("CREATE TABLE IF NOT EXISTS settings(uuid TEXT, announcements TEXT)");

            version = DB.getFirstColumn("PRAGMA user_version");
        } catch (SQLException e) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

        try {
            if (version == 0) {
                plugin.getLogger().info("Updated PureTickets database to have location column");
                DB.executeUpdate("ALTER TABLE ticket ADD location TEXT");
                version++;
            }

            if (version <= 1) {
                plugin.getLogger().info("Updated PureTickets database to remove tickets with empty locations and remove all pending notifications");
                DB.executeUpdate("DELETE FROM ticket WHERE location IS NULL OR trim(location) = ?", "");
                DB.executeUpdate("DELETE FROM notification");
                version++;
            }
        } catch (SQLException e) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        } finally {
            try {
                DB.executeUpdate("PRAGMA user_version = ?", version);
            } catch (SQLException e) {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }
    }

    @Override
    public Long getPureLong(DbRow row, String column) {
        return Long.valueOf(row.getString(column));
    }
}
