/**
 * 
 */
package it.unitn.wildmac;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * Downloads all contact information stored on flash.
 * 
 * @author Stefan Guna
 * 
 */
public class FlashDownload extends TimerTask implements ReportConsumer {
	/** Delay after which the mote starts sending the data stored on flash */
	public static final int DOWNLOAD_DELAY = 2000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());

		String source;
		long duration;
		boolean flush = false;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			return;
		}
		try {
			duration = new Integer(args[1]);
		} catch (Exception e) {
			System.err.println("Invalid experiment duration specified.");
			printSyntax();
			return;
		}
		try {
			flush = args[2].equals("ERASE");
		} catch (Exception e) {
		}

		WSNGateway gateway = WSNGateway.getGateway(source);
		FlashDownload experiment = new FlashDownload();
		gateway.registerConsumer(experiment);

		try {
			gateway.downloadFlash(DOWNLOAD_DELAY, flush);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the sink.");
			System.err.println(e.getMessage());
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(experiment, duration);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + FlashDownload.class.getName()
				+ " SOURCE DOWNLOAD_TIMEOUT [ERASE]");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long,
	 * java.util.Date)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			Date contactTime) {
		System.out.format("%d discovered %d in %d ms (%tc).\n", nodeId,
				neighbor, timestamp, contactTime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		System.out.println("Experiment done.");
		System.exit(0);
	}
}
