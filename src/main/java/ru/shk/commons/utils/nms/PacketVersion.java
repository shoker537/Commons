package ru.shk.commons.utils.nms;

import lombok.Getter;

public enum PacketVersion {
    v1_21_R1("1.21")
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
