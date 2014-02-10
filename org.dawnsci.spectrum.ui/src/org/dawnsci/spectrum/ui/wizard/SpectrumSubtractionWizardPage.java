package org.dawnsci.spectrum.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.spectrum.ui.file.IContain1DData;
import org.dawnsci.spectrum.ui.processing.SubtractionProcess;
import org.dawnsci.spectrum.ui.utils.PolynomialInterpolator1D;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;


import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;


public class SpectrumSubtractionWizardPage extends WizardPage {
	
	List<IContain1DData> dataList;
	IDataset xdata, ydata1, ydata2;
	IPlottingSystem system;
	
	SubtractionProcess process;

	public SpectrumSubtractionWizardPage(List<IContain1DData> dataList) {
		super("Processing Wizard page");
		this.dataList = dataList;
		process = new SubtractionProcess();
		process.setDatasetList(dataList);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		setControl(container);
		
		Label label = new Label(container,SWT.None); 
		label.setText("Subtraction wizard page");
		label.setLayoutData(new GridData());
		ActionBarWrapper actionBarWrapper = ActionBarWrapper.createActionBars(container, null);
		Composite controlComposite = new Composite(container, SWT.None);
		controlComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		controlComposite.setLayout(new GridLayout());
		//TODO MAke a single selection listener for all widgets
		//TODO make a show original data checkbox
		Button radioA = new Button(controlComposite, SWT.RADIO);
		radioA.setText("a-b*scale");
		radioA.setSelection(true);
		
		radioA.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button aButton = (Button)e.getSource();
				process.setAminusB(aButton.getSelection());
				update();
			}
			
		});
		
		Button radioB = new Button(controlComposite, SWT.RADIO);
		radioB.setText("b-a*scale");
		
		radioB.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button aButton = (Button)e.getSource();
				process.setAminusB(!aButton.getSelection());
				update();
			}
		});
		
		label = new Label(controlComposite,SWT.None); 
		label.setText("Scale:");
		
		Spinner spinner = new Spinner(controlComposite, SWT.None);
		spinner.setDigits(4);
		
		spinner.setMaximum(Integer.MAX_VALUE);
		spinner.setMinimum(1);
		spinner.setSelection((int)Math.pow(10, 4));
		spinner.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selection = ((Spinner)e.getSource()).getSelection();
		        int digits = ((Spinner)e.getSource()).getDigits();
		        double val = (selection / Math.pow(10, digits));
		        process.setScale(val);
				update();
			}
			
		});
		
		spinner.addTraverseListener(new TraverseListener() {
			
			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = false; 
			}
		});
		
		Composite plotComposite = new Composite(container, SWT.None);
		plotComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		plotComposite.setLayout(new FillLayout());
		
		
		try {
			system = PlottingFactory.createPlottingSystem();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		system.createPlotPart(plotComposite, "Spectrum Wizard",actionBarWrapper,PlotType.XY,null);
		
		update();
		
	}
	
	
	private void update() {
        
        List<IContain1DData> out = process.process();
        
        List<IDataset> data = new ArrayList<IDataset>();
        List<IContain1DData> orig = process.getDatasetList();
        
        if (process.isAminusB()) {
        	data.add(orig.get(0).getyDatasets().get(0));
        	data.add(Maths.multiply((AbstractDataset)orig.get(1).getyDatasets().get(0),process.getScale()));
        } else {
        	data.add(orig.get(1).getyDatasets().get(0));
        	data.add(Maths.multiply((AbstractDataset)orig.get(0).getyDatasets().get(0), process.getScale()));
        }
        
        data.add(out.get(0).getyDatasets().get(0));
        
		system.clear();
		system.createPlot1D(out.get(0).getxDataset(),data ,null);
		
	}
	
	public void setVisible(boolean visible) {
		   super.setVisible(visible);
		   // Set the initial field focus
		   if (visible) {
		      system.setFocus();
		   }
		}
}