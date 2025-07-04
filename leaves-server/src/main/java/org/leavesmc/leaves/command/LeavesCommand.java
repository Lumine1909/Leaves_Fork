package org.leavesmc.leaves.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.minecraft.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.leavesmc.leaves.command.subcommands.ConfigCommand;
import org.leavesmc.leaves.command.subcommands.CounterCommand;
import org.leavesmc.leaves.command.subcommands.PeacefulModeSwitchCommand;
import org.leavesmc.leaves.command.subcommands.ReloadCommand;
import org.leavesmc.leaves.command.subcommands.ReportCommand;
import org.leavesmc.leaves.command.subcommands.UpdateCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class LeavesCommand extends Command implements LeavesSuggestionCommand {

    public static final String BASE_PERM = "bukkit.command.leaves.";

    // subcommand label -> subcommand
    private static final Map<String, LeavesSubcommand> SUBCOMMANDS = Util.make(() -> {
        final Map<Set<String>, LeavesSubcommand> commands = new HashMap<>();
        commands.put(Set.of("config"), new ConfigCommand());
        commands.put(Set.of("update"), new UpdateCommand());
        commands.put(Set.of("peaceful"), new PeacefulModeSwitchCommand());
        commands.put(Set.of("counter"), new CounterCommand());
        commands.put(Set.of("reload"), new ReloadCommand());
        commands.put(Set.of("report"), new ReportCommand());

        return commands.entrySet().stream()
            .flatMap(entry -> entry.getKey().stream().map(s -> Map.entry(s, entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    public LeavesCommand(final String name) {
        super(name);
        this.description = "Leaves related commands";
        this.usageMessage = "/leaves [" + String.join(" | ", SUBCOMMANDS.keySet()) + "]";
        final List<String> permissions = new ArrayList<>();
        permissions.add("bukkit.command.leaves");
        permissions.addAll(SUBCOMMANDS.keySet().stream().map(s -> BASE_PERM + s).toList());
        this.setPermission(String.join(";", permissions));
        final PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        for (final String perm : permissions) {
            if (pluginManager.getPermission(perm) == null) {
                pluginManager.addPermission(new Permission(perm, PermissionDefault.OP));
            }
        }
    }

    private static boolean testPermission(final CommandSender sender, final String permission) {
        if (sender.hasPermission(BASE_PERM + permission) || sender.hasPermission("bukkit.command.leaves")) {
            return true;
        }
        sender.sendMessage(Bukkit.permissionMessage());
        return false;
    }

    @NotNull
    @Override
    public List<String> tabComplete(final @NotNull CommandSender sender, final @NotNull String alias, final String[] args, final @Nullable Location location) throws IllegalArgumentException {
        if (args.length <= 1) {
            return LeavesCommandUtil.getListMatchingLast(sender, args, usableSubcommands());
        }

        final @Nullable Pair<String, LeavesSubcommand> subCommand = resolveCommand(args[0]);
        if (subCommand != null) {
            return subCommand.second().tabComplete(sender, subCommand.first(), Arrays.copyOfRange(args, 1, args.length), location);
        }

        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CompletableFuture<Suggestions> tabSuggestion(final @NotNull CommandSender sender, final @NotNull String alias, final @NotNull String @NotNull [] args, final @Nullable Location location, final @NotNull SuggestionsBuilder builder) throws IllegalArgumentException {
        if (args.length > 1) {
            final @Nullable Pair<String, LeavesSubcommand> subCommand = resolveCommand(args[0]);
            if (subCommand != null) {
                return subCommand.second().tabSuggestion(sender, subCommand.first(), Arrays.copyOfRange(args, 1, args.length), location, builder);
            }
        }
        return null;
    }

    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String commandLabel, final String @NotNull [] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(unknownMessage());
            return false;
        }
        final Pair<String, LeavesSubcommand> subCommand = resolveCommand(args[0]);

        if (subCommand == null) {
            sender.sendMessage(unknownMessage());
            return false;
        }

        if (!testPermission(sender, subCommand.first())) {
            return true;
        }
        final String[] choppedArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.second().execute(sender, subCommand.first(), choppedArgs);
    }

    private Collection<String> usableSubcommands() {
        List<String> subcommands = new ArrayList<>();
        for (var entry : SUBCOMMANDS.entrySet()) {
            if (entry.getValue().tabCompletes()) {
                subcommands.add(entry.getKey());
            }
        }
        return subcommands;
    }

    public Component unknownMessage() {
        return text("Usage: /leaves [" + String.join(" | ", usableSubcommands()) + "]", RED);
    }

    @Nullable
    private static Pair<String, LeavesSubcommand> resolveCommand(String label) {
        label = label.toLowerCase(Locale.ENGLISH);
        LeavesSubcommand subCommand = SUBCOMMANDS.get(label);

        if (subCommand != null) {
            return Pair.of(label, subCommand);
        }

        return null;
    }

}
