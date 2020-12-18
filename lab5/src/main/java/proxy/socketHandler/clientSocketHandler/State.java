package proxy.socketHandler.clientSocketHandler;

public enum State {
    READING_GREETING,
    SENDING_CHOICE,
    READING_CONNECTION_REQUEST,
    SENDING_RESPONSE,
    FORWARDING
}
