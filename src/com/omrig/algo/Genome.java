package com.omrig.algo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nd4j.shade.protobuf.common.io.Files;

import com.omrig.algo.javaclient.GymJavaHttpClient;
import com.omrig.algo.javaclient.StepObject;

import lombok.Data;

@Data
public class Genome implements Serializable {

	private static final long serialVersionUID = 4276839530062173292L;
	
	private List<Node> inputNodes = new ArrayList<>();
	private List<Node> outputNodes = new ArrayList<>();
	private List<Node> hiddenNodes = new ArrayList<>();
	private Map<Integer,Node> nodes = new HashMap<>();
	private Map<Integer,List<Connection>> inConnections = new HashMap<>();
	private Map<Integer,List<Connection>> outConnections = new HashMap<>();
	private double fitness;
	//private MutationRate mutationRate;
	private double mutationRate;
	
	public Genome(Genome g) {
		this(g.getInputNodes().size(),g.getOutputNodes().size());
		for(Node hidden : g.getHiddenNodes()) {
			Node newNode = Node.createExistingNodeById(hidden.getId(), hidden.getActivation());
			hiddenNodes.add(newNode);
			nodes.put(newNode.getId(), newNode);
		}
		for(Entry<Integer,List<Connection>> entry : g.getInConnections().entrySet()) {
			inConnections.put(entry.getKey(), new ArrayList<>());
			for(Connection con : entry.getValue()) {
				inConnections.get(entry.getKey()).add(Connection.getConnection(con.getIn(), con.getOut(), con.getWeight(), con.isEnabled()));
			}
		}
		for(Entry<Integer,List<Connection>> entry : g.getOutConnections().entrySet()) {
			outConnections.put(entry.getKey(), new ArrayList<>());
			for(Connection con : entry.getValue()) {
				outConnections.get(entry.getKey()).add(Connection.getConnection(con.getIn(), con.getOut(), con.getWeight(), con.isEnabled()));
			}
		}
		//mutationRate = g.getMutationRate();
		mutationRate = g.getMutationRate();
	}
	
	public Genome(int inputSize,int outputSize) {
		int id = 0;
		for(int i = 0;i<inputSize;i++) {
			Node node = null;
			if(Node.getNodeTypeById(id) == null) {
				node = Node.createNewNode(NodeType.INPUT, "LINEAR");
			}
			else {
				node = Node.createExistingNodeById(id,"LINEAR");
			}
			
			
			inputNodes.add(node);
			nodes.put(node.getId(), node);
			id++;
		}

		for(int i = 0;i<outputSize;i++) {
			Node node = null;
			if(Node.getNodeTypeById(id) == null) {
				node = Node.createNewNode(NodeType.OUTPUT, "LINEAR");
			}
			else {
				node = Node.createExistingNodeById(id,"LINEAR");
			}
			outputNodes.add(node);
			nodes.put(node.getId(), node);
			id++;
		}
		
		/*mutationRate = new MutationRate();
		mutationRate.setChangeWeightLimit(ThreadLocalRandom.current().nextDouble());
		mutationRate.setAddConnectionLimit(ThreadLocalRandom.current().nextDouble(mutationRate.getChangeWeightLimit()));
		mutationRate.setAddNodeLimit(ThreadLocalRandom.current().nextDouble(mutationRate.getAddConnectionLimit()));
		mutationRate.setReplaceWeightLimit(ThreadLocalRandom.current().nextDouble(mutationRate.getAddNodeLimit()));
		mutationRate.setEnableDisableLimit(0.0);*/
		
	}
	
	
	public void addExistingNode(int nodeId,String activation) {
		if(nodes.containsKey(nodeId)) {
			return;
		}
		Node node = Node.createExistingNodeById(nodeId,activation);
		if(node == null) {
			throw new RuntimeException("node id does not exists");
		}
		nodes.put(nodeId, node);
		if(node.getType() == NodeType.INPUT) {
			inputNodes.add(node);
		}
		else if(node.getType() == NodeType.OUTPUT) {
			outputNodes.add(node);
		}
		else {
			hiddenNodes.add(node);
		}
	}
	
