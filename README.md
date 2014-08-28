MaintenanceServer
=================

MaintenanceServer is a small program which mimics the (ping) behaviour of a normal Minecraft Server.
When a client pings the server, it will send a message back containing the ping-data you have configured in
the maintenance-server.properties file.

MaintenanceServer is still a work-in-progress and will constantly be updated.

To-do:
======

- Add Legady ping handler
- Support colors in the messages/make use of the new message format
- When used in a normal Bukkit server environment (when placed in the same folder as CraftBukkit.jar)
  make it use the whitelist etc...