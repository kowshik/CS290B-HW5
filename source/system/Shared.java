package system;

/**
 * An interface abstracting properties of an object thats shared among all of a
 * computation's unfinished tasks by the compute space. Its value, when changed
 * by any task, is propagated with best effort to all unfinished tasks by the
 * compute space.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 * 
 */
public interface Shared<T> {
	/**
	 * 
	 * @return Returns true if the argument is older than this shared object
	 * @return Shared object to compare
	 */
	boolean isNewerThan(Shared<?> existingShared);

	/**
	 * 
	 * @return Returns the object that is being shared
	 */
	T get();
	
}
