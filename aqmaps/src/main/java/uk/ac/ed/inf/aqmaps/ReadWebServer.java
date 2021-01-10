package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class ReadWebServer {
	//returns web server content, given the non-constant part of the url and port to receive it from
	public static String webServerContent(String urlSegment, String port) throws IOException, InterruptedException {		
		var client = HttpClient.newHttpClient(); 
		
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+ port + urlSegment))
				.build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (java.net.ConnectException e) {
			System.out.println("Fatal error: Unable to connect to server at port " + port +".");
			System.exit(1);
		}
		return response.body();
	}
	
}
