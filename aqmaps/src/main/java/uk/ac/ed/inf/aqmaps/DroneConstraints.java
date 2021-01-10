package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;

public class DroneConstraints extends NoFlyZones{
	public final static double r = 0.0003; //distance travelled by drone for each move
	private final static double latUB = 55.946233; //latitude upper bound
	private final static double latLB = 55.942617;
	private final static double lonUB = -3.184319;
	private final static double lonLB = -3.192473; //longitude lower bound

	
	//returns True if drone is within confinement area
	private static boolean withinArea(Point dronePos) {
		var droneLat = dronePos.latitude();
		var droneLon = dronePos.longitude();
		return droneLat<latUB && droneLat>latLB && droneLon<lonUB && droneLon>lonLB;
	}
	
	//returns the centre of the confinement area
	private static Point areaCentre() {
		var centreLon = (lonLB + lonUB)/2;
		var centreLat = (latLB + latUB)/2;
		return Point.fromLngLat(centreLon, centreLat);
	}
	
	//return True if direction (angle) is a multiple of 10 i.e. the modulo 10 of it is 0
	private static boolean multipleOfTen(double direction) {
		return (direction % 10)==0; 
	}
	
	//returns True if direction is within the correct range 
	private static boolean withinDirRange(double direction) {
		return direction <=350 && direction >=0;
	}
	
	//returns True if inputed direction is valid
	private static boolean validDir(double direction) {
		return multipleOfTen(direction) && withinDirRange(direction);
	}
	
	//returns the new position of the drone, given the current position of the drone and its direction of travel 
	public static Point nextPos(int dirInDeg, Point currPos) {
		var dirInRad = Math.toRadians(dirInDeg); //have to convert to radians such that it is compatible with Math methods
		
		var currLat = currPos.latitude();
		var currLon = currPos.longitude();
		
		var newLat = currLat + r*Math.sin(dirInRad);
		var newLon = currLon + r*Math.cos(dirInRad);
		var newPos = Point.fromLngLat(newLon, newLat);
		
		return newPos; 
	}
	
	//returns a direction for the drone to move in, given the position of the drone and the desired position it wants to move towards 
	//the desired position of the drone is either in range of: the closest sensor or its initial position (such that we have a closed loop path)
	public static int computeDir(Point dronePos, Point desPos) {
		var droneLat = dronePos.latitude();
		var droneLon = dronePos.longitude();
		var desLat = desPos.latitude();
		var desLon = desPos.longitude();
		var angleInRad = Math.atan2(droneLat-desLat, droneLon-desLon) + Math.PI;
		var angle = Math.toDegrees(angleInRad);
		if(multipleOfTen(angle)==false && withinDirRange(angle)== true) {
			angle= angle - (angle%10); //equivalent to rounding down the angle to the nearest 10
		}
		else if (multipleOfTen(angle)==true && withinDirRange(angle) == false) {
			if(Math.abs(angle)>350) {
				angle = (angle%360); //converts angle to be between -360 and 360
			}
			if(angle<0) {
				angle = 360 + angle; //converts negative angle into its equivalent positive angle
			}
		}
		else if(validDir(angle) == false) {
			if(Math.abs(angle)>350) {
				angle = (angle%360); 
			}
			if(angle<0) {
				angle = 360 + angle; 
			}
			angle = angle - (angle%10);

		}
		return (int) angle;
	}
	
