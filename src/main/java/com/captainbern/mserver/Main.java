package com.captainbern.mserver;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.PrintWriter;

import static java.util.Arrays.asList;

public class Main {

    public static void main(String[] args) {
        OptionParser optionParser = new OptionParser() {
            {
                acceptsAll(asList("jline", "jlineEnabled")).withRequiredArg().ofType(boolean.class)
                        .describedAs("Whether or not Jline should be enabled").defaultsTo(true);

                acceptsAll(asList("log", "logFile")).withRequiredArg().ofType(String.class)
                        .describedAs("The file which should be used to log").defaultsTo("./logs/Log.log");

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
