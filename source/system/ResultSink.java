
package system;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import api.Result;
import api.Task;
import api.Task.QueuingStatus;

/**
 * A sink for all {@link api.Result api.Result objects} generated by
 * {@link api.Task#execute() api.Task.execute()} method. Whenever a result
 * object is added to the sink, the class unpacks the result and checks if
 * subtasks (if any) can be added to the local task queue to avoid communication
 * latency.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class ResultSink implements Runnable {

	private Queue<Result<?>> resultQueue;
	private Thread t;
	private ComputerImpl comp;
	private int maxQueueSize;

	/**
	 * 
	 * @param comp
	 *            Computer object that uses this sink
	 * @param maxQueueSize
	 *            Maximum permissible size of the sink
	 */
	public ResultSink(ComputerImpl comp, int maxQueueSize) {
		this.comp = comp;
		this.maxQueueSize = maxQueueSize;
		this.resultQueue = new LinkedList<Result<?>>();
		t = new Thread(this, "ResultSink");
		t.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * Polls the result queue for results. Whenever the number of elements in
	 * the queue exceeds the maximum permissible size of the queue, the result
	 * queue is emptied and results are sent to the compute space.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Sink thread started"+ this);
		while (true) {
			if (this.getQueueSize() > 0) {
				List<Result<?>> results = new LinkedList<Result<?>>();
				for (int index = 1; index <= 5; index++) {
					Result<?> aResult;
					if ((aResult = this.takeResult()) != null) {
						results.add(aResult);
					}
				}
				// Attempt to send results to the compute space
				try {
					comp.sendResults(results);
					
				} catch (RemoteException e) {
					System.err
							.println("RemoteException occured while sending results to space");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Thread-safe to ensure that multiple {@link system.Worker system.Worker}
	 * threads can access the sink concurrently
	 * 
	 * @param aResult
	 *            Result to be added to the sink
	 */
	public synchronized void putResult(Result<?> aResult) {
		this.resultQueue.add(aResult);
		/*
		 * Unpack the result object and check if subtasks can be added to the
		 * local queue to mitigate communication latency in RMI on the compute
		 * space
		 */
		if (aResult.getSubTasks() != null) {
			for (Task<?> t : aResult.getSubTasks()) {
				try {
					if (comp.getTaskQueueSize() < comp.getTaskQueueMaxSize()) {
						t.setQueuingStatus(QueuingStatus.QUEUED);
						comp.addTask(t);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 
	 * @return Result object at the head of the queue
	 */
	private synchronized Result<?> takeResult() {
		if (!this.resultQueue.isEmpty()) {
			return this.resultQueue.remove();
		}
		return null;
	}

	/**
	 * 
	 * @return Size of queue
	 */
	private synchronized int getQueueSize() {
		return this.resultQueue.size();
	}

}
