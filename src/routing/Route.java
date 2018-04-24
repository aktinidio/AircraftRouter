package routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Route {

	private ArrayList<Reservation> route = new ArrayList<Reservation>();
	private int id, startPos, endPos, pushback;
	private double assignedTime, latestEndTime = 0, minimumRoutingTime;
	private boolean arrival;

	public Route(int aircraftID, int source, int destination, double time, boolean arrival, int pushbackTime, double minimum) {
		id = aircraftID;
		startPos = source;
		endPos = destination;
		assignedTime = time;
		pushback = pushbackTime;
		this.arrival = arrival;
		minimumRoutingTime = minimum;
	}

	public int getID() {
		return id;
	}

	public void addReservation(Reservation r, boolean arrival) {
		route.add(r);
		r.getEdge().addReservation(r);
	}

	public void addSimpleReservation(Reservation r) {
		route.add(r);
	}

	public ArrayList<Reservation> getReservations() {
		return route;
	}

	public Reservation getReservation(int reservationNo) {
		return route.get(reservationNo);
	}
	
	public double getTotalTime() {
		//return route.get(0).getEndTime() - assignedTime; //route.get(route.size() - 1).getStartTime();
		if (arrival)
			return getEndTime() - assignedTime;
		else
			return assignedTime - getStartTime();
	}

	public double getTotalTimeRev() {
		return route.get(route.size() - 1).getEndTime() - route.get(0).getStartTime();
	}

	public int getSourcePos() {
		return startPos;
	}

	public int getDestinationPos() {
		return endPos;
	}

	public double getEndTime() {
		return (route.get(0).getEndTime() >  route.get(route.size() - 1).getEndTime() ? route.get(0).getEndTime() : route.get(route.size() - 1).getEndTime());
	}

	public double getStartTime() {
		return (route.get(route.size() - 1).getStartTime() < route.get(0).getStartTime() ? route.get(route.size() - 1).getStartTime() : route.get(0).getStartTime());
	}

	public boolean isArrival() {
		return arrival;
	}
	
	public double getMinimumRouting() {
		return minimumRoutingTime;
	}
	
	public void updateLatestTime(double time) {
		if (time > latestEndTime)
			latestEndTime = time;
	}
	
	public double getLatestTime() {
		return getEndTime() > latestEndTime ? getEndTime() : latestEndTime;
	}
	
	public int getPushbackTime() {
		return pushback;
	}

	public int size() {
		return route.size();
	}

	public void sortReservations() {
		Collections.sort(route, new Comparator<Reservation>() {
			@Override
			public int compare(Reservation r1, Reservation r2) {
				return Double.compare(r2.getStartTime(), r1.getStartTime());
			}
		});
	}
	
	public void sortIncReservations() {
		Collections.sort(route, new Comparator<Reservation>() {
			@Override
			public int compare(Reservation r1, Reservation r2) {
				return Double.compare(r1.getStartTime(), r2.getStartTime());
			}
		});
	}
}
