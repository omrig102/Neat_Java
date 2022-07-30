package com.omrig.algo;

import org.json.JSONObject;

import com.omrig.algo.javaclient.GymJavaHttpClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Neat {

	
	private Population population;
	private String envName;

	
	
	private void saveBestGenome(int generation) {
		Genome.save(generation, population.getBestGenome(),"best");
		Genome.save(generation, population.getWorstGenome(),"worst");
		Genome.save(generation, population.getMostConnectionsGenome(),"mostCons");
		if(Config.generation % Config.saveVideoInterval == 0) {
			new Genome(population.getBestGenome()).evaluateFitness(envName, false, Config.numOfAttempts, true);
		}
	}
	
	
	public void simulate(int generation,String envName,int numOfAttempts) {
		Genome g = Genome.read(generation);
		String envId = GymJavaHttpClient.createEnv(envName);
		JSONObject actionSpace = GymJavaHttpClient.actionSpace(envId);
		GymJavaHttpClient.actionSpaceSize(actionSpace);
		GymJavaHttpClient.flattenObservation(GymJavaHttpClient.resetEnv(envId));
		GymJavaHttpClient.closeEnv(envId);
		g.evaluateFitness(envName, true,numOfAttempts,false);
		log.info("fitness : {}",g.getFitness());
	}
	
	public void start(String envName,int populationSize,float highScore) {
		this.envName = envName;
		String envId = GymJavaHttpClient.createEnv(envName);
		JSONObject actionSpace = GymJavaHttpClient.actionSpace(envId);
		int outputSize = GymJavaHttpClient.actionSpaceSize(actionSpace);
		Double[] flattenObservation = GymJavaHttpClient.flattenObservation(GymJavaHttpClient.resetEnv(envId));
		int inputSize = flattenObservation.length;
		GymJavaHttpClient.closeEnv(envId);
		this.population = new Population(populationSize,inputSize,outputSize,envName);
		double currentScore = 0;
		while(currentScore < highScore) {
			//population.speciate();
			population.evaluateFitness();
			Genome bestGenome = population.getBestGenome();
			saveBestGenome(Config.generation);
			currentScore = bestGenome.getFitness();
			log.info("generation {} best fitenss : {}, average fitness : {}",Config.generation,currentScore,population.getAverageFitness());
			Config.generation++;
			
			population.createNewPopulation();
		}
		
	}
	
}
