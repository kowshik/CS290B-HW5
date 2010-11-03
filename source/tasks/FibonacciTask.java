package tasks;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import system.ResultImpl;
import system.SpaceRunnable;
import api.Result;
import api.Task;

/**
 * Computes the Nth fibonacci number
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public class FibonacciTask extends TaskBase<Integer> implements Serializable, SpaceRunnable {

	private static final long serialVersionUID = -9046135328040176063L;
	private static final int NUMBER_OF_CHILDREN = 2;
	private int n;
	private Vector<Integer> values;

	/**
	 * 
	 * @param n
	 *            Nth fibonacci number to be computed
	 */
	public FibonacciTask(int n) {
		super(DEFAULT_TASK_ID, DEFAULT_TASK_ID, Task.Status.DECOMPOSE,
				Task.QueuingStatus.NOT_QUEUED, System.currentTimeMillis());
		this.n = n;
	}

	private FibonacciTask(int n, Task.Status s, String taskId, String parentId) {
		this(n);
		init(s, taskId, parentId);
	}

	/**
	 * Implements the decompose phase of fibonacci generation
	 */
	private Result<Integer> decompose() {
		Result<Integer> r = new ResultImpl<Integer>(this.getId(),
				this.getParentId());
		if (n < 2) {
			r.setValue(n);
			return r;
		}
		List<Task<Integer>> subTasks = new Vector<Task<Integer>>();
		int decrement = 1;
		for (String id : this.getChildIds()) {
			subTasks.add(new FibonacciTask(n - decrement,
					Task.Status.DECOMPOSE, id, this.getId()));
			decrement++;
		}
		r.setSubTasks(subTasks);
		return r;
	}

	/**
	 * Implements the conquer phase of fibonacci generation
	 */
	private Result<Integer> compose() {
		Result<Integer> r = new ResultImpl<Integer>(this.getId(),
				this.getParentId());
		int sum = 0;
		for (Integer value : this.getValues()) {
			sum += value;
		}
		r.setValue(sum);
		return r;
	}

	/**
	 * Number of subtasks created in each stage of recursion
	 */
	@Override
	public int getDecompositionSize() {
		return FibonacciTask.NUMBER_OF_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#execute()
	 */
	@Override
	public Result<?> execute() {
		if (this.getStatus() == Task.Status.DECOMPOSE) {
			return this.decompose();
		}
		if (this.getStatus() == Task.Status.COMPOSE) {
			return this.compose();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#putValues(java.util.List)
	 */
	@Override
	public void putValues(List<?> values) {
		this.values = new Vector<Integer>();
		for (Object o : values) {
			Integer val = (Integer) o;
			this.values.add(val);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see api.Task#getValues()
	 */
	@Override
	public List<Integer> getValues() {

		return values;
	}

	}