	//returns the two possible next positions the drone can have, given that it can fly from coord1 to coord2 or coord2 to coord1 (to avoid intersection)
	//coord1 and coord2 are the points defining the side of the building the drone's path intersects with
	private static List<Point> possibleNextPos(Point currPos, Point coord1, Point coord2, double buildingSideGrad, Point buildingCentre){
		var coord1Lat = coord1.latitude();
		var coord1Lon = coord1.longitude();
		var coord2Lat = coord2.latitude();
		var coord2Lon = coord2.longitude();
		
		var currPosLon = currPos.longitude();
		var currPosLat = currPos.latitude();
		
		var buildingLon = buildingCentre.longitude();
		var buildingLat = buildingCentre.latitude();
		
		var dir1 = computeDir(coord1, coord2); //in the case that the drone is moving in the direction coord1 to coord2
		var nextPosTemp1 = nextPos(dir1, currPos); //the temporary next position if the drone moves in dir1
		
		var dir2 = computeDir(coord2, coord1); //in the case that the drone is moving in the direction coord2 to coord1
		var nextPosTemp2 = nextPos(dir2, currPos); //the temporary next position if the drone moves in dir2 
		
		var possibleNextPos = new ArrayList<Point>();
		
		if(Math.abs(buildingSideGrad)>=1) { //in this case, longitudes of building centre and drone current position are compared
			//coord1 to coord2 scenario
			if((coord1Lat>coord2Lat && buildingLon<currPosLon) || (coord1Lat<coord2Lat && buildingLon>currPosLon)) {
				dir1 = (dir1+10)%360; //angle increased such that drone doesn't fly over side of building
				nextPosTemp1 = nextPos(dir1, currPos);
			}
			//coord2 to coord1 scenario
			if((coord1Lat<coord2Lat && buildingLon<currPosLon) || (coord1Lat>coord2Lat && buildingLon>currPosLon)) {
				dir2 = (dir2+10)%360;
				nextPosTemp2 = nextPos(dir2, currPos);
			}			
		}
		else { //in this case, latitudes of building centre and drone current position are compared
			if((coord1Lon>coord2Lon && buildingLat>currPosLat) || (coord1Lon<coord2Lon && buildingLat<currPosLat)) {
				dir1 = (dir1+10)%360; 
				nextPosTemp1 = nextPos(dir1, currPos);
			}

			if((coord1Lon<coord2Lon && buildingLat>currPosLat) || (coord1Lon>coord2Lon && buildingLat<currPosLat)) {
				dir2 = (dir2+10)%360;
				nextPosTemp2 = nextPos(dir2, currPos);
			}
		}
		
		possibleNextPos.add(nextPosTemp1);
		possibleNextPos.add(nextPosTemp2);
		
		return possibleNextPos;
	}
	
	
	//calculates the Euclidean distance between two points
	public static double euclidDist(Point p1, Point p2) {
		var p1Lat = p1.latitude();
		var p1Lon = p1.longitude();
		var p2Lat = p2.latitude();
		var p2Lon = p2.longitude();
		var dist = Math.sqrt( ((p1Lat-p2Lat)*(p1Lat-p2Lat)) + ((p1Lon-p2Lon)*(p1Lon-p2Lon)) );
		return dist;
	}
	
	//returns the next position of the drone, such that it doesn't fly over one side of a building (defined by coord1 and coord2)
	private static Point chooseNextPos(Point currPos, Point droneNextDes, Point coord1, Point coord2, double buildingSideGrad, Point buildingCentre,  List<List<List<Point>>> noFlyZonesCoords, Point previous1, Point previous2) throws IOException, InterruptedException {
		var possibleNextPos = possibleNextPos(currPos, coord1, coord2, buildingSideGrad, buildingCentre);
		var nextPosTemp1 = possibleNextPos.get(0);
		var nextPosTemp2 = possibleNextPos.get(1);
		
		var euclidDist1 = euclidDist(droneNextDes, nextPosTemp1); 
		var euclidDist2 = euclidDist(droneNextDes, nextPosTemp2);
		
		Point nextPos = droneNextDes;
		

		//in the case that both directions result in no intersection, 
		//it chooses the one that minimises the distance between the drone's next position and desired position 
		//and doesn't result in the drone going back and fourth continuously 
		if(noIntersections(currPos, nextPosTemp1, noFlyZonesCoords)==true && noIntersections(currPos, nextPosTemp2, noFlyZonesCoords)==true) {
			var repeatedMoveDir1 = false; //in the case that previous1 and previous2 are null
			var repeatedMoveDir2 = false;
			if(previous1!=null && previous2!=null) {
				repeatedMoveDir1 = computeDir(previous2, previous1)==computeDir(currPos, nextPosTemp1); //next move is equal to 2 moves before, indicating repeated move
				repeatedMoveDir2 = computeDir(previous2, previous1)==computeDir(currPos, nextPosTemp2);
			}
			
			if(euclidDist1<euclidDist2 && repeatedMoveDir1==false || repeatedMoveDir2==true) {
				nextPos = nextPosTemp1;
			}
			if(repeatedMoveDir1==true || euclidDist2<euclidDist1 && repeatedMoveDir2==false){
				nextPos = nextPosTemp2;
			}
		}
		else if(noIntersections(currPos, nextPosTemp1, noFlyZonesCoords)==true && noIntersections(currPos, nextPosTemp2, noFlyZonesCoords)==false) {
			nextPos = nextPosTemp1;
		}
		else if(noIntersections(currPos, nextPosTemp1, noFlyZonesCoords)==false && noIntersections(currPos, nextPosTemp2, noFlyZonesCoords)==true) { //
			nextPos =  nextPosTemp2;
		}
		

		return nextPos;
	}
	
