package me.jarton.chat;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordBridge extends ListenerAdapter {

    private final JartonChat plugin;
    private final RanksManager ranksManager;
    private JDA jda;

    private String globalChannelId;
    private String staffChannelId;
    private String adminChannelId;
    private String managerChannelId;

    private boolean globalEnabled;
    private boolean staffEnabled;
    private boolean adminEnabled;
    private boolean managerEnabled;

    private String formatDiscordToMcGlobal;
    private String formatDiscordToMcStaff;
    private String formatDiscordToMcAdmin;
    private String formatDiscordToMcManager;

    private boolean webhookEnabled;

    private String globalWebhookUrl;
    private String staffWebhookUrl;
    private String adminWebhookUrl;
    private String managerWebhookUrl;

    private WebhookClient globalWebhook;
    private WebhookClient staffWebhook;
    private WebhookClient adminWebhook;
    private WebhookClient managerWebhook;

    private String statusChannelId;
    private boolean statusEnabled;
    private int statusIntervalTicks = 20 * 60;
    private String statusTitle;
    private String statusOnlineColorHex;
    private String statusOfflineColorHex;
    private boolean statusShowPlayerList;
    private String statusServerAddress;
    private String statusThumbnailUrl;
    private volatile Long statusMessageId = null;

    private File statusStateFile;
    private YamlConfiguration statusStateConfig;

    private static final int COLOR_AFK   = 0x3498DB;
    private static final int COLOR_JOIN  = 0x57F287;
    private static final int COLOR_LEAVE = 0xF39C12;
    private static final int COLOR_DEATH = 0xED4245;

    private static final Pattern AFK_NOW = Pattern.compile(
            "^([A-Za-z0-9_]{1,16})\\s+is\\s+now\\s+AFK(?:\\s*\\(idle\\))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AFK_NO_LONGER = Pattern.compile(
            "^([A-Za-z0-9_]{1,16})\\s+is\\s+no\\s+longer\\s+AFK$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JOIN = Pattern.compile(
            "^([A-Za-z0-9_]{1,16})\\s+(?:has\\s+)?joined\\s+the\\s+game$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LEAVE = Pattern.compile(
            "^([A-Za-z0-9_]{1,16})\\s+(?:has\\s+)?left\\s+the\\s+game$",
            Pattern.CASE_INSENSITIVE
    );

    public DiscordBridge(JartonChat plugin, RanksManager ranksManager) {
        this.plugin = plugin;
        this.ranksManager = ranksManager;

        try { LuckPermsProvider.get(); } catch (Throwable ignored) {}

        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "discord.yml");
        if (!file.exists()) {
            plugin.saveResource("discord.yml", false);
        }

        FileConfiguration cfg = plugin.loadOrSaveDefault("discord.yml", false);

        statusStateFile = new File(plugin.getDataFolder(), "status-state.yml");
        statusStateConfig = YamlConfiguration.loadConfiguration(statusStateFile);
        long storedId = statusStateConfig.getLong("status-message-id", 0L);
        if (storedId > 0) statusMessageId = storedId;

        String token = cfg.getString("bot.token");
        if (token == null || token.isBlank() || token.equalsIgnoreCase("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord bot token not set in discord.yml!");
            return;
        }

        globalChannelId = cfg.getString("channels.global", "");
        staffChannelId = cfg.getString("channels.staff", "");
        adminChannelId = cfg.getString("channels.admin", "");
        managerChannelId = cfg.getString("channels.manager", "");
        statusChannelId = cfg.getString("channels.status", "");

        globalEnabled = cfg.getBoolean("features.global-chat.enabled", true);
        staffEnabled = cfg.getBoolean("features.staff-chat.enabled", true);
        adminEnabled = cfg.getBoolean("features.admin-chat.enabled", true);
        managerEnabled = cfg.getBoolean("features.manager-chat.enabled", true);

        formatDiscordToMcGlobal = cfg.getString("features.global-chat.format.discord-to-mc",
                "&7[&bDiscord&7] &f%user%: &7%message%");
        formatDiscordToMcStaff = cfg.getString("features.staff-chat.format.discord-to-mc",
                " | %user%: &f%message%");
        formatDiscordToMcAdmin = cfg.getString("features.admin-chat.format.discord-to-mc",
                "&8[&6AdminChat&8] | %user%: &f%message%");
        formatDiscordToMcManager = cfg.getString("features.manager-chat.format.discord-to-mc",
                "&8[&eManagerChat&8] | %user%: &f%message%");

        webhookEnabled = cfg.getBoolean("features.webhook.enabled", true);

        globalWebhookUrl = cfg.getString("webhooks.global", "");
        staffWebhookUrl  = cfg.getString("webhooks.staff", "");
        adminWebhookUrl  = cfg.getString("webhooks.admin", "");
        managerWebhookUrl = cfg.getString("webhooks.manager", "");

        statusEnabled = cfg.getBoolean("features.status.enabled", false);
        statusTitle = cfg.getString("features.status.embed.title", "JartonMC Server Status");
        statusOnlineColorHex = cfg.getString("features.status.embed.online-color", "#57F287");
        statusOfflineColorHex = cfg.getString("features.status.embed.offline-color", "#ED4245");
        statusShowPlayerList = cfg.getBoolean("features.status.embed.show-player-list", true);
        statusServerAddress = cfg.getString("features.status.server-address", "mc.jarton.me");
        statusThumbnailUrl = cfg.getString("features.status.embed.thumbnail-url", null);

        int intervalSeconds = cfg.getInt("features.status.update-interval-seconds", 60);
        statusIntervalTicks = Math.max(20, intervalSeconds * 20);

        startBot(token);
    }

    private void saveStatusState() {
        if (statusStateConfig == null || statusStateFile == null) return;
        try {
            statusStateConfig.set("status-message-id", statusMessageId == null ? 0L : statusMessageId);
            statusStateConfig.save(statusStateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[DiscordBridge] Failed to save status-state.yml: " + e.getMessage());
        }
    }

    private void startBot(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS
                    )
                    .addEventListeners(this)
                    .build()
                    .awaitReady();

            plugin.getLogger().info("Discord bot connected successfully.");

            if (webhookEnabled) {
                setupWebhooks();
            }

            startStatusTask();

        } catch (InterruptedException e) {
            plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to start Discord bot: " + t.getMessage());
        }
    }

    private void setupWebhooks() {
        globalWebhook = buildWebhook(globalWebhookUrl, globalChannelId, "JartonChat-Global");
        staffWebhook  = buildWebhook(staffWebhookUrl, staffChannelId, "JartonChat-Staff");
        adminWebhook  = buildWebhook(adminWebhookUrl, adminChannelId, "JartonChat-Admin");
        managerWebhook = buildWebhook(managerWebhookUrl, managerChannelId, "JartonChat-Manager");
    }

    private WebhookClient buildWebhook(String url, String channelId, String hookName) {
        if (url != null && !url.isEmpty()) {
            plugin.getLogger().info("[DiscordBridge] Using provided webhook URL for " + hookName + ".");
            try {
                return new WebhookClientBuilder(url).build();
            } catch (Throwable t) {
                plugin.getLogger().warning("[DiscordBridge] Invalid webhook URL for " + hookName + ": " + t.getMessage());
                return null;
            }
        }

        if (channelId == null || channelId.isEmpty() || jda == null) return null;

        TextChannel ch = jda.getTextChannelById(channelId);
        if (ch == null) return null;

        try {
            Webhook hook = ch.retrieveWebhooks().complete().stream()
                    .filter(w -> w.getName().equals(hookName))
                    .findFirst()
                    .orElseGet(() -> ch.createWebhook(hookName).complete());
            return new WebhookClientBuilder(hook.getUrl()).build();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void startStatusTask() {
        if (!statusEnabled) {
            plugin.getLogger().info("[DiscordBridge] Status embed is disabled in config.");
            return;
        }
        if (jda == null) {
            plugin.getLogger().warning("[DiscordBridge] Cannot start status task: JDA is null.");
            return;
        }
        if (statusChannelId == null || statusChannelId.isEmpty()) {
            plugin.getLogger().warning("[DiscordBridge] Cannot start status task: channels.status not set.");
            return;
        }

        TextChannel statusChannel = jda.getTextChannelById(statusChannelId);
        if (statusChannel == null) {
            plugin.getLogger().warning("[DiscordBridge] Status channel ID is invalid or bot cannot see it.");
            return;
        }

        plugin.getLogger().info("[DiscordBridge] Starting status embed task every "
                + (statusIntervalTicks / 20) + " seconds.");

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                updateStatusEmbedAsync(statusChannel, true);
            } catch (Exception ex) {
                plugin.getLogger().warning("[DiscordBridge] Failed to update status embed: " + ex.getMessage());
            }
        }, 40L, statusIntervalTicks);
    }

    private EmbedBuilder buildStatusEmbed(boolean isOnline) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        String motdRaw = Bukkit.getMotd();
        String motd = motdRaw == null ? "" : ChatColor.stripColor(ColorUtil.color(motdRaw)).trim();

        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        EmbedBuilder eb = new EmbedBuilder();

        String titlePrefix = (statusTitle == null || statusTitle.isEmpty())
                ? "JartonMC Server Status"
                : statusTitle;
        String title = "**" + titlePrefix + ":** " + (isOnline ? "🟢 (ONLINE)" : "🔴 (OFFLINE)");
        eb.setTitle(title);

        eb.setTimestamp(Instant.now());
        eb.setColor(parseColor(isOnline ? statusOnlineColorHex : statusOfflineColorHex));

        if (statusThumbnailUrl != null && !statusThumbnailUrl.isEmpty()) {
            eb.setThumbnail(statusThumbnailUrl);
        }

        StringBuilder desc = new StringBuilder();

        if (statusServerAddress != null && !statusServerAddress.isEmpty()) {
            desc.append("**IP:** `").append(statusServerAddress).append("`\n");
        }

        desc.append("**Version:** 1.20.x - 1.21.x\n\n");

        if (isOnline) {
            if (online == 0) desc.append("");
            else if (online == 1) desc.append("1 player buzzing around 🐝");
            else desc.append(online).append(" players buzzing around 🐝");
        } else {
            desc.append("Server is currently offline.");
        }

        eb.setDescription(desc.toString());

        eb.addField("Status", isOnline ? "🟢 Online" : "🔴 Offline", true);
        eb.addField("Players", "👥 " + online + " / " + max, true);

        if (!motd.isEmpty()) {
            String motdField = "```" + motd + "```";
            eb.addField("MOTD", motdField, false);
        }

        if (statusShowPlayerList) {
            String list;
            if (!isOnline) list = "Server offline";
            else if (playerNames.isEmpty()) list = "No bees buzzing right now 😴";
            else if (playerNames.size() > 20) {
                List<String> first = playerNames.subList(0, 20);
                list = String.join(", ", first) + "\n…and " + (playerNames.size() - 20) + " more";
            } else {
                list = String.join(", ", playerNames);
            }
            eb.addField("Online Players", list, false);
        }

        return eb;
    }

    private void updateStatusEmbedAsync(TextChannel channel, boolean isOnline) {
        EmbedBuilder eb = buildStatusEmbed(isOnline);

        if (statusMessageId == null) {
            channel.sendMessageEmbeds(eb.build()).queue(msg -> {
                statusMessageId = msg.getIdLong();
                saveStatusState();
            });
        } else {
            channel.editMessageEmbedsById(statusMessageId, eb.build()).queue(
                    success -> {},
                    failure -> channel.sendMessageEmbeds(eb.build()).queue(msg -> {
                        statusMessageId = msg.getIdLong();
                        saveStatusState();
                    })
            );
        }
    }

    private void updateStatusEmbedBlocking(boolean isOnline) {
        if (!statusEnabled || jda == null || statusChannelId == null || statusChannelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(statusChannelId);
        if (channel == null) return;

        EmbedBuilder eb = buildStatusEmbed(isOnline);

        try {
            if (statusMessageId == null) statusMessageId = channel.sendMessageEmbeds(eb.build()).complete().getIdLong();
            else channel.editMessageEmbedsById(statusMessageId, eb.build()).complete();
            saveStatusState();
        } catch (Exception e) {
            plugin.getLogger().warning("[DiscordBridge] Failed to send blocking status: " + e.getMessage());
        }
    }

    private Color parseColor(String hex) {
        try {
            if (hex == null) return Color.GRAY;
            if (hex.startsWith("#")) return Color.decode(hex);
            return Color.decode("#" + hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    public void sendGlobalMessage(String displayName, String content) {
        if (!globalEnabled) return;

        String cleanName = ChatColor.stripColor(ColorUtil.color(displayName));
        String cleanContent = ChatColor.stripColor(ColorUtil.color(content)).trim();

        if (cleanContent.isEmpty()) return;

        Player p = Bukkit.getPlayerExact(cleanName);
        if (p == null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                String dn = ChatColor.stripColor(ColorUtil.color(online.getDisplayName()));
                if (dn != null && dn.equalsIgnoreCase(cleanName)) {
                    p = online;
                    break;
                }
            }
        }

        if (webhookEnabled && globalWebhook != null) {
            sendWebhook(globalWebhook, cleanName, cleanContent, p);
            return;
        }

        if (jda == null || globalChannelId == null || globalChannelId.isEmpty()) return;
        TextChannel channel = jda.getTextChannelById(globalChannelId);
        if (channel != null) {
            channel.sendMessage(cleanContent).queue();
        }
    }

    public void sendStaffChatWebhook(String displayName, String content, Player p) {
        sendStaffWebhook(displayName, content, p);
    }

    public void sendStaffWebhook(String displayName, String content, Player p) {
        if (!staffEnabled) return;
        if (staffWebhook != null && webhookEnabled) {
            String cleanName = ChatColor.stripColor(ColorUtil.color(displayName));
            String cleanContent = ChatColor.stripColor(ColorUtil.color(content)).trim();
            if (cleanContent.isEmpty()) return;
            sendWebhook(staffWebhook, cleanName, cleanContent, p);
        }
    }

    public void sendAdminChatWebhook(String displayName, String content, Player p) {
        if (!adminEnabled) return;
        if (adminWebhook != null && webhookEnabled) {
            String cleanName = ChatColor.stripColor(ColorUtil.color(displayName));
            String cleanContent = ChatColor.stripColor(ColorUtil.color(content)).trim();
            if (cleanContent.isEmpty()) return;
            sendWebhook(adminWebhook, cleanName, cleanContent, p);
        }
    }

    public void sendManagerChatWebhook(String displayName, String content, Player p) {
        if (!managerEnabled) return;
        if (managerWebhook != null && webhookEnabled) {
            String cleanName = ChatColor.stripColor(ColorUtil.color(displayName));
            String cleanContent = ChatColor.stripColor(ColorUtil.color(content)).trim();
            if (cleanContent.isEmpty()) return;
            sendWebhook(managerWebhook, cleanName, cleanContent, p);
        }
    }

    private void sendWebhook(WebhookClient client, String displayName, String content, Player p) {
        if (client == null) return;

        String safeContent = (content == null ? "" : content.trim());
        if (safeContent.isEmpty()) return;

        String avatarLink = getAvatarUrlForPlayer(p, (p != null ? p.getName() : null));

        String cleanName = (displayName == null ? "" : displayName.replaceAll("§.", "")).trim();
        if (cleanName.isEmpty()) cleanName = "JartonMC";

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(cleanName);
        builder.setAvatarUrl(avatarLink);
        builder.setContent(safeContent);

        client.send(builder.build());
    }

    private void sendTitleOnlyWebhook(WebhookClient client, String playerNameForAvatar, String title, int color) {
        String safeTitle = (title == null ? "" : title.trim());
        if (safeTitle.isEmpty()) return;

        if (safeTitle.length() > 256) safeTitle = safeTitle.substring(0, 253) + "...";

        String avatarLink = getAvatarUrlForPlayer(null, playerNameForAvatar);

        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(safeTitle, null))
                .setColor(color)
                .build();

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(playerNameForAvatar != null && !playerNameForAvatar.isBlank() ? playerNameForAvatar : "JartonMC");
        builder.setAvatarUrl(avatarLink);
        builder.setContent(" ");
        builder.addEmbeds(embed);

        client.send(builder.build());
    }

    private String getAvatarUrlForPlayer(Player p, String fallbackName) {
        if (p != null) {
            UUID uuid = p.getUniqueId();
            if (uuid.version() == 4) return "https://mc-heads.net/avatar/" + uuid;
            return "https://mc-heads.net/avatar/" + p.getName();
        }
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return "https://mc-heads.net/avatar/" + fallbackName;
        }
        return "https://mc-heads.net/avatar/Steve";
    }

    public void sendJoinLeaveTitle(String playerName, boolean joined) {
        if (!globalEnabled || !webhookEnabled || globalWebhook == null) return;
        String title = joined ? playerName + " joined the server" : playerName + " left the server";
        sendTitleOnlyWebhook(globalWebhook, playerName, title, joined ? COLOR_JOIN : COLOR_LEAVE);
    }

    private static class SystemLine {
        final String playerName;
        final String titleText;
        final int color;

        SystemLine(String playerName, String titleText, int color) {
            this.playerName = playerName;
            this.titleText = titleText;
            this.color = color;
        }
    }

    private Optional<SystemLine> parseSystemLine(String plainMessage) {
        if (plainMessage == null) return Optional.empty();
        String msg = plainMessage.trim();

        Matcher m1 = AFK_NOW.matcher(msg);
        if (m1.matches()) {
            String name = m1.group(1);
            return Optional.of(new SystemLine(name, name + " is now AFK", COLOR_AFK));
        }

        Matcher m2 = AFK_NO_LONGER.matcher(msg);
        if (m2.matches()) {
            String name = m2.group(1);
            return Optional.of(new SystemLine(name, name + " is no longer AFK", COLOR_AFK));
        }

        Matcher m3 = JOIN.matcher(msg);
        if (m3.matches()) {
            String name = m3.group(1);
            return Optional.of(new SystemLine(name, name + " joined the server", COLOR_JOIN));
        }

        Matcher m4 = LEAVE.matcher(msg);
        if (m4.matches()) {
            String name = m4.group(1);
            return Optional.of(new SystemLine(name, name + " left the server", COLOR_LEAVE));
        }

        return Optional.empty();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMember() == null) return;

        String channelId = event.getChannel().getId();
        String discordNick = event.getMember().getEffectiveName();

        Message msg = event.getMessage();
        StringBuilder content = new StringBuilder(msg.getContentDisplay());

        if (!msg.getAttachments().isEmpty()) {
            String attachments = msg.getAttachments().stream()
                    .map(Message.Attachment::getUrl)
                    .collect(Collectors.joining(" "));
            if (content.length() > 0) content.append(" ");
            content.append(attachments);
        }

        String message = content.toString().trim();
        if (message.isEmpty()) message = "[No Content]";

        Role topRole = event.getMember().getRoles().stream()
                .filter(r -> r.getColor() != null)
                .findFirst()
                .orElse(null);

        String roleName  = (topRole != null) ? topRole.getName() : "Member";
        String roleColor = (topRole != null && topRole.getColor() != null)
                ? hexToMinecraft(topRole.getColor())
                : "§7";

        String userFormatted = roleColor + roleName + " §f| " + discordNick;

        if (globalEnabled && globalChannelId != null && !globalChannelId.isEmpty() && channelId.equals(globalChannelId)) {
            String formatted = formatDiscordToMcGlobal
                    .replace("%user%", userFormatted)
                    .replace("%message%", "§7" + message);
            plugin.getChatService().broadcastDiscordMessage(formatted);
        }

        if (staffEnabled && staffChannelId != null && !staffChannelId.isEmpty() && channelId.equals(staffChannelId)) {
            String formatted = formatDiscordToMcStaff
                    .replace("%user%", userFormatted + " ")
                    .replace("%message%", message);

            if (plugin.getStaffChatManager() != null) {
                plugin.getStaffChatManager().sendStaffMessageFromDiscord(formatted, message);
            }
        }

        if (adminEnabled && adminChannelId != null && !adminChannelId.isEmpty() && channelId.equals(adminChannelId)) {
            String formatted = formatDiscordToMcAdmin
                    .replace("%user%", userFormatted + " ")
                    .replace("%message%", message);
            if (plugin.getAdminChatManager() != null) {
                plugin.getAdminChatManager().sendAdminMessageFromDiscord(formatted, message);
            }
        }

        if (managerEnabled && managerChannelId != null && !managerChannelId.isEmpty() && channelId.equals(managerChannelId)) {
            String formatted = formatDiscordToMcManager
                    .replace("%user%", userFormatted + " ")
                    .replace("%message%", message);
            if (plugin.getManagerChatManager() != null) {
                plugin.getManagerChatManager().sendManagerMessageFromDiscord(formatted, message);
            }
        }
    }

    public void sendDeathMessage(String player, String deathMessage) {
        if (!globalEnabled || jda == null) return;

        String clean = ChatColor.stripColor(ColorUtil.color(deathMessage)).trim();
        if (clean.isEmpty()) return;

        Player p = Bukkit.getPlayerExact(player);

        if (webhookEnabled && globalWebhook != null) {
            String playerName = (p != null ? p.getName() : player);
            sendTitleOnlyWebhook(globalWebhook, playerName, clean, COLOR_DEATH);
        } else {
            TextChannel channel = (globalChannelId == null || globalChannelId.isEmpty()) ? null : jda.getTextChannelById(globalChannelId);
            if (channel != null) channel.sendMessage(clean).queue();
        }
    }

    public void shutdown() {
        try { updateStatusEmbedBlocking(false); } catch (Exception ignored) {}

        if (jda != null) {
            try { jda.shutdown(); } catch (Throwable ignored) {}
        }
        if (globalWebhook != null) globalWebhook.close();
        if (staffWebhook != null) staffWebhook.close();
        if (adminWebhook != null) adminWebhook.close();
        if (managerWebhook != null) managerWebhook.close();
    }

    private String hexToMinecraft(Color color) {
        String hex = String.format("%06X", color.getRGB() & 0xFFFFFF);
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }
}
