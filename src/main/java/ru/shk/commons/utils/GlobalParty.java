package ru.shk.commons.utils;

import land.shield.playerapi.CachedPlayer;
import lombok.Getter;

import java.util.List;

@Getter
public class GlobalParty {
    private final List<CachedPlayer> players;
    private final CachedPlayer owner;

    public GlobalParty(List<CachedPlayer> players, CachedPlayer owner) {
        this.players = players;
        this.owner = owner;
    }

}
