package co.uk.magmo.puretickets.commands

import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.uk.magmo.puretickets.interactions.Notifications
import co.uk.magmo.puretickets.locale.Messages
import co.uk.magmo.puretickets.ticket.Message
import co.uk.magmo.puretickets.ticket.TicketManager
import co.uk.magmo.puretickets.utils.Constants
import co.uk.magmo.puretickets.utils.asName
import co.uk.magmo.puretickets.utils.bold
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.format.DateTimeFormatter

@CommandAlias("ticket|ti")
class TicketCommand : PureBaseCommand() {
    @Default
    @HelpCommand
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.helpEntries.removeAt(0)
        help.showHelp()
    }

    @Subcommand("create|c")
    @CommandPermission(Constants.USER_PERMISSION + ".create")
    @Description("Create a ticket")
    @Syntax("<Message>")
    fun onCreate(player: Player, message: Message) {
        val ticket = TicketManager.createTicket(player, message)
        Notifications.reply(player, Messages.TICKET__CREATED, "%id%", ticket.id.toString())
        Notifications.announce(Messages.ANNOUNCEMENTS__NEW_TICKET, "%user%", player.name, "%id%", ticket.id.toString(), "%ticket%", ticket.currentMessage()!!)
    }

    @Subcommand("update|u")
    @CommandCompletion("@IssuerTicketIds")
    @CommandPermission(Constants.USER_PERMISSION + ".update")
    @Description("Update a ticket")
    @Syntax("<Index> <Message>")
    fun onUpdate(player: Player, index: Int, message: Message) {
        val information = generateInformation(player, index)
        val id = TicketManager.update(information, message)

        Notifications.reply(player, Messages.TICKET__UPDATED, "%id%", id.toString())
        Notifications.announce(Messages.ANNOUNCEMENTS__UPDATED_TICKET, "%user%", player.name, "%id%", id.toString(), "%ticket%", message.data!!)
    }

    @Subcommand("close|cl")
    @CommandCompletion("@IssuerTicketIds")
    @CommandPermission(Constants.USER_PERMISSION + ".close")
    @Description("Close a ticket")
    @Syntax("[Index]")
    fun onClose(player: Player, @Optional index: Int?) {
        val information = generateInformation(player, index)
        val id = TicketManager.close(player, information)

        Notifications.reply(player, Messages.TICKET__CLOSED, "%id%", id.toString())
        Notifications.announce(Messages.ANNOUNCEMENTS__CLOSED_TICKET, "%user%", player.name, "%id%", id.toString())
    }

    @Subcommand("show|s")
    @CommandCompletion("@AllTicketHolders @UserTicketIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".show")
    @Description("Show a ticket")
    @Syntax("<Player> [Index]")
    fun onShow(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index)

        TicketManager[information.player, information.index]?.apply {
            val picker = if (pickerUUID == null) "Unpicked" else pickerUUID.asName()

            Notifications.reply(sender, Messages.TITLES__SHOW_TICKET, "%id%", id.toString())
            Notifications.reply(sender, Messages.SHOW__SENDER, "%player%", playerUUID.asName())
            Notifications.reply(sender, Messages.SHOW__PICKER, "%player%", picker)
            Notifications.reply(sender, Messages.SHOW__DATE, "%date%", dateOpened()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!)
            Notifications.reply(sender, Messages.SHOW__MESSAGE, "%message%", currentMessage()!!)
        }
    }

    @Subcommand("pick|p")
    @CommandCompletion("@AllTicketHolders @UserTicketIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".pick")
    @Description("Pick a ticket")
    @Syntax("<Player> [Index]")
    fun onPick(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index)
        val id = TicketManager.pick(sender, information)

        Notifications.reply(sender, Messages.TICKET__PICKED, "%id%", id.toString())
        Notifications.send(information.player, Messages.NOTIFICATIONS__PICK, "%user%", sender.name)
        Notifications.announce(Messages.ANNOUNCEMENTS__PICKED_TICKET, "%user%", sender.name, "%id%", id.toString())
    }

    @Subcommand("done|d")
    @CommandCompletion("@AllTicketHolders @UserTicketIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".done")
    @Description("Done-mark a ticket")
    @Syntax("<Player> [Index]")
    fun onDone(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index)
        val id = TicketManager.done(sender, information)

        Notifications.reply(sender, Messages.TICKET__DONE, "%id%", id.toString())
        Notifications.send(information.player, Messages.NOTIFICATIONS__DONE, "%user%", sender.name)
        Notifications.announce(Messages.ANNOUNCEMENTS__DONE_TICKET, "%user%", sender.name, "%id%", id.toString())
    }

    @Subcommand("yield|y")
    @CommandCompletion("@AllTicketHolders @UserTicketIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".yield")
    @Description("Yield a ticket")
    @Syntax("<Player> [Index]")
    fun onYield(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index)
        val id = TicketManager.yield(sender, information)

        Notifications.reply(sender, Messages.TICKET__YIELDED, "%id%", id.toString())
        Notifications.send(information.player, Messages.NOTIFICATIONS__YIELD, "%user%", sender.name)
        Notifications.announce(Messages.ANNOUNCEMENTS__YIELDED_TICKET, "%user%", sender.name, "%id%", id.toString())
    }

    @Subcommand("reopen|ro")
    @CommandCompletion("@UserOfflineNames @UserOfflineTicketIDs")
    @CommandPermission(Constants.STAFF_PERMISSION + ".reopen")
    @Description("Reopen a ticket")
    @Syntax("<Player> [Index]")
    fun onReopen(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index, true)
        val id = TicketManager.reopen(sender, information)

        Notifications.reply(sender, Messages.TICKET__REOPENED, "%id%", id.toString())
        Notifications.send(information.player, Messages.NOTIFICATIONS__REOPEN, "%user%", sender.name)
        Notifications.announce(Messages.ANNOUNCEMENTS__REOPEN_TICKET, "%user%", sender.name, "%id%", id.toString())
    }

    @Subcommand("log")
    @CommandCompletion("@AllTicketHolders @UserTicketIds")
    @CommandPermission(Constants.STAFF_PERMISSION + ".log")
    @Description("Log tickets messages")
    @Syntax("<Player> [Index]")
    fun onLog(sender: CommandSender, offlinePlayer: OfflinePlayer, @Optional index: Int?) {
        val information = generateInformation(offlinePlayer, index)
        val ticket = TicketManager[information.player, information.index] ?: return

        Notifications.reply(sender, Messages.TITLES__TICKET_LOG, ticket.id.toString())

        ticket.messages.forEach {
            sender.sendMessage("§f§l" + it.reason.name + " -  §8" + (it.data ?: it.sender.asName()))
        }
    }

    @Subcommand("list|l")
    @CommandPermission(Constants.USER_PERMISSION + ".list")
    @Description("List all tickets")
    fun onList(sender: CommandSender) {
        Notifications.reply(sender, Messages.TITLES__ALL_TICKETS)

        TicketManager.asMap().forEach { (uuid, tickets) ->
            sender.sendMessage(ChatColor.GREEN.toString() + uuid.asName())

            tickets.forEach { t -> sender.sendMessage(t.status.color.toString() + "#" + ChatColor.WHITE.bold() + t.id.toString() + ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + t.currentMessage()) }
        }
    }
}