	//returns new position of the drone such that it avoids the no fly zones
	private static Point avoidNoFlyZones(Point droneCurr, Point droneNextDes, List<List<List<Point>>> noFlyZonesCoords, List<Point> dronePositions, String port) throws IOException, InterruptedException {
		var intersectingLines = linesWhichIntersect(droneCurr, droneNextDes, noFlyZonesCoords);
		var numCoords = intersectingLines.size();
		
		var droneGrad = getGradient(droneCurr, droneNextDes);
		var droneYint = getYint(droneCurr, droneNextDes);
		var droneNext = droneNextDes; //in the case that there are no intersections with any lines
		
		Point previous1 = null;
		Point previous2 = null;
		var lastIndex = dronePositions.size()-1;
		if(lastIndex>1) { //checks that drone has been in at least 3 positions
			previous1 = dronePositions.get(lastIndex-1); //previous position of drone
			previous2 = dronePositions.get(lastIndex-2); //position before previous position
		}
		
		//in the case that the drone intersects with one line
		if(numCoords == 2) {
			var coord1 = intersectingLines.get(0);
			var coord2 = intersectingLines.get(1); 
			var buildingInd = buildingIndex(coord1, port);
			if(buildingInd != 100) { //index initialised as 100 in buildingIndex function- if coordinate not found in list, 100 is returned
				var buildingCentre = buildingCentres(port).get(buildingInd);
				var buildingSideGrad = getGradient(coord1, coord2);
				droneNext = chooseNextPos(droneCurr, droneNextDes, coord1, coord2, buildingSideGrad, buildingCentre, noFlyZonesCoords, previous1, previous2);
			}
			
		}
		//in the case that the drone intersects with two lines
		else if(numCoords == 4) {
			var coord1 = intersectingLines.get(0);
			var coord2 = intersectingLines.get(1);
			var coord3 = intersectingLines.get(2);
			var coord4 = intersectingLines.get(3);
			
			var buildingSide1Grad = getGradient(coord1, coord2);
			var buildingSide1Yint = getYint(coord1, coord2);
			var intersection1 = getIntersection(droneYint, buildingSide1Yint, droneGrad, buildingSide1Grad);
			var dist1 = euclidDist(intersection1, droneCurr);
			
			var buildingSide2Grad = getGradient(coord3, coord4);
			var buildingSide2Yint = getYint(coord3, coord4);
			var intersection2 = getIntersection(droneYint, buildingSide2Yint, droneGrad, buildingSide2Grad);
			var dist2 = euclidDist(intersection2, droneCurr);
			
			var buildingInd1 = buildingIndex(coord1, port); //in case the two lines which the drone intersects with are from 2 different buildings
			var buildingInd2 = buildingIndex(coord3, port);
			if(buildingInd1 !=100 && buildingInd2 !=100) { 
				var buildingCentre1 = buildingCentres(port).get(buildingInd1);
				var buildingCentre2 = buildingCentres(port).get(buildingInd2);
				
				if(dist1<dist2) { //to determine which side the drone path intersects with first
					droneNext = chooseNextPos(droneCurr, droneNextDes, coord1, coord2, buildingSide1Grad, buildingCentre1, noFlyZonesCoords, previous1, previous2);
				}
				else {
					droneNext = chooseNextPos(droneCurr, droneNextDes, coord3, coord4, buildingSide2Grad, buildingCentre2, noFlyZonesCoords, previous1, previous2);
				}
			}
				
		} 
		//in the case that the drone intersects with three lines
		else if(numCoords == 6) {
			var coord1 = intersectingLines.get(0);
			var coord2 = intersectingLines.get(1);
			var coord3 = intersectingLines.get(2);
			var coord4 = intersectingLines.get(3);
			var coord5 = intersectingLines.get(4);
			var coord6 = intersectingLines.get(5);
			
			var buildingSide1Grad = getGradient(coord1, coord2);
			var buildingSide1Yint = getYint(coord1, coord2);
			var intersection1 = getIntersection(droneYint, buildingSide1Yint, droneGrad, buildingSide1Grad);
			var dist1 = euclidDist(intersection1, droneCurr);
			
			var buildingSide2Grad = getGradient(coord3, coord4);
			var buildingSide2Yint = getYint(coord3, coord4);
			var intersection2 = getIntersection(droneYint, buildingSide2Yint, droneGrad, buildingSide2Grad);
			var dist2 = euclidDist(intersection2, droneCurr);
			
			var buildingSide3Grad = getGradient(coord5, coord6);
			var buildingSide3Yint = getYint(coord5, coord6);
			var intersection3 = getIntersection(droneYint, buildingSide3Yint, droneGrad, buildingSide3Grad);
			var dist3 = euclidDist(intersection3, droneCurr);
			
			var minDist = Math.min(Math.min(dist1, dist2),dist3);
			
			var buildingInd1 = buildingIndex(coord1, port); //in case the three lines which the drone intersects with are from 3 different buildings
			var buildingInd2 = buildingIndex(coord3, port);
			var buildingInd3 = buildingIndex(coord5, port);
			if(buildingInd1 !=100 && buildingInd2 != 100 && buildingInd3 != 100) {
				var buildingCentre1 = buildingCentres(port).get(buildingInd1);
				var buildingCentre2 = buildingCentres(port).get(buildingInd2);
				var buildingCentre3 = buildingCentres(port).get(buildingInd3);
								
				if(minDist==dist1) {
					droneNext = chooseNextPos(droneCurr, droneNextDes, coord1, coord2, buildingSide1Grad, buildingCentre1, noFlyZonesCoords, previous1, previous2);
				}
				else if(minDist==dist2) {
					droneNext = chooseNextPos(droneCurr, droneNextDes, coord3, coord4, buildingSide2Grad, buildingCentre2, noFlyZonesCoords, previous1, previous2);
				}
				else {
					droneNext = chooseNextPos(droneCurr, droneNextDes, coord5, coord6, buildingSide3Grad, buildingCentre3, noFlyZonesCoords, previous1, previous2);
				}
			}
		}
		
		
		return droneNext;
	}
	
	
	//returns the new position of the drone, in the case that the desired next position (which avoids the no fly zones) results in the drone leaving the confined area 
	private static Point avoidLeavingArea(Point droneCurr, Point droneNextDes, List<List<List<Point>>> noFlyZonesCoords, List<Point> dronePositions, String port) throws IOException, InterruptedException {
		var droneNext = droneNextDes;
		
		//the drone moves towards the centre of the confined area, while avoiding the no fly zones
		if(withinArea(droneNextDes)==false) {
			var dir = computeDir(droneCurr, areaCentre());
			droneNextDes = nextPos(dir, droneCurr);
			droneNext = avoidNoFlyZones(droneCurr, droneNextDes, noFlyZonesCoords, dronePositions, port);
		}
		return droneNext;
	}
	
	//returns the new position of the drone, which is legal
	public static Point avoidIllegalMove(Point droneCurr, Point droneNextDes, List<List<List<Point>>> noFlyZonesCoords, List<Point> dronePositions, String port) throws IOException, InterruptedException {
		var droneAvoidNoFly = avoidNoFlyZones(droneCurr, droneNextDes, noFlyZonesCoords, dronePositions, port);
		var droneAvoidLeaving = avoidLeavingArea(droneCurr, droneAvoidNoFly, noFlyZonesCoords, dronePositions, port);
		var legalNextMove = droneAvoidLeaving;
		return legalNextMove;
	}
}
