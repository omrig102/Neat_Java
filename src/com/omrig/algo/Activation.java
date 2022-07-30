package com.omrig.algo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Activation {

	public static Map<String,Function<Double,Double>> activations = new HashMap<>();
	public static String[] availableActivations = {"LINEAR","SIGMOID"};//{"SIGMOID","LINEAR","TANH","RELU","LEAKY_RELU"};
	static {
		activations.put("SIGMOID",x -> {
			return (1/( 1 + Math.pow(Math.E,(-1*x))));
		});
		activations.put("LINEAR",x -> x);
		activations.put("TANH",x -> Math.tanh(x));
		activations.put("RELU", x -> Math.max(0, x));
		activations.put("LEAKY_RELU",x-> {
			if(x > 0) {
				return x;
			}
			return 0.1 * x;
		});
	}
	
	public static double softmax(double input, double[] neuronValues) {
	    double total = Arrays.stream(neuronValues).map(Math::exp).sum();
	    return Math.exp(input) / total;
	}
	
}
