package com.monokek.printing.infrastructure;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Plain TCP reachability probe, shared by {@code PrinterService.testConnection}
 * and {@code NetworkPrintDispatcher} — both need to fail fast on an unreachable
 * printer instead of relying on {@code TcpIpOutputStream}'s own connection,
 * which only logs failures on a background thread instead of surfacing them
 * through its write path.
 */
@Component
public class NetworkPrinterProbe {

    private static final int CONNECT_TIMEOUT_MS = 2000;

    public boolean isReachable(String ip, int port) {
        try (Socket probe = new Socket()) {
            probe.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
