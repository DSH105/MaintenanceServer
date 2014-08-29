package com.captainbern.mserver;

public class Options {

    public static final String USE_CONFIG = "config";
    public static final String JLINE = "jline";
    public static final String LOG_FILE = "logFile";

    public static final String PORT = "port";
    public static final String IP = "ip";

    public static final String PROTOCOL = "protocol";
    public static final String VERSION = "version";

    public static final String ONLINE_PLAYERS = "online";
    public static final String MAX_ONLINE = "max-online";

    public static final String MOTD = "motd";

    public static final String KICK_MESSAGE = "kick-message";
    public static final String KICK_MESSAGE_BANNED = "kick-message-banned";
    public static final String KICK_MESSAGE_NOT_ON_WHITELIST = "kick-message-not-whitelisted";

    public static class Defaults {
        public static final boolean USE_CONFIG = true;
        public static final boolean JLINE_ENABLED = true;
        public static final String LOG_FILE = "./logs/Log.log";

        public static final int PORT = 25566;
        public static final String IP = "127.0.0.1";

        public static final int PROTOCOL = -1;
        public static final String VERSION = "MaintenanceServer";

        public static final int ONLINE_PLAYERS = 0;
        public static final int MAX_ONLINE_PLAYERS = 0;

        public static final String MOTD = "A Maintenance Server!";

        public static final String KICK_MESSAGE = "This is a Maintenance Server you silly goose!";
        public static final String KICK_MESSAGE_BANNED = "You are banned from this server";
        public static final String KICK_MESSAGE_NOT_ON_WHITELIST = "You are not whitelisted!";
    }
}
