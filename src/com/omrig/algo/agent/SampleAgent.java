package com.omrig.algo.agent;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.omrig.algo.javaclient.GymJavaHttpClient;
import com.omrig.algo.javaclient.StepObject;

public class SampleAgent {

    public static void main(String[] args) {

        GymJavaHttpClient.baseUrl = "http://127.0.0.1:5000"; // this is the default value, but just showing that you can change it

        String id = GymJavaHttpClient.createEnv("LunarLander-v2"); // create an environment
        GymJavaHttpClient.observationSpace(id);
        JSONObject actionSpace = GymJavaHttpClient.actionSpace(id);

        // Do this if not a standard attribute
        System.out.println(actionSpace.getClass().getName()); // helpful to know how to deal with
        System.out.println(actionSpace); // helpful to see format of object
        //int numActions = ((JSONObject)actionSpace).getInt("n");

        // but we have method to get action space size from action space object
        int numActions = GymJavaHttpClient.actionSpaceSize(actionSpace);

        Double[] action = null; // action for agent to do
        JSONArray obs = GymJavaHttpClient.resetEnv(id); // reset the environment (get initial observation)
        System.out.println(obs.getClass().getName());// see what observation looks like to work with it
        System.out.println(obs.toString());

        Boolean isDone = false; // whether current episode is done
        float reward = 0;
        int counter = 0;

        while(true) { // do steps
            //action = GymJavaHttpClient.sampleAction(id);
        	Double[] flattenObs = GymJavaHttpClient.flattenObservation(obs);
        	double sum = 0;
        	for(int i = 0;i<flattenObs.length;i++) {
        		sum += flattenObs[i];
        	}
        	if(GymJavaHttpClient.isDiscrete) {
        		action = new Double[1];
        		action[0] = (double) Math.round(sum % numActions);
        	}
        	else {
        		action = new Double[numActions];
        		JSONArray high = actionSpace.getJSONArray("high");
        		JSONArray low = actionSpace.getJSONArray("low");
        		for(int i = 0;i<numActions;i++) {
        			action[i] = sum % (high.getDouble(i) - low.getDouble(i));
        		}
        	}
            StepObject step = GymJavaHttpClient.stepEnv(id, action, true);
            obs = step.observation;
            isDone = step.done;
            if(isDone) {
            	GymJavaHttpClient.resetEnv(id);
            }
            reward += step.reward;
            counter++;
        }

        //System.out.println("The agent got reward: " + reward);
    }

    /**
     * Do a policy where you add the values in observation and return 1 or 0 based on the sum.
     * @param obs
     * @return
     */
    public static int obsToAction(Object obs) {
        Iterator<Object> iter = ((JSONArray)obs).iterator();
        double sum = 0;
        while(iter.hasNext()) {
            sum += (double)iter.next();
        }

        if(sum > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
