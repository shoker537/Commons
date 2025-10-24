package ru.shk.commons.utils.items.converter;

import java.util.Map;

public record MapValue(Map<String, Value> map) implements Value {
    @Override public String asString() { return map.toString(); }
}
