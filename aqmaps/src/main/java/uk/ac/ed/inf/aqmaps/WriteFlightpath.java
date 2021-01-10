package uk.ac.ed.inf.aqmaps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class WriteFlightpath extends DroneMovement {
	
	//creates a flightpath-dd-mm-yyyy.txt file, which lists where the drone has been and which sensors it connected to
	public static void writeTxtFile(String day, String month, String year, String lat, String lon, String port) throws IOException, InterruptedException {
		var droneFlight = dronePath(day, month, year, lat, lon, port);
		var dronePositions = droneFlight.get(0);
		var sensorsVisited = droneFlight.get(1);
		var numPositions = dronePositions.size();
		var directions = chosenDirections(dronePositions);
		var locFromPoint = pointToLocation(port, year, month, day);
		var fw = new FileWriter("flightpath-" + day + "-" + month + "-" + year + ".txt");
		var bw = new BufferedWriter(fw);
		var pw = new PrintWriter(bw);
		for(var i=0; i<numPositions-1; i++) {
			var moveNum = i+1;
			var positionBefore = dronePositions.get(i);
			var lonBefore = positionBefore.longitude();
			var latBefore = positionBefore.latitude();
			var positionAfter = dronePositions.get(i+1);
			var lonAfter = positionAfter.longitude();
			var latAfter = positionAfter.latitude();
			var direction = directions.get(i);
			var sensorVisited = sensorsVisited.get(i);
			String sensorLoc = null;
			if(sensorVisited != null) {
				sensorLoc = locFromPoint.get(sensorVisited);
			}
			pw.println(moveNum + "," + lonBefore + "," + latBefore + "," + direction + "," + lonAfter + "," + latAfter + "," + sensorLoc);
			
		}
		pw.flush();
		pw.close();
        bw.close();
        fw.close();
	}

}
