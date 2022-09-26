package ru.shk.commons.utils;

import lombok.Getter;

@Getter
public class CustomHead {
    private final int id;
    private final String key;
    private final String texture;

    public CustomHead(int id, String key, String texture) {
        this.id = id;
        this.key = key;
        this.texture = texture;
    }
}
