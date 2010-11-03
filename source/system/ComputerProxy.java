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

	public String getId() {
		return id;
	}

	public synchronized void setShared(Shared<?> newShared)
			throws RemoteException {
		compObj.setShared(newShared);
	}

	public List<Task<?>> getTaskQueue() {

		return queuedTasks;

	}

	public void addTaskToQueue(Task<?> task) {
		queuedTasks.add(task);
	}

	public Computer getCompObj() {
		return this.compObj;

	}

	public synchronized Task<?> getTaskFromQueue(String id) {

		for (int index = 0; index < queuedTasks.size(); index++) {
			Task<?> t = queuedTasks.get(index);
			if (t.getId().equals(id)) {
				return t;
			}
		}
		return null;

	}

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
