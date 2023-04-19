//******************************************************************************
// Copyright (C) 2019-2023 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Sat Apr  1 13:42:44 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20190227 [weaver]:	Original file.
// 20190318 [weaver]:	Modified for homework04.
// 20230401 [weaver]:	Added extensive TODO comments and sample code.
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
//   4. Add a method to modify (setFoo) the variable *asynchronously*.
//
// Concurrency management is important because the JOGL and the Java AWT run on
// different threads. The modify methods use the GLAutoDrawable.invoke() method
// so that all changes to variables take place on the JOGL thread. Because this
// happens at the END of GLEventListener.display(), all changes will be visible
// to the View.update() and render() methods in the next animation cycle.
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework05;

//import java.lang.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;
import com.jogamp.opengl.*;
import edu.ou.cs.cg.utilities.*;

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
	private final View						view;

	// Model variables
	private Point2D.Double					cursor;	// Current cursor coords

	// Nodes
	// TODO: PUT MEMBERS FOR THE NODES HERE
	// private SomeContainerOfNodes			nodes;
	// private Other						stuff;

	// Convex Hull and Pool
	// TODO: PUT MEMBERS FOR THE CONVEX HULL AND EXTRA POOL HERE
	// private SomeContainerOfPoints		hull;
	// private SomeOtherContainerOfPoints	pool;
	// private Other						stuff;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Model(View view)
	{
		this.view = view;

		// Initialize user-adjustable variables (with reasonable default values)
		cursor = null;

		// Initialize nodes and any stuff needed to manage them
		// TODO: INITIALIZE MEMBERS FOR THE NODES HERE

		//nodes = new SomeContainerOfNodes();
		//...use Network to read data then create View.Nodes

		// Initialize convex hull, extra point pool, any stuff to manage them
		// TODO: INITIALIZE MEMBERS FOR THE CONVEX HULL AND EXTRA POOL HERE

		//hull = new SomeContainerOfPoints();
		//pool = new SomeOtherContainerOfPoints();
		//...anything else to do?
	}

	//**********************************************************************
	// Public Methods (Access Variables)
	//**********************************************************************

	public Point2D.Double	getCursor()
	{
		if (cursor == null)
			return null;
		else
			return new Point2D.Double(cursor.x, cursor.y);
	}

	// TODO: ADD ACCESS METHODS FOR YOUR ADDITIONAL MEMBERS HERE (AS NEEDED)
	// Hint: You will need several of these for the View to access various
	// things for drawing.

	//**********************************************************************
	// Public Methods (Modify Variables)
	//**********************************************************************

	public void	setCursorInViewCoordinates(Point q)
	{
		if (q == null)
		{
			view.getCanvas().invoke(false, new BasicUpdater() {
					public void	update(GL2 gl) {
						cursor = null;
					}
				});;
		}
		else
		{
			view.getCanvas().invoke(false, new ViewPointUpdater(q) {
					public void	update(double[] p) {
						cursor = new Point2D.Double(p[0], p[1]);
					}
				});;
		}
	}

	// TODO: ADD MODIFY METHODS FOR YOUR ADDITIONAL MEMBERS HERE (AS NEEDED)
	// Hint: You will need many of these for the KeyHandler to adjust various
	// things, including the details of the currently selected Node. When you
	// show, hide, or transform a node, you will need to update the convex hull
	// by adding, removing, moving (remove then add) the node's center point.

	//public void	setSomething(Things fordoingthat)
	//{
	//	view.getCanvas().invoke(false, new BasicUpdater() {
	//			public void	update(GL2 gl) {
	//				// update model members somehow
	//				// including possibly updating the convex hull and pool
	//			}
	//		});;
	//}

	//**********************************************************************
	// Public Methods (Special)
	//**********************************************************************

	public void	selectNodeInViewCoordinates(Point q)
	{
		view.getCanvas().invoke(false, new ViewPointUpdater(q) {
			public void	update(double[] p) {
				Point2D.Double	r = new Point2D.Double(p[0], p[1]);

				view.selectNodeInSceneCoordinates(r);
			}
		});;
	}

	//**********************************************************************
	// Public Methods (Convex Hull)
	//**********************************************************************

	private void	addPointToHull(Point2D.Double p)
	{
		// TODO: ADD CODE TO ADD THE POINT TO THE HULL
	}

	private void	removePointFromHull(Point2D.Double p)
	{
		// TODO: ADD CODE TO REMOVE THE POINT FROM THE HULL
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
