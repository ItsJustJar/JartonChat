package me.jarton.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaffChatTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("sc")) {
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("sctoggle") ||
            command.getName().equalsIgnoreCase("sctogglesound")) {
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("screload")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("confirm");
            return suggestions;
        }

        return Collections.emptyList();
    }
}
