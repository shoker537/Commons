package ru.shk.commons.utils.items.converter;

public record NumberValue(Number value) implements Value {
    @Override public String asString() { return value.toString(); }
}
