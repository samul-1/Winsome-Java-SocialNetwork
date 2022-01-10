package main;

import java.io.File;
import java.io.IOException;

import services.Server;

public class MainServer {
    private static final File DEFAULT_CONFIG_FILE = new File("server_config.json");
    private static final File DEFAULT_API_SCHEMA_FILE = new File("apiRoutes.json");

    public static void main(String[] args) {
        File configFile = MainServer.DEFAULT_CONFIG_FILE;
        File apiSchemaFile = MainServer.DEFAULT_API_SCHEMA_FILE;
        if (args.length > 0) {
            configFile = new File(args[0]);
        }
        if (args.length == 2) {
            configFile = new File(args[1]);
        }
        if (args.length > 2) {
            System.out.println("Usage: java MainServer configFile? apiSchemaFile?");
            System.exit(1);
        }
        try {
            new Server(configFile, apiSchemaFile).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
