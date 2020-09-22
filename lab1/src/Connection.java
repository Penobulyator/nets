import java.net.InetAddress;

public class Connection implements Comparable<Connection>{
    InetAddress ipAddress;
    int port;

    public Connection(InetAddress ipAddress, int port){
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
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
