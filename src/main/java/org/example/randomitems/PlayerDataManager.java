package org.example.randomitems;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final File DATA_FILE = new File("config/playtime_rewards.json");
    private static final Gson GSON = new Gson();
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public PlayerDataManager() {
        // Ensure the file is created if it doesn't exist
        createFileIfNotExists();
        loadData();
    }

    public PlayerData getPlayerData(ServerPlayerEntity player) {
        return playerDataMap.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            GSON.toJson(playerDataMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (!DATA_FILE.exists()) return;

        try (FileReader reader = new FileReader(DATA_FILE)) {
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            Map<UUID, PlayerData> data = GSON.fromJson(reader, type);
            if (data != null) {
                playerDataMap.putAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createFileIfNotExists() {
        if (!DATA_FILE.exists()) {
            try {
                // Create the parent directories and file if they don't exist
                if (DATA_FILE.getParentFile() != null && !DATA_FILE.getParentFile().exists()) {
                    DATA_FILE.getParentFile().mkdirs();
                }
                DATA_FILE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PlayerData {
        public int playtimeTicks = 0;
        public int vouchers = 0;
    }
}