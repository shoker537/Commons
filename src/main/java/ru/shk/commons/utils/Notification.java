package ru.shk.commons.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import java.util.concurrent.ThreadLocalRandom;

public class Notification {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private NamespacedKey id;
    private final String icon;
    private final String header;
    private final String footer;

    public Notification(String header, String footer, String icon) {
        this.icon = icon;
        this.header = header;
        this.footer = footer;
        do {
            id = new NamespacedKey(Commons.getInstance(), "commons" + (ThreadLocalRandom.current().nextInt(1000)));
        } while (Bukkit.getAdvancement(id)!=null);
    }

    public void show(Player player) {
        register();
        grant(player);
        Commons.getInstance().syncLater(() -> {
            revoke(player);
            unregister();
        }, 20);
    }

    private void register() {
        try {
            Bukkit.getUnsafe().loadAdvancement(id, json());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private String json() {
        JsonObject json = new JsonObject();

        JsonObject display = new JsonObject();

        JsonObject icon = new JsonObject();
        icon.addProperty("item", this.icon);

        display.add("icon", icon);
        display.addProperty("title", Commons.colorizeWithHex(this.header + "\n" + this.footer));
        display.addProperty("description", "Commons Notification");
        display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
        display.addProperty("frame", "goal");
        display.addProperty("announce_to_chat", false);
        display.addProperty("show_toast", true);
        display.addProperty("hidden", true);

        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");

        JsonObject criteria = new JsonObject();
        criteria.add("impossible", trigger);

        json.add("criteria", criteria);
        json.add("display", display);

        return gson.toJson(json);
    }

    private void unregister() {
        Bukkit.getUnsafe().removeAdvancement(id);
    }

    private void grant(Player player) {
        Advancement advancement = Bukkit.getAdvancement(id);
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone()) {
            progress.getRemainingCriteria().forEach(progress::awardCriteria);
        }
    }

    private void revoke(Player player) {
        Advancement advancement = Bukkit.getAdvancement(id);
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        }
    }


}