package ru.shk.velocity.commons.cmd;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import ru.shk.velocity.commons.Commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CTPCommand implements SimpleCommand {
    private final Commons plugin;

    public CTPCommand(Commons plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if(!invocation.source().hasPermission("commons.cmd.tp")) return;
        if (invocation.arguments().length==0){
            invocation.source().sendRichMessage("<red> /ctp <player> [player]");
            return;
        }
        Player who;
        Player toWhom;
        if (invocation.arguments().length==1){
            if (!(invocation.source() instanceof Player p)){
                invocation.source().sendRichMessage("<red> Нужно быть игроком, чтобы использовать эту команду.");
                return;
            }
            who = p;
            Optional<Player> toWhomO = plugin.proxy().getPlayer(invocation.arguments()[0]);
            if (toWhomO.isEmpty()){
                invocation.source().sendRichMessage("<red> Игрок "+invocation.arguments()[0]+" не найден на сервере.");
                return;
            }
            toWhom = toWhomO.get();
        } else {
            Optional<Player> whoO = plugin.proxy().getPlayer(invocation.arguments()[0]);
            if (whoO.isEmpty()){
                invocation.source().sendRichMessage("<red> Игрок "+invocation.arguments()[0]+" не найден на сервере.");
                return;
            }
            who = whoO.get();
            Optional<Player> toWhomO = plugin.proxy().getPlayer(invocation.arguments()[1]);
            if (toWhomO.isEmpty()){
                invocation.source().sendRichMessage("<red> Игрок "+invocation.arguments()[1]+" не найден на сервере.");
                return;
            }
            toWhom = toWhomO.get();
        }
        invocation.source().sendRichMessage("<yellow> Телепортируем "+who.getUsername()+" к "+toWhom.getUsername()+"...");
        plugin.teleport(who, toWhom);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<Player> all = new ArrayList<>(plugin.proxy().getAllPlayers());
            if (invocation.arguments().length==0) return all.stream().map(Player::getUsername).toList();
            return all.stream().map(Player::getUsername).filter(s -> s.startsWith(invocation.arguments()[invocation.arguments().length-1].toLowerCase())).toList();
        });
    }
}