	public void addConnection(Connection connection) {
		
		int in = connection.getIn();
		int out = connection.getOut();
		if(!nodes.containsKey(in) || ! nodes.containsKey(out)) {
			throw new RuntimeException("connection nodes does not exist");
		}
		
		if(inConnections.containsKey(out) && inConnections.get(out).contains(connection)) {
			if(!outConnections.containsKey(in) || !outConnections.get(in).contains(connection)) {
				throw new RuntimeException("connections not in sync");
			}
			return;
		}
		
		
		if(outConnections.containsKey(in) && outConnections.get(in).contains(connection)) {
			if(!inConnections.containsKey(out) || !inConnections.get(out).contains(connection)) {
				throw new RuntimeException("connections not in sync");
			}
			return;
		}
		
		if(!inConnections.containsKey(out)) {
			inConnections.put(out, new ArrayList<>());
		}
		
		if(!outConnections.containsKey(in)) {
			outConnections.put(in, new ArrayList<>());
		}
		
		inConnections.get(out).add(connection);
		outConnections.get(in).add(connection);
	}
	
	public void removeConnection(Connection connection) {
		int in = connection.getIn();
		int out = connection.getOut();
		if(!nodes.containsKey(in) || ! nodes.containsKey(out)) {
			throw new RuntimeException("connection nodes does not exist");
		}
		
		if((inConnections.containsKey(out) && !outConnections.containsKey(in)) || (!inConnections.containsKey(out) && outConnections.containsKey(in))) {
			throw new RuntimeException("connections not in sync");
		}
		
		List<Connection> outCons = inConnections.get(out);
		outCons.remove(connection);
		if(outCons.isEmpty()) {
			inConnections.remove(out);
		}
		
		List<Connection> inCons = outConnections.get(in);
		inCons.remove(connection);
		if(inCons.isEmpty()) {
			outConnections.remove(in);
		}
	}
	
	public void removeNode(Node node) {
		if(!nodes.containsKey(node.getId())) {
			throw new RuntimeException("Node does not exists");
		}
		if(node.getType() == NodeType.HIDDEN) {
			hiddenNodes.remove(node);
		}
		else {
			throw new RuntimeException("removing input or output node");
		}
		
		List<Connection> inCons = inConnections.get(node.getId());
		if(inCons != null) {
			for(Connection con : inCons) {
				int in = con.getIn();
				List<Connection> outCons = outConnections.get(in);
				Iterator<Connection> it = outCons.iterator();
				while(it.hasNext()) {
					Connection inCon = it.next();
					if(inCon.getOut() == node.getId()) {
						it.remove();
					}
				}
				
				if(outCons.isEmpty()) {
					outConnections.remove(in);
				}
			}
			
			inConnections.remove(node.getId());
		}
		
		
		List<Connection> outCons = outConnections.get(node.getId());
		if(outCons != null) {
			for(Connection con : outCons) {
				int out = con.getOut();
				List<Connection> inCons2 = inConnections.get(out);
				Iterator<Connection> it = inCons2.iterator();
				while(it.hasNext()) {
					Connection outCon = it.next();
					if(outCon.getIn() == node.getId()) {
						it.remove();
					}
				}
				
				if(inCons2.isEmpty()) {
					inConnections.remove(out);
				}
			}
			
			outConnections.remove(node.getId());
		}
		
		
		nodes.remove(node.getId());
		
	}
	
	public static Genome generateFullyConnected(int inputSize,int outputSize) {
		Genome genome = new Genome(inputSize, outputSize);
		for(Node inNode : genome.getInputNodes()) {
			for(Node outNode : genome.getOutputNodes()) {
				genome.addConnection(Connection.getConnection(inNode.getId(), outNode.getId(), ThreadLocalRandom.current().nextDouble(-1.0, 1.0), true));
			}
		}
		
		return genome;
	}
	
