package model.client;

import model.snakeProto.SnakeProto;

public interface SnakeClientListener {
    void updateState(SnakeProto.GameState gameState);
    void gotException(Exception exception);
    void gameOver();
}
