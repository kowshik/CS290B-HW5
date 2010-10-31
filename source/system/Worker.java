
package system;

import api.Result;
import api.Task;

/**
 * 
 * A worker thread to execute tasks. The {@link system.Computer Computer} runs
 * many threads of this type to leverage a multi-processor infrastructure.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class Worker implements Runnable {

	private Thread t;
	private ComputerImpl comp;
	private String id;
	private ResultSink sink;

	/**
	 * @param comp
	 *            Computer owning this thread
	 * @param sink
	 *            Result sink to which results can be written
	 */
	public Worker(ComputerImpl comp, ResultSink sink) {
		this.comp = comp;
		this.sink = sink;
		this.setId("Worker_" + comp.getId() + "_"
				+ ComputerImpl.getRandomChars());
		t = new Thread(this, this.getId());
		t.start();
	}

	private void setId(String id) {
		this.id = id;
	}

	private String getId() {
		return this.id;
	}

	/*
	 * 
	 * Polls the task queue for tasks. If a task is available, it is executed
	 * and results are written to the sink (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Started worker thread : " + this.getId());
		while (true) {
			Task<?> aTask = comp.getTaskFromQueue();
			if (!(aTask == null)) {
				Result<?> r = aTask.execute();
				sink.putResult(r);
			}
		}
	}

}
