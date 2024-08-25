package ru.shk.commons.utils.gui;

public record ClickEvent (int slot, GUI.ClickType type, boolean shift) {}