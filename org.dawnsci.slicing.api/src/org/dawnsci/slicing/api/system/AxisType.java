/*-
 * Copyright (c) 2013 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.slicing.api.system;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.services.ServiceManager;

import uk.ac.diamond.scisoft.analysis.IAnalysisService;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;

public enum AxisType {

	NONE(null,             -Integer.MAX_VALUE), 
	X("X",                  0), 
	Y("Y",                  1), 
	Y_MANY("Y (Many)",      1),  // Not actually used as a legal type, just for labels.
	Z("Z",                  2), 
	SLICE("Slice",         -1), 
	MEAN("Mean",           -1, true), 
	MAX("Max",             -1, true), 
	MIN("Min",             -1, true), 
	//MODE("Mode",           -1, true), 
	MEDIAN("Median",       -1, true), 
	SUM("Sum",             -1, true), 
	RANGE("Range",         -1);
		
	private final String  name;
	private final int     index;
	private final boolean isAdvanced;

	AxisType(String label, int index) {
		this(label,index, false);
	}
	AxisType(String name, int index, boolean isAdvanced) {
		this.name  = name;
		this.index = index;
		this.isAdvanced = isAdvanced;
	}

	public String getLabel() {
		if (hasValue()) return "("+name+")";
		return name;
	}

	public boolean hasValue() {
		return index<0;
	}

	public int getIndex() {
		return index;
	}
	
	public String toString() {
		return getLabel();
	}
	
	/**
	 * If the axis type is only for advanced slicing, set to true.
	 * @return
	 */
	public boolean isAdvanced() {
		return isAdvanced;
	}
	
	public static final List<AxisType> getMathsTypes() {
		final List<AxisType> ret = new ArrayList<AxisType>(3);
		for (AxisType at : values()) {
			if (at.isAdvanced()) ret.add(at);
		}
		return ret;
	}
	public String getName() {
		return name;
	}
	public static AxisType forLabel(String value) {
		for (AxisType pa : AxisType.values()) {
			if (value.equals(pa.getLabel())) {
				return pa; // Bit of a bodge
			}
		}
		return null;
	}
	
	/**
	 * process "advanced" (aka maths) on the slice
	 * @param slice
	 * @param i
	 * @return
	 * @throws Exception 
	 */
	public IDataset process(IDataset slice, int i) throws Exception {
		
		if (!isAdvanced()) throw new Exception("Cannot process non-advanced axes!");
		
		final IAnalysisService service = (IAnalysisService)ServiceManager.getService(IAnalysisService.class);
		if (this==MEAN) { // TODO The other types!
			return service.mean(slice, i);
		} else if (this==MAX) { // TODO The other types!
			return service.max(slice, i);
		} else if (this==MIN) { // TODO The other types!
			return service.min(slice, i);
		} else if (this==MEDIAN) { // TODO The other types!
			return service.median(slice, i);
//		} else if (this==MODE) { // TODO The other types!
//			return service.mode(slice, i);
		} else if (this==SUM) { // TODO The other types!
			return service.sum(slice, i);
		}

		
		throw new Exception(getName()+" Has not yet been implemented! Sorry!");
	}

}
