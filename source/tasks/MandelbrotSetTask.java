package tasks;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import system.ResultImpl;
import system.SpaceRunnable;
import api.Result;
import api.Task;

/**
 * Computes the <a href="http://en.wikipedia.org/wiki/Mandelbrot_set">Mandelbrot
 * set</a>
 * 
 * @author Manasa Chandrasekhar
 * @author Kowshik Prakasam
 */

public class MandelbrotSetTask extends
		TaskBase<MandelbrotSetTask.MandelbrotSetTaskResult> implements
		Serializable, SpaceRunnable {

	/**
	 * Represents the result of Mandelbrot Set computation
	 * 
	 * @author Manasa Chandrasekhar
	 * @author Kowshik Prakasam
	 * 
	 */
	public class MandelbrotSetTaskResult implements Serializable {

		private static final long serialVersionUID = -6076247513686818330L;
		int x;
		int y;
		int[][] values;

		public MandelbrotSetTaskResult(int x, int y, int[][] values) {
			this.x = x;
			this.y = y;
			this.values = values;
		}

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public int[][] getValues() {
			return values;
		}

		public void setValues(int[][] values) {
			this.values = values;
		}
	}

	private static final long serialVersionUID = -2438392380951095822L;
	private static final int NUM_OF_CHILDREN = 16;
	private static final int MANDELBROT_LIMIT = 2;
	private double lowerX;
	private double lowerY;
	private double edgeLength;
	private int n;

	private int iterLimit;
	private int taskSize;
	private int chunkLocationX;
	private int chunkLocationY;
	
	private List<MandelbrotSetTaskResult> values;
	

	/**
	 * 
	 * @param lowerX
	 *            X-coordinate of the lower left corner of a square in the
	 *            complex plane
	 * @param lowerY
	 *            Y-coordinate of the lower left corner of a square in the
	 *            complex plane
	 * @param edgeLength
	 *            Edge length of the square in the complex plane, whose sides
	 *            are parallel to the axes
	 * @param n
	 *            Square region of the complex plane subdivided into n X n
	 *            squares, each of which is visualized by 1 pixel
	 * @param iterLimit
	 *            Defines when the representative point of a region is
	 *            considered to be in the Mandelbrot set.
	 */
	public MandelbrotSetTask(double lowerX, double lowerY, double edgeLength,
			int n, int iterLimit) {
		super(DEFAULT_TASK_ID, DEFAULT_TASK_ID, Task.Status.DECOMPOSE,
				Task.QueuingStatus.NOT_QUEUED, System.currentTimeMillis());
		this.lowerX = lowerX;
		this.lowerY = lowerY;
		this.edgeLength = edgeLength;
		this.n = n;
		this.iterLimit = iterLimit;
		this.taskSize = new Double(Math.sqrt(n * n / NUM_OF_CHILDREN))
				.intValue();

	}

	private MandelbrotSetTask(double lowerX, double lowerY, double edgeLength,
			int n, int iterLimit, int chunkLocationX, int chunkLocationY,
			Task.Status s, String taskId, String parentId) {
		this(lowerX, lowerY, edgeLength, n, iterLimit);
		this.chunkLocationX = chunkLocationX;
		this.chunkLocationY = chunkLocationY;
		super.init(s, taskId, parentId);
	}

	
	public Result<MandelbrotSetTaskResult> decompose() {
		Result<MandelbrotSetTaskResult> r = new ResultImpl<MandelbrotSetTaskResult>(
				this.getId(), this.getParentId());
		if (this.getId().equals(DEFAULT_TASK_ID)) {
			List<Task<MandelbrotSetTaskResult>> subTasks = this
					.chopMandelbrotTask();
			r.setSubTasks(subTasks);

		} else {
			MandelbrotSetTaskResult value = this.computeMandelbrotSet();
			r.setValue(value);

		}
		return r;
	}

	// Core mandelbrot set computation
	private MandelbrotSetTaskResult computeMandelbrotSet() {
		int[][] values = new int[n][n];
		int i = 0, j = 0;
		for (double xIndex = this.lowerX; i < n; xIndex += edgeLength, i++) {
			j = 0;

			for (double yIndex = this.lowerY; j < n; yIndex += edgeLength, j++) {
				double zLowerReal = xIndex;
				double zLowerComplex = yIndex;
				double zReal = zLowerReal;
				double zComplex = zLowerComplex;

				int k;
				for (k = 0; k < this.iterLimit
						&& (modulus(zReal, zComplex) <= MandelbrotSetTask.MANDELBROT_LIMIT); k++) {
					double zPrevReal = zReal;
					zReal = zReal * zReal - zComplex * zComplex + zLowerReal;
					zComplex = 2 * zPrevReal * zComplex + zLowerComplex;
				}

				if (modulus(zReal, zComplex) <= MandelbrotSetTask.MANDELBROT_LIMIT) {

					values[i][j] = this.iterLimit;
				} else {

					values[i][j] = k;
				}
			}
		}

		return new MandelbrotSetTaskResult(this.chunkLocationX,
				this.chunkLocationY, values);

	}

	/**
	 * Subdivides a mandelbrot set task
	 */
	private List<Task<MandelbrotSetTaskResult>> chopMandelbrotTask() {
		int i = 0, j = 0;
		double jump = edgeLength / n;
		List<String> childIds = this.getChildIds();
		int childIdIndex = 0;
		List<Task<MandelbrotSetTaskResult>> subTasks = new Vector<Task<MandelbrotSetTaskResult>>();
		for (double xIndex = this.lowerX; i < n; xIndex += jump * this.taskSize, i += this.taskSize) {
			j = 0;
			for (double yIndex = this.lowerY; j < n; yIndex += jump
					* this.taskSize, j += this.taskSize) {
				Task<MandelbrotSetTaskResult> aMandelbrotSetTask = new MandelbrotSetTask(
						xIndex, yIndex, jump, this.taskSize, iterLimit, i, j,
						Task.Status.DECOMPOSE, childIds.get(childIdIndex), this
								.getId());
				subTasks.add(aMandelbrotSetTask);
				childIdIndex++;
			}
		}
		return subTasks;
	}

	
	/**
	 * Implements the compose phase of Mandelbrot Set computation
	 */
	public Result<MandelbrotSetTaskResult> compose() {
		Result<MandelbrotSetTaskResult> r = new ResultImpl<MandelbrotSetTaskResult>(
				this.getId(), this.getParentId());
		List<MandelbrotSetTaskResult> listOfChunks = this.getValues();
		int[][] allValues = new int[this.n][this.n];

		for (MandelbrotSetTaskResult chunk : listOfChunks) {
			int[][] values = chunk.getValues();
			int startX = chunk.getX();
			int startY = chunk.getY();
			for (int valuesRow = 0; valuesRow < values.length; valuesRow++) {
				for (int valuesCol = 0; valuesCol < values[0].length; valuesCol++) {
					int actualX = new Double(valuesRow + startX).intValue();
					int actualY = new Double(n - 1 - (valuesCol + startY))
							.intValue();
					allValues[actualX][actualY] = values[valuesRow][valuesCol];
				}
			}
		}

		MandelbrotSetTaskResult finalResult = new MandelbrotSetTaskResult(
				this.chunkLocationX, this.chunkLocationY, allValues);
		r.setValue(finalResult);
		return r;

	}

	/**
	 * Number of subtasks created in each stage of recursion
	 */
	@Override
	public int getDecompositionSize() {
		return NUM_OF_CHILDREN;
	}

	// Modulus of a complex number
	private double modulus(double zReal, double zComplex) {
		return Math.sqrt(zReal * zReal + zComplex * zComplex);
	}
	
	public Result<?> execute() {
		if (this.getStatus() == Task.Status.DECOMPOSE) {
			return this.decompose();
		}
		if (this.getStatus() == Task.Status.COMPOSE) {
			return this.compose();
		}
		return null;
	}
	
	public List<MandelbrotSetTaskResult> getValues(){
		return this.values;
	}

	@Override
	public void putValues(List<?> values) {
		this.values = new Vector<MandelbrotSetTaskResult>();
		for (Object o : values) {
			MandelbrotSetTaskResult val = (MandelbrotSetTaskResult) o;
			this.values.add(val);

		}
	

}
}

