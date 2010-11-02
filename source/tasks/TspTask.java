package tasks;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import system.ResultImpl;
import system.Shared;
import system.TspShared;
import api.Result;
import api.Task;

/**
 * Computes an optimal solution for the <a
 * href="http://en.wikipedia.org/wiki/Travelling_salesman_problem">Travelling
 * Salesman Problem</a>
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public class TspTask extends TaskBase<List<TspTask.City>> implements
		Serializable {

	/**
	 * Represents a city in the travelling salesman problem by defining its
	 * x-coordinate, y-coordinate and label.
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam*
	 */
	public class City implements Serializable {

		private static final long serialVersionUID = -8660442769258565881L;
		private double x;
		private double y;
		private int label;

		public City(int label, double x, double y) {
			this.x = x;
			this.y = y;
			this.label = label;
		}

		public void setX(int x) {
			this.x = x;
		}

		public double getX() {
			return x;
		}

		public void setY(int y) {
			this.y = y;
		}

		public double getY() {
			return y;
		}

		public void setLabel(int label) {
			this.label = label;
		}

		public int getLabel() {
			return label;
		}

		public String toString() {
			return "" + label;
		}

		@Override
		public boolean equals(Object c) {
			return this.getLabel() == (((City) c).getLabel());
		}

	}

	private static final long serialVersionUID = 3276207466199157936L;
	private List<City> citiesList;
	private List<City> currentRoute;
	private City startCity;
	private int numberOfChildren;
	private double lowerBound;
	private List<List<City>> values;

	// Permissible recursion level beyond which the problem is solved locally
	private static final int NUMBER_OF_LEVELS = 3;

	/**
	 * @param cities
	 *            Represents the x and y coordinates of cities. cities[i][0] is
	 *            the x-coordinate of city[i] and cities[i][1] is the
	 *            y-coordinate of city[i].
	 */

	public TspTask(double[][] cities) {
		super(DEFAULT_TASK_ID, DEFAULT_TASK_ID, Task.Status.DECOMPOSE,
				Task.QueuingStatus.NOT_QUEUED, System.currentTimeMillis());
		citiesList = new Vector<City>();
		for (int cityIndex = 0; cityIndex < cities.length; cityIndex++) {
			citiesList.add(new City(cityIndex, cities[cityIndex][0],
					cities[cityIndex][1]));
		}
		this.startCity = new City(0, cities[0][0], cities[0][1]);
		this.currentRoute = new Vector<City>();
		this.currentRoute.add(this.startCity);
		this.numberOfChildren = citiesList.size() - 1;
		this.lowerBound = 0.0f;

	}

	/**
	 * This constructor is used by the decompose method to generate new Sub
	 * Tasks of of the given TSP task.
	 * 
	 * @param startCity
	 *            Represents the starting City for the current tour being
	 *            calculated
	 * @param route
	 *            The route which has been computed so far
	 * @param citiesList
	 *            The list of cities which are not part of the computed route
	 * @param taskId
	 *            Id of the current task
	 * @param parentId
	 *            Id of the parent task
	 * @param s
	 *            {@link api.Task.Status Status of a task}
	 * @param lowerBound
	 *            The computed value of the lower bound of the parent node which
	 *            will be passed to the child tasks
	 * 
	 */

	private TspTask(City startCity, List<City> route, List<City> citiesList,
			String taskId, String parentId, Task.Status s, double lowerBound) {
		super(taskId, parentId, Task.Status.DECOMPOSE,
				Task.QueuingStatus.NOT_QUEUED, System.currentTimeMillis());

		this.citiesList = citiesList;
		this.startCity = startCity;
		this.currentRoute = new Vector<City>(route);
		this.currentRoute.add(this.startCity);
		this.numberOfChildren = citiesList.size();
		this.lowerBound = computeLowerBound(this.currentRoute, this.citiesList);
	}

	// TODO : Improve the code below to use algorithm discussed in class
	/**
	 * 
	 * @return The lowerBound for every new child task is calculated and
	 *         returned using the two shortest edges incident on each city
	 */
	private double computeLowerBound(List<City> currentRoute,
			List<City> citiesList) {

		City firstCity = currentRoute.get(0);
		City lastCity = currentRoute.get(currentRoute.size() - 1);
		List<City> options = new Vector<City>(citiesList);
		options.add(lastCity);

		// Find length of current partial tour
		double minEdgeCostSum = findRouteLength(currentRoute);
		;

		/*
		 * Tighten the lower bound by adding only those edges that have a
		 * minimum cost among the set of remaining edges in the graph
		 */
		double minEdgeCost = 0.0d;
		for (City c : options) {
			minEdgeCost = 0.0d;
			if (!firstCity.equals(lastCity)) {
				minEdgeCost = findLength(c, firstCity);
			}
			minEdgeCost = findLength(c, firstCity);
			for (City otherCity : options) {
				if (!otherCity.equals(c)) {
					double thisLength = findLength(c, otherCity);
					if (thisLength < minEdgeCost) {
						minEdgeCost = thisLength;
					}
				}
			}

			// Add min edge cost
			minEdgeCostSum += minEdgeCost;
		}
		return minEdgeCostSum;

	}

	/**
	 * Implements the decompose phase of TSP divide and conquer solution. Every
	 * task calculates its new lower-bound initially and checks if it is lesser
	 * than the existing upper-bound.
	 * 
	 * If yes, then the task continues with the decomposition. A solution and a
	 * new upper-bound are computed if the task is the last node in the
	 * decomposition tree (or) the decomposition has reached an internal
	 * restricted recursion depth. The new upper-bound is communicated to the
	 * compute space.
	 * 
	 * Else, this node in the search tree is not explored further and it is
	 * pruned.
	 * 
	 * 
	 */
	private Result<List<City>> decompose() {
		System.out.println("TSP decompose with task "+ this.getId() + "  Parent = " + this.getParentId());
		Result<List<City>> r = new ResultImpl<List<City>>(this.getId(), this
				.getParentId());
		try {
			// Get the shared object from the computer
			TspShared compShared = (TspShared) this.computer.getShared();
			if (compShared == null){
				System.out.println("The shared object doesnt exist");
			}
			//System.out.println("Got shared object");
			// Is lower-bound greater than upper-bound ?
			if (compShared.get().equals(TspShared.INFINITY)
					|| lowerBound <= compShared.get()) {

				/*
				 * Has the decomposition hit the permissible depth of recursion
				 * ?
				 */
				if (this.currentRoute.size() < NUMBER_OF_LEVELS) {
					List<Task<List<City>>> subTasks = new Vector<Task<List<City>>>();
					List<String> childIds = this.getChildIds();
					int childIndex = 0;
					System.out.println("Children for level"+currentRoute.size()+": "+childIds);

					/*
					 * Find child cities for next level of decomposition and
					 * create subtasks
					 */
					for (int i = 0; i < citiesList.size(); i++) {
						if (!citiesList.get(i).equals(this.startCity)) {
							City newStartCity = citiesList.get(i);
							String childId = childIds.get(childIndex);
							childIndex++;
							List<City> childCities = new Vector<City>();
							for (int j = 0; j < citiesList.size(); j++) {
								if (!citiesList.get(j).equals(this.startCity)
										&& !citiesList.get(j).equals(
												newStartCity)) {
									childCities.add(citiesList.get(j));
								}
							}
							TspTask childTask = new TspTask(newStartCity,
									this.currentRoute, childCities, childId,
									this.getId(), Task.Status.DECOMPOSE,
									lowerBound);
							childTask.setComputer(computer);
							subTasks.add(childTask);
							System.out.println("Added subtask "+childId+" to the Result Sink");
						}
					}

					r.setSubTasks(subTasks);
					return r;
				}

				/*
				 * If max recursion depth has been reached, then find the
				 * minimum-cost route among remaining cities locally
				 */
				r.setValue(findMinRoute());
				return r;
			}
			/*
			 * If lower-bound is greater than upper-bound, then prune this node
			 * and return a null value
			 */
			r.setValue(null);
			return r;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Computes the distance between two points
	 */
	private double findLength(City c1, City c2) {
		double x1 = c1.getX();
		double y1 = c1.getY();
		double x2 = c2.getX();
		double y2 = c2.getY();
		return Point2D.distance(x1, y1, x2, y2);

	}

	@Override
	/**
	 * Number of subtasks created in each stage of recursion
	 */
	public int getDecompositionSize() {
		return this.numberOfChildren;
	}

	/**
	 * Implements the conquer phase of TSP divide and conquer solution.
	 * 
	 * Used by the successor of each task to integrate the routes returned by
	 * all its children and find the least cost route from all of them.
	 * 
	 */
	private Result<List<City>> compose() {

		/*
		 * getValues() may contain null values since a Node can die due to
		 * pruning. So before type casting to List<List<City>>, we do a null
		 * check.
		 * 
		 * If the list is null, then create an empty ResultImpl object and
		 * return it.
		 * 
		 * Else, compute the route with the minimum distance among the list of
		 * routes passed. Return this min-distance route in a ResultImpl object.
		 */

		List<List<City>> minRoutes = this.getValues();
		Result<List<City>> r = new ResultImpl<List<City>>(this.getId(), this
				.getParentId());
		if (minRoutes != null) {
			List<City> chosenMinRoute = null;
			double minLength = Double.MAX_VALUE;
			int level = this.getTaskLevel();
			for (List<City> route : minRoutes) {
				if (route != null) {
					City routeStartCity = route.get(0);

					double thisLength = findRouteLength(route)
							+ findLength(this.startCity, routeStartCity);
					if (level == DEFAULT_TASK_LEVEL) {
						City routeEndCity = route.get(route.size() - 1);
						thisLength += findLength(this.startCity, routeEndCity);
					}

					if (thisLength < minLength) {
						minLength = thisLength;
						chosenMinRoute = route;
					}
				}
			}
			if (chosenMinRoute != null) {
				chosenMinRoute.add(0, this.startCity);
			}
			r.setValue(chosenMinRoute);
			return r;
		} else {
			r.setValue(null);
			return r;
		}
	}

	/**
	 * 
	 * @param aListOfCities
	 *            A list of Cities whose route length has be calculated.
	 * @return The length of the route among the set of Cities recieved.
	 * 
	 */
	private double findRouteLength(List<City> aListOfCities) {

		double length = 0.0d;
		int i;
		for (i = 0; i < aListOfCities.size() - 1; i++) {
			length += findLength(aListOfCities.get(i), aListOfCities.get(i + 1));
		}
		return length;
	}

	/**
	 * Returns the minimum-cost route among cities in this sub-task using a
	 * depth-first search algorithm. Each new minimum-cost is propagated to the
	 * compute space.
	 * 
	 * @return
	 */

	private List<City> findMinRoute() {
		// Stack for DFS
		Stack<List<City>> s = new Stack<List<City>>();
		List<City> firstNewRoute = new Vector<City>();
		List<City> minRoute = null;
		firstNewRoute.add(this.startCity);
		System.err.println("First new route : "+firstNewRoute);
		s.add(firstNewRoute);
		try {

			// Perform DFS until stack is not empty
			while (!s.isEmpty()) {
				List<City> thisNewRoute = s.pop();
				List<City> kids = this.getKids(thisNewRoute);
				List<City> wholeRoute = clubRoutes(this.currentRoute,
						thisNewRoute);
				System.err.println("Whole route : "+wholeRoute);
				System.err.println("Kids : "+kids);
				/*
				 * Prunes the tree by checking if lowerbound has exceeded the
				 * upperbound
				 */
				if (this.getLatestUpperBound() == TspShared.INFINITY
						|| (computeLowerBound(wholeRoute, kids) <= this
								.getLatestUpperBound())) {

					// Leaf node
					if (kids.size() == 0) {
						System.err.println("Leaf node reached !");
						City lastCity = wholeRoute.get(wholeRoute.size() - 1);
						City firstCity = wholeRoute.get(0);
						double newUpperBound = findRouteLength(wholeRoute)
								+ findLength(lastCity, firstCity);
						Shared<Double> newShared = new TspShared(newUpperBound);
						if (this.getComputer().broadcast(newShared)) {

							minRoute = thisNewRoute;
						}
						
					}
					// Add each non-leaf node to the stack
					else {
						for (City kid : kids) {
							List<City> kidRoute = new Vector<City>(thisNewRoute);
							kidRoute.add(kid);
							s.add(kidRoute);
						}
					}
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return minRoute;
	}

	// Returns the current upper-bound value from the computer's shared object
	private double getLatestUpperBound() {
		try {
			TspShared sharedObj = (TspShared) (this.getShared());
			return sharedObj.get();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return 0;

	}

	// Returns children of last node in the passed route
	private List<City> getKids(List<City> route) {
		List<City> kidsList = new Vector<City>();
		for (City c : this.citiesList) {
			if (!route.contains(c)) {
				kidsList.add(c);
			}
		}
		return kidsList;
	}

	private List<City> clubRoutes(List<City> currentRoute, List<City> newRoute) {
		List<City> cities = new Vector<City>(currentRoute);
		for (int i = 1; i < newRoute.size(); i++) {
			cities.add(newRoute.get(i));
		}
		return cities;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#execute()
	 */
	@Override
	public Result<?> execute() {
		if (this.getStatus() == Task.Status.DECOMPOSE) {
			return this.decompose();
		}
		if (this.getStatus() == Task.Status.COMPOSE) {
			return this.compose();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#putValues(java.util.List)
	 */
	@Override
	public void putValues(List<?> values) {
		this.values = new Vector<List<City>>();
		for (Object o : values) {
			List<City> listOfCities = (List<City>) o;
			this.values.add(listOfCities);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#getValues()
	 */
	@Override
	public List<List<TspTask.City>> getValues() {

		return values;
	}

}
