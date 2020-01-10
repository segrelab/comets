package edu.bu.segrelab.comets.util;

import cern.jet.random.engine.DRand;
import java.util.NavigableMap;
import java.util.TreeMap;

public class WeightedSample<E> {
	private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
	private final DRand random;
	private double total = 0;
	
	public WeightedSample() {
		this(new DRand(new java.util.Date()));
	}
	
	public WeightedSample(DRand random) {
         this.random = random;
	}

	public void add(double weight, E result) {
		if (weight <= 0) return; // exit from the method                                                                                                                                                                                
		total += weight;
		map.put(total, result);
	}

	public E next() {
		double value = random.nextDouble() * total;
		return map.ceilingEntry(value).getValue();
	}
}



