package com.omrig.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Data;

@Data
public class Specie {

	private List<Genome> genomes = new ArrayList<>();
	private Genome leader;
	private double adjustedFitness = 0.0;
	private DistributedRandomNumberGenerator generator;
	private int staleness = 0;
	private double bestFitness = -Double.MAX_VALUE;
	
	
	public void calcAdjustedFitness() {
		adjustedFitness = 0;
		for(Genome g : genomes) {
			adjustedFitness += g.getFitness();
		}
		adjustedFitness /= genomes.size();
	}
	
	public Genome getRandomMember() {
		return genomes.get(generator.getDistributedRandomNumber());
	}
	
	public void sortGenomes() {
		if(genomes.isEmpty()) {
			staleness = 200;
			return;
		}
		Collections.sort(genomes,(g1,g2) -> {
			if(g1.getFitness() > g2.getFitness()) {
				return 1;
			}
			if(g1.getFitness() < g2.getFitness()) {
				return -1;
			}
			return 0;
		});
		
		leader = genomes.get(genomes.size()-1);
	}
	
	public void calcProbs() {
		generator = new DistributedRandomNumberGenerator();
		int i = 0;
		for(Genome g : genomes) {
			
			generator.addNumber(i, g.getFitness());
			i++;
		}
	}
	
	/*public void chooseMutationRate() {
		DistributedRandomNumberGenerator mutationGen = new DistributedRandomNumberGenerator();
		double minFitness = genomes.get(0).getFitness();
		double maxFitness = genomes.get(genomes.size()-1).getFitness();
		int i = 0;
		for(Genome g : genomes) {
			mutationGen.addNumber(i, Population.scaleRange(g.getFitness(), minFitness, maxFitness, 0, 1));
			i++;
		}
		
		Genome g1 = genomes.get(mutationGen.getDistributedRandomNumber());
		Genome g2 = genomes.get(mutationGen.getDistributedRandomNumber());
		double mutationRate = (g1.getChangeWeightLimit() + g2.getChangeWeightLimit())/2;
		if(ThreadLocalRandom.current().nextDouble() <= mutationRate) {
			mutationRate += ThreadLocalRandom.current().nextDouble(-0.5,0.5);
			mutationRate = Math.max(0.1, mutationRate);
			mutationRate = Math.min(1.0, mutationRate);
		}
		
		
		this.addNodeLimit = mutationRate * 0.01;
		this.addConnectionLimit = mutationRate * 0.09;
		this.removeNodeLimit = 0.0;
		this.removeConnectionLimit = 0.0;
		this.changeWeightLimit = mutationRate;
		this.replaceWeightLimit = mutationRate * 0.01;
		this.enableDisableLimit = mutationRate * 0.09;
		
	}*/
	
	public Genome getChild() {
		
		
		Genome child = null;
		if(ThreadLocalRandom.current().nextDouble() < 0.25) {
			child = new Genome(genomes.get(generator.getDistributedRandomNumber()));
		}
		else {
			int g1Index = generator.getDistributedRandomNumber();
			int g2Index = generator.getDistributedRandomNumber();
			child = Crossover.crossOver(genomes.get(g1Index), genomes.get(g2Index));
		}
		
		
		Mutation.mutate(child);
		
		
		return child;
	}
	
	public void cull() {
		if(genomes.size() > 2) {
			int size = (int)Math.ceil(genomes.size()*0.25);
			List<Genome> newGenomes = new ArrayList<>();
			for(int i = 1;i<=size;i++) {
				newGenomes.add(genomes.get(genomes.size()-i));
			}
			genomes = newGenomes;
		}
		
		for(Genome g : genomes) {
			g.setFitness(g.getFitness()/genomes.size());
		}
	}
	
	public boolean isStale() {
		if(leader.getFitness() > bestFitness) {
			staleness = 0;
			bestFitness = leader.getFitness();
			return false;
		}
		staleness++;
		return staleness >= 15;
	}
}
