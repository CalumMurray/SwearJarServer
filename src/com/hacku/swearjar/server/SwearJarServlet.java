package com.hacku.swearjar.server;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Servlet implementation class SwearJarServlet
 */
@WebServlet(description = "Converts incoming .wav file to .flac format before sending to Google's ASR.  Sends json response back to app.", urlPatterns = { "/convert" })
public class SwearJarServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Takes the audio at the specified path and sends it off to Google via HTTP
	 * POST. Packages the JSON response from Google into a SpeechResponse
	 * object.
	 * 
	 * @param speechFile path to the audio file
	 * @return SpeechResponse containing recognised speech, null if error occurs
	 */
	public static void getSpeechResponse(String speechFile) {
		try {
			// Read speech file
			InputStream inputStream = new FileInputStream(speechFile);
			ByteArrayInputStream data = new ByteArrayInputStream(
					IOUtils.toByteArray(inputStream));

			// Set up the POST request
			HttpPost postRequest = getPost(data);

			// Do the request
			HttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(postRequest);

			// Package the returned JSON into a SpeechResponse
			//SpeechResponse speechResponse = packageResponse(response);

			// Close the stream
			response.getEntity().getContent().close();

			//return speechResponse;

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		//return null;
	}


	/**
	 * Sets up the post request
	 * 
	 * @param data audio file
	 * @return HttpPost object with parameters initialised to audio file
	 */
	private static HttpPost getPost(ByteArrayInputStream data) {
		HttpPost postRequest = new HttpPost(
				"https://www.google.com/speech-api/v1/recognize"
						+ "?xjerr=1&pfilter=0&client=chromium&lang=en-US&maxresults=1");

		// Specify Content and Content-Type parameters for POST request
		MultipartEntity entity = new MultipartEntity();
		entity.addPart("Content", new InputStreamBody(data, "Content"));
		postRequest.setHeader("Content-Type", "audio/x-flac; rate=16000");
		postRequest.setEntity(entity);
		return postRequest;
	}

}
