/**
 * 
 */
package it.unitn.wildmac.demo;

import it.unitn.wildmac.WSNGateway;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Stefan Guna
 * 
 */
public class ExperimentConfigPanel extends JPanel {
	private class ExperimentTimer extends TimerTask {
		private int firedCount = 1;
		private Timer timer;

		public ExperimentTimer(Timer timer) {
			this.timer = timer;
		}

		@Override
		public void run() {
			if (firedCount == experimentDuration.getValue()
					+ Demo.EXPERIMENT_DELAY / 1000) {
				stopExperiment();
				return;
			}
			experimentProgress.setValue(firedCount);
			firedCount++;
		}

		private void stopExperiment() {
			timer.cancel();
			experimentDuration.setEnabled(true);
			protocolSlider.setEnabled(true);
			beaconDuration.setEnabled(true);
			samplesCount.setEnabled(true);
			startButton.setEnabled(true);

			experimentProgress.setVisible(false);
		}

	}

	public static final int MAX_BEACON = 500;
	public static final int MAX_SAMPLES = 99;
	private static final long serialVersionUID = 1L;
	private JSlider beaconDuration = null;
	private JLabel beaconValue = null;
	private JLabel behavior = null;
	private DemoForm demoForm;
	private JSlider experimentDuration = null;
	private JProgressBar experimentProgress = null;
	private JLabel experimentValue = null;
	private WSNGateway gateway;
	private JLabel jLabel = null;
	private JLabel jLabel1 = null;
	private JLabel jLabel2 = null;
	private JLabel jLabel3 = null;

	private JPanel jProgressPane = null;

	private JLabel periodValue = null;

	private JSlider protocolSlider = null;

	private JSlider samplesCount = null;
	private JLabel samplesValue = null;

	private JButton startButton = null;

	/**
	 * This is the default constructor
	 */
	public ExperimentConfigPanel(WSNGateway gateway, DemoForm demoForm) {
		super();
		this.gateway = gateway;
		this.demoForm = demoForm;
		initialize();
	}

