/**
 * 
 */
package it.unitn.wildmac;

import java.io.File;
import java.io.IOException;
import java.util.Random;
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
public class BulkExperiment extends TimerTask implements ReportConsumer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());

		long period, duration;
		int beacon, samples;
		boolean countFromMsg = false;
		int nodes;

		Random rand = new Random();

		try {
			nodes = new Integer(args[0]);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid number of nodes specified.");
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
			if (args[5].equals("on"))
				countFromMsg = true;
		} catch (Exception e) {
			System.err.println("Invalid counter started specified.");
			printSyntax();
			return;
		}

		WSNGateway gateways[] = new WSNGateway[nodes];
		BulkExperiment experiment = new BulkExperiment();

		for (int i = 0; i < nodes; i++) {
			String source = "serial@/dev/ttyUSB" + i + ":tmote";
			gateways[i] = WSNGateway.getGateway(source);
			gateways[i].registerConsumer(experiment);
		}

		try {
			for (int i = 0; i < nodes; i++)
				gateways[i].startExperiment(period, beacon, samples, duration,
						rand.nextInt((int) period), countFromMsg, false, true);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the mote");
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(experiment, duration + period);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + BulkExperiment.class.getName()
				+ " NODES PERIOD BEACON SAMPLES DURATION CNT_MSG");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp) {
		System.out.println(nodeId + " discovered " + neighbor + " in "
				+ timestamp + " ms.");
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
