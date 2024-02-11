package com.jnngl.portal.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;

public abstract class BrigadierCommand {

  public Class<? extends BrigadierCommand> parent() {
    return null;
  }

  public abstract ArgumentBuilder<CommandSourceStack, ?> node();

  public List<ArgumentBuilder<CommandSourceStack, ?>> aliases() {
    return List.of();
  }

  public CommandBuildContext commandBuildContext() {
    DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
    return CommandBuildContext.configurable(dedicatedServer.registryAccess(), dedicatedServer.getWorldData().getDataConfiguration().enabledFeatures());
  }
}
