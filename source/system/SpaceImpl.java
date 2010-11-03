package system;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import system.Successor.Closure;
import api.Client2Space;
import api.Result;
import api.Task;

/**
 * Implementation of the Space Interface. Represents a raw computing resource
 * where tasks ({@link api.Task Task}) are automatically executed by registered
 * workers as soon as they are dropped in. If a worker crashes, the computation
 * would still continue (assuming there are other workers still running), since
 * each task is executed under a transaction, which would be rolled back after
 * the worker crashed, leaving the task in the space for another worker to pick
 * up. For more information, please refer <a
 * href="http://today.java.net/pub/a/today/2005/04/21/farm.html">How to build a
 * compute farm</a>.
 * 
 * The multithreading design implemented by this class is that of the <a
 * href="http://en.wikipedia.org/wiki/Cilk">Cilk</a> runtime. Please read the
 * architecture of Cilk to understand the class better.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class SpaceImpl extends UnicastRemoteObject implements Client2Space,
		Computer2Space, Runnable {

	private Thread t;
	private static final long serialVersionUID = 3093568798450948074L;
	private Map<String, Successor> waitingTasks;
	private List<Task<?>> readyTasks;
	private LinkedBlockingQueue<Result<?>> results;
	private Map<ComputerProxy, Integer> proxies;
	private static final int PORT_NUMBER = 3672;
	private static final int THRESHOLD_QSIZE = 300;
	private Shared<?> shared;
	private Map<String, ComputerProxy> IdProxyMap;
	private static final int TASK_QUEUE_MAX_SIZE = 1000;

	// private static final int DEFAULT_QUEUE_SIZE = 1000;

	/**
	 * Default constructor
	 * 
	 * @throws RemoteException
	 */
	public SpaceImpl() throws RemoteException {

		this.waitingTasks = Collections
				.synchronizedMap(new HashMap<String, Successor>());
		this.results = new LinkedBlockingQueue<Result<?>>();
		this.proxies = Collections
				.synchronizedMap(new HashMap<ComputerProxy, Integer>());
		this.readyTasks = new Vector<Task<?>>();
		this.IdProxyMap = Collections
				.synchronizedMap(new HashMap<String, ComputerProxy>());
		t = new Thread(this, "Space");
		t.start();
	}

	public boolean put(Task<?> aTask) throws RemoteException {
		if (proxies.size() > 0) {
			synchronized (readyTasks) {
				readyTasks.add(aTask);
			}
			System.out.println("Added task :" + aTask.getId());
			return true;
		}

		return false;
	}

	/**
	 * @see api.Client2Space#compute(Task, Shared) Client2Space.compute(Task,
	 *      Shared)
	 */

	public Result<?> compute(Task<?> aTask, Shared<?> shared)
			throws java.rmi.RemoteException {

		this.shared = shared;
		for(Entry< String , ComputerProxy > e : this.IdProxyMap.entrySet()){
			e.getValue().setShared(shared);
		}
		
		if (this.put(aTask)) {
			try {
				return results.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.err
				.println("Unable to register tasks due to absence of computer proxies");
		return null;
	}

	/**
	 * Used to add to the queue of {@link api.Result Result} objects in this
	 * compute space
	 * 
	 * @throws RemoteException
	 */
	public void putResult(Result<?> result) throws RemoteException {
		results.add(result);
	}

	/**
	 * Remote method for the computers to register to the compute space
	 * 
	 * @throws RemoteException
	 */
	@Override
	public synchronized void register(Computer computer, String id,
			int numOfProcessors) throws RemoteException {
		System.out.println("In registration");
		ComputerProxy aProxy = new ComputerProxy(computer, id);
		this.proxies.put(aProxy, computer.getTaskQueueSize());
		this.IdProxyMap.put(id, aProxy);
		System.out.println("Registration completed");
		computer.setShared(shared);
		computer.startWorkers(numOfProcessors, TASK_QUEUE_MAX_SIZE);

	}

	public synchronized void addProxy(ComputerProxy aProxy) {
		try {
			this.proxies.put(aProxy, aProxy.getCompObj().getTaskQueueSize());
		} catch (RemoteException e) {
			System.err.println("The computer is not reachable");

		}
	}

	public synchronized void removeProxy(ComputerProxy aProxy) {
		this.proxies.remove(aProxy);
	}

	/**
	 * Starts the compute space and binds remote objects into the RMI registry
	 * 
	 * @param args
	 *            Command-line arguments can be passed (if any)
	 */
	public static void main(String[] args) {

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {

			Client2Space space = new SpaceImpl();
			Registry registry = LocateRegistry.createRegistry(PORT_NUMBER);
			registry.rebind(Client2Space.SERVICE_NAME, space);
			System.out.println("Space instance bound");
		} catch (Exception e) {
			System.err.println("SpaceImpl exception:");
			e.printStackTrace();

		}
	}

	/**
	 * Polls for {@link system.Successor Successor} threads to be execute once
	 * they move into READY status
	 */
	@Override
	public void run() {
		while (true) {

			// System.out.println("In run of Space");
			while (!proxies.isEmpty()) {
				ComputerProxy cp = this.getSmallestProxy();
				String thisProxyId = cp.getId();

				// System.out.println("got proxy :" + thisProxyId);
				try {

					if (cp.getCompObj().getTaskQueueSize() < THRESHOLD_QSIZE) {

						List<Task<?>> list = new Vector<Task<?>>();
						int noOfTasks = cp.getCompObj().getTaskQueueMaxSize()
								- cp.getCompObj().getTaskQueueSize();
						// System.out.println("No of tasks :"+ noOfTasks);
						int i;
						for (i = 0; (!readyTasks.isEmpty()) && i < noOfTasks; i++) {

							Task<?> t = removeReadyTask();
							System.err.println("Removed task " + t.getId()
									+ " from the ready list");

							list.add(t);
							cp.addTaskToQueue(t);
							System.err
									.println("Pushed task to computer proxy's Q : "
											+ t.getId());

						}
						cp.getCompObj().addTasks(list);

						/*
						 * Get the first entry from the proxy list and add tasks
						 * to it. Then set the map to show the new values of the
						 * queue sizes by polling over them.
						 */

						this.proxies.put(cp, i);
						/*
						 * Set<Entry<ComputerProxy, Integer>> proxySet = proxies
						 * .entrySet(); for (Entry<ComputerProxy, Integer> e :
						 * proxySet) { e.setValue(e.getKey().getCompObj()
						 * .getTaskQueueSize());
						 */
					}
				}

				catch (RemoteException e) {
					System.err
							.println("Remote Exception occurred in Computer: "
									+ thisProxyId);
					System.err.println("Reassigning tasks in Computer"
							+ thisProxyId + " to ready queue");

					// List<Task<?>> fullQ = cp.getTaskQueue();
					for (Task<?> task : cp.getTaskQueue()) {
						try {
							this.put(task);

						} catch (RemoteException ex) {
							System.err
									.println("Unable to reassign tasks to ready queue");
							ex.printStackTrace();
						}
					}
					proxies.remove(cp);
					IdProxyMap.remove(cp.getId());

				}
			}

		}
	}

	public void sendResult(Result<?> result, String computerId)
			throws RemoteException {

		ComputerProxy thisCp = IdProxyMap.get(computerId);
		/* t is the task that generated the result */
		Task<?> t = thisCp.getTaskFromQueue(result.getId());
		System.err.println("Results got from the Task " + result.getId()
				+ " with subtasks : " + result.getSubTasks());
		if (result.getSubTasks() != null) {

			Successor s = new Successor(t, this, t.getDecompositionSize());
			this.addSuccessor(s);

			for (Task<?> task : result.getSubTasks()) {

				if (task.getQueuingStatus().equals(Task.QueuingStatus.QUEUED)) {

					thisCp.addTaskToQueue(task);
				} else {
					System.err.println("This task has not been queued "
							+ task.getId());
					this.put(task);
				}
			}
		}

		else if (result.getValue() != null
				&& (t.getId().equals(t.getParentId()))) {
			this.putResult(result);
		} else {
			Closure parentClosure = this.getClosure(t.getParentId());
			parentClosure.put(result.getValue());
		}
		thisCp.removeTaskFromQueue(result.getId());
	}

	private Task<?> removeReadyTask() {
		synchronized (readyTasks) {
			Task<?> t = readyTasks.remove(0);
			System.err
					.println("Ready queue after task removal : " + readyTasks);
			return t;
		}
	}

	/**
	 * 
	 * @param s
	 *            Successor thread to be added to the queue
	 */
	public void addSuccessor(Successor s) {
		synchronized (this) {
			waitingTasks.put(s.getId(), s);
		}

	}

	/**
	 * 
	 * @param successorId
	 *            Successor thread to be removed from the queue
	 */
	public void removeSuccessor(String successorId) {
		synchronized (this) {
			waitingTasks.remove(successorId);
		}

	}

	/**
	 * 
	 * @param id
	 *            ID of the successor thread whose Closure object is required
	 * @return Gets the closure object corresponding to the Successor thread.
	 */
	public Successor.Closure getClosure(String id) {
		synchronized (this) {
			return waitingTasks.get(id).getClosure();
		}

	}

	/**
	 * This method is synchronized because no two computers can broadcast at the
	 * same time.
	 * 
	 * @see system.Computer2Space#broadcast(Broadcast)
	 *      system.Computer2Space.broadcast(Broadcast)
	 */
	@Override
	public synchronized void broadcast(Broadcast broadcast)
			throws RemoteException {
		Shared<?> newShared = broadcast.getShared();
		String computerId = broadcast.getComputerId();
		if (!shared.isNewerThan(newShared)) {
			this.setShared(newShared);
			Set<Entry<ComputerProxy, Integer>> proxySet = proxies.entrySet();
			for (Entry<ComputerProxy, Integer> e : proxySet) {
				if (!e.getKey().getId().equals(computerId)) {
					e.getKey().setShared(newShared);
				}
			}
		}
	}

	/**
	 * @param newShared
	 */
	public synchronized void setShared(Shared<?> newShared) {
		this.shared = newShared;
	}

	public synchronized Shared<?> getShared() {
		return this.shared;
	}

	public synchronized void removeFromWaitQ(Task<?> task) {

		
		Set<Entry<String, Successor>> waitQ = waitingTasks.entrySet();
		for (Entry<String, Successor> e : waitQ) {
			String thisKey = e.getKey();
			if (thisKey.equals(task.getId())) {
				waitingTasks.remove(thisKey);
				return;
			}
		}
	}

	private ComputerProxy getSmallestProxy() {

		int minQueueSize = Integer.MAX_VALUE;
		ComputerProxy minProxy = null;
		for (Entry<ComputerProxy, Integer> entry : this.proxies.entrySet()) {
			int thisQueueSize = entry.getValue();
			ComputerProxy thisProxy = entry.getKey();
			if (thisQueueSize < minQueueSize) {
				minQueueSize = thisQueueSize;
				minProxy = thisProxy;
			}
		}
		return minProxy;
	}
}
