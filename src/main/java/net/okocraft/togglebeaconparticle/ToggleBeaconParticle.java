package net.okocraft.togglebeaconparticle;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NullMarked
public class ToggleBeaconParticle extends JavaPlugin implements Listener {

    private final Set<UUID> hiding = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onEnable() {
        this.loadHiding();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.saveHiding();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by the player.", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("togglebeaconparticle.command")) {
            sender.sendMessage(Component.text("You don't have the permission to execute this command.", NamedTextColor.RED));
            return true;
        }

        Component toggledTo;
        if (this.hiding.contains(player.getUniqueId())) {
            this.hiding.remove(player.getUniqueId());
            toggledTo = Component.text("on", NamedTextColor.GREEN);
        } else {
            this.hiding.add(player.getUniqueId());
            toggledTo = Component.text("off", NamedTextColor.RED);
        }

        sender.sendMessage(Component.textOfChildren(
                Component.text("Beacon effect particle turned ", NamedTextColor.GRAY),
                toggledTo
        ));

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBeaconEffect(BeaconEffectEvent event) {
        if (this.hiding.contains(event.getPlayer().getUniqueId())) {
            event.setEffect(event.getEffect().withParticles(false));
        }
    }

    private void loadHiding() {
        Path file = this.getDataFile();

        if (!Files.isRegularFile(file)) {
            return;
        }

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.map(line -> {
                try {
                    return UUID.fromString(line);
                } catch (IllegalArgumentException e) {
                    this.getLogger().warning("Invalid uuid: " + line);
                    return null;
                }
            }).filter(Objects::nonNull).forEach(this.hiding::add);
        } catch (IOException e) {
            this.getLogger().severe("Could not load players who hiding beacon particles. Message: " + e.getMessage());
        }
    }

    private void saveHiding() {
        try {
            Path file = this.getDataFile();
            Path parent = file.getParent();

            if (!Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, this.hiding.stream().map(UUID::toString).collect(Collectors.joining(System.lineSeparator())), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            this.getLogger().severe("Could not save players who hiding beacon particles. Message: " + e.getMessage());
        }
    }

    private Path getDataFile() {
        return this.getDataFolder().toPath().resolve("players.txt");
    }
}
