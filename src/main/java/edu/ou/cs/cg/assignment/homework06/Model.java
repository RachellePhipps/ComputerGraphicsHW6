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

package edu.ou.cs.cg.assignment.homework06;

//import java.lang.*;
import java.awt.Point;
import java.awt.event.KeyEvent;

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
	private final View		view;

	// Model variables
	private double camera_distance;

	private double focal_point_height;

	private double scene_rotation_amount;

	private double scene_rotation_rate;

	private double			foo;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Model(View view)
	{
		this.view = view;

		// TODO: INITIALIZE YOUR MEMBERS HERE (AS NECESSARY)

		camera_distance = 1.0;
		focal_point_height = 0.2;
		scene_rotation_amount = 0.0;
		scene_rotation_rate = 0.0;

		foo = 0.0;
	}

	//**********************************************************************
	// Public Methods (Access Variables)
	//**********************************************************************
	public double	getFoo()
	{
		return foo;
	}

	public double getCamera_distance() {
		double cd_copy = camera_distance; return cd_copy;
	}
	public double getFocal_point_height() {
		double fph_copy = focal_point_height; return fph_copy;
	}

	public double getScene_rotation_amount() {
		double sra_copy = scene_rotation_amount; return sra_copy;
	}

	public double getScene_rotation_rate() {
		double srr_copy = scene_rotation_rate; return srr_copy;
	}

	//**********************************************************************
	// Public Methods (Modify Variables)
	//**********************************************************************
	public void	setFoo(double v)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void	update(GL2 gl) {
				foo = v;
			}
		});;
	}

	/*Add KeyHandler interactions to adjust the camera distance. When the <shift> key is up,
have the <comma> and <period> keys change the distance by x0.5 and x2.0, respectively.
When the <shift> key is down, change the distance by x0.9 and x1.1, respectively.*/
	public void setCamera_distance(boolean shift, int e)
	{
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void update(GL2 gl) {
				if(e == KeyEvent.VK_COMMA)
				{
					//comma + shift -> x0.9
					if (shift)
					camera_distance *= 0.9;
					//comma -> x0.5;
					else
						camera_distance *= 0.5;
				}

				else if (e == KeyEvent.VK_PERIOD)
				{
					//period + shift -> x2.0
					if (shift)
						camera_distance *= 2.0;
					//period -> x1.1
					else
						camera_distance *= 1.1;
				}
			}
		});
	}

	/*Add KeyHandler interactions to adjust the focal point height. When the <shift> key is up,
have the up and down <arrow> keys change the height by +0.2 and -0.2, respectively.
When the <shift> key is down, change the height by +0.02 and -0.02, respectively.*/
	public void setFocal_point_height(boolean shift, int e){
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void update(GL2 gl) {
				if(e == KeyEvent.VK_UP) {
					if (shift)
						//up + shift -> change by +0.02
						focal_point_height += 0.02;
					else
						//up -> change by +0.2
						focal_point_height += 0.2;
				}

				else if (e == KeyEvent.VK_DOWN) {
					//down + shift -> change by -0.02
					if (shift)
						focal_point_height -= 0.02;
					else
					//down -> change by -0.2
						focal_point_height -= 0.2;
				}
			}
		});
	}

	/*Add KeyHandler interactions to adjust scene rotation rate. Have the left and right <arrow>
keys change the rate by +0.1 and -0.1 (by +0.01 and -0.01 when the <shift> key is down).*/
	public void setScene_rotation_rate(boolean shift, int e){
		view.getCanvas().invoke(false, new BasicUpdater() {
			public void update(GL2 gl) {
				if (e == KeyEvent.VK_LEFT) {
					//left + shift -> +0.01
					if(shift)
						scene_rotation_rate += 0.01;
					//left -> +0.1
					else scene_rotation_rate += 0.1;
				}
				else if (e == KeyEvent.VK_RIGHT) {
					//Right + shift -> -0.01
					if (shift)
						scene_rotation_rate -= 0.01;
					//Right -> -0.1
					else
						scene_rotation_rate -= 0.1;
				}
			}
		});
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
