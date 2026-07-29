package fr.wilddifficulty.util;

import fr.wilddifficulty.WildDifficultyPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ChatPromptUtil {

    public static void prompt(WildDifficultyPlugin plugin, Player player, String promptText, Consumer<String> onComplete) {
        player.closeInventory();
        
        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new StringPrompt() {
                    @NotNull
                    @Override
                    public String getPromptText(@NotNull ConversationContext context) {
                        return "§e[WildDifficulty] §f" + promptText;
                    }

                    @Override
                    public Prompt acceptInput(@NotNull ConversationContext context, String input) {
                        if (input != null && !input.equalsIgnoreCase("cancel")) {
                            onComplete.accept(input);
                        } else {
                            player.sendMessage(Component.text("§cAction annulée."));
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }
                })
                .withLocalEcho(false)
                .withEscapeSequence("cancel")
                .withTimeout(60);

        Conversation conv = factory.buildConversation(player);
        conv.begin();
        player.sendMessage(Component.text("§7(Tapez 'cancel' pour annuler)"));
    }
}
