//******************************************************************************
// Copyright (C) 2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Wed Apr 24 16:37:17 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20190227 [weaver]:	Original file.
// 20190318 [weaver]:	Modified for homework04.
//
//******************************************************************************
//
// The model manages all of the user-adjustable variables utilized in the scene.
// (You can store non-user-adjustable scene data here too, if you want.)
//
// For each variable that you want to make interactive:
//
//   1. Add a member of the right type
//   2. Initialize it to a reasonable default value in the constructor.
//   3. Add a method to access (getFoo) a copy of the variable's current value.
//   4. Add a method to modify (setFoo) the variable.
//
// Concurrency management is important because the JOGL and the Java AWT run on
// different threads. The modify methods use the GLAutoDrawable.invoke() method
// so that all changes to variables take place on the JOGL thread. Because this
// happens at the END of GLEventListener.display(), all changes will be visible
// to the View.update() and render() methods in the next animation cycle.
//
//******************************************************************************

package edu.ou.cs.cg.application.texture;

//import java.lang.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import com.jogamp.opengl.*;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>Model</CODE> class.
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Model
{
	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final View					view;

	// Model variables
	private int						slices;

	private double						position;
	private double						rotation;
	private double						scale;
	private double						radius;

	private boolean					showEdges;
	private boolean					showSun;
	private boolean					showMovers;
	private boolean					showGround;
	private boolean					showSky;
	private boolean					showHorizon;
	private boolean					showRearView;
	private boolean					showDownView;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Model(View view)
	{
		this.view = view;

		// Initialize user-adjustable variables (with reasonable default values)
		slices = 12;

		position = 0.0;
		rotation = 0.0;
		scale = 2.0;
		radius = 4.0;

		showEdges = false;
		showSun = false;
		showMovers = false;
		showGround = false;
		showSky = false;
		showHorizon = false;
		showRearView = false;
		showDownView = false;
	}

	//**********************************************************************
	// Public Methods (Access Variables)
	//**********************************************************************

	public int	getSlices()
	{
		return slices;
	}

	public double	getPosition()
	{
		return position;
	}

	public double	getRotation()
	{
		return rotation;
	}

	public double	getScale()
	{
		return scale;
	}

	public double	getRadius()
	{
		return radius;
	}

	public boolean	getShowEdges()
	{
		return showEdges;
	}

	public boolean	getShowSun()
	{
		return showSun;
	}

	public boolean	getShowMovers()
	{
		return showMovers;
	}

	public boolean	getShowGround()
	{
		return showGround;
	}

	public boolean	getShowSky()
	{
		return showSky;
	}

	public boolean	getShowHorizon()
	{
		return showHorizon;
	}

	public boolean	getShowRearView()
	{
		return showRearView;
	}

	public boolean	getShowDownView()
	{
		return showDownView;
	}

	//**********************************************************************
	// Public Methods (Modify Variables)
	//**********************************************************************

	public void	setSlices(int v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				slices = Math.max(3, Math.min(v, 32));
			}
		});;
	}

	public void	setPosition(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				position = v;
			}
		});;
	}

	public void	setRotation(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				rotation = v;
			}
		});;
	}

	public void	setScale(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				scale = v;
			}
		});;
	}

	public void	setRadius(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				radius = v;
			}
		});;
	}

	public void	toggleShowEdges()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showEdges = !showEdges;
			}
		});;
	}

	public void	toggleShowSun()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showSun = !showSun;
			}
		});;
	}

	public void	toggleShowMovers()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showMovers = !showMovers;
			}
		});;
	}

	public void	toggleShowGround()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showGround = !showGround;
			}
		});;
	}

	public void	toggleShowSky()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showSky = !showSky;
			}
		});;
	}

	public void	toggleShowHorizon()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showHorizon = !showHorizon;
			}
		});;
	}

	public void	toggleShowRearView()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showRearView = !showRearView;
			}
		});;
	}

	public void	toggleShowDownView()
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				showDownView = !showDownView;
			}
		});;
	}

	//**********************************************************************
	// Inner Classes
	//**********************************************************************

	// Convenience class to simplify the implementation of most updaters.
	private abstract class BasicUpdater implements GLRunnable
	{
		public final boolean	run(GLAutoDrawable drawable)
		{
			GL2	gl = drawable.getGL().getGL2();

			update(gl);

			return true;	// Let animator take care of updating the display
		}

		public abstract void	update(GL2 gl);
	}

	// Convenience class to simplify updates in cases in which the input is a
	// single point in view coordinates (integers/pixels).
	private abstract class ViewPointUpdater extends BasicUpdater
	{
		private final Point	q;

		public ViewPointUpdater(Point q)
		{
			this.q = q;
		}

		public final void	update(GL2 gl)
		{
			int		h = view.getHeight();
			double[]	p = Utilities.mapViewToScene(gl, q.x, h - q.y, 0.0);

			update(p);
		}

		public abstract void	update(double[] p);
	}
}

//******************************************************************************
