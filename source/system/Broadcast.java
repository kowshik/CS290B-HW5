/**
 * 
 */
package system;

import java.io.Serializable;

/**
 * The class represents a broadcast message sent by a computer to the compute
 * space. It contains a shared object that represents the message and the ID of
 * the computer which creates that new shared object.
 * 
 * For example, in the branch-and-bound Travelling Salesman Problem, a broadcast
 * represents a new upper-bound value for the problem, which will be
 * communicated to the compute space periodically.
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public class Broadcast implements Serializable {

	private static final long serialVersionUID = 8867562356400662337L;
	private Shared<?> shared;
	private String computerId;

	/**
	 * 
	 * @param shared
	 *            Newly created shared object
	 * @param computerId
	 *            Unique ID of the computer which sends the shared object
	 */
	public Broadcast(Shared<?> shared, String computerId) {
		this.shared = shared;
		this.computerId = computerId;
	}

	/**
	 * 
	 * @return the value of the shared object
	 */
	public Shared<?> getShared() {
		return this.shared;
	}

	/**
	 * 
	 * @return Id of the computer trying to send the shared object
	 */
	public String getComputerId() {
		return computerId;
	}
}
