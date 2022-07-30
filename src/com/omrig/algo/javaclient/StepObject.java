package com.omrig.algo.javaclient;

import org.json.JSONArray;

/**
 * Information that is returned from a step in the environment.
 * https://gym.openai.com/docs/#observations
 * @author Ryan Amaral - (ryan-amaral on GitHub)
 */
public class StepObject {

    public StepObject(JSONArray observation, float reward, boolean done,Object info) {
        this.observation = observation;
        this.reward = reward;
        this.done = done;
        this.info = info;
    }

    public JSONArray observation;
    public float reward;
    public boolean done;
    public Object info;
}
