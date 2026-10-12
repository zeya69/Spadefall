package com.supercraftmc.spadefall.setup.chat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickable chat lines.
 *
 * Built on the BungeeCord chat API because it ships with both Spigot and
 * Paper. The Adventure API is nicer but Paper-only, and the first real server
 * this plugin ran on was Spigot.
 *
 * A line is a prefix of plain text followed by zero or more [buttons]. Each
 * button runs a command when clicked and shows a hover hint.
 */
public final class ChatUi {

    private ChatUi() { }

    public static final class Button {
        final String label;
        final String command;
        final String hover;
        final ChatColor colour;

        public Button(String label, String command, String hover, ChatColor colour) {
            this.label = label;
            this.command = command;
            this.hover = hover;
            this.colour = colour;
        }

        public static Button of(String label, String command, String hover) {
            return new Button(label, command, hover, ChatColor.AQUA);
        }

        public static Button green(String label, String command, String hover) {
            return new Button(label, command, hover, ChatColor.GREEN);
        }

        public static Button grey(String label, String command, String hover) {
            return new Button(label, command, hover, ChatColor.GRAY);
        }
    }

    /** Sends {@code legacyText} followed by the buttons on one line. */
    public static void send(Player player, String legacyText, Button... buttons) {
        ComponentBuilder builder = new ComponentBuilder();
        if (legacyText != null && !legacyText.isEmpty()) {
            builder.append(TextComponent.fromLegacyText(legacyText));
        }
        for (Button button : buttons) {
            builder.append("  ", ComponentBuilder.FormatRetention.NONE);
            builder.append("[" + button.label + "]", ComponentBuilder.FormatRetention.NONE)
                    .color(button.colour)
                    .bold(false)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, button.command));
            if (button.hover != null) {
                builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(button.hover))));
            }
        }
        player.spigot().sendMessage(builder.create());
    }

    /** A bare line with no buttons. */
    public static void line(Player player, String legacyText) {
        player.spigot().sendMessage(TextComponent.fromLegacyText(legacyText));
    }

    public static void rule(Player player) {
        line(player, "\u00A78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
    }

    public static List<BaseComponent> toComponents(String legacy) {
        List<BaseComponent> out = new ArrayList<>();
        for (BaseComponent c : TextComponent.fromLegacyText(legacy)) {
            out.add(c);
        }
        return out;
    }
}
