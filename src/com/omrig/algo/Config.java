package com.omrig.algo;

public class Config {
	
	public static int generation = 0;
	public static int saveVideoInterval = 10;
	public static int numOfThreads = 50;
	public static String gameName = "LunarLander-v2";
	public static int populationSize = 64;
	public static int numOfAttempts = 4;
	public static int maxSteps = 1000;
	public static double c1 = 1.0;
	public static double c2 = 0.5;
	public static double c3 = 1.0;
	public static double compatibilityThreshold = 1.0;
	public static double addNodeLimit = 0.01;
	public static double removeNodeLimit = 0.0;
	public static double addConnectionLimit = 0.09;
	public static double removeConnectionLimit = 0.0;
	public static double changeWeightLimit = 0.9;
	public static double replaceWeightLimit = 0.01;
	public static double enableDisableLimit = 0.09;
}
