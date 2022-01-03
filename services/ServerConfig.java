package services;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerConfig {
    private InetAddress serverAddr; // = Inet4Address.getLocalHost();
    private int tcpPort = 8000;
    private int udpPort = 33333;
    private InetAddress multicastAddr; // InetAddress.getByName("239.255.32.32");
    private int multicastPort = 44444;
    private InetAddress registryHost; // = InetAddress.getLocalHost();
    private int registryPort = 7777;
    private int sktTimeout = 100000;
    private String storageLocation = "db.json";
    private long timeInBetweenRewards = 10; // in seconds
    private double authorRewardPercentage = 70.0;

    public ServerConfig() throws UnknownHostException {
        this.multicastAddr = InetAddress.getByName("239.255.32.32");
        this.serverAddr = InetAddress.getLocalHost();
        this.registryHost = InetAddress.getLocalHost();
    }

    public long getTimeInBetweenRewards() {
        return this.timeInBetweenRewards;
    }

    public double getAuthorRewardPercentage() {
        return this.authorRewardPercentage;
    }

    public void setTimeInBetweenRewards(long time) {
        this.timeInBetweenRewards = time;
    }

    public void setAuthorRewardPercentage(double pct) {
        this.authorRewardPercentage = pct;
    }

    public InetAddress getServerAddr() {
        return this.serverAddr;
    }

    public void setServerAddr(InetAddress serverAddr) {
        this.serverAddr = serverAddr;
    }

    public int getTcpPort() {
        return this.tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getUdpPort() {
        return this.udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public InetAddress getMulticastAddr() {
        return this.multicastAddr;
    }

    public void setMulticastAddr(InetAddress multicastAddr) {
        this.multicastAddr = multicastAddr;
    }

    public int getMulticastPort() {
        return this.multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public InetAddress getRegistryHost() {
        return this.registryHost;
    }

    public void setRegistryHost(InetAddress registryHost) {
        this.registryHost = registryHost;
    }

    public int getRegistryPort() {
        return this.registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public int getSktTimeout() {
        return this.sktTimeout;
    }

    public void setSktTimeout(int sktTimeout) {
        this.sktTimeout = sktTimeout;
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }
}
