/**
 * 
 */
package it.unitn.wildmac;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class OneShotExperiment {

	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());

		String source;
		long period;
		int node, beacon, samples, power;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			return;
		}
		try {
			node = new Integer(args[1]);
		} catch (Exception e) {
			System.err.println("Invalid node specified.");
			printSyntax();
			return;
		}
		try {
			period = new Integer(args[2]);
		} catch (Exception e) {
			System.err.println("Invalid protocol period specified.");
			printSyntax();
			return;
		}
		try {
			beacon = new Integer(args[3]);
		} catch (Exception e) {
			System.err.println("Invalid beacon duration specified.");
			printSyntax();
			return;
		}
		try {
			samples = new Integer(args[4]);
		} catch (Exception e) {
			System.err.println("Invalid number of samples specified.");
			printSyntax();
			return;
		}
		try {
			power = new Integer(args[5]);
		} catch (Exception e) {
			System.err.println("Invalid beacon power specified.");
			printSyntax();
			return;
		}

		WSNGateway gateway = WSNGateway.getGateway(source);

		try {
			gateway.startExperiment(period, beacon, samples, power, node);
		} catch (IOException e) {
			System.err.println("Unable to communicate with the sink.");
			System.err.println(e.getMessage());
		}
		System.exit(0);
	}

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + OneShotExperiment.class.getName()
				+ " SOURCE NODE PERIOD BEACON SAMPLES POWER");
	}
}
