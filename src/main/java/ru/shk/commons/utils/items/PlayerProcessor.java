package ru.shk.commons.utils.items;

import java.util.UUID;

public interface PlayerProcessor {
    long idFromName(String name);
    UUID UUIDFromName(String name);
    String nameFromId(long id);
    UUID UUIDFromId(long id);
    String nameFromUUID(UUID uuid);
    long idFromUUID(UUID uuid);
}
