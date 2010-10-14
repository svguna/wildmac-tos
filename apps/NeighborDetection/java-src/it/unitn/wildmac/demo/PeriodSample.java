/**
 * 
 */
package it.unitn.wildmac.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

/**
 * @author Stefan Guna
 * 
 */
public class PeriodSample extends JPanel {

	private static final double PACKET_GAP = 0.48;

	private static final double PACKET_TIME = 0.75;

	private static final double RX_RAMPDOWN = 0.92;

	private static final double RX_RAMPUP = 3.56;
	private static final double RX_TIME = 5.742916667;
	private static final long serialVersionUID = 1L;
	private static final double TX_RAMPDOWN = 1.08;
	private static final double TX_RAMPUP = 4.56;
	private static final double VBORDER = 5;
	private double beacon;
	private double period;

	private int samples;

	/**
	 * @param beacon
	 * @param period
	 * @param samples
	 */
	public PeriodSample(double period, double beacon, int samples) {
		super();
		this.beacon = beacon;
		this.period = period;
		this.samples = samples;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		setPreferredSize(new Dimension(350, 50));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		int i;
		double txStart;
		double t, t2;

		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);

		double widthRatio = getWidth() / period;
		double height = getHeight();

		double down = height - height * VBORDER / 100;
		double up = height * VBORDER / 100;

		double txWidth = PACKET_TIME * widthRatio;
		double gapWidth = PACKET_GAP * widthRatio;

		double interSampleWidth = (beacon - RX_RAMPUP - RX_RAMPDOWN - RX_TIME)
				* widthRatio;
		double rxRampupWidth = RX_RAMPUP * widthRatio;
		double rxWidth = RX_TIME * widthRatio;
		double rxRampdownWidth = RX_RAMPDOWN * widthRatio;

		double generalHeight = down - up;

		g2.setColor(Color.black);

		int packets = (int) Math.ceil(beacon / (PACKET_TIME + PACKET_GAP));

		g2.setColor(Color.orange);
		for (i = 0, txStart = TX_RAMPUP * widthRatio; i < packets - 1; i++, txStart += txWidth
				+ gapWidth)
			g2
					.fill(new Rectangle2D.Double(txStart, up, txWidth,
							generalHeight));

		g2.fill(new Rectangle2D.Double(txStart, up, txWidth, generalHeight));

		t = TX_RAMPUP * widthRatio;
		g2.setColor(Color.black);
		g2.draw(new Line2D.Double(0, down, t, up));

		t2 = t + (packets - 1) * (txWidth + gapWidth);
		g2.draw(new Line2D.Double(t, up, t2, up));

		t = t2;
		t2 += TX_RAMPDOWN * widthRatio;

		g2.draw(new Line2D.Double(t, up, t2, down));
		t = t2;

		for (i = 0; i < samples; i++) {
			t2 = t + interSampleWidth;
			g2.draw(new Line2D.Double(t, down, t2, down));
			t = t2;

			g2.setColor(Color.lightGray);
			g2.fill(new Rectangle2D.Double(t + rxRampupWidth, up, rxWidth,
					generalHeight));

			g2.setColor(Color.black);

			t2 += rxRampupWidth;
			g2.draw(new Line2D.Double(t, down, t2, up));

			t = t2;
			t2 += rxWidth;
			g2.draw(new Line2D.Double(t, up, t2, up));
			t = t2;
			t2 += rxRampdownWidth;

			g2.draw(new Line2D.Double(t, up, t2, down));
			t = t2;
			t2 += rxRampdownWidth;
		}

		g2.draw(new Line2D.Double(t, down, getWidth(), down));
	}

	/**
	 * @param beacon
	 *            the beacon to set
	 */
	public void setBeacon(double beacon) {
		this.beacon = beacon;
		repaint();
	}

	/**
	 * @param period
	 *            the period to set
	 */
	public void setPeriod(double period) {
		this.period = period;
		repaint();
	}

	/**
	 * @param samples
	 *            the samples to set
	 */
	public void setSamples(int samples) {
		this.samples = samples;
		repaint();
	}
}