	public static Genome generateRandomBaseGenome(int inputSize,int outputSize) {
		Genome genome = new Genome(inputSize, outputSize);
		for(Node inNode : genome.getInputNodes()) {
			for(Node outNode : genome.getOutputNodes()) {
				if(ThreadLocalRandom.current().nextBoolean()) {
					genome.addConnection(Connection.getConnection(inNode.getId(), outNode.getId(), ThreadLocalRandom.current().nextDouble(-1.0, 1.0), true));
				}
				
			}
		}
		
		return genome;
	}
	
	/*public boolean isSameSpecie(Genome g) {
		if(inConnections.isEmpty()) {
			if(!g.getInConnections().isEmpty()) {
				return Config.c1 * g.getInConnections().size() + Config.c2 * 0 + Config.c3 * 0 <= Config.compatibilityThreshold;
			}
		}
		else {
			if(g.getInConnections().isEmpty()) {
				return Config.c1 * inConnections.size() + Config.c2 * 0 + Config.c3 * 0 <= Config.compatibilityThreshold;
			}
		}
	}*/
	
	
	
	private void calculateNodeOutput(Node node) {
		if(getInConnections().containsKey(node.getId())) {
			Double sum = 0.0;
			for(Connection con : getInConnections().get(node.getId())) {
				if(con.isEnabled()) {
					sum += nodes.get(con.getIn()).getValue() * con.getWeight();
				}
			}
			
			node.setValue(Activation.activations.get(node.getActivation()).apply(sum));
		}
		

	}
	
	public Double[] performAction(JSONArray state) {
		Double[] obs = GymJavaHttpClient.flattenObservation(state);
		
		nodes.values().stream().forEach(n->n.setValue(0));
		int iterations = hiddenNodes.size() + outputNodes.size();
		if(hiddenNodes.size() == 0) {
			iterations = 1;
		}
		Double[] output = new Double[outputNodes.size()];
		for(int i = 0;i<iterations;i++) {
			for(int j = 0;j<inputNodes.size();j++) {
				inputNodes.get(j).setValue(obs[j]);
			}
			
			for(int j = 0;j<hiddenNodes.size();j++) {
				calculateNodeOutput(hiddenNodes.get(j));
			}
			
			for(int j = 0;j<outputNodes.size();j++) {
				calculateNodeOutput(outputNodes.get(j));
				output[j] = outputNodes.get(j).getValue();
			}
		}
		return output;
	}
	
	
	public boolean isSimilar(Genome g) {
		if(this == g) {
			return true;
		}
		int numOfMatching = 0;
		double weightsDiff = 0.0;
		int maxInnovation1 = 0;
		List<Connection> cons1 = new ArrayList<>();
		for(List<Connection> cons : inConnections.values()) {
			cons1.addAll(cons);
			for(Connection con : cons) {
				if(maxInnovation1 < con.getInnovation()) {
					maxInnovation1 = con.getInnovation();
				}
			}
		}
		List<Connection> cons2 = new ArrayList<>();
		int maxInnovation2 = 0;
		for(List<Connection> cons : g.getInConnections().values()) {
			cons2.addAll(cons);
			for(Connection con : cons) {
				if(maxInnovation2 < con.getInnovation()) {
					maxInnovation2 = con.getInnovation();
				}
			}
		}
		if(cons1.isEmpty() && cons2.isEmpty()) {
			return true;
		}
		if(cons2.size() > cons1.size()) {
			List<Connection> temp = cons1;
			cons1 = cons2;
			cons2 = temp;
		}
		for(Connection con1 : cons1) {
			for(Connection con2 : cons2) {
				if(con1.getInnovation() == con2.getInnovation()) {
					numOfMatching++;
					weightsDiff += Math.abs(con1.getWeight() - con2.getWeight());
				}
			}
		}
		int n = Math.max(cons1.size(),cons2.size());
		if(n == 0) {
			n = 1;
		}
		double averageWeightDiff = numOfMatching == 0 ? 100 : weightsDiff / numOfMatching;
		
		return (Config.c1 * (cons1.size() + cons2.size() - 2 * numOfMatching)) / n + (Config.c2 * averageWeightDiff) < Config.compatibilityThreshold;
	}
	
