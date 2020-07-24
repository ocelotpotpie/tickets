package co.uk.magmo.puretickets.user;

import co.uk.magmo.corn.spigot.BaseUser;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class User extends BaseUser {
    public User(@NotNull CommandSender sender) {
        super(sender);
    }
}
