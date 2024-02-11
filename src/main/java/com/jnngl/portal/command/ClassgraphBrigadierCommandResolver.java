package com.jnngl.portal.command;

import com.jnngl.portal.dependency.DependencyInjector;
import com.jnngl.portal.dependency.annotation.Component;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;

@Component
@RequiredArgsConstructor
public class ClassgraphBrigadierCommandResolver implements ClasspathCommandResolver {

  private final DependencyInjector dependencyInjector;

  private ArgumentBuilder<CommandSourceStack, ? extends ArgumentBuilder<?, ?>> resolveChildren(
      Map<Class<? extends BrigadierCommand>, List<BrigadierCommand>> subcommands, BrigadierCommand command,
      ArgumentBuilder<CommandSourceStack, ? extends ArgumentBuilder<?, ?>> builder) {
    List<BrigadierCommand> children = subcommands.get(command.getClass());
    if (children == null) {
      return builder;
    }

    children.forEach(child -> {
      var childBuilder = this.resolveChildren(subcommands, child, child.node());
      CommandNode<CommandSourceStack> childNode = childBuilder.build();
      builder.then(childNode);

      child.aliases().forEach(alias -> builder.then(alias.redirect(childNode)));
    });

    return builder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<LiteralArgumentBuilder<CommandSourceStack>> resolve(String base) throws ReflectiveOperationException {
    List<BrigadierCommand> rootCommands = new ArrayList<>();
    Map<Class<? extends BrigadierCommand>, List<BrigadierCommand>> subcommands = new HashMap<>();

    try (ScanResult result = new ClassGraph()
        .enableClassInfo()
        .scan()) {
      ClassInfoList commands = result
          .getClassInfo(BrigadierCommand.class.getName())
          .getSubclasses()
          .filter(info -> info.getName().startsWith(base))
          .filter(info -> !info.isAbstract());

      for (ClassInfo classInfo : commands) {
        var commandClass = (Class<? extends BrigadierCommand>) Class.forName(classInfo.getName());
        BrigadierCommand command = this.dependencyInjector.compute(commandClass);
        if (command.parent() == null) {
          rootCommands.add(command);
        } else {
          subcommands.computeIfAbsent(command.parent(), k -> new ArrayList<>()).add(command);
        }
      }
    }

    List<ArgumentBuilder<CommandSourceStack, ?>> resolvedBuilders = new ArrayList<>();

    rootCommands.forEach(command -> {
      resolvedBuilders.add(this.resolveChildren(subcommands, command, command.node()));
      command.aliases().forEach(alias -> resolvedBuilders.add(this.resolveChildren(subcommands, command, alias)));
    });

    return resolvedBuilders.stream()
        .filter(builder -> builder instanceof LiteralArgumentBuilder<?>)
        .map(builder -> (LiteralArgumentBuilder<CommandSourceStack>) builder)
        .toList();
  }
}
