import java.net.InetAddress;
import java.net.UnknownHostException;

public class Connection implements Comparable<Connection>{
    private InetAddress ipAddress;
    private int port;

    public Connection(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public boolean equals(Connection otherConnection) {
        return this.ipAddress.equals(otherConnection.ipAddress) && this.port == otherConnection.port;
    }

    @Override
    public String toString() {
        return ipAddress.toString().substring(1) + ":" + port;
    }

    public static Connection fromString(String source) throws UnknownHostException {
        String[] splitted = source.split(":");
        InetAddress address = InetAddress.getByName(splitted[0]);
        int port = Integer.parseInt(splitted[1]);
        return new Connection(address, port);
    }

    @Override
    public int compareTo(Connection o) {
        int ret = this.port - o.port;
        if (ret == 0)
            return this.ipAddress.toString().compareTo(o.ipAddress.toString());
        else
            return ret;
    }
}
