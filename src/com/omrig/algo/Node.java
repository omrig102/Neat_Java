package com.omrig.algo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

@Data
public class Node implements Serializable {


	private static final long serialVersionUID = 5283279117383096061L;
	private NodeType type;
	private int id;
	private double value;
	private String activation;
	public static AtomicInteger nodeCounter = new AtomicInteger(0);
	private static Map<Integer,NodeType> idToNodeType = new HashMap<>();
	
	private Node(int id,NodeType type,String activation) {
		this.id = id;
		this.type = type;
		this.activation = activation;
		idToNodeType.put(id, type);
	}
	
	public static Node createExistingNodeById(int id,String activation) {
		NodeType nodeType = idToNodeType.get(id);
		if(nodeType == null) {
			return null;
		}
		return new Node(id,nodeType,activation);
		
	}
	
	public static Node createNewNode(NodeType type,String activation) {
		Node node = new Node(nodeCounter.getAndIncrement(),type,activation);
		idToNodeType.put(node.getId(), type);
		return node;
	}
	
	public static NodeType getNodeTypeById(int id) {
		return idToNodeType.get(id);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Node)) {
			return false;
		}
		
		return this.id == ((Node)o).getId();
	}
	
	public Node cloneNode() {
		Node newNode = new Node();
		newNode.setType(type);
		newNode.setActivation(activation);
		newNode.setId(id);
		
		return newNode;
	}
	
	
}
