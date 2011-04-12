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
 * A one shot experiment.
 * 
 * @author Stefan Guna
 * 
 */
public class OneShotExperiment extends TimerTask implements ReportConsumer {

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
		long period, duration;
		int beacon, samples, delay;
		boolean countFromMsg = false, randomDelay = false;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			return;
		}
		try {
			period = new Integer(args[1]);
		} catch (Exception e) {
			System.err.println("Invalid protocol period specified.");
			printSyntax();
			return;
		}
		try {
			beacon = new Integer(args[2]);
		} catch (Exception e) {
			System.err.println("Invalid beacon duration specified.");
			printSyntax();
			return;
		}
		try {
			samples = new Integer(args[3]);
		} catch (Exception e) {
			System.err.println("Invalid number of samples specified.");
			printSyntax();
			return;
		}
		try {
			duration = new Integer(args[4]);
		} catch (Exception e) {
			System.err.println("Invalid experiment duration specified.");
			printSyntax();
			return;
		}

		try {
			delay = new Integer(args[5]);
		} catch (Exception e) {
			System.err.println("Invalid delay specified.");
			printSyntax();
			return;
		}
		try {
			if (args[6].equals("on"))
				countFromMsg = true;
		} catch (Exception e) {
			System.err.println("Invalid delay specified.");
			printSyntax();
			return;
		}
		try {
			if (args[7].equals("on"))
				randomDelay = true;
		} catch (Exception e) {
			System.err.println("Invalid randomicity specified.");
			printSyntax();
			return;
		}

		WSNGateway gateway = WSNGateway.getGateway(source);
		OneShotExperiment experiment = new OneShotExperiment();
		gateway.registerConsumer(experiment);

		try {
			gateway.startExperiment(period, beacon, samples, duration, delay,
					countFromMsg, randomDelay, false);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the sink.");
			System.err.println(e.getMessage());
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(experiment, duration + delay);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out
				.println("\t java "
						+ OneShotExperiment.class.getName()
						+ " SOURCE PERIOD BEACON SAMPLES DURATION DELAY CNT_MSG RND_DELAY");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long,
	 * java.util.Date)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			Date contactTime) {
		System.out.format("%d discovered %d in %d ms ( ts=%d, %d is %tc).\n",
				nodeId, neighbor, timestamp, contactTime);
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
