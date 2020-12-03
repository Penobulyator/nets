package model.server;

import model.snakeProto.SnakeProto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SnakeModel {
    List<SnakeProto.GameState.Coord> food = new LinkedList<>();
    Map<SnakeProto.GamePlayer, SnakeProto.GameState.Snake> snakeMap = new ConcurrentHashMap<>();

    public SnakeModel(SnakeProto.GameConfig config) {
        this.gameConfig = config;
    }

    SnakeProto.GameConfig gameConfig;


    private int stateOrder = 0;

    private SnakeProto.GameState.Coord round(SnakeProto.GameState.Coord coord){
        return SnakeProto.GameState.Coord.newBuilder()
                .setX((gameConfig.getWidth() + coord.getX()) % gameConfig.getWidth())
                .setY((gameConfig.getHeight() + coord.getY()) % gameConfig.getHeight())
                .build();
    }

    private boolean snakeChangedDirection(SnakeProto.GameState.Snake snake){
        SnakeProto.GameState.Coord firstOffset = snake.getPoints(1);

        if (firstOffset.getX() == 0){
            if (firstOffset.getY() > 0)
                return snake.getHeadDirection() != SnakeProto.Direction.UP;
            else
                return snake.getHeadDirection() != SnakeProto.Direction.DOWN;
        }
        else {
            if (firstOffset.getX() > 0)
                return snake.getHeadDirection() != SnakeProto.Direction.LEFT;
            else
                return snake.getHeadDirection() != SnakeProto.Direction.RIGHT;
        }
    }

    private SnakeProto.GameState.Snake moveSnakeHead(SnakeProto.GameState.Snake snake){
        SnakeProto.GameState.Coord currentHead = snake.getPointsList().get(0);
        SnakeProto.Direction direction = snake.getHeadDirection();
        SnakeProto.GameState.Coord newHead = null;
        switch (direction){
            case UP:
                newHead = SnakeProto.GameState.Coord.newBuilder()
                        .setX(currentHead.getX())
                        .setY(currentHead.getY() - 1)
                        .build();
                break;
            case DOWN:
                newHead = SnakeProto.GameState.Coord.newBuilder()
                        .setX(currentHead.getX())
                        .setY(currentHead.getY() + 1)
                        .build();
                break;
            case LEFT:
                newHead = SnakeProto.GameState.Coord.newBuilder()
                        .setX(currentHead.getX() - 1)
                        .setY(currentHead.getY())
                        .build();
                break;
            case RIGHT:
                newHead = SnakeProto.GameState.Coord.newBuilder()
                        .setX(currentHead.getX() + 1)
                        .setY(currentHead.getY())
                        .build();
                break;
        }
        newHead = round(newHead);


        SnakeProto.GameState.Coord secondBodyCord = snake.getPoints(1);
        if (!snakeChangedDirection(snake)){
            if (secondBodyCord.getX() == 0){
                if (secondBodyCord.getY() > 0)
                    secondBodyCord = secondBodyCord.toBuilder().setY(secondBodyCord.getY() + 1).build();
                else
                    secondBodyCord = secondBodyCord.toBuilder().setY(secondBodyCord.getY() - 1).build();
            }
            else if (secondBodyCord.getY() == 0){
                if (secondBodyCord.getX() > 0)
                    secondBodyCord = secondBodyCord.toBuilder().setX(secondBodyCord.getX() + 1).build();
                else
                    secondBodyCord = secondBodyCord.toBuilder().setX(secondBodyCord.getX() - 1).build();
            }

            return snake.toBuilder().setPoints(0, newHead).setPoints(1, secondBodyCord).build();
        }
        else {
            switch (snake.getHeadDirection()){
                case UP:
                    secondBodyCord = SnakeProto.GameState.Coord.newBuilder()
                            .setX(0)
                            .setY(1)
                            .build();
                    break;
                case DOWN:
                    secondBodyCord = SnakeProto.GameState.Coord.newBuilder()
                            .setX(0)
                            .setY(-1)
                            .build();
                    break;
                case LEFT:
                    secondBodyCord = SnakeProto.GameState.Coord.newBuilder()
                            .setX(1)
                            .setY(0)
                            .build();
                    break;
                case RIGHT:
                    secondBodyCord = SnakeProto.GameState.Coord.newBuilder()
                            .setX(-1)
                            .setY(0)
                            .build();
                    break;
            }

            return snake.toBuilder().setPoints(0, secondBodyCord).addPoints(0, newHead).build();
        }

    }

    private SnakeProto.GameState.Coord getSnakePoint(SnakeProto.GameState.Snake snake, int bendIndex){
        SnakeProto.GameState.Coord point = snake.getPoints(0);
        for (int i = 1; i < bendIndex; i++){
            point.toBuilder()
                    .setX(point.getX() + snake.getPoints(i).getX())
                    .setY(point.getY() + snake.getPoints(i).getY())
                    .build();
        }
        return point;
    }

    private SnakeProto.GameState.Snake moveSnakeTail(SnakeProto.GameState.Snake snake){
        SnakeProto.GameState.Coord tail = snake.getPoints(snake.getPointsCount() - 1);
        if (tail.getX() == 0){
            if (tail.getY() > 0)
                tail = tail.toBuilder().setY(tail.getY() - 1).build();
            else
                tail = tail.toBuilder().setY(tail.getY() + 1).build();
        }
        else if (tail.getY() == 0) {

            if (tail.getX() > 0)
                tail = tail.toBuilder().setX(tail.getX() - 1).build();
            else
                tail = tail.toBuilder().setX(tail.getX() + 1).build();
        }
        if (tail.getX() == 0 && tail.getY() == 0)
            return snake.toBuilder().removePoints(snake.getPointsCount() - 1).build();
        else
            return snake.toBuilder().setPoints(snake.getPointsCount() - 1, tail).build();

    }

    private java.util.List<SnakeProto.GameState.Coord> snakeBodyCords(SnakeProto.GameState.Snake snake){
        java.util.List<SnakeProto.GameState.Coord> snakeOffsetPoints = snake.getPointsList();
        List<SnakeProto.GameState.Coord> snakeBodyPoints = new LinkedList<>();
        snakeBodyPoints.add(snakeOffsetPoints.get(0)); //add head
        for (int i = 1; i < snake.getPointsCount(); i++){
            SnakeProto.GameState.Coord lastBodyPoint = snakeBodyPoints.get(snakeBodyPoints.size() - 1);

            int xOffset = snakeOffsetPoints.get(i).getX();
            int yOffset = snakeOffsetPoints.get(i).getY();

            if (xOffset == 0){
                if (yOffset < 0)
                    for (int j = 0; j  < -yOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setY((lastBodyPoint.getY() - 1) % gameConfig.getHeight()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
                else
                    for (int j = 0; j  < yOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setY((lastBodyPoint.getY() + 1) % gameConfig.getHeight()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
            }
            else if (yOffset == 0){
                if (xOffset < 0)
                    for (int j = 0; j  < -xOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setX((lastBodyPoint.getX() - 1) % gameConfig.getWidth()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
                else
                    for (int j = 0; j  < xOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setX((lastBodyPoint.getX() + 1) % gameConfig.getWidth()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
            }
        }
        return  snakeBodyPoints;
    }

    void checkForCollisions(){
        for (Map.Entry<SnakeProto.GamePlayer, SnakeProto.GameState.Snake> snakeMapEntry: snakeMap.entrySet()){
            SnakeProto.GameState.Snake firstSnake = snakeMapEntry.getValue();
            SnakeProto.GameState.Coord firstSnakeHead = firstSnake.getPoints(0);
            for (SnakeProto.GameState.Snake secondSnake: snakeMap.values()){
                List<SnakeProto.GameState.Coord> snakeBodyCords = snakeBodyCords(secondSnake);
                if (snakeBodyCords.contains(firstSnakeHead)){
                    if (firstSnake == secondSnake && snakeBodyCords.indexOf(firstSnakeHead) == 0){
                        return;
                    }
                    else {
                        /*first snake dies*/

                        //randomly place food on dead snake body cords
                        for(SnakeProto.GameState.Coord firstSnakeBodyCoord: snakeBodyCords(firstSnake)){
                            if (new Random().nextFloat() > gameConfig.getDeadFoodProb())
                                food.add(firstSnakeBodyCoord);
                        }

                        //remove snake from game
                        snakeMap.remove(snakeMapEntry.getKey());
                        System.out.println(snakeMapEntry.getKey().getName() + " lost");
                    }
                }
            }
        }
    }

    private void eatFood(){
        for (Map.Entry<SnakeProto.GamePlayer, SnakeProto.GameState.Snake> snakeMapEntry: snakeMap.entrySet()){
            SnakeProto.GamePlayer player = snakeMapEntry.getKey();
            SnakeProto.GameState.Snake snake = snakeMapEntry.getValue();

            SnakeProto.GameState.Coord snakeHead = snake.getPoints(0);

            boolean snakeAteFood = false;
            for (SnakeProto.GameState.Coord foodCord: food){
                if (foodCord.equals(snakeHead)){
                    //snake eats food
                    player.toBuilder().setScore(player.getScore() + 1);
                    snakeAteFood = true;
                    food.remove(foodCord);
                    break;
                }
            }
            if (!snakeAteFood){
                snakeMapEntry.setValue(moveSnakeTail(snake));
            }
        }
    }

    boolean[][] getOccupiedFields(){
        boolean [][]occupiedFields = new boolean[gameConfig.getWidth()][gameConfig.getHeight()];
        for (boolean[] arr: occupiedFields){
            Arrays.fill(arr, false);
        }

        //set fields with food as occupied
        for(SnakeProto.GameState.Coord foodCord: food){
            occupiedFields[foodCord.getX()][foodCord.getY()] = true;
        }

        //set fields with snakes body parts as occupied
        for (SnakeProto.GameState.Snake snake: snakeMap.values()){
            for (SnakeProto.GameState.Coord snakeBodyCoord: snakeBodyCords(snake))
                occupiedFields[snakeBodyCoord.getX()][snakeBodyCoord.getY()] = false;
        }

        return occupiedFields;
    }

    //return upper left coord of 5x5 free square or null if there is no such square
    private SnakeProto.GameState.Coord findFreeSquare(){
        boolean[][] occupiedFields = getOccupiedFields();

        //find free square
        for (int i = 0; i < gameConfig.getWidth() - 5; i++)
            for (int j = 0; j < gameConfig.getHeight() - 5; j++){
                //check if square {i, j} {i + 5} {j + 5} is free
                boolean squareIsFree = true;
                for (int rightOffset = 0; rightOffset < 5; rightOffset++)
                    for(int downOffset = 0 ; downOffset < 5; downOffset ++)
                        if (occupiedFields[i + rightOffset][j + downOffset]){
                            //square has occupied filed
                            squareIsFree = false;
                            break;
                        }
                if (squareIsFree)
                    return SnakeProto.GameState.Coord.newBuilder()
                            .setX(i)
                            .setY(j)
                            .build();
            }
        return null;
    }

    private void placeNewFood(){
        boolean[][] occupiedFields = getOccupiedFields();

        int foodRequired = gameConfig.getFoodStatic() + (int)(gameConfig.getFoodPerPlayer() * snakeMap.size());

        for (int i = 0; i < foodRequired - food.size(); i++){
            //find place for food
            Random random = new Random();
            int x = random.nextInt(gameConfig.getWidth() - 1);
            int y = random.nextInt(gameConfig.getHeight() - 1);
            while(occupiedFields[x][y]){
                x = random.nextInt(gameConfig.getWidth());
                y = random.nextInt(gameConfig.getWidth());
            }

            occupiedFields[x][y] = true;
            food.add(SnakeProto.GameState.Coord.newBuilder()
                    .setX(x)
                    .setY(y)
                    .build());
        }
    }

    //returns true if player was added, returns false if there is no space for player
    public boolean addPlayer(SnakeProto.GamePlayer player){
        SnakeProto.GameState.Coord freeSquare = findFreeSquare();
        if (freeSquare == null)
            return false;
        //place head in center of square
        SnakeProto.GameState.Coord head = SnakeProto.GameState.Coord.newBuilder()
                .setX(freeSquare.getX() + 2)
                .setY(freeSquare.getY() + 2)
                .build();

        //pick random direction
        SnakeProto.Direction direction = SnakeProto.Direction.forNumber(new Random().nextInt(3) + 1);
        assert direction != null;

        //create tail offset coord
        SnakeProto.GameState.Coord tail = SnakeProto.GameState.Coord.newBuilder()
                .setX(0)
                .setY(0)
                .build();

        switch (direction){
            case UP:
                tail = tail.toBuilder().setY(1).build();
                break;
            case DOWN:
                tail = tail.toBuilder().setY(-1).build();
                break;
            case LEFT:
                tail = tail.toBuilder().setX(1).build();
                break;
            case RIGHT:
                tail = tail.toBuilder().setX(-1).build();
                break;
        }

        //create snake
        SnakeProto.GameState.Snake snake = SnakeProto.GameState.Snake.newBuilder()
                .setPlayerId(player.getId())
                .addPoints(0, head)
                .addPoints(1, tail)
                .setState(SnakeProto.GameState.Snake.SnakeState.ALIVE)
                .setHeadDirection(direction)
                .build();

        //add player and snake to map
        snakeMap.put(player, snake);

        return true;
    }

    public void changeDirection(SnakeProto.GamePlayer player, SnakeProto.Direction direction){
        SnakeProto.GameState.Snake snake = snakeMap.get(player);

        switch (direction){
            case UP:
                if (snake.getHeadDirection() == SnakeProto.Direction.DOWN)
                    return;
                break;
            case DOWN:
                if (snake.getHeadDirection() == SnakeProto.Direction.UP)
                    return;
                break;
            case LEFT:
                if (snake.getHeadDirection() == SnakeProto.Direction.RIGHT)
                    return;
                break;
            case RIGHT:
                if (snake.getHeadDirection() == SnakeProto.Direction.LEFT)
                    return;
                break;
        }
        snakeMap.put(player, snake.toBuilder().setHeadDirection(direction).build());
    }

    public void changeState(){
        for (Map.Entry<SnakeProto.GamePlayer, SnakeProto.GameState.Snake> snakeEntry : snakeMap.entrySet()){
            snakeEntry.setValue(moveSnakeHead(snakeEntry.getValue()));
        }
        eatFood();
        checkForCollisions();
        placeNewFood();
        stateOrder++;
    }

    public SnakeProto.GameState getGameState(){
        return SnakeProto.GameState.newBuilder()
                .setStateOrder(stateOrder)
                .addAllSnakes(snakeMap.values())
                .addAllFoods(food)
                .setPlayers(SnakeProto.GamePlayers.newBuilder().addAllPlayers(snakeMap.keySet()))
                .setConfig(gameConfig)
                .build();
    }
}
