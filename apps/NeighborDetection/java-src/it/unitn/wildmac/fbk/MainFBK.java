/**
 * 
 */
package it.unitn.wildmac.fbk;

import it.unitn.wildmac.ReportConsumer;
import it.unitn.wildmac.WSNGateway;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import net.tinyos.packet.PhoenixError;
import net.tinyos.util.Messenger;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author Stefan Guna
 * 
 */
public class MainFBK extends JFrame implements ActionListener, ReportConsumer,
		Messenger, PhoenixError {
	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + MainFBK.class.getName() + " SOURCE");
	}

	private static final long serialVersionUID = 1L;

	private static final List<Point2D> waypoint = Collections
			.unmodifiableList(new ArrayList<Point2D>() {
				private static final long serialVersionUID = 1L;
				{
					add(new Point2D.Float(88, 115));
					add(new Point2D.Float(88, 282));
					add(new Point2D.Float(302, 282));
					add(new Point2D.Float(302, 366));
					add(new Point2D.Float(357, 366));
					add(new Point2D.Float(357, 282));
					add(new Point2D.Float(502, 282));
					add(new Point2D.Float(502, 115));
				}
			});

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());
		final String source;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MainFBK thisClass = new MainFBK(WSNGateway.getGateway(source));
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	private JButton action = null;
	private JTextArea console = null;
	private JPanel jContentPane = null;

	private Map map = null;

	private int position = -1;

	private FileWriter log;

	/**
	 * This is the default constructor
	 * 
	 * @param wsnGateway
	 */
	public MainFBK(WSNGateway wsnGateway) {
		super();
		initialize();
		wsnGateway.registerConsumer(this);
		wsnGateway.registerMessenger(this);
		wsnGateway.registerPhoenixError(this);

		try {
			File logFile = File.createTempFile("log", ".txt",
					new File(System.getProperty("user.dir")));
			log = new FileWriter(logFile);
			getConsole().append(
					new Date() + ": created log file " + logFile + "\n");
		} catch (IOException e) {
			getConsole().append(
					new Date() + ": unable to create log file: "
							+ e.getMessage() + "\n");
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String message = new Date().toString() + ": ";
		reachedCheckpoint();
		message = message + "at checkpoint " + position + ", moving towards "
				+ getNext() + "\n";
		getAction().setText(
				"Move to checkpoint " + getNext() + " and click me!");
		logMessage(message);
	}

	@Override
	public void error(IOException arg0) {
		String message = new Date().toString() + ": exception "
				+ arg0.getMessage() + "\n";
		logMessage(message);
	}

	private void logMessage(String message) {
		getConsole().append(message);
		try {
			log.write(message);
			log.flush();
		} catch (IOException e) {
			getConsole().append(
					new Date() + ": unable to write to log: " + e.getMessage()
							+ "\n");
		}
	}

	private JButton getAction() {
		if (action == null) {
			action = new JButton();
			action.setFont(new Font("Dialog", Font.PLAIN, 24));
			action.setText("Move to START and click me!");
			action.addActionListener(this);
		}
		return action;
	}

	private JTextArea getConsole() {
		if (console == null) {
			console = new JTextArea(20, 80);
			console.setAutoscrolls(true);
			console.setEditable(false);
			console.setBackground(Color.BLACK);
			console.setForeground(Color.WHITE);
			DefaultCaret caret = (DefaultCaret) console.getCaret();
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		}
		return console;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
		}
		return jContentPane;
	}

	private Map getMap() {
		if (map == null) {
			GraphicsEnvironment env = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			map = new Map(env.getMaximumWindowBounds().width, 88, 115);
		}
		return map;
	}

	private int getNext() {
		int next = position + 1;
		if (next == waypoint.size())
			next = 0;
		return next;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("FBK experiment");
		this.setLocationRelativeTo(null);
		GraphicsEnvironment env = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		this.setSize(env.getMaximumWindowBounds().width,
				env.getMaximumWindowBounds().height);

		JScrollPane consoleScroll = new JScrollPane(getConsole());

		JPanel consoleHolder = new JPanel();
		consoleHolder.setLayout(new BorderLayout());
		// consoleHolder.add(new JLabel("Mote data:"), BorderLayout.NORTH);
		consoleHolder.add(consoleScroll, BorderLayout.CENTER);

		JPanel mapHolder = new JPanel();
		mapHolder.setLayout(new BoxLayout(mapHolder, BoxLayout.Y_AXIS));
		mapHolder.add(getAction());
		mapHolder.add(getMap());

		getJContentPane().add(mapHolder, BorderLayout.NORTH);
		getJContentPane().add(consoleHolder, BorderLayout.CENTER);
		setContentPane(getJContentPane());
		setTitle("FBK Experiment");
	}

	@Override
	public void message(String arg0) {
		String message = new Date().toString() + ": " + arg0 + "\n";
		logMessage(message);
	}

	@Override
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp,
			Date contactTime) {
		String message = new Date() + ": " + nodeId + " detected " + neighbor
				+ " (" + timestamp + ")\n";
		logMessage(message);
	}

	private void reachedCheckpoint() {
		position++;
		if (position == waypoint.size())
			position = 0;
		int next = getNext();
		getMap().setDirection(position, next, waypoint.get(position),
				waypoint.get(next));
	}
} // @jve:decl-index=0:visual-constraint="45,35"
