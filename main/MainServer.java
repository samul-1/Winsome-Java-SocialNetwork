package main;

import java.io.File;
import java.io.IOException;

import services.Server;

public class MainServer {
    private static final File DEFAULT_CONFIG_FILE = new File("config.json");
    private static final File DEFAULT_API_SCHEMA_FILE = new File("apiRoutes.json");

    public static void main(String[] args) {
        File configFile = MainServer.DEFAULT_CONFIG_FILE;
        File apiSchemaFile = MainServer.DEFAULT_API_SCHEMA_FILE;
        // TODO read arguments for config and schema file
        try {
            new Server(configFile, apiSchemaFile).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
