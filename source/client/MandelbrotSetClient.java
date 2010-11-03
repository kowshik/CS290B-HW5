package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import api.Client2Space;;

/**
 * Computes the Mandelbrot Set for a given input on a remote machine and
 * displays the result in a GUI
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */
public class MandelbrotSetClient {

	private static final String LOG_FILE="/cs/student/kowshik/mandelbrotset_client.log";
	
	// Size of JFrame displayed on the screen
	private static int N_PIXELS = 1024;

	// Spectrum of colours to represent the Mandelbrot Set on the screen
	private static Color[] myColours;

	// Input for the Mandelbrot Set job
	private static final double LOWERX = -0.7510975859375;
	private static final double LOWERY = 0.1315680625;
	private static final double EDGE_LENGTH = 0.01611;
	private static final int ITER_LIMIT = 512;
	private static final int SQUARE_SIZE = 1024;

	public static void main(String[] args) throws Exception {

		String computeSpaceServer = args[0];

		MandelbrotSetJob mandelbrotJob = new MandelbrotSetJob(LOWERX, LOWERY,
				EDGE_LENGTH, SQUARE_SIZE, ITER_LIMIT);

		MandelbrotSetClient.myColours = generateColours();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {

			Logger logger = Logger.getLogger("MandelbrotSetClient");
			logger.setUseParentHandlers(false);
			Handler fh = new FileHandler(
					LOG_FILE);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			long startTime=System.currentTimeMillis();
			Client2Space space = (Client2Space) Naming.lookup("//" + computeSpaceServer + "/"
					+ Client2Space.SERVICE_NAME);

			// ------Generate tasks and execute them remotely
			mandelbrotJob.executeJob(space);
			//mandelbrotJob.collectResults(space);
			int[][] values = mandelbrotJob.getAllResults();
			// -------------------------------------

			JLabel mandelbrotLabel = displayMandelbrotSetTaskReturnValue(values);

			// display JLabels: graphic images
			JFrame frame = new JFrame("Result Visualizations");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			Container container = frame.getContentPane();
			container.setLayout(new BorderLayout());
			container.add(new JScrollPane(mandelbrotLabel), BorderLayout.WEST);
			frame.pack();
			frame.setVisible(true);
			logger.info("Elapsed Time="+(System.currentTimeMillis()-startTime));
			
		} catch (RemoteException e) {
			System.err.println("MandelbrotSetClient exception : ");
			e.printStackTrace();

		} catch (MalformedURLException e) {
			System.err.println("MandelbrotSetClient exception : ");
			e.printStackTrace();
		} catch (NotBoundException e) {
			System.err.println("MandelbrotSetClient exception : ");
			e.printStackTrace();
		}

	}

	private static JLabel displayMandelbrotSetTaskReturnValue(int[][] counts) {
		Image image = new BufferedImage(N_PIXELS, N_PIXELS,
				BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = image.getGraphics();
		for (int i = 0; i < counts.length; i++) {
			for (int j = 0; j < counts.length; j++) {
				graphics.setColor(getColor(counts[i][j]));
				graphics.fillRect(i, j, 1, 1);
			}
		}
		ImageIcon imageIcon = new ImageIcon(image);
		return new JLabel(imageIcon);
	}

	private static Color getColor(int i) {
		if (i == ITER_LIMIT) {
			return Color.BLACK;
		}
		return myColours[i];
	}

	private static Color[] generateColours() {
		int count = 0;
		Color[] myColours = new Color[ITER_LIMIT];
		for (float r = 0; r < 1.0; r += 1.0 / 8.0) {
			for (float g = 0; g < 1.0; g += 1.0 / 8.0) {
				for (float b = 0; b < 1.0; b += 1.0 / 8.0) {
					Color c = new Color(r, g, b);
					myColours[count] = c;
					count++;
				}
			}
		}
		return myColours;
	}
}
