package com.omrig.algo.javaclient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.omrig.algo.Activation;
import com.omrig.algo.javaclient.GymFeignClient.CloseEnvRequest;
import com.omrig.algo.javaclient.GymFeignClient.CloseMonitorRequest;
import com.omrig.algo.javaclient.GymFeignClient.CreateEnvRequest;
import com.omrig.algo.javaclient.GymFeignClient.ResetEnvRequest;
import com.omrig.algo.javaclient.GymFeignClient.StartMonitorRequest;
import com.omrig.algo.javaclient.GymFeignClient.StepEnvRequest;
import com.omrig.algo.javaclient.GymFeignClient.uploadRequest;

import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

/**
 * Contains methods that correspond with the OpenAI Gym HTTP API 
 * (https://github.com/openai/gym-http-api), check there for more details about the methods.
 * @author Ryan Amaral - (ryan-amaral on GitHub)
 */
public class GymJavaHttpClient {

    public static String baseUrl = "http://127.0.0.1:5000"; // probably "http://127.0.0.1:5000"
    private static GymFeignClient feignClient = null;
    public static boolean isDiscrete = false;
    public static JSONObject actionSpace;
    public static JSONObject observationSpace;
    
    static {
    	feignClient = Feign.builder()
    			  .client(new OkHttpClient())
    			  .encoder(new GsonEncoder())
    			  .decoder(new GsonDecoder())
    			  .logger(new Slf4jLogger(GymFeignClient.class))
    			  .logLevel(Logger.Level.FULL)
    			  .target(GymFeignClient.class, baseUrl);

    }
    
