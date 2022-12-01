package ru.shk.commons.utils;

import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

@Getter
public class CustomHead {
    private final int id;
    private final String key;
    private final String texture;


    public CustomHead(int id, @NonNull String key, @NonNull String texture) {
        this.id = id;
        this.key = key;
        this.texture = texture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomHead that = (CustomHead) o;
        return id == that.id && key.equals(that.key) && texture.equals(that.texture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, texture);
    }
}
