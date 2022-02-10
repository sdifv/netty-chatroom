package cn.itcast.client.handle;

import cn.itcast.message.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ChannelHandler.Sharable
public class ChatClientHandle extends ChannelInboundHandlerAdapter {
    AtomicBoolean LOGIN = new AtomicBoolean(false);
    CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Scanner sc = new Scanner(System.in);
        new Thread(() -> {
            System.out.println("请输入用户名:");
            String username = sc.nextLine();
            System.out.println("请输入密码:");
            String password = sc.nextLine();
            ctx.writeAndFlush(new LoginRequestMessage(username, password));

            System.out.println("等待后续操作...");
            try {
                WAIT_FOR_LOGIN.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!LOGIN.get()) {
                ctx.channel().close();
                return;
            }
            while (true) {
                showMenu();
                String cmd = sc.nextLine();
                processUserCommand(ctx, username, cmd);
            }
        }, "login").start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof LoginResponseMessage){
            LoginResponseMessage response = (LoginResponseMessage) msg;
            if(response.isSuccess()){
                LOGIN.set(true);
            }
            WAIT_FOR_LOGIN.countDown();
        }else if(msg instanceof ChatResponseMessage){
            ChatResponseMessage response = (ChatResponseMessage)msg;
            System.out.println("re: " + response);
        }
    }

    private void processUserCommand(ChannelHandlerContext ctx, String username, String cmd) {
        String[] tokens = cmd.split(" ");
        switch (tokens[0]) {
            case "send":
                ctx.writeAndFlush(new ChatRequestMessage(username, tokens[1], tokens[2]));
                break;
            case "gsend":
                ctx.writeAndFlush(new GroupChatRequestMessage(username, tokens[1], tokens[2]));
                break;
            case "gcreate":
                Set<String> set = new HashSet<>(Arrays.asList(tokens[2].split(",")));
                set.add(username); // 加入自己
                ctx.writeAndFlush(new GroupCreateRequestMessage(tokens[1], set));
                break;
            case "gmembers":
                ctx.writeAndFlush(new GroupMembersRequestMessage(tokens[1]));
                break;
            case "gjoin":
                ctx.writeAndFlush(new GroupJoinRequestMessage(username, tokens[1]));
                break;
            case "gquit":
                ctx.writeAndFlush(new GroupQuitRequestMessage(username, tokens[1]));
                break;
            case "quit":
                ctx.channel().close();
                return;
        }
    }

    private void showMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================\n")
                .append("send [username] [content]\n")
                .append("gsend [group name] [content]\n")
                .append("gcreate [group name] [m1,m2,m3...]\n")
                .append("gmembers [group name]\n")
                .append("gjoin [group name]\n")
                .append("gquit [group name]\n")
                .append("quit\n")
                .append("==================================");
        System.out.println(sb.toString());
    }
}
