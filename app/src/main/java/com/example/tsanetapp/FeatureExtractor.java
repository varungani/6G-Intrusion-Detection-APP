package com.example.tsanetapp;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class FeatureExtractor {

    private static final String TAG = "FeatureExtractor";
    private static final long FLOW_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final Map<String, List<PacketParser.ParsedPacketData>> activeFlows = new HashMap<>();

    public static String extractFeatures(PacketParser.ParsedPacketData packet) {
        if (packet == null) {
            String dummyFeatures = generateDummyFeatures();
            Log.d(TAG, "Generating Dummy Features (Null Packet): " + dummyFeatures);
            return dummyFeatures;
        }

        String flowKey = generateFlowKey(packet.srcIp, packet.destIp, packet.srcPort, packet.destPort, packet.protocol);
        if (!activeFlows.containsKey(flowKey)) {
            activeFlows.put(flowKey, new ArrayList<>());
        }
        activeFlows.get(flowKey).add(packet);

        long currentTime = System.currentTimeMillis();
        activeFlows.entrySet().removeIf(entry -> {
            List<PacketParser.ParsedPacketData> packets = entry.getValue();
            if (!packets.isEmpty() && (currentTime - packets.get(0).timestamp) > FLOW_TIMEOUT_MS) {
                Log.d(TAG, "Flow timed out: " + entry.getKey());
                return true;
            }
            return false;
        });

        List<PacketParser.ParsedPacketData> flow = activeFlows.get(flowKey);
        String features;
        if (flow != null && (flow.size() >= 2 || (currentTime - flow.get(0).timestamp) > FLOW_TIMEOUT_MS)) {
            features = calculateFeaturesForFlow(flow);
        } else {
            features = generateDummyFeatures();
            Log.d(TAG, "Generating Dummy Features (Insufficient Data): " + features);
        }
        return features;
    }

    private static String calculateFeaturesForFlow(List<PacketParser.ParsedPacketData> flowPackets) {
        if (flowPackets.isEmpty()) {
            String dummyFeatures = generateDummyFeatures();
            Log.d(TAG, "Generating Dummy Features (Empty Flow): " + dummyFeatures);
            return dummyFeatures;
        }

        // ... (rest of the calculateFeaturesForFlow function remains the same) ...
        long flowStart = flowPackets.get(0).timestamp;
        long flowEnd = flowPackets.get(flowPackets.size() - 1).timestamp;
        float flowDuration = (float) (flowEnd - flowStart) / 1000.0f;
        int totalPackets = flowPackets.size();
        int totalBytes = 0;
        int bwdPacketLengthMax = 0;
        float bwdPacketLengthMean = 0;
        float bwdPacketLengthStdDevSum = 0;
        int bwdPacketCount = 0;
        long flowIATSum = 0;
        long flowIATMin = Long.MAX_VALUE;
        long flowIATMax = Long.MIN_VALUE;
        long fwdIATTotal = 0;
        long fwdIATSum = 0;
        long fwdIATMin = Long.MAX_VALUE;
        long fwdIATMax = Long.MIN_VALUE;
        long bwdIATSum = 0;
        long bwdIATMin = Long.MAX_VALUE;
        long bwdIATMax = Long.MIN_VALUE;
        int maxPacketLength = 0;
        int finFlagCount = 0;
        int pshFlagCount = 0;
        int ackFlagCount = 0;
        int totalFwdPackets = 0;
        int totalBwdPackets = 0;
        long previousPacketTime = 0;
        long previousFwdTime = 0;
        long previousBwdTime = 0;
        float packetLengthSum = 0;
        float packetLengthSqSum = 0;
        int initWinBytesForward = 0;

        for (int i = 0; i < flowPackets.size(); i++) {
            PacketParser.ParsedPacketData packet = flowPackets.get(i);
            totalBytes += packet.totalLength;
            maxPacketLength = Math.max(maxPacketLength, packet.totalLength);
            packetLengthSum += packet.totalLength;
            packetLengthSqSum += (long) packet.totalLength * packet.totalLength;

            // Placeholder for flag counts
            // if ((packet.flagsAndOffset & 0x01) != 0) finFlagCount++;
            // if ((packet.flagsAndOffset & 0x08) != 0) pshFlagCount++;
            // if ((packet.flagsAndOffset & 0x10) != 0) ackFlagCount++;

            if (packet.srcPort != -1 && packet.srcPort < packet.destPort) {
                totalFwdPackets++;
                if (i > 0 && previousFwdTime != 0) {
                    long iat = packet.timestamp - previousFwdTime;
                    fwdIATTotal += iat;
                    fwdIATSum += iat;
                    fwdIATMin = Math.min(fwdIATMin, iat);
                    fwdIATMax = Math.max(fwdIATMax, iat);
                }
                previousFwdTime = packet.timestamp;
                // Placeholder for Init_Win_bytes_forward
                // if (i == 0 && packet.protocol == 6) { ... }
            } else {
                totalBwdPackets++;
                if (packet.totalLength > bwdPacketLengthMax) bwdPacketLengthMax = packet.totalLength;
                bwdPacketLengthMean += packet.totalLength;
                if (i > 0 && previousBwdTime != 0) {
                    long iat = packet.timestamp - previousBwdTime;
                    bwdIATSum += iat;
                    bwdIATMin = Math.min(bwdIATMin, iat);
                    bwdIATMax = Math.max(bwdIATMax, iat);
                }
                previousBwdTime = packet.timestamp;
                bwdPacketCount++;
            }

            if (i > 0) {
                long iat = packet.timestamp - previousPacketTime;
                flowIATSum += iat;
                flowIATMin = Math.min(flowIATMin, iat);
                flowIATMax = Math.max(flowIATMax, iat);
            }
            previousPacketTime = packet.timestamp;
        }

        float flowIATMean = totalPackets > 1 ? (float) flowIATSum / (totalPackets - 1) : 0;
        float fwdIATMean = totalFwdPackets > 1 ? (float) fwdIATSum / (totalFwdPackets - 1) : 0;
        float bwdIATMean = totalBwdPackets > 1 ? (float) bwdIATSum / (totalBwdPackets - 1) : 0;
        float packetsPerSecond = flowDuration > 0 ? totalPackets / flowDuration : 0;
        float packetLengthMean = totalPackets > 0 ? packetLengthSum / totalPackets : 0;
        float packetLengthVariance = totalPackets > 0 ? (packetLengthSqSum - (packetLengthSum * packetLengthSum) / totalPackets) / totalPackets : 0;
        float packetLengthStd = (float) Math.sqrt(packetLengthVariance);
        bwdPacketLengthMean = bwdPacketCount > 0 ? bwdPacketLengthMean / bwdPacketCount : 0;
        for (PacketParser.ParsedPacketData packet : flowPackets) {
            if (packet.srcPort != -1 && packet.srcPort < packet.destPort) {
                // Forward packet
            } else if (bwdPacketCount > 0) {
                bwdPacketLengthStdDevSum += Math.pow(packet.totalLength - bwdPacketLengthMean, 2);
            }
        }
        float bwdPacketLengthStd = bwdPacketCount > 0 ? (float) Math.sqrt(bwdPacketLengthStdDevSum / bwdPacketCount) : 0;
        float averagePacketSize = totalPackets > 0 ? (float) totalBytes / totalPackets : 0;
        float avgBwdSegmentSize = totalBwdPackets > 0 ? (float) (totalBytes - (totalFwdPackets > 0 ? flowPackets.stream().filter(p -> p.srcPort < p.destPort).mapToInt(p -> p.totalLength).sum() : 0)) / totalBwdPackets : 0;

        StringBuilder features = new StringBuilder();
        features.append(flowDuration).append(",");
        features.append(bwdPacketLengthMax).append(",");
        features.append(bwdPacketLengthMean).append(",");
        features.append(bwdPacketLengthStd).append(",");
        features.append(flowIATMean).append(",");
        features.append(flowIATStd(flowPackets, flowIATMean)).append(",");
        features.append(flowIATMax).append(",");
        features.append(flowIATMin).append(",");
        features.append(fwdIATTotal).append(",");
        features.append(fwdIATMean).append(",");
        features.append(fwdIATStd(flowPackets, fwdIATMean, true)).append(",");
        features.append(fwdIATMax).append(",");
        features.append(bwdIATMean).append(",");
        features.append(bwdIATStd(flowPackets, bwdIATMean, false)).append(",");
        features.append(bwdIATMax).append(",");
        features.append(packetsPerSecond).append(",");
        features.append(maxPacketLength).append(",");
        features.append(packetLengthMean).append(",");
        features.append(packetLengthStd).append(",");
        features.append(packetLengthVariance).append(",");
        features.append(finFlagCount).append(",");
        features.append(pshFlagCount).append(",");
        features.append(ackFlagCount).append(",");
        features.append(averagePacketSize).append(",");
        features.append(avgBwdSegmentSize).append(",");
        features.append(initWinBytesForward).append(",");
        features.append(0.0f).append(","); // Placeholder for Active Mean
        features.append(0.0f).append(","); // Placeholder for Active Min
        features.append(0.0f).append(","); // Placeholder for Idle Mean
        features.append(0.0f).append(","); // Placeholder for Idle Std
        features.append(0.0f).append(","); // Placeholder for Idle Max
        features.append(0.0f);          // Placeholder for Idle Min

        Log.d(TAG, "Extracted Features: " + features.toString());
        return features.toString();
    }

    private static float flowIATStd(List<PacketParser.ParsedPacketData> packets, float mean) {
        if (packets.size() <= 1) return 0;
        double sum = 0;
        for (int i = 0; i < packets.size() - 1; i++) {
            sum += Math.pow((packets.get(i + 1).timestamp - packets.get(i).timestamp) - mean, 2);
        }
        return (float) Math.sqrt(sum / (packets.size() - 1));
    }

    private static float fwdIATStd(List<PacketParser.ParsedPacketData> packets, float mean, boolean isForward) {
        List<Long> iats = new ArrayList<>();
        long previousTime = 0;
        for (PacketParser.ParsedPacketData packet : packets) {
            if ((isForward && packet.srcPort < packet.destPort) || (!isForward && packet.srcPort >= packet.destPort)) {
                if (previousTime != 0) {
                    iats.add(packet.timestamp - previousTime);
                }
                previousTime = packet.timestamp;
            }
        }
        if (iats.size() <= 1) return 0;
        double sum = 0;
        for (long iat : iats) {
            sum += Math.pow(iat - mean, 2);
        }
        return (float) Math.sqrt(sum / (iats.size() - 1));
    }

    private static float bwdIATStd(List<PacketParser.ParsedPacketData> packets, float mean, boolean isForward) {
        return fwdIATStd(packets, mean, !isForward);
    }

    private static String generateFlowKey(String srcIp, String destIp, int srcPort, int destPort, int protocol) {
        return srcIp + ":" + srcPort + "-" + destIp + ":" + destPort + "-" + protocol;
    }

    private static String generateDummyFeatures() {
        java.util.Random random = new java.util.Random();
        StringBuilder features = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            features.append(random.nextFloat());
            if (i < 31) {
                features.append(",");
            }
        }
        return features.toString();
    }
}