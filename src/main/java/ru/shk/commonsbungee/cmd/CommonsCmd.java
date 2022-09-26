package ru.shk.commonsbungee.cmd;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import ru.shk.commonsbungee.Commons;

public class CommonsCmd extends Command {
    public CommonsCmd(String name) {
        super("commons", "group.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool active count: &f"+Commons.getInstance().getThreadPool().getActiveCount()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool queue count: &f"+Commons.getInstance().getThreadPool().getQueue().size()));
        sender.sendMessage(Commons.getInstance().colorize(" &bThreadPool pool size: &f"+Commons.getInstance().getThreadPool().getPoolSize()));
    }
}
