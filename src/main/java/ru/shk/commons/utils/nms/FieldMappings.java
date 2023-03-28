package ru.shk.commons.utils.nms;

import ru.shk.commons.Commons;

public enum FieldMappings {
    ENTITY_GETID("getId", "ae", "ae", "af"),
    ENTITY_GETDATAWATCHER("getDataWatcher", "ai", "ai", "aj"),
    ENTITY_LOCX("locX", "dc", "dg", "dl"),
    ENTITY_LOCY("locY", "de", "di", "dn"),
    ENTITY_LOCZ("locZ", "di", "dm", "dr"),
    ENTITY_SET_POS("setPosition", "e", "e", "e"),
    ENTITY_SETNOGRAVITY("setNoGravity", "e", "e", "e"),
    ENTITY_SETCUSTOMNAMEVISIBLE("setCustomNameVisible", "n", "n", "n"),
    ENTITY_SETCUSTOMNAME("setCustomName", "a", "b", "b"),
    ARMORSTAND_SETINVISIBLE("setInvisible", "j", "j", "j"),
    ARMORSTAND_SETBASEPLATE("setBasePlate", "s", "s", "s"),
    ARMORSTAND_SETARMS("setArms", "r", "r", "a"),
    ARMORSTAND_SETMARKER("setMarker", "t", "t", "u"),
    ARMORSTAND_SETHEADPOSE("setHeadPose", "a", "a", "a"),
    ARMORSTAND_SETSMALL("setSmall", "a", "a", "t"),
    SCOREBOARDTEAM_SETCOLLISIONMODE("setCollisionRule", "a", "a", "a"),
    SCOREBOARDTEAM_SETCANSEEFRIENDLYINVISIBLE("setCanSeeFriendlyInvisibles", "b", "b", "b"),
    SCOREBOARDTEAM_SETFRIENDLYFIRE("setAllowFriendlyFire", "a", "a", "a"),
    ENTITYHUMAN_STARTUSERIPTIDE("s", "t", "t", "s"),

    BLOCKBASE_GETMAPCOLOR("s", "t", "s", "t"),

    MAPCOLOR_INTCOLOR("al", "ak", "ak", "ak")
    ;

    private static int versionId;

    static {
        init();
    }

    private static void init(){
        versionId = switch (Commons.getServerVersion()){
            case v1_18_R1, v1_18_R2 -> 1;
            case v1_19_R1 -> 2;
            default -> 3;
        };
    }

    private final String[] mappings;
    FieldMappings(String... mappings){
        this.mappings = mappings;
    }

    public String getField(){
        return mappings[versionId];
    }
}
