package ru.shk.commons.utils.nms;

import ru.shk.commons.Commons;

public enum FieldMappings {
    ENTITY_GETID("getId", "ae"),
    ENTITY_GETDATAWATCHER("getDataWatcher", "ai"),
    ENTITY_LOCX("locX", "dc"),
    ENTITY_LOCY("locY", "de"),
    ENTITY_LOCZ("locZ", "di"),
    ENTITY_SET_POS("setPosition", "e"),
    ENTITY_SETNOGRAVITY("setNoGravity", "e"),
    ENTITY_SETCUSTOMNAMEVISIBLE("setCustomNameVisible", "n"),
    ENTITY_SETCUSTOMNAME("setCustomNameVisible", "a"),
    ARMORSTAND_SETINVISIBLE("setInvisible", "j"),
    ARMORSTAND_SETBASEPLATE("setBasePlate", "s"),
    ARMORSTAND_SETARMS("setArms", "r"),
    ARMORSTAND_SETMARKER("setMarker", "t"),
    ARMORSTAND_SETHEADPOSE("setHeadPose", "a"),
    ARMORSTAND_SETSMALL("setSmall", "a"),

    BLOCKBASE_GETMAPCOLOR("s", "t"),

    MAPCOLOR_INTCOLOR("al", "ak")
    ;

    private static int versionId;

    static {
        init();
    }

    private static void init(){
        versionId = switch (Commons.getServerVersion()){
            case v1_17_R1 -> 0;
            default -> 1;
        };
//        for (int i = 0; i < PacketVersion.values().length; i++) {
//            if(Commons.getServerVersion()==PacketVersion.values()[i]){
//                versionId = i;
//                return;
//            }
//        }
//        versionId = -1;
    }

    private final String[] mappings;
    FieldMappings(String... mappings){
        this.mappings = mappings;
    }

    public String getField(){
        return mappings[versionId];
    }
}
