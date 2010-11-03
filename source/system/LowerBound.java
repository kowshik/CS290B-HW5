/**
 * 
 */
package system;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import tasks.TspTask.City;

/**
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class LowerBound implements Cloneable, Serializable {
	
	private static final long serialVersionUID = -1230131808278869416L;
	private Map<City, Pair<Edge>> edgeMap;

	private enum EdgeType {
		REAL, VIRTUAL
	};

	public String toString() {
		return "Lower bound : " + getLowerBoundValue() + "\nMap : "
				+ edgeMap.toString();
	}

	public double getLowerBoundValue() {
		double lowerBound = 0.0d;
		for (Entry<City, Pair<Edge>> e : edgeMap.entrySet()) {
			Pair<Edge> p = e.getValue();
			if (p != null) {
				if (p.getFirst() != null) {
					lowerBound += p.getFirst().getEdgeLength();
				}
				if (p.getSecond() != null) {
					lowerBound += p.getSecond().getEdgeLength();
				}
			}

		}
		return lowerBound / 2.0d;
	}

	public void addRealEdge(City rEdgeStart, City rEdgeEnd) {
		if(rEdgeStart == null ||rEdgeEnd == null|| rEdgeStart.equals(rEdgeEnd)){
			return;
		}
		Edge realEdge = new Edge(rEdgeStart, rEdgeEnd);
		Pair<Edge> rEdgeStartPair = this.edgeMap.get(rEdgeStart);
		Pair<Edge> rEdgeEndPair = this.edgeMap.get(rEdgeEnd);
		if(rEdgeStartPair.contains(realEdge) || rEdgeEndPair.contains(realEdge)){
			return;
		}
		addRealEdge(rEdgeStartPair, realEdge);
		addRealEdge(rEdgeEndPair, realEdge);
	}

	public void addRealEdge(Pair<Edge> p, Edge realEdge) {
		int edgeToReplace = 1;
		Edge firstEdge = p.getFirst();
		Edge secondEdge = p.getFirst();
		if (firstEdge.getEdgeType() == EdgeType.VIRTUAL
				&& secondEdge.getEdgeType() == EdgeType.VIRTUAL) {
			if (firstEdge.getEdgeLength() < secondEdge.getEdgeLength()) {
				edgeToReplace = 2;
			}
		}

		else if (secondEdge.getEdgeType() == EdgeType.VIRTUAL) {
			edgeToReplace = 2;
		}
		switch (edgeToReplace) {
		case 1:
			p.setFirst(realEdge);
			break;
		case 2:
			p.setSecond(realEdge);
			break;
		}
	}

	public LowerBound(List<City> citiesList) {
		edgeMap = new LinkedHashMap<City, Pair<Edge>>();
		List<Edge> listOfEdges = new Vector<Edge>();
		for (City aStartCity : citiesList) {
			listOfEdges.clear();
			for (City aEndCity : citiesList) {
				if (!aStartCity.equals(aEndCity)) {
					Edge e = new Edge(aStartCity, aEndCity);
					listOfEdges.add(e);
				}
			}
			Collections.sort(listOfEdges, new MinEdgeComparator());

			Edge firstMinEdge = null;
			if (listOfEdges.size() >= 1) {
				firstMinEdge = listOfEdges.get(0);
			}
			Edge secondMinEdge = null;
			if (listOfEdges.size() >= 2) {
				secondMinEdge = listOfEdges.get(1);
			}
			Pair<Edge> edgePair = new Pair<Edge>(firstMinEdge, secondMinEdge);
			edgeMap.put(aStartCity, edgePair);
		}
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static class Edge  implements Serializable {
		
		private static final long serialVersionUID = -534808833417374784L;
		private City startCity;

		public City getStartCity() {
			return startCity;
		}

		public void setStartCity(City startCity) {
			this.startCity = startCity;
			this.edgeLength = computeDistance();
		}

		private City endCity;

		public City getEndCity() {
			return endCity;
		}

		public void setEndCity(City endCity) {
			this.endCity = endCity;
			this.edgeLength = computeDistance();
		}

		private double edgeLength;

		public double getEdgeLength() {
			return edgeLength;
		}

		private EdgeType edgeType;

		public EdgeType getEdgeType() {
			return edgeType;
		}

		public void setEdgeType(EdgeType edgeType) {
			this.edgeType = edgeType;
		}

		public Edge(City start, City end) {
			this.startCity = start;
			this.endCity = end;
			this.edgeLength = computeDistance();
			this.edgeType = EdgeType.VIRTUAL;
		}

		private double computeDistance() {
			return Point2D.distance(startCity.getX(), startCity.getY(),
					endCity.getX(), endCity.getY());
		}

		public String toString() {
			return edgeType + " [ " + startCity + " -> " + endCity + " ] = "
					+ this.edgeLength;
		}
		
		public boolean equals(Object o){
			Edge e=(Edge)o;
			if(this.startCity.equals(e.getEndCity()) && this.endCity.equals(e.getEndCity()))
				return true;
			return false;
		}

	}

	private static class MinEdgeComparator implements Comparator<Edge>, Serializable {

		private static final long serialVersionUID = 2140916688517970507L;

		public MinEdgeComparator() {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Edge edgeA, Edge edgeB) {

			if (edgeA.getEdgeLength() < edgeB.getEdgeLength()) {
				return -1;
			} else if (edgeB.getEdgeLength() < edgeA.getEdgeLength()) {
				return 1;
			}
			return 0;
		}

	}

	/**
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 * 
	 */
	private static class Pair<T> implements Serializable{

		private static final long serialVersionUID = 1685795769196233024L;
		public T o1;
		public T o2;

		public Pair(T o1, T o2) {
			this.o1 = o1;
			this.o2 = o2;
		}

		public static boolean same(Object o1, Object o2) {
			return o1 == null ? o2 == null : o1.equals(o2);
		}

		T getFirst() {
			return o1;
		}

		T getSecond() {
			return o2;
		}

		void setFirst(T o) {
			o1 = o;
		}

		void setSecond(T o) {
			o2 = o;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof Pair))
				return false;
			Pair<?> p = (Pair<?>) obj;
			return same(p.o1, this.o1) && same(p.o2, this.o2);
		}

		public String toString() {
			return "Pair{" + o1 + ", " + o2 + "}";
		}

		public boolean contains(T someEdge) {
			if (same(this.o1, someEdge) || same(this.o2, someEdge)) {
				return true;
			}
			return false;
		}
	}

	public static void main(String[] args) {
		double[][] cities = { { 1, 1 }, { 8, 1 }, { 8, 8 }, { 1, 8 }, { 2, 2 },
				{ 7, 2 }, { 7, 7 }, { 2, 7 }, { 3, 3 }, { 6, 3 }, { 6, 6 },
				{ 3, 6 }, { 4, 4 } };

		List<City> citiesList = new Vector<City>();
		for (int cityIndex = 0; cityIndex < cities.length; cityIndex++) {
			City c = new City(cityIndex, cities[cityIndex][0],
					cities[cityIndex][1]);
			citiesList.add(c);
		}
		LowerBound lb = new LowerBound(citiesList);
		System.out.println(lb);
	}
}
