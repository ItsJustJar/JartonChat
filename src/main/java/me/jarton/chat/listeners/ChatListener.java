package me.jarton.chat.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jarton.chat.AdminChatManager;
import me.jarton.chat.ChatService;
import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;
import me.jarton.chat.ManagerChatManager;
import me.jarton.chat.RanksManager;
import me.jarton.chat.StaffChatManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {
    private final JartonChat plugin;
    private final ChatService chatService;
    private final StaffChatManager staffChatManager;
    private final AdminChatManager adminChatManager;
    private final ManagerChatManager managerChatManager;
    private final CooldownManager cooldownManager = new DefaultCooldownManager();

    private static final LegacyComponentSerializer AMP =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static final String SHOW_INV_CMD = "/jartonchat_showinv";
    private static final String INV_TITLE_PREFIX = "\u00A7b";
    private static final String CLICKABLE_NAME_TOKEN = "|||CLICKABLE_NAME|||";
    private final Set<Inventory> viewOnlyInventories = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatListener(JartonChat plugin, ChatService chatService, StaffChatManager staffChatManager,
                        AdminChatManager adminChatManager, ManagerChatManager managerChatManager) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.staffChatManager = staffChatManager;
        this.adminChatManager = adminChatManager;
        this.managerChatManager = managerChatManager;
    }

    private static boolean containsItemToken(String text) {
        return text.contains("[item]") || text.contains("[i]");
    }

    private enum PrivateChatRoute {
        NONE,
        STAFF,
        ADMIN,
        MANAGER
    }

    private PrivateChatRoute resolvePrivateChatRoute(Player player, String message) {
        if (staffChatManager != null && staffChatManager.isPrefixEnabled() && message.startsWith(staffChatManager.getPrefixSymbol())) {
            return PrivateChatRoute.STAFF;
        }
        if (adminChatManager != null && adminChatManager.isPrefixEnabled() && message.startsWith(adminChatManager.getPrefixSymbol())) {
            return PrivateChatRoute.ADMIN;
        }
        if (managerChatManager != null && managerChatManager.isPrefixEnabled() && message.startsWith(managerChatManager.getPrefixSymbol())) {
            return PrivateChatRoute.MANAGER;
        }

        if (staffChatManager != null && staffChatManager.isInStaffChat(player)) {
            return PrivateChatRoute.STAFF;
        }
        if (adminChatManager != null && adminChatManager.isInAdminChat(player)) {
            return PrivateChatRoute.ADMIN;
        }
        if (managerChatManager != null && managerChatManager.isInManagerChat(player)) {
            return PrivateChatRoute.MANAGER;
        }
        return PrivateChatRoute.NONE;
    }

    private static String stripPrefix(String message, String prefix) {
        if (prefix == null || prefix.isEmpty()) return message;
        if (message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return message;
    }

    private boolean routePrivateChatIfNeeded(AsyncChatEvent event, Player player, String rawMessage, PrivateChatRoute route) {
        if (route == PrivateChatRoute.NONE) return false;

        String message = rawMessage;
        event.setCancelled(true);

        if (route == PrivateChatRoute.STAFF) {
            if (staffChatManager == null) return true;
            if (staffChatManager.isPrefixEnabled()) {
                message = stripPrefix(message, staffChatManager.getPrefixSymbol());
            }
            if (!player.hasPermission("jartonchat.staffchat.use")) {
                staffChatManager.setStaffChatEnabled(player, false);
                player.sendMessage(ChatColor.RED + "You don't have permission to use staffchat.");
                return true;
            }
            if (message.isBlank()) return true;
            staffChatManager.sendStaffMessage(player, message);
            return true;
        }

        if (route == PrivateChatRoute.ADMIN) {
            if (adminChatManager == null) return true;
            if (adminChatManager.isPrefixEnabled()) {
                message = stripPrefix(message, adminChatManager.getPrefixSymbol());
            }
            if (!player.hasPermission("jartonchat.adminchat.use")) {
                adminChatManager.setAdminChatEnabled(player, false);
                player.sendMessage(ChatColor.RED + "You don't have permission to use adminchat.");
                return true;
            }
            if (message.isBlank()) return true;
            adminChatManager.sendAdminMessage(player, message);
            return true;
        }

        if (managerChatManager == null) return true;
        if (managerChatManager.isPrefixEnabled()) {
            message = stripPrefix(message, managerChatManager.getPrefixSymbol());
        }
        if (!managerChatManager.canUseManagerChat(player)) {
            managerChatManager.setManagerChatEnabled(player, false);
            player.sendMessage(ChatColor.RED + "You don't have permission to use ManagerChat.");
            return true;
        }
        if (message.isBlank()) return true;
        managerChatManager.sendManagerMessage(player, message);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!plugin.chatEnabled) return;

        final Player player = event.getPlayer();
        Component original = event.message();
        String message = PLAIN.serialize(original);

        PrivateChatRoute route = resolvePrivateChatRoute(player, message);
        if (routePrivateChatIfNeeded(event, player, message, route)) {
            return;
        }

        if (!player.hasPermission("jarton.exemptcooldown")) {
            long remaining = cooldownManager.getRemainingTime(player.getUniqueId());

            if (remaining > 0) {
                double sec = remaining / 1000.0;
                player.sendMessage(ChatColor.RED + "You must wait " +
                        String.format("%.1f", sec) + "s before chatting again.");
                event.setCancelled(true);
                return;
            }

            cooldownManager.resetCooldown(player.getUniqueId());
        }

        if (containsItemToken(message)) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Hold an item in your hand before using " + ChatColor.WHITE + "[i]" + ChatColor.RED + " / " + ChatColor.WHITE + "[item]" + ChatColor.RED + ".");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                return;
            }
        }

        RanksManager ranks = plugin.getRanksManager();

        boolean staffOnly = plugin.staffOnlyDisplay.stream()
                .anyMatch(n -> n.equalsIgnoreCase(player.getName()));

        List<String> pieces = new ArrayList<>();
        String paid = "";
        String rankup = "";
        String staff = "";

        if (staffOnly) {
            staff = ranks.getDisplayForTrack(player, "staff");
            if (!staff.isEmpty()) pieces.add(staff);
        } else {
            paid = ranks.getDisplayForTrack(player, "paid");
            rankup = ranks.getDisplayForTrack(player, "rankup");
            staff = ranks.getDisplayForTrack(player, "staff");

            if (!paid.isEmpty()) pieces.add(paid);
            if (!rankup.isEmpty()) pieces.add(rankup);
            if (!staff.isEmpty()) pieces.add(staff);
        }

        String sep = plugin.chatSeparator;
        String ranksPart = String.join(sep, pieces);
        if (!ranksPart.isEmpty()) ranksPart = ranksPart + sep;

        String nameColor = ranks.getRankNameColor(player);
        String baseMessageColor = ranks.getRankMessageColor(player);

        String messageColor = plugin.getChatColorManager()
                .getEffectiveMessageColor(player, baseMessageColor);
        if (messageColor == null || messageColor.isEmpty()) {
            messageColor = baseMessageColor;
        }

        if (plugin.censorEnabled) {
            message = chatService.censor(player, message);
        }

        String legacyMessageColor = messageColor.replace('\u00A7', '&');

        if (plugin.emojisEnabled) {
            message = chatService.replaceEmojis(player, message, legacyMessageColor);
        }

        Player mentioned = chatService.findMentioned(message);
        if (mentioned != null) {
            message = chatService.highlightMention(message, mentioned, legacyMessageColor);
            mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }

        String rawDisplayName = player.getDisplayName();
        boolean hasNick = !rawDisplayName.equals(player.getName());
        String normalizedDisplayName = rawDisplayName.replace('\u00A7', '&');

        String namePart = hasNick ? normalizedDisplayName : (nameColor + normalizedDisplayName);

        String nameWithToken = CLICKABLE_NAME_TOKEN + namePart;

        String format = plugin.chatLineFormat;
        String builtLine = format
                .replace("{paid}",     paid)
                .replace("{rankup}",   rankup)
                .replace("{staff}",    staff)
                .replace("{sep}",      sep)
                .replace("{ranks}",    ranksPart)
                .replace("{name}",     nameWithToken)
                .replace("{message}",  legacyMessageColor + message);

        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

        String coloredLine = ColorUtil.color(builtLine);
        String finalLine = coloredLine;

        if (papi != null && papi.isEnabled()) {
            try {
                finalLine = PlaceholderAPI.setPlaceholders(player, coloredLine);
            } catch (Throwable ignored) { }
        }
        String shadowMuteSentinel = "\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7\u00A7";
        boolean shadowMuted = message.endsWith(shadowMuteSentinel);
        final String template = finalLine.replace(shadowMuteSentinel, "").replace('\u00A7', '&');

        event.renderer((source, displayName, msg, viewer) -> {
            String perViewer = template;
            if (papi != null && papi.isEnabled() && viewer instanceof Player v) {
                try {
                    perViewer = PlaceholderAPI.setRelationalPlaceholders(v, source, perViewer);
                } catch (Throwable ignored) { }
            }
            return buildRichChatComponent(perViewer, source);
        });

        if (shadowMuted)
            return;

        try {
            String discordMsg = ChatColor.stripColor(message).trim();
            if (!discordMsg.isEmpty() && plugin.getDiscordBridge() != null) {
                plugin.getDiscordBridge().sendGlobalMessage(player.getName(), discordMsg);
            }
        } catch (Throwable ignored) {}
    }

    private Component buildRichChatComponent(String text, Player source) {
        boolean hasNameToken = text.contains(CLICKABLE_NAME_TOKEN);
        boolean hasOtherTokens = containsToken(text);

        if (!hasNameToken && !hasOtherTokens) {
            return AMP.deserialize(text);
        }

        TextComponent.Builder out = Component.text();
        int idx = 0;

        while (idx < text.length()) {
            int nameTokenPos = text.indexOf(CLICKABLE_NAME_TOKEN, idx);
            int i1 = text.indexOf("[item]", idx);
            int i2 = text.indexOf("[i]", idx);
            int v1 = text.indexOf("[inv]", idx);
            int v2 = text.indexOf("[inventory]", idx);

            int next = minPositive(minPositive(minPositive(i1, i2), minPositive(v1, v2)), nameTokenPos);

            if (next == -1) {
                out.append(AMP.deserialize(text.substring(idx)));
                break;
            }

            if (next > idx) {
                out.append(AMP.deserialize(text.substring(idx, next)));
            }

            if (next == nameTokenPos) {
                idx = next + CLICKABLE_NAME_TOKEN.length();
                int nextTokenAfterName = idx;
                int nextItem1 = text.indexOf("[item]", idx);
                int nextItem2 = text.indexOf("[i]", idx);
                int nextInv1 = text.indexOf("[inv]", idx);
                int nextInv2 = text.indexOf("[inventory]", idx);
                int nameEnd = minPositive(minPositive(minPositive(nextItem1, nextItem2), minPositive(nextInv1, nextInv2)),
                                          text.indexOf(":", idx)); // Name usually ends with ":"

                if (nameEnd == -1) nameEnd = text.length();

                String namePartWithFormat = text.substring(idx, nameEnd);
                out.append(createClickableNameComponent(namePartWithFormat, source));
                idx = nameEnd;
            } else if (next == i1 || next == i2) {
                out.append(itemComponent(source));
                idx = next + (next == i1 ? "[item]".length() : "[i]".length());
            } else {
                out.append(inventoryComponent(source));
                idx = next + (next == v1 ? "[inv]".length() : "[inventory]".length());
            }
        }

        return out.build();
    }

    private static int minPositive(int a, int b) {
        if (a == -1) return b;
        if (b == -1) return a;
        return Math.min(a, b);
    }

    private static boolean containsToken(String text) {
        return text.contains("[item]") || text.contains("[i]") || text.contains("[inv]") || text.contains("[inventory]");
    }

    private Component itemComponent(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean empty = (hand == null || hand.getType().isAir());
        if (empty) {
            return AMP.deserialize("&7[no item]")
                    .hoverEvent(HoverEvent.showText(Component.text("Empty hand", NamedTextColor.GRAY)));
        }
        Component name = hand.displayName();
        return name.hoverEvent(hand.asHoverEvent());
    }

    private Component inventoryComponent(Player player) {
        String username = player.getName();
        Component label = AMP.deserialize("&b" + username + "'s Inventory");
        Component hover = AMP.deserialize("&bClick to view &f" + username + "'s Inventory");
        String cmd = SHOW_INV_CMD + " " + player.getUniqueId();
        return label.hoverEvent(HoverEvent.showText(hover)).clickEvent(ClickEvent.runCommand(cmd));
    }

    private Component createClickableNameComponent(String nameWithFormatting, Player source) {
        if (!plugin.clickableNameEnabled) {
            return AMP.deserialize(nameWithFormatting);
        }

        String playerName = source.getName();

        String command = plugin.clickableNameCommand.replace("{player}", playerName);
        String hoverText = plugin.clickableNameHoverText.replace("{player}", playerName);

        Component hoverComponent = AMP.deserialize(hoverText);

        ClickEvent clickEvent = ClickEvent.runCommand(command);

        return AMP.deserialize(nameWithFormatting)
                .hoverEvent(HoverEvent.showText(hoverComponent))
                .clickEvent(clickEvent);
    }

    private boolean handleHiddenChannelCommand(PlayerCommandPreprocessEvent e) {
        String raw = e.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') return false;

        String[] split = raw.substring(1).split("\\s+", 2);
        if (split.length == 0) return false;

        String label = split[0].toLowerCase(Locale.ROOT);
        int namespaceIdx = label.indexOf(':');
        if (namespaceIdx >= 0 && namespaceIdx < label.length() - 1) {
            label = label.substring(namespaceIdx + 1);
        }
        String message = split.length > 1 ? split[1].trim() : "";
        Player player = e.getPlayer();

        if ("ac".equals(label)) {
            e.setMessage(message.isEmpty() ? "/ac" : "/ac <hidden>");
            e.setCancelled(true);
            if (!player.hasPermission("jartonchat.adminchat.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use adminchat.");
                return true;
            }
            if (message.isEmpty()) {
                plugin.getAdminChatManager().toggleAdminChat(player);
            } else {
                plugin.getAdminChatManager().sendAdminMessage(player, message);
            }
            return true;
        }

        if ("mc".equals(label)) {
            e.setMessage(message.isEmpty() ? "/mc" : "/mc <hidden>");
            e.setCancelled(true);
            if (!plugin.getManagerChatManager().canUseManagerChat(player)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use ManagerChat.");
                plugin.getManagerChatManager().setManagerChatEnabled(player, false);
                return true;
            }
            if (message.isEmpty()) {
                plugin.getManagerChatManager().toggleManagerChat(player);
            } else {
                plugin.getManagerChatManager().sendManagerMessage(player, message);
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        if (handleHiddenChannelCommand(e)) return;

        String msg = e.getMessage();
        if (!msg.startsWith(SHOW_INV_CMD)) return;
        e.setCancelled(true);
        String[] parts = msg.split("\\s+");
        if (parts.length < 2) return;
        UUID targetId;
        try {
            targetId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        Player viewer = e.getPlayer();
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "That player is no longer online.");
            return;
        }
        openViewOnlyInventory(viewer, target);
    }

    private void openViewOnlyInventory(Player viewer, Player target) {
        String title = INV_TITLE_PREFIX + target.getName() + "'s Inventory";
        Inventory inv = Bukkit.createInventory(null, 54, AMP.deserialize(title));
        ItemStack[] storage = target.getInventory().getStorageContents();
        int idx = 0;
        for (ItemStack stack : storage) {
            if (idx >= inv.getSize()) break;
            if (stack != null) inv.setItem(idx, stack.clone());
            idx++;
        }
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (idx >= inv.getSize()) break;
            if (piece != null) inv.setItem(idx, piece.clone());
            idx++;
        }
        ItemStack off = target.getInventory().getItemInOffHand();
        if (idx < inv.getSize() && off != null) {
            inv.setItem(idx, off.clone());
            idx++;
        }
        if (idx < inv.getSize()) {
            ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta meta = xpBottle.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Experience: " +
                    ChatColor.WHITE + target.getTotalExperience());
            xpBottle.setItemMeta(meta);
            inv.setItem(idx, xpBottle);
        }
        viewOnlyInventories.add(inv);
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        if (e.getView().getType() == InventoryType.CRAFTING) return;
        if (viewOnlyInventories.contains(top)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null)
            return;
        if (viewOnlyInventories.contains(top)) {
            e.setCancelled(true);
        }
    }

    public interface CooldownManager {
        long getRemainingTime(UUID playerId);
        void resetCooldown(UUID playerId);
    }

    public class DefaultCooldownManager implements CooldownManager {
        private static final long COOLDOWN_MS = 2000;

        private final Map<UUID, Long> lastMessageTimes = new HashMap<>();

        @Override
        public long getRemainingTime(UUID playerId) {
            var lastMessageTime = lastMessageTimes.get(playerId);
            if (lastMessageTime == null) {
                return 0;
            }

            var remaining = COOLDOWN_MS + lastMessageTime - System.currentTimeMillis();
            return Math.max(0, remaining);
        }

        @Override
        public void resetCooldown(UUID playerId) {
            lastMessageTimes.put(playerId, System.currentTimeMillis());
        }
    }
}
