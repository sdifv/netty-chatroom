package cn.itcast.server.handle;

import cn.itcast.message.*;
import cn.itcast.server.session.Group;
import cn.itcast.server.session.GroupSession;
import cn.itcast.server.session.GroupSessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Set;

@ChannelHandler.Sharable
public class GroupChatRequestHandle extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof GroupCreateRequestMessage){   // 处理创建群聊请求
            GroupCreateRequestMessage groupCreate = (GroupCreateRequestMessage) msg;
            String groupName = groupCreate.getGroupName();
            Set<String> members = groupCreate.getMembers();
            GroupSession groupSession = GroupSessionFactory.getGroupSession();
            Group group = groupSession.createGroup(groupName, members);
            if(group == null){
                ctx.writeAndFlush(new GroupCreateResponseMessage(true, groupName+"创建成功!"));
                members.forEach(member->{
                    groupSession.joinMember(groupName, member);
                });
                groupSession.getMembersChannel(groupName).forEach(channel -> {
                    channel.writeAndFlush(new GroupChatResponseMessage(true, "您已被拉入"+groupName));
                });
            }else{
                ctx.writeAndFlush(new GroupCreateResponseMessage(false, groupName+"已经存在!"));
            }
        } else if(msg instanceof GroupChatRequestMessage){   // 处理群消息请求
            GroupChatRequestMessage groupChat = (GroupChatRequestMessage) msg;
            String groupName = groupChat.getGroupName();
            GroupSessionFactory.getGroupSession().getMembersChannel(groupName).forEach(
                    channel -> channel.writeAndFlush(new GroupChatResponseMessage(groupChat.getFrom(), groupChat.getContent()))
            );
        }else if(msg instanceof GroupJoinRequestMessage){   // 处理加群请求
            GroupJoinRequestMessage groupJoin = (GroupJoinRequestMessage) msg;
            String groupName = groupJoin.getGroupName();
            String username = groupJoin.getUsername();
            Group group = GroupSessionFactory.getGroupSession().joinMember(groupName, username);
            if(group == null){
                ctx.writeAndFlush(new GroupJoinResponseMessage(false, groupName+"不存在!"));
            }else{
                ctx.writeAndFlush(new GroupJoinResponseMessage(true, "加入成功!"));
            }
        }else if(msg instanceof GroupQuitRequestMessage){   // 处理退群请求
            GroupQuitRequestMessage groupQuit = (GroupQuitRequestMessage) msg;
            String groupName = groupQuit.getGroupName();
            String username = groupQuit.getUsername();
            Group group = GroupSessionFactory.getGroupSession().removeMember(groupName, username);
            if(group == null){
                ctx.writeAndFlush(new GroupQuitResponseMessage(false, groupName+"不存在!"));
            }else{
                ctx.writeAndFlush(new GroupQuitResponseMessage(true, "退出成功!"));
            }
        }
        else if(msg instanceof GroupMembersRequestMessage){ // 处理群成员查询请求
            GroupMembersRequestMessage groupMembers = (GroupMembersRequestMessage) msg;
            String groupName = groupMembers.getGroupName();
            Set<String> members = GroupSessionFactory.getGroupSession().getMembers(groupName);
            ctx.writeAndFlush(new GroupMembersResponseMessage(members));
        }
    }
}
