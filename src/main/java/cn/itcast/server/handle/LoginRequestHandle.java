package cn.itcast.server.handle;

import cn.itcast.message.LoginRequestMessage;
import cn.itcast.message.LoginResponseMessage;
import cn.itcast.server.service.UserServiceFactory;
import cn.itcast.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class LoginRequestHandle extends SimpleChannelInboundHandler<LoginRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        boolean login = UserServiceFactory.getUserService().login(msg.getUsername(), msg.getPassword());
        if(login){
            SessionFactory.getSession().bind(ctx.channel(), msg.getUsername());
            ctx.writeAndFlush(new LoginResponseMessage(true, "登录成功!"));
        }else{
            ctx.writeAndFlush(new LoginResponseMessage(false, "登陆失败"));
        }
    }
}
