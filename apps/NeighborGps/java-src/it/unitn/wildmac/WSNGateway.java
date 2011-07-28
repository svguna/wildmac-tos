/**
 * 
 */
package it.unitn.wildmac;

import it.unitn.wildmac.messages.ExperimentControl;
import it.unitn.wildmac.messages.LogStats;
import it.unitn.wildmac.messages.Report;
import it.unitn.wildmac.messages.ReportControl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixError;
import net.tinyos.packet.PhoenixSource;
import net.tinyos.util.Messenger;

import org.apache.log4j.Logger;

public class WSNGateway implements MessageListener, Messenger, PhoenixError {
	private static Hashtable<String, WSNGateway> gatewayInstances = new Hashtable<String, WSNGateway>();
	public static Logger log = Logger.getLogger(WSNGateway.class.getName());

	public static synchronized WSNGateway getGateway(String source) {
		if (gatewayInstances.get(source) == null)
			gatewayInstances.put(source, new WSNGateway(source));
		return gatewayInstances.get(source);
	}

	private List<ReportConsumer> consumers;

	private MoteIF mote;

	private Random random;

	private WSNGateway(String source) {
		log.debug("Building phoenix on " + source);
		PhoenixSource phoenix = BuildSource.makePhoenix(source, this);
		phoenix.setPacketErrorHandler(this);
		mote = new MoteIF(phoenix);
		mote.registerListener(new Report(), this);
		mote.registerListener(new LogStats(), this);
		consumers = new ArrayList<ReportConsumer>();
		random = new Random();
	}

	public void downloadFlash(boolean flush) throws IOException {
		ReportControl reportControl = new ReportControl();
		reportControl.set_flush((flush) ? 1 : 0);

		log.info("Downloading flash log...");

		mote.send(MoteIF.TOS_BCAST_ADDR, reportControl);

	}

	public void error(IOException e) {
		log.warn("Serial exception: " + e.getMessage());
	}

	public void message(String msg) {
		log.debug(msg);
	}

	public void messageReceived(int nodeId, Message msg) {
		if (!(msg instanceof Report) && !(msg instanceof LogStats))
			log.error("Invalid message received.");

		switch (msg.amType()) {
		case Report.AM_TYPE:
			Report report = (Report) msg;
			for (ReportConsumer consumer : consumers)
				consumer.neighborDiscovered(report.get_src(),
						report.get_addr(), report.get_timestamp(),
						new GpsPosition(report));
			break;
		case LogStats.AM_TYPE:
			LogStats logStats = (LogStats) msg;
			for (ReportConsumer consumer : consumers)
				consumer.entriesCount(logStats.get_entries());
			break;
		}
	}

	public void registerConsumer(ReportConsumer consumer) {
		log.trace("Registered consumer " + consumer);
		consumers.add(consumer);
	}

	public void startExperiment(long period, int beacon, int samples,
			int power, int node) throws IOException {
		long rnd = random.nextLong();
		log.info("Using seed " + rnd);

		ExperimentControl experiment = new ExperimentControl();
		experiment.set_node(node);
		experiment.set_period(period);
		experiment.set_beacon(beacon);
		experiment.set_samples(samples);
		experiment.set_power(power);
		experiment.set_seed(rnd);

		mote.send(MoteIF.TOS_BCAST_ADDR, experiment);

		log.info("Node " + node + " configured: period=" + period + " beacon="
				+ beacon + " samples=" + samples + " power=" + power);
	}

	public void unregisterConsumer(ReportConsumer consumer) {
		consumers.remove(consumer);
	}
}
