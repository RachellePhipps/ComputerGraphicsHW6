//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Feb  9 17:35:33 2021 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200121 [weaver]:	Modified to set up OpenGL and UI on the Swing thread.
// 20201215 [weaver]:	Added setIdentifyPixelScale() to canvas setup.
// 20210209 [weaver]:	Added point smoothing for Hi-DPI displays.
// 20220402 [weaver]:	Modified from the Lorentz class.
//
//******************************************************************************
// Notes:
//
// Warning! This code uses deprecated features of OpenGL, including immediate
// mode vertex attribute specification, for sake of easier classroom learning.
// See www.khronos.org/opengl/wiki/Legacy_OpenGL
//
//******************************************************************************

package edu.ou.cs.cg.example;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

//******************************************************************************

/**
 * The <CODE></CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Grid
	implements GLEventListener, Runnable
{
	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT	MYGLUT = new GLUT();
	public static final Random	RANDOM = new Random();

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private int				w;				// Canvas width
	private int				h;				// Canvas height
	private int				k = 0;			// Animation counter
	private TextRenderer	renderer;

	private int				n = 0;			// Number of vertices to draw
	private int				m = 0;			// Vertex count while drawing

	//**********************************************************************
	// Main
	//**********************************************************************

	public static void	main(String[] args)
	{
		SwingUtilities.invokeLater(new Grid(args));
	}

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Grid(String[] args)
	{
	}

	//**********************************************************************
	// Override Methods (Runnable)
	//**********************************************************************

	public void	run()
	{
		GLProfile		profile = GLProfile.getDefault();

		System.out.println("Running on Java version " + 
			System.getProperty("java.version"));
		System.out.println("Running with OpenGL version " +
			profile.getName());

		GLCapabilities	capabilities = new GLCapabilities(profile);
		GLCanvas		canvas = new GLCanvas(capabilities);	// Single-buffer
		//GLJPanel		canvas = new GLJPanel(capabilities);	// Double-buffer
		JFrame			frame = new JFrame("Grid");

		// Rectify display scaling issues when in Hi-DPI mode on macOS.
		edu.ou.cs.cg.utilities.Utilities.setIdentityPixelScale(canvas);

		// Specify the starting width and height of the canvas itself
		canvas.setPreferredSize(new Dimension(500, 500));

		// Populate and show the frame
		frame.setBounds(50, 50, 200, 200);
		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Exit when the user clicks the frame's close button
		frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			});

		// Register this class to update whenever OpenGL needs it
		canvas.addGLEventListener(this);

		// Have OpenGL call display() to update the canvas 60 times per second
		FPSAnimator	animator = new FPSAnimator(canvas, 20);

		animator.start();
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	// Called immediately after the GLContext of the GLCanvas is initialized.
	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Serif", Font.PLAIN, 18),
									true, true);

		GL2	gl = drawable.getGL().getGL2();

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing

	}

	// Notification to release resources for the GLContext.
	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	// Called to initiate rendering of each frame into the GLCanvas.
	public void	display(GLAutoDrawable drawable)
	{
		update(drawable);
		render(drawable);
	}

	// Called during the first repaint after a resize of the GLCanvas.
	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	// Update the scene model for the current animation frame.
	private void	update(GLAutoDrawable drawable)
	{
		k++;
		
		n++;						// Advance animation counter
		n = ((n > 120) ? 0 : n);	// Reset point count after 2x vertices

		m = 0;
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);	// White background
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);	// Clear the buffer

		setProjection(gl);				    // Use a custom projection

		// Set ModelView matrix to identity (no transform) to draw the scene.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glPointSize(5.0f);
		gl.glLineWidth(3.0f);

		// Draw the scene
		drawGrid(gl, 0.10);					// Draw the grid
		drawText(drawable);					// Draw some text

		gl.glFlush();						// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	// Position and orient the default camera to view in 2-D, centered above.
	private void	setProjection(GL2 gl)
	{
		GLU	glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);			// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(-0.2f, 0.9f, -0.8f, 0.3f);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawGrid(GL2 gl, double r)
	{
		gl.glPushMatrix();						// New matrix for grid

		// Apply Pythagorean Theorem
		double dx = r * Math.sqrt(3.0) * 0.5;
		double dy = r * 1.5;

		for (int row=0; row<=3; row++)   		// Loop over rows
		{
			gl.glPushMatrix();					// New matrix for row

			if ((row & 1) == 1)					// Odd numbered row?
				gl.glTranslated(dx, 0.0, 0.0);	// Then indent it!

			for (int col=0; col<=4; col++)		// Loop over columns
			{
				gl.glBegin(GL.GL_POINTS);
				gl.glColor3f(1.0f, 0.0f, 0.0f);	// Red
				gl.glVertex2d(0.0, 0.0);		// Draw cell center point
				gl.glEnd();

				drawUnitCell(gl, r);			// Draw cell
				gl.glTranslated(2.0 * dx, 0.0, 0.0);	// Move to next cell
			}

			gl.glPopMatrix();					// Back to start of row
			gl.glTranslated(0.0, -dy, 0.0);		// Move down one row
		}

		gl.glPopMatrix();						// Back to start of grid
	}

	private void	drawUnitCell(GL2 gl, double r)
	{
		gl.glPushMatrix();						// New matrix for cell
		gl.glScaled(r, r, 1.0);					// Scale larger by r
		gl.glRotated(90.0, 0.0, 0.0, 1.0);  	// Rotate CCW about *Z*

		gl.glBegin(GL.GL_LINE_STRIP);			// Draw cell edges

		for (int i=0; i<=3; i++)				// 4 vertices for 3 edges
		{
			double	theta = i * Math.PI / 3.0;	// 60 degrees apart

			gl.glColor3f(0.0f, 0.0f, 1.0f);		// Blue
			gl.glVertex2d(Math.cos(theta), Math.sin(theta));

			if (++m >= n)
				break;							// Apply animation limit
		}

		gl.glEnd();
 
		gl.glPopMatrix();						// Back to start of cell
	}

	// Warning! Text is drawn in unprojected canvas/viewport coordinates.
	// For more on text rendering, the example on this page is long but helpful:
	// jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	private void	drawText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);
		renderer.draw("Grid (Vertices: " + n + ")", 2, h - 14);
		renderer.endRendering();
	}
}

//******************************************************************************
