package client;

import java.rmi.RemoteException;

import api.Client2Space;

/**
 * Interface that defines a job to be executed remotely.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public abstract class Job {

	/**
	 * Executes the job in a compute space represented internally by a
	 * {@link api.Task Task} object. Collects the result returned by the compute
	 * space and stores them.
	 * 
	 * @param space
	 *            Compute space to which {@link api.Task Task} objects should be
	 *            sent for execution
	 * @throws RemoteException
	 *             If the compute space throws RemoteException while writing
	 *             tasks or reading results, then the exception is in turn thrown by this
	 *             method
	 */
	public abstract void executeJob(Client2Space space) throws RemoteException;

	/**
	 * Transforms values returned by {@link api.Result Result} object into
	 * something relevant to the subclass implementing this interface.
	 * 
	 * @return A container with values from all {@link api.Result Result}
	 *         objects
	 */
	public abstract Object getAllResults();

}
