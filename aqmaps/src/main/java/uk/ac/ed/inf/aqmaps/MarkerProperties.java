package uk.ac.ed.inf.aqmaps;

public class MarkerProperties extends SensorLocation{
	//returns the RGB string corresponding to inputed sensor reading and battery
	public static String getColour(String sensorReading, double sensorBattery) {
		var colour = "#aaaaaa"; //default return value is RGB string corresponding to 'not visited' sensor to show incorrect data
		if(sensorBattery<10) {
			colour = "#000000";
		}
		else {
			if(sensorReading != null || sensorReading != "NaN") {
				var reading = Double.parseDouble(sensorReading);
			
				if(0<=reading && reading<32 ) {
					colour= "#00ff00";
				}
				else if(32<=reading && reading<64 ) {
					colour= "#40ff00";
				}
				else if(64<=reading && reading<96 ) {
					colour= "#80ff00";
				}
				else if(96<=reading && reading<128 ) {
					colour= "#c0ff00";
				}
				else if(128<=reading && reading<160 ) {
					colour= "#ffc000";
				}
				else if(160<=reading && reading<192 ) {
					colour= "#ff8000";
				}		
				else if(192<=reading && reading<224 ) {
					colour= "#ff4000";
				}
				else if(224<=reading && reading<256 ) {
					colour= "#ff0000";
				}
			}
		}	
		return colour;
	}
	
	//returns the marker symbol corresponding to inputed sensor reading and battery
	public static String getSymbol(String sensorReading, double sensorBattery) {
		var symbol = ""; //default return value is no symbol corresponding to 'not visited' sensor to show incorrect data
		if(sensorBattery<10) {
			symbol = "cross";
		}
		else {
			if(sensorReading != null || sensorReading != "NaN") {
				var reading = Double.parseDouble(sensorReading);
				if(0<=reading && reading<128) {
					symbol = "lighthouse";
				}
				else if(128<=reading && reading<256) {
					symbol = "danger";
				}
			}
		}
		return symbol;
	}
	
	

}