	/**
	 * This method initializes beaconDuration
	 * 
	 * @return javax.swing.JSlider
	 */
	private JSlider getBeaconDuration() {
		if (beaconDuration == null) {
			beaconDuration = new JSlider();

			beaconDuration.setMinimum(20);
			beaconDuration.setValue(100);
			beaconDuration.setMaximum(500);

			beaconDuration.setPaintTicks(true);
			beaconDuration.setMajorTickSpacing(50);
			beaconDuration.setMinorTickSpacing(10);
			beaconDuration.setSnapToTicks(true);

			beaconDuration.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (beaconDuration.getValueIsAdjusting())
						return;

					Integer value = beaconDuration.getValue();
					beaconValue.setText(value + " ms");

					setSamples();
					setBehavior();
				}
			});
		}
		return beaconDuration;
	}

	/**
	 * This method initializes experimentDuration
	 * 
	 * @return javax.swing.JSlider
	 */
	private JSlider getExperimentDuration() {
		if (experimentDuration == null) {
			experimentDuration = new JSlider();

			experimentDuration.setMinimum(1);
			experimentDuration.setMaximum(300);
			experimentDuration.setValue(2);

			experimentDuration.setMinorTickSpacing(1);
			experimentDuration.setSnapToTicks(true);

			experimentDuration.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (experimentDuration.getValueIsAdjusting())
						return;
					int value = experimentDuration.getValue();
					experimentValue.setText(value + " s");
					experimentProgress.setMaximum(value);
				}
			});
		}
		return experimentDuration;
	}

	/**
	 * This method initializes experimentProgress
	 * 
	 * @return javax.swing.JProgressBar
	 */
	private JProgressBar getExperimentProgress() {
		if (experimentProgress == null) {
			experimentProgress = new JProgressBar();
			experimentProgress.setSize(400, 5);
			experimentProgress.setStringPainted(true);
			experimentProgress.setVisible(false);
		}
		return experimentProgress;
	}

	private JPanel getJProgressPane() {
		if (jProgressPane == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(1);
			jProgressPane = new JPanel();
			jProgressPane.setLayout(gridLayout);
			jProgressPane.setSize(400, 5);
			jProgressPane.add(getExperimentProgress());
		}
		return jProgressPane;
	}

	/**
	 * This method initializes protocolSlider
	 * 
	 * @return javax.swing.JSlider
	 */
	private JSlider getProtocolSlider() {
		if (protocolSlider == null) {
			protocolSlider = new JSlider();

			protocolSlider.setMinimum(100);
			protocolSlider.setMaximum(5000);
			protocolSlider.setValue(1000);

			protocolSlider.setPaintTicks(true);
			protocolSlider.setMinorTickSpacing(100);
			protocolSlider.setMajorTickSpacing(1000);
			protocolSlider.setSnapToTicks(true);

			protocolSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (protocolSlider.getValueIsAdjusting())
						return;

					int value = protocolSlider.getValue();
					periodValue.setText(value + " ms");

					setBeacon();
					setSamples();
					setBehavior();
				}
			});
		}
		return protocolSlider;
	}

	/**
	 * This method initializes samplesCount
	 * 
	 * @return javax.swing.JSlider
	 */
	private JSlider getSamplesCount() {
		if (samplesCount == null) {
			samplesCount = new JSlider();

			samplesCount.setMinimum(1);
			samplesCount.setValue(5);
			samplesCount.setMaximum(9);

			samplesCount.setPaintTicks(true);
			samplesCount.setMinorTickSpacing(1);
			samplesCount.setSnapToTicks(true);

			samplesCount.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (samplesCount.getValueIsAdjusting())
						return;

					samplesValue.setText((new Integer(samplesCount.getValue()))
							.toString());
					setBehavior();
				}
			});
		}
		return samplesCount;
	}

	/**
	 * This method initializes startButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton();
			startButton.setText("Start");
			startButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					experimentDuration.setEnabled(false);
					protocolSlider.setEnabled(false);
					beaconDuration.setEnabled(false);
					samplesCount.setEnabled(false);
					startButton.setEnabled(false);

					experimentProgress.setVisible(true);
					experimentProgress.setValue(0);
					experimentProgress.setMaximum((int) (experimentDuration
							.getValue() + Demo.EXPERIMENT_DELAY / 1000));
					demoForm.reset(protocolSlider.getValue());
					try {
						gateway.startExperiment(protocolSlider.getValue(),
								beaconDuration.getValue(), samplesCount
										.getValue(), experimentDuration
										.getValue() * 1000,
								Demo.EXPERIMENT_DELAY);

						Timer timer = new Timer();
						TimerTask experimentTimer = new ExperimentTimer(timer);
						timer.scheduleAtFixedRate(experimentTimer, 0, 1000);
					} catch (IOException e1) {
						System.err.println("Communication error: "
								+ e1.getMessage());
						System.exit(-1);
					}
				}
			});
		}
		return startButton;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
		gridBagConstraints13.gridx = 0;
		gridBagConstraints13.gridwidth = 3;
		gridBagConstraints13.gridy = 6;
		behavior = new JLabel();
		Font curFont = behavior.getFont();
		behavior.setFont(new Font(curFont.getFontName(), Font.BOLD,
				(int) (curFont.getSize() * 1.25)));
		behavior.setText("DETERMINISTIC DETECTION");
		GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
		gridBagConstraints21.gridx = 0;
		gridBagConstraints21.gridwidth = 3;
		gridBagConstraints21.weightx = 400.0;
		gridBagConstraints21.weighty = 0.0;
		gridBagConstraints21.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints21.ipadx = 0;
		gridBagConstraints21.ipady = 0;
		gridBagConstraints21.gridy = 0;
		GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
		gridBagConstraints12.gridx = 0;
		gridBagConstraints12.gridwidth = 3;
		gridBagConstraints12.weightx = 0.0;
		gridBagConstraints12.weighty = 200.0;
		gridBagConstraints12.gridy = 7;
		GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
		gridBagConstraints10.gridx = 2;
		gridBagConstraints10.anchor = GridBagConstraints.WEST;
		gridBagConstraints10.weightx = 0.0;
		gridBagConstraints10.weighty = 0.0;
		gridBagConstraints10.fill = GridBagConstraints.NONE;
		gridBagConstraints10.gridy = 2;
		experimentValue = new JLabel();
		experimentValue.setText("2 s");
		GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
		gridBagConstraints9.fill = GridBagConstraints.NONE;
		gridBagConstraints9.gridy = 2;
		gridBagConstraints9.weightx = 0.0;
		gridBagConstraints9.weighty = 0.0;
		gridBagConstraints9.anchor = GridBagConstraints.WEST;
		gridBagConstraints9.gridx = 1;
		GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
		gridBagConstraints8.gridx = 0;
		gridBagConstraints8.anchor = GridBagConstraints.WEST;
		gridBagConstraints8.weightx = 0.0;
		gridBagConstraints8.weighty = 0.0;
		gridBagConstraints8.fill = GridBagConstraints.NONE;
		gridBagConstraints8.gridy = 2;
		jLabel3 = new JLabel();
		jLabel3.setText("Experiment duration:");
		GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
		gridBagConstraints7.gridx = 2;
		gridBagConstraints7.anchor = GridBagConstraints.WEST;
		gridBagConstraints7.weightx = 0.0;
		gridBagConstraints7.weighty = 0.0;
		gridBagConstraints7.fill = GridBagConstraints.NONE;
		gridBagConstraints7.gridy = 5;
		samplesValue = new JLabel();
		samplesValue.setText("5");
		GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
		gridBagConstraints6.gridx = 2;
		gridBagConstraints6.anchor = GridBagConstraints.WEST;
		gridBagConstraints6.weightx = 0.0;
		gridBagConstraints6.fill = GridBagConstraints.NONE;
		gridBagConstraints6.gridy = 4;
		beaconValue = new JLabel();
		beaconValue.setText("100 ms");
		GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
		gridBagConstraints5.gridx = 2;
		gridBagConstraints5.anchor = GridBagConstraints.WEST;
		gridBagConstraints5.weightx = 0.0;
		gridBagConstraints5.weighty = 0.0;
		gridBagConstraints5.fill = GridBagConstraints.NONE;
		gridBagConstraints5.gridy = 3;
		periodValue = new JLabel();
		periodValue.setText("1000 ms");
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.fill = GridBagConstraints.NONE;
		gridBagConstraints4.gridy = 5;
		gridBagConstraints4.weightx = 0.0;
		gridBagConstraints4.anchor = GridBagConstraints.WEST;
		gridBagConstraints4.gridx = 1;
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 0;
		gridBagConstraints3.anchor = GridBagConstraints.WEST;
		gridBagConstraints3.weightx = 0.0;
		gridBagConstraints3.fill = GridBagConstraints.NONE;
		gridBagConstraints3.gridy = 5;
		jLabel2 = new JLabel();
		jLabel2.setText("Number of samples:");
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.fill = GridBagConstraints.NONE;
		gridBagConstraints2.gridy = 4;
		gridBagConstraints2.weightx = 0.0;
		gridBagConstraints2.anchor = GridBagConstraints.WEST;
		gridBagConstraints2.gridx = 1;
		GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
		gridBagConstraints11.gridx = 0;
		gridBagConstraints11.anchor = GridBagConstraints.WEST;
		gridBagConstraints11.weightx = 0.0;
		gridBagConstraints11.fill = GridBagConstraints.NONE;
		gridBagConstraints11.gridy = 4;
		jLabel1 = new JLabel();
		jLabel1.setText("Beacon duration:");
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.fill = GridBagConstraints.NONE;
		gridBagConstraints1.gridy = 3;
		gridBagConstraints1.weightx = 0.0;
		gridBagConstraints1.anchor = GridBagConstraints.WEST;
		gridBagConstraints1.gridx = 1;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.gridy = 3;
		jLabel = new JLabel();
		jLabel.setText("Protocol period:");
		this.setLayout(new GridBagLayout());
		this.add(getJProgressPane(), gridBagConstraints21);
		this.add(jLabel, gridBagConstraints);
		this.add(getProtocolSlider(), gridBagConstraints1);
		this.add(jLabel1, gridBagConstraints11);
		this.add(getBeaconDuration(), gridBagConstraints2);
		this.add(jLabel2, gridBagConstraints3);
		this.add(getSamplesCount(), gridBagConstraints4);
		this.add(periodValue, gridBagConstraints5);
		this.add(beaconValue, gridBagConstraints6);
		this.add(samplesValue, gridBagConstraints7);
		this.add(jLabel3, gridBagConstraints8);
		this.add(getExperimentDuration(), gridBagConstraints9);
		this.add(experimentValue, gridBagConstraints10);
		this.add(getStartButton(), gridBagConstraints12);
		this.add(behavior, gridBagConstraints13);
	}

	/**
	 * 
	 */
	private void setBeacon() {
		int maxBeacon = protocolSlider.getValue() / 2;
		if (maxBeacon > MAX_BEACON)
			maxBeacon = MAX_BEACON;
		beaconDuration.setMaximum(maxBeacon);

		if (beaconDuration.getValue() > maxBeacon)
			beaconDuration.setValue(maxBeacon);
	}

	private void setBehavior() {
		if (beaconDuration.getValue() * (samplesCount.getValue() + 1) > protocolSlider
				.getValue() / 2)
			behavior.setText("DETERMINISTIC DETECTION");
		else
			behavior.setText("PROBABILISTIC DETECTION");
	}

	/**
	 * 
	 */
	private void setSamples() {
		int max_samples = protocolSlider.getValue() / beaconDuration.getValue()
				- 1;
		if (max_samples > MAX_SAMPLES)
			max_samples = MAX_SAMPLES;
		samplesCount.setMaximum(max_samples);

		if (samplesCount.getValue() > max_samples)
			samplesCount.setValue(max_samples);
	}

} // @jve:decl-index=0:visual-constraint="10,10"