    /**
     * List all of the environments you started that are currently running on the server.
     * @return A set of the environments' instance Id's.
     */
    public static Set<String> listEnvs() {
    	Response listEnvs = feignClient.listEnvs();
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(listEnvs.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/", "GET", null);
        return json.getJSONObject("all_envs").keySet();
    }

    /**
     * Creates a new environment of the type specified.
     * @param envId The id of the environment to create (ex: "CartPole-v0").
     * @return The instance id of the created environment.
     */
    public static String createEnv(String envId) {
    	Response createEnv = feignClient.createEnv(new CreateEnvRequest(envId));
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(createEnv.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/", "POST", "{\"env_id\":\"" + envId + "\"}");
        return json.getString("instance_id");
    }
    
    public static void closeEnv(String instanceId) {
    	feignClient.closeEnv(instanceId, new CloseEnvRequest(instanceId));
    	//connectAndFetch("/v1/envs/" + instanceId + "/close/", "POST", "{\"instance_id\":\"" + instanceId + "\"}");
    }

    /**
     * Resets the selected environment.
     * @param instanceId The id of the environment.
     * @return Whatever the observation of the environment is. Probably JSONArray.
     */
    public static JSONArray resetEnv(String instanceId) {
    	Response resetEnv = feignClient.resetEnv(instanceId, new ResetEnvRequest(instanceId));
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(resetEnv.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/" + instanceId + "/reset/", "POST", "{\"instance_id\":\"" + instanceId + "\"}");
        return json.getJSONArray("observation"); // probably of type JSONArray
    }
    
    public static Double[] flattenObservation(JSONArray obs) {
    	List<Double> obsArr = new ArrayList<>();
    	for(int i = 0;i<obs.length();i++) {
    		Object obj = obs.get(i);
    		if(obj instanceof JSONArray) {
    			Double[] res = flattenObservation((JSONArray)obj);
    			for(int j = 0;j<res.length;j++) {
    				obsArr.add(res[j]);
    			}
    		}
    		else {
    			obsArr.add(((Number)obj).doubleValue());
    		}
    	}
    	return obsArr.toArray(new Double[0]);
    }

    /**
     * Steps the environment.
     * @param instanceId The id of the environment.
     * @param action The action to do in the step.
     * @param isDiscreteSpace Whether space in the environment is discrete or not.
     * @return A StepObject, check out that class.
     */
    public static StepObject stepEnv(String instanceId, Double[] action, boolean render) {
    	JSONObject response = null;
    	
    	Object actionToUse = action;
    	if(isDiscrete) {
    		double[] actions = new double[action.length];
    		for(int i = 0;i<action.length;i++) {
    			actions[i] = action[i];
    		}
    		for(int i = 0;i<action.length;i++) {
    			action[i] = Activation.softmax(action[i].doubleValue(), actions);
    		}
    		double max = -Double.MAX_VALUE;
    		actionToUse = 0;
    		for(int i = 0;i<action.length;i++) {
    			if(max < action[i]) {
    				max = action[i];
    				actionToUse = i;
    			}
    		}
		}
		Response stepEnv = feignClient.stepEnv(instanceId, new StepEnvRequest(instanceId, actionToUse, render));
		try {
			response = new JSONObject(IOUtils.toString(stepEnv.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
        
        return new StepObject(
        		response.getJSONArray("observation"), response.getFloat("reward"), 
        		response.getBoolean("done"), response.get("info"));
    }
    
    public static Object sampleAction(String instanceId) {
    	Response sampleAction = feignClient.sampleAction(instanceId);
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(sampleAction.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/"+instanceId+"/action_space/sample", "GET", "{\"instance_id\":\"" + instanceId + "\"}");
    	return json.get("action");
    }

    /**
     * Gets the name and the dimensions of the environment's action space.
     * @param instanceId The id of the environment.
     * @return Whatever the action space of the environment is. Probably JSONObject.
     */
    public static JSONObject actionSpace(String instanceId) {
    	Response actionSpaceResponse = feignClient.getActionSpace(instanceId);
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(actionSpaceResponse.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/" + instanceId + "/action_space/", "GET", "{\"instance_id\":\"" + instanceId + "\"}");
    	actionSpace = json.getJSONObject("info");
    	isDiscrete = isActionSpaceDiscrete(actionSpace);
        return actionSpace;
    }
    
    /**
     * Gets the dimension from the JSONObject obtained from actionSpace.
     * @param jobj JSONObject from actionSpace.
     * @return Whether the space is discrete.
     */
    public static boolean isActionSpaceDiscrete(JSONObject jobj) {
        String name = jobj.getString("name");
        if(name.equals("Discrete")) {
            return true;
        }else {
            return false;
        }
    }
    
    /**
     * Gets the size of the action space (number of distinct actions).
     * @param jobj JSONObject from actionSpace.
     * @return Size of actionSpace.
     */
    public static int actionSpaceSize(JSONObject jobj) {
    	if(isActionSpaceDiscrete(jobj)) {
    		return jobj.getInt("n");
    	}
        return jobj.getJSONArray("high").length();
    }

    /**
     * *** I COULDN'T ACTUALLY GET THIS ONE TO WORK, MAYBE MY TEST ENVIRONMENT DOESN'T USE THIS? ***
     * Gets the name and the dimensions of the environment's observation space.
     * @param instanceId The id of the environment.
     * @return Whatever the observation space of the environment is.
     */
    public static JSONObject observationSpace(String instanceId) {
    	Response observationSpaceResponse = feignClient.getObservationSpace(instanceId);
    	JSONObject json = null;
		try {
			json = new JSONObject(IOUtils.toString(observationSpaceResponse.body().asInputStream(),StandardCharsets.UTF_8));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	//JSONObject response = connectAndFetch("/v1/envs/" + instanceId + "/observation_space/", "GET", "{\"instance_id\":\"" + instanceId + "\"}");
    	
    	observationSpace = json.getJSONObject("info");
    	
    	return observationSpace;
    }

    /**
     * *** DIDN'T TEST! ***
     * Start monitoring.
     * @param instanceId The id of the environment.
     * @param force Whether to clear existing training data.
     * @param resume Keep data that's already in.
     */
    public static void startMonitor(String instanceId, boolean force, boolean resume,String directory) {
    	feignClient.startMonitor(instanceId, new StartMonitorRequest(instanceId, force, resume,directory));
    	//connectAndFetch("/v1/envs/" + instanceId + "/monitor/start/", "POST", "{\"instance_id\":\"" + instanceId
		//        + "\", \"force\":" + Boolean.toString(force) + ", \"resume\":" + Boolean.toString(resume) + "}");
    }

    /**
     * *** DIDN'T TEST! ***
     * Flush all monitor data to disk.
     * @param instanceId The id of the environment.
     */
    public static void closeMonitor(String instanceId) {
    	feignClient.closeMonitor(instanceId, new CloseMonitorRequest(instanceId));
    	//connectAndFetch("/v1/envs/" + instanceId + "/monitor/close/", "POST", "{\"instance_id\":\"" + instanceId + "\"}");
    }

    /**
     * *** DIDN'T TEST! ***
     * Probably uploads your thing to OpenAI? The method just said "Flush all monitor data to disk"
     * on the Gym HTTP API GitHub page, but it seems to do something different, I'm new to gym so I
     * don't really know.
     * @param trainingDir
     * @param apiKey
     * @param algId
     */
    public static void upload(String trainingDir, String apiKey, String algId) {
    	feignClient.upload(new uploadRequest(trainingDir, apiKey, algId));
    	//connectAndFetch("/v1/upload/", "POST", "{\"training_dir\":\"" + trainingDir + "\"," + "\"api_key\":\"" + apiKey + "\","
		//        + "\"algorithm_id\":\"" + algId + "\"}");
    }

    /**
     * *** COULDN'T GET IT TO WORK! ***
     * Attempts to shutdown the server.
     */
    public static void shutdownServer() {
    	feignClient.shutDownServer();
    	//connectAndFetch("/v1/shutdown/", "POST", null);
    }

    /**
     * Does either a post or get request on the base url + urlEx. Learned from:
     * https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/ .
     * @param urlEx The extension to add onto base url.
     * @param mthd POST or GET.
     * @param args What to pass for a Post request, make null if not used.
     * @throws IOException 
     */
    private static JSONObject connectAndFetch(String urlEx, String mthd, String args) {
    	PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    	manager.setMaxTotal(20);
    	CloseableHttpClient client = HttpClients.custom().setConnectionManager(manager).build();
    	HttpResponse response = null;
    	HttpRequestBase request = null;
    	if("GET".equalsIgnoreCase(mthd)) {
    		request = new HttpGet(baseUrl + urlEx);
    		
    	}
    	else {
    		request = new HttpPost(baseUrl + urlEx);
			((HttpPost)request).setEntity(new StringEntity(args,ContentType.APPLICATION_JSON));
    	}
    	try {
			response = client.execute(request);
			if(response != null && response.getEntity() != null) {
				InputStream content = response.getEntity().getContent();
				return new JSONObject(IOUtils.toString(content,StandardCharsets.UTF_8));
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
    	finally {
    		try {
				client.close();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
    	}
		
		
    	
    }
}
