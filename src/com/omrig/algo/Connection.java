package com.omrig.algo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

public class Connection implements Comparable<Connection>,Serializable {

	private static final long serialVersionUID = -8267953619660943213L;
	@Getter
	private int in;
	@Getter
	private int out;
	@Getter
	private int innovation;
	@Getter
	@Setter
	private double weight;
	@Getter
	@Setter
	private boolean enabled;
	private static AtomicInteger innovationCounter = new AtomicInteger(0);
	private static Map<String,Integer> nodesToInnovation = new HashMap<>();
	private static Map<Integer,String> innovationToNodes = new HashMap<>();
	
	
	private Connection(int in,int out,int innovation,double weight,boolean enabled) {
		this.in = in;
		this.out = out;
		this.innovation = innovation;
		this.weight = weight;
		this.enabled = enabled;
	}
	
	public Connection cloneConnection() {
		Connection con = new Connection(in,out,this.innovation,this.weight,this.enabled);
		return con;
	}
	
	public static Integer getMiddleNodeId(int in,int out) {
		for(String key1 : nodesToInnovation.keySet()) {
			String[] split1 = key1.split("_");
			int in1 = Integer.parseInt(split1[0]);
			int out1 = Integer.parseInt(split1[1]);
			if(in1 == in) {
				for(String key2 : nodesToInnovation.keySet()) {
					String[] split2 = key2.split("_");
					int in2 = Integer.parseInt(split2[0]);
					int out2 = Integer.parseInt(split2[1]);
					if(out2 == out && out1 == in2) {
						return out1; 
					}
				}
			}
			
		}
		
		return null;
	}
	
	private static Connection createNewConnection(int in,int out,double weight,boolean enabled) {
		String key = in + "_" + out;
		Connection con = new Connection(in,out,innovationCounter.getAndIncrement(),weight,enabled);
		nodesToInnovation.put(key, con.getInnovation());
		innovationToNodes.put(con.getInnovation(), key);
		return con;
	}
	
	public static Connection getConnection(int in,int out,double weight,boolean enabled) {
		Integer innov = nodesToInnovation.get(in + "_" + out);
		if(innov == null) {
			return createNewConnection(in, out, weight, enabled);
		}
		
		return new Connection(in,out,innov,weight,enabled);
	}
	
	@Override
	public int compareTo(Connection o) {
		if(this.getInnovation() > o.getInnovation()) {
			return 1;
		}
		if(this.getInnovation() < o.getInnovation()) {
			return -1;
		}
		
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Connection)) {
			return false;
		}
		return innovation == ((Connection)o).getInnovation();
	}
}
