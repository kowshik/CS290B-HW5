package system;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import system.Successor.Closure;
import api.Result;
import api.Task;

/**
 * For every {@link system.Computer Computer} instance that registers with the
 * compute space ({@link api.Client2Space Space}), there is a proxy maintained
 * by the space. This allows the compute space to maintain multiple threads for
 * each instance of registered {@link system.Computer Computer} objects. This
 * class is responsible for execution of {@link api.Task Task} objects in the
 * registered remote computers.
 * 
 * Each proxy maintains a queue of tasks that need to be executed one after the
 * other on a remote machine. These tasks can either represent the Divide phase
 * or the Conquer phase in the <a
 * href="http://en.wikipedia.org/wiki/Divide_and_conquer_algorithm">Divide and
 * conquer algorithm</a>.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class ComputerProxy {
	private static final String LOG_FILE_PREFIX = "/cs/student/kowshik/computerproxy_";
	private Computer compObj;
	private SpaceImpl space;
	// private Thread t;
	private LinkedBlockingQueue<Task<?>> tasks;
	private String id;
	private Logger logger;
	private Handler handler;
	private List<Task<?>> queuedTasks;

	/**
	 * 
	 * @param compObj
	 *            Computer registed with the compute space (
	 *            {@link api.Client2Space Space})
	 * @param space
	 *            Implementation of ({@link api.Client2Space Space}) which is
	 *            responsible for maintaining each instance of this class
	 * @throws RemoteException
	 */
	public ComputerProxy(Computer compObj, SpaceImpl space, String proxyId)
			throws RemoteException {
		this.compObj = compObj;
		this.space = space;
		this.tasks = new LinkedBlockingQueue<Task<?>>();
		this.id = proxyId;
		compObj.setId(this.id);
		this.logger = Logger.getLogger("ComputerProxy" + id);
		this.logger.setUseParentHandlers(false);
		this.handler = null;
		try {
			this.handler = new FileHandler(LOG_FILE_PREFIX + id + ".log");

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.handler.setFormatter(new SimpleFormatter());
		logger.addHandler(handler);

		this.queuedTasks = new Vector<Task<?>>();

	}

	public String getId() {
		return id;
	}

	public synchronized void setShared(Shared<?> newShared)
			throws RemoteException {
		compObj.setShared(newShared);
	}

	public List<Task<?>> getQueuedTasks() {

		return queuedTasks;

	}

	public void setQueuedTasks(Task<?> task) {
		queuedTasks.add(task);
	}

	/**
	 * Loops infinitely and attempts to fetch a {@link api.Task Task} object
	 * from the proxy's queue and executes it. If the thread is interrupted, the
	 * task is returned to the compute space's queue. If the task execution is
	 * successful, then the {@link api.Result Result} produced is also added to
	 * compute space's queue of {@link api.Result Result} objects.
	 * 
	 * These tasks can either represent the Divide phase or the Conquer phase in
	 * the <a
	 * href="http://en.wikipedia.org/wiki/Divide_and_conquer_algorithm">Divide
	 * and conquer algorithm</a>. Divide tasks are represented by the DECOMPOSE
	 * status and Conquer tasks are represented by the CONQUER status. The proxy
	 * switches the status of the task to COMPOSE, immediately after the Divide
	 * phase is over.
	 */

	/**
	 * 
	 * @param aTask
	 *            A task to be added to this proxy's queue
	 */
	public synchronized void addTask(Task<?> aTask) {
		try {
			this.tasks.put(aTask);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return A random thread name made up of exactly three alphabets
	 */
	/*
	 * private static String getRandomProxyName() { char first = (char) ((new
	 * Random().nextInt(26)) + 65); char second = (char) ((new
	 * Random().nextInt(26)) + 65); char third = (char) ((new
	 * Random().nextInt(26)) + 65); return "" + first + second + third; }
	 */

	public Computer getCompObj() {
		return this.compObj;

	}

	public Task<?> getQtask(String id) {
		synchronized (queuedTasks) {
			for (Task<?> t : queuedTasks)
				if (t.getId().equals(id)) {
					return t;
				}

			return null;
		}
	}

	public void removeQTask(String id) {
		synchronized(queuedTasks){
		for (Task<?> t : queuedTasks)
			if (t.getId().equals(id)) {
				queuedTasks.remove(t);
			}
		return;
		}
	}

}
