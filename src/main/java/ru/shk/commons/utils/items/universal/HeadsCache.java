package ru.shk.commons.utils.items.universal;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.items.CachedPlayerProcessor;
import ru.shk.commons.utils.HTTPRequest;
import ru.shk.commons.utils.items.PlayerProcessor;
import ru.shk.commonsbungee.Commons;
import ru.shk.mysql.database.MySQL;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HeadsCache {
    @Getter@Setter@Accessors(fluent = true)
    private static MySQL mysql;
    private final ConcurrentLinkedQueue<Pair<Long, String>> cachedHeads = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Pair<UUID, String>> cachedHeadsFallback = new ConcurrentLinkedQueue<>();
    @Getter@Setter@Accessors(fluent = true)
    private static PlayerProcessor playerProcessor = null;

    static {
        try {
            Class.forName("land.shield.playerapi.CachedPlayer");
            playerProcessor = new CachedPlayerProcessor();
        } catch (ClassNotFoundException e) {}
    }

//    @Nullable
//    public String getPlayerTexture(String name){
//        CachedPlayer cp = CachedPlayer.of(name);
//        return cp.isPresent()?getPlayerTexture(cp):null;
//    }
//
//    @Nullable
//    public String getPlayerTexture(UUID uuid){
//        CachedPlayer cp = CachedPlayer.of(uuid);
//        return cp.isPresent()?getPlayerTexture(cp):null;
//    }
//
//    @Nullable
//    public String getPlayerTexture(CachedPlayer cp){
//        Pair<Integer, String> pair = cachedHeads.stream().filter(p -> p.getLeft()==cp.getId()).findAny().orElse(null);
//        String texture = pair==null?null:pair.getRight();
//        if(texture==null){
//            texture = getSkinTexture(cp);
//            if(texture!=null) {
//                cachedHeads.add(Pair.of(cp.getId(), texture));
//                while (cachedHeads.size()>200) cachedHeads.remove();
//            }
//        }
//        return texture;
//    }
//
//    @Nullable
//    public String getSkinTexture(CachedPlayer cp){
//        if(mysql==null || !mysql.isConnected()) return getSkinTextureFromMojang(cp.getUuid());
//        try (ResultSet rs = mysql.Query("SELECT texture FROM heads_texture_cache WHERE player_id="+cp.getId()+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;")) {
//            if (rs.next()) {
//                return rs.getString(1);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return null;
//        }
//        String texture = getSkinTextureFromMojang(cp.getUuid());
//        if(texture==null) return null;
//        mysql.Update("INSERT INTO heads_texture_cache SET player_id="+cp.getId()+", texture='"+texture+"', updated_at="+System.currentTimeMillis()+" ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
//        return texture;
//    }

    @Nullable
    public String getPlayerTexture(String name){
        if(playerProcessor==null) return null;
        try {
            return getPlayerTexture(playerProcessor.idFromName(name));
        } catch (Throwable t){
            return null;
        }
    }

    @Nullable
    public String getPlayerTexture(UUID uuid){
        if(playerProcessor==null) return getFallbackPlayerTexture(uuid);
        try {
            return getPlayerTexture(playerProcessor.idFromUUID(uuid));
        } catch (Throwable t){
            return null;
        }
    }

    @Nullable
    public String getPlayerTexture(long id){
        if(playerProcessor==null) return null;
        Pair<Long, String> pair = cachedHeads.stream().filter(p -> p.getLeft()==id).findAny().orElse(null);
        String texture = pair==null?null:pair.getRight();
        if(texture==null){
            texture = getSkinTexture(id);
            if(texture!=null) {
                cachedHeads.add(Pair.of(id, texture));
                while (cachedHeads.size()>200) cachedHeads.remove();
            }
        }
        return texture;
    }

    @Nullable
    private String getFallbackPlayerTexture(UUID uuid){
        Pair<UUID, String> pair = cachedHeadsFallback.stream().filter(p -> p.getLeft().equals(uuid)).findAny().orElse(null);
        String texture = pair==null?null:pair.getRight();
        if(texture==null){
            texture = getSkinTextureFromMojang(uuid);
            if(texture!=null){
                cachedHeadsFallback.add(Pair.of(uuid, texture));
                while (cachedHeadsFallback.size()>200) cachedHeadsFallback.remove();
            }
        }
        return texture;
    }

    @Nullable
    private String getSkinTexture(long id){
        UUID uuid = playerProcessor().UUIDFromId(id);
        if(mysql==null || !mysql.isConnected()) return getSkinTextureFromMojang(uuid);
        try (ResultSet rs = mysql.Query("SELECT texture FROM heads_texture_cache WHERE player_id="+id+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        String texture = getSkinTextureFromMojang(uuid);
        if(texture==null) return null;
        mysql.Update("INSERT INTO heads_texture_cache SET player_id="+id+", texture='"+texture+"', updated_at="+System.currentTimeMillis()+" ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
        return texture;
    }

    private static synchronized String getSkinTextureFromMojang(UUID uuid) {
        try {
            String trimmedUUID = uuid.toString().replace("-", "");
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/"+trimmedUUID+"?unsigned=false");
            JsonObject o = new HTTPRequest(url).get().asJson();
            return o.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
        } catch (Exception e){
            Commons.getInstance().warning(e.getMessage());
            return null;
        }
    }

    public int cacheSize(){
        return cachedHeadsFallback.size()+cachedHeads.size();
    }
}
