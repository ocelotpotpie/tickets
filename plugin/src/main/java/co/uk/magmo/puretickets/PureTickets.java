package co.uk.magmo.puretickets;

import ai.broccol.corn.spigot.locale.LocaleManager;
import ai.broccol.corn.spigot.locale.LocaleUtils;
import co.aikar.idb.DB;
import co.uk.magmo.puretickets.commands.CommandManager;
import co.uk.magmo.puretickets.configuration.Config;
import co.uk.magmo.puretickets.integrations.DiscordManager;
import co.uk.magmo.puretickets.interactions.NotificationManager;
import co.uk.magmo.puretickets.locale.Messages;
import co.uk.magmo.puretickets.storage.SQLManager;
import co.uk.magmo.puretickets.tasks.TaskManager;
import co.uk.magmo.puretickets.ticket.TicketManager;
import co.uk.magmo.puretickets.user.UserManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class PureTickets extends JavaPlugin {
    private TaskManager taskManager;
    private NotificationManager notificationManager;

    @Override
    public void onEnable() {
        Map<Locale, FileConfiguration> locales = LocaleUtils.saveLocales(this,
                new File(getDataFolder(), "locales"), "/locales");

        Config config = new Config(this);
        DiscordManager discordManager = new DiscordManager(this.getLogger(), config);
        LocaleManager localeManager = new LocaleManager(locales.get(Locale.ENGLISH), Messages.PREFIX);
        SQLManager sqlManager = new SQLManager(this, localeManager, config);

        UserManager userManager = new UserManager(sqlManager);
        TicketManager ticketManager = new TicketManager(config, sqlManager);
        CommandManager commandManager = new CommandManager(this, config, localeManager, ticketManager);


        taskManager = new TaskManager(this);
        notificationManager = new NotificationManager(config, sqlManager, taskManager, localeManager, userManager, discordManager, ticketManager);

        localeManager.registerKeys(Messages.class);

        commandManager.registerCompletions(ticketManager);
        commandManager.registerInjections(config, userManager, ticketManager, notificationManager, taskManager);
        commandManager.registerCommands();

        getServer().getPluginManager().registerEvents(notificationManager, this);
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.clear();
        }

        if (notificationManager != null) {
            notificationManager.save();
        }

        DB.close();
    }
}
