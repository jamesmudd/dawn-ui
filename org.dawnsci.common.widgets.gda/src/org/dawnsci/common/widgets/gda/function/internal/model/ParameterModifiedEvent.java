package org.dawnsci.common.widgets.gda.function.internal.model;

import org.dawnsci.common.widgets.gda.function.IParameterModifiedEvent;

import uk.ac.diamond.scisoft.analysis.fitting.functions.IFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IParameter;

public final class ParameterModifiedEvent implements
		IParameterModifiedEvent {
	private final ParameterModel parameterModel;

	ParameterModifiedEvent(ParameterModel parameterModel) {
		this.parameterModel = parameterModel;
	}

	@Override
	public IParameter getParameter() {
		return this.parameterModel.getParameter();
	}

	@Override
	public int getIndexInFunction() {
		return this.getIndexInFunction();
	}

	@Override
	public IFunction getFunction() {
		return this.parameterModel.getFunction();
	}

	public ParameterModel getParameterModel() {
		return parameterModel;
	}
}