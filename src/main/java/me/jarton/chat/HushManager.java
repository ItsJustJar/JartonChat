package me.jarton.chat;

import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HushManager {

    public enum ChatType {
        STAFF,
        ADMIN,
        MANAGER
    }

    private final Map<UUID, EnumSet<ChatType>> hushed = new ConcurrentHashMap<>();

    public boolean isHushed(Player p, ChatType type) {
        if (p == null || type == null) return false;
        EnumSet<ChatType> set = hushed.get(p.getUniqueId());
        return set != null && set.contains(type);
    }

    public void setHushed(Player p, ChatType type, boolean value) {
        if (p == null || type == null) return;

        UUID uuid = p.getUniqueId();
        hushed.compute(uuid, (k, set) -> {
            EnumSet<ChatType> out = (set == null) ? EnumSet.noneOf(ChatType.class) : EnumSet.copyOf(set);
            if (value) out.add(type);
            else out.remove(type);
            return out.isEmpty() ? null : out;
        });
    }

    public void setAll(Player p, boolean value) {
        if (p == null) return;
        if (value) hushed.put(p.getUniqueId(), EnumSet.allOf(ChatType.class));
        else hushed.remove(p.getUniqueId());
    }

    public void clear(Player p) {
        if (p == null) return;
        hushed.remove(p.getUniqueId());
    }

    public Set<ChatType> getHushed(Player p) {
        if (p == null) return EnumSet.noneOf(ChatType.class);
        EnumSet<ChatType> set = hushed.get(p.getUniqueId());
        return set == null ? EnumSet.noneOf(ChatType.class) : EnumSet.copyOf(set);
    }
}
