package com.finplan.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

final class ClientIpResolver {
    private final List<Network> trustedProxies;

    ClientIpResolver(String trustedProxyCidrs) {
        this.trustedProxies = new ArrayList<>();
        if (trustedProxyCidrs == null || trustedProxyCidrs.isBlank()) return;
        for (String cidr : trustedProxyCidrs.split(",")) {
            String value = cidr.trim();
            if (value.isEmpty()) continue;
            try {
                String[] parts = value.split("/", -1);
                InetAddress address = parseIp(parts[0]);
                int prefix = parts.length == 1 ? address.getAddress().length * 8 : Integer.parseInt(parts[1]);
                if (parts.length > 2 || prefix < 0 || prefix > address.getAddress().length * 8) {
                    throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
                }
                trustedProxies.add(new Network(address.getAddress(), prefix));
            } catch (UnknownHostException | NumberFormatException e) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, e);
            }
        }
    }

    String resolve(InetAddress remoteAddress, String forwardedFor) {
        if (remoteAddress == null) return "unknown";
        String remote = remoteAddress.getHostAddress();
        if (!isTrusted(remoteAddress) || forwardedFor == null || forwardedFor.isBlank()) return remote;

        String[] chain = forwardedFor.split(",", -1);
        InetAddress hop = remoteAddress;
        for (int i = chain.length - 1; i >= 0; i--) {
            if (!isTrusted(hop)) return hop.getHostAddress();
            try {
                hop = parseIp(chain[i].trim());
            } catch (UnknownHostException | IllegalArgumentException e) {
                return remote;
            }
        }
        return hop.getHostAddress();
    }

    private boolean isTrusted(InetAddress address) {
        for (Network network : trustedProxies) {
            if (network.contains(address.getAddress())) return true;
        }
        return false;
    }

    private static InetAddress parseIp(String value) throws UnknownHostException {
        if (value.indexOf(':') >= 0) {
            if (!value.matches("[0-9a-fA-F:.]+")) throw new UnknownHostException(value);
            return InetAddress.getByName(value);
        }
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) throw new UnknownHostException(value);
        byte[] bytes = new byte[4];
        for (int i = 0; i < octets.length; i++) {
            if (!octets[i].matches("\\d{1,3}")) throw new UnknownHostException(value);
            int octet = Integer.parseInt(octets[i]);
            if (octet > 255) throw new UnknownHostException(value);
            bytes[i] = (byte) octet;
        }
        return InetAddress.getByAddress(bytes);
    }

    private record Network(byte[] address, int prefix) {
        boolean contains(byte[] candidate) {
            if (address.length != candidate.length) return false;
            int completeBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < completeBytes; i++) {
                if (address[i] != candidate[i]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = 0xff << (8 - remainingBits);
            return (address[completeBytes] & mask) == (candidate[completeBytes] & mask);
        }
    }
}
