package system;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import api.Result;
import api.Task;

/**
 * Defines the remote server which is accessed by the client for execution of
 * objects of type {@link api.Task Task}
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */

public class ComputerImpl extends UnicastRemoteObject implements Computer {

	private static final long serialVersionUID = -4634299253959618077L;
	private Shared<?> shared;
	private Computer2Space space;
	private String id;
	private int numOfProcessors;
	private Queue<Task<?>> taskQueue;
	private ResultSink sink;
	private int taskQueueMaxSize;

	// Return max size of task queue
	public int getTaskQueueMaxSize() {
		return taskQueueMaxSize;
	}

	// Gets num of processors available for use by the VM
	private int getNumOfProcessors() {
		return numOfProcessors;
	}

	// Sets num of processors available for use by the VM
	private void setNumOfProcessors(int numOfProcessors) {
		this.numOfProcessors = numOfProcessors;
	}

	/**
	 * Sets up a compute node for execution
	 * 
	 * @throws RemoteException
	 */
	public ComputerImpl(Computer2Space space) throws RemoteException {
		super();
		this.space = space;

		this.taskQueue = new LinkedList<Task<?>>();
		this.shared=null;
		try {
			this.setNumOfProcessors(Runtime.getRuntime().availableProcessors());
			this.setId(InetAddress.getLocalHost().getHostName() + "_"
					+ ComputerImpl.getRandomChars());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * @see api.Task Task
	 */
	@Override
	public synchronized void addTasks(List<Task<?>> listOfTasks)
			throws RemoteException {
		for (Task<?> t : listOfTasks) {
			t.setComputer(this);
			this.taskQueue.add(t);
			System.out.println("Added task"+t.getId()+ "to computer task Q");
		}
	}

	// Add a task to the tail of the queue
	public synchronized void addTask(Task<?> aTask) {
		this.taskQueue.add(aTask);
	}

	@Override
	public void setShared(Shared<?> shared) {
		System.err.println("Setting shared : "+shared);
		this.shared = shared;

	}

	@Override
	public synchronized boolean broadcast(Shared<?> proposedShared)
			throws RemoteException {
		if (proposedShared.isNewerThan(shared)) {
			shared = proposedShared;
			space.broadcast(new Broadcast(this.shared, this.getId()));
			return true;
		}
		
		return false;
	}

	@Override
	public synchronized Shared<?> getShared() {
		return this.shared;
	}

	// Return the ID of this computer
	public String getId() {

		return this.id;
	}

	// Set the ID of this computer
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * Register Computer objects to the compute space
	 */
	public static void main(String[] args) {
		String computeSpaceServer = args[0];
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {

			Computer2Space space = (Computer2Space) Naming.lookup("//"
					+ computeSpaceServer + "/" + Computer2Space.SERVICE_NAME);
			ComputerImpl comp = new ComputerImpl(space);
			space.register(comp, comp.getId(), comp.getNumOfProcessors());
			System.out.println("Computer ready : " + comp.getId());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see system.Computer#getQueueSize()
	 */
	@Override
	public synchronized Integer getTaskQueueSize() throws RemoteException {
		return this.taskQueue.size();
	}

	/**
	 * 
	 * @return A random thread name made up of exactly three alphabets
	 */
	public static String getRandomChars() {
		char first = (char) ((new Random().nextInt(26)) + 65);
		char second = (char) ((new Random().nextInt(26)) + 65);
		char third = (char) ((new Random().nextInt(26)) + 65);
		return "" + first + second + third;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see system.Computer#startWorkers(int)
	 */
	@Override
	public void startWorkers(int numOfWorkers, int taskQueueMaxSize)
			throws RemoteException {
		this.taskQueueMaxSize = taskQueueMaxSize;
		this.sink = new ResultSink(this, taskQueueMaxSize);
		for (int i = 0; i < numOfWorkers; i++) {
			new Worker(this, sink);
		}
	}

	/**
	 * @return A task at the head of the task queue
	 */
	public synchronized Task<?> getTaskFromQueue() {
		if (!this.taskQueue.isEmpty()) {
			return this.taskQueue.remove();
		}
		return null;
	}

	@Override
	public void sendResults(Result<?> result) throws RemoteException {
		space.sendResult(result, this.id);
		
	}


	
	

}
