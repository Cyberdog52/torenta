package ch.andreskonrad.torenta.bittorrent.service;

import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RoutableAddressResolverTest {

    @Test
    void resolve_returnsEitherNothingOrAnAddressThatCanReachTheInternet() {
        Optional<InetAddress> address = RoutableAddressResolver.resolve();

        address.ifPresent(resolved -> {
            assertInstanceOf(Inet4Address.class, resolved);
            assertFalse(resolved.isAnyLocalAddress(), "wildcard address is not routable");
            assertFalse(resolved.isLoopbackAddress(), "loopback address is not routable");
            assertFalse(resolved.isLinkLocalAddress(), "link-local address has no internet route");
        });
    }
}
