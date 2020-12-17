package model.snakeNode;

import model.snakeProto.SnakeProto;

public interface SnakeNodeListener {
    void updateState(SnakeProto.GameState gameState, int myId);
    void gotException(Exception exception);
    void gameOver();
}
