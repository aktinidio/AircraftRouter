package routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class QPPTWarrivalFix {

	private boolean flip;
	private Label labelN, label;
	private static final double SAFETY_TIME = 0.01;
	private final List<Edge> edges;
	private final List<Vertex> nodes;
	private final Integer[] destNode;
	private FibonacciHeap<Label> fib;
	private ArrayList<Route> routes = new ArrayList<Route>();
	private ArrayList<Label> labelList;
	private ArrayList<FibonacciHeap.Entry<Label>> entries;
	private int pushbackTime;
	
	public QPPTWarrivalFix(List<Edge> e, List<Vertex> v, Integer[] d) {
		edges = e;
		nodes = v;
		destNode = d;
		setConflEdges();
	}

	public void execute(List<Flight> flights) {

		routes = new ArrayList<Route>();
		for (int i = 0; i < flights.size(); i++) {
			runQpptw(flights.get(i));
			readjustWindows(i, flights.get(i).isArrival());
			routes.get(i).sortReservations();
		}
	}
	
	private void setConflEdges() {
		for (Edge e1 : edges)
			for (Edge e2 : edges)
				if (e1 != e2)
					if (e1.getDestination() == e2.getDestination() || e1.getDestination() == e2.getSource())
						e1.addConflOutEdge(e2);
					else if (e1.getSource() == e2.getDestination() || e1.getSource() == e2.getSource())
						e1.addConflInEdge(e2);
	}
	
	private void runQpptw(Flight f) {
		
		boolean arrival = f.isArrival();
		int aircraftID = f.getId(), sourcePos = arrival ? f.getSource() : f.getTarget(), targetPos = arrival ? f.getTarget() : f.getSource();
		double time = f.getTime();
		pushbackTime = f.getPushbackTime();
		
		routes.add(new Route(aircraftID, sourcePos, targetPos, time, arrival, pushbackTime, f.getMinimumRoutingTime()));

		boolean insert;
		double timeIn, timeOut;
		Vertex node;
		
		PriorityQueue<Label> heap =  new PriorityQueue<Label>();
		labelList = new ArrayList<Label>();
		
		if(arrival) {
			label = new Label(sourcePos, time + f.getMinimumRoutingTime(), time, Double.MAX_VALUE, aircraftID, -1, -1, false);				
			heap.add(label);
		} else {
			label = new Label(sourcePos, -time + f.getMinimumRoutingTime(), 0, time, aircraftID, -1, -1, false);
			heap.add(label);
		}
		labelList.add(label);
		
		while (!heap.isEmpty()) {							
			label = heap.poll();
			if (label.labelNodePos == targetPos) {
				reconstructRoute(label, aircraftID, arrival);
				return;
			}
			for (Edge edge : edges) {
				if (isOutgoingEdgeOfVertex(edge, nodes.get(label.labelNodePos), label.predNodePos > -1 ? nodes.get(label.predNodePos) : null)) {
					for (int j = 0; j < edge.getWindows().size(); j++) {
						if (edge.getWindows().get(j).getStartTime() > label.interval[1]) 
							break;
						if (edge.getWindows().get(j).getEndTime() >= label.interval[0]) {

							if (arrival) {
								timeIn = Math.max(label.interval[0], edge.getWindows().get(j).getStartTime());
								timeOut = timeIn + edge.getWeight();

								if (timeOut <= edge.getWindows().get(j).getEndTime()) {
									node = flip ? edge.getSource() : edge.getDestination();
									labelN = new Label(node.getId(), timeOut, edge.getWindows().get(j).getEndTime(), aircraftID, label.labelNodePos, labelList.indexOf(label), flip);
									insert = true;

									for (int k = 0; k < labelList.size(); k++) {
										if (dominates(labelList.get(k), labelN)) {
											insert = false;
											break;
										}
										if (dominates(labelN, labelList.get(k)))
											heap.remove(labelList.get(k));
									} 
									if (insert) {
										labelN.setCost(labelN.interval[0] + (labelN.labelNodePos == targetPos ? 0 : node.getConnectionCost(Arrays.asList(destNode).indexOf(targetPos))));
										heap.add(labelN);
										labelList.add(labelN);
									}
								}
							} else {
								node = flip ? edge.getSource() : edge.getDestination();
								timeOut = Math.min(label.interval[1], edge.getWindows().get(j).getEndTime());
								timeIn = timeOut - edge.getWeight() - (node.getId() == targetPos ? pushbackTime : 0);
								if (timeIn >= edge.getWindows().get(j).getStartTime()) {
									
									labelN = new Label(node.getId(), edge.getWindows().get(j).getStartTime(), timeIn, aircraftID, label.labelNodePos, labelList.indexOf(label), flip);
									insert = true;

									for (int k = 0; k < labelList.size(); k++) {
										if (dominates(labelList.get(k), labelN)) { 
											insert = false;
											break;
										}
										if (dominates(labelN, labelList.get(k)))
											heap.remove(labelList.get(k));	
									}
									if (insert) {
										labelN.setCost(-labelN.interval[1] + (labelN.labelNodePos == targetPos ? -pushbackTime : node.getConnectionCost(Arrays.asList(destNode).indexOf(targetPos))));
										heap.add(labelN);
										labelList.add(labelN); 
									}
								}
							}
						}
					}
				}
			}
		}
		System.err.println("There is no s-t route... :/ ");
		return;
	}
	
	private void reconstructRoute(Label label, int aircraftID, boolean arrival) {

		int labelNodePos = label.labelNodePos, predNodePos = label.predNodePos, labelsPos = labelList.indexOf(label), firstNodePos = labelNodePos;
		double beginTime = Double.MAX_VALUE, endTime = Double.MAX_VALUE;

		double extraTime = pushbackTime;
		if (arrival)
			while (labelsPos != 0) {

				Edge edge = edgeByVertexPos(predNodePos, labelNodePos);

				if (labelNodePos == label.labelNodePos) {
					endTime = labelList.get(labelsPos).interval[0];
					beginTime = Math.min(endTime - edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[1]);
				} else {
					endTime = beginTime;
					beginTime = Math.min(labelList.get(labelsPos).interval[0] - edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[1]);
				}

				routes.get(aircraftID).addReservation(new Reservation(edge, new double[] { beginTime, endTime }, aircraftID, labelList.get(labelsPos).backwards), arrival);
				edge.fixWindowsByReservation(beginTime, endTime, false);

				labelsPos = label.labelListPredPos;
				label = labelList.get(labelsPos);
				labelNodePos = label.labelNodePos;
				predNodePos = label.predNodePos;
			}
		else
			while (labelsPos != 0) {

				Edge edge = edgeByVertexPos(predNodePos, labelNodePos);

				if (labelNodePos == label.labelNodePos) {
					beginTime = labelList.get(labelsPos).interval[1];
					endTime = Math.max(beginTime + edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[0]);
				} else {
					beginTime = endTime;
					endTime = Math.max(labelList.get(labelsPos).interval[1] + edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[0]);
				}

				if (labelNodePos == firstNodePos)
					endTime += extraTime;

				routes.get(aircraftID).addReservation(new Reservation(edge, new double[] { beginTime, endTime }, aircraftID, labelList.get(labelsPos).backwards), arrival);
				edge.fixWindowsByReservation(beginTime, endTime, false);

				labelsPos = label.labelListPredPos;
				label = labelList.get(labelsPos);
				labelNodePos = label.labelNodePos;
				predNodePos = label.predNodePos;
			}
	}

	private void readjustWindows(int aircraftID, boolean arrival) {

		for (Reservation reservation : routes.get(aircraftID).getReservations()) {
			double inTime = reservation.getStartTime(), outTime = reservation.getEndTime() + SAFETY_TIME;

			for (Edge edge : reservation.getEdge().getConflEdges(reservation.isBackwards(), arrival)) {
				edge.addReservation(new Reservation(edge, new double[] { inTime, outTime }, aircraftID, reservation.isBackwards()));
				for (int j = 0; j < edge.getWindows().size(); j++) {
					if (outTime <= edge.getWindows().get(j).getStartTime())
						break;
					if (inTime < edge.getWindows().get(j).getEndTime()) {
						if (inTime < edge.getWindows().get(j).getStartTime() + SAFETY_TIME) {
							if (edge.getWindows().get(j).getEndTime() - SAFETY_TIME < outTime)
								edge.deleteWindow(j--);
							else
								edge.getWindows().get(j).setStartTime(outTime);
						} else {
							if (edge.getWindows().get(j).getEndTime() - SAFETY_TIME < outTime)
								edge.getWindows().get(j).setEndTime(inTime);
							else {
								edge.insertWindow(edge.getWindows().get(j).getStartTime(), inTime);
								edge.getWindows().get(j).setStartTime(outTime);
							}
						}
					}
					edge.sortWindows(false);
				}
			}
		}
	}
	
	public int[] runSimpleQpptw(int sourcePos, int targetPos, int[][] nodesIDs) {
		
		Edge[] blockedEdge = new Edge[nodesIDs.length];
		for (int i = 0; i< nodesIDs.length; i++)
			blockedEdge[i] = edgeByVertexPos(nodesIDs[i][0], nodesIDs[i][1]);
		
		boolean insert;
		int pushbackT = 0;
		double timeIn, timeOut, time = 0;
		boolean arrival = true;

		fib = new FibonacciHeap<Label>();
		labelList = new ArrayList<Label>();
		entries = new ArrayList<FibonacciHeap.Entry<Label>>();

		if (arrival) {
			label = new Label(sourcePos, time, Double.MAX_VALUE, 0, -1, -1, false);
			entries.add(fib.enqueue(label, time));
		} else {
			label = new Label(sourcePos, 0, time, 0, -1, -1, false);
			entries.add(fib.enqueue(label, -time));
		}
		labelList.add(label);

		while (!fib.isEmpty()) {
			label = fib.dequeueMin().getValue();

			if (label.labelNodePos == targetPos)
				return getPath(reconstructSimpleRoute(label, time, arrival, pushbackT));
			outerloop:
			for (Edge edge : edges) {
				for (Edge blocked : blockedEdge) 
					if (edge == blocked) 
						continue outerloop;
				if (isOutgoingEdgeOfVertex(edge, nodes.get(label.labelNodePos), label.predNodePos > -1 ? nodes.get(label.predNodePos) : null)) {
					for (int j = 0; j < edge.getTempWindows().size(); j++) {
						if (edge.getTempWindows().get(j).getStartTime() > label.interval[1])
							break;
						if (edge.getTempWindows().get(j).getEndTime() >= label.interval[0]) {
							if (arrival) {
								timeIn = Math.max(label.interval[0], edge.getTempWindows().get(j).getStartTime());
								timeOut = timeIn + edge.getWeight();

								if (timeOut <= edge.getTempWindows().get(j).getEndTime()) {

									labelN = new Label((flip ? edge.getSource() : edge.getDestination()).getId(), timeOut, edge.getTempWindows().get(j).getEndTime(), 0, label.labelNodePos, labelList.indexOf(label), flip);
									insert = true;

									for (int k = 0; k < labelList.size(); k++) {
										if (dominates(labelList.get(k), labelN)) {
											insert = false;
											break;
										}
										if (dominates(labelN, labelList.get(k)))
											fib.delete(entries.get(k));
									}
									if (insert) {
										entries.add(fib.enqueue(labelN, labelN.interval[0]));
										labelList.add(labelN);
									}
								}
							} else {
								timeOut = Math.min(label.interval[1], edge.getTempWindows().get(j).getEndTime());
								timeIn = timeOut - edge.getWeight() - ((flip ? edge.getSource() : edge.getDestination()).getId() == targetPos ? pushbackTime : 0);
								if (timeIn >= edge.getTempWindows().get(j).getStartTime()) {

									labelN = new Label((flip ? edge.getSource() : edge.getDestination()).getId(), edge.getTempWindows().get(j).getStartTime(), timeIn, 0, label.labelNodePos, labelList.indexOf(label), flip);
									insert = true;

									for (int k = 0; k < labelList.size(); k++) {
										if (dominates(labelList.get(k), labelN)) {
											insert = false;
											break;
										}
										if (dominates(labelN, labelList.get(k)))
											fib.delete(entries.get(k));
									}
									if (insert) {
										entries.add(fib.enqueue(labelN, -labelN.interval[1]));
										labelList.add(labelN);
									}
								}
							}
						}
					}
				}
			}
		}
		System.err.println("There is no s-t route... :/ ");
		return null;
	}
	
	private Route reconstructSimpleRoute(Label label, double time, boolean arrival, int pushbackT) {

		int labelNodePos = label.labelNodePos, predNodePos = label.predNodePos, labelsPos = labelList.indexOf(label), firstNodePos = labelNodePos;
		double beginTime = Double.MAX_VALUE, endTime = Double.MAX_VALUE;
		Route r = new Route(0, 0, 0, time, arrival, pushbackT, 0);
		double extraTime = pushbackT;

		if (arrival)
			while (labelsPos != 0) {
				Edge edge = edgeByVertexPos(predNodePos, labelNodePos);
				if (labelNodePos == label.labelNodePos) {
					endTime = labelList.get(labelsPos).interval[0];
					beginTime = Math.min(endTime - edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[1]);
				} else {
					endTime = beginTime;
					beginTime = Math.min(labelList.get(labelsPos).interval[0] - edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[1]);
				}
				r.addSimpleReservation(new Reservation(edge, new double[] { beginTime, endTime }, 0, labelList.get(labelsPos).backwards));

				labelsPos = label.labelListPredPos;
				label = labelList.get(labelsPos);
				labelNodePos = label.labelNodePos;
				predNodePos = label.predNodePos;
			}
		else
			while (labelsPos != 0) {

				Edge edge = edgeByVertexPos(predNodePos, labelNodePos);

				if (labelNodePos == label.labelNodePos) {
					beginTime = labelList.get(labelsPos).interval[1];
					endTime = Math.max(beginTime + edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[0]);
				} else {
					beginTime = endTime;
					endTime = Math.max(labelList.get(labelsPos).interval[1] + edge.getWeight(), labelList.get(labelList.get(labelsPos).labelListPredPos).interval[0]);
				}
				if (labelNodePos == firstNodePos)
					endTime += extraTime;

				r.addSimpleReservation(new Reservation(edge, new double[] { beginTime, endTime }, 0, labelList.get(labelsPos).backwards));

				labelsPos = label.labelListPredPos;
				label = labelList.get(labelsPos);
				labelNodePos = label.labelNodePos;
				predNodePos = label.predNodePos;
			}
		return r;
	}
	
	public double getTotalRoutingTime() {
		
		double totalDelay = 0;		
		for (Route r : routes) {
			double total = r.getTotalTime();
			double min = r.getMinimumRouting();
			double delay = Math.round((total - min - (r.isArrival() ? 0 : r.getPushbackTime())) * 1000) / 1000;
			totalDelay += delay;
		}
		return totalDelay;
	}
	
	private boolean isOutgoingEdgeOfVertex(Edge e, Vertex v1, Vertex v2) {

		if (e.getSource() == v1 && e.getDestination() != v2)
			flip = false;
		else if (e.getDestination() == v1 && e.getSource() != v2)
			flip = true;
		else
			return false;
		return true;
	}

	private boolean dominates(Label higher, Label lower) {
		if (lower.activ)
			if (higher.labelNodePos == lower.labelNodePos)
				if (higher.interval[0] <= lower.interval[0])
					if (higher.interval[1] >= lower.interval[1]) {
						lower.activ = false;
						return true;
					}
		return false;
	}

	private Edge edgeByVertexPos(int vertex1, int vertex2) {
		for (Edge edge : edges)
			if (edge.getSource().getId() == vertex1 && edge.getDestination().getId() == vertex2 || edge.getSource().getId() == vertex2 && edge.getDestination().getId() == vertex1)
				return edge;
		return null;
	}

	private int[] getPath(Route route) {
		List<Reservation> a = route.getReservations();
		int[] path = new int[a.size()];
		Collections.reverse(a);

		for (int i = 0; i < a.size(); i++)
			path[i] = (a.get(i).isBackwards() ? a.get(i).getEdge().getSource().getId() : a.get(i).getEdge().getDestination().getId());

		Reservation l = a.get(a.size() - 1);
		path[a.size() - 1] = ((!l.isBackwards()) ? l.getEdge().getSource().getId() : l.getEdge().getDestination().getId());

		return path;
	}
}
