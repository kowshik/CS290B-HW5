package system;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import api.Task;

/**
 * 
 * 
 * The multithreading design implemented by this class is that of the <a
 * href="http://en.wikipedia.org/wiki/Cilk">Cilk</a> runtime. Please read the
 * architecture of Cilk to understand the class better.
 * 
 * This class represents a Successor node in the DAG generated in the Cilk
 * runtime during the execution of a Multithreaded program.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public class Successor{

	
	private String id;
	private SpaceImpl space;
	private Task<?> task;
	private Closure aClosure;

	/**
	 * Status of this successor thread
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 * 
	 */
	

	/**
	 * 
	 * @param joinCounter
	 *            Number of missing variables in the internal Closure object
	 */
	private Successor(int joinCounter) {
	
		this.aClosure = new Closure(joinCounter);
	}

	/**
	 * 
	 * @param aTask
	 *            Task object representing the computational task of this thread
	 * @param spaceImpl
	 * @param joinCounter
	 *            Number of missing variables in the internal Closure object
	 */
	public Successor(Task<?> aTask, SpaceImpl spaceImpl, int joinCounter) {
		this(joinCounter);
		this.space = spaceImpl;
		this.task = aTask;
		this.id = task.getId();
		

	}

	
	public String getId() {
		return this.id;
	}

	/**
	 * 
	 * Closure used to store the missing arguments in <a
	 * href="http://en.wikipedia.org/wiki/Continuation-passing_style"
	 * >Continuation-passing style</a> of programming used in <a
	 * href="http://en.wikipedia.org/wiki/Cilk">Cilk</a>.
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 * 
	 */
	public class Closure {
		private List<Object> values;
		private int joinCounter;

		/**
		 * 
		 * @param joinCounter
		 *            Number of missing variables in the internal Closure object
		 */
		public Closure(int joinCounter) {
			this.joinCounter = joinCounter;
			this.values = new Vector<Object>();
		}

		/**
		 * Adds an argument to the Closure's list of values
		 * 
		 * @param value
		 */
		public void put(Object value) {
			if (value != null) {
				values.add(value);
			}
			joinCounter--;
			if (this.joinCounter == 0) {
				task.putValues(values);
				
				try {
					space.put(task);
				} catch (RemoteException e) {
					System.err.println("RemoteException inside Closure");
					e.printStackTrace();
				}
				space.removeFromWaitQ(task);
			
			}
		}

		/**
		 * 
		 * @return All values stored by the Closure
		 */
		public final List<Object> getValues() {
			return this.values;
		}
	}

	/**
	 * 
	 * @return Returns the internal closure object of this thread
	 */
	public Closure getClosure() {
		return this.aClosure;

	}
}
