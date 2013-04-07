package com.hacku.swearjar.speechapi;

import java.io.Serializable;

/**
 * Container for JSON returned from Google Speech API
 *
 * @author Neil
 */
public class Hypothesis implements Serializable {

    private static final long serialVersionUID = 1L;
    private String utterance;
    private float confidence;

    public Hypothesis() {
        utterance = "";
        confidence = 0f;
    }

    public Hypothesis(String utterance, float confidence) {
        this.utterance = utterance;
        this.confidence = confidence;
    }

    public String getUtterance() {
        return utterance;
    }

    public void setUtterance(String utterance) {
        this.utterance = utterance;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Concatenates the utterance from another hypothesis to this 
     * @param hyp 
     */
    public void concat(Hypothesis hyp){
        utterance = utterance.concat(" " + hyp.utterance);
        utterance = utterance.trim();
    }
}
