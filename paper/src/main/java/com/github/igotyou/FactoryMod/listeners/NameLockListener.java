package com.github.igotyou.FactoryMod.listeners;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.events.FactoryActivateEvent;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import com.github.igotyou.FactoryMod.recipes.NameLockRecipe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener that prevents name-locked items from being renamed in an anvil or via wordbank.
 */
public class NameLockListener implements Listener {

    /**
     * Prevents name-locked items from being renamed or modified in an anvil.
     * Uses PrepareAnvilEvent to nullify the result before it's even shown to the player.
     * No message is sent - just silent blocking (matching the pattern used by ExilePearl, OldEnchanting, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getFirstItem();

        if (firstItem == null) {
            return;
        }

        // Check if the first item is name-locked
        if (NameLockRecipe.isNameLocked(firstItem)) {
            // Block all anvil operations on name-locked items
            event.setResult(null);

            // Force client inventory update (Minecraft predicts results client-side)
            Bukkit.getScheduler().runTaskLater(FactoryMod.getInstance(), () -> {
                for (HumanEntity viewer : inventory.getViewers()) {
                    if (viewer instanceof Player player) {
                        player.updateInventory();
                    }
                }
            }, 1L);
        }
    }

    /**
     * Prevents players from wordbanking name-locked items.
     */
    @EventHandler
    public void onWordbankNameLockedItem(FactoryActivateEvent event) {
        if (!(event.getFactory() instanceof FurnCraftChestFactory fac)) {
            return;
        }

        if (!fac.getCurrentRecipe().getTypeIdentifier().equals("WORDBANK")) {
            return;
        }

        ItemStack targetItem = fac.getInputInventory().getItem(0);
        if (NameLockRecipe.isNameLocked(targetItem)) {
            event.setCancelled(true);
            event.getActivator().sendMessage(ChatColor.RED + "You can not wordbank name-locked items");
        }
    }
}
