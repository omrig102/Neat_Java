package com.omrig.algo;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Simulate {

	public static void main(String[] args) {
		BasicConfigurator.configure();
		Logger root = Logger.getRootLogger();
		root.setLevel(Level.INFO);
		Neat neat = new Neat();
		neat.simulate(100, Config.gameName,1);
	}
	
}
