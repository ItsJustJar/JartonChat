package me.jarton.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.jarton.chat.commands.BcCommand;
import me.jarton.chat.commands.ChatColorCommand;
import me.jarton.chat.commands.ClearChatCommand;
import me.jarton.chat.commands.EmojiReloadCommand;
import me.jarton.chat.commands.MsgCommand;
import me.jarton.chat.commands.MuteChatCommand;
import me.jarton.chat.commands.MuteCommand;
import me.jarton.chat.commands.ReloadCommand;
import me.jarton.chat.commands.ReplyCommand;
import me.jarton.chat.commands.ScCommand;
import me.jarton.chat.commands.ScReloadCommand;
import me.jarton.chat.commands.ScToggleCommand;
import me.jarton.chat.commands.ScToggleSoundCommand;
import me.jarton.chat.commands.UnmuteCommand;

import me.jarton.chat.commands.CommandSpyCommand;
import me.jarton.chat.commands.SocialSpyCommand;
import me.jarton.chat.listeners.SpyListener;

import me.jarton.chat.listeners.ChatListener;
import me.jarton.chat.listeners.DeathListener;
import me.jarton.chat.listeners.JoinQuitListener;
import me.jarton.chat.playerresolution.DisplayNamePlayerComparator;
import me.jarton.chat.playerresolution.PartialNickPlayerResolver;
import me.jarton.chat.players.yaml.ChatPlayerConfigurationProvider;
import me.jarton.chat.players.yaml.YamlChatPlayerRepository;

