package net.zerotoil.dev.cybertravel.cache;

import lombok.Getter;
import net.zerotoil.dev.cybertravel.CyberTravel;
import net.zerotoil.dev.cybertravel.object.PlayerData;
import net.zerotoil.dev.cybertravel.object.region.Region;
import net.zerotoil.dev.cybertravel.object.region.RegionFactory;
import net.zerotoil.dev.cybertravel.object.region.settings.RegionLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    private final CyberTravel main;

    private Config config;
    @Getter private Map<String, Region> regions;
    @Getter private Map<String, PlayerData> players;

    private final RegionFactory regionFactory;

    public Cache(CyberTravel main) {
        this.main = main;
        regionFactory = new RegionFactory(main);
    }

    /**
     * Reload all cache.
     */
    public void reload() {
        load(true);
    }

    public void load(boolean loadCore) {

        if (loadCore) main.reloadCore();

        config = new Config(main);
        reloadRegions();
        loadPlayers();

        if (loadCore) main.core().loadFinish();

    }

    /**
     * Reloads all regions in config.
     */
    public void reloadRegions() {

        long startTime = System.currentTimeMillis();
        main.logger("&cLoading region configurations...");

        ConfigurationSection section = main.core().files().getConfig("regions").getConfigurationSection("regions");
        if (section == null) return;

        regions = new HashMap<>();
        for (String s : section.getKeys(false)){
            Region region = new Region(main, s);
            if (region.isEnabled()) regions.put(s, region);
        }

        main.logger("&7Loaded &e" + regions.size() + "&7 regions in &a" + (System.currentTimeMillis() - startTime) + "ms&7.", "");

    }

    /**
     * Contains all values of the config.yml
     * saved to cache.
     *
     * @return Config containing values
     */
    public Config config() {
        return config;
    }

    public RegionFactory regionFactory() {
        return regionFactory;
    }

    /**
     * Obtains a region based on an ID.
     *
     * @param id ID of an existing region
     * @return Region binded to the ID
     */
    public Region getRegion(String id) {
        if (!isRegion(id)) return null;
        return regions.get(id);
    }

    /**
     * Checks if a region with given ID is
     * already in the config/loaded to cache.
     *
     * @param id ID of a region
     * @return If the region already exists
     */
    public boolean isRegion(String id) {
        return (regions.containsKey(id));
    }

    /**
     * Constructs a region object based on the region
     * location. Generates necessary data in the
     * regions.yml file. Do not use this method if
     * the RegionLocation is not fully set up or is
     * already binded to another region!
     *
     * @param id New ID (must not already exist)
     * @param location Region location that is full set up
     * @return If the region was successfully created
     */
    public boolean createRegion(String id, RegionLocation location) {
        try {
            regions.put(id, new Region(main, id, location));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean checkRegionDiscovery(Player player) {

        if (!player.hasPermission("cst.player.discover")) return false;

        PlayerData playerData = getPlayer(player);
        boolean discovered = false;

        for (Region region : regions.values()) {
            if (!region.inRegion(player.getLocation())) continue;
            if (playerData.isDiscovered(region)) continue;
            playerData.addRegion(region);
            discovered = true;
        }

        return discovered;
    }

    // Checks if a location is within a region
    private boolean inRegion(Location location) {
        for (Region r : regions.values())
            if (r.inRegion(location)) return true;
        return false;
    }

    /**
     * Returns a region in which the location is
     * located. Will return null if it is not
     * within a region.
     *
     * @param location Location in question
     * @return Region the location is in
     */
    public Region getRegionAt(Location location) {
        for (Region r : regions.values())
            if (r.inRegion(location)) return r;
        return null;
    }

    /**
     * Teleports a player to a region.
     *
     * @param player Player in question
     * @param region Region to teleport to
     * @return True always
     */
    public boolean teleportToRegion(Player player, String region) {
        if (!regions.containsKey(region)) return !main.sendMessage(player, "invalid-region", new String[]{"regionID"}, region);
        if (!getPlayer(player).isDiscovered(region)) return !main.sendMessage(player, "not-discovered", new String[]{"regionID"}, region);
        Region r = regions.get(region);
        return r.getTeleport().teleportPlayer(player);
    }

    /**
     * Load all online players.
     */
    public void loadPlayers() {
        players = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers())
            loadPlayer(player);
    }

    /**
     * Unload all players in cache.
     */
    public void unloadPlayers() {
        for (Player player : Bukkit.getOnlinePlayers())
            savePlayer(player);
        players = new HashMap<>();
    }

    /**
     * Loads a certain player's data into cache.
     * If the player does not have any data yet,
     * then it will automatically generate.
     *
     * @param player Player in question
     * @return False if player is already in cache
     */
    public boolean loadPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (players.containsKey(uuid)) return false;
        players.put(uuid, new PlayerData(main, player));
        return true;
    }

    /**
     * Saves a certain player's data to file
     * and removes them from cache.
     *
     * @param player Player in question
     * @return False if player is not in cache
     */
    public boolean unloadPlayer(Player player) {
        boolean bool = savePlayer(player);
        players.remove(player.getUniqueId().toString());
        return bool;
    }

    /**
     * Saves a certain player's data to file.
     *
     * @param player Player in question
     * @return False if player is not in cache
     */
    public boolean savePlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (!players.containsKey(uuid)) return false;
        players.get(uuid).saveData();
        return true;
    }

    /**
     * Gets a certain player's data based on
     * the bukkit player object.
     *
     * @param player Player in question
     * @return Player data saved to cache
     */
    public PlayerData getPlayer(Player player) {
        return player != null ? getPlayer(player.getUniqueId().toString()) : null;
    }

    /**
     * Gets a certain player's data based on
     * that player's UUID.
     *
     * @param uuid UUID of player in question
     * @return Player data saved to cache
     */
    public PlayerData getPlayer(String uuid) {
        return players.get(uuid);
    }

}
