package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.mapbox.geojson.Point;

public class DroneMovement extends DroneConstraints{
	private final static double connectRange = 0.0002; //drone can connect to sensor if it is within this range
	private final static int maxMoves = 150;
	private static int movesMade = 0;
	
	//returns the maximum number of moves the drone can make
	private static int getMaxMoves() {
		return maxMoves;
	}
	
	//increases the number of moves the drone has made by 1
	private static void incrementMoves() {
		movesMade ++;
	}

	
	//returns the number of moves the drone has made
	private static int getMoves() {
		return movesMade;
	}
	
	//returns True if drone is within range to connect to sensor
	private static boolean withinRange(Point dronePos, Point sensorPos) {
		var dist = euclidDist(dronePos, sensorPos);
		return dist<connectRange;
	}
	
	//returns True if current drone position is close to initial drone position
	private static boolean isClosedLoop(Point initPos, Point currPos) {
		var dist = euclidDist(initPos, currPos);
		return dist<r;
	}
	
	//returns the closest sensor to the drone
	private static Point closestSensor(Point dronePos, List<Point> sensors) throws IOException, InterruptedException {
		var numOfCoords = sensors.size();
		var closestSensor = sensors.get(0);
		var smallestDist = euclidDist(dronePos, closestSensor);
		for(var i=1; i<numOfCoords; i++) {
			var sensorPos = sensors.get(i);
			var dist = euclidDist(dronePos, sensorPos);
			if(dist<smallestDist) {
				smallestDist = dist;
				closestSensor = sensorPos;
			}
			
		}
		return closestSensor;
	}
	
	//returns a list of two lists, where the first list contains all of the drone's positions and the second contains all the sensors visited, during its flight
	public static List<List<Point>> dronePath(String day, String month, String year, String latStr, String lonStr, String port) throws IOException, InterruptedException{
		var lat = Double.parseDouble(latStr);
		var lon = Double.parseDouble(lonStr);
		var initPos = Point.fromLngLat(lon, lat);
		var currPos = initPos;
		var dronePositions = new ArrayList<Point>();
		dronePositions.add(currPos);
		var sensorsVisited = new ArrayList<Point>();
		var droneFlight = new ArrayList<List<Point>>();
		var allSensors = getSensorsCoords(port, year, month, day);
		var noFlyCoordPairs = noFlyZoneCoordPairs(port);
		while(getMaxMoves() != getMoves() && allSensors.isEmpty()==false)  { 
			var closestSensor = closestSensor(currPos, allSensors);	
			var desDir = computeDir(currPos, closestSensor);
			var desNextPos = nextPos(desDir, currPos);
			currPos = avoidIllegalMove(currPos, desNextPos, noFlyCoordPairs, dronePositions, port);
			incrementMoves();
			dronePositions.add(currPos);
			closestSensor = closestSensor(currPos, allSensors);
			if(withinRange(currPos, closestSensor)==true) {
				sensorsVisited.add(closestSensor);
				allSensors.remove(closestSensor);
			}
			else {
				sensorsVisited.add(null);
			}
		}
		//once all sensors have been visited, and moves haven't reached 150, the drone flies towards initial position
		while(getMaxMoves() != getMoves() && isClosedLoop(initPos, currPos)==false) { 
			var desDir = computeDir(currPos, initPos);
			var desNextPos = nextPos(desDir, currPos);
			currPos = avoidIllegalMove(currPos, desNextPos, noFlyCoordPairs, dronePositions, port);
			dronePositions.add(currPos);	
			sensorsVisited.add(null);
		}
		droneFlight.add(dronePositions);
		droneFlight.add(sensorsVisited);
		return droneFlight;
	}
	
	//returns a list of directions the drone chose to move in during its flight path, given the drone's positions
	public static List<Integer> chosenDirections(List<Point> dronePositions) throws IOException, InterruptedException{
		var numPositions = dronePositions.size();
		var dirList = new ArrayList<Integer>();
		for(var i=0; i<numPositions-1; i++) {
			var dir = computeDir(dronePositions.get(i), dronePositions.get(i+1));
			dirList.add(dir);
		}
		return dirList;
	}
	
	//returns a list of the sensors not visited by the drone
	public static List<Point> sensorsNotVisited(String port, String day, String month, String year, List<Point> sensorsVisited) throws IOException, InterruptedException{
		var sensors = getSensorsCoords(port, year, month, day);
		var numSensorsVisit = sensorsVisited.size();
		for(var i=0; i<numSensorsVisit; i++) {
			var sensorVisited = sensorsVisited.get(i);
			if(sensorVisited != null) {
				sensors.remove(sensorVisited);
			}	
		}
		return sensors;
	}
	

}
