package api;

import java.nio.ReadOnlyBufferException;
import java.rmi.RemoteException;

import system.Shared;

/**
 * Represents a raw computing resource where tasks ({@link api.Task Task}) are
 * automatically executed by registered workers as soon as they are dropped in.
 * If a worker crashes, the computation would still continue (assuming there are
 * other workers still running), since each task is executed under a
 * transaction, which would be rolled back after the worker crashed, leaving the
 * task in the space for another worker to pick up. For more information, please
 * refer <a href="http://today.java.net/pub/a/today/2005/04/21/farm.html">How to
 * build a compute farm</a>.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public interface Client2Space extends java.rmi.Remote {

	/**
	 * Name of the exposed service
	 */
	String SERVICE_NAME = "Space";

	/**
	 * The client passes a {@link api.Task Task} object representing a complex
	 * computation to the Space via this method. In principle, these
	 * {@link api.Task Task} objects are processed in parallel by workers by the
	 * Space.
	 * 
	 * This method blocks until a {@link api.Result Result} object containing
	 * the result of the task is available to be returned to the client.
	 * 
	 * @param task
	 *            task to be added to the Compute Space
	 * @param shared
	 *            Shared object to be used to broadcast messages across workers
	 *            in the compute space for this task
	 * 
	 * @throws java.rmi.RemoteException
	 *             Thrown if any read/write errors occur during the process of
	 *             adding a task to the queue in the compute space or if any
	 *             errors occur during resource allocation in the compute space
	 * 
	 * 
	 * @return One of the results obtained upon completion of individual tasks
	 * @throws java.rmi.RemoteException
	 */
	Result<?> compute(Task<?> task, Shared<?> shared)
			throws java.rmi.RemoteException;

	/**
	 * Allows the client to switch on/off latency optimization. If turned on,
	 * then the compute space tries to mitigate RMI overhead due to
	 * communication latency by using a local queue to cache tasks in the remote
	 * computer.
	 * 
	 * @param latency
	 * @throws RemoteException
	 */

	void setLatencyOptimization(boolean latency) throws RemoteException;

	/**
	 * Allows the client to switch on/off multicore processor optimization. If
	 * turned on, then the compute space attempts to leverage multiple cores for
	 * processing tasks in remote computer.
	 * 
	 * @param latency
	 * @throws RemoteException
	 */
	void setMcoreSwitch(boolean mcore) throws RemoteException;

	/**
	 * 
	 * @return Status of latency optimization switch
	 * @throws RemoteException
	 */
	boolean getLatencyOptimization() throws RemoteException;

	/**
	 * 
	 * @return Status of multi core processing switch
	 * @throws RemoteException
	 */
	boolean getMcoreSwitch() throws RemoteException;
}