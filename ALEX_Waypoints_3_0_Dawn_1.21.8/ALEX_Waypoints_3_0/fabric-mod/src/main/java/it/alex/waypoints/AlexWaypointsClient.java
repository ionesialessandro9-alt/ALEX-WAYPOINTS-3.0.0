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
import java.nio.file.*;
import java.util.*;

public class AlexWaypointsClient implements ClientModInitializer {
 private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
 private static final Path DIR=FabricLoader.getInstance().getConfigDir();
 private static final Path SAVE=DIR.resolve("alex-waypoints.json"), LIVE=DIR.resolve("alex-waypoints-live.json");
 private static final List<WP> W=new ArrayList<>();
 static class WP { String name; double x,y,z; WP(String n,double a,double b,double c){name=n;x=a;y=b;z=c;} }
 static class Data { String name; double x,y,z,distance,playerX,playerY,playerZ; float yaw,pitch; long updatedAt; List<WP> waypoints;
 Data(WP w,double d,MinecraftClient m){name=w.name;x=w.x;y=w.y;z=w.z;distance=d;playerX=m.player.getX();playerY=m.player.getY();playerZ=m.player.getZ();yaw=m.player.getYaw();pitch=m.player.getPitch();updatedAt=System.currentTimeMillis();waypoints=new ArrayList<>(W);} }
 public void onInitializeClient(){
  load();
  ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(ClientCommandManager.literal("alexwp")
   .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("name",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{MinecraftClient m=MinecraftClient.getInstance();String n=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"name");add(n,m.player.getX(),m.player.getY(),m.player.getZ());m.player.sendMessage(Text.literal("[ALEX] Salvato "+n),false);return 1;})))
   .then(ClientCommandManager.literal("addpos").then(ClientCommandManager.argument("name",com.mojang.brigadier.arguments.StringArgumentType.word()).then(ClientCommandManager.argument("x",com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg()).then(ClientCommandManager.argument("y",com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg()).then(ClientCommandManager.argument("z",com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg()).executes(c->{String n=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"name");add(n,com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"x"),com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"y"),com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,"z"));return 1;}))))))
   .then(ClientCommandManager.literal("list").executes(c->{for(WP w:W)MinecraftClient.getInstance().player.sendMessage(Text.literal("[ALEX] "+w.name+" "+(int)w.x+" "+(int)w.y+" "+(int)w.z),false);return 1;}))
   .then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("name",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{String n=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"name");W.removeIf(w->w.name.equalsIgnoreCase(n));save();return 1;})))
  )));
  ClientTickEvents.END_CLIENT_TICK.register(m->writeLive());
 }
 static void add(String n,double x,double y,double z){W.removeIf(w->w.name.equalsIgnoreCase(n));W.add(new WP(n,x,y,z));save();}
 static void load(){try{Files.createDirectories(DIR);if(Files.exists(SAVE)){WP[] a=G.fromJson(Files.readString(SAVE),WP[].class);if(a!=null)W.addAll(Arrays.asList(a));}}catch(Exception e){System.err.println(e);}}
 static void save(){try{Files.createDirectories(DIR);Files.writeString(SAVE,G.toJson(W));}catch(Exception e){System.err.println(e);}}
 static void writeLive(){MinecraftClient m=MinecraftClient.getInstance();if(m.player==null||m.world==null)return;try{WP best=null;double bd=Double.MAX_VALUE;for(WP w:W){double x=w.x-m.player.getX(),y=w.y-m.player.getY(),z=w.z-m.player.getZ(),d=Math.sqrt(x*x+y*y+z*z);if(d<bd){bd=d;best=w;}}if(best==null){best=new WP("Nessun waypoint",m.player.getX(),m.player.getY(),m.player.getZ());bd=0;}Files.createDirectories(DIR);Files.writeString(LIVE,G.toJson(new Data(best,bd,m)));}catch(Exception e){System.err.println(e);}}
}
