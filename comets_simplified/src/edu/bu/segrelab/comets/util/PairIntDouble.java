/**
 * 
 */
package edu.bu.segrelab.comets.util;

/**A simple class to implement an Integer-Double 2-Tuple.
 * 
 * These are used to store media indexes and corresponding kinetic parameters 
 * as the values in Maps used for external reactions
 * 
 * @author mquintin
 *
 */
public class PairIntDouble {
	public final Integer i;
	public final Double d;
	
	public PairIntDouble(Integer i, Double d) {
		this.i = i;
		this.d = d;
	}

	public Integer getInt(){return i;}
	public Integer getInteger(){return i;}
	public Integer getI(){return i;}
	public Double getDouble(){return d;}
	public Double getD(){return d;}

}
