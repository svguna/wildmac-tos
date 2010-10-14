/**
 * 
 */
package it.unitn.wildmac.demo;

import it.unitn.wildmac.WSNGateway;

import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Stefan Guna
 * 
 */
public class DemoForm extends JFrame {

	private static final long serialVersionUID = 1L;
	private ExperimentConfigPanel experimentConfigPanel = null;
	private WSNGateway gateway;
	private JPanel jContentPane = null;
	private TimelinePanel timelinePanel = null;
	private JLabel jLabel = null;
	private JPanel jAboutPane = null;

	private JPanel getJAboutPane() {
		if (jAboutPane == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			jAboutPane = new JPanel();
			jLabel = new JLabel();
			jLabel
					.setText("(C) 2010 \u015Etefan Gun\u0103, guna@disi.unitn.it");
			Font curFont = jLabel.getFont();
			jLabel.setFont(new Font(curFont.getFontName(), curFont.getStyle(),
					(int) (curFont.getSize() * 0.75)));
			jLabel.setVerticalTextPosition(JLabel.BOTTOM);
			jLabel.setHorizontalTextPosition(JLabel.LEFT);
			jAboutPane.setLayout(gridLayout);
			jAboutPane.add(jLabel, null);
			jLabel.setSize(400, curFont.getSize());

			jAboutPane.setSize(400, curFont.getSize());
		}
		return jAboutPane;
	}

	/**
	 * This is the default constructor
	 */
	public DemoForm(WSNGateway gateway) {
		super();
		this.gateway = gateway;
		initialize();
		gateway.registerConsumer(getTimelinePanel());
	}

	/**
	 * This method initializes experimentConfigPanel
	 * 
	 * @return it.unitn.wildmac.demo.ExperimentConfigPanel
	 */
	private ExperimentConfigPanel getExperimentConfigPanel() {
		if (experimentConfigPanel == null) {
			experimentConfigPanel = new ExperimentConfigPanel(gateway, this);
		}
		return experimentConfigPanel;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {

			jContentPane = new JPanel();

			jContentPane
					.setLayout(new BoxLayout(jContentPane, BoxLayout.Y_AXIS));

			jContentPane.add(getTimelinePanel());
			jContentPane.add(getExperimentConfigPanel());
			jContentPane.add(getJAboutPane());
		}
		return jContentPane;
	}

	/**
	 * This method initializes timelinePanel
	 * 
	 * @return it.unitn.wildmac.demo.TimelinePanel
	 */
	private TimelinePanel getTimelinePanel() {
		if (timelinePanel == null) {
			timelinePanel = new TimelinePanel();
		}
		return timelinePanel;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(400, 400);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		this.setTitle("WildMAC demo");
		this.setLocationRelativeTo(null);
	}

	public void reset(int period) {
		getTimelinePanel().reset(period);
	}

} // @jve:decl-index=0:visual-constraint="10,10"
