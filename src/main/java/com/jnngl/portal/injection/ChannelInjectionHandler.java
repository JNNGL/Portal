package com.jnngl.portal.injection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

@ChannelHandler.Sharable
public class ChannelInjectionHandler extends ChannelInboundHandlerAdapter {

  private final List<ChannelInjector> injectors;
  private final Set<Channel> injectedChannels = new HashSet<>();

  public ChannelInjectionHandler(List<ChannelInjector> injectors) {
    this.injectors = injectors;
  }

  @Override
  public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
    Channel channel = (Channel) msg;

    channel.pipeline().addLast(new ChannelInitializer<>() {

      @Override
      protected void initChannel(@NotNull Channel ch) {
        channel.pipeline().addLast(new ChannelDuplexHandler() {

          @Override
          public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
            ctx.pipeline().remove(this);
            ChannelInjectionHandler.this.inject(ctx.channel());

            super.channelRead(ctx, msg);
          }

          @Override
          public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ctx.pipeline().remove(this);
            ChannelInjectionHandler.this.inject(ctx.channel());

            super.write(ctx, msg, promise);
          }
        });
      }
    });

    super.channelRead(ctx, msg);
  }

  private void inject(Channel channel) {
    channel.pipeline().addFirst("injection_handler", new ChannelInboundHandlerAdapter() {

      @Override
      public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        ChannelInjectionHandler.this.injectedChannels.remove(channel);
        super.channelInactive(ctx);
      }
    });
    this.injectedChannels.add(channel);
    this.injectors.forEach(injector -> injector.inject(channel));
  }

  public void deject() {
    this.injectedChannels.forEach(channel -> {
      channel.pipeline().remove("injection_handler");
      this.injectors.forEach(injector -> injector.deject(channel));
    });
    this.injectedChannels.clear();
  }
}
