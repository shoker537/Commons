package ru.shk.commons.utils.items.converter;

public record StringValue(String value) implements Value {
    @Override public String asString() { return value; }
}
