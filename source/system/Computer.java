package system;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import api.Result;
import api.Task;

/**
 * Defines a computer that can execute a {@link api.Task Task}. using <a
 * href="http://en.wikipedia.org/wiki/Divide_and_conquer_algorithm">Divide and
 * conquer algorithm</a>.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public interface Computer extends Remote {

	/**
	 * @param listOfTasks
	 *            Accepts a list of tasks to be executed using the generic <a
	 *            href
	 *            ="http://en.wikipedia.org/wiki/Divide_and_conquer_algorithm"
	 *            >Divide and conquer</a> task on a remote machine
	 * 
	 * @throws java.rmi.RemoteException
	 */
	void addTasks(List<Task<?>> listOfTasks) throws RemoteException;

	/**
	 * Sends a new value of the shared object to the compute space
	 * 
	 * @param proposedShared
	 *            A new proposed value of the ({@link system.Shared Shared})
	 *            object
	 * @throws RemoteException
	 */
	boolean broadcast(Shared<?> proposedShared) throws RemoteException;

	/**
	 * Sets the internal shared object which is present in each computer
	 * 
	 * @param proposedShared
	 *            New shared object
	 * @throws RemoteException
	 */
	void setShared(Shared<?> proposedShared) throws RemoteException;

	/**
	 * 
	 * @return The shared object stored by the Computer
	 * @throws RemoteException
	 */
	Shared<?> getShared() throws RemoteException;


	/**
	 * 
	 * @return Returns the size of the computer's queue at runtime
	 * 
	 * @throws RemoteException
	 */
	Integer getTaskQueueSize() throws RemoteException;

	/**
	 * This method starts worker threads in the remote computer. The number of
	 * worker threads is decided by the compute space.
	 * 
	 * @param numOfWorkers
	 *            Number of worker threads to be started in the remote computer
	 * @throws RemoteException
	 */
	void startWorkers(int numOfWorkers,  int taskQueueMaxSize) throws RemoteException;
	
	
	void sendResults(List<Result<?>> results)throws RemoteException;
	
}
