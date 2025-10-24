package ru.shk.commons.utils.items.converter;

import java.util.List;

public record ArrayValue(List<Value> values) implements Value {
    @Override public String asString() { return values.toString(); }
}
