package cn.itcast.server.handle;

import cn.itcast.message.ChatRequestMessage;
import cn.itcast.message.ChatResponseMessage;
import cn.itcast.server.session.SessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class ChatRequestHandle extends SimpleChannelInboundHandler<ChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        Channel toChannel = SessionFactory.getSession().getChannel(msg.getTo());
        if(toChannel != null){
            toChannel.writeAndFlush(new ChatResponseMessage(msg.getFrom(), msg.getContent()));
        }else{
            ctx.writeAndFlush(new ChatResponseMessage(false, msg.getTo()+"不在线!"));
        }

    }
}
