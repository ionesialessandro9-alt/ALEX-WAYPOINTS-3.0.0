
package it.alex.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlexWaypointsClient implements ClientModInitializer {

    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path SAVE = DIR.resolve("alex-waypoints.json");
    private static final Path LIVE = DIR.resolve("alex-waypoints-live.json");

    private static final List<WP> W = new ArrayList<>();

    static class WP {
        String name;
        double x, y, z;

        WP(String name, double x, double y, double z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    static class Data {
        String name;
        double x, y, z;
        double distance;
        double playerX, playerY, playerZ;
        float yaw, pitch;
        long updatedAt;
        List<WP> waypoints;

        Data(WP w, double distance, MinecraftClient m) {
            name = w.name;
            x = w.x;
            y = w.y;
            z = w.z;
            this.distance = distance;
            playerX = m.player.getX();
            playerY = m.player.getY();
            playerZ = m.player.getZ();
            yaw = m.player.getYaw();
            pitch = m.player.getPitch();
            updatedAt = System.currentTimeMillis();
            waypoints = new ArrayList<>(W);
        }
    }

    @Override
    public void onInitializeClient() {
        load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("alexwp")

                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument(
                            "name",
                            com.mojang.brigadier.arguments.StringArgumentType.word()
                        ).executes(context -> {
                            MinecraftClient m = MinecraftClient.getInstance();
                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");

                            if (m.player != null) {
                                add(name, m.player.getX(), m.player.getY(), m.player.getZ());
                                m.player.sendMessage(Text.literal("[ALEX] Salvato: " + name), false);
                            }
                            return 1;
                        }))
                    )

                    .then(ClientCommandManager.literal("addpos")
                        .then(ClientCommandManager.argument("name",
                            com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(ClientCommandManager.argument("x",
                                com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                .then(ClientCommandManager.argument("y",
                                    com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                    .then(ClientCommandManager.argument("z",
                                        com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                            double x = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "x");
                                            double y = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "y");
                                            double z = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "z");

                                            add(name, x, y, z);
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )

                    .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            MinecraftClient m = MinecraftClient.getInstance();

                            if (m.player != null) {
                                for (WP w : W) {
                                    m.player.sendMessage(
                                        Text.literal("[ALEX] " + w.name + " " +
                                            (int) w.x + " " + (int) w.y + " " + (int) w.z),
                                        false
                                    );
                                }
                            }
                            return 1;
                        })
                    )

                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name",
                            com.mojang.brigadier.arguments.StringArgumentType.word())
                            .executes(context -> {
                                String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                W.removeIf(w -> w.name.equalsIgnoreCase(name));
                                save();
                                return 1;
                            })
                        )
                    )
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> writeLive());
    }

    private static void add(String name, double x, double y, double z) {
        W.removeIf(w -> w.name.equalsIgnoreCase(name));
        W.add(new WP(name, x, y, z));
        save();
    }

    private static void load() {
        try {
            Files.createDirectories(DIR);

            if (Files.exists(SAVE)) {
                WP[] saved = G.fromJson(Files.readString(SAVE), WP[].class);

                if (saved != null) {
                    W.addAll(Arrays.asList(saved));
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(SAVE, G.toJson(W));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void writeLive() {
        MinecraftClient m = MinecraftClient.getInstance();

        if (m.player == null || m.world == null) {
            return;
        }

        try {
            WP best = null;
            double bestDistance = Double.MAX_VALUE;

            for (WP w : W) {
                double dx = w.x - m.player.getX();
                double dy = w.y - m.player.getY();
                double dz = w.z - m.player.getZ();

                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = w;
                }
            }

            if (best == null) {
                best = new WP(
                    "Nessun waypoint",
                    m.player.getX(),
                    m.player.getY(),
                    m.player.getZ()
                );
                bestDistance = 0;
            }

            Files.createDirectories(DIR);
            Files.writeString(LIVE, G.toJson(new Data(best, bestDistance, m)));

        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
