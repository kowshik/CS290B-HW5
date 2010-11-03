package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import api.Client2Space;

/**
 * Computes the Nth Fibonacci number for a given input on a remote machine and
 * displays the result on the console
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public class FibonacciClient {

	private static final int N = 18;
	private static final String LOG_FILE = "/cs/student/kowshik/fibonacci_client.log";

	public static void main(String[] args) {

		String computeSpaceServer = args[0];

		FibonacciJob fibJob = new FibonacciJob(N);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		Client2Space space;
		try {
			Logger logger = Logger.getLogger("MandelbrotSetClient");
			logger.setUseParentHandlers(false);
			Handler fh = new FileHandler(LOG_FILE);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			long startTime = System.currentTimeMillis();
			space = (Client2Space) Naming.lookup("//" + computeSpaceServer + "/"
					+ Client2Space.SERVICE_NAME);
			System.out.println("Generating fibonacci number for N = " + N);
			// ------Generate tasks and execute them remotely
			fibJob.executeJob(space);
			int fibValue = fibJob.getAllResults();
			// -------------------------------------

			System.out.println("Fibonacci Value : " + fibValue);
			logger.info("Elapsed Time="
					+ (System.currentTimeMillis() - startTime));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}