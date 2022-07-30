package com.omrig.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.nd4j.nativeblas.Nd4jCpu.concat;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class Population {

	private List<Genome> genomes;
	private int populationSize;
	private int inputSize;
	private int outputSize;
	private String envName;
	private Genome bestGenome;
	private Genome worstGenome;
	private Genome mostConnectionsGenome;
	private double averageFitness;
	private List<Specie> species = new ArrayList<>();
	
	
	public Population(int populationSize,int inputSize,int outputSize,String envName) {
		this.populationSize = populationSize;
		this.inputSize = inputSize;
		this.outputSize = outputSize;
		this.envName = envName;
		createInitialPopulation();
	}
	
	
	private void createInitialPopulation() {
		this.genomes = new ArrayList<>();
		for(int i = 0;i<populationSize;i++) {
			//Genome g = Genome.generateRandomBaseGenome(inputSize, outputSize);
			Genome g = new Genome(inputSize, outputSize);
			//Genome g = Genome.generateFullyConnected(inputSize, outputSize);
			genomes.add(g);
		}
	}
	 
	
	public void evaluateFitness() {
		ExecutorService executor = Executors.newFixedThreadPool(Config.numOfThreads);
		for(Genome genome : this.genomes) {
			executor.submit(() -> {
				Genome g = genome;
				try {
					g.evaluateFitness(envName,false,Config.numOfAttempts,false);
				}
				catch(Exception e) {
					log.error("error while evaluating fitness",e);
					throw new RuntimeException(e);
				}
				
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		  
		}
		
		double maxFitness = -Double.MAX_VALUE;
		double minFitness = Double.MAX_VALUE;
		int maxNumOfConnections = Integer.MIN_VALUE;
		averageFitness = 0;
		bestGenome = null;
		worstGenome = null;
		mostConnectionsGenome = null;
		for(Genome genome : this.genomes) {
			averageFitness += genome.getFitness();
			if(maxFitness < genome.getFitness()) {
				maxFitness = genome.getFitness();
				bestGenome = genome;
			}
			if(minFitness > genome.getFitness()) {
				minFitness = genome.getFitness();
				worstGenome = genome;
			}
			if(maxNumOfConnections < genome.getInConnections().size()) {
				maxNumOfConnections = genome.getInConnections().size();
				mostConnectionsGenome = genome;
			}
 		}
		averageFitness /= this.genomes.size();

		
	}
	public static double scaleRange(double x,double fromMin,double fromMax,double toMin,double toMax) {
		return (x - fromMin) * (toMax - toMin) / (fromMax - fromMin) + toMin;
	}
	
	public void createNewPopulation() {
		Collections.sort(genomes,(g1,g2)->{
			if(g1.getFitness() > g2.getFitness()) {
				return 1;
			}
			if(g1.getFitness() < g2.getFitness()) {
				return -1;
			}
			return 0;
		});
		
		double maxFitness = genomes.get(genomes.size()-1).getFitness();
		double minFitness = genomes.get(0).getFitness();
		
		speciate();
		List<Specie> survivedSpecies = new ArrayList<>();
		for(Specie s : species) {
			if(s.getGenomes().isEmpty()) {
				continue;
			}
			s.sortGenomes();
			if(!s.isStale()) {
				for(Genome g : s.getGenomes()) {
					if(g.getFitness() == maxFitness) {
						g.setFitness(1.0);
					}
					else {
						g.setFitness(scaleRange(g.getFitness(), minFitness, maxFitness, 0, 1));
					}
				}
				
				s.cull();
				s.calcAdjustedFitness();
				s.calcProbs();
				survivedSpecies.add(s);
			}
			
		}
		
		
		species = survivedSpecies;
		if(species.isEmpty()) {
			//Config.compatibilityThreshold -= 0.1;
			Config.compatibilityThreshold = Math.max(0.0, Config.compatibilityThreshold);
			createNewPopulation();
			return;
		}
		Collections.sort(species,(s1,s2) -> {
			if(s1.getAdjustedFitness() > s2.getAdjustedFitness()) {
				return 1;
			}
			else if(s1.getAdjustedFitness() < s2.getAdjustedFitness()) {
				return -1;
			}
			return 0;
		});
		
		genomes = new ArrayList<>();
		double sumAdjustedFitness = 0.0;
		for(Specie s : species) {
			sumAdjustedFitness += s.getAdjustedFitness();
		}
		
		
		List<Specie> newSpecies = new ArrayList<>();
		for(Specie s : species) {
			if((s.getAdjustedFitness() / sumAdjustedFitness) * populationSize >= 1) {
				newSpecies.add(s);
				genomes.add(s.getLeader());
			}
		}
		species = newSpecies;
		for(Specie s : species) {
			int numOfChilds = (int)Math.floor(((s.getAdjustedFitness() / sumAdjustedFitness) * populationSize)) - 1;
			for(int i = 0;i<numOfChilds;i++) {
				genomes.add(s.getChild());
			}
		}
		
		while(genomes.size() < populationSize) {
			genomes.add(species.get(species.size()-1).getChild());
		}
		
		if(genomes.size() != populationSize) {
			throw new RuntimeException("CRAP");
		}
	}
	
	public void createNewPopulation2() {
		
		Collections.sort(genomes,(g1,g2)->{
			if(g1.getFitness() > g2.getFitness()) {
				return 1;
			}
			if(g1.getFitness() < g2.getFitness()) {
				return -1;
			}
			return 0;
		});
		
		double maxFitness = genomes.get(genomes.size()-1).getFitness();
		double minFitness = genomes.get(0).getFitness();
		
		
		/*DistributedRandomNumberGenerator mutationGen = new DistributedRandomNumberGenerator();
		int mutationCount = 0;
		for(Genome g : genomes) {
			mutationGen.addNumber(mutationCount, scaleRange(g.getFitness(), minFitness, maxFitness, 0, 1));
			mutationCount++;
		}
		Genome mutationG1 = genomes.get(mutationGen.getDistributedRandomNumber());
		Genome mutationG2 = genomes.get(mutationGen.getDistributedRandomNumber());
		double mutationRate = (mutationG1.getChangeWeightLimit() + mutationG2.getChangeWeightLimit())/2;
		if(ThreadLocalRandom.current().nextDouble() >= mutationRate) {
			mutationRate += ThreadLocalRandom.current().nextDouble(-0.5,0.5);
			mutationRate = Math.max(0.1, mutationRate);
			mutationRate = Math.min(1.0, mutationRate);
		}
		
		
		addNodeLimit = mutationRate * 0.01;
		addConnectionLimit = mutationRate * 0.09;
		removeNodeLimit = 0.0;
		removeConnectionLimit = 0.0;
		changeWeightLimit = mutationRate;
		replaceWeightLimit = mutationRate * 0.01;
		enableDisableLimit = mutationRate * 0.09;*/
		speciate();
		List<Specie> survivedSpecies = new ArrayList<>();
		for(Specie s : species) {

			s.sortGenomes();
			if(!s.isStale()) {
				for(Genome g : s.getGenomes()) {
					g.setFitness(scaleRange(g.getFitness(), minFitness, maxFitness, 0, 1));
				}
				
				s.cull();
				//s.chooseMutationRate();
				s.calcAdjustedFitness();
				s.calcProbs();
				survivedSpecies.add(s);
			}
			
		}
		
		
		/*double averageFitness = 0.0;
		for(Genome g : genomes) {
			averageFitness += g.getFitness();
		}
		averageFitness /= genomes.size();
		for(Specie s : species) {
			s.calcMutationRate(averageFitness);
		}*/
		
		
		species = survivedSpecies;
		if(species.isEmpty()) {
			Config.compatibilityThreshold -= 0.1;
			Config.compatibilityThreshold = Math.max(0.0, Config.compatibilityThreshold);
			createNewPopulation();
			//createInitialPopulation();
			return;
		}
		Collections.sort(species,(s1,s2) -> {
			if(s1.getAdjustedFitness() > s2.getAdjustedFitness()) {
				return 1;
			}
			else if(s1.getAdjustedFitness() < s2.getAdjustedFitness()) {
				return -1;
			}
			return 0;
		});
		
		
		
		
		genomes = new ArrayList<>();
		double sumAdjustedFitness = 0.0;
		for(Specie s : species) {
			sumAdjustedFitness += s.getAdjustedFitness();
		}
		
		
		List<Specie> newSpecies = new ArrayList<>();
		//DistributedRandomNumberGenerator generator = new DistributedRandomNumberGenerator();
		int j = 0;
		for(Specie s : species) {
			if((s.getAdjustedFitness() / sumAdjustedFitness) * populationSize >= 1) {
				newSpecies.add(s);
				genomes.add(s.getLeader());
				//generator.addNumber(j, scaleRange(s.getAdjustedFitness(), minAdjustedFitness, maxAdjustedFitenss, 0.1, 1));
				j++;
			}
		}
		species = newSpecies;
		for(Specie s : species) {
			int numOfChilds = (int)Math.floor(((s.getAdjustedFitness() / sumAdjustedFitness) * populationSize)) - 1;
			for(int i = 0;i<numOfChilds;i++) {
				genomes.add(s.getChild());
			}
		}
		/*while(genomes.size() < populationSize && species.size() > 1) {
			Specie s1 = species.get(generator.getDistributedRandomNumber());
			Specie s2 = species.get(generator.getDistributedRandomNumber());
			Genome g1 = s1.getRandomMember();
			Genome g2 = s2.getRandomMember();
			Genome child = Crossover.crossOver(g1, g2);
			Mutation.mutate(child,(s1.getAddNodeLimit() + s2.getAddNodeLimit())/2,(s1.getAddConnectionLimit() + s2.getAddConnectionLimit())/2,(s1.getChangeWeightLimit() + s2.getChangeWeightLimit())/2,(s1.getReplaceWeightLimit() + s2.getReplaceWeightLimit())/2,(s1.getEnableDisableLimit() + s2.getEnableDisableLimit())/2);
			genomes.add(child);
		}*/
		
		while(genomes.size() < populationSize) {
			genomes.add(species.get(species.size()-1).getChild());
		}
		
		if(genomes.size() != populationSize) {
			throw new RuntimeException("CRAP");
		}
		
		/*genomes.add(species.get(species.size()-1).getLeader());
		if(species.size() > 1) {
			genomes.add(species.get(species.size()-2).getLeader());
		}
		DistributedRandomNumberGenerator generator = new DistributedRandomNumberGenerator();
		
	
		double maxAverageFitness = species.get(species.size()-1).getAdjustedFitness();
		double minAverageFitness = species.get(0).getAdjustedFitness();
		int i = 0;
		for(Specie s : species) {
			generator.addNumber(i, (s.getAdjustedFitness() - minAverageFitness) / (Math.max(1.0, maxAverageFitness-minAverageFitness)));
			i++;
		}
		while(genomes.size() < populationSize) {
			Specie s = species.get(generator.getDistributedRandomNumber());
			genomes.add(s.getChild());
		}*/
		
		
		/*genomes = new ArrayList<>();
		int i = 0;
		survivedSpecies = new ArrayList<>();
		for(Specie s : species) {
			double prob = scaleRange(s.getAdjustedFitness(), minAdjustedFitness, maxAdjustedFitness, 0, 1);
			if(prob > 0.0) {
				if(ThreadLocalRandom.current().nextBoolean()) {
					genomes.add(s.getLeader());
				}
				
				generator.addNumber(i, prob);
				survivedSpecies.add(s);
				i++;
			}
			
			
		}
		species = survivedSpecies;
		int numOfChilds = populationSize - genomes.size();
		for(int j = 0;j<numOfChilds;j++) {
			Specie s = species.get(generator.getDistributedRandomNumber());
			genomes.add(s.getChild());
		}*/
		
		
		/*DistributedRandomNumberGenerator generator = new DistributedRandomNumberGenerator();
		for(int i = 0;i<genomes.size();i++) {
			double probability = 0.0;
			if(maxFitness != 0.0) {
				double fitness = genomes.get(i).getFitness();
				probability = scaleRange(fitness, minFitness, maxFitness *(-1), 0, 1);
				
				
			}
			generator.addNumber(i, probability);
		}
		
		int pickedSize = populationSize/2;
		List<Genome> pickedGenomes = new ArrayList<>();
		for(int i = populationSize-1;i>=populationSize - pickedSize;i--) {
			pickedGenomes.add(genomes.get(generator.getDistributedRandomNumber()));
		}
		generator = new DistributedRandomNumberGenerator();
		for(int i = 0;i<pickedGenomes.size();i++) {
			double probability = 0.0;
			if(maxFitness != 0.0) {
				double fitness = pickedGenomes.get(i).getFitness();
				probability = scaleRange(fitness, minFitness, maxFitness, 0, 1);
				
				
			}
			generator.addNumber(i, probability);
		}
		
		genomes = new ArrayList<>();
		
		for(int i = 0;i<populationSize;i++) {
			int g1Index = ThreadLocalRandom.current().nextInt(pickedGenomes.size());
			int g2Index = ThreadLocalRandom.current().nextInt(pickedGenomes.size());
			Genome child = Crossover.crossOver(pickedGenomes.get(g1Index), pickedGenomes.get(g2Index));
			Mutation.mutate(child);
			genomes.add(child);
		}
		for(Genome g : pickedGenomes) {
			this.genomes.add(g);
		}*/
	}
	
	public void speciate() {


		List<Genome> leaders = new ArrayList<>();
		for(Specie s : species) {
			s.setGenomes(new ArrayList<>());
			//s.getGenomes().add(s.getLeader());
			//leaders.add(s.getLeader());
		}
		
		for(Genome g : genomes) {
			//if(leaders.contains(g)) {
			//	continue;
			//}
			boolean foundSpecie = false;
			for(Specie s: species) {
				if(s.getLeader().isSimilar(g)) {
					s.getGenomes().add(g);
					foundSpecie = true;
					break;
				}
			} 
			
			if(!foundSpecie) {
				Specie newSpecie = new Specie();
				newSpecie.setLeader(g);
				newSpecie.getGenomes().add(g);
				species.add(newSpecie);
			}
		}
	}
	
}
