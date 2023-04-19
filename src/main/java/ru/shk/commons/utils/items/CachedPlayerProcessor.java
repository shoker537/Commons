package ru.shk.commons.utils.items;

import land.shield.playerapi.CachedPlayer;

import java.util.UUID;

public class CachedPlayerProcessor implements PlayerProcessor {
    @Override
    public long idFromName(String name) {
        return CachedPlayer.of(name).getId();
    }

    @Override
    public UUID UUIDFromName(String name) {
        return CachedPlayer.of(name).getUuid();
    }

    @Override
    public String nameFromId(long id) {
        return CachedPlayer.of((int)id).getName();
    }

    @Override
    public UUID UUIDFromId(long id) {
        return CachedPlayer.of((int)id).getUuid();
    }

    @Override
    public String nameFromUUID(UUID uuid) {
        return CachedPlayer.of(uuid).getName();
    }

    @Override
    public long idFromUUID(UUID uuid) {
        return CachedPlayer.of(uuid).getId();
    }
}
