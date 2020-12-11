package model.snakeNode;

import model.snakeProto.SnakeProto;

public interface SnakeNodeListener {
    void updateState(SnakeProto.GameState gameState);
    void gotException(Exception exception);
    void gameOver();
}
