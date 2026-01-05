package com.github.igotyou.FactoryMod.recipes;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A recipe that locks the name of a copy-protected map, preventing it from
 * being renamed via anvil or wordbank.
 */
public class NameLockRecipe extends InputRecipe {

    public static final NamespacedKey NAME_LOCKED_KEY = new NamespacedKey(FactoryMod.getInstance(), "name-locked");

    // Key used by SimpleAdminHacks MapCopyProtection
    private static final NamespacedKey COPY_PROTECTED_KEY = new NamespacedKey("simpleadminhacks", "copy-protected");

    public NameLockRecipe(String identifier, String name, int productionTime, ItemMap input) {
        super(identifier, name, productionTime, input);
    }

    @Override
    public boolean applyEffect(Inventory inputInv, Inventory outputInv, FurnCraftChestFactory fccf) {
        // Find the copy-protected map in the input inventory
        ItemStack map = findCopyProtectedMap(inputInv);
        if (map == null) {
            return false;
        }

        // Check if already name-locked
        if (isNameLocked(map)) {
            if (fccf.getActivator() != null) {
                Player player = Bukkit.getPlayer(fccf.getActivator());
                if (player != null) {
                    player.sendMessage(ChatColor.RED + "This map is already name-locked");
                }
            }
            return false;
        }

        // Remove the input materials (coal)
        ItemMap toRemove = input.clone();
        if (!toRemove.isContainedIn(inputInv)) {
            return false;
        }
        toRemove.removeSafelyFrom(inputInv);

        // Apply the name-lock tag
        ItemMeta meta = map.getItemMeta();
        meta.getPersistentDataContainer().set(NAME_LOCKED_KEY, PersistentDataType.BYTE, (byte) 1);

        // Add lore indicating the map is name-locked
        List<Component> existingLore = meta.lore();
        List<Component> newLore = existingLore != null ? new ArrayList<>(existingLore) : new ArrayList<>();

        String lockerName = "Unknown";
        if (fccf.getActivator() != null) {
            Player player = Bukkit.getPlayer(fccf.getActivator());
            if (player != null) {
                lockerName = player.getName();
            }
        }
        newLore.add(Component.text("Name locked by " + lockerName).color(NamedTextColor.GOLD));
        meta.lore(newLore);

        map.setItemMeta(meta);

        // Notify the player
        if (fccf.getActivator() != null) {
            Player player = Bukkit.getPlayer(fccf.getActivator());
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + "Map name has been locked and cannot be changed");
            }
        }

        return true;
    }

    @Override
    public String getTypeIdentifier() {
        return "NAMELOCK";
    }

    @Override
    public List<ItemStack> getInputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        List<ItemStack> result = new ArrayList<>();

        // Show a filled map as the main input
        ItemStack mapStack = new ItemStack(Material.FILLED_MAP, 1);
        ItemUtils.addLore(mapStack, ChatColor.GOLD + "A copy-protected map");
        result.add(mapStack);

        // Add the other inputs (coal)
        result.addAll(input.getItemStackRepresentation());

        return result;
    }

    @Override
    public List<String> getTextualInputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        List<String> result = new ArrayList<>();
        result.add("1 Copy Protected Map");
        result.addAll(formatLore(input));
        return result;
    }

    @Override
    public List<ItemStack> getOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        ItemStack mapStack = new ItemStack(Material.FILLED_MAP, 1);
        ItemUtils.setLore(mapStack,
            ChatColor.AQUA + "Copy protected map",
            ChatColor.GOLD + "Name locked"
        );
        return Collections.singletonList(mapStack);
    }

    @Override
    public List<String> getTextualOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        return Collections.singletonList("1 Name Locked Map");
    }

    @Override
    public Material getRecipeRepresentationMaterial() {
        return Material.TRIAL_KEY;
    }

    /**
     * Finds a copy-protected map in the given inventory.
     */
    private ItemStack findCopyProtectedMap(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.FILLED_MAP) {
                if (isCopyProtected(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given item is copy-protected (has the SimpleAdminHacks tag).
     */
    public static boolean isCopyProtected(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(COPY_PROTECTED_KEY, PersistentDataType.INTEGER);
    }

    /**
     * Checks if the given item is name-locked.
     */
    public static boolean isNameLocked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(NAME_LOCKED_KEY, PersistentDataType.BYTE);
    }

    @Override
    public boolean enoughMaterialAvailable(Inventory inputInv) {
        if (!input.isContainedIn(inputInv)) {
            return false;
        }
        // Must have a copy-protected map that isn't already name-locked
        ItemStack map = findCopyProtectedMap(inputInv);
        return map != null && !isNameLocked(map);
    }
}
