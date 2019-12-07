package dto;

public class PacketInfo {

    private String username;
    private String password;
    private String sipHost;
    private int sipPort;

    public PacketInfo() {
    }

    public PacketInfo(String username, String password, String sipHost, int sipPort) {
        this.username = username;
        this.password = password;
        this.sipHost = sipHost;
        this.sipPort = sipPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSipHost() {
        return sipHost;
    }

    public void setSipHost(String sipHost) {
        this.sipHost = sipHost;
    }

    public int getSipPort() {
        return sipPort;
    }

    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }
}
