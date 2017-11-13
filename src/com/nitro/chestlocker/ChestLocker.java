package com.nitro.chestlocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.UUID;

public class ChestLocker extends JavaPlugin implements Listener {

    private File langFile, chestsFile;
    private FileConfiguration lang, chests;

    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        registerFiles();
        loadFiles();
    }

    public void onDisable() {
        saveFiles();
    }

    private void registerFiles() {
        langFile = new File(getDataFolder() + "/lang.yml");
        lang = new YamlConfiguration().loadConfiguration(langFile);
        chestsFile = new File(getDataFolder() + "/chests.yml");
        chests = new YamlConfiguration().loadConfiguration(chestsFile);
    }

    private void loadDefaults(FileConfiguration configFile, String resourceFile)
    {
        Reader defConfigStream;
        try {
            defConfigStream = new InputStreamReader(this.getResource(resourceFile), "UTF8");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                configFile.setDefaults(defConfig);
                defConfigStream.close();
                saveFiles();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFiles() {
        if (langFile.exists() && chestsFile.exists()) {
            try {
                lang.load(langFile);
                chests.load(chestsFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            saveFiles();
        }
    }

    private void saveFiles() {
        if (!langFile.exists()) {
            try {
                lang.save(langFile);
                loadDefaults(lang, "lang.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            lang.save(langFile);
            chests.save(chestsFile);
            loadFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd,String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("lock")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("playerOnly")));
                return false;
            }
            Player p = (Player) sender;
            if (!(p.hasPermission("chestlocker.lock"))) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("noPermission")));
                return false;
            }

            PlayerInventory pInv = p.getInventory();
            if (pInv.getItem(0).getType() != Material.AIR) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("clearFirstSlot")));
                return false;
            }

            ItemStack lockTool = new ItemStack(Material.LEASH, 1);
            ItemMeta lockToolMeta = lockTool.getItemMeta();
            lockToolMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Lock Tool"));
            lockToolMeta.getLore().add(ChatColor.translateAlternateColorCodes('&', "&8Left click on a chest to lock it."));
            lockTool.setItemMeta(lockToolMeta);
            pInv.setItem(0, lockTool);

            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("lockTutorial")));
        } else if (cmd.getName().equalsIgnoreCase("unlock")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("playerOnly")));
                return false;
            }
            Player p = (Player) sender;
            if (!(p.hasPermission("chestlocker.unlock"))) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("noPermission")));
                return false;
            }

            PlayerInventory pInv = p.getInventory();

            ItemStack lockTool = new ItemStack(Material.LEASH, 1);
            ItemMeta lockToolMeta = lockTool.getItemMeta();
            lockToolMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Lock Tool"));
            lockToolMeta.getLore().add(ChatColor.translateAlternateColorCodes('&', "&8Left click on a chest to lock it."));
            lockTool.setItemMeta(lockToolMeta);

            if (pInv.getItem(0) == lockTool) {
                pInv.setItem(0, new ItemStack(Material.AIR));
            } else if (pInv.getItem(0).getType() != Material.AIR) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("clearFirstSlot")));
                return false;
            }

            ItemStack unlockTool = new ItemStack(Material.LEASH, 1);
            ItemMeta unlockToolMeta = unlockTool.getItemMeta();
            unlockToolMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Unlock Tool"));
            unlockToolMeta.getLore().add(ChatColor.translateAlternateColorCodes('&', "&8Left click on a chest to unlock it."));
            unlockTool.setItemMeta(unlockToolMeta);
            pInv.setItem(0, unlockTool);

            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("unlockTutorial")));
        }
        return false;
    }

    private boolean isLocked(Location loc) {
        String locString = Integer.toString(loc.getBlockX()) + Integer.toString(loc.getBlockY()) + Integer.toString(loc.getBlockZ());
        if (chests.getKeys(false).contains(locString)) {
            return true;
        } else {
            return false;
        }
    }

    private UUID whoLocked(Location loc) {
        String locString = Integer.toString(loc.getBlockX()) + Integer.toString(loc.getBlockY()) + Integer.toString(loc.getBlockZ());
        return UUID.fromString(chests.get(locString).toString());
    }

    private boolean lock(Player p, Location loc) {
            String locString = Integer.toString(loc.getBlockX()) + Integer.toString(loc.getBlockY()) + Integer.toString(loc.getBlockZ());
            if (chests.getKeys(false).contains(locString)) {
                return false;
            }
            chests.set(locString, p.getUniqueId());
            saveFiles();
            return true;
    }

    private int unLock(Player p, Location loc) {
        String locString = Integer.toString(loc.getBlockX()) + Integer.toString(loc.getBlockY()) + Integer.toString(loc.getBlockZ());
        if (chests.getKeys(false).contains(locString)) {
            if (chests.get(locString).equals(p.getUniqueId())) {
                chests.set(locString, null);
                saveFiles();
                return 1;
            } else {
                return 2;
            }
        } return 0;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        PlayerInventory pInv = p.getInventory();
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (pInv.getItemInMainHand().getType() == Material.LEASH) {
                if (pInv.getItemInMainHand().getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', "&7Lock Tool"))) {
                    if (e.getClickedBlock().getType() == Material.CHEST) {
                        if (lock(p, e.getClickedBlock().getLocation())) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("locked")));
                            return;
                        } else {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.getString("lockingFailed")));
                            return;
                        }
                    }
                } else if (pInv.getItemInMainHand().getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', "&7Unlock Tool"))) {
                    int unlockResult = unLock(p, e.getClickedBlock().getLocation());
                    if (unlockResult == 0) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "chestNotLocked"));
                        return;
                    } else if (unlockResult == 2) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "chestIsntYours"));
                        return;
                    } else if (unlockResult == 1) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "unlocked"));
                        return;
                    }
                }
            }
            if (e.getClickedBlock().getType() == Material.CHEST) {
                if (isLocked(e.getClickedBlock().getLocation())) {
                    if (whoLocked(e.getClickedBlock().getLocation()) == p.getUniqueId()) {
                        return;
                    } else {
                        if (p.hasPermission("chestlocker.bypass")) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "bypass"));
                            return;
                        }
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "isLocked"));
                    }
                }
            }
        }
    }
}
