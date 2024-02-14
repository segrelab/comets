package edu.bu.segrelab.comets.util;

//import java.text.NumberFormat;
import java.awt.Point;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsParameters;

/**
 * Implements a number of static utility functions. For example, this includes a 
 * random number generator, an array summation method, methods for searching arrays
 * for nonzero values, and a method for performing Fickian diffusion.
 * <p>
 * I guess you could use your own random number generator, but the RNG used here is static,
 * so it's the only one used in any given instance of the program, thus it's more likely
 * to have values coming from a random stream.
 * @author Bill Riehl briehl@bu.edu
 * 
 */
public class Utility implements CometsConstants
{
	private static Random rand = new Random();
	private static final int X_DIM = 1;
	private static final int Y_DIM = 2;
	private static final int Z_DIM = 3;

	/**
	 * Returns true if any of the boolean values in <code>arr</code> is true.
	 * Returns false, otherwise.
	 * @param arr
	 */
	public static boolean hasTrue(boolean[] arr)
	{
		for (int i=0; i<arr.length; i++)
		{
			if (arr[i])
				return true;
		}
		return false;
	}
	
	/**
	 * Sums together all the double values in <code>arr</code> and returns the answer.
	 * 
	 * @param arr a double array
	 * @return the sum of all values in arr
	 */
	public static double sum(double[] arr)
	{
		double val = 0;
		for (int i = 0; i < arr.length; i++)
		{
			val += arr[i];
		}
		return val;
	}
	
	public static double sum(double[][] mat)
	{
		double val = 0;
		for (int i=0; i<mat.length; i++)
		{
			for (int j=0; j<mat[i].length; j++)
			{
				val += mat[i][j];
			}
		}
		return val;
	}

	/**
	 * Sums together all the float values in arr and returns the answer.
	 * 
	 * @param arr a float array
	 * @return the sum of all values in arr
	 */
	public static float sum(float[] arr)
	{
		float val = 0;
		for (int i = 0; i < arr.length; i++)
		{
			val += arr[i];
		}
		return val;
	}
	
	/**
	 * Sums together all the int values in arr and returns the answer.
	 * 
	 * @param arr an int array
	 * @return the sum of all values in arr
	 */
	public static int sum(int[] arr)
	{
		int val = 0;
		for (int i = 0; i < arr.length; i++)
		{
			val += arr[i];
		}
		return val;
	}

	/**
	 * Makes an array with numbers 0->n-1 in random order
	 * 
	 * @param n
	 * @return a randomized array with values ranging from 0 to n-1.
	 */
	public static int[] randomOrder(int n)
	{
		int[] order = new int[n];
		for (int i = 0; i < n; i++)
		{
			order[i] = i;
		}

		for (int i = 0; i < n; i++)
		{
			int randomPos = rand.nextInt(n);
			int temp = order[i];
			order[i] = order[randomPos];
			order[randomPos] = temp;
		}
		return order;
	}

	/**
	 * Uses its pre-initialized random number generator to get the next integer
	 * within the range [0,n-1].
	 * 
	 * @param n
	 * @return a random integer value from 0 to n-1
	 */
	public static int randomInt(int n)
	{
		return rand.nextInt(n);
	}

	/**
	 * Uses a pre-initialized random number generator to calculate a double
	 * value between 0 and 1.
	 * 
	 * @return a random value between 0 and 1
	 */
	public static double randomDouble()
	{
		return rand.nextDouble();
	}

	/**
	 * Uses a pre-initialized random number generator to calculate a float value
	 * between 0 and 1.
	 * 
	 * @return a random value between 0 and 1
	 */
	public static float randomFloat()
	{
		return rand.nextFloat();
	}
	
    /**
     * Sets the seed, resets with the seed, the random number generator.
     * 
     */
	public static void randomSetSeed(long seed)
	{
		rand.setSeed(seed);
	}
	
	/**
	 * Finds and returns the maximum double value (i.e. the value closest to
	 * positive infinity) from the array.
	 * 
	 * @param arr
	 * @return
	 */
	public static double max(double[] arr)
	{
		double val = -100000;
		for (int i = 0; i < arr.length; i++)
		{
			if (arr[i] > val)
				val = arr[i];
		}
		return val;
	}

	/**
	 * Finds and returns the maximum double value (i.e. the value closest to
	 * positive infinity) in the entire 2D array.
	 * 
	 * @param arr
	 * @return
	 */
	public static double max(double[][] arr)
	{
		double val = -100000;
		for (int i = 0; i < arr.length; i++)
		{
			for (int j = 0; j < arr[0].length; j++)
			{
				if (arr[i][j] > val)
					val = arr[i][j];
			}
		}
		return val;
	}

	/**
	 * Finds the maximum double value (i.e. value closest to positive infinity)
	 * from the 2D array specified by arr[][][layer].
	 * 
	 * @param arr
	 * @param layer
	 * @return
	 */
	public static double max(double[][][] arr, int layer)
	{
		double val = -100000;
		for (int i = 0; i < arr.length; i++)
		{
			for (int j = 0; j < arr[0].length; j++)
			{
				if (arr[i][j][layer] > val)
					val = arr[i][j][layer];
			}
		}
		return val;
	}
	
	/**
	 * Finds the maximum double value (i.e. value closest to positive infinity)
	 * from the 2D array specified by arr[][][][layer].
	 * 
	 * @param arr
	 * @param layer
	 * @return
	 */
	public static double max(double[][][][] arr, int layer)
	{
		double val = -100000;
		for (int i = 0; i < arr.length; i++)
		{
			for (int j = 0; j < arr[0].length; j++)
			{
				for (int l = 0; l < arr[0][0].length; l++)
				{
					if (arr[i][j][l][layer] > val)
						val = arr[i][j][l][layer];
				}
			}
		}
		return val;
	}
	
