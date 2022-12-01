package ru.shk.commonsbungee.cmd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import ru.shk.commons.utils.TextComponentBuilder;
import ru.shk.commonsbungee.Commons;

import java.util.Collections;
import java.util.Locale;

public class CommonsTp extends Command implements TabExecutor {
    public CommonsTp() {
        super("commonstp", "commons.tp", "ctp");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length==0 || args.length>2){
            if(sender.equals(ProxyServer.getInstance().getConsole())){
                sender.sendMessage("/ctp <кого> <к кому>");
            } else {
                sender.sendMessage("/ctp <к кому>");
                sender.sendMessage("/ctp <кого> <к кому>");
            }
            return;
        }
        if(args.length==1){
            if(sender.equals(ProxyServer.getInstance().getConsole())){
                sender.sendMessage(ChatColor.RED+" Консоль не может телепортироваться к игрокам!!!");
            } else {
                ProxiedPlayer p1 = (ProxiedPlayer) sender;
                ProxiedPlayer p2 = ProxyServer.getInstance().getPlayer(args[0]);
                if(p2==null || !p2.isConnected() || p2.getServer()==null){
                    sender.sendMessage(ChatColor.RED+" Игрок "+args[0]+" сейчас не на сервере.");
                    return;
                }
                p1.sendMessage(new TextComponentBuilder("Телепортируемся к игроку "+p2.getName()).withColor(ChatColor.GOLD).build());
                Commons.getInstance().teleport(p1, p2);
            }
            return;
        }
        ProxiedPlayer p1 = ProxyServer.getInstance().getPlayer(args[0]);
        ProxiedPlayer p2 = ProxyServer.getInstance().getPlayer(args[1]);
        if(p2==null || !p2.isConnected() || p2.getServer()==null){
            sender.sendMessage(ChatColor.RED+" Игрок "+args[0]+" сейчас не на сервере.");
            return;
        }
        p1.sendMessage(new TextComponentBuilder("Телепортируем "+p1.getName()+" к игроку "+p2.getName()).withColor(ChatColor.GOLD).build());
        Commons.getInstance().teleport(p1, p2);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length>2) return null;
        if(args.length==0) return ProxyServer.getInstance().getPlayers().stream().map(CommandSender::getName).toList();
        String arg = args[args.length-1].toLowerCase();
        return ProxyServer.getInstance().getPlayers().stream().map(CommandSender::getName).filter(s -> arg.length()==0 || s.toLowerCase().startsWith(arg)).toList();
    }
}
