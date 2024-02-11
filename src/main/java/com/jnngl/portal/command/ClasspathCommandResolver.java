package com.jnngl.portal.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;

public interface ClasspathCommandResolver {

  Collection<LiteralArgumentBuilder<CommandSourceStack>> resolve(String base) throws ReflectiveOperationException;

}
