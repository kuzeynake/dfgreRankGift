package me.dfgre.rankgift;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class Main extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private FileConfiguration lang;
    private File dataFile;
    private FileConfiguration data;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final Map<UUID, PendingGift> pendingGifts = new HashMap<>();
    private String latestVersion = "";
    private boolean updateAvailable = false;
    private final int resourceId = 131275;

    private record PendingGift(UUID gifter, String rankKey, int cost, BukkitTask timerTask) {}

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLang();
        createDataFile();
        
        Objects.requireNonNull(getCommand("gift")).setExecutor(this);
        Objects.requireNonNull(getCommand("gift")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("gold")).setExecutor(this);
        Objects.requireNonNull(getCommand("goldadmin")).setExecutor(this);
        Objects.requireNonNull(getCommand("goldadmin")).setTabCompleter(this);
        
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("---------------------------------------");
        getLogger().info("This plugin was developed by dfgre for free.");
        getLogger().info("For any issues or information about updates,");
        getLogger().info("you can contact discord: dfgre.");
        getLogger().info("---------------------------------------");

        checkUpdates();
    }

    private void checkUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URLConnection connection = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openConnection();
                latestVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
                if (!getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                    updateAvailable = true;
                    getLogger().warning("A new update is available for dfgreRankGift!");
                    getLogger().warning("Current version: " + getDescription().getVersion() + " | Latest: " + latestVersion);
                    getLogger().warning("Download: https://www.spigotmc.org/resources/" + resourceId);
                }
            } catch (IOException e) {
                getLogger().warning("Could not check for updates.");
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("dfgrerankgift.admin") && updateAvailable) {
            player.sendMessage(serializer.deserialize("&8[&bdfgreRankGift&8] &aA new update is available! &7(&e" + latestVersion + "&7)"));
            player.sendMessage(serializer.deserialize("&eDownload here: &b&nhttps://www.spigotmc.org/resources/" + resourceId));
        }
    }

    private void loadLang() {
        reloadConfig();
        String langType = getConfig().getString("language", "en");
        File langFile = new File(getDataFolder(), "lang_" + langType + ".yml");
        if (!langFile.exists()) {
            saveResource("lang_tr.yml", false);
            saveResource("lang_en.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang_" + langType + ".yml"));
    }

    private void createDataFile() {
        dataFile = new File(getDataFolder() + "/data", "players.yml");
        if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveCustomData() {
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("gold")) {
            if (!(sender instanceof Player player)) return true;
            player.sendMessage(serializer.deserialize(lang.getString("own-gold", "&6Gold amount: &e%amount%").replace("%amount%", String.valueOf(data.getInt(player.getUniqueId() + ".gold", 0)))));
            return true;
        }

        if (command.getName().equalsIgnoreCase("goldadmin")) {
            if (!sender.hasPermission("dfgrerankgift.admin")) return true;
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) {
                if (args.length < 3) {
                    sender.sendMessage(serializer.deserialize("&cUsage: /goldadmin <add/remove/set> <player> <amount>"));
                    return true;
                }
                Player t = Bukkit.getPlayer(args[1]);
                if (t == null) return true;
                int amt = Integer.parseInt(args[2]);
                if (args[0].equalsIgnoreCase("add")) {
                    data.set(t.getUniqueId() + ".gold", data.getInt(t.getUniqueId() + ".gold", 0) + amt);
                    sender.sendMessage(serializer.deserialize(lang.getString("gold-added").replace("%player%", t.getName()).replace("%amount%", String.valueOf(amt))));
                } else if (args[0].equalsIgnoreCase("remove")) {
                    data.set(t.getUniqueId() + ".gold", Math.max(0, data.getInt(t.getUniqueId() + ".gold", 0) - amt));
                    sender.sendMessage(serializer.deserialize(lang.getString("gold-removed").replace("%player%", t.getName()).replace("%amount%", String.valueOf(amt))));
                } else if (args[0].equalsIgnoreCase("set")) {
                    data.set(t.getUniqueId() + ".gold", Math.max(0, amt));
                    sender.sendMessage(serializer.deserialize("&aGold amount of " + t.getName() + " set to " + amt + "."));
                }
                saveCustomData();
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("gift")) {
            if (args.length == 0) return false;
            
            if (args[0].equalsIgnoreCase("help") && sender.hasPermission("dfgrerankgift.admin")) {
                sendHelp(sender);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("dfgrerankgift.admin")) {
                loadLang();
                sender.sendMessage(serializer.deserialize("&a[dfgreRankGift] Config and Language files reloaded!"));
                return true;
            }
            if (!(sender instanceof Player player)) return true;
            
            if (args.length == 1) {
                if (player.getName().equalsIgnoreCase(args[0])) {
                    player.sendMessage(serializer.deserialize(lang.getString("cannot-gift-self", "&cYou cannot gift yourself!")));
                    return true;
                }
                openRankMenu(player, args[0]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("confirm")) {
                openConfirmMenu(player, args[1], args[2]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("execute")) {
                handleExecute(player, args[1], args[2]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("accept")) {
                handleAccept(player, args[1], args[2]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("deny")) {
                handleDeny(player, args[1], args[2]);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(serializer.deserialize("&8&m-------&r &b&ldfgreRankGift Help &8&m-------"));
        s.sendMessage(serializer.deserialize("&e/gift <player> &8- &7Opens the gift menu."));
        s.sendMessage(serializer.deserialize("&e/gold &8- &7Shows your gold count."));
        
        if (s.hasPermission("dfgrerankgift.admin")) {
            s.sendMessage(serializer.deserialize("&e/gift help &8- &7Shows this menu."));
            s.sendMessage(serializer.deserialize("&8&m-------&r &6&lAdmin Commands &8&m-------"));
            s.sendMessage(serializer.deserialize("&e/goldadmin add <name> <amount> &8- &7Adds gold to player."));
            s.sendMessage(serializer.deserialize("&e/goldadmin remove <name> <amount> &8- &7Removes gold from player."));
            s.sendMessage(serializer.deserialize("&e/goldadmin set <name> <amount> &8- &7Sets player's gold amount."));
            s.sendMessage(serializer.deserialize("&e/gift reload &8- &7Reloads the files."));
        }
        s.sendMessage(serializer.deserialize("&8&m--------------------------------"));
    }

    private void openRankMenu(Player player, String target) {
        ConfigurationSection ranksSection = getConfig().getConfigurationSection("ranks");
        if (ranksSection == null) return;
        
        List<Component> pages = new ArrayList<>();
        List<String> keys = new ArrayList<>(ranksSection.getKeys(false));
        int totalRanks = keys.size();
        int ranksPerPage = 5;

        for (int i = 0; i < totalRanks; i += ranksPerPage) {
            TextComponent.Builder pageBuilder = Component.text()
                    .append(Component.text(target, NamedTextColor.GOLD))
                    .append(Component.text(" " + lang.getString("book-choose-rank", "select rank:") + "\n\n", NamedTextColor.BLACK));

            for (int j = i; j < i + ranksPerPage && j < totalRanks; j++) {
                String key = keys.get(j);
                String rankEntry = ranksSection.getString(key);
                if (rankEntry == null) continue;

                String[] split = rankEntry.split(":");
                pageBuilder.append(Component.text("> ", NamedTextColor.DARK_GRAY))
                        .append(serializer.deserialize(split[0])
                        .clickEvent(ClickEvent.runCommand("/gift confirm " + target + " " + key)))
                        .append(Component.newline());
            }
            pages.add(pageBuilder.build());
        }
        
        player.openBook(Book.book(Component.text("Gift"), Component.text("dfgre"), pages));
    }

    private void openConfirmMenu(Player player, String target, String rankKey) {
        String rankData = getConfig().getString("ranks." + rankKey);
        if (rankData == null) return;
        String rankDisplay = rankData.split(":")[0];

        TextComponent content = Component.text(target, NamedTextColor.GOLD)
                .append(Component.text(" " + lang.getString("book-confirm-to", "to player") + " ", NamedTextColor.BLACK))
                .append(serializer.deserialize(rankDisplay))
                .append(Component.text("\n" + lang.getString("book-confirm-question", "gift it?") + "\n\n", NamedTextColor.BLACK))
                .append(Component.text("          "))
                .append(Component.text("[" + lang.getString("accept", "YES") + "]", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(serializer.deserialize(lang.getString("accept-hover", "Click to accept"))))
                        .clickEvent(ClickEvent.runCommand("/gift execute " + target + " " + rankKey)))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("          "))
                .append(Component.text("[" + lang.getString("deny", "NO") + "]", NamedTextColor.RED)
                        .hoverEvent(HoverEvent.showText(serializer.deserialize(lang.getString("deny-hover", "Click to deny"))))
                        .clickEvent(ClickEvent.runCommand("/gift " + target)));
        player.openBook(Book.book(Component.text("Confirm"), Component.text("dfgre"), content));
    }

    private void handleExecute(Player player, String targetName, String rankKey) {
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(serializer.deserialize(lang.getString("cannot-gift-self", "&cYou cannot gift yourself!")));
            return;
        }

        String rankData = getConfig().getString("ranks." + rankKey);
        if (rankData == null) return;
        
        int cost = Integer.parseInt(rankData.split(":")[1]);
        if (data.getInt(player.getUniqueId() + ".gold", 0) < cost) {
            player.sendMessage(serializer.deserialize(lang.getString("insufficient-gold", "&cNo gold!")));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            data.set(player.getUniqueId() + ".gold", data.getInt(player.getUniqueId() + ".gold") - cost);
            saveCustomData();
            
            BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
                if (pendingGifts.containsKey(target.getUniqueId())) {
                    target.closeInventory();
                    refundGift(target.getUniqueId());
                }
            }, 30 * 20L);

            pendingGifts.put(target.getUniqueId(), new PendingGift(player.getUniqueId(), rankKey, cost, task));
            openReceiveMenu(target, player.getName(), rankKey);
            player.sendMessage(serializer.deserialize(lang.getString("gift-sent", "&aSent to %target%.").replace("%target%", targetName)));
        }
    }

    private void openReceiveMenu(Player target, String gifter, String rankKey) {
        String rankData = getConfig().getString("ranks." + rankKey);
        if (rankData == null) return;
        String rankDisplay = rankData.split(":")[0];

        TextComponent content = Component.text(gifter, NamedTextColor.GOLD)
                .append(Component.text(" " + lang.getString("receive-body", "wants to gift you %rank%!").replace("%gifter%", "").replace("%rank%", "") + "\n", NamedTextColor.BLACK))
                .append(serializer.deserialize(rankDisplay))
                .append(Component.text("\n\n"))
                .append(Component.text("           "))
                .append(Component.text(lang.getString("accept", "YES"), NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(serializer.deserialize(lang.getString("accept-hover", "Click to accept"))))
                        .clickEvent(ClickEvent.runCommand("/gift accept " + gifter + " " + rankKey)))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("           "))
                .append(Component.text(lang.getString("deny", "NO"), NamedTextColor.RED).decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(serializer.deserialize(lang.getString("deny-hover", "Click to deny"))))
                        .clickEvent(ClickEvent.runCommand("/gift deny " + gifter + " " + rankKey)));
        target.openBook(Book.book(Component.text("Gift"), Component.text("dfgre"), content));
    }

    private void handleAccept(Player target, String gifterName, String rankKey) {
        if (!pendingGifts.containsKey(target.getUniqueId())) return;
        PendingGift pg = pendingGifts.remove(target.getUniqueId());
        if (pg.timerTask() != null) pg.timerTask().cancel();

        String rankData = getConfig().getString("ranks." + rankKey);
        ConfigurationSection commandsSection = getConfig().getConfigurationSection("commands");
        if (rankData == null || commandsSection == null) return;

        String[] split = rankData.split(":");
        data.set(pg.gifter + ".sent", data.getInt(pg.gifter + ".sent", 0) + 1);
        saveCustomData();

        String cmdTemplate = commandsSection.getString(rankKey);
        if (cmdTemplate != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdTemplate.replace("%player%", target.getName()));
        }
        
        Component bcast = serializer.deserialize(lang.getString("broadcast-msg", "&6&kXX &b%gifter% &egifted %rank% &eto &a%target%&e! &6&kXX")
                .replace("%gifter%", gifterName).replace("%target%", target.getName()).replace("%rank%", split[0]))
                .append(Component.newline())
                .append(serializer.deserialize(lang.getString("broadcast-count", "&eThey have sent &6%count% &egifts so far!").replace("%count%", String.valueOf(data.getInt(pg.gifter + ".sent")))));
        Bukkit.broadcast(bcast);
    }

    private void handleDeny(Player target, String gifterName, String rankKey) {
        if (!pendingGifts.containsKey(target.getUniqueId())) return;
        refundGift(target.getUniqueId());
    }

    private void refundGift(UUID targetUUID) {
        PendingGift pg = pendingGifts.remove(targetUUID);
        if (pg == null) return;
        if (pg.timerTask() != null) pg.timerTask().cancel();

        data.set(pg.gifter + ".gold", data.getInt(pg.gifter + ".gold", 0) + pg.cost);
        saveCustomData();

        Player gifter = Bukkit.getPlayer(pg.gifter);
        if (gifter != null) {
            gifter.sendMessage(serializer.deserialize(lang.getString("gift-refunded", "&cGift canceled. Gold refunded.")));
        }
    }

    @EventHandler
    public void onBookClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (pendingGifts.containsKey(player.getUniqueId())) {
                refundGift(player.getUniqueId());
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("goldadmin")) {
            if (!sender.hasPermission("dfgrerankgift.admin")) return Collections.emptyList();
            if (args.length == 1) return Arrays.asList("add", "remove", "set");
            if (args.length == 2) return null; 
        }
        if (command.getName().equalsIgnoreCase("gift")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                if (sender.hasPermission("dfgrerankgift.admin")) {
                    list.add("help");
                    list.add("reload");
                }
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.getName().equals(sender.getName())) {
                        list.add(p.getName());
                    }
                });
                return list;
            }
        }
        return Collections.emptyList();
    }
}