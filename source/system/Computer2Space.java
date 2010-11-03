package system;

import java.rmi.RemoteException;
import java.util.List;

import api.Result;

/**
 * Compute server's abstraction of the compute space ({@link api.Client2Space
 * Space})
 */
public interface Computer2Space extends java.rmi.Remote {

	String SERVICE_NAME = "Space";

	/**
	 * {@link system.Computer Computer} objects can use this method to register
	 * with a compute space ({@link api.Client2Space Space})
	 * 
	 * @param computer
	 *            Registers a remote computer with the resource allocator that
	 *            manages the cluster operations
	 * @throws java.rmi.RemoteException
	 */
	void register(Computer computer, String id, int numOfProcessors) throws RemoteException;

	/**
	 * Used by ({@link system.Computer Computer}) Objects to communicate new
	 * shared objects to Space. The space in-turn propagates the shared object
	 * to other computer proxies, if the shared object encapsulated in the
	 * broadcast message contains a newer upper bound.
	 * 
	 * @param broadcast
	 *            Broadcast message containing a proposed new shared object and
	 *            the ID of the computer that proposed the new shared object
	 * 
	 * @throws RemoteException
	 */
	void broadcast(Broadcast broadcast) throws RemoteException;
	void sendResult(Result<?> result, String ComputerId) throws RemoteException;
}