package ru.shk.commons.utils.nms;

import ru.shk.commons.Commons;

public enum FieldMappings {
    ENTITY_GETID("getId", "ae", "ae"),
    ENTITY_GETDATAWATCHER("getDataWatcher", "ai", "ai"),
    ENTITY_LOCX("locX", "dc", "dg"),
    ENTITY_LOCY("locY", "de", "di"),
    ENTITY_LOCZ("locZ", "di", "dm"),
    ENTITY_SET_POS("setPosition", "e", "e"),
    ENTITY_SETNOGRAVITY("setNoGravity", "e", "e"),
    ENTITY_SETCUSTOMNAMEVISIBLE("setCustomNameVisible", "n", "n"),
    ENTITY_SETCUSTOMNAME("setCustomName", "a", "b"),
    ARMORSTAND_SETINVISIBLE("setInvisible", "j", "j"),
    ARMORSTAND_SETBASEPLATE("setBasePlate", "s", "s"),
    ARMORSTAND_SETARMS("setArms", "r", "r"),
    ARMORSTAND_SETMARKER("setMarker", "t", "t"),
    ARMORSTAND_SETHEADPOSE("setHeadPose", "a", "a"),
    ARMORSTAND_SETSMALL("setSmall", "a", "a"),
    SCOREBOARDTEAM_SETCOLLISIONMODE("setCollisionRule", "a", "a"),

    BLOCKBASE_GETMAPCOLOR("s", "t", "s"),

    MAPCOLOR_INTCOLOR("al", "ak", "ak")
    ;

    private static int versionId;

    static {
        init();
    }

    private static void init(){
        versionId = switch (Commons.getServerVersion()){
            case v1_17_R1 -> 0;
            case v1_18_R1, v1_18_R2 -> 1;
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
