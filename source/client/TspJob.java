package client;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import system.Shared;
import system.TspShared;
import tasks.TspTask;
import tasks.TspTask.City;
import api.Client2Space;
import api.Result;

/**
 * Defines a Travelling Salesman Problem through the generic {@link client.Job
 * Job} interface
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class TspJob extends Job {

	private static final String LOG_FILE = "/cs/student/kowshik/tsp_joib.log";

	private double[][] cities;
	private int[] minRoute;
	private Logger logger;
	private Handler handler;
	private long startTime;

	/**
	 * @param cities
	 *            Represents the x and y coordinates of cities. cities[i][0] is
	 *            the x-coordinate of city[i] and cities[i][1] is the
	 *            y-coordinate of city[i].
	 */
	public TspJob(final double[][] cities) {
		this.cities = cities.clone();
		this.logger = Logger.getLogger("TspJob");
		this.logger.setUseParentHandlers(false);
		this.handler = null;
		try {
			this.handler = new FileHandler(LOG_FILE);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.handler.setFormatter(new SimpleFormatter());
		logger.addHandler(handler);
	}

	/**
	 * Executes the Travelling Salesman Problem remotely in a compute space (
	 * {@link api.Client2Space Space}) and stores the result.
	 * 
	 * @param space
	 *            Compute space to which @{link tasks.TspTask TspTask} objects
	 *            should be sent for execution
	 * @throws RemoteException
	 * 
	 * @see client.Job Job
	 */
	public void executeJob(Client2Space space) throws RemoteException {
		this.startTime = System.currentTimeMillis();
		Shared<Double> tspSharedObj=new TspShared(TspShared.INFINITY);
		Result<List<City>> r = (Result<List<City>>) space.compute(new TspTask(cities),tspSharedObj);
		logger.info("Elapsed Time=" + (System.currentTimeMillis() - startTime));
		this.minRoute = new int[r.getValue().size()];
		int index = 0;
		for (City c : r.getValue()) {
			minRoute[index] = c.getLabel();
			index++;
		}

		this.handler.close();

	}

	

	/**
	 * Returns values cached by {@link #executeJob(Client2Space)
	 * executeJob(Client2Space space)} method. Each value in the returned array
	 * represents the a city index in the optimal solution to the Travelling
	 * Salesman Problem.
	 * 
	 * @return An array that contains cities from all {@link api.Result Result}
	 *         objects that form an optimal solution to TSP
	 * @see client.Job Job
	 */
	@Override
	public int[] getAllResults() {
		return this.minRoute;
	}

}
