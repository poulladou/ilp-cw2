package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.mapbox.geojson.Point;

public class SensorLocation extends SensorDetails{
	private Coordinates coordinates;
	
	public static class Coordinates{
		double lng;
		double lat;
	}
	
	
	//returns the coordinates of a sensor, given the three words that define its location
	public static Coordinates getCoords(String port, String firstWord, String secondWord, String thirdWord) throws IOException, InterruptedException{
		var jsonDetailsString = webServerContent("/words/" + firstWord + "/" + secondWord + "/" + thirdWord + "/details.json" , port);
		var details = new Gson().fromJson(jsonDetailsString, SensorLocation.class);
		return details.coordinates;
		
	}
	
	//returns the coordinates of a sensor as a Point
	private static Point coordsAsPoint(String port, String firstWord, String secondWord, String thirdWord) throws IOException, InterruptedException {
		var sensorCoords =getCoords(port, firstWord, secondWord, thirdWord);
		var sensorLat = sensorCoords.lat;
		var sensorLon = sensorCoords.lng;
		var sensorPoint = Point.fromLngLat(sensorLon, sensorLat);
		return sensorPoint;
		
	}
	
	//returns a list of the coordinates of all the sensors to be visited on the date specified by day, month and year
	public static List<Point> getSensorsCoords(String port, String year, String month, String day) throws IOException, InterruptedException{
		var wordsList = get3WordsList(port, year, month, day);
		var numOfSensors = wordsList.size();
		var locList = new ArrayList<Point>();
		for(var i=0; i<numOfSensors; i++) {
			var threeWords = wordsList.get(i);
			var firstWord = threeWords.get(0);
			var secondWord = threeWords.get(1);
			var thirdWord = threeWords.get(2);
			var sensorLoc = coordsAsPoint(port, firstWord, secondWord, thirdWord);
			locList.add(sensorLoc);
		}
		return locList;
	}
	
	//returns the mapping of the sensor positions to their string location
	public static HashMap<Point,String> pointToLocation(String port, String year, String month, String day) throws IOException, InterruptedException {
		var sensorsCoords = getSensorsCoords(port, year, month, day);
		var detailsList = getSensors(port, year, month, day);
		var numOfSensors = sensorsCoords.size();
		var locations = new HashMap<Point,String>();
		for(var i=0; i<numOfSensors; i++) {
			var sensorLoc = location(detailsList.get(i));
			var sensorCoords = sensorsCoords.get(i);
			locations.put(sensorCoords, sensorLoc);
		}
		return locations;
	}
	
	
	//returns the mapping of the sensor positions to their battery values
	public static HashMap<Point, Double> pointToBattery(String port, String year, String month, String day) throws IOException, InterruptedException{
		var locList = getSensorsCoords(port, year, month, day);
		var batteryList = getBatteryList(port, year, month, day);
		var numOfSensors = locList.size();
		var batteryValues = new HashMap<Point,Double>();
		for(var i=0; i<numOfSensors; i++) {
			batteryValues.put(locList.get(i), batteryList.get(i));
		}
		return batteryValues;
	}
	
	
	//returns the mapping of the sensor positions to their readings
	public static HashMap<Point, String> pointToReading(String port, String year, String month, String day) throws IOException, InterruptedException{
		var locList = getSensorsCoords(port, year, month, day);
		var readingList = getReadingList(port, year, month, day);
		var numOfSensors = locList.size();
		var readings = new HashMap<Point,String>();
		for(var i=0; i<numOfSensors; i++) {
			readings.put(locList.get(i), readingList.get(i));
		}
		return readings;
	}
	
	
	

}
