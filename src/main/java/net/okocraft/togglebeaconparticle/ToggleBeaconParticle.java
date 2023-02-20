package net.okocraft.togglebeaconparticle;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ToggleBeaconParticle extends JavaPlugin implements Listener {

    private final Set<UUID> hiding = new HashSet<>();

    @Override
    public void onEnable() {
        loadHiding();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveHiding();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by the player.");
            return true;
        }

        if (sender.hasPermission("togglebeaconparticle.command")) {
            if (hiding.contains(player.getUniqueId())) {
                hiding.remove(player.getUniqueId());
                sender.sendMessage("§7Beacon effect particle turned §aon");
            } else {
                hiding.add(player.getUniqueId());
                sender.sendMessage("§7Beacon effect particle turned §coff");
            }
        } else {
            sender.sendMessage("You don't have the permission to execute this command.");
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBeaconEffect(BeaconEffectEvent event) {
        if (hiding.contains(event.getPlayer().getUniqueId())) {
            event.setEffect(event.getEffect().withParticles(false));
        }
    }

    private void loadHiding() {
        var file = getDataFile();

        if (Files.isRegularFile(file)) {
            try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
                lines.map(line -> {
                    try {
                        return UUID.fromString(line);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid uuid: " + line);
                        return null;
                    }
                }).filter(Objects::nonNull).forEach(hiding::add);
            } catch (IOException e) {
                getLogger().severe("Could not load players who hiding beacon particles. Message: " + e.getMessage());
            }
        }
    }

    private void saveHiding() {
        try {
            var file = getDataFile();
            var parent = file.getParent();

            if (!Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, hiding.stream().map(UUID::toString).collect(Collectors.joining(System.lineSeparator())), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            getLogger().severe("Could not save players who hiding beacon particles. Message: " + e.getMessage());
        }
    }

    private @NotNull Path getDataFile() {
        return getDataFolder().toPath().resolve("players.txt");
    }
}
