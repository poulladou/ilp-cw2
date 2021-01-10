package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SensorDetails extends ReadWebServer{
	private String location;
	private double battery;
	private String reading;
	
	
	//returns a list of the sensors to be visited on the date specified by day, month and year
	public static List<SensorDetails> getSensors(String port, String year, String month, String day) throws IOException, InterruptedException{
		var jsonListString = webServerContent("/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json" , port);
		Type listType = new TypeToken<ArrayList<SensorDetails>>(){}.getType();
		ArrayList<SensorDetails> detailsList = new Gson().fromJson(jsonListString, listType);
		return detailsList;
		
	}
	
	//returns the location, given the SensorDetails defining that sensor
	public static String location(SensorDetails details) {
		return details.location;
	}
	
		
	//returns a list of the 3 words defining the location of a sensor, given the SensorDetails defining that sensor
	private static List<String> getSensorWords(SensorDetails details){
		var sensorLoc = details.location;
		var fullstopIndex1 = sensorLoc.indexOf(".");
		var fullstopIndex2 = sensorLoc.lastIndexOf(".");
		var strLength = sensorLoc.length();
		var firstWord = sensorLoc.substring(0, fullstopIndex1);
		var secondWord = sensorLoc.substring(fullstopIndex1 + 1, fullstopIndex2);
		var thirdWord = sensorLoc.substring(fullstopIndex2 + 1, strLength);
		var wordsList = List.of(firstWord, secondWord, thirdWord);
		return wordsList;
	}
	
	//returns a list of lists, where each list contains the 3 words defining the location of a sensor
	public static List<List<String>> get3WordsList(String port, String year, String month, String day) throws IOException, InterruptedException{
		var detailsList = getSensors(port, year, month, day);
		var numOfSensors = detailsList.size();
		var locationList = new ArrayList<List<String>>();
		for(var i=0; i<numOfSensors; i++) {
			var details = detailsList.get(i);
			var wordsList = getSensorWords(details);
			locationList.add(wordsList);
		}
		return locationList;
		
	}
	
	//returns a list of the battery values
	public static List<Double> getBatteryList(String port, String year, String month, String day) throws IOException, InterruptedException{
		var detailsList = getSensors(port, year, month, day);
		var numOfSensors = detailsList.size();
		var batteryList = new ArrayList<Double>();
		for(var i=0; i<numOfSensors; i++) {
			var details = detailsList.get(i);
			var battery = details.battery;
			batteryList.add(battery);
		}
		return batteryList;
	}
	
	//returns a list of the readings
	public static List<String> getReadingList(String port, String year, String month, String day) throws IOException, InterruptedException{
		var detailsList = getSensors(port, year, month, day);
		var numOfSensors = detailsList.size();
		var readingList = new ArrayList<String>();
		for(var i=0; i<numOfSensors; i++) {
			var details = detailsList.get(i);
			var reading = details.reading;
			readingList.add(reading);
		}
		return readingList;
	}


}
