package com.bonesnetwork.moonwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class MoonWars extends JavaPlugin implements Listener {
    private HashMap<UUID, Vector> velocities = new HashMap<>();
    private HashMap<UUID, Location> positions = new HashMap<>();
    private HashMap<UUID, Boolean> onGround = new HashMap<>();
    private double gravity = 0.1;
    
    public void onEnable() {
        
        saveDefaultConfig();
        getLogger().info("MoonWars has been loaded.");

        gravity = this.getConfig().getDouble("GravityValue");
        getLogger().info(getConfig().getStringList("Worlds").toString());

        for(World world: Bukkit.getWorlds()) {
            getLogger().info(world.getName());
        }

        getLogger().info("Gravity Loaded On Worlds:");
        for (String worldName : this.getConfig().getStringList("Worlds")) {
            World world = Bukkit.getWorld(worldName);
            getLogger().info(world.getName());
            BukkitRunnable task = new BukkitRunnable() {
                public void run() {
                    try {
                        updateVelocities(world);
                    } catch (Exception ignored) {
                    }
                }
            };
            task.runTaskTimer(this, 1, 1);
        }
        
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                entity.removeMetadata("DisableFallDamage", this);
            }
        }
        
        getLogger().info("MoonWars has been unloaded.");
    }
    
    public void updateVelocities(World world) {
        for (Entity entity : world.getEntities()) {
            UUID uuid = entity.getUniqueId();
            if (velocities.containsKey(uuid) && onGround.containsKey(uuid) && !entity.isOnGround() && !entity.isInsideVehicle()) {
                Vector oldV = velocities.get(uuid).clone();
                boolean isOnGround = onGround.get(uuid);
                
                if (!isOnGround) {
                    double dy = oldV.clone().subtract(entity.getVelocity()).getY();
                    
                    if (dy > 0.0 && (entity.getVelocity().getY() < -0.01 || entity.getVelocity().getY() > 0.01)) {
                        Location loc = entity.getLocation().clone();
                        int highestY = loc.getWorld().getHighestBlockYAt(loc);
                        
                        if (loc.getY() > highestY) {
                            loc.setY(highestY);
                        }
                       
                        
                        Vector velocity = entity.getVelocity().clone().setY(oldV.getY() - dy * gravity);
                        
                        if (entity instanceof Player) {
                            if (velocity.getY() > 4.0) {
                                velocity.setY(4);
                            }
                            
                            if (velocity.getY() < -10.0) {
                                velocity.setY(-10);
                            }
                        }
                        
                        if (gravity > 0.0 && gravity < 1.0 && entity instanceof LivingEntity) {
                            Vector facing = ((LivingEntity) entity).getEyeLocation().getDirection().multiply(0.01);
                            velocity.add(facing);
                        }
                        
                        if (gravity != 0.0) {
                            entity.setVelocity(velocity);
                            
                            entity.setMetadata("DisableFallDamage", new FixedMetadataValue(this, true));
                        }
                    }
                }
            }
            
            velocities.put(uuid, entity.getVelocity().clone().setY(entity.getVelocity().getY()));
            onGround.put(uuid, entity.isOnGround());
            positions.put(uuid, entity.getLocation());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity().hasMetadata("DisableFallDamage")) {
            event.setCancelled(true);
            MetadataValue metadataValue = event.getEntity().getMetadata("DisableFallDamage").get(0);
            event.getEntity().removeMetadata("DisableFallDamage", metadataValue.getOwningPlugin());
        }
    }
}