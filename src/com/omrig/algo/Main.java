package com.omrig.algo;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.omrig.algo.javaclient.GymJavaHttpClient;

public class Main {

	public static void main(String[] args) throws IOException {

		FileUtils.deleteDirectory(new File("./genomes/"));
		FileUtils.deleteDirectory(new File("./movies/"));
		FileUtils.forceMkdir(new File("./movies"));
		Set<String> envs = GymJavaHttpClient.listEnvs();
		for(String env : envs) {
			GymJavaHttpClient.closeEnv(env);
		}
		BasicConfigurator.configure();
		Logger root = Logger.getRootLogger();
		root.setLevel(Level.INFO);
		Neat neat = new Neat();
		neat.start(Config.gameName, Config.populationSize, 5000);
	}
	
}
