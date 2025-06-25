package com.example.backend.Client.Manager;

import com.example.backend.Client.ImapClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ImapClientManager {

    private final String host;
    private final int port;

    private final Map<String, ImapClient> userClients = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivityTimestamps = new ConcurrentHashMap<>();
    private static final long IDLE_TIMEOUT_MINUTES = 30;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS_PER_USER = 2;

    public ImapClientManager(
            @Value("${server.imap.host}") String host,
            @Value("${server.imap.port}") int port) {
        this.host = host;
        this.port = port;
    }

    public ImapClient getClientForUser(String userEmail) {
        cleanIdleConnections();

        long userConnectionCount = userClients.values().stream()
                .filter(client -> client.getUserEmail() != null && client.getUserEmail().equals(userEmail))
                .count();

        if (userConnectionCount >= MAX_CONNECTIONS_PER_USER) {
            throw new IllegalStateException("Maximum connections reached for user: " + userEmail);
        }

        return userClients.computeIfAbsent(userEmail, key -> {
            ImapClient newClient = new ImapClient(host, port);
            newClient.setUserEmail(userEmail);
            activeConnections.incrementAndGet();
            return newClient;
        });
    }

    private void updateLastActivity(String userEmail) {
        lastActivityTimestamps.put(userEmail, System.currentTimeMillis());
    }

    private void cleanIdleConnections() {
        long currentTime = System.currentTimeMillis();
        userClients.entrySet().removeIf(entry -> {
            Long lastActivity = lastActivityTimestamps.get(entry.getKey());
            if (lastActivity != null &&
                    (currentTime - lastActivity) > TimeUnit.MINUTES.toMillis(IDLE_TIMEOUT_MINUTES)) {
                entry.getValue().disconnect();
                lastActivityTimestamps.remove(entry.getKey());
                activeConnections.decrementAndGet();
                return true;
            }
            return false;
        });
    }

    public void removeClientForUser(String userEmail) {
        ImapClient client = userClients.remove(userEmail);
        if (client != null) {
            client.disconnect();
            lastActivityTimestamps.remove(userEmail);
            activeConnections.decrementAndGet();
        }
    }

    @PreDestroy
    public void disconnectAll() {
        userClients.values().forEach(ImapClient::disconnect);
        userClients.clear();
        lastActivityTimestamps.clear();
        activeConnections.set(0);
    }

    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
}