	/**
	 * Generates a Processing-style 32 bit int version of a color by
	 * bit-shifting the rgb values together. If any channel color is out of the
	 * [0,255] bounds, it is automatically adjusted to fit.
	 * 
	 * @param r the value of the red channel, between 0 and 255
	 * @param g the value of the green channel, between 0 and 255
	 * @param b the value of the blue channel, between 0 and 255
	 * @return packed 32-bit int version of a color
	 * Note that Processing, and the associated <code>CometsRunPane</code>
	 * is no longer used.
	 */
	public static int pColor(int r, int g, int b)
	{
		if (r > 255)
			r = 255;
		else if (r < 0)
			r = 0;
		
		if (g > 255)
			g = 255;
		else if (g < 0)
			g = 0;
		
		if (b > 255)
			b = 255;
		else if (b < 0)
			b = 0;

		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	/**
	 * Searches for a nonzero value in the given array. Returns true if it finds one.
	 * @param arr - an array of numbers
	 * @return true if arr has at least one nonzero value
	 */
	public static boolean hasNonzeroValue(double[] arr)
	{
		for (int i=0; i<arr.length; i++)
		{
			if (arr[i] != 0)
				return true;
		}
		return false;
	}
	
	public static boolean hasNonzeroValue(double[][] mat)
	{
		for (int i=0; i<mat.length; i++)
		{
			for (int j=0; j<mat[i].length; j++)
			{
				if (mat[i][j] != 0)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Similar to {@link #hasNonzeroValue(double[])}, this searches the array for
	 * nonzero values, but only returns true if there are at least 2 nonzero values.
	 * @param arr - an array of numbers
	 * @return true if arr has at least two nonzero values
	 */
	public static boolean hasMultipleNonzeroValues(double[] arr)
	{
		int j=0;
		for (int i=0; i<arr.length; i++)
		{
			if (arr[i] != 0) 
				j++;
			if (j > 1)
				return true;
		}
		return false;
	}
	
	/**
	 * Searches through the array and returns an integer array of indices of the nonzero
	 * values.
	 * @param arr - a numerical array
	 * @return an integer array of indices (0->arr.length-1) of the nonzero numbers in arr
	 */
	public static int[] findNonzeroValues(double[] arr)
	{
		// the max number of nonzero values is equal to arr.length
		int[] idx = new int[arr.length];
		
		int i=0;
		for (int j=0; j<arr.length; j++)
		{
			if (arr[j] != 0)
			{
				idx[i] = j;
				i++;
			}
		}
		// At this point, the indices of all nonzero values occupy the first i positions
		// of idx[]. Just copy those into a new array of the right length.
		int[] retIdx = new int[i];
		System.arraycopy(idx, 0, retIdx, 0, i);
		return retIdx;
	}
	
	/**
	 * Gets all the points in a filled-in circle starting at point (x,y) with radius r.
	 * This is a modification of Bresenham's Midpoint Circle algorithm. Instead of drawing a
	 * point in each of 4 quadrants (and their mirrors), it draws lines from the top of the
	 * right and left quadrants to the bottom of the lower right and left quadrants.
	 * 
	 * There are probably more efficient algorithms, but this should work well enough for now.
	 * @return a HashSet of Points
	 */
	public static Set<Point> getCirclePoints(int x, int y, int r)
	{
		Set<Point> points = new HashSet<Point>();
		
		int f = 1 - r;
		int ddF_x = 1;
		int ddF_y = -2 * r;
		int x0 = x;
		int y0 = y;
		x = 0;
		y = r;

		for (int i=x0-r; i<x0+r; i++)
			points.add(new Point(i, y0));
		
		while (x < y)
		{
			if (f >= 0)
			{
				y--;
				ddF_y += 2;
				f += ddF_y;
			}
			x++;
			ddF_x += 2;
			f += ddF_x;

			for (int i = x0 - x; i < x0 + x; i++)
			{
				points.add(new Point(i, y0 + y));
				points.add(new Point(i, y0 - y));
			}
			
			for (int i = x0 - y; i < x0 + y; i++)
			{
				points.add(new Point(i, y0 + x));
				points.add(new Point(i, y0 - x));
			}
		}
		return points;
	}
	

	/**
	 * Converts a point to a string representing its values, as "x y"
	 * 
	 * @param p
	 * @return
	 */
	public static String pointToString(Point p)
	{
		return (p.getX() + " " + p.getY());
	}
	
	/**
	 * Converts a point to a string representing its values as "x y"
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static String pointToString(int x, int y)
	{
		return x + " " + y;
	}

	/**
	 * A general tridiagonal matrix solver.
	 * Solves the system Ax=d where A is a tridiagonal matrix composed like this:
	 * <quote>
	 * [ b1  c1  0   0   0  ... 0             ]
	 * [ a2  b2  c2  0   0  ... 0             ]
	 * [ 0   a3  b3  c3  0  ... 0             ]
	 * [ 0   0   a4  b4  c4 ... 0             ]
	 * [ .   .   .  .  .     .                ]
	 * [ .   .   .  .  .     .                ]
	 * [ 0     ...   0   a(n-1) b(n-1) c(n-1) ]
	 * [ 0     ...   0   0      a(n)   b(n)   ]
	 * </quote>
	 * Assumes the matrix is square with dimensions equal to the length of the
	 * right hand side vector d.<br>
	 * Also assumes that each diagonal vector a, b, and c are of the same length. However,
	 * a[0] and c[n-1] are not used and may be left as 0. Otherwise, the values should
	 * be taken from the matrix A.
	 * @param a the lower off-diagonal values of the matrix A
	 * @param b the diagonal values of the matrix A
	 * @param c the upper off-diagonal values of the matrix A
	 * @param d the right hand side values of the system
	 * @return the solution vector x
	 */
	public static double[] tdmaSolve(double[] a, double b[], double c[], double[] d)
	{
		if (b[0] == 0)
			return null;

		c[0] /= b[0];
		d[0] /= b[0];
		
		for (int i=1; i<d.length; i++)
		{
			if (b[i] - (c[i-1]*a[i]) == 0)
				return null;
			double id = 1/(b[i] - (c[i-1]*a[i]));
			c[i] *= id;
			d[i] = (d[i] - d[i-1] * a[i]) * id;
		}

		double[] x = new double[d.length];
		x[d.length-1] = d[d.length-1];
		for (int i=d.length-2; i>=0; i--)
		{
			x[i] = d[i] - c[i] * x[i+1];
		}
		
		return x;
	}

	
	/**
	 * Approximates Fick's second law of diffusion in two dimensions using an
	 * alternating direction implicit (ADI) scheme with a central difference
	 * formulation.
	 * 
	 * This method diffuses horizontally for one-half time point (based on the 
	 * parameter <code>s</code>).
	 * 
	 * This is implmented such that each row is diffused across independently of all
	 * other rows, using a modified 1D diffusion scheme. Though the central difference
	 * formula, diffusion across a single row can be converted to a linear system 
	 * involving a tridiagonal matrix of bandwidth 3, which can then be solved in
	 * linear time (see @link tdmaSolve()).
	 * 
	 * See also my dissertation for details...
	 * 
	 * @param x the 2D field to be diffused. Diffusion in this function occurs across
	 * 			the second dimension (e.g. if x = x[rows][cols], this diffuses across
	 * 			the [cols])
	 * @param neumannBound a 2D boolean array defining which boxes are to be treated
	 * 					   as Neumann boundaries (e.g. barriers that block diffusion)
	 * @param dirichletBound a 2D boolean array defining which boxes are to be treated
	 * 						 as Dirichlet boundaries (e.g. spaces that act as sinks)
	 * @param s the parameterized form of D * dT / (2 * dX^2) 
	 * @return
	 */
	public static double[][] diffuseHorizontal(double[][] x,
										  	   boolean[][] neumannBound,
										  	   boolean[][] dirichletBound,
										  	   double s)
	{
		//double s = (diffConst * dT) / (2 * dX * dX);

		/* A little confusing point here, and a break from typical matrices.
		 * In COMETS, we treat most terrain 2D arrays as typical coordinates.
		 * So neumannBound[x][y] = is there a Neumann boundary at coord (x, y)?
		 * Where x references the horizontal direction and y the vertical.
		 * 
		 * This is a little different from most matrix representations where 
		 * mat(i,j) is the i-th row and j-th column. So i = vertical, and y = horizontal.
		 *
		 * That's why things are a mirror image here.
		 */
		int numCols = x.length;
		int numRows = x[0].length;
		if (numCols == 1)
			return x;

		double[][] step = new double[numCols][numRows];  // = the intermediate step in the ADI method

		for (int i = 0; i < numRows; i++)  // diffuse across the x-direction first
		{
			// do diffusion.
			// update into step[][]
			// initialize and populate 4 arrays: a, b, c, and rhs
			// a, b, and c make up a tri-diagonal matrix. nonzeros are a[1]..a[n-1], b[0]..b[n-1], and c[0]..c[n-2]
			// rhs is the right-hand-side of the matrix equation Mx = rhs
			// where M is the tri-diagonal matrix made up by a, b, and c.
			// This is then fed into Utility.tdmaSolve()
			
			double[] a = new double[numCols];
			double[] b = new double[numCols];
			double[] c = new double[numCols];
			double[] rhs = new double[numCols];
			
			// First, init a, b, c, and rhs
			for (int j=0; j<numCols; j++)
			{
			    if (neumannBound[j][i]) // on Neumann boundary
		        {
			    	b[j] = 1;
			    	if (j > 0)
			    		c[j-1] = 0;
			        if (j < numCols-1)
			            a[j+1] = 0;
			        rhs[j] = 0;
		        }
			    else if (dirichletBound[j][i])
			    {
			    	b[j] = 1;
			        if (j > 0)
			            c[j-1] = -s;
			        if (j < numCols-1)
			            a[j+1] = -s;
			        rhs[j] = x[j][i];
			    }
			    else // only other option = no boundaries (assume that they're checked beforehand...) 
			    {
			    	// unbounded sides case
			    	if ((j > 0 && j < numCols-1) && 
			    		!dirichletBound[j-1][i] &&
			    		!dirichletBound[j+1][i] &&
			    		!neumannBound[j-1][i] &&
			    		!neumannBound[j+1][i])
			    	{
			    		b[j] = 1+2*s;
			    		a[j+1] = -s;
			    		c[j-1] = -s;
		            	rhs[j] = (1-2*s)*x[j][i] + s*x[j-1][i] + s*x[j+1][i];
			    	}
			    	// left bounded, right unbounded
			    	else if ((j < numCols-1 && (!dirichletBound[j+1][i] && !neumannBound[j+1][i])) &&
			    			 (j == 0 || dirichletBound[j-1][i] || neumannBound[j-1][i]))
			    	{
			    		// left side = Neumann
			    		if (j == 0 || neumannBound[j-1][i])
			    		{
			                b[j] = 1+s;
			                a[j+1] = -s;
			                if (j > 0)
			                	c[j-1] = 0;
			                rhs[j] = (1-s)*x[j][i] + s*x[j+1][i];
			    		}
			            else // other case = left side is Dirichlet
			            {
			            	b[j] = 1+2*s;
			            	a[j+1] = -s;
			            	c[j-1] = 0;
			            	rhs[j] = (1-2*s)*x[j][i] + s*x[j+1][i] + s*x[j-1][i];
			            }
			    	}
			    	// right bounded, left unbounded
			    	else if ((j > 0 && (!dirichletBound[j-1][i] && !neumannBound[j-1][i])) &&
			    			 (j == numCols-1 || dirichletBound[j+1][i] || neumannBound[j+1][i]))
			    	{
			    		// Neumann bound on the right
			    		if (j == numCols-1 || neumannBound[j+1][i])
			    		{
			    			b[j] = 1+s;
			    			c[j-1] = -s;
			    			if (j < numCols-1)
			    				a[j+1] = 0;
			    			rhs[j] = (1-s)*x[j][i] + s*x[j-1][i];
			    		}
			    		else // remaining case: Dirichlet on the right
			    		{
			    			b[j] = 1+2*s;
			    			a[j+1] = 0;
			    			c[j-1] = -s;
			    			rhs[j] = (1-s)*x[j][i] + s*x[j-1][i] + s*x[j+1][i];
			    		}
			    	}
			    	// lump 3 cases into one:
			    	// left edge, Neumann on the right; right edge, Neumann on the left;
			    	// or Neumann on both sides
			    	else if ((j == 0 && neumannBound[j+1][i]) ||
			    			 (j == numCols-1 && neumannBound[j-1][i]) ||
			    			 (neumannBound[j-1][i] && neumannBound[j+1][i]))
			    	{
			    		b[j] = 1;
			    		if (j < numCols-1)
			    			a[j+1] = 0;
			    		if (j > 0)
			    			c[j-1] = 0;
			    		rhs[j] = x[j][i];
			    	}
			    	// Neumann or edge on the left, Dirichlet on the right
			    	else if ((j < numCols-1 && dirichletBound[j+1][i]) &&
			    			 (j == 0 || neumannBound[j-1][i]))
			    	{
			    		b[j] = 1+s;
			    		a[j+1] = 0;
			    		if (j > 0)
			    			c[j-1] = 0;
			    		rhs[j] = (1-s)*x[j][i] + s*x[j+1][i];
			    	}
			    	// Neumann or edge on the right, Dirichlet on the left
			    	else if ((j > 0 && dirichletBound[j-1][i]) &&
			    			 (j == 0 || neumannBound[j+1][i]))
			    	{
			    		b[j] = 1+s;
			    		c[j-1] = 0;
			    		if (i < numCols-1)
			    			a[j+1] = 0;
			    		rhs[j] = (1-s)*x[j][i] - s*x[j-1][i];
			    	}
			    	// Surrounded by Dirichlet
			    	else if (dirichletBound[j-1][i] && dirichletBound[j+1][i])
			    	{
			    		b[j] = 1+2*s;
			    		a[j+1] = 0;
			    		c[j-1] = 0;
			    	}
			    }
			}
//			System.out.println("calculating row " + i);
			double[] newRow = Utility.tdmaSolve(a, b, c, rhs);
			for (int j=0; j<numCols; j++)
			{
				step[j][i] = newRow[j];
			}
		}
		return step;
	}
	
	/**
	 * Functions as @link diffuseHorizontal(), but on the opposite dimension
	 * 
	 * @param x
	 * @param neumannBound
	 * @param dirichletBound
	 * @param s
	 * @return
	 */
	public static double[][] diffuseVertical(double[][] x,
											 boolean[][] neumannBound,
											 boolean[][] dirichletBound,
											 double s)
	{
		//double s = (diffConst * dT) / (2 * dX * dX);

		/* A little confusing point here, and a break from typical matrices.
		 * In COMETS, we treat most terrain 2D arrays as typical coordinates.
		 * So neumannBound[x][y] = is there a Neumann boundary at coord (x, y)?
		 * Where x references the horizontal direction and y the vertical.
		 * 
		 * This is a little different from most matrix representations where 
		 * mat(i,j) is the i-th row and j-th column. So i = vertical, and y = horizontal.
		 *
		 * That's why things are a mirror image here.
		 */
		int numCols = x.length;
		int numRows = x[0].length;
		
		if (numRows == 1)
			return x;

		double[][] step = new double[numCols][numRows];  // = the intermediate step in the ADI method

		for (int j = 0; j < numCols; j++)  // diffuse across the y-direction last
		{
			// do diffusion.
			// update into media[..][..][k]
			double[] a = new double[numRows];
			double[] b = new double[numRows];
			double[] c = new double[numRows];
			double[] rhs = new double[numRows];
			
			// First, init a, b, c, and rhs
			for (int i=0; i<numRows; i++)
			{
				// On Neumann boundary
				if (neumannBound[j][i])
				{
					b[i] = 1;
					if (i > 0)
						c[i-1] = 0;
					if (i < numRows-1)
						a[i+1] = 0;
					rhs[i] = 0;
				}
				// on Dirichlet boundary
				else if (dirichletBound[j][i])
				{
					b[i] = 1;
					if (i > 0)
						c[i-1] = -s;
					if (i < numRows-1)
						a[i+1] = -s;
					rhs[i] = x[j][i];
				}
				else // current space is NOT on a boundary
				{
					// unbounded sides
					if ((i > 0 && i < numRows-1) && 
						!neumannBound[j][i-1] &&
						!neumannBound[j][i+1] &&
						!dirichletBound[j][i-1] &&
						!dirichletBound[j][i+1])
					{
						b[i] = 1+2*s;
						a[i+1] = -s;
						c[i-1] = -s;
						rhs[i] = (1-2*s)*x[j][i] + s*x[j][i-1] + s*x[j][i+1];
					}
					
					// top bounded or edge, bottom unbounded
			    	else if ((i < numRows-1 && 
			    			(!dirichletBound[j][i+1] && !neumannBound[j][i+1])) &&
			    			 (i == 0 || dirichletBound[j][i-1] || neumannBound[j][i-1]))
			    	{
			    		// top = Neumann
			    		if (i == 0 || neumannBound[j][i-1])
			    		{
			    			b[i] = 1+s;
			    			a[i+1] = -s;
			    			if (i > 0)
			    				c[i-1] = 0;
			    			rhs[i] = (1-s)*x[j][i] + s*x[j][i+1];
			    		}
			    		else // other case = Dirichlet bound above
			    		{
			    			b[i] = 1+2*s;
			    			a[i+1] = -s;
			    			c[i-1] = 0;
			    			rhs[i] = (1-2*s)*x[j][i] + s*x[j][i+1] + s*x[j][i-1];
			    		}
			    	}
					
					// bottom bounded, top unbounded
			    	else if ((i > 0 && (!dirichletBound[j][i-1] && !neumannBound[j][i-1])) &&
			    			(i == numRows-1 || dirichletBound[j][i+1] || neumannBound[j][i+1]))
			    	{
			    		// Neumann bound below
			    		if (i == numRows-1 || neumannBound[j][i+1])
			    		{
			    			b[i] = 1+s;
			    			c[i-1] = -s;
			    			if (i < numRows-1)
			    				a[i+1] = 0;
			    			rhs[i] = (1-s)*x[j][i] + s*x[j][i-1];
			    		}
			    		else // remaining case: Dirichlet bound below
			    		{
			    			b[i] = 1+2*s;
			    			a[i+1] = 0;
			    			c[i-1] = -s;
			    			rhs[i] = (1-s)*x[j][i] + s*x[j][i-1] + s*x[j][i+1];
			    		}
			    	}
					
					// lump 3 cases into one.
					// top edge, Neumann below; bottom edge, Neumann above; Neumann above and below
			    	else if ((i == 0 && neumannBound[j][i+1]) ||
			    			 (i == numRows-1 && neumannBound[j][i-1]) ||
			    			 (neumannBound[j][i-1] && neumannBound[j][i+1]))
			    	{
			    		b[i] = 1;
			    		if (i < numRows-1)
			    			a[i+1] = 0;
			    		if (i > 0)
			    			c[i-1] = 0;
			    		rhs[i] = x[j][i];
			    	}
					
					// Neumann bound or edge above, Dirichlet below
			    	else if ((i < numRows-1 && dirichletBound[j][i+1]) &&
			    			 (i == 0 || neumannBound[j][i-1]))
			    	{
			    		b[i] = 1+s;
			    		a[i+1] = 0;
			    		if (i > 0)
			    			c[i-1] = 0;
			    		rhs[i] = (1-s)*x[j][i] + s*x[j][i+1];
			    	}
					
					// Neumann bound or edge below, Dirichlet above
			    	else if ((i > 0 && dirichletBound[j][i-1]) &&
			    			 (i == numRows-1 || neumannBound[j][i+1]))
			    	{
			    		b[i] = 1+s;
			    		c[i-1] = 0;
			    		if (i < numRows-1)
			    			a[i+1] = 0;
			    		rhs[i] = (1-s)*x[j][i] - s*x[j][i-1];
			    	}
					
					// last case - surrounded by Dirichlet
			    	else if (dirichletBound[j][i-1] && dirichletBound[j][i+1])
			    	{
			    		b[i] = 1+2*s;
			    		a[i+1] = 0;
			    		c[i-1] = 0;
			    		rhs[i] = (1-2*s)*x[j][i] + s*x[j][i+1] + s*x[j][i-1];
			    	}
				}
			}
			double[] newCol = Utility.tdmaSolve(a, b, c, rhs);
			for (int i=0; i<numRows; i++)
			{
				step[j][i] = newCol[i];
			}
		}
		return step;		
	}

	/**
	 * Returns the right hand side of the 2D convection equation; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][] getConvectionRHS(double[][] totalBiomassDensity, double[][] biomassDensity,double[][] convDiffConstField,double packedDensity,boolean[][] barrier,double dX,double elasticModulusConstant,double frictionConstant)
	{   
        double[][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        double[][] advection=advection2D(totalBiomassDensity, biomassDensity,barrier,dX,elasticModulusConstant,frictionConstant,packedDensity);
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
        //System.out.println("Input "+biomassDensity[50][50]);
        double[][] diffusion=diffusionDLaplacianRho2(biomassDensity,convDiffConstField,barrier,dX);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				convectionRHS[i][j]=0.0;
				if(!barrier[i][j])
				{
					convectionRHS[i][j]+=advection[i][j];
					convectionRHS[i][j]+=diffusion[i][j];
				}
			}
		}		
		//System.out.println("Convection "+convectionRHS[50][50]);
		return convectionRHS;
	}
	
	/**
	 * Returns the right hand side of the 2D convection equation with nonlinear diffusion; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][] getConvectionRHSNonLinD(double[][] totalBiomassDensity, double [][] deltaDensity, double[][] biomassDensity,double[] nonLinDiffConst,double nonLinDiffExponent, double packedDensity,boolean[][] barrier,double dX,double elasticModulusConstant,double frictionConstant,double hillK, double hillN)
	{   
        double[][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        double[][] advection=advection2D(totalBiomassDensity, biomassDensity,barrier,dX,elasticModulusConstant,frictionConstant,packedDensity);
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
        double[][] diffusion=nablaDnablaRho(deltaDensity, biomassDensity,nonLinDiffConst,nonLinDiffExponent,barrier,dX,hillK,hillN);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				convectionRHS[i][j]=0.0;
				if(!barrier[i][j])
				{
					convectionRHS[i][j]+=advection[i][j];
					convectionRHS[i][j]+=diffusion[i][j];
				}
			}
		}		
		return convectionRHS;
	}
	
	/**
	 * Returns the right hand side of the 2D convection equation with nonlinear diffusion; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][] getRHSJointNonLinD( double [][] deltaDensity, double[][] biomassDensity, double[][] biomassDensityModel,double[] nonLinDiffConst,double nonLinDiffExponent,boolean[][] barrier,double dX,double hillK, double hillN)
	{   
        //double[][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        //double[][] advection=advection2D(totalBiomassDensity, biomassDensity,barrier,dX,elasticModulusConstant,frictionConstant,packedDensity);
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
		//System.out.println("Input "+deltaDensity[50][50]+" "+biomassDensity[50][50]+" "+biomassDensityModel[50][50]);
		double[][] diffusion=nablaDRhoNablaRhoModel(deltaDensity, biomassDensity,biomassDensityModel,nonLinDiffConst,nonLinDiffExponent,barrier,dX,hillK,hillN);
		//System.out.println("NonlinDiff "+diffusion[50][50]);
		return diffusion;
	}
	
	/**
	 * Returns the right hand side of the 2D convection equation; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][] getConvectionRHSc(double[][] totalBiomassDensity, double[][] biomassDensity,double[][] convDiffConstField,double packedDensity,boolean[][] barrier,double dX,double elasticModulusConstant,double[][] frictionConstant)
	{   
				//TODO stuff here
		//System.out.println("ok1");
        double[][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        double[][] advection=advection2Da(totalBiomassDensity, biomassDensity,barrier,dX,elasticModulusConstant,frictionConstant,packedDensity);
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
        //double[][] diffusion=diffusionDLaplacianRho(biomassDensity,convDiffConstField,barrier,dX);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				convectionRHS[i][j]=0.0;
				if(!barrier[i][j])
				{
					convectionRHS[i][j]+=advection[i][j];
					//convectionRHS[i][j]+=diffusion[i][j];
				}
			}
		}		
		return convectionRHS;
	}
	
	/**
	 * Returns the right hand side of the 2D convection equation; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][] getDiffusionRHS(double[][] biomassDensity,double[][] convDiffConstField,boolean[][] barrier,double dX)
	{   
				
        double[][] diffusionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
        double[][] diffusion=diffusionDLaplacianRho2(biomassDensity,convDiffConstField,barrier,dX);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				diffusionRHS[i][j]=0.0;
				if(!barrier[i][j])
				{
					diffusionRHS[i][j]+=diffusion[i][j];
				}
			}
		}		
		return diffusionRHS;
	}
	
	/**
	 * Returns the right hand side of the 2D convection equation; includes diffusive term, plus a constant flow term.
	 * @param biomass
	 * @return
	 */
	public static double[][] getDiffusionFlowRHS(double[][] biomassDensity,double[][] convDiffConstField,boolean[][] barrier,double dX, double[] flowVelocityVector)
	{   
				
        double[][] diffusionRHS=new double[biomassDensity.length][biomassDensity[0].length];
        //double[][] diffusion=diffusionGradDGradRho(biomassDensity,convDiffConstField,barrier,dX,advection)+diffusionDLaplaceRho(biomassDensity,convDiffConstField,barrier,dX,advection);
        double[][] diffusion=diffusionDLaplacianRho2Flow(biomassDensity,convDiffConstField,barrier,dX,flowVelocityVector);
        for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				diffusionRHS[i][j]=0.0;
				if(!barrier[i][j])
				{
					diffusionRHS[i][j]+=diffusion[i][j];
				}
			}
		}		
		return diffusionRHS;
	}
	
	/**
	 * Returns the right hand side of the 3D convection equation; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][][] getConvectionRHS3D(double[][][] totalBiomassDensity, double[][][] biomassDensity,double[][][] convDiffConstField,double packedDensity,boolean[][][] barrier,double dX,double elasticModulusConstant,double frictionConstant)
	{   
				
        double[][][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length][biomassDensity[0][0].length];
        double[][][] advection=advection3D(totalBiomassDensity, biomassDensity,barrier,dX,elasticModulusConstant,frictionConstant,packedDensity);
        double[][][] diffusion=diffusionDLaplacianRho3D(biomassDensity,convDiffConstField,barrier,dX);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				for(int l=0; l<biomassDensity[0][0].length;l++)
				{
					convectionRHS[i][j][l]=0.0;
					if(!barrier[i][j][l])
					{
						convectionRHS[i][j][l]+=advection[i][j][l];
						convectionRHS[i][j][l]+=diffusion[i][j][l];
					}
				}
			}
		}		
		return convectionRHS;
	}
	
	/**
	 * Returns the right hand side of the 3D convection equation; includes both advective and diffusive terms.
	 * @param biomass
	 * @return
	 */
	public static double[][][] getDiffusionRHS3D(double[][][] biomassDensity,double[][][] convDiffConstField,boolean[][][] barrier,double dX)
	{   
				
        double[][][] convectionRHS=new double[biomassDensity.length][biomassDensity[0].length][biomassDensity[0][0].length];
        double[][][] diffusion=diffusionDLaplacianRho3D(biomassDensity,convDiffConstField,barrier,dX);
		for(int i=0;i<biomassDensity.length;i++)
		{
			for(int j=0;j<biomassDensity[0].length;j++)
			{
				for(int l=0; l<biomassDensity[0][0].length;l++)
				{
					convectionRHS[i][j][l]=0.0;
					if(!barrier[i][j][l])
					{
						convectionRHS[i][j][l]+=diffusion[i][j][l];
					}
				}
			}
		}		
		return convectionRHS;
	}
	
	/**
	 * Returns a random number from the gaussian distribution: exp(-0.5*x^2/variance)/sqrt(variance*2*PI).
	 * 
	 * @param variance
	 * @return
	 */
	public static double gaussianNoise(double variance)
	{
		double rand1,rand2;
		rand1=Math.random();
		rand2=Math.random();
		
		if(rand1==0.0)
		{
			return 0.0;
		}
		else
		{
			return Math.sqrt(-2.0*variance*Math.log(rand1))*Math.sin(2.0*Math.PI*rand2);
		}
	}
	
	/**
	 * Returns a random number from the gaussian distribution: exp(-0.5*x^2/variance)/sqrt(variance*2*PI).
	 * Same as above, with rnd number generator seed as input. If seed is zero, the old method is used.
	 * @param variance
	 * @return
	 */
	public static double gaussianNoise(double variance, long seed)
	{
		double rand1,rand2;
		//System.out.println(seed);
		if(seed==0)
		{
			rand1=Math.random();
			rand2=Math.random();
		}
		else
		{	
			rand1=rand.nextDouble();
			rand2=rand.nextDouble();
		}
		
		if(rand1==0.0)
		{
			return 0.0;
		}
		else
		{
			return Math.sqrt(-2.0*variance*Math.log(rand1))*Math.sin(2.0*Math.PI*rand2);
		}
	}
	
	/**
	 * Approximates the advective term in the 2D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D pressure field. The boundary conditions are 
	 * Neumann.
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][][] velocity2D(double[][] totalBiomassDensity, double[][] biomassDensity,boolean[][] barrier,double dX,double elasticModulusConst, double frictionConstant,double packedDensity)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][][] velocity=new double[numCols][numRows][2];
		double[][] pressure=pressure2D(totalBiomassDensity, elasticModulusConst/frictionConstant, packedDensity, dX);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				if(totalBiomassDensity[i][j]>0.0)
				{
				pressure[i][j]=pressure[i][j]*(biomassDensity[i][j]/totalBiomassDensity[i][j]);
				}
				
			}
		}
	
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				velocity[i][j][0]=0.0;
				velocity[i][j][1]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					velocity[i][j][0]=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i+1][j]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i-1][j]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				else
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i+1][j]-pressure[i-1][j])/(2.0*dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					velocity[i][j][1]=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j+1]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j-1]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				else
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j+1]-pressure[i][j-1])/(2.0*dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				velocity[i][j][0]=-1.0*velocity[i][j][0];
				velocity[i][j][1]=-1.0*velocity[i][j][1];
			}
		}
		return velocity;
	}
	/**
	 * Approximates the advective term in the 2D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D pressure field. The boundary conditions are 
	 * Neumann.
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][] advection2Dc(double[][] totalBiomassDensity, double[][] biomassDensity,boolean[][] barrier,double dX,double elasticModulusConst, double[][] frictionConstant,double packedDensity)
	{
		//System.out.println("ok2");
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] advection=new double[numCols][numRows];
		double[][] pressure=pressure2Dc(totalBiomassDensity, elasticModulusConst,frictionConstant, packedDensity);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				advection[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					advection[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					advection[i][j]+=(pressure[i+1][j]-pressure[i][j])/(dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					advection[i][j]+=(pressure[i-1][j]-pressure[i][j])/(dX*dX);
				}
				else
				{
					advection[i][j]+=(pressure[i+1][j]-2.0*pressure[i][j]+pressure[i-1][j])/(dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					advection[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					advection[i][j]+=(pressure[i][j+1]-pressure[i][j])/(dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					advection[i][j]+=(pressure[i][j-1]-pressure[i][j])/(dX*dX);
				}
				else
				{
					advection[i][j]+=(pressure[i][j+1]-2.0*pressure[i][j]+pressure[i][j-1])/(dX*dX);
				}
			}
		}
		return advection;
	}
	/**
	 * Approximates the advective term in the 2D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D pressure field. The boundary conditions are 
	 * Neumann.
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][] advection2Da(double[][] totalBiomassDensity, double[][] biomassDensity,boolean[][] barrier,double dX,double elasticModulusConst, double[][] frictionConstant,double packedDensity)
	{
		//System.out.println("ok2");
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] advection=new double[numCols][numRows];
		double[][] pressure=pressure2Da(totalBiomassDensity, elasticModulusConst,frictionConstant, packedDensity,dX);
		double[][] feConst=frictElast2D(biomassDensity, elasticModulusConst,frictionConstant, packedDensity);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				advection[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]) || feConst[i][j] == 0)
				{
					advection[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(feConst[i+1][j] != 0){
						advection[i][j]+=(feConst[i+1][j]+feConst[i][j])*0.5*(pressure[i+1][j]-pressure[i][j])/(dX*dX);
					}
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(feConst[i-1][j] != 0){
						advection[i][j]+=(feConst[i-1][j]+feConst[i][j])*0.5*(pressure[i-1][j]-pressure[i][j])/(dX*dX);
					}
				}
				else
				{
					if(feConst[i+1][j] != 0){
						advection[i][j]+=(feConst[i+1][j]+feConst[i][j])*0.5*(pressure[i+1][j]-pressure[i][j])/(dX*dX);
					}
					if(feConst[i-1][j] != 0){
						advection[i][j]+=(feConst[i-1][j]+feConst[i][j])*0.5*(pressure[i-1][j]-pressure[i][j])/(dX*dX);
					}
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]) || feConst[i][j] == 0)
				{
					advection[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(feConst[i][j+1] != 0){
						advection[i][j]+=(feConst[i][j+1]+feConst[i][j])*0.5*(pressure[i][j+1]-pressure[i][j])/(dX*dX);
					}
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(feConst[i][j-1] != 0){
						advection[i][j]+=(feConst[i][j-1]+feConst[i][j])*0.5*(pressure[i][j-1]-pressure[i][j])/(dX*dX);
					}
				}
				else
				{
					if(feConst[i][j+1] != 0){
						advection[i][j]+=(feConst[i][j+1]+feConst[i][j])*0.5*(pressure[i][j+1]-pressure[i][j])/(dX*dX);
					}
					if(feConst[i][j-1] != 0){
						advection[i][j]+=(feConst[i][j-1]+feConst[i][j])*0.5*(pressure[i][j-1]-pressure[i][j])/(dX*dX);
					}
				}
			}
		}
		return advection;
	}
	/**
	 * Approximates the advective term in the 3D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 3D pressure field. The boundary conditions
	 * are Neumann.
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][][] advection3D(double[][][] totalBiomassDensity, double[][][] biomassDensity,boolean[][][] barrier,double dX,double elasticModulusConst, double frictionConstant,double packedDensity)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		int numLayers=biomassDensity[0][0].length;
		double[][][] advection=new double[numCols][numRows][numLayers];
		double[][][] pressure=pressure3D(totalBiomassDensity, elasticModulusConst/frictionConstant, packedDensity);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				for (int l=0;l<numLayers;l++)
				{
					advection[i][j][l]=0.0;
					//Do x direction first
					if(numCols==1 || (i==0 && barrier[i+1][j][l]) || (i==(numCols-1) && barrier[numCols-2][j][l]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j][l] && barrier[i+1][j][l]))
					{
						advection[i][j][l]+=0.0;
					}
					else if(i==0 || barrier[i-1][j][l])
					{
						advection[i][j][l]+=(pressure[i+1][j][l]-pressure[i][j][l])/(dX*dX);
					}
					else if(i==(numCols-1) || barrier[i+1][j][l])
					{
						advection[i][j][l]+=(pressure[i-1][j][l]-pressure[i][j][l])/(dX*dX);
					}
					else
					{
						advection[i][j][l]+=(pressure[i+1][j][l]-2.0*pressure[i][j][l]+pressure[i-1][j][l])/(dX*dX);
					}
				
					//Then do y direction
					if(numRows==1 || (j==0 && barrier[i][j+1][l]) || (j==(numRows-1) && barrier[i][numRows-2][l]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1][l] && barrier[i][j+1][l]))
					{
						advection[i][j][l]+=0.0;
					}
					else if(j==0 || barrier[i][j-1][l])
					{
						advection[i][j][l]+=(pressure[i][j+1][l]-pressure[i][j][l])/(dX*dX);
					}
					else if(j==(numRows-1) || barrier[i][j+1][l])
					{
						advection[i][j][l]+=(pressure[i][j-1][l]-pressure[i][j][l])/(dX*dX);
					}
					else
					{
						advection[i][j][l]+=(pressure[i][j+1][l]-2.0*pressure[i][j][l]+pressure[i][j-1][l])/(dX*dX);
					}
					
					//Finally do z direction
					if(numLayers==1 || (l==0 && barrier[i][j][l+1]) || (l==(numLayers-1) && barrier[i][j][numLayers-2]) ||(l!=0 && l!=(numLayers-1) && barrier[i][j][l-1] && barrier[i][j][l+1]))
					{
						advection[i][j][l]+=0.0;
					}
					else if(l==0 || barrier[i][j][l-1])
					{
						advection[i][j][l]+=(pressure[i][j][l+1]-pressure[i][j][l])/(dX*dX);
					}
					else if(l==(numLayers-1) || barrier[i][j][l+1])
					{
						advection[i][j][l]+=(pressure[i][j][l-1]-pressure[i][j][l])/(dX*dX);
					}
					else
					{
						advection[i][j][l]+=(pressure[i][j][l+1]-2.0*pressure[i][j][l]+pressure[i][j][l-1])/(dX*dX);
					}
				}
			}
		}
		return advection;
	}
	
	/**
	 * Approximates the diffusive term in the 2D convection model. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D density field. The boundary conditions
	 * are Neumann.
	 * @param biomassDensity
	 * @param convDiffConstField
	 * @param barrier
	 * @param dX
	 * @return
	 */
	
	public static double[][] diffusionDLaplacianRho(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					diffusion[i][j]+=(biomassDensity[i+1][j]-biomassDensity[i][j])/(dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					diffusion[i][j]+=(biomassDensity[i-1][j]-biomassDensity[i][j])/(dX*dX);
				}
				else
				{
					diffusion[i][j]+=(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					diffusion[i][j]+=(biomassDensity[i][j+1]-biomassDensity[i][j])/(dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					diffusion[i][j]+=(biomassDensity[i][j-1]-biomassDensity[i][j])/(dX*dX);
				}
				else
				{
					diffusion[i][j]+=(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
				}
				
				diffusion[i][j]=convDiffConstField[i][j]*diffusion[i][j];
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		return diffusion;
	}
	
	
	/**
	 * Approximates the diffusive term in the 2D convection model. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D density field. The boundary conditions
	 * are Neumann. The boundary condition replaces biomass[-1][j] with biomass[0][j] etc.
	 * @param biomassDensity
	 * @param convDiffConstField
	 * @param barrier
	 * @param dX
	 * @return
	 */
	
	public static double[][] nablaDnablaRho(double [][] deltaBiomass, double[][] biomass,double[] nonLinDiffConst, double nonLinDiffExponent, boolean[][] barrier,double dX,double hillK, double hillN)
	{
		int numCols=biomass.length;
		int numRows=biomass[0].length;
		double[][] diffusion=new double[numCols][numRows];
		double hill=0.0;
		double hillRight=0.0;
		double hillLeft=0.0;
		double diffConsRhoN=0.0;
		double diffConsRhoNRight=0.0;
		double diffConsRhoNLeft=0.0;
		double avrgDiffConstRhoNHillRight=0.0;
		double avrgDiffConstRhoNHillLeft=0.0;
		
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				//System.out.println(i+" "+j+"\n" );
				diffusion[i][j]=0.0;
				//Do x direction first Hill*D1*(nablaRho)^2+Hill*(D0+D1rho)*LaplacianRho+D1NablaHill*NablaRho
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numCols==2 && i==0) || (i==0 && barrier[i+2][j]) || (i!=0 && barrier[i-1][j] && barrier[i+2][j]))
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i+1][j]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i][j];
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i+1][j];
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i+1][j]-biomass[i][j]);
					//System.out.println("1  "+ diffusion[i][j]);
				}
				else if((numCols==2 && i==1 && i!=0) || (i!=0 && i==numCols-1 && barrier[i-2][j]) || (i!=0 && i!=1 && i!=numCols-1 && barrier[i-2][j] && barrier[i+1][j]))
				{

					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i-1][j]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i][j];
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i-1][j];
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i-1][j]);
					//System.out.println("2  "+ diffusion[i][j]);
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i+1][j]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i][j];
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i+1][j];
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i+1][j]-biomass[i][j]);
					//System.out.println("3  "+ diffusion[i][j]);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i-1][j]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i][j];
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*biomass[i-1][j];
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i-1][j]);
					//System.out.println("4  "+ diffusion[i][j]);
				}
				else
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
					
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i+1][j]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
					}
					
					if(biomass[i-1][j]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i+1][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i-1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					//System.out.println("5a  "+ diffusion[i][j]+"  "+biomass[i][j]+"   "+Math.pow(biomass[i][j],nonLinDiffExponent));
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i+1][j]-biomass[i][j])-avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i-1][j]);
					//System.out.println("5b  "+ diffusion[i][j]+"  "+biomass[i][j]+"   "+Math.pow(biomass[i][j],nonLinDiffExponent));
				}
				
				//Then do y direction 
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) || (j!=0 && j!=(numRows-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numRows==2 && j==0) || (j==0 && barrier[i][j+2]) || (j!=0 && barrier[i][j-1] && barrier[i][j+2]))
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i][j+1]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i][j+1]-biomass[i][j]);
					//System.out.println("6  "+ diffusion[i][j]);
				}
				else if((numRows==2 && j==1 && j!=0) || (j!=0 && j==numRows-1 && barrier[i][j-2]) || (j!=0 && j!=1 && j!=numRows-1 && barrier[i][j-2] && barrier[i][j+1]))
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i][j-1]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i][j-1]);
					//System.out.println("7  "+ diffusion[i][j]);
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i][j+1]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i][j+1]-biomass[i][j]);
					//System.out.println("8  "+ diffusion[i][j]);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i][j-1]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i][j-1]);
					//System.out.println("9  "+ diffusion[i][j]);
				}
				else
				{
					if(biomass[i][j]==0.0)
					{
						hill=0.0;
					}
					else
					{
						hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
					}
					
					if(biomass[i][j+1]==0.0)
					{
						hillRight=0.0;
					}
					else
					{
						hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
					}
					
					if(biomass[i][j-1]==0.0)
					{
						hillLeft=0.0;
					}
					else
					{
						hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					//System.out.println("10a  "+ diffusion[i][j]+"  "+biomass[i][j]+"  "+biomass[i][j-1]+"  "+biomass[i][j+1]);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomass[i][j+1]-biomass[i][j])-avrgDiffConstRhoNHillLeft*(biomass[i][j]-biomass[i][j-1]);
					//System.out.println("10a  "+ diffusion[i][j]+"  "+biomass[i][j]+"  "+biomass[i][j-1]+"  "+biomass[i][j+1]);
					
					//System.out.println("10b  "+avrgDiffConstRhoNHillRight+"  "+avrgDiffConstRhoNHillLeft+"  "+ biomass[i][j+1]+"  "+biomass[i][j]+"  "+biomass[i][j-1]);
					//System.out.println("10c  "+hill+"  "+hillLeft+"  "+diffConsRhoNLeft+"  "+diffConsRhoN+"  "+diffConsRhoNRight);
					
				}
			}
		}
		
		return diffusion;
	}

	/**
	 * Approximates the diffusive term in the 2D convection model. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D density field. The boundary conditions
	 * are Neumann. The boundary condition replaces biomass[-1][j] with biomass[0][j] etc.
	 * @param biomassDensity
	 * @param convDiffConstField
	 * @param barrier
	 * @param dX
	 * @return
	 */
	
	public static double[][] nablaDRhoNablaRhoModel(double [][] deltaBiomass, double[][] biomass,double[][] biomassModel,double[] nonLinDiffConst, double nonLinDiffExponent, boolean[][] barrier,double dX,double hillK, double hillN)
	{
		int numCols=biomass.length;
		int numRows=biomass[0].length;
		double[][] diffusion=new double[numCols][numRows];
		double hill=0.0;
		double hillRight=0.0;
		double hillLeft=0.0;
		double diffConsRhoN=0.0;
		double diffConsRhoNRight=0.0;
		double diffConsRhoNLeft=0.0;
		double avrgDiffConstRhoNHillRight=0.0;
		double avrgDiffConstRhoNHillLeft=0.0;
		
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				//System.out.println(i+" "+j+"\n" );
				diffusion[i][j]=0.0;
				//Do x direction first Hill*D1*(nablaRho)^2+Hill*(D0+D1rho)*LaplacianRho+D1NablaHill*NablaRho
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numCols==2 && i==0) || (i==0 && barrier[i+2][j]) || (i!=0 && barrier[i-1][j] && barrier[i+2][j]))
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
					
						if(biomass[i+1][j]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i+1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i+1][j]-biomassModel[i][j])/(dX*dX);
					//System.out.println("Hill  "+ hill);
				}
				else if((numCols==2 && i==1 && i!=0) || (i!=0 && i==numCols-1 && barrier[i-2][j]) || (i!=0 && i!=1 && i!=numCols-1 && barrier[i-2][j] && barrier[i+1][j]))
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillLeft=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i-1][j]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i-1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i-1][j])/(dX*dX);
					//System.out.println("2  "+ diffusion[i][j]);
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i+1][j]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
						}
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i+1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i+1][j]-biomassModel[i][j])/(dX*dX);
					//System.out.println("3  "+ diffusion[i][j]);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillLeft=0.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i-1][j]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i-1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i-1][j])/(dX*dX);
					//System.out.println("4  "+ diffusion[i][j]);
				}
				else
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
						hillLeft=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
						
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i+1][j]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i+1][j]/biomass[i+1][j],hillN));
						}
						
						if(biomass[i-1][j]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i-1][j]/biomass[i-1][j],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i+1][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i-1][j],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					//System.out.println("5a  "+ diffusion[i][j]+"  "+biomass[i][j]+"   "+Math.pow(biomass[i][j],nonLinDiffExponent));
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i+1][j]-biomassModel[i][j])/(dX*dX)-avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i-1][j])/(dX*dX);
					//System.out.println("Hill  "+ hill);
					//System.out.println("5b  "+ diffusion[i][j]+"  "+biomass[i][j]+"   "+Math.pow(biomass[i][j],nonLinDiffExponent));
				}
				
				//Then do y direction 
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) || (j!=0 && j!=(numRows-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numRows==2 && j==0) || (j==0 && barrier[i][j+2]) || (j!=0 && barrier[i][j-1] && barrier[i][j+2]))
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
					}
					else
					{
						
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i][j+1]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
						}
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i][j+1]-biomassModel[i][j])/(dX*dX);
					//System.out.println("6  "+ diffusion[i][j]);
				}
				else if((numRows==2 && j==1 && j!=0) || (j!=0 && j==numRows-1 && barrier[i][j-2]) || (j!=0 && j!=1 && j!=numRows-1 && barrier[i][j-2] && barrier[i][j+1]))
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillLeft=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i][j-1]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
						}
					}
					
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i][j-1])/(dX*dX);
					//System.out.println("7  "+ diffusion[i][j]);
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i][j+1]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i][j+1]-biomassModel[i][j])/(dX*dX);
					//System.out.println("8  "+ diffusion[i][j]);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillLeft=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i][j-1]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					diffusion[i][j]+=-1.0*avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i][j-1])/(dX*dX);
					//System.out.println("9  "+ diffusion[i][j]);
				}
				else
				{
					if(hillK==0.0)
					{
						hill=1.0;
						hillRight=1.0;
						hillLeft=1.0;
					}
					else
					{
					
						if(biomass[i][j]==0.0)
						{
							hill=0.0;
						}
						else
						{
							hill=(Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j]/biomass[i][j],hillN));
						}
						
						if(biomass[i][j+1]==0.0)
						{
							hillRight=0.0;
						}
						else
						{
							hillRight=(Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j+1]/biomass[i][j+1],hillN));
						}
						
						if(biomass[i][j-1]==0.0)
						{
							hillLeft=0.0;
						}
						else
						{
							hillLeft=(Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN))/(Math.pow(hillK,hillN)+Math.pow(deltaBiomass[i][j-1]/biomass[i][j-1],hillN));
						}
					}
					diffConsRhoN=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j],nonLinDiffExponent);
					diffConsRhoNRight=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j+1],nonLinDiffExponent);
					diffConsRhoNLeft=nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomass[i][j-1],nonLinDiffExponent);
					
					avrgDiffConstRhoNHillRight=0.5*(hillRight*diffConsRhoNRight+hill*diffConsRhoN);
					avrgDiffConstRhoNHillLeft=0.5*(hillLeft*diffConsRhoNLeft+hill*diffConsRhoN);
					
					//System.out.println("10a  "+ diffusion[i][j]+"  "+biomass[i][j]+"  "+biomass[i][j-1]+"  "+biomass[i][j+1]);
					
					diffusion[i][j]+=avrgDiffConstRhoNHillRight*(biomassModel[i][j+1]-biomassModel[i][j])/(dX*dX)-avrgDiffConstRhoNHillLeft*(biomassModel[i][j]-biomassModel[i][j-1])/(dX*dX);
					//System.out.println("10a  "+ diffusion[i][j]+"  "+biomass[i][j]+"  "+biomass[i][j-1]+"  "+biomass[i][j+1]);
					
					//System.out.println("10b  "+avrgDiffConstRhoNHillRight+"  "+avrgDiffConstRhoNHillLeft+"  "+ biomass[i][j+1]+"  "+biomass[i][j]+"  "+biomass[i][j-1]);
					//System.out.println("10c  "+hill+"  "+hillLeft+"  "+diffConsRhoNLeft+"  "+diffConsRhoN+"  "+diffConsRhoNRight);
					
				}
			}
		}
		//System.out.println("Diff "+avrgDiffConstRhoNHillRight+" "+avrgDiffConstRhoNHillLeft);
		
		//System.out.println("Method "+diffusion[50][50]);
		return diffusion;
	}

	
	/* Old version of the method
	public static double[][] nablaDnablaRho(double [][] deltaDensity, double[][] biomassDensity,double[] nonLinDiffConst, double nonLinDiffExponent, boolean[][] barrier,double dX,double hillK, double hillN)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		double biomassNeighborsAverage=0.0;
		double growthNeighborsAverage=0.0;
		double hill=0.0;
		double hillForward=0.0;
		double hillBackward=0.0;
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				//System.out.println(i+" "+j+"\n" );
				diffusion[i][j]=0.0;
				//Do x direction first Hill*D1*(nablaRho)^2+Hill*(D0+D1rho)*LaplacianRho+D1NablaHill*NablaRho
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numCols==2 && i==0) || (i==0 && barrier[i+2][j]) || (i!=0 && barrier[i-1][j] && barrier[i+2][j]))
				{
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i+1][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i+1][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i+1][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i+1][j]-biomassDensity[i][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				
				}
				else if((numCols==2 && i==1 && i!=0) || (i!=0 && i==numCols-1 && barrier[i-2][j]) || (i!=0 && i!=1 && i!=numCols-1 && barrier[i-2][j] && barrier[i+1][j]))
				{
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i-1][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i-1][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i-1][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j]-biomassDensity[i-1][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));				
				}
				else if(i==0 || barrier[i-1][j])
				{
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i+2][j]+biomassDensity[i+1][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+2][j]+deltaDensity[i+1][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i+1][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i+1][j]-biomassDensity[i][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(i==1 || barrier[i-2][j])
				{
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i+2][j]+biomassDensity[i+1][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+2][j]+deltaDensity[i+1][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i-1][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i-1][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i+1][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i-1][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i+1][j]-biomassDensity[i-1][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i-1][j]+biomassDensity[i-2][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i-1][j]+deltaDensity[i-2][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i-1][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j]-biomassDensity[i-1][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(i==(numCols-2) || barrier[i+2][j])
				{
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i+1][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i+1][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i-1][j]+biomassDensity[i-2][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i-1][j]+deltaDensity[i-2][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i+1][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i-1][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i+1][j]-biomassDensity[i-1][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else
				{
					biomassNeighborsAverage=(biomassDensity[i+1][j]+biomassDensity[i][j]+biomassDensity[i-1][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+1][j]+deltaDensity[i][j]+deltaDensity[i-1][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i+2][j]+biomassDensity[i+1][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i+2][j]+deltaDensity[i+1][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i-1][j]+biomassDensity[i-2][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i-1][j]+deltaDensity[i-2][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i+1][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i-1][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i+1][j]-2.0*biomassDensity[i][j]+biomassDensity[i-1][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i+1][j]-biomassDensity[i-1][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				
				//Then do y direction 
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if((numRows==2 && j==0) || (j==0 && barrier[i][j+2]) || (j!=0 && barrier[i][j-1] && barrier[i][j+2]))
				{
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j+1]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j+1]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j+1],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j+1]-biomassDensity[i][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				
				}
				else if((numRows==2 && j==1 && j!=0) || (j!=0 && j==numRows-1 && barrier[i][j-2]) || (j!=0 && j!=1 && j!=numRows-1 && barrier[i][j-2] && barrier[i][j+1]))
				{
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j-1]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j-1]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j-1],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j]-biomassDensity[i][j-1])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));				
				}
				else if(j==0 || barrier[i][j-1])
				{
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j+2]+biomassDensity[i][j+1]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+2]+deltaDensity[i][j+1]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j+1],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j+1]-biomassDensity[i][j])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(j==1 || barrier[i][j-2])
				{
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j+2]+biomassDensity[i][j+1]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+2]+deltaDensity[i][j+1]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j-1]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j-1]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j+1],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j-1],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j+1]-biomassDensity[i][j-1])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j-2]+biomassDensity[i][j-2])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j-2]+deltaDensity[i][j-2])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j-1],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j]-biomassDensity[i][j-1])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else if(j==(numRows-2) || barrier[i][j+2])
				{
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j+1]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j+1]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j-1]+biomassDensity[i][j-2])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j-1]+deltaDensity[i][j-2])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j+1],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j-1],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j+1]-biomassDensity[i][j-1])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				else
				{
					biomassNeighborsAverage=(biomassDensity[i][j+1]+biomassDensity[i][j]+biomassDensity[i][j-1])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+1]+deltaDensity[i][j]+deltaDensity[i][j-1])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hill=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hill=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j+2]+biomassDensity[i][j+1]+biomassDensity[i][j])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j+2]+deltaDensity[i][j+1]+deltaDensity[i][j])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillForward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillForward=0.0;
					}	
					
					biomassNeighborsAverage=(biomassDensity[i][j]+biomassDensity[i][j-1]+biomassDensity[i][j-2])/3.0;
					growthNeighborsAverage=(deltaDensity[i][j]+deltaDensity[i][j-1]+deltaDensity[i][j-2])/3.0;
					
					if(biomassNeighborsAverage>0.0)
					{
						hillBackward=(Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN))/(Math.pow(hillK,hillN)+Math.pow(growthNeighborsAverage/biomassNeighborsAverage,hillN));
					}
					else
					{
						hillBackward=0.0;
					}	
					
					diffusion[i][j]+=hill*nonLinDiffConst[1]*((Math.pow(biomassDensity[i][j+1],nonLinDiffExponent+1.0)-2*Math.pow(biomassDensity[i][j],nonLinDiffExponent+1.0)+Math.pow(biomassDensity[i][j-1],nonLinDiffExponent+1.0))/((nonLinDiffExponent+1.0)*dX*dX));
					diffusion[i][j]+=hill*nonLinDiffConst[0]*(biomassDensity[i][j+1]-2.0*biomassDensity[i][j]+biomassDensity[i][j-1])/(dX*dX);
					diffusion[i][j]+=(nonLinDiffConst[0]+nonLinDiffConst[1]*Math.pow(biomassDensity[i][j],nonLinDiffExponent))*((biomassDensity[i][j+1]-biomassDensity[i][j-1])/(2.0*dX))*((hillForward-hillBackward)/(2.0*dX));
				}
				
				//diffusion[i][j]=convDiffConstField[i][j]*diffusion[i][j];
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		
		return diffusion;
	}
	*/
	
	public static double[][] diffusionDLaplacianRho2(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]) || convDiffConstField[i][j] == 0)
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(convDiffConstField[i+1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i+1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i+1][j]-biomassDensity[i][j])/(dX*dX);
					}
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(convDiffConstField[i-1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i-1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i-1][j]-biomassDensity[i][j])/(dX*dX);
					}
				}	
				else
				{
					if(convDiffConstField[i+1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i+1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i+1][j]-biomassDensity[i][j])/(dX*dX);
					}
					if(convDiffConstField[i-1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i-1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i-1][j]-biomassDensity[i][j])/(dX*dX);
					}
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1])|| convDiffConstField[i][j] == 0)
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(convDiffConstField[i][j+1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j+1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j+1]-biomassDensity[i][j])/(dX*dX);
					}
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(convDiffConstField[i][j-1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j-1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j-1]-biomassDensity[i][j])/(dX*dX);
					}
				}
				else
				{
					if(convDiffConstField[i][j+1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j+1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j+1]-biomassDensity[i][j])/(dX*dX);
					}
					if(convDiffConstField[i][j-1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j-1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j-1]-biomassDensity[i][j])/(dX*dX);
					}
				}
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		//System.out.println("Diff "+convDiffConstField[50][50]+" "+convDiffConstField[49][50]+" "+convDiffConstField[51][50]+" "+convDiffConstField[50][49]+" "+convDiffConstField[50][51]);
		//System.out.println("Method "+diffusion[50][50]);
		return diffusion;
	}
	
	
	
	
	public static double[][] diffusionDLaplacianRho2Flow(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX, double[] flowVelocityVector)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		//System.out.println("HERE");
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]) || convDiffConstField[i][j] == 0)
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(convDiffConstField[i+1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i+1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i+1][j]-biomassDensity[i][j])/(dX*dX);
					}
					diffusion[i][j]-=flowVelocityVector[0]*(biomassDensity[i+1][j]-biomassDensity[i][j])/dX;
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(convDiffConstField[i-1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i-1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i-1][j]-biomassDensity[i][j])/(dX*dX);
					}
					diffusion[i][j]-=flowVelocityVector[0]*(biomassDensity[i][j]-biomassDensity[i-1][j])/dX;
				}	
				else
				{
					if(convDiffConstField[i+1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i+1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i+1][j]-biomassDensity[i][j])/(dX*dX);
					}
					if(convDiffConstField[i-1][j] != 0){
						diffusion[i][j]+=(convDiffConstField[i-1][j]+convDiffConstField[i][j])*0.5*(biomassDensity[i-1][j]-biomassDensity[i][j])/(dX*dX);
					}
					//Flow term here
					diffusion[i][j]-=flowVelocityVector[0]*0.5*(biomassDensity[i+1][j]-biomassDensity[i-1][j])/dX;
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1])|| convDiffConstField[i][j] == 0)
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(convDiffConstField[i][j+1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j+1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j+1]-biomassDensity[i][j])/(dX*dX);
					}
					diffusion[i][j]-=flowVelocityVector[1]*(biomassDensity[i][j+1]-biomassDensity[i][j])/dX;
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(convDiffConstField[i][j-1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j-1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j-1]-biomassDensity[i][j])/(dX*dX);
					}
					diffusion[i][j]-=flowVelocityVector[1]*(biomassDensity[i][j]-biomassDensity[i][j-1])/dX;
				}
				else
				{
					if(convDiffConstField[i][j+1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j+1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j+1]-biomassDensity[i][j])/(dX*dX);
					}
					if(convDiffConstField[i][j-1] != 0){
						diffusion[i][j]+=(convDiffConstField[i][j-1]+convDiffConstField[i][j])*0.5*(biomassDensity[i][j-1]-biomassDensity[i][j])/(dX*dX);
					}
					//Flow term here
					diffusion[i][j]-=flowVelocityVector[1]*0.5*(biomassDensity[i][j+1]-biomassDensity[i][j-1])/dX;					
				}
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		return diffusion;
	}
	
	
	public static double[][] diffusionDLaplacianRho3(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					diffusion[i][j]+= 0;//(convDiffConstField[i+1][j])*(biomassDensity[i+1][j])/(4*dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i-1][j])*(-biomassDensity[i-1][j])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=(convDiffConstField[i+1][j]-convDiffConstField[i-1][j])*(biomassDensity[i+1][j]-biomassDensity[i-1][j])/(4*dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					diffusion[i][j]+=0;//(convDiffConstField[i][j+1])*(biomassDensity[i][j+1])/(4*dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i][j-1])*(-biomassDensity[i][j-1])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=(convDiffConstField[i][j+1]-convDiffConstField[i][j-1])*(biomassDensity[i][j+1]-biomassDensity[i][j-1])/(4*dX*dX);
				}
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		return diffusion;
	}
	
	public static double[][] diffusionDLaplacianRho4(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j] || i==1 || barrier[i-2][j])
				{
					diffusion[i][j]+= 0;//(convDiffConstField[i+1][j])*(biomassDensity[i+1][j])/(4*dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j] || i==(numCols-2) || barrier[i+2][j])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i-1][j])*(-biomassDensity[i-1][j])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=(8*(convDiffConstField[i+1][j]-convDiffConstField[i-1][j])-(convDiffConstField[i+2][j]-convDiffConstField[i-2][j]))*(8*(biomassDensity[i+1][j]-biomassDensity[i-1][j])-(biomassDensity[i+2][j]-biomassDensity[i-2][j]))/(144*dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1]|| j==1 || barrier[i][j-2])
				{
					diffusion[i][j]+=0;//(convDiffConstField[i][j+1])*(biomassDensity[i][j+1])/(4*dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1] || j==(numRows-2) || barrier[i][j+2])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i][j-1])*(-biomassDensity[i][j-1])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=(8*(convDiffConstField[i][j+1]-convDiffConstField[i][j-1])-(convDiffConstField[i][j+2]-convDiffConstField[i][j-2]))*(8*(biomassDensity[i][j+1]-biomassDensity[i][j-1])-(biomassDensity[i][j+2]-biomassDensity[i][j-2]))/(144*dX*dX);
				}
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		return diffusion;
	}

	public static double[][] diffusionDLaplacianRho5(double[][] biomassDensity,double[][] convDiffConstField, boolean[][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] diffusion=new double[numCols][numRows];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				diffusion[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j] || i==1 || barrier[i-2][j])
				{
					diffusion[i][j]+= 0;//(convDiffConstField[i+1][j])*(biomassDensity[i+1][j])/(4*dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j] || i==(numCols-2) || barrier[i+2][j])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i-1][j])*(-biomassDensity[i-1][j])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=((convDiffConstField[i+1][j]*(biomassDensity[i+2][j]-biomassDensity[i][j]))-(convDiffConstField[i-1][j]*(biomassDensity[i][j]-biomassDensity[i-2][j])))/(4*dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					diffusion[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1]|| j==1 || barrier[i][j-2])
				{
					diffusion[i][j]+=0;//(convDiffConstField[i][j+1])*(biomassDensity[i][j+1])/(4*dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1] || j==(numRows-2) || barrier[i][j+2])
				{
					diffusion[i][j]+=0;//(-convDiffConstField[i][j-1])*(-biomassDensity[i][j-1])/(4*dX*dX);
				}
				else
				{
					diffusion[i][j]+=((convDiffConstField[i][j+1]*(biomassDensity[i][j+2]-biomassDensity[i][j]))-(convDiffConstField[i][j-1]*(biomassDensity[i][j]-biomassDensity[i][j-2])))/(4*dX*dX);
				}
				//System.out.println("advection"+i+","+j+"    "+advection[i][j]);
			}
		}
		return diffusion;
	}

	/**
	 * Approximates the diffusive term in the 3D convection model. It calculates the 
	 * finite differences approximation of the Laplacian of the 3D density field. The boundary conditions
	 * are Neumann.
	 * @param biomassDensity
	 * @param convDiffConstField
	 * @param barrier
	 * @param dX
	 * @return
	 */
	
	public static double[][][] diffusionDLaplacianRho3D(double[][][] biomassDensity,double[][][] convDiffConstField, boolean[][][] barrier,double dX)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		int numLayers=biomassDensity[0][0].length;
		double[][][] diffusion=new double[numCols][numRows][numLayers];
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				for(int l=0;l<numLayers;l++)
				{
					diffusion[i][j][l]=0.0;
					//Do x direction first
					if(numCols==1 || (i==0 && barrier[i+1][j][l]) || (i==(numCols-1) && barrier[numCols-2][j][l]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j][l] && barrier[i+1][j][l]))
					{
						diffusion[i][j][l]+=0.0;
					}
					else if(i==0 || barrier[i-1][j][l])
					{
						diffusion[i][j][l]+=(biomassDensity[i+1][j][l]-biomassDensity[i][j][l])/(dX*dX);
					}
					else if(i==(numCols-1) || barrier[i+1][j][l])
					{
						diffusion[i][j][l]+=(biomassDensity[i-1][j][l]-biomassDensity[i][j][l])/(dX*dX);
					}
					else
					{
						diffusion[i][j][l]+=(biomassDensity[i+1][j][l]-2.0*biomassDensity[i][j][l]+biomassDensity[i-1][j][l])/(dX*dX);
					}
				
					//Then do y direction
					if(numRows==1 || (j==0 && barrier[i][j+1][l]) || (j==(numRows-1) && barrier[i][numRows-2][l]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1][l] && barrier[i][j+1][l]))
					{
						diffusion[i][j][l]+=0.0;
					}
					else if(j==0 || barrier[i][j-1][l])
					{
						diffusion[i][j][l]+=(biomassDensity[i][j+1][l]-biomassDensity[i][j][l])/(dX*dX);
					}
					else if(j==(numRows-1) || barrier[i][j+1][l])
					{
						diffusion[i][j][l]+=(biomassDensity[i][j-1][l]-biomassDensity[i][j][l])/(dX*dX);
					}
					else
					{
						diffusion[i][j][l]+=(biomassDensity[i][j+1][l]-2.0*biomassDensity[i][j][l]+biomassDensity[i][j-1][l])/(dX*dX);
					}
					
					//Finally do z direction
					if(numLayers==1 || (l==0 && barrier[i][j][l+1]) || (l==(numLayers-1) && barrier[i][j][numLayers-2]) ||(l!=0 && l!=(numLayers-1) && barrier[i][j][l-1] && barrier[i][j][l+1]))
					{
						diffusion[i][j][l]+=0.0;
					}
					else if(l==0 || barrier[i][j][l-1])
					{
						diffusion[i][j][l]+=(biomassDensity[i][j][l+1]-biomassDensity[i][j][l])/(dX*dX);
					}
					else if(l==(numLayers-1) || barrier[i][j][l+1])
					{
						diffusion[i][j][l]+=(biomassDensity[i][j][l-1]-biomassDensity[i][j][l])/(dX*dX);
					}
					else
					{
						diffusion[i][j][l]+=(biomassDensity[i][j][l+1]-2.0*biomassDensity[i][j][l]+biomassDensity[i][j][l-1])/(dX*dX);
					}
					
					
					diffusion[i][j][l]=convDiffConstField[i][j][l]*diffusion[i][j][l];

				}
			}
		}
		return diffusion;
	}

	
	/**
	 * Calculates the pressure according to the Farrel et. al. model: PRL 111, 168101(2013).
	 * @param biomass
	 * @return
	 */
	public static double[][] pressure2Dc(double[][] biomass,double elasticModulusConst, double[][] frictionConst, double packDensity)
	{
		double[][] pressure=new double[biomass.length][biomass[0].length];
		for(int i=0;i<biomass.length;i++)
		{
			for(int j=0;j<biomass[0].length;j++)
			{
				//pressure[i][j]=elasticModulusConst*biomass[i][j];
				
				if(biomass[i][j]>packDensity)
				{
					if (frictionConst[i][j] > 0){
						pressure[i][j]=(elasticModulusConst/frictionConst[i][j])*Math.pow(1.0-packDensity/biomass[i][j],1.5);
					} else{
						pressure[i][j] = 0;
					}
				}
				else
				{
					pressure[i][j]=0.0;
				}
				//System.out.println(i + " " + j + " " + pressure[i][j]);
				//System.out.println("pressure"+"    "+i+","+j+"   "+pressure[i][j]);
			}
		}		
		return pressure;
	}
	
	/**
	 * Calculates the pressure according to the Farrel et. al. model: PRL 111, 168101(2013).
	 * @param biomass
	 * @return
	 */
	public static double[][] pressure2Da(double[][] biomass,double elasticModulusConst, double[][] frictionConst, double packDensity, double dX)
	{
		double[][] pressure=new double[biomass.length][biomass[0].length];
		for(int i=0;i<biomass.length;i++)
		{
			for(int j=0;j<biomass[0].length;j++)
			{
				//pressure[i][j]=elasticModulusConst*biomass[i][j];
				
				if(biomass[i][j]>packDensity*dX*dX)
				{
					pressure[i][j]=Math.pow(1.0-packDensity*dX*dX/biomass[i][j],1.5);	
				}
				else
				{
					pressure[i][j]=0.0;
				}
				//System.out.println(i + " " + j + " " + pressure[i][j]);
				//System.out.println("pressure"+"    "+i+","+j+"   "+pressure[i][j]);
			}
		}		
		return pressure;
	}
	
	/**
	 * Calculates the pressure according to the Farrel et. al. model: PRL 111, 168101(2013).
	 * @param biomass
	 * @return
	 */
	public static double[][] frictElast2D(double[][] biomass,double elasticModulusConst, double[][] frictionConst, double packDensity)
	{
		double[][] feConst=new double[biomass.length][biomass[0].length];
		for(int i=0;i<biomass.length;i++)
		{
			for(int j=0;j<biomass[0].length;j++)
			{
				//pressure[i][j]=elasticModulusConst*biomass[i][j];
				if (frictionConst[i][j] > 0){
					feConst[i][j]=(elasticModulusConst/frictionConst[i][j]);
				} else{
					feConst[i][j] = 0;
				}

			}
				//System.out.println(i + " " + j + " " + pressure[i][j]);
				//System.out.println("pressure"+"    "+i+","+j+"   "+pressure[i][j]);
		}
		return feConst;
	}		
		
	/**
	 * 3D case: Calculates the pressure according to the Farrel et. al. model: PRL 111, 168101(2013).
	 * @param biomass
	 * @return
	 */
	public static double[][][] pressure3D(double[][][] biomass,double elasticModulusConst,double packDensity)
	{
		double[][][] pressure=new double[biomass.length][biomass[0].length][biomass[0][0].length];
		for(int i=0;i<biomass.length;i++)
		{
			for(int j=0;j<biomass[0].length;j++)
			{
				for(int l=0;l<biomass[0][0].length;l++)
				{				
					if(biomass[i][j][l]>packDensity)
					{
						pressure[i][j][l]=elasticModulusConst*Math.pow(1.0-packDensity/biomass[i][j][l],1.5);
					}
					else
					{
						pressure[i][j][l]=0.0;
					}				
				}
			}
		}		
		return pressure;
	}
	

	public static double[][] diffuseFick(double[][] x,			// mM on each space
										 boolean[][] neumannBound,
										 boolean[][] dirichletBound,
										 double diffConst,	// m^2/s
										 double dT,			// s
										 double dX,			// m
										 int numTimes)		// number of times to do repeat the diffusion
	{
		//dT *= 3600;
		double dTmax = dX*dX/diffConst;
		int numSteps = 1;
		if (dT > dTmax)
		{
			//System.out.println("Unstable diffusion constants. Try setting the time step below " + dTmax + "seconds");
//			numSteps = (int)Math.ceil(dT/dTmax);
//			dT /= numSteps;
		}

		double s = (diffConst * dT) / (2 * dX * dX);
		
		for (int t=0; t<numTimes; t++)
		{
			for (int k=0; k<numSteps; k++)
			{
				double[][] step1 = diffuseHorizontal(x, neumannBound, dirichletBound, s);
				step1 = diffuseVertical(step1, neumannBound, dirichletBound, s);
				
				double[][] step2 = diffuseVertical(x, neumannBound, dirichletBound, s);
				step2 = diffuseHorizontal(step2, neumannBound, dirichletBound, s);
				
				for (int i=0; i<x.length; i++)
					for (int j=0; j<x[0].length; j++)
						x[i][j] = (step1[i][j] + step2[i][j])/2;
			}
		}
		return x;
	}
	
	public static double[][] diffuseCrankNicolson2D(double[][] x,
													boolean[][] neumannBound,
													boolean[][] dirichletBound,
													double diffConst,
													double dT,
													double dX)
	{
		double s = diffConst * dT / (2 * dX * dX);
		
		double[][] step1 = diffuseCNStep(x, neumannBound, dirichletBound, s, 0);
//		step1 = diffuseCNStep(step1, neumannBound, dirichletBound, s, 1);
//		
//		double[][] step2 = diffuseCNStep(x, neumannBound, dirichletBound, s, 1);
//		step2 = diffuseCNStep(step2, neumannBound, dirichletBound, s, 0);
//		
//		for (int i=0; i<x.length; i++)
//			for (int j=0; j<x[0].length; j++)
//				x[i][j] = (step1[i][j] + step2[i][j])/2;
		
		return step1;
	}
	
	private static double[][] diffuseCNStep(double[][] x,
										   boolean[][] neumannBound,
										   boolean[][] dirichletBound,
										   double s,
										   int dir)
	{
		int numCols = x.length;
		int numRows = x[0].length;
		
		// go horizontal, across all columns this version
		
		double[][] step = new double[numCols][numRows];

		// lower tri-diagonal band (uses from a[0]..a[n-2])
		double[] a = new double[numCols];
		
		// middle band
		double[] b = new double[numCols];
		
		// upper band (uses from c[1]..c[n-1])
		double[] c = new double[numCols];
		
		double[] rhs = new double[numCols];
		
//		int numOpen;
		// stick with just Neumann bounds for now. Dirichlet in a bit.
		for (int i=0; i<numRows; i++)
		{
			// a bit redundant and adds a few more operations,
			// but it's cheaper than re-allocating a and c each time
			for (int j=0; j<numCols; j++)
			{
				a[j] = 0;
				c[j] = 0;
			}
			
			// set up the problem
			for (int j=0; j<numCols; j++)
			{
				rhs[j] = x[j][i];
				b[j] = 1;
				
				// a, b, and c are based on the horizontal boundary state,
				// and the rhs is based on the vertical boundaries and x values
				
				// horizontal first.
				// case 1: open on the left
				if (j > 0 && !neumannBound[j-1][i])
				{
					c[j-1] = -s;
					b[j] += s;
				}
				// case 2: open on the right
				if (j < numCols-1 && !neumannBound[j+1][i])
				{
					a[j+1] = -s;
					b[j] += s;
				}
				
				// now vertical cases to build the rhs vector
				// case 1: open above
				if (i > 0 && !neumannBound[j][i-1])
				{
					rhs[j] += s*(x[j][i-1] - x[j][i]);
				}
				
				// case 2: open below
				if (i < numRows-1 && !neumannBound[j][i+1])
				{
					rhs[j] += s*(x[j][i+1] - x[j][i]);
				}
			}
			double[] newRow = Utility.tdmaSolve(a, b, c, rhs);
			for (int j=0; j<numCols; j++)
			{
				step[j][i] = newRow[j];
			}
		}
		return step;
	}

	// order of dimension sets to diffuse across. Easiest and most manageable
	// to just instantiate it once, than every time the 3d diffuser is run.
	private static final int[][] DIMENSION_SET = { { X_DIM, Y_DIM, Z_DIM },
							   					   { X_DIM, Z_DIM, Y_DIM },
							   					   { Y_DIM, X_DIM, Z_DIM },
							   					   { Y_DIM, Z_DIM, X_DIM },
							   					   { Z_DIM, X_DIM, Y_DIM },
							   					   { Z_DIM, Y_DIM, X_DIM } };

	
	/**
	 * Solves the 3D diffusion equation for a given time step dT with a given diffusion
	 * constant diffConst, over a uniform cubic space with individual cube length dX.
	 * <p>
	 * This works as an implementation of the Douglass-Gunn ADI method in 3D. The 
	 * partial differential equation:
	 * <p>
	 * dU/dt = D(d2U/dx2 + d2U/dy2 + d2U/dz2)
	 * <p>
	 * is separated into 3 equations
	 * <br>//TODO: write the 3 equations in a pretty way for the javadoc to display
	 * <p>
	 * [1]: (1-s d2x)U(n+*) = (1 + s d2x + 2s d2y + 2s d2z)U(n)
	 * [2]: (1-s d2y)U(n+**) = U(n+*) - s d2y U(n)
	 * [3]: (1-s d2z)U(n+1) = U(n+**) - s d2z U(n)
	 * <p>
	 * Where d2i = the central difference in dimension i (Ui-1 - 2Ui + Ui+1) 
	 * <br>
	 * U(n) = the state of the concentration field at time point n
	 * <br>
	 * s = D*dt/(2*dx^2)
	 * <p>
	 * Thus, this solves each direction for a partial time step (n+*, n+**), and uses the
	 * final solution to solve for the next whole time step (n+1).
	 * <p>
	 * As opposed to other methods, this has the advantage of being (supposedly) 
	 * unconditionally stable for any dT (though I haven't found it to work especially
	 * well that way), while each equation step can be calculated by solving a series
	 * of tridiagonal equations using a standard algorithm with O(n) runtime.
	 * (@see tdmaSolve())
	 * <p> 
	 * However, for the purposes of COMETS, there may be odd shaped 3D barriers in
	 * the field, solving in the x-dimension, then y, then z might give bias to the 
	 * shape of the diffused structure. To remove this bias, diffusion is performed in
	 * every permutation of directions, and the results are averaged. Thus, there are
	 * 6 stages of diffusion (X->Y->Z, X->Z->Y, Y->X->Z, etc) that are averaged.
	 * <p>
	 * Couple this with the calculation of diffused quantities of each grid space 3 times
	 * per diffusion, and you get a runtime of O(n^3) with a rather large coefficient
	 * (assuming the number of elements in each dimension is n). So, while it is accurate,
	 * doing things in 3D takes some extra horsepower.
	 * @param x
	 * @param bound
	 * @param diffConst
	 * @param dT
	 * @param dX
	 * @return
	 */
	public static double[][][] diffuse3D(double[][][] x,
										 boolean[][][] bound,
										 double diffConst,
										 double dT,
										 double dX)
	{
		if (x == null)
			throw new IllegalArgumentException("Can't diffuse a null matrix!");
		if (bound == null)
			throw new IllegalArgumentException("The boundary matrix must not be null!");
		
		int M = x.length;
		int N = x[0].length;
		int P = x[0][0].length;

		// check for sanity
		if (bound.length != M || bound[0].length != N || bound[0][0].length != P)
		{
			throw new IllegalArgumentException("The boundary matrix must be the same size as the problem matrix");
		}

		// calculate the main coefficient that's used over and over...
		double s = diffConst*dT/(2*dX*dX);

		double[][][] u = new double[M][N][P];
		
		// do the whole set of calculations, once in each dimension.
		// additionally, this is done 6 times, then averaged together.
		for (int z=0; z<DIMENSION_SET.length; z++)
		{
			double[][][] step = diffuse3DCalculation(x, null, bound, s, DIMENSION_SET[z][0], 1);
			double[][][] step2 = diffuse3DCalculation(x, step, bound, s, DIMENSION_SET[z][1], 2);
			step = diffuse3DCalculation(x, step2, bound, s, DIMENSION_SET[z][2], 3);
			
			for (int i=0; i<M; i++)
			{
				for (int j=0; j<N; j++)
				{
					for (int k=0; k<P; k++)
					{
						u[i][j][k] += step[i][j][k];
					}
				}
			}
		}
		for (int i=0; i<M; i++)
		{
			for (int j=0; j<N; j++)
			{
				for (int k=0; k<P; k++)
				{
					u[i][j][k] /= DIMENSION_SET.length;
				}
			}
		}
		return u;
	}
	
	/**
	 * This function is ugly. But not horribly ugly. Maybe it's even cute in its 
	 * ugliness, like a pug dog. Or a 3-toed sloth... which isn't that bad of a
	 * metaphor for this method.
	 * <p>
	 * Anyway, what we do here is actually compute one of the Douglass-Gunn 3D ADI
	 * equations, with arbitrary Neumann boundary spaces in the 3D field. Though
	 * there are three different equations (@see diffuse3D()), the latter 2 have
	 * the same shape, just over different dimensions. Additionally, since we
	 * need to be able to solve all three equations over any dimension. It seems
	 * that the best way to do this (aside from peeling out strips of various 
	 * matrices) is to make 3 different sets of loops, one for each dimension.
	 * <p>
	 * It's ugly, and winds up with a bunch of copy-pasting of code, but it's a
	 * decent enough trade off between efficiency and code readability. Its possible
	 * to rig together some wacky logic that shuffles the index ordering to
	 * address the right dimension, but it would probably look like spaghetti.
	 * <p>
	 * As much as I like spaghetti, this works, and it's readable.
	 * @param x
	 * @param xStep
	 * @param bound
	 * @param s
	 * @param dim
	 * @param step
	 * @return
	 * @throws IllegalArgumentException
	 */
	private static double[][][] diffuse3DCalculation(double[][][] x, double[][][] xStep, boolean[][][] bound, 
												     double s, int dim, int step) throws IllegalArgumentException
	{
		int M = x.length;
		int N = x[0].length;
		int P = x[0][0].length;
		
		// parameter sanity check
		if (step > 1)
		{
			// only need the xStep matrix to be non-null if we're not on the 
			// first diffusion step
			if (xStep == null)
				throw new IllegalArgumentException("For each calculation step past the first, the intermediate solution matrix is required");
			if (xStep.length != M || xStep[0].length != N || xStep[0][0].length != P)
				throw new IllegalArgumentException("The intermediate step matrix must have the same size as the problem matrix");
		}
		
		// checking that we have a correct value for the dimension is done later
		
		double[][][] u = new double[M][N][P];

		/* As all three of these are essentially the same in function,
		 * just applied to different directions, I'll just explain it here.
		 * 
		 * We're setting up a set of linear equations. The left hand side (LHS)
		 * is a tridiagonal matrix, with values defined by the state of the
		 * boundary at that row and in that dimension. The RHS, then, is
		 * a function of three things.
		 * 
		 * 1. what step we're on (e.g. what the right side of the equation
		 * looks like as defined in the Douglass-Gunn ADI method)
		 * 2. what dimension we're in
		 * 3. the previous step (if after step 1)
		 * 
		 * Once these are calculated, just put them in the solver method
		 * and put the solution into the matrix in the right dimension.
		 * Done!
		 */
		if (dim == X_DIM)
		{
			double[] rhs = new double[M];
			for (int j=0; j<N; j++)
			{
				for (int k=0; k<P; k++)
				{
					double[][] lhs = buildDiffuse3DMatrixLHS(bound, j, k, X_DIM, s);
					for (int i=0; i<M; i++)
					{
						rhs[i] = calcDiffuse3DMatrixRHS(x, xStep, bound, s, dim, i, j, k, step);
					}
					double[] sol = tdmaSolve(lhs[0], lhs[1], lhs[2], rhs);
					for (int i=0; i<M; i++)
						u[i][j][k] = sol[i];
				}
			}
		}
		else if (dim == Y_DIM)
		{
			double[] rhs = new double[N];
			for (int i=0; i<M; i++)
			{
				for (int k=0; k<P; k++)
				{
					double[][] lhs = buildDiffuse3DMatrixLHS(bound, i, k, Y_DIM, s);
					for (int j=0; j<N; j++)
					{
						rhs[j] = calcDiffuse3DMatrixRHS(x, xStep, bound, s, dim, i, j, k, step);
					}
					double[] sol = tdmaSolve(lhs[0], lhs[1], lhs[2], rhs);
					for (int j=0; j<N; j++)
						u[i][j][k] = sol[j];
				}
			}
		}
		else if (dim == Z_DIM)
		{
			double[] rhs = new double[P];
			for (int i=0; i<M; i++)
			{
				for (int j=0; j<N; j++)
				{
					double[][] lhs = buildDiffuse3DMatrixLHS(bound, i, j, Z_DIM, s);
					for (int k=0; k<P; k++)
					{
						rhs[k] = calcDiffuse3DMatrixRHS(x, xStep, bound, s, dim, i, j, k, step);
					}
					double[] sol = tdmaSolve(lhs[0], lhs[1], lhs[2], rhs);
					for (int k=0; k<P; k++)
						u[i][j][k] = sol[k];
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("Illegal dimension for diffusion!");
		}
		return u;
	}

	private static double calcDiffuse3DMatrixRHS(double[][][] x, double[][][] xStep, boolean[][][] bound, double s,
			int dim, int i, int j, int k, int step)
	{
		double rhs;
		if (step == 1)
			rhs = x[i][j][k];
		else
			rhs = xStep[i][j][k];
		if (!bound[i][j][k])
		{
			if (step == 1)
			{
				rhs += calcCentralDifference(x, bound, s, dim, i, j, k);
				rhs += calcCentralDifference(x, bound, 2*s, rotateDimension(dim, 1), i, j, k);
				rhs += calcCentralDifference(x, bound, 2*s, rotateDimension(dim, 2), i, j, k);
			}
			else
				rhs += calcCentralDifference(x, bound, -s, dim, i, j, k);
		}
		return rhs;
	}

	private static int rotateDimension(int dim, int rot)
	{
		// X_DIM --> Y_DIM --> Z_DIM --> X_DIM --> ...
		for (int i=0; i<rot; i++)
			switch (dim)
			{
				case X_DIM:
					dim = Y_DIM;
					break;
				case Y_DIM:
					dim = Z_DIM;
					break;
				case Z_DIM:
					dim = X_DIM;
					break;
				default:
					dim = X_DIM;
					break;
			}
		return dim;
	}

	
	private static double calcCentralDifference(double[][][] x, boolean[][][] bound,
											    double s, int dim, int i, int j, int k)
	{
		double diff = 0;
		
		if (dim == X_DIM)
		{
			if (i > 0 && !bound[i-1][j][k])
			{
				diff += s * (x[i-1][j][k] - x[i][j][k]);
			}
			if (i < x.length-1 && !bound[i+1][j][k])
			{
				diff += s * (x[i+1][j][k] - x[i][j][k]);
			}
		}
		else if (dim == Y_DIM)
		{
			if (j > 0 && !bound[i][j-1][k])
			{
				diff += s * (x[i][j-1][k] - x[i][j][k]);
			}
			if (j < x[0].length-1 && !bound[i][j+1][k])
			{
				diff += s * (x[i][j+1][k] - x[i][j][k]);
			}
		}
		else if (dim == Z_DIM)
		{
			if (k > 0 && !bound[i][j][k-1])
			{
				diff += s * (x[i][j][k-1] - x[i][j][k]);
			}
			if (k < x[0][0].length-1 && !bound[i][j][k+1])
			{
				diff += s * (x[i][j][k+1] - x[i][j][k]);
			}
		}
		else
		{
			throw new IllegalArgumentException("The passed dimension must be either X_DIM, Y_DIM, or Z_DIM");
		}
		return diff;
	}

	private static double[][] buildDiffuse3DMatrixLHS(boolean[][][] bound,
													  int c1,
													  int c2,
													  int dim,
													  double s)
	{
		int M = bound.length;
		int N = bound[0].length;
		int P = bound[0][0].length;
		
		double[][] mat;
		// dim can be 1, 2, or 3 for going across that direction
		// if it's 1, then the array at bound[][c1][c2] is used
		// if it's 2, then the array at bound[c1][][c2] is used
		// and if it's 3, then the array at bound[c1][c2][] is used
		//
		// note: DOUBLE CHECK PARAMETERS.
		
		boolean[] vec;
		switch(dim)
		{
			case 1:
				mat = new double[3][M];
				vec = new boolean[M];
				for (int i=0; i<M; i++)
				{
					vec[i] = bound[i][c1][c2];
				}
				break;
			case 2:
				mat = new double[3][N];
				vec = new boolean[N];
				for (int i=0; i<N; i++)
				{
					vec[i] = bound[c1][i][c2];
				}
				break;
			default:
				mat = new double[3][P];
				vec = new boolean[P];
				for (int i=0; i<P; i++)
				{
					vec[i] = bound[c1][c2][i];
				}
				break;
		}

		for (int i=0; i<vec.length; i++)
		{
			mat[1][i] = 1;

			// horizontal first.
			// case 1: open on the left
			if (i > 0 && !vec[i-1])
			{
				mat[2][i-1] = -s;
				mat[1][i] += s;
			}
			// case 2: open on the right
			if (i < vec.length-1 && !vec[i+1])
			{
				mat[0][i+1] = -s;
				mat[1][i] += s;
			}
		}
		return mat;
	}
	
	public static double[][] diffuseEightPoint(double[][] x, double dx, double D, double dt, boolean[][] bound)
	{
		System.out.println("doing eight point diffusion...");
		int numCols = x.length;
		int numRows = x[0].length;
		
		double[][] delta = new double[numCols][numRows];
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				delta[i][j] = D*dt*calculateLaplace(x, bound, i, j, dx);
			}
		}
		
		return delta;
	}
	
	private static double calculateLaplace(double[][] x, boolean[][] bound, int i, int j, double dx) 
	{
		/**
		 * tl, tr, bl, br = +0.5
		 * l, r, t, b = +1
		 * center = -6
		 * sum, over dx^2
		 */
		if (isBound(bound, i, j))
			return 0;
		
		double[][] laplace = { { 0.5, 1.0, 0.5 },
							   { 1.0, 0.0, 1.0 },
							   { 0.5, 1.0, 0.5 } };
		double centerCoeff = 0;
		double delta = 0;
		for (int a=-1; a<=1; a++)
		{
			for (int b=-1; b<=1; b++)
			{
				if (a == b && a == 0 || isBound(bound, i+a, j+b))
					continue; // skip the center until the end. just go 'round the outside.
				else
				{
					double l = laplace[a+1][b+1];
					centerCoeff += l;
					delta += l*(x[i+a][j+b]);
				}
			}
		}
		delta -= centerCoeff*x[i][j];
		return delta / (dx*dx);
	}
	
	private static boolean isBound(boolean[][] bound, int i, int j)
	{
		return (i < 0 || i >= bound.length ||
				j < 0 || j >= bound[0].length ||
				bound[i][j]);
	}
	
	public static double[][] diffuseApprox2D(double[][] x, boolean[][] bound)
	{
		// second order approximation using the older style method (as described in the BacSim paper)
		int numCols = x.length;
		int numRows = x[0].length;
		
		double[][] delta = new double[numCols][numRows];
		
		// do calculations.
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				// calculate delta[i][j] based on the areas around that are not bounded
				double sum = 0;
				
				/* a little tricksy:
				 * aCtr and bCtr point to where we're looking on DIFFUSION_SCALE_2D
				 * 
				 * a and b are the current coordinates in the neighborhood around 
				 * the box we're diffusing out of.
				 * note that these can be < 0 or > numCols or numRows, so be careful!
				 */
				int aCtr = 0;
				for (int a=i-1; a<=i+1; a++)
				{
					int bCtr = 0;
					
					for (int b=j-1; b<=j+1; b++)
					{
						// check range and bound
						if ((a != i || b != j) && 
							a >= 0 && a < numCols && 
							b >= 0 && b < numRows && 
							!bound[a][b])
						{
							delta[a][b] += x[i][j]*DIFFUSION_SCALE_2D[aCtr][bCtr];
							sum += DIFFUSION_SCALE_2D[aCtr][bCtr];
						}
						bCtr++;
					}
					aCtr++;
				}
				delta[i][j] += x[i][j] * -sum;
			}
		}
		
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				System.out.print(delta[i][j] + " ");
				x[i][j] += delta[i][j];
			}
			System.out.println();
		}
		System.out.println();
		return x;
	}
	
	public static double[][][] diffuseApprox3D(double[][][] x, boolean[][][] bound)
	{
		// as in the diffuseApprox2D method, but in 3D!
		int numCols = x.length;
		int numRows = x[0].length;
		int numLayers = x[0][0].length;
		
		double[][][] delta = new double[numCols][numRows][numLayers];
		
		// do calculations.
		// do calculations.
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				for (int k=0; k<numLayers; k++)
				{
					// calculate delta[i][j] based on the areas around that are not bounded
					double sum = 0;
					
					/* a little tricksy:
					 * aCtr and bCtr point to where we're looking on DIFFUSION_SCALE_2D
					 * 
					 * a and b are the current coordinates in the neighborhood around 
					 * the box we're diffusing out of.
					 * note that these can be < 0 or > numCols or numRows, so be careful!
					 */
					int aCtr = 0;
					for (int a=i-1; a<=i+1; a++)
					{
						int bCtr = 0;
						for (int b=j-1; b<=j+1; b++)
						{
							int cCtr = 0;
							for (int c=k-1; c<=k+1; c++)
							{
								// check range and bound
								if ((a != i || b != j || c != k) && 
									 a >= 0 && a < numCols && 
									 b >= 0 && b < numRows && 
									 c >= 0 && c < numLayers &&
									 !bound[a][b][c])
								{
									delta[a][b][c] += x[i][j][k]*DIFFUSION_SCALE_3D[aCtr][bCtr][cCtr];
									sum += DIFFUSION_SCALE_3D[aCtr][bCtr][cCtr];
								}
								cCtr++;
							}
							bCtr++;
						}
						aCtr++;
					}
					delta[i][j][k] += x[i][j][k] * -sum;
				}
			}
		}

		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				for (int k=0; k<numLayers; k++)
				{
					x[i][j][k] += delta[i][j][k];
				}
			}
		}
		return x;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void mainApprox3D(String[] args)
	{
		int x = 5;
		int y = 5;
		int z = 5;
		double[][][] u = new double[x][y][z];
		u[2][2][2] = 10;
		
		boolean[][][] bound = new boolean[x][y][z];
		for (int i=0; i<x; i++)
			for (int j=0; j<y; j++)
				for (int k=0; k<z; k++)
					bound[i][j][k] = false;
		
		u = Utility.diffuseApprox3D(u, bound);
		
		double total = 0;
		for (int i=0; i<x; i++)
		{
			for (int j=0; j<y; j++)
			{
				for (int k=0; k<z; k++)
				{
					System.out.print(u[i][j][k] + " ");
					total += u[i][j][k];
				}
				System.out.println();
			}
			System.out.println();
		}
		System.out.println("total = " + total);
	}

	public static void mainApprox2D(String[] args)
	{
		int x = 5;
		int y = 5;
		double[][] u = new double[x][y];
		u[2][2] = 10;
		u[2][1] = 10;
		u[2][0] = 10;
		
		boolean[][] bound = new boolean[x][y];
		for (int i=0; i<x; i++)
			for (int j=0; j<y; j++)
				bound[i][j] = false;
		
		u = Utility.diffuseApprox2D(u, bound);
		
		double total = 0;
		for (int i=0; i<x; i++)
		{
			for (int j=0; j<y; j++)
			{
				System.out.print(u[i][j] + " ");
				total += u[i][j];
			}
			System.out.println();
		}
		System.out.println("total = " + total);
	}
	
	public static void main3d(String[] args)
	{
		int x = 5;
		int y = 5;
		int z = 5;
		double[][][] u = new double[x][y][z];
		u[1][2][2] = 10;
		
		boolean[][][] bound = new boolean[x][y][z];
		for (int i=0; i<x; i++)
			for (int j=0; j<y; j++)
				for (int k=0; k<z; k++)
					bound[i][j][k] = false;
		
		bound[1][1][1] = true;
		bound[2][1][1] = true;
		bound[2][2][1] = true;
		bound[1][1][2] = true;
		bound[2][1][2] = true;
		bound[2][2][2] = true;
		bound[1][1][3] = true;
		bound[2][1][3] = true;
		bound[2][2][3] = true;
		
		long time = System.currentTimeMillis();
		for (int i=0; i<1; i++)
		{
			u = Utility.diffuse3D(u, bound, 0.5, 1, 1);
		}
		System.out.println("total time = " + (System.currentTimeMillis() - time) + "ms");

		double total = 0;
		for (int i=0; i<x; i++)
		{
			for (int j=0; j<y; j++)
			{
				for (int k=0; k<z; k++)
				{
					System.out.print(u[i][j][k] + " ");
					total += u[i][j][k];
				}
				System.out.println();
			}
			System.out.println();
		}
		System.out.println("total = " + total);
		
	}
	
	/**
	 * The main method here mainly just tests the diffuseFick method for diffusing media.
	 * @param args
	 */
	public static void mainFick(String[] args)
	{
		int numRows = 5;
		int numCols = 5;
		boolean[][] neumann = new boolean[numRows][numCols];
		boolean[][] dirichlet = new boolean[numRows][numCols];
		double[][] media = new double[numRows][numCols];
		
		System.out.println(media.length + "\t" + media[0].length);
		// set no Dirichlet or Neumann boundaries
		for (int i=0; i<numRows; i++)
		{
			for (int j=0; j<numCols; j++)
			{
				neumann[i][j] = false;
				dirichlet[i][j] = false;
			}
		}

		media[2][2] = 10;

		double dX = 1;
		double D = 2e-4;
		
		double[] dtSet = {10, 100, 1000};
		
		long time = System.currentTimeMillis();
		
		for (int i=0; i<dtSet.length; i++)
		{
			double[][] mediaTest1 = new double[numRows][];
			double[][] mediaTest2 = new double[numRows][];
			double[][] mediaTest3 = new double[numRows][];
			double[][] mediaTest4 = new double[numRows][];
			
			for (int j=0; j<numRows; j++)
			{
				mediaTest1[j] = media[j].clone();
				mediaTest2[j] = media[j].clone();
				mediaTest3[j] = media[j].clone();
				mediaTest4[j] = media[j].clone();
			}
			
			mediaTest1 = Utility.diffuseFick(mediaTest1, neumann, dirichlet, D, dtSet[i], dX, 1);
			
			for (int j=0; j<10; j++)
			{
				mediaTest2 = Utility.diffuseFick(mediaTest2, neumann, dirichlet, D, dtSet[i]/10, dX, 1);
			}
			for (int j=0; j<100; j++)
			{
				mediaTest3 = Utility.diffuseFick(mediaTest3, neumann, dirichlet, D, dtSet[i]/100, dX, 1);
			}
			for (int j=0; j<1000; j++)
			{
				mediaTest4 = Utility.diffuseFick(mediaTest4, neumann, dirichlet, D, dtSet[i]/1000, dX, 1);
			}
			
			System.out.print("X" + (int)dtSet[i] + "_1 = [");
			for (int j=0; j<numRows; j++)
			{
				System.out.print(mediaTest1[j][0]);
				for (int k=1; k<numCols; k++)
				{
					System.out.print(", " + mediaTest1[j][k]);
				}
				System.out.println();
			}
			System.out.println("];\n");

			System.out.print("X" + (int)dtSet[i] + "_10 = [");
			for (int j=0; j<numRows; j++)
			{
				System.out.print(mediaTest2[j][0]);
				for (int k=1; k<numCols; k++)
				{
					System.out.print(", " + mediaTest2[j][k]);
				}
				System.out.println();
			}
			System.out.println("];\n");
			System.out.print("X" + (int)dtSet[i] + "_100 = [");
			for (int j=0; j<numRows; j++)
			{
				System.out.print(mediaTest3[j][0]);
				for (int k=1; k<numCols; k++)
				{
					System.out.print(", " + mediaTest3[j][k]);
				}
				System.out.println();
			}
			System.out.println("];\n");
			System.out.print("X" + (int)dtSet[i] + "_1000 = [");
			for (int j=0; j<numRows; j++)
			{
				System.out.print(mediaTest4[j][0]);
				for (int k=1; k<numCols; k++)
				{
					System.out.print(", " + mediaTest4[j][k]);
				}
				System.out.println();
			}
			System.out.println("];\n");
		}

		System.out.println("time = " + (System.currentTimeMillis() - time) + "ms");
	}
	
	public static void main(String[] args)
	{
		int w = 5;
		int h = 5;
		double[][] x = { { 0, 0, 0, 0, 0 },
						 { 0, 0, 0, 0, 0 },
						 { 0, 0, 10, 0, 0 },
						 { 0, 0, 0, 0, 0 },
						 { 0, 0, 0, 0, 0 } };
		
		double D = 0.1;
		double dt = 1;
		double dx = 1;
		
		boolean[][] bound = new boolean[5][5];
//		bound[2][2] = true;
		
		double[][] diffused = Utility.diffuseEightPoint(x, dx, D, dt, bound);
		for (int i=0; i<10; i++) {
			diffused = Utility.diffuseEightPoint(diffused, dx, D, dt, bound);
		}
		double totalDiff = 0;
		double total = 0;
		for (int i=0; i<w; i++) {
			for (int j=0; j<h; j++) {
				total += x[i][j];
				diffused[i][j] += x[i][j];
				totalDiff += diffused[i][j];
				System.out.print(diffused[i][j] + "\t");
			}
			System.out.println();
		}
		System.out.println("x: " + total);
		System.out.println("diff: " + totalDiff);
		
	}

	/**
	 * Calculates the pressure according to the Farrel et. al. model: PRL 111, 168101(2013).
	 * @param biomass
	 * @return
	 */
	public static double[][] pressure2D(double[][] biomass,double elasticModulusConst,double packDensity,double dX)
	{
		double[][] pressure=new double[biomass.length][biomass[0].length];
		for(int i=0;i<biomass.length;i++)
		{
			for(int j=0;j<biomass[0].length;j++)
			{
				//pressure[i][j]=elasticModulusConst*biomass[i][j];
				
				if(biomass[i][j]>packDensity*dX*dX)
				{
					pressure[i][j]=elasticModulusConst*Math.pow(1.0-packDensity*dX*dX/biomass[i][j],1.5);
				}
				else
				{
					pressure[i][j]=0.0;
				}
				
				//System.out.println("pressure"+"    "+i+","+j+"   "+pressure[i][j]);
			}
		}		
		return pressure;
	}

	/**
	 * Approximates the advective term in the 2D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D pressure field. The boundary conditions are 
	 * Neumann.
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][] advection2D(double[][] totalBiomassDensity, double[][] biomassDensity,boolean[][] barrier,double dX,double elasticModulusConst, double frictionConstant,double packedDensity)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][] advection=new double[numCols][numRows];
		double[][] pressure=pressure2D(totalBiomassDensity, elasticModulusConst/frictionConstant, packedDensity, dX);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				if(totalBiomassDensity[i][j]>0.0)
				{
				pressure[i][j]=pressure[i][j]*(biomassDensity[i][j]/totalBiomassDensity[i][j]);
				}
				
			}
		}
	
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				advection[i][j]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					advection[i][j]+=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					advection[i][j]+=(pressure[i+1][j]-pressure[i][j])/(dX*dX);
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					advection[i][j]+=(pressure[i-1][j]-pressure[i][j])/(dX*dX);
				}
				else
				{
					advection[i][j]+=(pressure[i+1][j]-2.0*pressure[i][j]+pressure[i-1][j])/(dX*dX);
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					advection[i][j]+=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					advection[i][j]+=(pressure[i][j+1]-pressure[i][j])/(dX*dX);
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					advection[i][j]+=(pressure[i][j-1]-pressure[i][j])/(dX*dX);
				}
				else
				{
					advection[i][j]+=(pressure[i][j+1]-2.0*pressure[i][j]+pressure[i][j-1])/(dX*dX);
				}
				
				/*
				if(totalBiomassDensity[i][j]==0.0)
				{
					advection[i][j]=0.0;
				}
				else
				{
					advection[i][j]=advection[i][j]*(biomassDensity[i][j]/totalBiomassDensity[i][j]);
				}
				*/
			}
		}
		return advection;
	}

	/**
	 * Approximates the advective term in the 2D convection model of growth. It calculates the 
	 * finite differences approximation of the Laplacian of the 2D pressure field. The boundary conditions are 
	 * Neumann. This one is if the friction is in a spatial field context. 
	 * @param biomassDensity
	 * @param barrier
	 * @param dX
	 * @param elasticModulusConst
	 * @param frictionConstant
	 * @param packedDensity
	 * @return
	 */
	
	public static double[][][] velocity2D(double[][] totalBiomassDensity, double[][] biomassDensity,boolean[][] barrier,double dX,double elasticModulusConst, double[][] frictionConstant,double packedDensity)
	{
		int numCols=biomassDensity.length;
		int numRows=biomassDensity[0].length;
		double[][][] velocity=new double[numCols][numRows][2];
		double[][] pressure=pressure2D(totalBiomassDensity, elasticModulusConst, packedDensity, dX);
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				if(totalBiomassDensity[i][j]>0.0)
				{
				pressure[i][j]=pressure[i][j]*(biomassDensity[i][j]/(frictionConstant[i][j]*totalBiomassDensity[i][j]));
				}
				
			}
		}
	
		
		for(int i=0;i<numCols;i++)
		{
			for(int j=0;j<numRows;j++)
			{
				velocity[i][j][0]=0.0;
				velocity[i][j][1]=0.0;
				//Do x direction first
				if(numCols==1 || (i==0 && barrier[i+1][j]) || (i==(numCols-1) && barrier[numCols-2][j]) || (i!=0 && i!=(numCols-1) && barrier[i-1][j] && barrier[i+1][j]))
				{
					velocity[i][j][0]=0.0;
				}
				else if(i==0 || barrier[i-1][j])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i+1][j]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				else if(i==(numCols-1) || barrier[i+1][j])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i-1][j]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				else
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][0]=(pressure[i+1][j]-pressure[i-1][j])/(2.0*dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][0]=0.0;
					}
				}
				
				//Then do y direction
				if(numRows==1 || (j==0 && barrier[i][j+1]) || (j==(numRows-1) && barrier[i][numRows-2]) ||(j!=0 && j!=(numCols-1) && barrier[i][j-1] && barrier[i][j+1]))
				{
					velocity[i][j][1]=0.0;
				}
				else if(j==0 || barrier[i][j-1])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j+1]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				else if(j==(numRows-1) || barrier[i][j+1])
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j-1]-pressure[i][j])/(dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				else
				{
					if(biomassDensity[i][j]>0.0)
					{
						velocity[i][j][1]=(pressure[i][j+1]-pressure[i][j-1])/(2.0*dX*biomassDensity[i][j]);
					}
					else
					{
						velocity[i][j][1]=0.0;
					}
				}
				velocity[i][j][0]=-1.0*velocity[i][j][0];
				velocity[i][j][1]=-1.0*velocity[i][j][1];
			}
		}
		return velocity;
	}
	
}
