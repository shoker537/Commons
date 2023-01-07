package ru.shk.commons.utils.items.universal;

import com.google.gson.JsonObject;
import land.shield.playerapi.CachedPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.HTTPRequest;
import ru.shk.commonsbungee.Commons;
import ru.shk.mysql.database.MySQL;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HeadsCache {
    @Getter@Setter@Accessors(fluent = true)
    private static MySQL mysql;
    private final ConcurrentLinkedQueue<Pair<Integer, String>> cachedHeads = new ConcurrentLinkedQueue<>();

    @Nullable
    public String getPlayerTexture(String name){
        CachedPlayer cp = CachedPlayer.of(name);
        return cp.isPresent()?getPlayerTexture(cp):null;
    }

    @Nullable
    public String getPlayerTexture(UUID uuid){
        CachedPlayer cp = CachedPlayer.of(uuid);
        return cp.isPresent()?getPlayerTexture(cp):null;
    }

    @Nullable
    public String getPlayerTexture(CachedPlayer cp){
        Pair<Integer, String> pair = cachedHeads.stream().filter(p -> p.getLeft()==cp.getId()).findAny().orElse(null);
        String texture = pair==null?null:pair.getRight();
        if(texture==null){
            texture = getSkinTexture(cp);
            if(texture!=null) {
                cachedHeads.add(Pair.of(cp.getId(), texture));
                while (cachedHeads.size()>200) cachedHeads.remove();
            }
        }
        return texture;
    }

    @Nullable
    public String getSkinTexture(CachedPlayer cp){
        if(mysql==null || !mysql.isConnected()) return getSkinTextureFromMojang(cp.getUuid());
        try (ResultSet rs = mysql.Query("SELECT texture FROM heads_texture_cache WHERE player_id="+cp.getId()+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        String texture = getSkinTextureFromMojang(cp.getUuid());
        if(texture==null) return null;
        mysql.Update("INSERT INTO heads_texture_cache SET player_id="+cp.getId()+", texture='"+texture+"', updated_at="+System.currentTimeMillis()+" ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
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
}
