package ru.shk.commons.utils.gui;

import lombok.NonNull;
import ru.shk.commons.utils.items.ItemStackBuilder;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public record Item (@NonNull ItemStackBuilder stack, @Nullable Consumer<ClickEvent> onClick, boolean runAsync) {
    public Item (@NonNull ItemStackBuilder stack, @Nullable Consumer<ClickEvent> onClick){
        this(stack, onClick, false);
    }
}
