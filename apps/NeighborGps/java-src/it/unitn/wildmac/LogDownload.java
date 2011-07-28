/**
 * 
 */
package it.unitn.wildmac;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class LogDownload implements ReportConsumer {
	private int downloaded = 0;
	long toDownload = 0;

	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());

		String source;
		boolean flush;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			return;
		}
		try {
			flush = args[1].toLowerCase().equals("true");
		} catch (ArrayIndexOutOfBoundsException e) {
			flush = false;
		}

		WSNGateway gateway = WSNGateway.getGateway(source);
		LogDownload download = new LogDownload();
		gateway.registerConsumer(download);

		try {
			gateway.downloadFlash(flush);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the sink.");
			System.err.println(e.getMessage());
		}
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + OneShotExperiment.class.getName()
				+ " SOURCE [ERASE_LOG]");
	}

	public void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			GpsPosition gpsPosition) {
		downloaded++;
		System.out
				.format("%d discovered %d in %d ms. Last position %s %s at %s (%d ms). Estimated time %s.\n",
						nodeId, neighbor, timestamp, gpsPosition.getLatitude(),
						gpsPosition.getLongitude(), gpsPosition.getTime(),
						gpsPosition.getTimestamp(), gpsPosition.getTime()
								.convertTimestamp(timestamp));
		if (downloaded == toDownload)
			System.exit(0);
	}

	public void entriesCount(long entriesCount) {
		System.out.println("Entries: " + entriesCount);
		toDownload = entriesCount;
		if (toDownload == 0)
			System.exit(0);
	}
}
