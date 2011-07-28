/**
 * 
 */
package it.unitn.wildmac;

public interface ReportConsumer {
	void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			GpsPosition position);

	void entriesCount(long get_entries);
}
