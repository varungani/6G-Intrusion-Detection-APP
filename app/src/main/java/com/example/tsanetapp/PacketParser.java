package com.example.tsanetapp;

import android.util.Log;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketParser {

    private static final String TAG = "PacketParser";

    public static class ParsedPacketData {
        public long timestamp;
        public String srcIp;
        public String destIp;
        public int srcPort;
        public int destPort;
        public int protocol;
        public int totalLength;
        // Add other relevant fields you parse

        public ParsedPacketData(long timestamp, String srcIp, String destIp, int srcPort, int destPort, int protocol, int totalLength) {
            this.timestamp = timestamp;
            this.srcIp = srcIp;
            this.destIp = destIp;
            this.srcPort = srcPort;
            this.destPort = destPort;
            this.protocol = protocol;
            this.totalLength = totalLength;
            // Initialize other fields if needed
        }
    }

    public static ParsedPacketData parsePacket(ByteBuffer buffer) {
        long timestamp = System.currentTimeMillis();
        if (buffer.remaining() < 20) { // Minimum IP header size
            Log.w(TAG, "Incomplete packet");
            return null;
        }

        ByteBuffer packetBuffer = buffer.order(ByteOrder.BIG_ENDIAN);

        byte versionAndIhl = packetBuffer.get(0);
        byte version = (byte) (versionAndIhl >> 4);
        int ihl = (versionAndIhl & 0x0F) * 4; // Internet Header Length in bytes

        if (version != 4) {
            Log.w(TAG, "Not IPv4 or incomplete IP header");
            return null;
        }

        if (packetBuffer.remaining() < ihl) {
            Log.w(TAG, "Incomplete IP header");
            return null;
        }

        int totalLength = packetBuffer.getShort(2) & 0xFFFF;

        byte protocolNumber = packetBuffer.get(9);
        String srcIp = formatIpAddress(packetBuffer.getInt(12));
        String destIp = formatIpAddress(packetBuffer.getInt(16));
        int srcPort = -1;
        int destPort = -1;

        if (protocolNumber == 6) { // TCP
            if (packetBuffer.remaining() >= ihl + 20) { // TCP header is at least 20 bytes
                srcPort = packetBuffer.getShort(ihl) & 0xFFFF;
                destPort = packetBuffer.getShort(ihl + 2) & 0xFFFF;
            }
        } else if (protocolNumber == 17) { // UDP
            if (packetBuffer.remaining() >= ihl + 8) { // UDP header is 8 bytes
                srcPort = packetBuffer.getShort(ihl) & 0xFFFF;
                destPort = packetBuffer.getShort(ihl + 2) & 0xFFFF;
            }
        }

        ParsedPacketData parsedData = new ParsedPacketData(timestamp, srcIp, destIp, srcPort, destPort, protocolNumber, totalLength);
        Log.d(TAG, "Parsed Packet: " +
                "Timestamp=" + parsedData.timestamp + ", " +
                "SrcIP=" + parsedData.srcIp + ":" + parsedData.srcPort + ", " +
                "DestIP=" + parsedData.destIp + ":" + parsedData.destPort + ", " +
                "Protocol=" + parsedData.protocol + ", " +
                "Length=" + parsedData.totalLength);

        return parsedData;
    }

    private static String formatIpAddress(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                ((ipAddress >> 24) & 0xFF);
    }
}