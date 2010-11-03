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

import tasks.TspTask;
import tasks.TspTask.City;

/**
 * Represents a lower bound for the travelling salesman problem. Uses the two
 * smallest edges of every city to calculate a lower bound value.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class LowerBound implements Cloneable, Serializable {

	private static final long serialVersionUID = -1230131808278869416L;
	private Map<City, EdgePair> edgeMap;

	private enum EdgeType {
		REAL, VIRTUAL
	};

	public String toString() {
		return "Lower bound : " + getLowerBoundValue() + "\nMap : "
				+ edgeMap.toString();
	}

	/**
	 * 
	 * @return Lowerbound value of this object. i.e. (Sum of all real edges + virtual edges) / 2.0
	 */
	public double getLowerBoundValue() {
		double lowerBound = 0.0d;
		for (Entry<City, EdgePair> e : edgeMap.entrySet()) {
			EdgePair p = e.getValue();
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

	/** 
	 * Adds  a real edge to the data structure by replacing the virtual edge with the maximum cost
	 * @param rEdgeStart Start city of real edge to be added
	 * @param rEdgeEnd End city of real edge to be added
	 */
	public void addRealEdge(City rEdgeStart, City rEdgeEnd) {
		if (rEdgeStart == null || rEdgeEnd == null
				|| rEdgeStart.equals(rEdgeEnd)) {
			return;
		}
		Edge realEdge = new Edge(rEdgeStart, rEdgeEnd);
		realEdge.setEdgeType(EdgeType.REAL);
		EdgePair rEdgeStartPair = this.edgeMap.get(rEdgeStart);
		EdgePair rEdgeEndPair = this.edgeMap.get(rEdgeEnd);
		if (rEdgeStartPair.contains(realEdge)
				|| rEdgeEndPair.contains(realEdge)) {
			return;
		}
		addRealEdge(rEdgeStartPair, realEdge);
		addRealEdge(rEdgeEndPair, realEdge);
	}

	private void addRealEdge(EdgePair p, Edge realEdge) {
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
		} else if (firstEdge.getEdgeType() != EdgeType.VIRTUAL) {
			return;
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

	/**
	 * 
	 * @param citiesList List of cities represenging a TSP
	 */
	public LowerBound(List<City> citiesList) {
		edgeMap = new LinkedHashMap<City, EdgePair>();
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
			EdgePair edgePair = new EdgePair(firstMinEdge, secondMinEdge);
			edgeMap.put(aStartCity, edgePair);
		}
	}

	@Override
	public Object clone() {
		try {
			LowerBound clone = (LowerBound) super.clone();
			clone.edgeMap = new LinkedHashMap<City, EdgePair>();
			for (Entry<City, EdgePair> e : this.edgeMap.entrySet()) {
				City cityClone = (City) e.getKey().clone();
				EdgePair edgePairClone = (EdgePair) e.getValue().clone();
				clone.edgeMap.put(cityClone, edgePairClone);
			}
			return clone;

		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Abstracts an edge in the TSP
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 *
	 */
	private static class Edge implements Serializable, Cloneable {

		private static final long serialVersionUID = -534808833417374784L;
		private City startCity;
		private City endCity;
		private double edgeLength;
		private EdgeType edgeType;

		public City getStartCity() {
			return startCity;
		}

		public void setStartCity(City startCity) {
			this.startCity = startCity;
			this.edgeLength = computeDistance();
		}

		public City getEndCity() {
			return endCity;
		}

		public void setEndCity(City endCity) {
			this.endCity = endCity;
			this.edgeLength = computeDistance();
		}

		public double getEdgeLength() {
			return edgeLength;
		}

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

		public boolean equals(Object o) {
			Edge e = (Edge) o;
			if (this.startCity.equals(e.getEndCity())
					&& this.endCity.equals(e.getEndCity()))
				return true;
			return false;
		}

		@Override
		public Object clone() {
			try {
				Edge clone = (Edge) super.clone();
				clone.startCity = (City) this.startCity.clone();
				clone.endCity = (City) this.endCity.clone();
				return clone;

			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return null;

		}

	}

	private static class MinEdgeComparator implements Comparator<Edge>,
			Serializable {

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
	 * 
	 * Abstracts a pair of edges in the TSP
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 * 
	 */
	private static class EdgePair implements Serializable, Cloneable {

		private static final long serialVersionUID = 1685795769196233024L;
		private Edge o1;
		private Edge o2;

		public EdgePair(Edge o1, Edge o2) {
			this.o1 = o1;
			this.o2 = o2;
		}

		public static boolean same(Edge o1, Edge o2) {
			return o1 == null ? o2 == null : o1.equals(o2);
		}

		public Edge getFirst() {
			return o1;
		}

		public Edge getSecond() {
			return o2;
		}

		public void setFirst(Edge o) {
			o1 = o;
		}

		public void setSecond(Edge o) {
			o2 = o;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof EdgePair))
				return false;
			EdgePair p = (EdgePair) obj;
			return same(p.o1, this.o1) && same(p.o2, this.o2);
		}

		public String toString() {
			return "Pair{" + o1 + ", " + o2 + "}";
		}

		public boolean contains(Edge someEdge) {
			if (same(this.o1, someEdge) || same(this.o2, someEdge)) {
				return true;
			}
			return false;
		}

		@Override
		public Object clone() {
			try {
				EdgePair clone = (EdgePair) super.clone();
				clone.o1 = (Edge) this.o1.clone();
				clone.o2 = (Edge) this.o2.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
}
