package co.uk.magmo.puretickets.user;

import ai.broccol.corn.spigot.BaseUser;
import ai.broccol.corn.spigot.locale.LocaleManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class User extends BaseUser {

    public User(@NotNull final LocaleManager localeManager, @NotNull final CommandSender sender) {
        super(localeManager, sender);
    }

    public User(@NotNull final LocaleManager localeManager, @Nullable final UUID uuid) {
        super(localeManager, uuid);
    }
}
