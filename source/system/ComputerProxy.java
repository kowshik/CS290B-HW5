package system;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import api.Task;

/**
 * For every {@link system.Computer Computer} instance that registers with the
 * compute space ({@link api.Client2Space Space}), there is a proxy maintained
 * by the space. This allows the compute space to maintain multiple threads for
 * each instance of registered {@link system.Computer Computer} objects. This
 * class is responsible for maintaining all {@link api.Task Task} objects queued
 * in the registered remote computers.
 * 
 * Each proxy maintains a queue of tasks that are currently being executed in
 * the underlying remote machine or are queued locally in the remote
 * machine. These tasks can either represent the Divide phase or the Conquer
 * phase in the <a
 * href="http://en.wikipedia.org/wiki/Divide_and_conquer_algorithm">Divide and
 * conquer algorithm</a>.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class ComputerProxy {
	private Computer compObj;
	private String id;
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
	public ComputerProxy(Computer compObj, String proxyId)
			throws RemoteException {
		this.compObj = compObj;
		this.id = proxyId;
		compObj.setId(this.id);
		this.queuedTasks = new Vector<Task<?>>();

	}

	/**
	 * 
	 * @return ID of this computer proxy
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the shared variable of the internal remote computer
	 * 
	 * @param newShared
	 * @throws RemoteException
	 */
	public synchronized void setShared(Shared<?> newShared)
			throws RemoteException {
		compObj.setShared(newShared);
	}

	/**
	 * 
	 * @return Size of task queue
	 */

	public List<Task<?>> getTaskQueue() {

		return queuedTasks;

	}

	/**
	 * 
	 * @param task
	 *            Task to be appended to internal task queue
	 */
	public void addTaskToQueue(Task<?> task) {
		queuedTasks.add(task);
	}

	/**
	 * 
	 * @return Remote computer object
	 */

	public Computer getCompObj() {
		return this.compObj;

	}

	/**
	 * 
	 * @param id
	 *            ID of task to be fetched from internal task queue
	 * @return
	 */

	public synchronized Task<?> getTaskFromQueue(String id) {

		for (int index = 0; index < queuedTasks.size(); index++) {
			Task<?> t = queuedTasks.get(index);
			if (t.getId().equals(id)) {
				return t;
			}
		}
		return null;

	}

	/**
	 * 
	 * @param id
	 *            ID of task to be removed from internal task queue
	 */

	public synchronized void removeTaskFromQueue(String id) {
		Task<?> t = null;
		for (int index = 0; index < queuedTasks.size(); index++) {
			t = queuedTasks.get(index);
			if (t.getId().equals(id)) {
				break;
			}
		}
		if (t != null) {
			queuedTasks.remove(t);
		}
	}
}
