package ru.shk.commons.utils.gui;

import ru.shk.commons.utils.items.ItemStackBuilder;

public record UniversalClick (int slot, ItemStackBuilder item, GUI.ClickType type, boolean shift) {}
