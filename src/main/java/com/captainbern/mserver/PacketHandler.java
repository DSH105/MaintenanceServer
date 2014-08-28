package com.captainbern.mserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.json.JSONObject;

public class PacketHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final MaintenanceServer maintenanceServer;

    private Protocol currentProtocol;

    public PacketHandler(MaintenanceServer maintenanceServer) {
        this.maintenanceServer = maintenanceServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.currentProtocol = Protocol.HANDSHAKE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, ByteBuf byteBuf) throws Exception {
        Channel channel = handlerContext.channel();

        int length = ByteBufUtils.readVarInt(byteBuf);
        int opcode = ByteBufUtils.readVarInt(byteBuf);

        switch (this.currentProtocol) {
            case HANDSHAKE:
                handleHandshake(channel, byteBuf);
                break;
            case STATUS:
                handleStatus(channel, opcode, byteBuf);
                break;
            case LOGIN:
                handleLogin(channel);
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext handlerContext, Throwable throwable) {
        MaintenanceServer.LOGGER.warn("An exception occurred while handling the packets for: " + handlerContext.channel().remoteAddress(), throwable);
    }

    /**
     * Handles the Handshake packet, basically preparing the PacketHandler for the next packets
     * @param channel
     * @param byteBuf
     */
    private void handleHandshake(Channel channel, ByteBuf byteBuf) {
        int protoVersion = ByteBufUtils.readVarInt(byteBuf);
        String address = ByteBufUtils.readUTF(byteBuf);
        int port = byteBuf.readUnsignedShort();
        int state = ByteBufUtils.readVarInt(byteBuf);

        // NetworkServer.LOGGER.info("ProtoVersion: " + protoVersion + ", Address: " + address + ", Port: " + port + ", State: " + state);

        Protocol[] protocols = Protocol.values();

        if (state < 0 || state >= protocols.length)
            disconnect(channel, "Requested state is out of bounds!");

        this.currentProtocol = protocols[state];

        if (this.currentProtocol == Protocol.LOGIN) {
            handleLogin(channel);
        } else if (this.currentProtocol == Protocol.STATUS) {
            sendStatusResponse(channel);
        }
    }

    /**
     * Handles any packet that is part of the Status Protocol
     * @param channel
     * @param byteBuf
     */
    private void handleStatus(Channel channel, int opcode, ByteBuf byteBuf) {
        if (opcode == 0x0) { // Status Request
            sendStatusResponse(channel);
        } else if (opcode == 0x01) { // Ping Packet
            sendPingResponse(channel, byteBuf.readLong());
        }
    }

    /**
     * Sends a StatusResponse to the client
     * @param channel
     */
    private void sendStatusResponse(Channel channel) {
        ByteBuf header = Unpooled.buffer();
        ByteBuf data = Unpooled.buffer();

        // Construct the PingResponse packet
        ByteBufUtils.writeVarInt(data, 0x0);
        ByteBufUtils.writeUTF(data, this.maintenanceServer.getPingResponse().toString());

        // Construct the PacketHeader
        ByteBufUtils.writeVarInt(header, data.readableBytes());

        // Send the Response
        sendPacket(channel, Unpooled.wrappedBuffer(header, data));
    }

    /**
     * Sends a PingResponse to the client, whenever we receive one
     * @param channel
     * @param time
     */
    private void sendPingResponse(Channel channel, long time) {
        ByteBuf header = Unpooled.buffer();
        ByteBuf data = Unpooled.buffer();

        // Write the contents of the packet
        ByteBufUtils.writeVarInt(data, 0x1);
        data.writeLong(time);

        // Create the PacketHeader
        ByteBufUtils.writeVarInt(header, data.readableBytes());

        // Send a Ping-response to let the client know we're still alive
        sendPacket(channel, Unpooled.wrappedBuffer(header, data));
    }

    /**
     * This server can't handle logins, so we'll just kick anyone who attempts to login...
     * @param channel
     */
    private void handleLogin(Channel channel) {
        disconnect(channel, this.maintenanceServer.getKickMessage());
    }

    /**
     * Sends a disconnect packet to the client with the given message
     * @param channel
     * @param message
     */
    private void disconnect(Channel channel, String message) {
        if (channel.isActive() && this.currentProtocol == Protocol.PLAY || this.currentProtocol == Protocol.LOGIN) {

            ByteBuf header = Unpooled.buffer();
            ByteBuf data = Unpooled.buffer();

            ByteBufUtils.writeVarInt(data, 0x0);
            ByteBufUtils.writeUTF(data, new JSONObject().put("text", message).toString());

            ByteBufUtils.writeVarInt(header, data.readableBytes());

            sendPacket(channel, Unpooled.wrappedBuffer(header, data));

        } else {
            channel.close();
        }
    }

    private void sendPacket(final Channel channel, ByteBuf packet) {
        channel.writeAndFlush(packet).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.cause() != null)
                    handlePacketSendException(channel, future.cause());
            }
        });
    }

    private void handlePacketSendException(Channel channel, Throwable cause) {
        MaintenanceServer.LOGGER.warn("An exception occurred while sending a packet to: " + channel.remoteAddress());
    }

    private static enum Protocol {
        HANDSHAKE,
        STATUS,
        LOGIN,
        PLAY
    }
}