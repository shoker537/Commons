package ru.shk.commonsbungee.cmd;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commonsbungee.Commons;

public class CommonsCmd extends Command {
    public CommonsCmd() {
        super("commons", "group.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Commons.getInstance().colorize(" &b          Commons v"+Commons.getInstance().getDescription().getVersion()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool active count: &f"+Commons.getInstance().getThreadPool().getActiveCount()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool queue count: &f"+Commons.getInstance().getThreadPool().getQueue().size()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool size: &f"+Commons.getInstance().getThreadPool().getPoolSize()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool maxSize: &f"+Commons.getInstance().getThreadPool().getMaximumPoolSize()));
        sender.sendMessage(Commons.getInstance().colorize(" &bHeadsCache size: &f"+ ItemStackBuilder.headsCache().cacheSize()));
        sender.sendMessage(Commons.getInstance().colorize(" &bLegacy ItemStackBuilder heads cache size: &f"+ ru.shk.commonsbungee.ItemStackBuilder.headsCacheSize()));
    }
}
