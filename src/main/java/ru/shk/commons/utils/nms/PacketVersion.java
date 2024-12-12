package ru.shk.commons.utils.nms;

import lombok.Getter;

public enum PacketVersion {
    v1_21_3("1.21.3")
    ;

    @Getter private final String versionName;

    PacketVersion(String versionName) {
        this.versionName = versionName;
    }

    public static PacketVersion byName(String versionName){
        for (PacketVersion value : values()) {
            if(value.versionName.equals(versionName)) return value;
        }
        return null;
    }
}
