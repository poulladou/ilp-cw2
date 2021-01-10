package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class NoFlyZones extends MarkerProperties{
	
	
	//returns the gradient (m) of a line, given two Points that lie on it
	public static double getGradient(Point p1, Point p2) {
		var p1Lat = p1.latitude();
		var p1Lon = p1.longitude();
		
		var p2Lat = p2.latitude();
		var p2Lon = p2.longitude();
		
		var m = (p1Lat-p2Lat)/(p1Lon-p2Lon);
		return m;
	}
	
	
	//returns the y-intercept (c) of a line, given two Points that lie on it
	public static double getYint(Point p1, Point p2) {
		var m = getGradient(p1,p2);
		
		var p1Lat = p1.latitude();
		var p1Lon = p1.longitude();
		
		var c = p1Lat-(m*p1Lon);
		return c;
	}
	
	//returns the x coordinate of where the drone path intersects with a side of a building
	private static double getIntersectionX(double droneYint, double buildingSideYint, double droneGrad, double buildingSideGrad) {
		var x = (droneYint - buildingSideYint) / (buildingSideGrad - droneGrad);
		return x;
	}
	
	//returns the y coordinate of where the drone path intersects with a side of a building
	private static double getIntersectionY(double droneYint, double buildingSideYint, double droneGrad, double buildingSideGrad) {
		var x = getIntersectionX(droneYint, buildingSideYint, droneGrad, buildingSideGrad);
		var y = droneGrad*x + droneYint;
		return y;
	}
	
	//returns the point where the drone path intersects with a side of a building
	public static Point getIntersection(double droneYint, double buildingSideYint, double droneGrad, double buildingSideGrad) {
		var x = getIntersectionX(droneYint, buildingSideYint, droneGrad, buildingSideGrad);
		var y = getIntersectionY(droneYint, buildingSideYint, droneGrad, buildingSideGrad);
		return Point.fromLngLat(x, y);
	}
	
	//returns True if the line joining the drone's current and next position does not intersect 
	//with the line joining two coordinates (coord1 and coord2) defining a side of a building in the no fly zone
	private static boolean noIntersection(Point droneCurr, Point droneNext, Point coord1, Point coord2) {		
		var droneGrad = getGradient(droneCurr,droneNext);
		var droneYint = getYint(droneCurr, droneNext);
		
		var buildingSideGrad = getGradient(coord1, coord2);
		var buildingSideYint = getYint(coord1, coord2);
		
		var coord1Lon = coord1.longitude();
		var coord2Lon = coord2.longitude();
		
		var xUB = Math.max(coord1Lon, coord2Lon); //largest x coordinate of the two (upper bound)
		var xLB = Math.min(coord1Lon, coord2Lon); 
		
		var droneCurrLon = droneCurr.longitude();
		var droneNextLon = droneNext.longitude();
		
		var droneLonUB = Math.max(droneCurrLon, droneNextLon);
		var droneLonLB = Math.min(droneCurrLon, droneNextLon); //smallest longitude value of the drone's two positions (lower bound)
		
		var noIntersect = true; //initialised as true as the lines only intersect in the scenarios defined below
		
		if(droneGrad==buildingSideGrad && droneYint!=buildingSideYint) {
			noIntersect = true; //lines don't intersect because they're parallel 
		}
		
		else if(droneGrad==buildingSideGrad && droneYint==buildingSideYint) {
			if(((droneLonLB<xLB && droneLonUB<=xUB) || (droneLonLB>=xLB && droneLonUB>xUB) || (droneLonLB<xLB && droneLonUB>xUB))) {
				noIntersect = false; //lines fall onto each other, therefore intersect
			}
			else {
				noIntersect = true; 
			}	
		}
		
		else {
			var x = getIntersectionX(droneYint, buildingSideYint, droneGrad, buildingSideGrad); //x coordinate where the two lines intersect when extended 
			
			if(x<=xUB && x>=xLB && x<=droneLonUB && x>=droneLonLB) { 
				noIntersect = false; //lines intersect because the x coordinate falls within the range of x values the specified side of the building and drone path have 
			}
			else {
				noIntersect = true;
			}
		}
		return noIntersect;	
	}
	
	
	
	//returns a list of lists of Points, where each list contains the Points defining one no-fly zone (one building) 
	private static List<List<Point>> noFlyZoneCoords(String port) throws IOException, InterruptedException{
		var jsonStr = webServerContent("/buildings/no-fly-zones.geojson", port);
		var featureCollection = FeatureCollection.fromJson(jsonStr);
		var features = featureCollection.features();
		var numOfFeatures = features.size();
		var allCoords = new ArrayList<List<Point>>();
		for(var i=0; i<numOfFeatures; i++) {
			var feature = features.get(i);
			var polygon = (Polygon) feature.geometry();
			var polygonCoords = polygon.coordinates().get(0);
			var numOfPolyCoords = polygonCoords.size();
			var listOfPolyCoords = new ArrayList<Point>();
			for(var j=0; j<numOfPolyCoords; j++) {
				var polyCoord = polygonCoords.get(j);
				listOfPolyCoords.add(polyCoord);
			}
			allCoords.add(listOfPolyCoords);
		}

		return allCoords;
	}
	
		
	//returns a list of the Points defining each no-fly zone, in pairs, each pair representing the two coordinates defining one side of that building
	public static List<List<List<Point>>> noFlyZoneCoordPairs(String port) throws IOException, InterruptedException{
		var allCoords = noFlyZoneCoords(port);
		var numNoFlyZones = allCoords.size();
		var coordPairs = new ArrayList<List<List<Point>>>();
		for(var i=0; i<numNoFlyZones; i++) {
			var pairsOfCoords = new ArrayList<List<Point>>();
			var polyCoords = allCoords.get(i);
			var numPolyCoords = polyCoords.size();
			for(var j=0; j<numPolyCoords-1; j++) {
				var polyCoordPair = new ArrayList<Point>();
				var polyCoord1 = polyCoords.get(j);
				var polyCoord2 = polyCoords.get(j+1);
				polyCoordPair.add(polyCoord1);
				polyCoordPair.add(polyCoord2);
				pairsOfCoords.add(polyCoordPair);
			}
			coordPairs.add(pairsOfCoords);
		}
		return coordPairs;
	}
	
	//if the line joining the drone's current and next position intersects
	//with any line joining two coordinates defining a side of a building in the no fly zone, these coordinates are returned
	public static List<Point> linesWhichIntersect(Point droneCurr, Point droneNext, List<List<List<Point>>> noFlyZonesCoords) throws IOException, InterruptedException {
		var numNoFlyZones = noFlyZonesCoords.size();
		var noLinesIntersect = true;
		var intersectingLines = new ArrayList<Point>();
		for(var i=0; i<numNoFlyZones;i++) {
			var noFlyZonePairs = noFlyZonesCoords.get(i);
			var numCoordPairs = noFlyZonePairs.size();
			for(var j=0; j<numCoordPairs; j++) {
				var coord1 = noFlyZonePairs.get(j).get(0);
				var coord2 = noFlyZonePairs.get(j).get(1);
				noLinesIntersect = noLinesIntersect && noIntersection(droneCurr, droneNext, coord1, coord2);
				if(noLinesIntersect == false) {
					intersectingLines.add(coord1);
					intersectingLines.add(coord2);
					noLinesIntersect = true;
				}
			}	
		}
		return intersectingLines;
	}
	
	//returns True if the line joining the drone's current and next position does not intersect 
	//with any line joining two coordinates defining a side of a building in the no fly zone
	public static boolean noIntersections(Point droneCurr, Point droneNext, List<List<List<Point>>> noFlyZonesCoords) throws IOException, InterruptedException {
		var numNoFlyZones = noFlyZonesCoords.size();
		var noLinesIntersect = true;
		OUTER_LOOP:
			for(var i=0; i<numNoFlyZones;i++) {
				var noFlyZonePairs = noFlyZonesCoords.get(i);
				var numCoordPairs = noFlyZonePairs.size();
				for(var j=0; j<numCoordPairs; j++) {
					var coord1 = noFlyZonePairs.get(j).get(0);
					var coord2 = noFlyZonePairs.get(j).get(1);
					noLinesIntersect = noLinesIntersect && noIntersection(droneCurr, droneNext, coord1, coord2);
					if(noLinesIntersect == false) {
						break OUTER_LOOP;
					}
				}	
			}
		return noLinesIntersect;
	}
	
	//returns the index of the building in the no-fly zones list which contains the inputed coordinates
	public static int buildingIndex(Point coord1, String port) throws IOException, InterruptedException {
		var allCoords = noFlyZoneCoords(port);
		var numZones = allCoords.size();
		var zone = 100;
		OUTER_LOOP:
			for(var i=0; i<numZones; i++) {
				var coordsInZone = allCoords.get(i);
				var numCoords = coordsInZone.size();
				for(var j=0; j<numCoords; j++) {
					if(coord1.equals(coordsInZone.get(j))) {
						zone = i;
						break OUTER_LOOP;
					}
				}
			}
		return zone;

	}
	
	//returns the sum of two points
	private static Point sumPoints(Point point1, Point point2) {
	var point1Lon = point1.longitude();
	var point1Lat = point1.latitude();
	var point2Lon = point2.longitude();
	var point2Lat = point2.latitude();
	return Point.fromLngLat(point1Lon+point2Lon, point1Lat+point2Lat);
	}
	
	//given the summed points and number of coordinates which were summed, returns the average point
	private static Point averagePoint(Point pointsSummed, int numCoordsInZone) {
		var averageLat = pointsSummed.latitude()/numCoordsInZone;
		var averageLon = pointsSummed.longitude()/numCoordsInZone;
		return Point.fromLngLat(averageLon, averageLat);
	}
	
	//returns the centre of each building, which is the average of all points defining it 
	public static List<Point> buildingCentres(String port) throws IOException, InterruptedException{
		var allCoords = noFlyZoneCoords(port);
		var numZones = allCoords.size();
		var centres = new ArrayList<Point>();
		for(var i=0; i<numZones; i++) {
			var coordsInZone = allCoords.get(i);
			var numCoordsInZone = coordsInZone.size();
			var zoneCentre = coordsInZone.get(0);
			for(var j=1; j<numCoordsInZone-1; j++) { //don't account for last coordinate in each no fly zone as it is a duplicate of the first coordinate 
				zoneCentre = sumPoints(zoneCentre,coordsInZone.get(j));
			}
			zoneCentre = averagePoint(zoneCentre,numCoordsInZone-1);
			centres.add(zoneCentre);
		}
		return centres;
	}

 

}
