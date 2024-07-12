///Ricsvagyok, h163500@stud.u-szeged.hu

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import game.racetrack.Direction;
import game.racetrack.RaceTrackPlayer;
import game.racetrack.utils.Coin;
import game.racetrack.utils.PathCell;
import game.racetrack.utils.PlayerState;

import static game.racetrack.RaceTrackGame.*;

public class Agent extends RaceTrackPlayer {

    PathCell endCell, startCell;
    CellWithCosts endCellWithCosts;
    boolean endReached = false;

    LinkedList<Direction> directions = new LinkedList<>();
    int directionIndex;

    public Agent(PlayerState state, Random random, int[][] track, Coin[] coins, int color) {
        super(state, random, track, coins, color);
        LinkedList<PathCell> path = (LinkedList<PathCell>) BFS(state.i, state.j, track);
        endCell = path.getLast();
        startCell = new PathCell(state.i, state.j, null);
        backTrackAStarPath();
    }

    @Override
    public Direction getDirection(long remainingTime) {
        Direction nextAction = directions.get(directionIndex);
        directionIndex++;
        return nextAction;
    }

    private class CellWithCosts extends PathCell {
        int f, g, h;

        public CellWithCosts(int i, int j, PathCell parent) {
            super(i, j, parent);
            this.g = manhattanDistance(this, startCell);
            this.h = Heuristic();
            this.f = this.g + this.h;
        }

        private int Heuristic() {
//            int heur = manhattanDistance(this, endCell);
//            //Ezzel próbálom megoldani hogy felvegye a Coinokat, egyenlőre ink nem lesz ez itt ne zavarjon be
//            if (mask(track[this.i][this.j], COIN)) {
//                heur = 0;
//            }
            return manhattanDistance(this, endCell);
        }
    }

    private void AStar() {
        LinkedList<CellWithCosts> openList = new LinkedList<>();
        LinkedList<CellWithCosts> closedList = new LinkedList<>();
        CellWithCosts current = new CellWithCosts(state.i, state.j, null);
        int i, j;
        int minFCost = Integer.MAX_VALUE;
        int minFCostIndex = 0;


        while (!endReached) {
            closedList.addFirst(current);
            openList.remove(current);

            for (int idx = 1; idx < DIRECTIONS.length; idx++) {
                i = current.i + DIRECTIONS[idx].i;
                j = current.j + DIRECTIONS[idx].j;
                CellWithCosts neighbor = new CellWithCosts(i, j, current);
                if (isNotWall(i, j, track) && !closedList.contains(neighbor) && !openList.contains(neighbor)) {
                    openList.add(neighbor);
                }
            }
            minFCost = Integer.MAX_VALUE;
            minFCostIndex = 0;
            for(int x = 0; x < openList.size(); x++){
                if(openList.get(x).f < minFCost){
                    minFCost = openList.get(x).f;
                    minFCostIndex = x;
                }
                else if(openList.get(x).f == minFCost){
                    if(openList.get(x).g < openList.get(minFCostIndex).g){
                        minFCostIndex = x;
                    }
                }
            }
            current = openList.get(minFCostIndex);
            if(current.same(endCell)){
                endCellWithCosts = current;
                endReached = true;
            }
        }
    }

    private void backTrackAStarPath(){
        AStar();
        CellWithCosts current = endCellWithCosts;

        while(!current.same(startCell)){
            directions.addFirst(direction(current.parent, current));
            if(!current.same(startCell)){
                current = (CellWithCosts) current.parent;
            }
        }
        int i, j;
        LinkedList<Direction> newDirections = new LinkedList<>();
        newDirections.add(directions.get(0));
        for(int x = 0; x < directions.size(); x++){
            if(x + 1 < directions.size()){
                i = directions.get(x + 1).i - directions.get(x).i;
                j = directions.get(x + 1).j - directions.get(x).j;
                Direction next = new Direction(i, j);
                if(Math.abs(i) > 1){
                    i = next.i;
                    j = directions.get(x).j * -1;
                    Direction slowDown = new Direction(i, j);
                    newDirections.add(slowDown);
                    newDirections.add(directions.get(x + 1));
                }
                else if(Math.abs(j) > 1){
                    i = directions.get(x).i * -1;
                    j = next.j;
                    Direction slowDown = new Direction(i, j);
                    newDirections.add(slowDown);
                    newDirections.add(directions.get(x + 1));
                }
                else{
                    newDirections.add(next);
                }
            }
        }
        newDirections.add(directions.getLast());
        directions = newDirections;
        directionIndex = 0;
        //Ezelőttig tökéletes a pályakövetés
        straightLineSpeed();
    }
    //Ha 3nál többet megy egynesen akkor megfogjuk azt a nehány lépést és replaceljük
    //Itt majd olyannal lehet gond hogy a speed amivel kijön az egyenesből nem ugyanolyan mint amikor szépen komótosan lépkedünk
    //Az előző fgv miatt a sok 0,0ás esetet keressük
    //Tehát azt nézzük hogy van egy érték és utána legalább 3 0,0 akkor csinálunk groupot

    //Az a baj ha már egy korábbi sebességből és egy új directionből van összerakva az irány akkor
    //csak a komponens egy részét látja a straighLineSpeed emiatt pl átló helyett a gyorsítást csak az egyik irányba teszi meg így eltéved
    private void straightLineSpeed(){
        LinkedList<Direction> newDirections = new LinkedList<>();
        ArrayList<Direction> group;
        Direction zeroZero = new Direction(0, 0);
        int originalGroupSize;
        while(directionIndex < directions.size()){
            System.out.println(directionIndex);
            group = new ArrayList<>();
            for(int x = directionIndex; x < directions.size(); x++){
                Direction currentDirection = directions.get(x);
                //System.out.println(currentDirection);
                if(!currentDirection.same(zeroZero) && group.size() > 0){
                    break;
                }
                group.add(currentDirection);
//                if(!group.get(0).same(same) && currentDirection.same(same)){
//                    continue;
//                }
//                else{
//                    break;
//                }
            }
            for(int i = 0; i < group.size(); i++){
                System.out.println(i + ". item" + group.get(i));
            }
            originalGroupSize = group.size();
            if(group.size() >= 4){
                group = breakingDistance(group);
            }
            newDirections.addAll(group);
            directionIndex += originalGroupSize;
        }
        directionIndex = 0;
        for(int i = 0; i < newDirections.size(); i++){
            System.out.println(i + ". lepes" + newDirections.get(i));
        }
        directions = newDirections;
    }

    private ArrayList<Direction> breakingDistance(ArrayList<Direction> directions){
        ArrayList<Direction> newDirections = new ArrayList<>();
        int sqrtOfCount = (int) Math.sqrt(directions.size());
//        int squareCount = (int) Math.pow(sqrtOfCount, 2);
//        int currentSpeed = 0;
        Direction accl = directions.get(0);
        Direction decel = breakMechanism(accl);
        newDirections.add(directions.get(0));
        int counter = sqrtOfCount - 1; //Mivel az első gyorsítást már hozzáadtuk
        while(counter > 0){
            newDirections.add(accl);
            counter--;
        }
        counter = sqrtOfCount - 1;
        while(counter > 0){
            newDirections.add(decel);
            counter--;
        }
        return newDirections;
    }

    private Direction breakMechanism(Direction main){
        int i = 0;
        int j = 0;
        if(main.i != 0){
            i = main.i * -1;
        }
        if(main.j != 0){
            j = main.j * -1;
        }
        return new Direction(i, j);
    }

//    private void noDirectionChangeTillEnd(){
//
//    }
}