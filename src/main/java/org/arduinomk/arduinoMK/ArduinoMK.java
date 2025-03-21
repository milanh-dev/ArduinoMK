package org.arduinomk.arduinoMK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public final class ArduinoMK extends JavaPlugin implements Listener {
    private Logger log = Logger.getLogger("Minecraft");
    private final HashMap<UUID, String> chatMessages = new HashMap<>();

    // TCP Server Details
    private final String TCP_SERVER_IP = "127.0.0.1";
    private final int TCP_SERVER_PORT = 12345;
    private ServerSocket serverSocket;
    private Thread serverThread;

    @Override
    public void onEnable() {
        this.log.info("[ArduinoMK] Loading version " + this.getDescription().getVersion() + "...");

        getServer().getPluginManager().registerEvents(this, this);
        startTCPServer();

        this.log.info("[ArduinoMK] Plugin loaded and enabled.");
    }

    @Override
    public void onDisable() {
        log.info("[ArduinoMK] Disabling plugin...");
        stopTCPServer();
        log.info("[ArduinoMK] Plugin disabled!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);

        String message = e.getMessage();
        sendTCPMessage(message);

        e.getPlayer().sendMessage("Message sent to TCP server: " + message);
    }

    private void sendTCPMessage(String message) {
        try (Socket socket = new Socket(TCP_SERVER_IP, TCP_SERVER_PORT);
             OutputStream out = socket.getOutputStream()) {

            out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException ex) {
            log.severe("[ArduinoMK] Failed to send message over TCP: " + ex.getMessage());
        }
    }

    private void startTCPServer() {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(TCP_SERVER_PORT);
                log.severe("[ArduinoMK] TCP Server listening on port " + TCP_SERVER_PORT);

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    log.severe("[ArduinoMK] New connection from " + clientSocket.getInetAddress());

                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                log.severe("[ArduinoMK] Error in TCP Server: " + e.getMessage());
            }
        });

        serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket; var in = clientSocket.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                String receivedMessage = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                log.severe("[ArduinoMK] Received from client: " + receivedMessage);
            }
        } catch (IOException e) {
            log.severe("[ArduinoMK] Client connection error: " + e.getMessage());
        }
    }

    private void stopTCPServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
            }
        } catch (IOException e) {
            log.severe("[ArduinoMK] Error closing TCP server: " + e.getMessage());
        }
    }
}
