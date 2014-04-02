/*-
 * Copyright 2014 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dawnsci.plotting.draw2d.swtxy.selection;

import java.util.ArrayList;
import java.util.List;

import org.dawnsci.plotting.api.axis.ICoordinateSystem;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.region.IRegionContainer;
import org.dawnsci.plotting.api.region.ROIEvent;
import org.dawnsci.plotting.draw2d.swtxy.translate.FigureTranslator;
import org.dawnsci.plotting.draw2d.swtxy.translate.TranslationEvent;
import org.dawnsci.plotting.draw2d.swtxy.translate.TranslationListener;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;
import uk.ac.diamond.scisoft.analysis.roi.handler.HandleStatus;
import uk.ac.diamond.scisoft.analysis.roi.handler.ROIHandler;

/**
 * Class for a shape based on a ROI and uses a ROIHandler
 */
public abstract class ROIShape<T extends IROI> extends Shape implements IRegionContainer {
	protected Figure parent;
	protected AbstractSelectionRegion region;
	protected ICoordinateSystem cs;
	protected List<IFigure> handles;
	protected List<FigureTranslator> fTranslators;
	protected ROIHandler roiHandler;
	private TranslationListener handleListener;
	protected FigureListener moveListener;
	private boolean isMobile;
	protected Rectangle bnds;
	protected boolean dirty = true;
	protected T croi;
	private T troi = null; // temporary ROI used in dragging

	protected static final int SIDE = 8;

	public ROIShape(final Figure parent, AbstractSelectionRegion region) {
		super();
		this.parent = parent;
		this.region = region;
		cs = region.getCoordinateSystem();
		handles = new ArrayList<IFigure>();
		fTranslators = new ArrayList<FigureTranslator>();
		roiHandler = createROIHandler(croi);
		handleListener = createHandleNotifier();
		moveListener = new FigureListener() {
			@Override
			public void figureMoved(IFigure source) {
				parent.repaint();
			}
		};
	}

	abstract protected ROIHandler createROIHandler(IROI roi);

	abstract public void setCentre(Point nc);

	abstract public void setup(PointList corners);

	@Override
	public boolean containsPoint(int x, int y) {
		if (croi == null)
			return super.containsPoint(x, y);
		double[] pt = cs.getPositionValue(x, y);
		return croi.containsPoint(pt[0], pt[1]);
	}

	protected void calcBox(IROI proi, boolean redraw) {
		RectangularROI rroi = (RectangularROI) proi.getBounds();
		int[] bp = cs.getValuePosition(rroi.getPointRef());
		int[] ep = cs.getValuePosition(rroi.getEndPoint());
		bnds = new Rectangle(new Point(bp[0], bp[1]), new Point(ep[0], ep[1]));
		if (redraw) {
			setBounds(bnds);
		}
		dirty = false;
	}

	public void dispose() {
		for (IFigure f : handles) {
			((SelectionHandle) f).removeMouseListeners();
		}
		for (FigureTranslator t : fTranslators) {
			t.removeTranslationListeners();
		}
		removeFigureListener(moveListener);
	}

	public T getROI() {
		return troi != null ? troi : croi;
	}

	protected void configureHandles() {
		boolean mobile = region.isMobile();
		boolean visible = isVisible() && mobile;
		// handles
		FigureTranslator mover;
		final int imax = roiHandler.size();
		for (int i = 0; i < imax; i++) {
			double[] hpt = roiHandler.getAnchorPoint(i, SIDE);
			roiHandler.set(i, i);
			
			int[] p = cs.getValuePosition(hpt);
			RectangularHandle h = new RectangularHandle(cs, region.getRegionColor(), this, SIDE, p[0], p[1]);
			h.setVisible(visible);
			parent.add(h);
			mover = new FigureTranslator(region.getXyGraph(), h);
			mover.setActive(mobile);
			mover.addTranslationListener(handleListener);
			fTranslators.add(mover);
			h.addFigureListener(moveListener);
			handles.add(h);
		}

		addFigureListener(moveListener);
		mover = new FigureTranslator(region.getXyGraph(), parent, this, handles) {
			public void mouseDragged(MouseEvent event) {
				super.mouseDragged(event);
			}
		};
		mover.setActive(mobile);
		mover.addTranslationListener(createRegionNotifier());
		fTranslators.add(mover);
		region.setRegionObjects(this, handles);
		Rectangle b = getBounds();
		if (b != null)
			setBounds(b);
	}

	private TranslationListener createRegionNotifier() {
		return new TranslationListener() {
			@Override
			public void translateBefore(TranslationEvent evt) {
			}

			@Override
			public void translationAfter(TranslationEvent evt) {
				region.updateBounds();
				region.fireROIDragged(region.createROI(false), ROIEvent.DRAG_TYPE.TRANSLATE);
			}

			@Override
			public void translationCompleted(TranslationEvent evt) {
				region.fireROIChanged(region.createROI(true));
				roiHandler.setROI(croi);
				region.fireROISelection();
			}

			@Override
			public void onActivate(TranslationEvent evt) {
			}
		};
	}

