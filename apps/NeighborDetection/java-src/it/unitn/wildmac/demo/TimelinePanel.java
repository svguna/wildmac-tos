/**
 * 
 */
package it.unitn.wildmac.demo;

import it.unitn.wildmac.ReportConsumer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;

/**
 * @author Stefan Guna
 * 
 */
public class TimelinePanel extends JPanel implements ReportConsumer {
	private class ValueComparator implements Comparator<Integer> {
		private Map<Integer, Long> data;

		public ValueComparator(Map<Integer, Long> data) {
			this.data = data;
		}

		public int compare(Integer o1, Integer o2) {
			Long e1 = data.get(o1);
			Long e2 = data.get(o2);
			return e1.compareTo(e2);
		}
	}

	private static final long serialVersionUID = 1L;

	private HashMap<Integer, Long> contacts; // @jve:decl-index=0:

	private int protocolPeriod;

	/**
	 * This is the default constructor
	 */
	public TimelinePanel() {
		super();
		contacts = new HashMap<Integer, Long>();
		initialize();
	}

	private long getMaxContact() {
		long max = 0;
		for (Long timestamp : contacts.values())
			if (timestamp > max)
				max = timestamp;
		return max;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(400, 100);
		this.setPreferredSize(new Dimension(400, 100));
		setBackground(Color.white);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.wildmac.ReportConsumer#neighborDiscovered(int, int, long)
	 */
	public void neighborDiscovered(int nodeId, int neighbor, long timestamp) {
		contacts.put(new Integer(neighbor), new Long(timestamp));
		repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);

		if (contacts.isEmpty())
			return;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();

		int ftSize = (int) Math.round(12.0 * screenRes / 72.0);
		Font fontTicks = new Font("Arial", Font.PLAIN, ftSize);
		FontMetrics ftm = g2.getFontMetrics(fontTicks);

		int fcSize = (int) Math.round(10.0 * screenRes / 72.0);
		Font fontContacts = new Font("Arial", Font.PLAIN, fcSize);
		FontMetrics fcm = g2.getFontMetrics(fontContacts);

		long max = getMaxContact();

		if (max % protocolPeriod != 0)
			max = (max / protocolPeriod + 1) * protocolPeriod;

		g2.setColor(Color.black);
		Line2D axis = new Line2D.Float(10, 80, 380, 80);
		BasicStroke axisStroke = new BasicStroke(2, BasicStroke.CAP_SQUARE,
				BasicStroke.JOIN_ROUND);
		g2.setStroke(axisStroke);
		g2.draw(axis);

		g2.setFont(fontTicks);
		g2.drawString("0", 10 - ftm.stringWidth("0") / 2, 80 + ftSize + 2);
		g2.drawString(new Long(max).toString(), 380 - ftm.stringWidth(new Long(
				max).toString()) / 2, 80 + ftSize + 2);

		g2.setFont(fontContacts);
		BasicStroke contactStroke = new BasicStroke((float) 0.5,
				BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);
		g2.setStroke(contactStroke);

		int levelNo = 80 / (8 + fcSize);
		long levels[] = new long[levelNo];
		for (int i = 0; i < levelNo; i++)
			levels[i] = 0;

		TreeMap<Integer, Long> sortedContacts = new TreeMap<Integer, Long>(
				new ValueComparator(contacts));
		sortedContacts.putAll(contacts);

		g2.setColor(Color.red);
		for (Integer nodeId : sortedContacts.keySet()) {
			int level = 0;
			long x = 10 + 370 * sortedContacts.get(nodeId) / max;
			String node = nodeId.toString();
			long left = x - fcm.stringWidth(node) / 2;

			for (level = 0; level < levelNo; level++)
				if (left > levels[level])
					break;

			long lineHeight = level * (8 + fcSize) + 10;

			Line2D contactLine = new Line2D.Float(x, 80 - lineHeight, x, 80);
			Line2D underline = new Line2D.Float(left, 80 - lineHeight, left
					+ fcm.stringWidth(node), 80 - lineHeight);
			g2.draw(contactLine);
			g2.draw(underline);
			g2.drawString(node, x - fcm.stringWidth(node) / 2, 78 - lineHeight);
			levels[level] = x + fcm.stringWidth(node) / 2 + 2;
		}

		float dash[] = { 3.0f };
		BasicStroke periodStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, 3f, dash, 0.0f);
		g2.setStroke(periodStroke);
		g2.setColor(Color.gray);
		for (int i = 0; i <= max / protocolPeriod; i++) {
			long x = 10 + 370 * i * protocolPeriod / max;
			Line2D periodLine = new Line2D.Float(x, 20, x, 80);
			g2.draw(periodLine);
		}
	}

	/**
	 * @param period
	 * 
	 */
	public void reset(int period) {
		this.protocolPeriod = period;
		contacts = new HashMap<Integer, Long>();
		repaint();
	}
}
