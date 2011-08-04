package it.unitn.wildmac.fbk;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Map extends JPanel {
	private static final long serialVersionUID = 1L;
	private Integer expStartX = null;
	private Integer expStartY = null;
	private int offset;

	private Point2D start, stop;

	private int startId;

	private int stopId;

	public Map(int width, int expStart, int expStop) {
		setMinimumSize(new Dimension(592, 400));
		setPreferredSize(new Dimension(592, 400));
		this.expStartX = expStart;
		this.expStartY = expStop;
		offset = (width - 592) / 2;
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);

		try {
			paintMap(g2);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (start != null || stop != null)
			paintDirection(g2);
		else
			paintStart(g2);
	}

	private void paintDirection(Graphics2D g2) {
		g2.setColor(Color.red);
		Line2D line = new Line2D.Float(start, stop);

		BasicStroke axisStroke = new BasicStroke(4, BasicStroke.CAP_SQUARE,
				BasicStroke.JOIN_ROUND);
		g2.setStroke(axisStroke);

		g2.draw(line);

		Polygon arrowHead = new Polygon();
		arrowHead.addPoint(0, 7);
		arrowHead.addPoint(-7, -7);
		arrowHead.addPoint(7, -7);

		AffineTransform tx = new AffineTransform();
		tx.setToIdentity();
		double angle = Math.atan2(line.getY2() - line.getY1(), line.getX2()
				- line.getX1());
		tx.translate(line.getX2(), line.getY2());
		tx.rotate((angle - Math.PI / 2d));

		Graphics2D g = (Graphics2D) g2.create();
		g.setTransform(tx);
		g.fill(arrowHead);
		g.dispose();

		int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
		int ftSize = (int) Math.round(24.0 * screenRes / 72.0);
		Font lf = new Font("Arial", Font.BOLD, ftSize);
		FontMetrics lfm = g2.getFontMetrics(lf);
		g2.setFont(lf);
		g2.setColor(Color.blue);

		String start = new Integer(startId).toString();
		String stop = new Integer(stopId).toString();
		g2.drawString(start, (int) (line.getX1() - lfm.stringWidth(start) / 2),
				(int) line.getY1() + ftSize / 2);
		g2.drawString(stop, (int) (line.getX2() - lfm.stringWidth(stop) / 2),
				(int) line.getY2() + ftSize / 2);
	}

	private void paintMap(Graphics2D g2) throws IOException {
		BufferedImage img = ImageIO.read(new File("map.jpg"));
		g2.drawImage(img, offset, 0, null);
	}

	private void paintStart(Graphics2D g2) {
		int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
		int ftSize = (int) Math.round(12.0 * screenRes / 72.0);
		Font lf = new Font("Arial", Font.BOLD, ftSize);
		FontMetrics lfm = g2.getFontMetrics(lf);
		g2.setFont(lf);
		g2.setColor(Color.blue);

		g2.drawString("START", expStartX + offset - lfm.stringWidth("START")
				/ 2, expStartY + ftSize / 2);

	}

	public void setDirection(int startId, int stopId, Point2D start,
			Point2D stop) {
		this.startId = startId;
		this.stopId = stopId;
		this.start = new Point2D.Double(start.getX() + offset, start.getY());
		this.stop = new Point2D.Double(stop.getX() + offset, stop.getY());
		repaint();
	}
}
