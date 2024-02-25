package ru.shk.guilib;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public record ClickEvent (Player player, ClickType clickType) {}
