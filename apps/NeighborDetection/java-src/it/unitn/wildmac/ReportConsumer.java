/**
 * 
 */
package it.unitn.wildmac;

/**
 * Consumer of events originating from the WSN.
 * 
 * @author Stefan Guna
 * 
 */
public interface ReportConsumer {
	/**
	 * Called when the sink detects a contact.
	 * 
	 * @param nodeId
	 *            ID of the sink.
	 * @param neighbor
	 *            The neighbor that has been discovered.
	 * @param timestamp
	 *            The time (counting in ms since the start of the experiment)
	 *            when the neighbor was discovered.
	 */
	void neighborDiscovered(int nodeId, int neighbor, long timestamp);
}
