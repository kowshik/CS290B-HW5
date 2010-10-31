package tasks;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import system.Computer;
import system.Shared;
import api.Task;

/**
 * Models properties of a generic divide and conquer task
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public abstract  class TaskBase<T> implements Task<T>, Serializable {

	private static final long serialVersionUID = -139155829609653917L;

	private Status status;
	private QueuingStatus queueingStatus;
	private String parentId;
	private String taskId;
	private long startTime;
	protected Computer computer;

	

	protected static final String ID_DELIM = "-";
	protected static final int DEFAULT_TASK_LEVEL = 0;
	protected static final int DEFAULT_CHILD_LEVEL = 0;
	protected static final String DEFAULT_TASK_ID = DEFAULT_TASK_LEVEL
			+ ID_DELIM + DEFAULT_CHILD_LEVEL;

	public TaskBase(String taskId, String parentId, Status status, Task.QueuingStatus queueingStatus,
			long startTime) {
		setId(taskId);
		setParentId(parentId);
		setStatus(status);
		setStartTime(startTime);
		setQueuingStatus(queueingStatus);
	}

	@Override
	public Task.Status getStatus() {
		return status;
	}

	@Override
	public void setStatus(Task.Status s) {
		this.status = s;
	}

	@Override
	public String getParentId() {
		return this.parentId;
	}

	@Override
	public void setParentId(String id) {
		this.parentId = id;
	}

	@Override
	public String getId() {
		return this.taskId;
	}

	@Override
	public void setId(String taskId) {
		this.taskId = taskId;
	}

	@Override
	public List<String> getChildIds() {
		String[] parts = this.getId().split(ID_DELIM);
		int newLevel = Integer.parseInt(parts[0]) + 1;
		List<String> ids = new Vector<String>();
		int childBase = Integer.parseInt(parts[1])
				* this.getDecompositionSize();
		for (int childNumber = childBase; childNumber < childBase
				+ this.getDecompositionSize(); childNumber++) {
			String childId = newLevel + ID_DELIM + childNumber;
			ids.add(childId);
		}
		return ids;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getStartTime() {
		return startTime;
	}

	protected void init(Task.Status s, String taskId, String parentId) {
		this.setStatus(s);
		this.setId(taskId);
		this.setParentId(parentId);
	}

	protected int getTaskLevel() {
		String[] parts = this.getId().split(ID_DELIM);
		int level = Integer.parseInt(parts[0]);
		return level;
	}


	@Override
	public Object getShared() throws RemoteException {
		return computer.getShared();
	}

	
	@Override
	public void setShared(Shared<?> shared) throws RemoteException {
		computer.broadcast(shared);
	}

	
	@Override
	public void setComputer(Computer computer) {
		this.computer=computer;
	}
	
	@Override
	public Computer getComputer() {
		return this.computer;
	}
	
	/* (non-Javadoc)
	 * @see api.Task#getQueuingStatus()
	 */
	@Override
	public api.Task.QueuingStatus getQueuingStatus() {
		return this.queueingStatus;
	}

	/* (non-Javadoc)
	 * @see api.Task#getQueuingStatus(api.Task.QueuingStatus)
	 */
	@Override
	public void setQueuingStatus(api.Task.QueuingStatus status) {
		this.queueingStatus=status;
	}
}
