/**
 * 
 */
package it.unitn.wildmac;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import com.sun.mail.util.MailSSLSocketFactory;

/**
 * A one shot experiment.
 * 
 * @author Stefan Guna
 * 
 */
public class PaichExperiment extends TimerTask implements ReportConsumer {

	private static int beacon;

	private static boolean countFromMsg = false;

	private static long duration;
	private static PaichExperiment experiment;
	protected static final int FIXED_DURATION = 2000;
	private static final int PAICH_BASE_PORT = 50001;

	private static WSNGateway gateways[];
	private static int nodes;
	private static long period;
	private static Random rand;
	private static int samples;
	private static int nodesOffset = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i;
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
			nodesOffset = new Integer(args[8]) + PAICH_BASE_PORT;
			System.out.println("nodes counting from " + nodesOffset);
		} catch (Exception e) {
			System.out.println("nodes counting from 0");
			nodesOffset = PAICH_BASE_PORT;
		}

		gateways = new WSNGateway[nodes];
		experiment = new PaichExperiment();

		for (i = 0; i < nodes; i++) {
			String source = "sf@paich.fbk.eu:" + (i + nodesOffset);
			gateways[i] = WSNGateway.getGateway(source);
			gateways[i].registerConsumer(experiment);
		}

		System.out.println("Starting  experiment.");
		runExperiment();
	}

	private static void runExperiment() {
		int i = 0;

		long experimentBegin = System.currentTimeMillis();
		try {
			for (i = 0; i < nodes; i++)
				gateways[i].startExperiment(period, beacon, samples, duration,
						FIXED_DURATION + rand.nextInt((int) period),
						countFromMsg, false, true);
		} catch (Exception e) {
			System.out.println("Unable to talk to sf@paich.fbk.eu:"
					+ (i + nodesOffset));
			System.out.println(e.getMessage());
		}

		if (System.currentTimeMillis() - experimentBegin > FIXED_DURATION)
			System.out.println("Experiment desynchronized");

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(experiment, FIXED_DURATION + duration + period
				* 2);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out
				.println("\t java "
						+ PaichExperiment.class.getName()
						+ " NODES PERIOD BEACON SAMPLES DURATION CNT_FROM_MSG [NODE_OFFSET]");
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
