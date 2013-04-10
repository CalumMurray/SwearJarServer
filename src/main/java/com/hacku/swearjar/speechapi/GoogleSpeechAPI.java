/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hacku.swearjar.speechapi;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Sends a flac file to Google Speech API for the processing.  Can be run 
 * asynchronously. 
 * 
 * @author Neil
 */
public class GoogleSpeechAPI implements Callable<SpeechResponse>{
    
    private String filename;
    
    /**
     * 
     * @param speechFilename the path to speech file which is to be analysed
     */
    public GoogleSpeechAPI(String speechFilepath){
        filename = speechFilepath;
    }
    
    /**
     * Takes the audio at the specified path and sends it off to Google via HTTP
     * POST. Packages the JSON response from Google into a SpeechResponse
     * object.
     *
     * @param speechFilename path to the audio file
     * @return SpeechResponse containing recognised speech, null if error occurs
     */
    @Override
    public SpeechResponse call(){
        FileLock lock = null;

        try {
            // Read speech file 
            File file = new File(filename);
            FileInputStream inputStream = new FileInputStream(file);

            //Wait for file to become available
            FileChannel channel = inputStream.getChannel();
            lock = channel.lock(0, Long.MAX_VALUE, true);//channel.lock(); 
            ByteArrayInputStream data = new ByteArrayInputStream(
                    IOUtils.toByteArray(inputStream));

            // Set up the POST request
            HttpPost postRequest = getPost(data);

            // Make the request to google
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(postRequest);
                 
            //return the JSON stream
            SpeechResponse speechResponse = packageResponse(response);
            Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.INFO, "response {0}", speechResponse.toJson());
            return speechResponse;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioe) {
            Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.SEVERE, null, ioe);
        } catch (Exception ex) {
            Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                lock.release();
            } catch (IOException ex) {
                Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException npe) {
                Logger.getLogger(GoogleSpeechAPI.class.getName()).log(Level.SEVERE, null, npe);
            }
        }
        return null;
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

    /**
     * Uses GSON library to put the returned JSON into a SpeechResponse object
     *
     * @param response containing JSON to be packaged
     * @return SpeechResponse containing recognised speech
     * @throws IOException
     */
    private static SpeechResponse packageResponse(HttpResponse response) throws IOException {
        Gson gson = new Gson();
        InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
        SpeechResponse speechResponse = gson.fromJson(isr, SpeechResponse.class);
        return speechResponse;
    }
}
