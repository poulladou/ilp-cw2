package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

public class App extends WriteReadings{
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
    	var day = args[0];
    	var month = args[1];
    	var year = args[2];
    	var lat = args[3];
    	var lon = args[4];
    	var seed = args[5];
    	var port = args[6];
    	
    	
    	writeTxtFile(day, month, year, lat, lon, port);
    	writeGeojsonFile(day, month, year, lat, lon, port);
    	

    }
}
