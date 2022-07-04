package ru.shk.commons.utils.scoreboards;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.PacketUtil;

import java.lang.reflect.InvocationTargetException;

public class SB {

    @Getter
    private final Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    @Getter
    private final Objective objective;

    private byte c;
    private byte space = 1;
    private int last_entry = 0;

    public SB(String title, String objective, int maxValue) {
        this.objective = board.registerNewObjective(objective, "dummy", (title.replace("&", "§")));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        if (maxValue != -1) load(maxValue);
    }

    public void load(int maxValue) {
        c = (byte) (maxValue - 1);
        objective.getScore(" ").setScore(c + 1);
    }

    public void addLine(String line) {
        objective.getScore(line.replace("&", "§")).setScore(c);
        c--;
    }

    public void addDynamicLine(String team, String prefix, String suffix) {
        String entry = getNextEntry()+"";
        if (board.getTeam(team) != null) {
            board.getTeam(team).unregister();
        }
        Team t = board.registerNewTeam(team);
        t.addEntry(entry);
        t.setPrefix((prefix.replace("&", "§")));
        t.setSuffix((suffix.replace("&", "§")));
        objective.getScore(entry).setScore(c);
        c--;
    }

    public void setLine(String line) {
        objective.getScore(line.replace("&", "§")).setScore(c);
    }

    public void setDynamicLine(String team, String suffix) {
        board.getTeam(team).setSuffix((suffix.replace("&", "§")));
    }

    public void setDynamicLine(String team, String prefix, String suffix) {
        /*if(prefix.length()>16) prefix = prefix.substring(16);
        if(suffix.length()>16) suffix = suffix.substring(16);*/
        board.getTeam(team).setSuffix((suffix.replace("&", "§")));
        board.getTeam(team).setPrefix((prefix.replace("&", "§")));
    }

    public void addBlank() {
        objective.getScore((" " + " ".repeat(Math.max(0, 20 - space))).replace("&", "§")).setScore(c);
        c--;
        space++;
    }

    public SB sidebar(@NonNull String title) {
        objective.setDisplayName((Commons.getInstance().colorize(title)));
        return this;
    }

    public SB blankLine() {
        addBlank();
        return this;
    }

    public SB dynamicLine(@NonNull String team, @NonNull String prefix, @NonNull String suffix) {
        addDynamicLine(team, prefix, suffix);
        return this;
    }

    public SB line(@NonNull String line) {
        addLine(line);
        return this;
    }

    private ChatColor getNextEntry(){
        ChatColor next = ChatColor.values()[last_entry];
        last_entry++;
        return next;
    }

    public static void updateScoreboardPersonally(Player player, String team, String prefix, String suffix) {
        sendScoreboardTeamPacket(player, team.replace("&", "§"), prefix.replace("&", "§"), suffix.replace("&", "§"));
    }

    private static void sendScoreboardTeamPacket(Player p, String team, String prefix, String suffix){
        try {
            PacketUtil.sendScoreboardTeamPacket(false,p, team, prefix, suffix);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}