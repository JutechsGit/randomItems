package org.example.randomitems;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {

    private final PlayerDataManager dataManager = new PlayerDataManager();
    private static final int TICKS_PER_HOUR = 20 * 60 * 10; //15 is the time in minutes here. 60 for an hour etc.

    @Override
    public void onInitialize() {
        // Save data on server stop
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> dataManager.saveData());

        // Track playtime on each tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(this::trackPlaytime);
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("redeem").executes(context -> redeemVoucher(context.getSource())));
        });
    }

    private void trackPlaytime(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);

        // Increment playtime ticks
        data.playtimeTicks++;

        // Award a voucher if an hour of playtime has passed
        if (data.playtimeTicks >= TICKS_PER_HOUR) {
            data.playtimeTicks = 0;
            data.vouchers++;
            player.sendMessage(Text.literal("You've received a voucher!"), false);
        }
    }
    private static final Set<Item> BLACKLISTED_ITEMS = new HashSet<>();
    static {
        // Add blacklisted items to the set
        BLACKLISTED_ITEMS.add(Items.BARRIER);
        BLACKLISTED_ITEMS.add(Items.COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Items.CHAIN_COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Items.REPEATING_COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Items.STRUCTURE_BLOCK);
        BLACKLISTED_ITEMS.add(Items.STRUCTURE_VOID);
        BLACKLISTED_ITEMS.add(Items.JIGSAW);
        BLACKLISTED_ITEMS.add(Items.DEBUG_STICK);
        BLACKLISTED_ITEMS.add(Items.COMMAND_BLOCK_MINECART);
        BLACKLISTED_ITEMS.add(Items.KNOWLEDGE_BOOK);
        BLACKLISTED_ITEMS.add(Items.WRITTEN_BOOK);

        // Add more items as needed
    }
    private int redeemVoucher(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);

        if (data.vouchers > 0) {
            int runs = 0;

            // Attempt to redeem vouchers
            while (data.vouchers > 0) {
                data.vouchers--;
                runs++;

                // Select a random item from the registry
                boolean validItemFound = false;
                for (int attempts = 0; attempts < 10; attempts++) {
                    RegistryEntry.Reference<Item> randomItem = Registries.ITEM.getRandom(source.getWorld().getRandom())
                            .orElseThrow(() -> new IllegalStateException("No items in registry"));

                    if (!BLACKLISTED_ITEMS.contains(randomItem.value())) {
                        ItemStack reward = new ItemStack(randomItem.value());
                        player.dropItem(reward, false);
                        validItemFound = true;
                        break; // Exit the attempts loop once a valid item is found
                    }
                }

                if (!validItemFound) {
                    player.sendMessage(Text.literal("Could not find a valid item to give you. Please inform me @jutechsv4 on Discord.").formatted(Formatting.RED), false);
                    return 0; // Command failure
                }
            }

            player.sendMessage(Text.literal("You've redeemed " + runs + " voucher(s)!").formatted(Formatting.GREEN), false);
            return 1; // Command success

        } else {
            // Calculate remaining time for next voucher
            int remainingTicks = TICKS_PER_HOUR - data.playtimeTicks;
            int remainingMinutes = remainingTicks / (20 * 60); // 20 ticks per second, 60 seconds per minute
            int remainingSeconds = (remainingTicks / 20) % 60; // Remaining seconds after minutes

            // Send message about remaining time
            player.sendMessage(Text.literal("You don't have any vouchers to redeem!").formatted(Formatting.RED), false);
            player.sendMessage(Text.literal("Next voucher in: " + remainingMinutes + "m " + remainingSeconds + "s").formatted(Formatting.YELLOW), false);
            return 0; // Command failure
        }
    }
}