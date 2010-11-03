package client;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import tasks.MandelbrotSetTask;
import api.Result;
import api.Client2Space;
import api.Task;

/**
 * A job to perform remote computation of <a
 * href="http://en.wikipedia.org/wiki/Mandelbrot_set">Mandelbrot Set</a> by
 * splitting it up into smaller tasks of type {@link api.Task task}
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class MandelbrotSetJob extends Job {

	private double lowerX;
	private double lowerY;
	private double edgeLength;
	private int n;
	private int iterLimit;
	private Logger logger;
	private Handler handler;
	private int[][] allValues;
	private long startTime;
	private static final String LOG_FILE = "/cs/student/kowshik/mandelbrotset_job.log";

	/**
	 * 
	 * @param lowerX
	 *            X-coordinate of the lower left corner of a square in the
	 *            complex plane
	 * @param lowerY
	 *            Y-coordinate of the lower left corner of a square in the
	 *            complex plane
	 * @param edgeLength
	 *            Edge length of the square in the complex plane, whose sides
	 *            are parallel to the axes
	 * @param n
	 *            Square region of the complex plane subdivided into n X n
	 *            squares, each of which is visualized by 1 pixel
	 * @param iterLimit
	 *            Defines when the representative point of a region is
	 *            considered to be in the Mandelbrot set.
	 */
	public MandelbrotSetJob(double lowerX, double lowerY, double edgeLength,
			int n, int iterLimit) {
		super();
		this.lowerX = lowerX;
		this.lowerY = lowerY;
		this.edgeLength = edgeLength;
		this.n = n;
		this.iterLimit = iterLimit;
		this.logger = Logger.getLogger("MandelbrotSetJob");
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
	 * Executes the task in the compute space. Gathers {@link api.Result Result}
	 * objects from the compute space and caches them in a simple data structure
	 * that can be quickly retrieved by the client through the
	 * {@link #getAllResults getAllResults()} method
	 * 
	 * @param space
	 *            Compute space containing the results obtained after remote
	 *            execution of tasks
	 * @throws RemoteException
	 * @see client.Job Job
	 */
	public void executeJob(Client2Space space) throws RemoteException {
		Result<MandelbrotSetTask.MandelbrotSetTaskResult> r = (Result<MandelbrotSetTask.MandelbrotSetTaskResult>) space
				.compute(new MandelbrotSetTask(lowerX, lowerY, edgeLength, n,
						iterLimit), null);
		this.allValues = r.getValue().getValues();
		logger.info("Elapsed Time=" + (System.currentTimeMillis() - startTime));
		this.handler.close();
	}

	/**
	 * Returns values cached by {@link #executeJob(Client2Space)
	 * executeJob(Client2Space)} method. Each value in the returned array
	 * represents the colour of a pixel to be displayed on the screen to
	 * represent the Mandelbrot Set.
	 * 
	 * @return An array that contains Mandelbrot Set integer values from all
	 *         {@link api.Result Result} objects
	 * @see client.Job Job
	 */
	public int[][] getAllResults() {
		return this.allValues;
	}

}
