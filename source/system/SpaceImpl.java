package system;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
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
	private static final int MIN_COMP_QSIZE = 500;
	private Shared<?> shared;
	private Map<String, ComputerProxy> IdProxyMap;
	private static final int TASK_QUEUE_MAX_SIZE = 1000;
	private static final int TASK_QUEUE_MIN_SIZE = 1;
	private static final int MIN_PROCESSORS = 1;
	private boolean latencySwitch;
	private boolean mcoreSwitch;

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

	/**
	 * 
	 * @param aTask Task to be added to ready queue
	 * @return
	 * @throws RemoteException
	 */
	public boolean put(Task<?> aTask) throws RemoteException {
		if (proxies.size() > 0) {
			synchronized (readyTasks) {
				readyTasks.add(aTask);
			}
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

		if (this.latencySwitch && aTask instanceof SpaceRunnable) {
			registerLocalComputer();
		}
		this.shared = shared;
		for (Entry<String, ComputerProxy> e : this.IdProxyMap.entrySet()) {
			e.getValue().setShared(shared);
		}

		if (this.put(aTask)) {
			try {
				return results.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 
	 */
	private void registerLocalComputer() {

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {

			Computer2Space space = (Computer2Space) Naming.lookup("//"
					+ "localhost:" + PORT_NUMBER + "/"
					+ Computer2Space.SERVICE_NAME);
			ComputerImpl comp = new ComputerImpl(space);
			space.register(comp, comp.getId(), MIN_PROCESSORS);
			System.out.println("SpaceImpl -> Queue Size of " + comp.getId()
					+ " = " + comp.getTaskQueueMaxSize());
			System.out.println("SpaceImpl -> Local Computer ready : "
					+ comp.getId());
		} catch (RemoteException e) {
			System.err.println("ComputerImpl Remote exception : ");
			e.printStackTrace();

		} catch (MalformedURLException e) {
			System.err.println("ComputerImpl Malformed exception : ");
			e.printStackTrace();
		} catch (NotBoundException e) {
			System.err.println("ComputerImpl NotBound exception : ");
			e.printStackTrace();
		}

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
		ComputerProxy aProxy = new ComputerProxy(computer, id);
		this.proxies.put(aProxy, computer.getTaskQueueSize());
		this.IdProxyMap.put(id, aProxy);
		System.out.println("SpaceImpl -> Computer registration successful : "
				+ id);
		computer.setShared(shared);

		if (this.latencySwitch && this.mcoreSwitch) {
			computer.startWorkers(numOfProcessors, numOfProcessors
					* TASK_QUEUE_MAX_SIZE);
		} else if (this.latencySwitch && !this.mcoreSwitch) {
			computer.startWorkers(MIN_PROCESSORS, numOfProcessors
					* TASK_QUEUE_MAX_SIZE);
		} else if (!this.latencySwitch && this.mcoreSwitch) {
			computer.startWorkers(numOfProcessors, TASK_QUEUE_MIN_SIZE);
		} else {
			computer.startWorkers(MIN_PROCESSORS, TASK_QUEUE_MIN_SIZE);
		}
	}

	/**
	 * 
	 * @param aProxy Computer Proxy to be added to the Space
	 */
	public synchronized void addProxy(ComputerProxy aProxy) {
		try {
			this.proxies.put(aProxy, aProxy.getCompObj().getTaskQueueSize());
		} catch (RemoteException e) {
			System.err.println("The computer is not reachable");

		}
	}

	/**
	 * 
	 * @param aProxy  @param aProxy Computer Proxy to be removed to the Space
	 */
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
		String latency = args[0];
		String mcore = args[1];

		boolean latencySwitch = false;
		if (latency.equals("1")) {
			latencySwitch = true;
		}

		boolean mcoreSwitch = false;
		if (mcore.equals("1")) {
			mcoreSwitch = true;
		}

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {

			Client2Space space = new SpaceImpl();
			space.setLatencyOptimization(latencySwitch);
			space.setMcoreSwitch(mcoreSwitch);
			System.out.println("Latency -> " + space.getLatencyOptimization());
			System.out.println("Mutlicore -> " + space.getMcoreSwitch());
			Registry registry = LocateRegistry.createRegistry(PORT_NUMBER);
			registry.rebind(Client2Space.SERVICE_NAME, space);
			System.out.println("SpaceImpl -> Space instance bound");
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
			while (!proxies.isEmpty()) {

				ComputerProxy cp = this.getSmallestProxy();
				String thisProxyId = cp.getId();

				try {

					if (cp.getCompObj().getTaskQueueSize() < MIN_COMP_QSIZE) {
						List<Task<?>> list = new Vector<Task<?>>();
						int noOfTasks = cp.getCompObj().getTaskQueueMaxSize()
								- cp.getCompObj().getTaskQueueSize();
						int i;
						for (i = 0; (!readyTasks.isEmpty()) && i < noOfTasks; i++) {

							Task<?> t = removeReadyTask();
							list.add(t);
							cp.addTaskToQueue(t);
						}
						if (list.size() != 0) {
							cp.getCompObj().addTasks(list);
						}

						/*
						 * Get the first entry from the proxy list and add tasks
						 * to it. Then set the map to show the new values of the
						 * queue sizes by polling over them.
						 */

						this.proxies.put(cp, i);
						
					}
				}

				catch (RemoteException e) {
					System.err
							.println("Remote Exception occurred in Computer: "
									+ thisProxyId);
					System.err.println("Reassigning tasks in Computer"
							+ thisProxyId + " to ready queue");

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

	@Override
	/**
	 * Used by Computer to send results to the space
	 */
	public void sendResult(Result<?> result, String computerId)
			throws RemoteException {

		ComputerProxy thisCp = IdProxyMap.get(computerId);
		/* t is the task that generated the result */
		Task<?> t = thisCp.getTaskFromQueue(result.getId());
		if (result.getSubTasks() != null) {

			Successor s = new Successor(t, this, t.getDecompositionSize());
			this.addSuccessor(s);

			for (Task<?> task : result.getSubTasks()) {

				if (task.getQueuingStatus().equals(Task.QueuingStatus.QUEUED)) {

					thisCp.addTaskToQueue(task);
				} else {
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

	/**
	 *
	 * @return Internal shared variable in Space
	 */
	public synchronized Shared<?> getShared() {
		return this.shared;
	}
	/**
	 * 
	 * @param task Task to be removed from the wait queue
	 */

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
		List<ComputerProxy> minProxies = new Vector<ComputerProxy>();
		for (Entry<ComputerProxy, Integer> entry : this.proxies.entrySet()) {
			int thisQueueSize = entry.getValue();
			ComputerProxy thisProxy = entry.getKey();
			if (thisQueueSize < minQueueSize) {
				minQueueSize = thisQueueSize;
				minProxies.clear();
				minProxies.add(thisProxy);
			} else if (thisQueueSize == minQueueSize) {
				minProxies.add(thisProxy);
			}
		}
		Collections.shuffle(minProxies);
		return minProxies.get(0);
	}

	@Override
	public void setMcoreSwitch(boolean mcore) {
		this.mcoreSwitch = mcore;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Client2Space#getLatency()
	 */
	@Override
	public boolean getLatencyOptimization() throws RemoteException {
		return this.latencySwitch;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Client2Space#getMcore()
	 */
	@Override
	public boolean getMcoreSwitch() throws RemoteException {
		return this.mcoreSwitch;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Client2Space#setLatencyOptimization(boolean)
	 */
	@Override
	public void setLatencyOptimization(boolean latency) throws RemoteException {
		this.latencySwitch = latency;

	}
}
