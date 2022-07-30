package com.omrig.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.nd4j.shade.guava.collect.Streams;

public class Mutation {


	
	public static void mutate(Genome g,double addNodeLimit,double addConnectionLimit,double removeNodeLimit,double removeConnectionLimit,double changeWeightLimit,double replaceWeightLimit,double enableDisableLimit) {
		addNode(g,addNodeLimit);
		addConnection(g,addConnectionLimit);
		changeWeight(g,changeWeightLimit,replaceWeightLimit);
		changeEnableDisableConnection(g, 0.0);
		removeConnection(g, 0.0);
		removeNode(g, 0.0);
	}
	
	public static void mutate(Genome g) {
		mutate(g,Config.addNodeLimit,Config.addConnectionLimit,Config.removeNodeLimit,Config.removeConnectionLimit,Config.changeWeightLimit,Config.replaceWeightLimit,Config.enableDisableLimit);
	}
	
	private static void changeEnableDisableConnection(Genome g,double limit) {
		if(ThreadLocalRandom.current().nextDouble() <= limit) {
			List<Connection> cons = new ArrayList<>();
			for(List<Connection> inCons : g.getInConnections().values()) {
				cons.addAll(inCons);
			}
			if(cons.isEmpty()) {
				return;
			}
			int index = ThreadLocalRandom.current().nextInt(cons.size());
			Connection con = cons.get(index);
			con.setEnabled(con.isEnabled() ? false : true);
		}
		
	}

	private static void addNode(Genome g,double limit) {
		if(ThreadLocalRandom.current().nextDouble() <= limit) {
			List<Connection> cons = new ArrayList<>();
			for(List<Connection> inCons : g.getInConnections().values()) {
				cons.addAll(inCons);
			}
			
			if(cons.isEmpty()) {
				return;
			}
			int index = ThreadLocalRandom.current().nextInt(cons.size());
			Connection con = cons.get(index);
			con.setEnabled(false);
			Integer sharedNodeId = Connection.getMiddleNodeId(con.getIn(), con.getOut());	
			
			Node node = null;
			if(sharedNodeId == null) {
				node = Node.createNewNode(NodeType.HIDDEN, "SIGMOID");
				g.getNodes().put(node.getId(), node);
				g.getHiddenNodes().add(node);
			}
			else if(g.getNodes().containsKey(sharedNodeId)){
				node = g.getNodes().get(sharedNodeId);
				
			}
			else {
				node = Node.createExistingNodeById(sharedNodeId,"SIGMOID");
				g.getNodes().put(node.getId(), node);
				g.getHiddenNodes().add(node);
			}
			Connection con1 = Connection.getConnection(con.getIn(), node.getId(), 1.0, true);
			Connection con2 = Connection.getConnection(node.getId(),con.getOut(), con.getWeight(), true);
			
			g.addConnection(con1);
			g.addConnection(con2);
		}
		
		
	}
	
	private static void removeNode(Genome g,double limit) {
		if(ThreadLocalRandom.current().nextDouble() <= limit) {
			if(g.getHiddenNodes().isEmpty()) {
				return;
			}
			int index = ThreadLocalRandom.current().nextInt(g.getHiddenNodes().size());
			Node nodeToRemove = g.getHiddenNodes().get(index);
			g.removeNode(nodeToRemove);
			
		}
		
		
	}
	
	private static void addConnection(Genome g,double limit) {
		if(ThreadLocalRandom.current().nextDouble() <= limit) {
			Node from = null;
			Node to = null;
			List<Node> inputsHiddens = Streams.concat(g.getInputNodes().stream(),g.getHiddenNodes().stream()).collect(Collectors.toList());
			List<Node> HiddensOutputs = Streams.concat(g.getHiddenNodes().stream(),g.getOutputNodes().stream()).collect(Collectors.toList());
			
			for(int i = 0;i<100;i++) {
				
				int fromIndex = ThreadLocalRandom.current().nextInt(inputsHiddens.size());
				from = inputsHiddens.get(fromIndex);
				
				int toIndex = ThreadLocalRandom.current().nextInt(HiddensOutputs.size());
				to = HiddensOutputs.get(toIndex);
				boolean found = false;
				if(g.getOutConnections().get(from.getId()) != null) {
					List<Connection> cons = g.getOutConnections().get(from.getId());
					for(Connection con : cons) {
						if(con.getOut() == to.getId()) {
							from = null;
							to = null;
							found = true;
							break;
						}
					}
					
				}
				if(found) {
					continue;
				}
				else {
					break;
				}
			}
			
			if(from != null && to != null) {
				Connection newCon = Connection.getConnection(from.getId(), to.getId(), ThreadLocalRandom.current().nextDouble(-1.0, 1.0), true);
				g.addConnection(newCon);
			}
			
			
			
		}
		
		
		
	}
	
	private static void removeConnection(Genome g,double limit) {
		if(ThreadLocalRandom.current().nextDouble() <= limit) {
			Collection<List<Connection>> cons = g.getInConnections().values();
			if(cons.isEmpty()) {
				return;
			}
			Iterator<List<Connection>> it = cons.iterator();
			int indexCons = ThreadLocalRandom.current().nextInt(cons.size());
			int i = 0;
			List<Connection> chosenCons = null;
			while(it.hasNext()) {
				chosenCons = it.next();
				if(i == indexCons) {
					break;
				}
				i++;
			}
			
			int index = ThreadLocalRandom.current().nextInt(chosenCons.size());
			Connection conRemoved = chosenCons.get(index);
			g.removeConnection(conRemoved);
			
		}
	}
	
	private static void changeWeight(Genome g,double changeLimit,double replaceLimit) {
		if(ThreadLocalRandom.current().nextDouble() <= changeLimit) {
			List<Connection> cons = new ArrayList<>();
			for(List<Connection> inCons : g.getInConnections().values()) {
				cons.addAll(inCons);
			}
			
			if(cons.isEmpty()) {
				return;
			}
			int index = ThreadLocalRandom.current().nextInt(cons.size());
			Connection con = cons.get(index);
			con.setWeight(con.getWeight() + ThreadLocalRandom.current().nextDouble(-1.0, 1.0));
		}
		if(ThreadLocalRandom.current().nextDouble() <= replaceLimit) {
			List<Connection> cons = new ArrayList<>();
			for(List<Connection> inCons : g.getInConnections().values()) {
				cons.addAll(inCons);
			}
			
			if(cons.isEmpty()) {
				return;
			}
			int index = ThreadLocalRandom.current().nextInt(cons.size());
			Connection con = cons.get(index);
			con.setWeight(con.getWeight() + ThreadLocalRandom.current().nextDouble(-1.0, 1.0));
		}
	}
	
}
