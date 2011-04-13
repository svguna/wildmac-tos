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
public class RadioControlledExperiment extends TimerTask implements
		ReportConsumer {

	public static final int DELAY = 2000;

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
		int beacon, samples;

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

		WSNGateway gateway = WSNGateway.getGateway(source);
		RadioControlledExperiment experiment = new RadioControlledExperiment();
		gateway.registerConsumer(experiment);

		try {
			gateway.startExperiment(period, beacon, samples, duration, DELAY,
					true, true, false);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the sink.");
			System.err.println(e.getMessage());
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(experiment, duration + DELAY);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java "
				+ RadioControlledExperiment.class.getName()
				+ " SOURCE PERIOD BEACON SAMPLES DURATION");
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
