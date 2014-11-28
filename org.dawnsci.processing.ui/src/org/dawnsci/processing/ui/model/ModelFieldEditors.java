package org.dawnsci.processing.ui.model;

import java.util.Arrays;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawnsci.common.widgets.celleditor.CComboCellEditor;
import org.dawnsci.common.widgets.celleditor.ClassCellEditor;
import org.dawnsci.common.widgets.celleditor.FileDialogCellEditor;
import org.dawnsci.plotting.roi.RegionCellEditor;
import org.eclipse.dawnsci.analysis.api.processing.model.FileType;
import org.eclipse.dawnsci.analysis.api.processing.model.ModelField;
import org.eclipse.dawnsci.analysis.api.processing.model.OperationModelField;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

public class ModelFieldEditors {

	private static ISelectionListener selectionListener;
	private static ToolTip            currentHint;
	/**
	 * Create a new editor for a field.
	 * @param field
	 * @return
	 */
	public static CellEditor createEditor(ModelField field, Composite parent) {
        
		Object value;
		try {
			value = field.get();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		Class<? extends Object> clazz = null;
		if (value!=null) {
			clazz = value.getClass();
		} else {
			try {
				clazz = field.getType();
			} catch (NoSuchFieldException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
        
		CellEditor ed = null;
    	final OperationModelField anot = field.getAnnotation();
    	
        if (clazz == Boolean.class) {
        	ed = new CheckboxCellEditor(parent, SWT.NONE);
        	
        } else if (Number.class.isAssignableFrom(clazz) || isNumberArray(clazz)) {        	
        	ed = getNumberEditor(field, clazz, parent);
        	
        } else if (IROI.class.isAssignableFrom(clazz)) {        	
        	ed = new RegionCellEditor(parent);
        	
        } else if (Enum.class.isAssignableFrom(clazz)) {
        	ed = getChoiceEditor((Class<? extends Enum>)clazz, parent);
        	
        } else if (FileDialogCellEditor.isEditorFor(clazz) || (anot!=null && anot.file()!=FileType.NONE)) {
        	FileDialogCellEditor fe = new FileDialogCellEditor(parent);
        	fe.setValueClass(clazz);
        	ed = fe;
        	if (anot!=null) {
        		fe.setDirectory(anot.file().isDirectory());
        		fe.setNewFile(anot.file().isNewFile());
        	}
        
        } else if (String.class.equals(clazz)) {
        	ed = new TextCellEditor(parent);
        }
        
        // Show the tooltip, if there is one
        if (ed!=null) {
        	if (anot!=null) {
        		String hint = anot.hint();
        		if (hint!=null && !"".equals(hint)) {
        			showHint(hint, parent);
        		}
        	}
        }
        
        return ed;

	}
	

	private static void showHint(final String hint, final Composite parent) {
		
		if (parent.isDisposed()) return;
		if (parent!=null) parent.getDisplay().asyncExec(new Runnable() {
			public void run() {
				
				currentHint = new DefaultToolTip(parent, ToolTip.NO_RECREATE, true);
				((DefaultToolTip)currentHint).setText(hint);
				currentHint.setHideOnMouseDown(true);
				currentHint.show(new Point(0, parent.getSize().y));
				
				if (selectionListener==null) {
					if (EclipseUtils.getPage()!=null) {
						selectionListener = new ISelectionListener() {
							@Override
							public void selectionChanged(IWorkbenchPart part, ISelection selection) {
								if (currentHint!=null) currentHint.hide();
							} 
						};
						
						EclipseUtils.getPage().addSelectionListener(selectionListener);
					}

				}
			}
		});
	}

	private static boolean isNumberArray(Class<? extends Object> clazz) {
		
		if (clazz==null)      return false;
		if (!clazz.isArray()) return false;
		
		return double[].class.isAssignableFrom(clazz) || float[].class.isAssignableFrom(clazz) ||
               int[].class.isAssignableFrom(clazz)    || long[].class.isAssignableFrom(clazz);
	}

	private static CellEditor getChoiceEditor(final Class<? extends Enum> clazz, Composite parent) {
		
		final Enum[]   values = clazz.getEnumConstants();
	    final String[] items  = Arrays.toString(values).replaceAll("^.|.$", "").split(", ");
		
		CComboCellEditor cellEd = new CComboCellEditor(parent, items) {
    	    protected void doSetValue(Object value) {
                if (value instanceof Enum) value = ((Enum) value).ordinal();
                super.doSetValue(value);
    	    }
    		protected Object doGetValue() {
    			Integer ordinal = (Integer)super.doGetValue();
    			return values[ordinal];
    		}
		};
		
		return cellEd;
	}

	private static CellEditor getNumberEditor(ModelField field, final Class<? extends Object> clazz, Composite parent) {
    	
		OperationModelField anot = field.getAnnotation();
		CellEditor textEd = null;
	    if (anot!=null) {
	    	textEd = new ClassCellEditor(parent, clazz, anot.min(), anot.max(), anot.unit(), SWT.NONE);
	    } else {
	    	textEd = new ClassCellEditor(parent, clazz, SWT.NONE);
	    }

    	return textEd;
	}

}
