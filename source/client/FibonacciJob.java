package client;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tasks.FibonacciTask;
import tasks.TspTask.City;
import api.Client2Space;
import api.Result;

/**
 * A job to perform remote computation of Nth Fibonacci number
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */

public class FibonacciJob extends Job {

	private static final String LOG_FILE = "/cs/student/kowshik/fibonacci_job.log";

	private Logger logger;
	private Handler handler;

	private int n;
	private int fibValue;
	private long startTime;

	/**
	 * 
	 * @param n
	 *            The Nth fibonacci number to be generated
	 */
	public FibonacciJob(int n) {

		this.n = n;

		this.logger = Logger.getLogger("FibonacciJob");
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
	 * Returns values representing the Nth fibonacci number that was cached by {@link #collectResults(Space)
	 * collectResults(Space space)} method. 
	 * 
	 * @return Nth fibonacci number
	 * 
	 * @see client.Job Job
	 */
	@Override
	public Integer getAllResults() {
		return fibValue;
	}



	/**
	 * Executes fibonacci computation in a compute space ({@link api.Space Space}) and stores the result
	 * 
	 * @param space
	 *            Compute space to which @{link tasks.FibonacciTask
	 *            FibonacciTask} objects should be sent for execution
	 * @throws RemoteException
	 * 
	 * @see client.Job Job
	 */
	@Override
	public void executeJob(Client2Space space) throws RemoteException {
		
		Result<Integer> r = (Result<Integer>) space.compute(new FibonacciTask(n),null);
		logger.info("Elapsed Time=" + (System.currentTimeMillis() - startTime));
		this.handler.close();
		this.fibValue=r.getValue();
	}

}
