package ru.shk.commons.utils.nms;

import ru.shk.commons.Commons;

public enum FieldMappings {
    ENTITY_GETID("getId", "ae", "ae", "af"),
    ENTITY_GETDATAWATCHER("getDataWatcher", "ai", "ai", "aj"),
    ENTITY_GETLEVEL(null, "W", "W", "Y"),
    ENTITY_LOCX("locX", "dc", "df", "dl"),
    ENTITY_LOCY("locY", "de", "dh", "dn"),
    ENTITY_LOCZ("locZ", "di", "dl", "dr"),
    ENTITY_SETSHAREDFLAG(null, null, "b", "b"),
    ENTITY_SET_POS("setPosition", "e", "e", "e"),
    ENTITY_TELEPORT_TO(null,null, "a", "a"),
    ENTITY_SETNOGRAVITY("setNoGravity", "e", "e", "e"),
    ENTITY_SETCUSTOMNAMEVISIBLE("setCustomNameVisible", "n", "n", "n"),
    ENTITY_SETCUSTOMNAME("setCustomName", "a", "b", "b"),
    ARMORSTAND_SETINVISIBLE("setInvisible", "j", "j", "j"),
    ARMORSTAND_SETBASEPLATE("setBasePlate", "s", "s", "s"),
    ARMORSTAND_SETARMS("setArms", "r", "r", "a"),
    ARMORSTAND_SETMARKER("setMarker", "t", "t", "u"),
    ARMORSTAND_SETHEADPOSE("setHeadPose", "a", "a", "a"),
    ARMORSTAND_SETBODYPOSE("setBodyPose", "b", "b", "b"),
    ARMORSTAND_SETLEFTARMPOSE("setLeftArmPose", "c", "c", "c"),
    ARMORSTAND_SETRIGHTARMPOSE("setRightArmPose", "d", "d", "d"),
    ARMORSTAND_SETLEFTLEGPOSE("setLeftLegPose", "e", "e", "e"),
    ARMORSTAND_SETRIGHTLEGPOSE("setRightLegPose", "f", "f", "f"),
    ARMORSTAND_SETSMALL("setSmall", "a", "a", "t"),
    MOB_SETLEASHEDTO(null, null, "b", "b"),
    MOB_GETLEASHHOLDER(null, null, "fz", "fJ"),
    SCOREBOARDTEAM_SETCOLLISIONMODE("setCollisionRule", "a", "a", "a"),
    SCOREBOARDTEAM_SETCANSEEFRIENDLYINVISIBLE("setCanSeeFriendlyInvisibles", "b", "b", "b"),
    SCOREBOARDTEAM_SETFRIENDLYFIRE("setAllowFriendlyFire", "a", "a", "a"),
    ENTITYHUMAN_STARTUSERIPTIDE("s", "t", "t", "s"),
    DATAWATCHER_PACKDIRTY(null, "b", "b", "b"),
    BLOCK_DESCRIPTIONID("h", "h", "g", "h"),
    ITEM_DESCRIPTIONID("a", "a", "a", "a"),

    BLOCKBASE_GETMAPCOLOR("s", "t", "s", "t"),

    MAPCOLOR_INTCOLOR("al", "ak", "ak", "ak"),


    DISPLAY_SETTRANSFORMATION(null, null, null, "a"),
    DISPLAY_CREATETRANSFORMATION(null, null, null, "a"),
    DISPLAY_SETINTERPOLATIONDURATION(null, null, null, "setInterpolationDuration_"),
    DISPLAY_GETINTERPOLATIONDURATION(null, null, null, "o"),
    DISPLAY_SETINTERPOLATIONSTART(null, null, null, "setInterpolationDelay_"),
    DISPLAY_GETINTERPOLATIONSTART(null, null, null, "p"),
    DISPLAY_SETBILLBOARD(null, null, null, "setBillboardConstraints_"),
    TEXTDISPLAY_SETTEXT(null, null, null, "c"),
    TEXTDISPLAY_GETTEXT(null, null, null, "o"),
    TEXTDISPLAY_GETLINEWIDTH(null, null, null, "p"),
    TEXTDISPLAY_SETLINEWIDTH(null, null, null, "b"),
    TEXTDISPLAY_SETTEXTOPACITY(null, null, null, "c"),
    TEXTDISPLAY_GETBACKFROUNDCOLOR(null, null, null, "s"),
    TEXTDISPLAY_SETBACKFROUNDCOLOR(null, null, null, "c"),
    TEXTDISPLAY_GETFLAGS(null, null, null, "q"),
    TEXTDISPLAY_SETFLAGS(null, null, null, "d"),
    TEXTDISPLAY_GETALIGNMENT(null, null, null, "a"),
    TEXTDISPLAY_SETBACKGROUND(null, null, null, "c"),
    TEXTDISPLAY_GETBACKGROUND(null, null, null, "s"),

    ENTITYTYPE_BYSTRING(null, "a", "a", "a")
    ;

    private static int versionId;

    static {
        init();
    }

    private static void init(){
        versionId = switch (Commons.getServerVersion()){
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
