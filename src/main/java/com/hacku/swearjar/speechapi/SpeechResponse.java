package com.hacku.swearjar.speechapi;

import com.google.gson.Gson;
import java.io.Serializable;

/**
 * Container for JSON returned from Google Speech API
 *
 * @author Neil
 */
public class SpeechResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    int status;
    String id;
    Hypothesis[] hypotheses;

    public SpeechResponse() {
        status = 6;
        id = "";
        hypotheses = new Hypothesis[1];
        hypotheses[0] = new Hypothesis();
    }

    /**
     * Constructs a single SpeechResponse object which contains a composite
     * hypotheses from multiple SpeechResponse objects
     *
     * @param responses
     */
    public SpeechResponse(SpeechResponse[] responses) {
        status = responses[0].status;
        id = responses[0].id;
        hypotheses = responses[0].hypotheses;

        for (SpeechResponse response : responses) {
            hypotheses[0].setUtterance(getBestUtterance().concat(" " + response.getBestUtterance()));
        }
    }

    /**
     * Concatenates the hypotheses of the input SpeechResponse object to this
     * hypotheses of this SpeechResponse
     *
     * SpeechResponses must have the same number of hypotheses to be
     * concatenated
     *
     * @param response
     */
    public void concat(SpeechResponse response) {
        //Check length equality and valid status code
        if (hypotheses.length == response.hypotheses.length && response.status == 0) {
            for (int i = 0; i < hypotheses.length; i++) {
                hypotheses[i].concat(response.hypotheses[i]);
            }
        }
    }

    public String getBestUtterance() {
        try {
            if (hypotheses == null || hypotheses[0].getUtterance() == null) {
                return "";
            }
            return hypotheses[0].getUtterance();
        } catch (ArrayIndexOutOfBoundsException aie) {
            return "";
        }

    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Hypothesis[] getHypotheisis() {
        return hypotheses;
    }

    public void setHypotheisis(Hypothesis[] hypotheisis) {
        this.hypotheses = hypotheisis;
    }

    /**
     * @return this object represented as a JSON string
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
