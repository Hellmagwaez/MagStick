package com.hellmagwaez.reachextender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class ReachExtender extends JavaPlugin {

    private NamespacedKey key;
    private double bonusReach;
    private Material targetMaterial;
    private String targetName;
    private List<String> targetLore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        key = new NamespacedKey(this, "reach_modifier");
        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, 20L, getConfig().getLong("check-interval"));
        getLogger().info("ReachExtender от HellMagWaez включен!");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeReach(p);
        }
    }

    private void loadConfigValues() {
        bonusReach = getConfig().getDouble("bonus-reach", 3.0);
        String matName = getConfig().getString("item.material", "STICK");
        targetMaterial = Material.getMaterial(matName);
        if (targetMaterial == null) targetMaterial = Material.STICK;
        targetName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("item.name", ""));

        // Получаем и переводим Lore
        targetLore = getConfig().getStringList("item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    // Метод для создания предмета
    private ItemStack createReachStick() {
        ItemStack item = new ItemStack(targetMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(targetName);
            meta.setLore(targetLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("getreachstick")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("reachextender.give")) {
                player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
                return true;
            }

            ItemStack reachStick = createReachStick();
            player.getInventory().addItem(reachStick);
            player.sendMessage(ChatColor.GREEN + "Вам выдана " + targetName + ChatColor.GREEN + "!");
            return true;
        }
        return false;
    }

    // Остальные методы (applyReach, removeReach и т.д.) без изменений...
    private void checkPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (isSpecialItem(offHandItem) && player.hasPermission("reachextender.use")) {
                applyReach(player);
            } else {
                removeReach(player);
            }
        }
    }

    private boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getType() != targetMaterial) return false;
        if (targetName.isEmpty()) return true;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(targetName);
    }

    private void applyReach(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
        if (attr == null) return;

        boolean hasModifier = false;
        for (AttributeModifier modifier : attr.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                hasModifier = true;
                break;
            }
        }

        if (!hasModifier) {
            AttributeModifier modifier = new AttributeModifier(
                    key,
                    bonusReach,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.OFFHAND
            );
            attr.addModifier(modifier);
        }
    }

    private void removeReach(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
        if (attr == null) return;

        for (AttributeModifier modifier : attr.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                attr.removeModifier(modifier);
                break;
            }
        }
    }
}