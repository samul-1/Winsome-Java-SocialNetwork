package main;

import java.io.File;
import java.io.IOException;

import client.Client;

public class MainClient {
    private static final File DEFAULT_CONFIG_FILE = new File("client_config.json");

    public static void main(String[] args) {
        File configFile = MainClient.DEFAULT_CONFIG_FILE;
        if (args.length == 1) {
            configFile = new File(args[0]);
        } else if (args.length > 1) {
            System.out.println("Usage: java MainClient configFile?");
            System.exit(1);
        }
        try {
            new Client(configFile).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
