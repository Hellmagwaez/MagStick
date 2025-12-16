package com.hellmagwaez.reachextender;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

// 1. Добавили "implements Listener"
public class ReachExtender extends JavaPlugin implements Listener {

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

        // 2. Регистрируем события (чтобы ритуал работал)
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, 20L, getConfig().getLong("check-interval"));
        getLogger().info("ReachExtender включен!");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeReach(p);
        }
    }

    // --- ЛОГИКА РИТУАЛА ---
    @EventHandler
    public void onLightningDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            // 1. Проверяем палку в правой руке
            boolean isStick = mainHand.getType() == Material.STICK && !isSpecialItem(mainHand);

            // 2. Проверяем Дыхание дракона в левой руке
            boolean isDragonBreath = offHand.getType() == Material.DRAGON_BREATH;

            if (isStick && isDragonBreath) {

                // Забираем предметы
                mainHand.setAmount(mainHand.getAmount() - 1);
                offHand.setAmount(offHand.getAmount() - 1);

                // Выдаем магическую палку
                player.getInventory().addItem(createReachStick()).forEach((index, item) ->
                        player.getWorld().dropItem(player.getLocation(), item));

                // Эффекты
                player.sendMessage(ChatColor.LIGHT_PURPLE + "⚡ Дыхание дракона слилось с молнией!");
            }
        }
    }
    // -----------------------

    private void loadConfigValues() {
        bonusReach = getConfig().getDouble("bonus-reach", 3.0);
        String matName = getConfig().getString("item.material", "STICK");
        targetMaterial = Material.getMaterial(matName);
        if (targetMaterial == null) targetMaterial = Material.STICK;
        targetName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("item.name", ""));
        targetLore = getConfig().getStringList("item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    private ItemStack createReachStick() {
        ItemStack item = new ItemStack(targetMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(targetName);
            meta.setLore(targetLore);

            // --- ДОБАВЛЕНО ---
            // 1. Добавляем "фейковое" зачарование (Прочность 1), true позволяет наложить его даже на палку
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);

            // 2. Скрываем отображение зачарований в лоре
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            // -----------------

            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("getreachstick")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (!player.hasPermission("reachextender.give")) return true;
            player.getInventory().addItem(createReachStick());
            return true;
        }
        return false;
    }

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
                    key, bonusReach, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.OFFHAND
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