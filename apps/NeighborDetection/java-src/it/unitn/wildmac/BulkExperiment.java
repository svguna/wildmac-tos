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
public class BulkExperiment implements ReportConsumer {
	private static class ExperimentStop extends TimerTask {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			experiment.reset();

			System.out.println("Experiment " + experimentCnt + " done.");

			experimentCnt++;
			if (experimentCnt == experimentMax) {
				try {
					sendAlert("Set of experiments finished", -1);
				} catch (Exception e1) {
					System.out.println("Unable to send notification");
					System.out.println(e1.getMessage());
				}
				System.exit(0);
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			runNextExperiment();
		}
	}

	private static class SMTPAuthenticator extends javax.mail.Authenticator {
		private Properties props;

		public SMTPAuthenticator(Properties props) {
			this.props = props;
		}

		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(
					props.getProperty("mail.smtp.user"),
					props.getProperty("mail.smtp.password"));

		}
	}

	private static final int ALERT_THRESHOLD = 5;

	private static int beacon;

	private static boolean countFromMsg = false;

	private static long duration;
	private static BulkExperiment experiment;
	private static int experimentCnt = 0;
	private static int experimentMax;
	protected static final int FIXED_DURATION = 2000;

	private static WSNGateway gateways[];
	private static int nodes;
	private static long period;
	private static int problemsInRow = 0;
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
			experimentMax = new Integer(args[6]);
		} catch (Exception e) {
			System.err.println("Invalid number of experiments specified.");
			printSyntax();
			return;
		}

		try {
			experimentCnt = new Integer(args[7]);
			System.out.println("starting from experiment " + experimentCnt);
		} catch (Exception e) {
			System.out.println("starting from experiment 0");
			experimentCnt = 0;
		}
		try {
			experimentCnt = new Integer(args[7]);
			System.out.println("starting from experiment " + experimentCnt);
		} catch (Exception e) {
			System.out.println("starting from experiment 0");
			experimentCnt = 0;
		}
		try {
			nodesOffset = new Integer(args[8]);
			System.out.println("nodes counting from " + nodesOffset);
		} catch (Exception e) {
			System.out.println("nodes counting from 0");
			nodesOffset = 0;
		}

		try {
			sendAlert("Set of experiments started", -1);
		} catch (Exception e) {
			System.out.println("Unable to send notification");
			System.out.println(e.getMessage());
		}

		gateways = new WSNGateway[nodes];
		experiment = new BulkExperiment();

		for (i = 0; i < nodes; i++) {
			String source = "serial@/dev/ttyUSB" + (i + nodesOffset) + ":tmote";
			gateways[i] = WSNGateway.getGateway(source);
			gateways[i].registerConsumer(experiment);
		}

		System.out
				.println("Sleeping to allow the start of the USB connection...");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			System.out.println("Unable to sleep: " + e.getMessage());
		}
		System.out.println("Starting first experiment.");

		runNextExperiment();
	}

	private static void runNextExperiment() {
		int i = 0;

		long experimentBegin = System.currentTimeMillis();
		try {
			for (i = 0; i < nodes; i++)
				gateways[i].startExperiment(period, beacon, samples, duration,
						FIXED_DURATION + rand.nextInt((int) period),
						countFromMsg, false, true);
		} catch (Exception e) {
			System.out.println("Experiment " + experimentCnt
					+ " unable to communicate with mote " + (i + nodesOffset));
			System.out.println(e.getMessage());

			problemsInRow++;
			experimentMax++;
			duration = 60000;

			try {
				sendAlert(e.getMessage(), i);
			} catch (Exception e1) {
				System.out.println("Unable to send notification");
				System.out.println(e1.getMessage());
			}
		}

		if (System.currentTimeMillis() - experimentBegin > FIXED_DURATION) {
			System.out.println("Experiment " + experimentCnt
					+ " desynchronized");

			problemsInRow++;
			experimentMax++;
			duration = 60000;

			try {
				sendAlert("Desynchronization error", -1);
			} catch (Exception e1) {
				System.out.println("Unable to send notification");
				System.out.println(e1.getMessage());
			}
		}

		if (problemsInRow > ALERT_THRESHOLD) {
			Toolkit.getDefaultToolkit().beep();
		}

		Timer timeoutTimer = new Timer();
		timeoutTimer.schedule(new ExperimentStop(), FIXED_DURATION + duration
				+ period * 2);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out
				.println("\t java "
						+ BulkExperiment.class.getName()
						+ " NODES PERIOD BEACON SAMPLES DURATION CNT_FROM_MSG EXPERIMENTS [EXPERIMENT_START [NODE_OFFSET]]");
	}

	private static void sendAlert(String message, int moteId)
			throws FileNotFoundException, IOException, AddressException,
			MessagingException, GeneralSecurityException {
		Properties props = new Properties();

		props.load(new FileInputStream("mail.config"));

		MailSSLSocketFactory sf = new MailSSLSocketFactory();
		sf.setTrustAllHosts(true);
		props.put("mail.smtp.ssl.socketFactory", sf);

		StringBuffer buf = new StringBuffer();
		buf.append("Host: " + InetAddress.getLocalHost().getHostName() + "\n");
		buf.append("Experiment: " + experimentCnt + "\n");
		buf.append("Experiments left: " + (experimentMax - experimentCnt)
				+ "\n");
		double eta = (experimentMax - experimentCnt)
				* (FIXED_DURATION + duration + period * 2) / 1000 / 3600.;
		buf.append("Estimated time: " + new DecimalFormat("0.##").format(eta)
				+ "h\n");
		buf.append("Protocol configuration:\n");
		buf.append("\tnodes: \t\t" + nodesOffset + " -> "
				+ (nodes + nodesOffset) + "\n");
		buf.append("\tperiod: \t\t" + period + "ms\n");
		buf.append("\tbeacon: \t\t" + beacon + "ms\n");
		buf.append("\tsamples: \t" + samples + "\n");
		if (gateways != null && moteId >= 0 && moteId < gateways.length)
			buf.append("Mote: " + (moteId + nodesOffset) + "\n");
		buf.append("\n");
		buf.append(message);

		Session session = Session.getDefaultInstance(props,
				new SMTPAuthenticator(props));
		MimeMessage email = new MimeMessage(session);

		Address from[] = new Address[1];
		from[0] = new InternetAddress("WildMAC experiment <"
				+ props.getProperty("experiment.from") + ">");
		email.addFrom(from);
		email.addRecipient(Message.RecipientType.TO,
				new InternetAddress(props.getProperty("experiment.to")));
		if (props.getProperty("experiment.cc") != null)
			email.addRecipient(Message.RecipientType.CC, new InternetAddress(
					props.getProperty("experiment.cc")));
		email.setSubject("Experiment alert");
		email.setText(buf.toString());
		Transport.send(email);
	}

	private HashMap<Integer, HashMap<Integer, Long>> detections = new HashMap<Integer, HashMap<Integer, Long>>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long,
	 * java.util.Date)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			Date contactTime) {
		HashMap<Integer, Long> detected = detections.get(nodeId);
		if (detected == null) {
			detected = new HashMap<Integer, Long>();
			detections.put(nodeId, detected);
		}
		if (detected.get(neighbor) == null)
			detected.put(neighbor, timestamp);

		detected = detections.get(neighbor);
		if (detected == null) {
			detected = new HashMap<Integer, Long>();
			detections.put(neighbor, detected);
		}
		if (detected.get(nodeId) == null)
			detected.put(nodeId, timestamp);

		System.out.format("EXPERIMENT %d: %d discovered %d in %d ms (%tc).\n",
				experimentCnt, nodeId, neighbor, timestamp, contactTime);
	}

	public void reset() {
		for (Integer nodeId : new TreeSet<Integer>(detections.keySet())) {
			HashMap<Integer, Long> detected = detections.get(nodeId);
			for (Integer neighbor : new TreeSet<Integer>(detected.keySet()))
				System.out.println("EXPERIMENT " + experimentCnt + " pair: "
						+ nodeId + " " + neighbor + " in "
						+ detected.get(neighbor));
			System.out.println();
		}

		detections = new HashMap<Integer, HashMap<Integer, Long>>();
	}
}
