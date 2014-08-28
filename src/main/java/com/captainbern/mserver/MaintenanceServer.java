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
import java.net.UnknownHostException;

public class MaintenanceServer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceServer.class);

    private final ConsoleManager consoleManager = new ConsoleManager(this);

    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final OptionSet options;

    private final PropertyHandler propertyHandler;

    private int port;
    private String ip;

    private JSONObject pingResponse;

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

        // Create the server root
        if (!getRoot().exists())
            getRoot().mkdir();

        this.options = options;

        LOGGER.info("Loading properties");
        // This will create the default properties...
        this.propertyHandler = new PropertyHandler(new File(getRoot(), "maintenance-server.properties"));
        this.propertyHandler.getInt("protocol", -1);
        this.propertyHandler.getString("version", "Maintenance Server");
        this.propertyHandler.getInt("online", 0);
        this.propertyHandler.getInt("maxOnline", 0);
        this.propertyHandler.getString("motd", "A Maintenance Server");
        this.propertyHandler.getString("kickMessage", "This is a Maintenance Server you silly goose!");

        this.port = this.propertyHandler.getInt("port", 25566);
        this.ip = this.propertyHandler.getString("ip", "127.0.0.1");

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
            this.bind(InetAddress.getByName(this.ip), this.port);
        } catch (UnknownHostException e) {
            LOGGER.warn("Unknown ip: " + this.ip);
        }

        try {
            this.handleFavicon();
        } catch (IOException e) {
            LOGGER.warn("Something went wrong while reading the server-icon!");
        }

        this.consoleManager.startConsole((Boolean) options.valueOf("jline"));
        this.consoleManager.startFile((String) options.valueOf("logFile"));

        long done = System.currentTimeMillis() - start;
        LOGGER.info("Done (" + done + "ms)! To stop the server, type \"stop\" or \"halt\"");
    }

    public void bind(InetAddress address, int port) {
        LOGGER.info("Starting a Maintenance Server on " + this.ip + ":" + port);

        ChannelFuture future = this.bootstrap.bind(address, port);
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
        version.put("name", this.propertyHandler.getString("version", "Maintenance Server"));
        version.put("protocol", this.propertyHandler.getInt("protocol", 5));
        this.pingResponse.put("version", version);

        JSONObject players = new JSONObject();
        players.put("max", this.propertyHandler.getInt("maxOnline", 0));
        players.put("online", this.propertyHandler.getInt("online", 0));
        this.pingResponse.put("players", players);

        JSONObject description = new JSONObject();
        description.put("text", this.propertyHandler.getString("motd", "A Maintenance Server"));
        this.pingResponse.put("description", description);
    }

    public JSONObject getPingResponse() {
        if (this.pingResponse == null)
            createPingResponse();

        return this.pingResponse;
    }

    public String getKickMessage() {
        return this.propertyHandler.getString("kickMessage", "This is a Maintenance Server you silly goose!");
    }

    protected void handleCommand(String command) {
        if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("halt"))
            this.stop();
    }

    public File getRoot() {
        return new File(".");
    }
}
