package com.finplan.gateway;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {
    @Test
    void ignoresForwardedHeaderFromUntrustedPeer() throws Exception {
        var resolver = new ClientIpResolver("10.0.0.0/8");
        assertEquals("203.0.113.10", resolver.resolve(InetAddress.getByName("203.0.113.10"), "198.51.100.7"));
    }

    @Test
    void resolvesClientFromTrustedProxyChainRightToLeft() throws Exception {
        var resolver = new ClientIpResolver("10.0.0.0/8");
        assertEquals("198.51.100.7", resolver.resolve(InetAddress.getByName("10.0.0.2"),
                "198.51.100.7, 10.0.0.3"));
    }

    @Test
    void rejectsMalformedTrustedProxyConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver("not-an-ip"));
    }

    @Test
    void ignoresMalformedForwardedChainFromTrustedPeer() throws Exception {
        var resolver = new ClientIpResolver("10.0.0.0/8");
        assertEquals("10.0.0.2", resolver.resolve(InetAddress.getByName("10.0.0.2"), "attacker"));
    }
}
