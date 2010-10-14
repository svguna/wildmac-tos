/**
 * 
 */
package it.unitn.wildmac;

import it.unitn.wildmac.messages.ExperimentControl;
import it.unitn.wildmac.messages.Report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;
import net.tinyos.util.Messenger;

import org.apache.log4j.Logger;

/**
 * Abstracts the connection to the WSN sink.
 * 
 * The class is a singleton, where an unique object instance is created for each
 * serial connection to the mote. To get a reference to the class, interested
 * objects must call the static method {@code #getGateway(String)}.
 * 
 * Objects interested in receiving reports from the sink must implement the
 * {@code ReportConsumer} interface and must register as a consumer to this
 * object through {@code #registerConsumer(ReportConsumer)}.
 * 
 * To start an experiment, objects must call the {@code #startExperiment(long,
 * int, int, long, long)} method of this class.
 * 
 * @author Stefan Guna
 * 
 */
public class WSNGateway implements MessageListener, Messenger {
	private static Hashtable<String, WSNGateway> gatewayInstances = new Hashtable<String, WSNGateway>();
	public static Logger log = Logger.getLogger(WSNGateway.class.getName());

	/**
	 * Obtains a reference to the object handling the given source.
	 * 
	 * @param source
	 *            A valid packet source to be passed to {@code
	 *            net.tinyos.packet.BuildSource#makePhoenix(String, Messenger)}.
	 * @return An unique reference to the handling object.
	 */
	public static synchronized WSNGateway getGateway(String source) {
		if (gatewayInstances.get(source) == null)
			gatewayInstances.put(source, new WSNGateway(source));
		return gatewayInstances.get(source);
	}

	private List<ReportConsumer> consumers;

	private MoteIF mote;

	private WSNGateway(String source) {
		log.debug("Building phoenix on " + source);
		PhoenixSource phoenix = BuildSource.makePhoenix(source, this);
		mote = new MoteIF(phoenix);
		mote.registerListener(new Report(), this);
		consumers = new ArrayList<ReportConsumer>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tinyos.util.Messenger#message(java.lang.String)
	 */
	public void message(String msg) {
		log.debug(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tinyos.message.MessageListener#messageReceived(int,
	 * net.tinyos.message.Message)
	 */
	public void messageReceived(int nodeId, Message msg) {
		if (!(msg instanceof Report))
			log.error("Invalid message received.");

		if (nodeId == 65535)
			nodeId = 0;

		Report report = (Report) msg;
		log.info("Discovery " + nodeId + "-" + report.get_addr() + " in "
				+ report.get_timestamp() + "ms.");
		for (ReportConsumer consumer : consumers)
			consumer.neighborDiscovered(nodeId, report.get_addr(), report
					.get_timestamp());
	}

	/**
	 * Registers a new consumer interested in receiving reports from the
	 * gateway.
	 * 
	 * @param consumer
	 *            A consumer object implementing the {@code ReportConsumer}
	 *            interface.
	 */
	public void registerConsumer(ReportConsumer consumer) {
		log.trace("Registered consumer " + consumer);
		consumers.add(consumer);
	}

	/**
	 * Starts a new experiment with the following parameters.
	 * 
	 * @param period
	 *            The duration (ms) of the WildMAC protocol period.
	 * @param beacon
	 *            The duration (ms) of the beacon.
	 * @param samples
	 *            The number of samples.
	 * @param duration
	 *            The duration (ms) of the experiment.
	 * @param delay
	 *            The delay (ms) after which the experiment should be started.
	 * @param countFromMsg
	 *            Values should be counted from when the experiment starts, and
	 *            not from when the motes receive the experiment configuration
	 *            message.
	 *@param randomDelay
	 *           Nodes should wait a random delay (in the range 0 to delay)
	 *            before the experiment starts. Otherwise, the delay is fixed.
	 * @throws IOException
	 *             In case of communication error.
	 */
	public void startExperiment(long period, int beacon, int samples,
			long duration, long delay, boolean countFromMsg, boolean randomDelay)
			throws IOException {
		ExperimentControl experiment = new ExperimentControl();
		experiment.set_period(period);
		experiment.set_beacon(beacon);
		experiment.set_samples(samples);
		experiment.set_timeout(duration);
		experiment.set_delay(delay);
		if (countFromMsg)
			experiment.set_countFromMsgRcv((short) 1);
		else
			experiment.set_countFromMsgRcv((short) 0);
		if (randomDelay)
			experiment.set_randomDelay((short) 1);
		else
			experiment.set_randomDelay((short) 0);

		mote.send(MoteIF.TOS_BCAST_ADDR, experiment);
		log.info("Experiment started: period=" + period + " beacon=" + beacon
				+ " samples=" + samples + " duration=" + duration + " delay="
				+ delay + " countFromMsg=" + countFromMsg + " randomDelay="
				+ randomDelay);
	}

	/**
	 * Unregisters a consumer.
	 * 
	 * @see #registerConsumer(ReportConsumer)
	 * @param consumer
	 *            The consumer to be unregistered.
	 */
	public void unregisterConsumer(ReportConsumer consumer) {
		consumers.remove(consumer);
		log.trace("Removed consumer " + consumer);
	}
}
