package ru.shk.commons.utils;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Events implements Listener {
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e){
        if(e.getDamager().getType()!=EntityType.FIREWORK) return;
        if(e.getDamager().hasMetadata("effect")) e.setCancelled(true);
    }
}
