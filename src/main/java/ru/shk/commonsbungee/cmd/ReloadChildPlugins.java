package ru.shk.commonsbungee.cmd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;

public class ReloadChildPlugins extends Command {

    public ReloadChildPlugins() {
        super("creload", "group.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GREEN+"> Reloading all children plugins...");
        ProxyServer.getInstance().getPluginManager().getPlugins().stream().filter(plugin -> plugin.getDescription().getDepends().contains("Commons")).forEach(plugin -> {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "bsu reloadplugin "+plugin.getDescription().getName()+" -f");
        });
        sender.sendMessage(ChatColor.GREEN+"> Reloading done!");
    }
}
