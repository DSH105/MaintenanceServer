package com.captainbern.mserver;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class ByteBufUtils {

    public static int readVarInt(ByteBuf byteBuf) {
        int out = 0;
        int bytes = 0;
        byte in;
        while (true) {
            in = byteBuf.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);
            if (bytes > 5) {
                throw new IllegalStateException("Integer is bigger than maximum allowed!");
            }
            if ((in & 0x80) != 0x80) {
                break;
            }
        }
        return out;
    }

    public static void writeVarInt(ByteBuf byteBuf, int varInt) {
        int part;
        while (true) {
            part = varInt & 0x7F;
            varInt >>>= 7;
            if (varInt != 0) {
                part |= 0x80;
            }
            byteBuf.writeByte(part);
            if (varInt == 0) {
                break;
            }
        }
    }

    public static String readUTF(ByteBuf byteBuf) {
        byte[] bytes = new byte[readVarInt(byteBuf)];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeUTF(ByteBuf byteBuf, String utf) {
        byte[] data = utf.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, utf.length());
        byteBuf.writeBytes(data);
    }
}
