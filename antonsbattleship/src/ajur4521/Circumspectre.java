package ajur4521;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import util.Coordinate;
import util.Ship;
import util.ShipPlacement;
import util.ShipPlacement.Direction;
import core.GameConfiguration;
import core.Player;

public class Circumspectre implements Player {
	enum Phase{SEARCH1, SEARCH2, PERIMETER, DESTROY};
	Phase phase;
	ArrayList<int[]> adjacency;
	
	GameConfiguration configuration;
	
	Coordinate[][] coords;
	
	ArrayList<Coordinate> searchPattern1;
	ArrayList<Coordinate> searchPattern2;
	ArrayList<Coordinate> destroyPattern; 
	ArrayList<Coordinate> perimPattern;
	ArrayList<int[]> prefDirections;
	HashSet<Coordinate> destroyed;
	
	Coordinate lastShot;
	
	int height;
	int width;
	
	int nonFatalHits;
	int perimeterHits;
	
	Random random;
	
	public Circumspectre() {
		phase = Phase.SEARCH1;
		height = 10;
		width = 10;
		nonFatalHits = 0;
		perimeterHits = 0;
		
		adjacency = new ArrayList<int[]>();
		adjacency.add(new int[]{0,1});
		adjacency.add(new int[]{0,-1});
		adjacency.add(new int[]{1,0});
		adjacency.add(new int[]{-1,0});
		
		prefDirections = new ArrayList<int[]>();
		
		coords = new Coordinate[height][width];
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				coords[i][j] = new Coordinate(j, i);
			}
		}
		
		
		searchPattern1 = generateSearchPattern(3, 4, width, height);
		
		ArrayList<Coordinate> perimCoords = new ArrayList<Coordinate>();
		for (Coordinate c : searchPattern1) {
			if (c.getX() == 0 || c.getX() == width-1 || c.getY() == 0 || c.getY() == height-1) {
				perimCoords.add(c);
			}
		}
		
		for (Coordinate c : perimCoords) {
			searchPattern1.remove(c);
			searchPattern1.add(0, c);
		}
		
		searchPattern2 = generateSearchPattern(1, 4, width, height);
		
		perimPattern = generatePerimeterPattern(2, width, height);
		
		destroyPattern = new ArrayList<Coordinate>();
		destroyed = new HashSet<Coordinate>();
		
		lastShot = new Coordinate(-1, -1);
		
		random = new Random();
	}
	
	public ArrayList<Coordinate> generatePerimeterPattern(int spacing, int width, int height) {
		ArrayList<Coordinate> perim = new ArrayList<Coordinate>();
		
		//NORTH
		for (int i = 1; i < width; i += spacing) {
			perim.add(coords[0][i]);
		}
		//EAST
		for (int i = perim.contains(coords[0][width-1]) ? 2 : 1; i < height; i += spacing) {
			perim.add(coords[i][width - 1]);
		}
		//WEST
		for (int i = 1; i < height; i += spacing) {
			perim.add(coords[i][0]);
		}
		//SOUTH
		for (int i = perim.contains(coords[0][width-1]) ? 2 : 1; i < height - 1; i += spacing) {
			perim.add(coords[height - 1][i]);
		}
		
		Collections.shuffle(perim);
		return perim;
	}
	
	public ArrayList<Coordinate> generateSearchPattern(int offset, int interval, int width, int height) {
		ArrayList<Coordinate> pattern = new ArrayList<Coordinate>();
		int startingPos = offset;
		int currentPos;
		
		for (int row = 0; row < height; row++) {
			currentPos = startingPos;
			while (currentPos < width) {
				pattern.add(coords[row][currentPos]);
				currentPos += interval;
			}
			
			startingPos--;
			if (startingPos < 0) startingPos += interval;
		}
		Collections.shuffle(pattern);
		return pattern;	
	}
	
	public boolean isValidCoordinate(int x, int y) {
		if ( x < width && x >= 0 && y < height && y >= 0) {
			return true;
		} 
		else {
			return false;			
		}
	}
	
	@Override
	public Coordinate getNextShot(char[][] myBoard,
			char[][] myViewOfOpponentBoard) {
		
		if (phase == Phase.DESTROY && destroyPattern.size() == 0) {
			if (searchPattern1.size() != 0) {
				phase = Phase.SEARCH1;
			}
			else {
				phase = Phase.SEARCH2;
			}
		}
		if (phase == Phase.PERIMETER && perimPattern.size() == 0) {
			if (searchPattern1.size() != 0) {
				phase = Phase.SEARCH1;
			}
			else {
				phase = Phase.SEARCH2;
			}
		}
		if (perimPattern.size() != 0 && phase == Phase.SEARCH1 && perimeterHits >= 9) {
				phase = Phase.PERIMETER;
		}
		
		if (phase == Phase.SEARCH1) {
			lastShot = searchPattern1.get(0);
			searchPattern1.remove(0);
			destroyed.add(lastShot);
			
			if (searchPattern1.size() == 0) {
				phase = Phase.SEARCH2;
			}
			
			return lastShot; 
			
		}
		else if (phase == Phase.SEARCH2) {
			lastShot = searchPattern2.get(0);
			searchPattern2.remove(0);
			destroyed.add(lastShot);
			
			return lastShot;
		}
		else if (phase == Phase.DESTROY) {
			lastShot = destroyPattern.get(0);
			destroyPattern.remove(0);
			destroyed.add(lastShot);
			
			if (searchPattern1.contains(lastShot)) {
				searchPattern1.remove(lastShot);
			}
			if (searchPattern2.contains(lastShot)) {
				searchPattern2.remove(lastShot);
			}
			
			
			if (destroyPattern.size() == 0) {
				if (searchPattern1.size() != 0) {
					if (perimeterHits >= 9) {
						phase = Phase.PERIMETER;
						for (Coordinate hit : destroyed) {
							if (perimPattern.contains(hit)) {
								perimPattern.remove(hit);
							}
						}
						
						if (perimPattern.size() == 0) {
							phase = Phase.SEARCH1;
						}
						
					}
					else {
						phase = Phase.SEARCH1;						
					}
				}
				else {
					phase = Phase.SEARCH2;
				}
			}
			
			return lastShot;
		}
		else if (phase == Phase.PERIMETER) {
			lastShot = perimPattern.get(0);
			perimPattern.remove(0);
			destroyed.add(lastShot);
			
			if (searchPattern1.contains(lastShot)) {
				searchPattern1.remove(lastShot);
			}
			if (searchPattern2.contains(lastShot)) {
				searchPattern2.remove(lastShot);
			}
			
			if (perimPattern.size() == 0) {
				if (searchPattern1.size() != 0) {
					phase = Phase.SEARCH1;						
				}
				else {
					phase = Phase.SEARCH2;
				}
			}
			
			return lastShot;
		}
		else {
			
			Coordinate randomCoord = coords[0][0];
			
			while (destroyed.contains(randomCoord)){
				randomCoord = coords[random.nextInt(height)][random.nextInt(width)];
			}
			
			return randomCoord;
		}
	}
	
	public boolean isAdjacent(Coordinate coord1, Coordinate coord2) {
		int[] difference = new int[]{coord1.getX() - coord2.getX(), coord1.getY() - coord2.getY()};
		
		for (int [] d : adjacency) {
			if (Arrays.equals(d, difference)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isNextToOccupiedCell(int x, int y, char[][] myBoard) {
		if (isValidCoordinate(x+1, y) && myBoard[y][x+1] != (char) 0){
			return true;
		}
		if (isValidCoordinate(x-1, y) && myBoard[y][x-1] != (char) 0){
			return true;
		}
		if (isValidCoordinate(x, y+1) && myBoard[y+1][x] != (char) 0){
			return true;
		}
		if (isValidCoordinate(x, y-1) && myBoard[y-1][x] != (char) 0){
			return true;
		}
		
		return false;
	}
	
	public boolean isValidPlacement(Ship ship, ShipPlacement shipPlacement, char[][] myBoard) {
		Coordinate beginning = shipPlacement.getBeginning();
		int[] ending = {beginning.getX(), beginning.getY()};
		int[] direction = new int[2];
		
		switch (shipPlacement.getDirection()) {
		case NORTH:
			ending[1] += ship.getShipLength();
			direction[1] = 1;
			break;
		case SOUTH:
			ending[1] -= ship.getShipLength();
			direction[1] = -1;
			break;
		case EAST:
			ending[0] += ship.getShipLength();
			direction[0] = 1;
			break;
		case WEST:
			ending[0] -= ship.getShipLength();
			direction[0] = -1;
			break;
		}
		
		if (isValidCoordinate(beginning.getX(), beginning.getY()) && isValidCoordinate(ending[0], ending[1])){
			int beginx = beginning.getX();
			int beginy = beginning.getY();
			
			for (int i = 0; i < ship.getShipLength(); i++) {
				int x = beginx + (i * direction[0]);
				int y = beginy + (i * direction[1]);
				
				char cellState = myBoard[y][x]; 
				if (cellState != (char) 0) {
					return false;
				}
				if (isNextToOccupiedCell(x, y, myBoard)) {
					return false;
				}
			}
		}
		else {
			return false;
		}
			
		return true;
	}
	
	public int[] getDirection(Coordinate stationary, Coordinate moving) {
		
		
		int x = moving.getX() - stationary.getX();
		int y = moving.getY() - stationary.getY();
		
		if (x < 0) {
			x /= Math.abs(x);
		}
		if (y < 0) {
			y /= Math.abs(y);			
		}
		
		return new int[]{x, y};
	}
	
	@Override
	public ShipPlacement getShipPlacement(Ship ship, char[][] myBoard) {
		boolean isValid = false;
		ArrayList<Direction> directionList = new ArrayList<Direction>();
		directionList.add(Direction.NORTH);
		directionList.add(Direction.SOUTH);
		directionList.add(Direction.EAST);
		directionList.add(Direction.WEST);
		
		int x;
		int y;
		
		ShipPlacement shipPlacement = new ShipPlacement(new Coordinate(0, 0), directionList.get(1)); 
		
		while (!isValid) {
			
			x = random.nextInt(width);
			y = random.nextInt(height);
			shipPlacement = new ShipPlacement(new Coordinate(x, y), directionList.get(random.nextInt(4)));
			
			isValid = isValidPlacement(ship, shipPlacement, myBoard);
		}
		
		return shipPlacement;
	}
	public Ship getShipWithHandle(char c) {
		for (Ship ship : configuration.getShips()) {
			if (ship.getShipHandle() == c) {
				return ship;
			}
		}
		return null;
	}
	
	@Override
	public void notify(String message) {
		//System.out.println("Player 1: " + message);
		if (message.startsWith("HIT")) {
			phase = Phase.DESTROY;
			nonFatalHits++;
			
			int x = lastShot.getX();
			int y = lastShot.getY();
			
			if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
				perimeterHits++;
			}
			
			if (isValidCoordinate(x+1, y) && !destroyed.contains(coords[y][x+1])){
				destroyPattern.add(coords[y][x+1]);
			}
			if (isValidCoordinate(x-1, y) && !destroyed.contains(coords[y][x-1])){
				destroyPattern.add(coords[y][x-1]);
			}
			if (isValidCoordinate(x, y+1) && !destroyed.contains(coords[y+1][x])){
				destroyPattern.add(coords[y+1][x]);
			}
			if (isValidCoordinate(x, y-1) && !destroyed.contains(coords[y-1][x])){
				destroyPattern.add(coords[y-1][x]);
			}
			
			for (Coordinate hit : destroyed) {
				if (isAdjacent(lastShot, hit)) {
					prefDirections.add(new int[]{x - hit.getX(), y - hit.getY()});
					prefDirections.add(new int[]{-(x - hit.getX()), -(y - hit.getY())});
				}
			}
			
			ArrayList<Coordinate> prefCoordinates = new ArrayList<Coordinate>();
			
			for (int[] direction : prefDirections) {
				for (Coordinate toShoot : destroyPattern) {
					if (Arrays.equals(getDirection(lastShot, toShoot), direction)) {
						prefCoordinates.add(toShoot);
					}
				}
			}
			
			for (Coordinate toShoot : prefCoordinates) {
				destroyPattern.remove(toShoot);
				destroyPattern.add(0, toShoot);			
			}	
		}
		if (message.startsWith("SUNK")) {
			char handle = message.charAt(message.length() - 1);
			nonFatalHits -= getShipWithHandle(handle).getShipLength();
			prefDirections.clear();
			
			if (phase == Phase.DESTROY && nonFatalHits == 0) {
				destroyPattern.clear();
				if (searchPattern1.size() != 0) {
					phase = Phase.SEARCH1;
				}
				else {
					phase = Phase.SEARCH2;
				}
			}
		}
		
	}

	@Override
	public void newGame(String opponent, GameConfiguration config) {
		configuration = config;
		height = config.getGridHeight();
		width = config.getGridWidth();
		nonFatalHits = 0;
		perimeterHits = 0;
		lastShot = new Coordinate(-1, -1);
		
		phase = Phase.SEARCH1;
		
		searchPattern1 = generateSearchPattern(3, 4, width, height);
		
		ArrayList<Coordinate> perimCoords = new ArrayList<Coordinate>();
		for (Coordinate c : searchPattern1) {
			if (c.getX() == 0 || c.getX() == width-1 || c.getY() == 0 || c.getY() == height-1) {
				perimCoords.add(c);
			}
		}
		
		for (Coordinate c : perimCoords) {
			searchPattern1.remove(c);
			searchPattern1.add(0, c);
		}
		
		searchPattern2 = generateSearchPattern(1, 4, width, height);
		
		perimPattern = generatePerimeterPattern(2, width, height);
		
		prefDirections = new ArrayList<int[]>();
		
		destroyPattern = new ArrayList<Coordinate>();
		destroyed = new HashSet<Coordinate>();
		
		
		
	}

}
