package com.captainbern.mserver;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.PrintWriter;

import static java.util.Arrays.asList;
import static com.captainbern.mserver.Options.*;

public class Main {

    public static void main(String[] args) {
        OptionParser optionParser = new OptionParser() {
            {
                accepts(USE_CONFIG).withRequiredArg().ofType(boolean.class).defaultsTo(Defaults.USE_CONFIG)
                        .describedAs("Whether or not the server should create/use a config file");

                acceptsAll(asList(JLINE, "jlineEnabled")).withRequiredArg().ofType(boolean.class).defaultsTo(Defaults.JLINE_ENABLED)
                        .describedAs("Whether or not Jline should be enabled");

                acceptsAll(asList(LOG_FILE, "log")).withRequiredArg().ofType(String.class).defaultsTo(Defaults.LOG_FILE)
                        .describedAs("The file which should be used to log");

                acceptsAll(asList(PORT, "p")).withRequiredArg().ofType(int.class).defaultsTo(Defaults.PORT)
                        .describedAs("The port the server should bind to.");

                accepts(IP).withRequiredArg().ofType(String.class).defaultsTo(Defaults.IP)
                        .describedAs("The IP the server should bind to");

                accepts(PROTOCOL).withRequiredArg().ofType(int.class).defaultsTo(Defaults.PROTOCOL)
                        .describedAs("The Protocol version the server should use");

                accepts(VERSION).withRequiredArg().ofType(String.class).defaultsTo(Defaults.VERSION)
                        .describedAs("The version (will appear client side when the protocol versions don't match)");

                accepts(ONLINE_PLAYERS).withRequiredArg().ofType(int.class).defaultsTo(Defaults.ONLINE_PLAYERS)
                        .describedAs("The online players (will appear client side)");

                accepts(MAX_ONLINE).withRequiredArg().ofType(int.class)
                        .describedAs("The max online players (will appear client side)").defaultsTo(Defaults.MAX_ONLINE_PLAYERS);

                accepts(MOTD).withRequiredArg().ofType(String.class).defaultsTo(Defaults.MOTD)
                        .describedAs("The motd");

                accepts(KICK_MESSAGE).withRequiredArg().ofType(String.class).defaultsTo(Defaults.KICK_MESSAGE)
                        .describedAs("The default kick message that will be send to the player when he/she tries to login");

                accepts(KICK_MESSAGE_BANNED).withRequiredArg().ofType(String.class).defaultsTo(Defaults.KICK_MESSAGE_BANNED)
                        .describedAs("The default kick message that will be used when a banned player tries to login");

                accepts(KICK_MESSAGE_NOT_ON_WHITELIST).withRequiredArg().ofType(String.class).defaultsTo(Defaults.KICK_MESSAGE_NOT_ON_WHITELIST)
                        .describedAs("The default kick message that will be used when a non-whitelisted player tries to login");

                acceptsAll(asList("h", "?", "help", "info"), "Displays some help/info");
            }
        };

        OptionSet optionSet = optionParser.parse(args);

        if (optionSet.has("help")) {
            PrintWriter printWriter = null;
            try {
                optionParser.printHelpOn(printWriter = new PrintWriter(System.out));
            } catch (IOException e) {
                MaintenanceServer.LOGGER.warn("Failed to display the help!", e);
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
            return;
        }

        MaintenanceServer server = new MaintenanceServer(optionSet);

        try {
            server.start();
        } catch (Throwable throwable) {
            MaintenanceServer.LOGGER.error("An error occurred during server startup", throwable);
            System.exit(-1);
        }
    }
}
