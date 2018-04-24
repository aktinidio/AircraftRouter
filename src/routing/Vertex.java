package routing;

public class Vertex {
	private final int id;
	private int cost[];

	public Vertex(int id, int[] cost) {
		this.id = id;
		this.cost = cost;
	}
	
	public Vertex(int id) {
		this.id = id;
		cost = null;
	}
	
	public int getId() {
		return id;
	}
	
	public void setConnectionsSize(int size) {
		cost = new int[size];
	}
	
	public double getConnectionCost(int vertexID) {
		if (cost == null)
			return Integer.MAX_VALUE;
		return cost[vertexID];
	}
	
	public void setConnectionCost(int id, int cost) {
		this.cost[id] = cost;
	}
}