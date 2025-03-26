package ru.shk.velocity.commons.cmd;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ru.shk.velocity.commons.Commons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FindCMD implements SimpleCommand {
    private final Commons plugin;

    public FindCMD(Commons plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("commons.cmd.find")) return;
        if (invocation.arguments().length==0){
            invocation.source().sendMessage(Component.text("/find <player>").color(NamedTextColor.RED));
            return;
        }
        Optional<Player> target = plugin.proxy().getPlayer(invocation.arguments()[0]);
        if (target.isEmpty()){
            invocation.source().sendMessage(Component.text("На сервере нет игрока с ником "+invocation.arguments()[0]).color(NamedTextColor.RED));
            return;
        }
        Optional<ServerConnection> s = target.get().getCurrentServer();
        if (s.isEmpty()){
            invocation.source().sendMessage(Component.text("Игрок не находится ни на одном из серверов. Вероятно, он еще в процессе соединения.").color(NamedTextColor.RED));
            return;
        }
        String server = s.get().getServerInfo().getName();
        plugin.playerLocationReceiver().findPlayer(target.get(), c -> {
            if (c==null) {
                invocation.source().sendMessage(Component.text("Игрок находится на сервере "+server+", но координаты получить не удалось :(").color(NamedTextColor.YELLOW));
            } else {
                invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(String.format("<aqua>Игрок <white>%s<aqua> находится на сервере <white>%s<aqua> в мире <white>%s<aqua> на <aqua>%d %d %d", target.get().getUsername(), server, c.getWorld(), c.getX(), c.getY(), c.getZ())));
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<Player> all = plugin.proxy().getAllPlayers();
            if (invocation.arguments().length==0 || invocation.arguments()[0].isEmpty()) return all.stream().map(Player::getUsername).toList();
            return all.stream().map(Player::getUsername).filter(s -> s.startsWith(invocation.arguments()[0].toLowerCase())).toList();
        });
    }
}
