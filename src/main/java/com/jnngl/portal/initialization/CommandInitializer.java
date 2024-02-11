package com.jnngl.portal.initialization;

import com.jnngl.portal.command.ClasspathCommandResolver;
import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.lifecycle.annotation.OnReload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;

public class CommandInitializer {

  @OnReload(priority = 50)
  @SneakyThrows
  public static void initializeCommands(DependencyInjector dependencyInjector) {
    DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
    CommandDispatcher<CommandSourceStack> dispatcher = dedicatedServer.vanillaCommandDispatcher.getDispatcher();
    var commands = dependencyInjector.get(ClasspathCommandResolver.class).resolve("com.jnngl");
    commands.forEach(command -> {
      LiteralCommandNode<CommandSourceStack> commandNode = command.build();
      RootCommandNode<CommandSourceStack> rootCommandNode = dispatcher.getRoot();
      rootCommandNode.removeCommand(commandNode.getName());
      rootCommandNode.addChild(commandNode);
    });
  }
}
