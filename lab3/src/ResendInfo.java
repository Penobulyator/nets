public class ResendInfo {
    public Connection connection;
    public Message message;
    public int silenceTime;

    public ResendInfo(Connection connection, Message message, int silenceTime) {
        this.connection = connection;
        this.message = message;
        this.silenceTime = silenceTime;
    }
}
