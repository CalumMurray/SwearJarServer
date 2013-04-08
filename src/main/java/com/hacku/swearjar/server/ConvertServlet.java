package com.hacku.swearjar.server;

import com.hacku.swearjar.speechapi.GoogleSpeechAPI;
import com.hacku.swearjar.speechapi.SpeechResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.IOUtils;

/**
 * Servlet implementation class ConvertServlet
 */
@WebServlet(description = "Converts incoming file to .flac format before sending to Google's ASR.  Sends json response back to app.",
urlPatterns = {"/convert"})
@MultipartConfig(maxFileSize = 1024 * 1024 * 32)  //Accept files upto 32MB
public class ConvertServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final ExecutorService speechServicePool = Executors.newFixedThreadPool(1000);

    private static void log(String filename, String output) {
        FileOutputStream eos = null;
        try {
            eos = new FileOutputStream("/tmp/" + filename);
            IOUtils.copy(IOUtils.toInputStream(output), eos);
            eos.flush();
            eos.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioe) {
            Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ioe);
        } finally {
            try {
                eos.close();
            } catch (IOException ex) {
                Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Takes an audio file, transcodes it to flac, then performs speech
     * recognition. Gives a JSON response containing the recognised speech.
     *
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String baseDir = "/tmp";
        String baseFilename = "SwearJar_"
                + request.getSession().getCreationTime() //Timestamp
                + "_" + request.getSession().getId();  //Session ID

        String inputExt = ".3gp";
        String outputExt = ".flac";
        String inputFilename = baseDir + "/" + baseFilename + inputExt;
        String flacFilename = baseFilename + outputExt;

        //Read the wav file sent and store it in a .wav file
        Part part = request.getPart("Content");
        InputStream inputStream = part.getInputStream();
        FileOutputStream fos = new FileOutputStream(inputFilename);
        IOUtils.copy(inputStream, fos);
        fos.flush();
        fos.close();

        //encode the file as flac
        String[] outputFilenames = transcode(baseDir, baseFilename, inputExt, outputExt);

        String filenames = "";
        for (int i = 0; i < outputFilenames.length; i++) {
            filenames = filenames.concat(outputFilenames[i] + "\n");
        }

        //Do speech recogntion and return JSON
        SpeechResponse aggregateSpeech = getSpeechResponse(outputFilenames);
        response.getOutputStream().print(aggregateSpeech.toJson());
        //IOUtils.copy(IOUtils.toInputStream(aggregateSpeech.toJson()), response.getOutputStream());

        log("response", aggregateSpeech.toJson());

        //Temporary files can be deleted now
        /*delete(inputFilename);
         for(String filename : outputFilenames)
         delete(filename);*/
    }

    /**
     * Gets an aggregate SpeechResponse object based on the speech contained in
     * multiple flac files
     *
     * @param speechFiles
     * @return aggregate SpeechResponse
     */
    private static SpeechResponse getSpeechResponse(String[] speechFiles) {

        List<Future<SpeechResponse>> futureSpeechResponses = new LinkedList();

        for (String filename : speechFiles) {
            //Fire off files to Google asyncronously
            GoogleSpeechAPI speechService = new GoogleSpeechAPI(filename);
            futureSpeechResponses.add(speechServicePool.submit(speechService));
        }

        SpeechResponse aggregateSpeech = new SpeechResponse();

        //Wait for all the responses to become available and collate them
        for (Future<SpeechResponse> futureSpeechResponse : futureSpeechResponses) {

            try {
                //Wait, if necessary, until the response is available
                SpeechResponse response = futureSpeechResponse.get(20, TimeUnit.SECONDS);

                if (response != null) {
                    aggregateSpeech.concat(response);
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TimeoutException ex) {
                Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                // stop the executor pool to stop accepting new requests
                speechServicePool.shutdown();
            }
        }

        return aggregateSpeech;
    }

    /**
     * Causes the calling thread to wait for a maximum of millis for the File at
     * filename to be created
     *
     * @param millis
     * @param filename
     * @deprecated
     */
    private static File waitForFileCreation(String filename, int millis) {
        while (millis > 0) {
            try {
                File file = new File(filename);
                if (file.exists() && file.canRead()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return file;
                }

                Thread.sleep(1);
                millis--;
            } catch (InterruptedException ex) {
                Logger.getLogger(ConvertServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    /**
     * Deletes a file if it exists
     *
     * @param filename
     */
    private static void delete(String filename) {
        try {
            new File(filename).delete();
        } catch (NullPointerException ioe) {
            System.err.println("Error deleting: " + filename);
        }
    }

    /**
     * Transcodes input file to flac
     *
     * @param inputFile
     * @param outputFile
     * @return array of files created
     */
    private static String[] transcode(String baseDir, String baseFilename, String inputExt, String outputExt) {
        Runtime rt = Runtime.getRuntime();
        String output = "";

        try {

            String str = "sox_splitter " + baseDir + " " + baseFilename + " " + inputExt + " " + outputExt;
            //"echo test &>> /tmp/output";
                    /*"ffmpeg -i " + //Location of vlc
             inputFile + " -ar 8000 -sample_fmt s16 "//Location of input 
             + " " + outputFile;*/
            /*"run \"C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe\" -I --dummy-quiet " + //Location of vlc
             inputFile + //Location of input 
             " --sout=\"#transcode{acodec=flac, channels=1 ab=16 samplerate=16000}"
             + ":std{access=file, mux=raw, dst="
             + outputFile + //Location of output
             "}\" vlc://quit";*/

            Process pr = rt.exec(str);

            int exitStatus = pr.waitFor();

            output = IOUtils.toString(pr.getInputStream());

            /*FileOutputStream fos = new FileOutputStream("/tmp/output");
             IOUtils.copy(pr.getInputStream(), fos);
             fos.flush();
             fos.close();
             */
            FileOutputStream eos = new FileOutputStream("/tmp/errors");
            IOUtils.copy(pr.getErrorStream(), eos);
            eos.flush();
            eos.close();

            //output = IOUtils.toString(pr.getInputStream());

            System.out.println(System.currentTimeMillis() + " VLC exit code: " + exitStatus);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            log("output", output);
            return output.split("\n");
        }
    }
}
