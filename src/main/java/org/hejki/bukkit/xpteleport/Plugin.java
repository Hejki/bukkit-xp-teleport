package org.hejki.bukkit.xpteleport;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Random;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL;

/**
 * TODO Document me.
 *
 * @author Petr Hejkal
 */
public class Plugin extends JavaPlugin {
    private Random random;

    @Override
    public void onEnable() {
        random = new Random(System.currentTimeMillis());

        saveDefaultConfig();
        super.onEnable();
        log("Enabled successfully!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("go".equals(label)) {
            Player player = getServer().getPlayer(sender.getName());

            sender.sendMessage("L:"+player.getLevel());
            sender.sendMessage("E:"+player.getExp());
            sender.sendMessage("T:"+player.getTotalExperience());
            sender.sendMessage("X:"+player.getExpToLevel());

            if ("c".equals(args[0])) {
                player.setLevel(0);
                player.setExp(0);
            }
            if (args[0].startsWith("+")) {
                player.setExp(Float.valueOf(args[0]));
            }
            if (args[0].startsWith("l")) {
                player.setLevel(Integer.valueOf(args[0].substring(1)));
            }
            return true;
        }

        if (args.length >= 1) {
            String arg = args[0];

            if ("list".equals(arg)) {
                printTeleportLocations(sender);
                return true;
            }

            if ("set".equals(arg)) {
                if (args.length < 2) {
                    return false;
                }

                setTeleportLocation(sender, args[1], (String[]) ArrayUtils.subarray(args, 2, args.length));
                return true;
            }

            if ("info".equals(arg)) {
                if (args.length < 2) {
                    return false;
                }

                printTeleportLocationInfo(sender, args[1]);
                return true;
            }

            teleportPlayer(sender, arg);
            return true;
        }
        return false;
    }

    private void teleportPlayer(CommandSender sender, String locationId) {
        Player player = getServer().getPlayer(sender.getName());
        Vector position = getConfig().getVector(locationConfigKey(player.getWorld(), locationId, "position"));

        if (position == null) {
            sender.sendMessage("Location " + locationId + " was not found.");
            return;
        }

        int playerLevel = player.getLevel();
        int levelCost = calculateLvlCost(player, position, null);
        if (playerLevel - levelCost < 0) {
            sender.sendMessage("Teleport to location " + locationId + " requires " + levelCost + " levels.");
        }

        player.setLevel(playerLevel - levelCost);
        player.teleport(new Location(player.getWorld(), position.getX(), position.getY(), position.getZ()), ENDER_PEARL);

        int damage = (int) (random.nextGaussian() * 2);
        if (damage > 0) {
            player.damage(damage, player);
        }
    }

    private void printTeleportLocationInfo(CommandSender sender, String locationId) {
        Player player = getServer().getPlayer(sender.getName());
        Vector position = getConfig().getVector(locationConfigKey(player.getWorld(), locationId, "position"));
        String description = getConfig().getString(locationConfigKey(player.getWorld(), locationId, "description"));
        int maxPrice = calculateLvlCost(player, position, "max");

        sender.sendMessage("Teleport location: " + locationId);
        sender.sendMessage(String.format(" * x = %.0f, y = %.0f, z = %.0f", position.getX(), position.getY(), position.getZ()));

        if (maxPrice > 0) {
            sender.sendMessage(String.format(" * transport price = %d - %d levels", calculateLvlCost(player, position, "min"), maxPrice));
        } else {
            sender.sendMessage(" * transport price = free");
        }

        if (description != null) {
            sender.sendMessage(" * " + description);
        }
    }

    private void setTeleportLocation(CommandSender sender, String locationId, String[] descriptionArray) {
        Player player = getServer().getPlayer(sender.getName());

        getConfig().set(locationConfigKey(player.getWorld(), locationId, "position"), player.getLocation().toVector());

        if (descriptionArray.length != 0) {
            String description = StringUtils.join(descriptionArray, ' ');
            getConfig().set(locationConfigKey(player.getWorld(), locationId, "description"), description);
        }
        saveConfig();
    }

    private String locationConfigKey(World world, String locationId, String config) {
        if (locationId == null) {
            return String.format("locations.%s", world.getName());
        }
        if (config == null) {
            return String.format("locations.%s.%s", world.getName(), locationId);
        }
        return String.format("locations.%s.%s.%s", world.getName(), locationId, config);
    }

    private void printTeleportLocations(CommandSender sender) {
        Player player = getServer().getPlayer(sender.getName());
        ConfigurationSection worldConfig = getConfig().getConfigurationSection(locationConfigKey(player.getWorld(), null, null));

        if (worldConfig != null) {
            sender.sendMessage("Teleport locations:");

            for (String locationId : worldConfig.getKeys(false)) {
                String description = getConfig().getString(locationConfigKey(player.getWorld(), locationId, "description"));
                String msg;

                if (description != null) {
                    msg = String.format(" * %s - %s", locationId, description);
                } else {
                    msg = String.format(" * %s", locationId);
                }
                sender.sendMessage(msg);
            }
        }
    }

    private int calculateLvlCost(Player player, Vector position, String minOrMax) {
        double distance = player.getLocation().toVector().distance(position);

        if ("max".equals(minOrMax)) {
            return (int) (distance / 300);
        }
        if ("min".equals(minOrMax)) {
            return (int) (distance / 1000);
        }
        return (int) (distance / (random.nextInt(900) + 300));
    }

    public void log(String message) {
        System.out.println(String.format("[%s] %s", getName(), message));
    }
}
