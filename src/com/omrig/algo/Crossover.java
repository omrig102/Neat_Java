package com.omrig.algo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Crossover {

	public static Genome crossOver(Genome g1, Genome g2) {
		Genome child = new Genome(g1.getInputNodes().size(), g1.getOutputNodes().size());
		Genome parent1 = g1;
		Genome parent2 = g2;
		if(g1.getFitness() < g2.getFitness()) {
			parent1 = g2;
			parent2 = g1;
		}
		else if(g1.getFitness() == g2.getFitness()) {
			if(ThreadLocalRandom.current().nextBoolean()) {
				parent1 = g2;
				parent2 = g1;
			}
			else {
				parent1 = g1;
				parent2 = g2;
			}
		}
		Set<Connection> parent1Cons = new HashSet<>();
		for(List<Connection> cons : parent1.getInConnections().values()) {
			parent1Cons.addAll(cons);
		}
		
		
		Set<Connection> parent2Cons = new HashSet<>();
		for(List<Connection> cons : parent2.getInConnections().values()) {
			parent2Cons.addAll(cons);
		}
		
		
		for(Connection parent1Con : parent1Cons) {
			Connection con = parent1Con;
			Connection parent2Con = null;
			Map<Integer,Node> nodes = parent1.getNodes();
			Genome chosenGenome = parent1;
			for(Connection c : parent2Cons) {
				if(con.getInnovation() == c.getInnovation()) {
					parent2Con = c;
					if(ThreadLocalRandom.current().nextBoolean()) {
						con = c;
						nodes = parent2.getNodes();
						chosenGenome = parent2;
					}
					break;
				}
			}
			
			Connection childCon = con.cloneConnection();
			childCon.setWeight(con.getWeight());
			
			if(parent2Con != null && (!parent1Con.isEnabled() || !parent2Con.isEnabled())) {
				if(ThreadLocalRandom.current().nextDouble() < 0.75) {
					childCon.setEnabled(false);
				}
				else {
					childCon.setEnabled(true);
				}
			}
			else if(parent2Con == null) {
				childCon.setEnabled(con.isEnabled());
			}
			
			
			Node in = child.getNodes().get(con.getIn());
			if(in == null) {
				child.addExistingNode(con.getIn(), nodes.get(con.getIn()).getActivation());
			}
			

			Node out = child.getNodes().get(con.getOut());
			if(out == null) {
				child.addExistingNode(con.getOut(), nodes.get(con.getOut()).getActivation());
				
			}
			
			
			
			child.addConnection(childCon);
			
		}
		return child;
	}
	
	
}
