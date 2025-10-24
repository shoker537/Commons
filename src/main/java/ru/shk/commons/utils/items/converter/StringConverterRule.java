package ru.shk.commons.utils.items.converter;

import java.util.function.BiConsumer;
import java.util.function.Function;

// === Rule for Conversion ===
public class StringConverterRule<B> {
    final String key;
    final Function<B, Value> getter;
    final BiConsumer<B, Value> setter;

    public StringConverterRule(String key, Function<B, Value> getter, BiConsumer<B, Value> setter) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
    }
}
