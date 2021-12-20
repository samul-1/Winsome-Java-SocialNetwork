package main;

import java.io.File;
import java.io.IOException;

import client.Client;

public class MainClient {
    private static final File DEFAULT_CONFIG_FILE = new File("config.json");

    public static void main(String[] args) {
        System.out.println("Started main client");
        File configFile = MainClient.DEFAULT_CONFIG_FILE;
        try {
            new Client(configFile).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
