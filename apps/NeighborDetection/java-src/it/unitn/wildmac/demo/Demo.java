/**
 * 
 */
package it.unitn.wildmac.demo;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import it.unitn.wildmac.WSNGateway;

/**
 * @author Stefan Guna
 * 
 */
public class Demo {

	protected static final int EXPERIMENT_DELAY = 2000;

	private static void printSyntax() {
		System.out.println("Syntax:");
		System.out.println("\t java " + Demo.class.getName() + " SOURCE");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File logProperties = new File("log.config");
		if (!logProperties.exists())
			BasicConfigurator.configure();
		else
			PropertyConfigurator.configure(logProperties.getPath());
		String source = null;

		try {
			source = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Invalid packet source specified.");
			printSyntax();
			System.exit(-1);
		}

		DemoForm demo = new DemoForm(WSNGateway.getGateway(source));
		demo.setVisible(true);
	}

}