	private TranslationListener createHandleNotifier() {
		return new TranslationListener() {
			private double[] spt;

			@Override
			public void onActivate(TranslationEvent evt) {
				troi = null;
				Object src = evt.getSource();
				if (src instanceof FigureTranslator) {
					final FigureTranslator translator = (FigureTranslator) src;
					Point start = translator.getStartLocation();
					spt = cs.getPositionValue(start.x(), start.y());
					final IFigure handle = translator.getRedrawFigure();
					final int h = handles.indexOf(handle);
					HandleStatus status = h == roiHandler.getCentreHandle() ? HandleStatus.RMOVE : HandleStatus.RESIZE;
					roiHandler.configureDragging(h, status);
				}
			}

			@Override
			public void translateBefore(TranslationEvent evt) {
			}

			@SuppressWarnings("unchecked")
			@Override
			public void translationAfter(TranslationEvent evt) {
				Object src = evt.getSource();
				if (src instanceof FigureTranslator) {
					final FigureTranslator translator = (FigureTranslator) src;
					Point end = translator.getEndLocation();
					
					if (end==null) return;
					double[] c = cs.getPositionValue(end.x(), end.y());
					troi = (T) roiHandler.interpretMouseDragging(spt, c);

					intUpdateFromROI(troi);
					region.fireROIDragged(troi, roiHandler.getStatus() == HandleStatus.RESIZE ?
							ROIEvent.DRAG_TYPE.RESIZE : ROIEvent.DRAG_TYPE.TRANSLATE);
				}
			}

			@Override
			public void translationCompleted(TranslationEvent evt) {
				Object src = evt.getSource();
				if (src instanceof FigureTranslator) {
					troi = null;
					final FigureTranslator translator = (FigureTranslator) src;
					Point end = translator.getEndLocation();

					double[] c = cs.getPositionValue(end.x(), end.y());
					@SuppressWarnings("unchecked")
					T croi = (T) roiHandler.interpretMouseDragging(spt, c);

					updateFromROI(croi);
					roiHandler.unconfigureDragging();
					region.createROI(true);

					region.fireROIChanged(croi);
					region.fireROISelection();
				}
			}
		};
	}

	/**
	 * Called by updateBounds in region notifiers
	 * @return
	 */
	protected Rectangle updateFromHandles() {
		if (handles.size() > 0) {
			IFigure f = handles.get(roiHandler.getCentreHandle()); // centre point
			if (f instanceof SelectionHandle) {
				SelectionHandle h = (SelectionHandle) f;
				setCentre(h.getSelectionPoint());
			}
		}
		return getBounds();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		for (IFigure h : handles) {
			if (isMobile && visible && !h.isVisible())
				h.setVisible(true);
		}
	}

	public void setMobile(boolean mobile) {
		if (mobile == isMobile)
			return;
		isMobile = mobile;

		for (IFigure h : handles) {
			if (h.isVisible() != mobile)
				h.setVisible(mobile);
		}
		for (FigureTranslator f : fTranslators) {
			f.setActive(mobile);
		}
	}

	/**
	 * @return list of handle points (can be null)
	 */
	public PointList getPoints() {
		int imax = handles.size() - 1;
		if (imax < 0)
			return null;

		PointList pts = new PointList(imax);
		for (int i = 0; i < imax; i++) {
			IFigure f = handles.get(i);
			if (f instanceof SelectionHandle) {
				SelectionHandle h = (SelectionHandle) f;
				pts.addPoint(h.getSelectionPoint());
			}
		}
		return pts;
	}

	@Override
	public Rectangle getBounds() {
		if (getROI() != null && dirty)
			calcBox(getROI(), false);
		dirty = false;
		Rectangle b = bnds == null ? super.getBounds() : new Rectangle(bnds);
		if (handles != null) {
			for (IFigure f : handles) {
				if (f instanceof SelectionHandle) {
					SelectionHandle h = (SelectionHandle) f;
					b.union(h.getBounds());
				}
			}
		}
		return b;
	}

	/**
	 * Update according to ROI
	 * @param rroi
	 */
	public void updateFromROI(T rroi) {
		croi = rroi;
		roiHandler.setROI(rroi);
		intUpdateFromROI(rroi);
	}

	/**
	 * Update according to ROI
	 * @param rroi
	 */
	private void intUpdateFromROI(IROI rroi) {
		int imax = handles.size();
		if (imax != roiHandler.size()) {
			configureHandles();
		} else {
			ROIHandler handler = createROIHandler(rroi);
			for (int i = 0; i < imax; i++) {
				double[] hpt = handler.getAnchorPoint(i, SIDE);
				SelectionHandle handle = (SelectionHandle) handles.get(i);
				int[] pta = cs.getValuePosition(hpt);
				handle.setSelectionPoint(new Point(pta[0], pta[1]));
			}
		}
		dirty = true;
		calcBox(rroi, true);
	}

	@Override
	public IRegion getRegion() {
		return region;
	}

	@Override
	public void setRegion(IRegion region) {
	}

}