import me.jarton.chat.commands.HushCommand;
import me.jarton.chat.commands.AcCommand;
import me.jarton.chat.commands.ManagerChatCommand;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JartonChat extends JavaPlugin {

    private static JartonChat instance;
    public static JartonChat get() { return instance; }

    public boolean chatEnabled;
    public boolean censorEnabled;
    public boolean antiSpamEnabled;
    public boolean emojisEnabled;
    public boolean builtInDeathMessages;
    public boolean chatSound;
    public boolean cmdArgSound;

    public final Set<String> bannedWords = ConcurrentHashMap.newKeySet();
    public final List<String> mildConcern = new ArrayList<>();

    public final List<String> joinMessages = new ArrayList<>();
    public final List<String> quitMessages = new ArrayList<>();
    public final List<String> combatQuitMessages = new ArrayList<>();
    public final Map<String, String> messageMap = new HashMap<>();
    public final Map<String, List<String>> broadcasts = new LinkedHashMap<>();
    public boolean broadcastsEnabled = true;
    public int broadcastsSeconds = 900;

    public final Map<String, String> emojiMap = new LinkedHashMap<>();

    public String pmFormatSender = "&6&lME &e-> &eRECIPENT&f: MESSAGE";
    public String pmFormatRecipient = "&eSENDER &e-> &6&lME&f: MESSAGE";

    public String chatLineFormat = "{ranks}&f{name}&f: &7{message}";
    public String chatSeparator = "&7 | ";
    public List<String> staffOnlyDisplay = new ArrayList<>();

    public boolean clickableNameEnabled = true;
    public String clickableNameCommand = "/friend {player}";
    public String clickableNameHoverText = "&eClick to manage friendship with &f{player}";

    private String adminChatFormatIngame = "&8[&6AdminChat&8] &7%player%: &f%message%";

    private MuteStorage muteStorage;
    private ChatService chatService;
    private AutoBroadcastTask broadcastTask;
    private RanksManager ranksManager;

    private StaffChatManager staffChatManager;
    private AdminChatManager adminChatManager;
    private ManagerChatManager managerChatManager;

    private DiscordBridge discordBridge;

    private ChatColorManager chatColorManager;
    private ChatPlayerConfigurationProvider chatPlayerConfigurationProvider;
    private YamlChatPlayerRepository chatPlayerRepository;

    private HushManager hushManager;

    public YamlConfiguration loadOrSaveDefault(String fileName, boolean overwrite) {
        File file = new File(getDataFolder(), fileName);
        if (overwrite || !file.exists()) {
            saveResource(fileName, true);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void loadConfigFiles() {
        FileConfiguration cfg = loadOrSaveDefault("config.yml", false);
        chatEnabled = cfg.getBoolean("chat.enabled", true);
        censorEnabled = cfg.getBoolean("chat.censor", true);
        antiSpamEnabled = cfg.getBoolean("chat.anti_spam", true);
        emojisEnabled = cfg.getBoolean("chat.emojis", true);
        builtInDeathMessages = cfg.getBoolean("chat.built_in_death_messages", false);
        chatSound = cfg.getBoolean("chat.chat_sound", true);
        cmdArgSound = cfg.getBoolean("chat.command_argument_sound", false);

        emojiMap.clear();
        YamlConfiguration emojisCfg = loadOrSaveDefault("emojis.yml", false);
        if (emojisCfg.isConfigurationSection("emojis")) {
            for (String key : emojisCfg.getConfigurationSection("emojis").getKeys(false)) {
                String val = emojisCfg.getString("emojis." + key, "");
                if (val != null) emojiMap.put(key, val);
            }
        }

        bannedWords.clear();
        YamlConfiguration bw = loadOrSaveDefault("banned-words.yml", false);
        for (String s : bw.getStringList("banned-words")) {
            if (s != null) bannedWords.add(s.toLowerCase(Locale.ROOT));
        }

        YamlConfiguration msgs = loadOrSaveDefault("messages.yml", false);
        joinMessages.clear();
        quitMessages.clear();
        combatQuitMessages.clear();
        messageMap.clear();
        joinMessages.addAll(msgs.getStringList("join_messages"));
        quitMessages.addAll(msgs.getStringList("quit_messages"));
        combatQuitMessages.addAll(msgs.getStringList("combat_quit_messages"));
        if (msgs.isConfigurationSection("messages")) {
            for (String k : msgs.getConfigurationSection("messages").getKeys(false)) {
                messageMap.put(k, msgs.getString("messages." + k, ""));
            }
        }

        pmFormatSender = msgs.getString("private-messages.format.sender",
                msgs.getString("private-messages.format.to-sender",
                        msgs.getString("pm-format.sender", "&6&lME &e-> &e{recipient}&f: {message}")));
        pmFormatRecipient = msgs.getString("private-messages.format.recipient",
                msgs.getString("private-messages.format.to-recipient",
                        msgs.getString("pm-format.recipient", "&e{sender} &e-> &6&lME&f: {message}")));

        clickableNameEnabled = msgs.getBoolean("clickable_name.enabled", true);
        clickableNameCommand = msgs.getString("clickable_name.command", "/friend {player}");
        clickableNameHoverText = msgs.getString("clickable_name.hover_text", "&eClick to manage friendship with &f{player}");

        YamlConfiguration mc = loadOrSaveDefault("messages-of-mild-concern.yml", false);
        mildConcern.clear();
        mildConcern.addAll(mc.getStringList("messages-of-mild-concern"));
        if (mildConcern.isEmpty()) mildConcern.add("&7[Tip] Let's keep it friendly.");

        YamlConfiguration fmt = loadOrSaveDefault("chat-format.yml", false);
        chatLineFormat = fmt.getString("chat-format.format", chatLineFormat);
        chatSeparator = fmt.getString("separator", chatSeparator);
        staffOnlyDisplay = fmt.getStringList("staff-only-display");

        broadcasts.clear();
        YamlConfiguration ab = loadOrSaveDefault("auto-broadcast.yml", false);
        broadcastsEnabled = ab.getBoolean("options.enabled", true);
        broadcastsSeconds = ab.getInt("options.time", 900);
        if (ab.isConfigurationSection("broadcasts")) {
            for (String key : ab.getConfigurationSection("broadcasts").getKeys(false)) {
                if (ab.getBoolean("broadcasts." + key + ".enabled", true)) {
                    broadcasts.put(key, ab.getStringList("broadcasts." + key + ".message"));
                }
            }
        }

        File adminFile = new File(getDataFolder(), "adminchat.yml");
        if (!adminFile.exists()) {
            saveResource("adminchat.yml", false);
        }
        YamlConfiguration adminCfg = YamlConfiguration.loadConfiguration(adminFile);
        adminChatFormatIngame = adminCfg.getString("formatting.ingame", adminChatFormatIngame);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfigFiles();

        this.hushManager = new HushManager();


        this.muteStorage = new MuteStorage(this);
        this.muteStorage.load();

        this.chatService = new ChatService(this);
        this.ranksManager = new RanksManager(this);

        try {
            setupChatPlayerRepository();
        } catch (IOException ex) {
            getLogger().severe("Unable to initialize YamlChatPlayerRepository: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.chatColorManager = new ChatColorManager(chatPlayerRepository);

        this.discordBridge = new DiscordBridge(this, ranksManager);

        this.staffChatManager = new StaffChatManager(this);
        this.managerChatManager = new ManagerChatManager(this);
        this.adminChatManager = new AdminChatManager(this, discordBridge);

        Bukkit.getPluginManager().registerEvents(
                new ChatListener(this, chatService, staffChatManager, adminChatManager, managerChatManager),
                this
        );
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this, chatService), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);

        getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("mutechat").setExecutor(new MuteChatCommand(this));
        getCommand("msg").setExecutor(new MsgCommand(this, chatService, new PartialNickPlayerResolver(new DisplayNamePlayerComparator())));
        getCommand("reply").setExecutor(new ReplyCommand(this, chatService));
        getCommand("jartonchat").setExecutor(new ReloadCommand(this));

        BcCommand bcCommand = new BcCommand(this);
        getCommand("jcbc").setExecutor(bcCommand);
        getCommand("jcbc").setTabCompleter(bcCommand);

        getCommand("sc").setExecutor(new ScCommand(staffChatManager));
        getCommand("sc").setTabCompleter(new StaffChatTabCompleter());

        getCommand("sctoggle").setExecutor(new ScToggleCommand(staffChatManager));
        getCommand("sctoggle").setTabCompleter(new StaffChatTabCompleter());

        getCommand("sctogglesound").setExecutor(new ScToggleSoundCommand(staffChatManager));
        getCommand("sctogglesound").setTabCompleter(new StaffChatTabCompleter());

        getCommand("screload").setExecutor(new ScReloadCommand(staffChatManager));
        getCommand("screload").setTabCompleter(new StaffChatTabCompleter());

        getCommand("ac").setExecutor(new AcCommand(this));
        getCommand("mc").setExecutor(new ManagerChatCommand(this));

        getCommand("hush").setExecutor(new HushCommand(this));

        PluginCommand chatColorCmd = getCommand("chatcolor");
        if (chatColorCmd != null) chatColorCmd.setExecutor(new ChatColorCommand(chatColorManager));

        PluginCommand emojiReloadCmd = getCommand("emojireload");
        if (emojiReloadCmd != null) emojiReloadCmd.setExecutor(new EmojiReloadCommand(this));

        SpyListener spyListener = new SpyListener(this);
        Bukkit.getPluginManager().registerEvents(spyListener, this);

        PluginCommand socialSpyCmd = getCommand("socialspy");
        if (socialSpyCmd != null) socialSpyCmd.setExecutor(new SocialSpyCommand(spyListener));

        PluginCommand commandSpyCmd = getCommand("commandspy");
        if (commandSpyCmd != null) commandSpyCmd.setExecutor(new CommandSpyCommand(spyListener));

        if (broadcastTask != null) broadcastTask.cancel();
        if (broadcastsEnabled && !broadcasts.isEmpty() && broadcastsSeconds > 0) {
            broadcastTask = new AutoBroadcastTask(this, broadcastsSeconds);
            broadcastTask.runTaskTimer(this, broadcastsSeconds * 20L, broadcastsSeconds * 20L);
        }

        getLogger().info("JartonChat enabled.");
    }

    @Override
    public void onDisable() {
        if (broadcastTask != null) broadcastTask.cancel();
        if (muteStorage != null) muteStorage.save();

        if (discordBridge != null) discordBridge.shutdown();

        if (chatPlayerRepository != null) chatPlayerRepository.flush();
        try { if (chatPlayerConfigurationProvider != null) chatPlayerConfigurationProvider.save(); }
        catch (IOException ex) { getLogger().severe("Failed to save player data: " + ex.getMessage()); }

        getLogger().info("JartonChat disabled.");
    }

    public MuteStorage getMuteStorage() { return muteStorage; }
    public ChatService getChatService() { return chatService; }
    public RanksManager getRanksManager() { return ranksManager; }

    public StaffChatManager getStaffChatManager() { return staffChatManager; }
    public AdminChatManager getAdminChatManager() { return adminChatManager; }
    public ManagerChatManager getManagerChatManager() { return managerChatManager; }

    public DiscordBridge getDiscordBridge() { return discordBridge; }
    public ChatColorManager getChatColorManager() { return chatColorManager; }

    public HushManager getHushManager() { return hushManager; }

    public String getAdminChatFormatIngame() { return adminChatFormatIngame; }

    public void reloadAll() {
        reloadConfig();
        loadConfigFiles();
        if (ranksManager != null) ranksManager.reload();

        if (staffChatManager != null) staffChatManager.loadConfig();
        if (adminChatManager != null) adminChatManager.loadConfig();
        if (managerChatManager != null) managerChatManager.loadConfig();

        if (broadcastTask != null) broadcastTask.cancel();
        if (broadcastsEnabled && !broadcasts.isEmpty() && broadcastsSeconds > 0) {
            broadcastTask = new AutoBroadcastTask(this, broadcastsSeconds);
            broadcastTask.runTaskTimer(this, broadcastsSeconds * 20L, broadcastsSeconds * 20L);
        }
    }

    private void setupChatPlayerRepository() throws IOException {
        chatPlayerConfigurationProvider = new ChatPlayerConfigurationProvider(getDataFolder(), "player-data.yml");
        chatPlayerRepository = new YamlChatPlayerRepository(chatPlayerConfigurationProvider);
        chatPlayerRepository.loadFromYaml();
    }
}
