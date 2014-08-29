package com.captainbern.mserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import joptsimple.OptionSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import static com.captainbern.mserver.Options.*;

public class MaintenanceServer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceServer.class);

    private final ConsoleManager consoleManager = new ConsoleManager(this);

    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final OptionSet options;
    private PropertyHandler propertyHandler;

    private int port;
    private String ip;

    private JSONObject pingResponse;
    private int protocolVersion;
    private String serverVersion;
    private int onlinePlayers;
    private int maxOnlinePlayers;
    private String motd;

    private String defaultKickMessage;
    private String kickMessageNotOnWhiteList;
    private String kickMessageBanned;

    private static String BANNED_PLAYERS = "banned-players.json";
    private static String BANNED_IPS = "banned-ips.json";
    private static String WHITELIST = "whitelist.json";

    public MaintenanceServer(OptionSet options) {
        this.bootstrap
                .group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast("handler", new PacketHandler(MaintenanceServer.this));
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true);

        this.options = options;

        if (((Boolean) options.valueOf(USE_CONFIG))) {
            this.propertyHandler = new PropertyHandler(new File(getRoot(), "maintenance-server.properties"));
            loadProperties();
        } else {
            loadArgs(options);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("MaintenanceServer Shutdown Hook") {
            @Override
            public void run() {
                MaintenanceServer.this.stop();
            }
        });
    }

    public void start() {
        long start = System.currentTimeMillis();

        try {
            this.handleFavicon();
        } catch (IOException e) {
            LOGGER.warn("Something went wrong while reading the server-icon!");
        }

        this.consoleManager.startConsole((Boolean) options.valueOf(JLINE));
        this.consoleManager.startFile((String) options.valueOf(LOG_FILE));

        long done = System.currentTimeMillis() - start;
        LOGGER.info("Done (" + done + "ms)! To stop the server, type \"stop\" or \"halt\"");
    }

    public void bind() {

        SocketAddress address;
        if (ip.isEmpty()) {
            address = new InetSocketAddress(this.port);
        } else {
            address = new InetSocketAddress(this.ip, this.port);
        }

        LOGGER.info("Starting a Maintenance Server on " + address);

        ChannelFuture future = this.bootstrap.bind(address);
        Channel channel = future.awaitUninterruptibly().channel();

        if (!channel.isActive()) {
            throw new RuntimeException("**** FAILED TO BIND TO PORT! Perhaps a server is already running on that port?");
        }
    }

    public void stop() {
        LOGGER.info("Stopping server");
        this.consoleManager.stop();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
    }

    private void loadProperties() {
        LOGGER.info("Loading properties");
        // This will create the default properties...
        this.protocolVersion = this.propertyHandler.getInt(PROTOCOL, Defaults.PROTOCOL);
        this.serverVersion = this.propertyHandler.getString(VERSION, Defaults.VERSION);
        this.onlinePlayers = this.propertyHandler.getInt(ONLINE_PLAYERS, Defaults.ONLINE_PLAYERS);
        this.maxOnlinePlayers = this.propertyHandler.getInt(MAX_ONLINE, Defaults.MAX_ONLINE_PLAYERS);
        this.motd = this.propertyHandler.getString(MOTD, Defaults.MOTD);
        this.kickMessageBanned = this.propertyHandler.getString(KICK_MESSAGE, Defaults.KICK_MESSAGE);
        this.kickMessageNotOnWhiteList = this.propertyHandler.getString(KICK_MESSAGE_NOT_ON_WHITELIST, Defaults.KICK_MESSAGE_NOT_ON_WHITELIST);
        this.kickMessageBanned = this.propertyHandler.getString(KICK_MESSAGE_BANNED, Defaults.KICK_MESSAGE_BANNED);

        this.port = this.propertyHandler.getInt(PORT, Defaults.PORT);
        this.ip = this.propertyHandler.getString(IP, Defaults.IP);
    }

    private void loadArgs(OptionSet set) {
        this.port = (int) set.valueOf(PORT);
        this.ip = (String) set.valueOf(IP);
        this.protocolVersion = (int) set.valueOf(PROTOCOL);
        this.serverVersion = (String) set.valueOf(VERSION);
        this.onlinePlayers = (int) set.valueOf(ONLINE_PLAYERS);
        this.maxOnlinePlayers = (int) set.valueOf(MAX_ONLINE);
        this.motd = (String) set.valueOf(MOTD);
        this.defaultKickMessage = (String) set.valueOf(KICK_MESSAGE);
        this.kickMessageBanned = (String) set.valueOf(KICK_MESSAGE_BANNED);
        this.kickMessageNotOnWhiteList = (String) set.valueOf(KICK_MESSAGE_NOT_ON_WHITELIST);

    }

    private void handleFavicon() throws IOException {
        File favicon = new File("server-icon.png");
        if (favicon.exists()) {
            BufferedImage image = ImageIO.read(favicon);

            if (image.getWidth() != 64 || image.getHeight() != 64) {
                LOGGER.warn("Found a server-icon.png but it has an illegal size! Please make sure your server-icon is 64x64");
                return;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            if (this.pingResponse == null)
                createPingResponse();

            this.pingResponse.put("favicon", "data:image/png;base64," + DatatypeConverter.printBase64Binary(outputStream.toByteArray()));
        }
    }

    private void createPingResponse() {
        this.pingResponse = new JSONObject();

        JSONObject version = new JSONObject();
        version.put("name", this.serverVersion);
        version.put("protocol", this.protocolVersion);
        this.pingResponse.put("version", version);

        JSONObject players = new JSONObject();
        players.put("max", this.maxOnlinePlayers);
        players.put("online", this.onlinePlayers);
        this.pingResponse.put("players", players);

        JSONObject description = new JSONObject();
        description.put("text", this.motd);
        this.pingResponse.put("description", description);
    }

    public JSONObject getPingResponse() {
        if (this.pingResponse == null)
            createPingResponse();

        return this.pingResponse;
    }

    public String getKickMessage() {
        return this.defaultKickMessage;
    }

    protected void handleCommand(String command) {
        if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("halt"))
            this.stop();
    }

    public File getRoot() {
        return new File(".");
    }
}
