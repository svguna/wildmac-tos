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
public class BulkExperiment implements ReportConsumer {
	private static class ExperimentStop extends TimerTask {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			experimentCnt++;
			System.out.println("Experiment " + experimentCnt + " done.");
			if (experimentCnt == experimentMax)
				System.exit(0);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				for (int i = 0; i < nodes; i++)
					gateways[i].startExperiment(period, beacon, samples,
							duration, rand.nextInt((int) period), countFromMsg,
							false, true);
			} catch (IOException e) {
				System.err.println("Unable to communicate with the mote");
				System.err.println(e.getMessage());
			}

			Timer timeoutTimer = new Timer();
			timeoutTimer.schedule(new ExperimentStop(), duration + period * 2);
		}
	}

	static int beacon;
	static boolean countFromMsg = false;
	static long duration;
	static int experimentCnt = 0;
	static int experimentMax;
	static WSNGateway gateways[];
	static int nodes;
	static long period;
	static Random rand;

	static int samples;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());

		rand = new Random();

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

		try {
			System.out.println(args[6]);
			experimentMax = new Integer(args[6]);

		} catch (Exception e) {
			System.err.println("Invalid number of experiments specified.");
			printSyntax();
			return;
		}

		gateways = new WSNGateway[nodes];
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
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(new ExperimentStop(), duration + period * 2);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + BulkExperiment.class.getName()
				+ " NODES PERIOD BEACON SAMPLES DURATION CNT_MSG EXPERIMENTS");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp) {
		System.out.println("EXPERIMENT " + experimentCnt + ": " + nodeId
				+ " discovered " + neighbor + " in " + timestamp + " ms.");
	}
}
