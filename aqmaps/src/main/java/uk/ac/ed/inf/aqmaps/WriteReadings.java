package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class WriteReadings extends WriteFlightpath{

	//returns a Feature consisting of the Point geometry with properties "location","rgb-string", "marker-color" and "marker-symbol"
	private static Feature createMarkerFeature(Point sensorPos, String location, String colour, String symbol) {
		var feature = Feature.fromGeometry((Geometry) sensorPos);
		feature.addStringProperty("location", location);
		feature.addStringProperty("rbg-string", colour);
		feature.addStringProperty("marker-color", colour);
		feature.addStringProperty("marker-symbol", symbol);
		return feature;
	}
	
	//returns a Feature Collection of the drone's flight
	private static FeatureCollection createFeatureCollection(String day, String month, String year, String lat, String lon, String port) throws IOException, InterruptedException {
		var featureList = new ArrayList<Feature>();
		
		var droneFlight = dronePath(day, month, year, lat, lon, port);
		var dronePositions = droneFlight.get(0);
		var sensorsVisited = droneFlight.get(1);
		var numVisited = sensorsVisited.size();
		
		var lineStr = LineString.fromLngLats(dronePositions);
		var flightpath = Feature.fromGeometry((Geometry) lineStr);
		featureList.add(flightpath);
		
		var locFromPoint = pointToLocation(port, year, month, day);
		var batteryFromPoint = pointToBattery(port, year, month, day);
		var readingFromPoint = pointToReading(port, year, month, day);
		
		//marker properties for the sensors visited
		for(var i=0; i<numVisited; i++) {
			var sensorPos = sensorsVisited.get(i);
			if(sensorPos != null) {
				var location = locFromPoint.get(sensorPos);
				var battery = batteryFromPoint.get(sensorPos);
				var reading = readingFromPoint.get(sensorPos);
				var colour = getColour(reading, battery);
				var symbol = getSymbol(reading, battery);
				
				var feature = createMarkerFeature(sensorPos, location, colour, symbol);
				featureList.add(feature);
			}
		}
		
		//marker properties for the sensors not visited
		var sensorsNotVisit = sensorsNotVisited(port, day, month, year, sensorsVisited);
		if(sensorsNotVisit.isEmpty() == false) {
			var numNotVisit = sensorsNotVisit.size();
			for(var i=0; i<numNotVisit; i++) {
				var sensorPos = sensorsNotVisit.get(i);
				var location = locFromPoint.get(sensorPos);
				var colour = "#aaaaaa";
				String symbol = null;
				var feature = createMarkerFeature(sensorPos, location, colour, symbol);
				featureList.add(feature);
			}
			
		}
		return FeatureCollection.fromFeatures(featureList);
		
	}
	
	//creates a readings-dd-mm-yyyy.geojson file of the drone's flight
	public static void writeGeojsonFile(String day, String month, String year, String lat, String lon, String port) throws IOException, InterruptedException {
		var fw = new FileWriter("readings-" + day + "-" + month + "-" + year +".geojson");
		fw.write(createFeatureCollection(day, month, year, lat, lon, port).toJson());
		fw.close();
	}


}