	public static void save(int generation,Genome g,String name) {
		File file = new File("./genomes/" + generation + "/" + name);
		if(!file.exists()) {
			if(!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		try(FileOutputStream f = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(f);) {
			o.writeObject(g);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		File jsonData = new File("./genomes/" + generation + "/" + name + "GraphData.json");
		JSONObject json = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray connections = new JSONArray();
		
		for(Node node : g.getNodes().values()) {
			if(node.getType() == NodeType.HIDDEN) {
				nodes.put(node.getId()  + " , " + node.getValue());
			}
			else if(node.getType() == NodeType.INPUT) {
				nodes.put("input-" + node.getId()  + " , " + node.getValue());
			}
			else {
				nodes.put("output-" + node.getId()  + " , " + node.getValue());
			}
		}
		
		Set<Connection> allCons = new HashSet<>();
		for(List<Connection> inCons : g.getInConnections().values()) {
			allCons.addAll(inCons);
		}
		
		Iterator<Connection> it = allCons.iterator();
		while(it.hasNext()) {
			Connection con = it.next();
			if(!con.isEnabled()) {
				continue;
			}
			JSONObject conObj = new JSONObject();
			String fromName = "";
			Node from = g.getNodes().get(con.getIn());
			
			
			if(from.getType() == NodeType.HIDDEN) {
				fromName = from.getId()  + " , " + from.getValue();
			}
			else if(from.getType() == NodeType.OUTPUT) {
				fromName = "output-" + from.getId() + " , " + from.getValue();
			}
			else {
				fromName = "input-" + from.getId() + " , " + from.getValue();
			}
			Node to = g.getNodes().get(con.getOut());
			String toName = "";
			if(to.getType() == NodeType.HIDDEN) {
				toName = to.getId()  + " , " + to.getValue();
			}
			else if(to.getType() == NodeType.OUTPUT) {
				toName = "output-" + to.getId() + " , " + to.getValue();
			}
			else {
				toName = "input-" + to.getId() + " , " + to.getValue();
			}
			conObj.put("from", fromName);
			conObj.put("to", toName);
			conObj.put("innov", con.getInnovation());
			connections.put(conObj);
		}
		
		json.put("nodes", nodes);
		json.put("connections", connections);
		
		try {
			Files.write(json.toString().getBytes(StandardCharsets.UTF_8), jsonData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static Genome read(int generation) {
		try(FileInputStream f = new FileInputStream(new File("genomes/" + generation + "/best"));
				ObjectInputStream in = new ObjectInputStream(f);) {
			return (Genome)in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
		
	}
	
	public void evaluateFitness(String envName,boolean render,int numOfAttempts,boolean monitor) {
		String envId = GymJavaHttpClient.createEnv(envName);
		if(monitor) {
			GymJavaHttpClient.startMonitor(envId, true, false,"./movies/" + Config.generation);
		}
		
		float totalFitness = 0;
		
		for(int i = 0;i<numOfAttempts;i++) {
			JSONArray obs = GymJavaHttpClient.resetEnv(envId);
			boolean done = false;
			int steps = 0;
			while(!done && steps < Config.maxSteps) {
				Double[] action = performAction(obs);
				StepObject actionResult = GymJavaHttpClient.stepEnv(envId, action, render);
				done = actionResult.done;
				obs = actionResult.observation;
				totalFitness += actionResult.reward;
				steps++;
			}
		}
		
		if(monitor) {
			GymJavaHttpClient.closeMonitor(envId);
		}
		GymJavaHttpClient.closeEnv(envId);
		fitness = totalFitness / Config.numOfAttempts;
		
	}
}
