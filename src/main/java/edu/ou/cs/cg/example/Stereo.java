//******************************************************************************
// Copyright (C) 2016-2021 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Dec 15 12:32:28 2020 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160418 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200121 [weaver]:	Modified to set up OpenGL and UI on the Swing thread.
// 20201215 [weaver]:	Added setIdentifyPixelScale() to canvas setup.
// 20210209 [weaver]:	Added point smoothing for Hi-DPI displays.
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
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

//******************************************************************************

/**
 * The <CODE>Stereo</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Stereo
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
	private TextRenderer		renderer;

	//**********************************************************************
	// Main
	//**********************************************************************

	public static void	main(String[] args)
	{
		SwingUtilities.invokeLater(new Stereo(args));
	}

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Stereo(String[] args)
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
		JFrame			frame = new JFrame("Stereo");

		// Rectify display scaling issues when in Hi-DPI mode on macOS.
		edu.ou.cs.cg.utilities.Utilities.setIdentityPixelScale(canvas);

		// Specify the starting width and height of the canvas itself
		canvas.setPreferredSize(new Dimension(1000, 750));

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
		FPSAnimator	animator = new FPSAnimator(canvas, 60);

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

		initPipeline(drawable);
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
		k++;										// Advance animation counter
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);	// White background
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);			// Clear the buffer

		int	n = 2;									// Number of eyes

		for (int i=0; i<n; i++)						// Draw for each eye
		{
			updatePipeline(drawable, i, n);

			// Draw the scene
			drawThings(gl);							// Draw...things!
		}

		//drawText(drawable);						// Draw some text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glLightModel.xml
	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glShadeModel.xml
	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		// See com.jogamp.opengl.GL
		gl.glEnable(GL2.GL_LINE_SMOOTH);	// Turn on line anti-aliasing

		gl.glEnable(GL.GL_BLEND);			// Turn on color channel blending
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void	updatePipeline(GLAutoDrawable drawable, int i, int n)
	{
		GL2	gl = drawable.getGL().getGL2();
		GLU	glu = GLU.createGLU();

		// ****************************************
		// Zeroth step: Determine which part of the screen/window to draw into
		// ****************************************

		// Let JOGL take care of this (usually)
		//gl.glViewport(0, 0, w, h);
		gl.glViewport(i * w / n, 0, w / n - 8, h);	// N ports left to right

		// Use to adjust the frustum (clipped volume) relative to viewport
		float	aspect = (float)w/(float)h;	// Aspect ratio of viewport

		// ****************************************
		// First step: Position and orient the default camera
		// ****************************************

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		// v1: parallel projection (scaled by >sqrt(3) so cube always fits)
		//gl.glOrtho(-1.75 * aspect, 1.75 * aspect, -1.75, 1.75, 0.1, 100);

		// v2: perspective projection with 45 degree field-of-view
		// closest z shown is 0.1, farthest z shown is 10.0
		//glu.gluPerspective(45.0f, aspect, 0.1f, 10.0f);
		glu.gluPerspective(45.0f, aspect / n, 0.1f, 10.0f);

		// ****************************************
		// Second step: Position and orient the actual camera
		// www.opengl.org/archives/resources/faq/technical/viewing.htm
		// ****************************************

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// v1: camera at X=Y=Z=1.5, looking at origin
		//glu.gluLookAt(1.5, 1.5, 1.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);

		// v2: camera at X=Y=Z=3.0, looking at center of cube
		//glu.gluLookAt(3.0, 3.0, 3.0, 0.01, 0.01, 0.5, 0.0, 1.0, 0.0);

		// v3: camera at X=eye, Y=Z=0.0, looking at center of cube
		glu.gluLookAt(3.0 + i * 0.20, 0.0, 4.0, 0.5, 0.5, 0.5, 0.0, 1.0, 0.0);

		// ****************************************
		// Third step: position and orient the scene
		// ****************************************

		// v1: rotate x, y, z at independent rates over time
		gl.glRotated((k / 50.0), (k / 30.0), (k / 20.0), 0);

		// v2: scale x, y, z so that cube pulses over time
		double	f = Math.cos(2.0 * Math.PI * (k % 1200) / 1200.0);

		gl.glScaled(f * 0.1 + 1.0, f * 0.2 + 1.0, f * 0.4 + 1.0);

		// ****************************************
		// Fourth step: position and orient objects in the scene
		// ****************************************

		// This all happens later in render()
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawThings(GL2 gl)
	{
		gl.glColor4f(0.75f, 0.75f, 0.75f, 0.50f);	// Translucent medium gray

		// Draw axes
		drawAxis(gl, 0.5);
		gl.glPushMatrix();
		gl.glRotated(90.0, 0.0, 1.0, 0);
		drawAxis(gl, 0.5);
		gl.glRotated(-90.0, 1.0, 0.0, 0);
		drawAxis(gl, 0.5);
		gl.glPopMatrix();

		gl.glColor4f(0.75f, 0.75f, 0.75f, 0.25f);	// Translucent light gray

		gl.glLineWidth(3.0f);					// Set line width (in pixels)
		gl.glPushMatrix();
		gl.glTranslated(0.5, 0.5, 0.5);
		MYGLUT.glutWireCube(1.0f);
		gl.glPopMatrix();
		gl.glLineWidth(1.0f);					// Reset line width (in pixels)

		gl.glColor4f(0.25f, 0.25f, 0.25f, 1.00f);	// Opaque dark gray

		gl.glPushMatrix();
		gl.glTranslated(1.0, 1.0, 0.0);
		MYGLUT.glutWireSphere(0.25, 10, 8);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(1.0, 0.0, 1.0);
		MYGLUT.glutWireCone(0.2, 0.5, 10, 8);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(1.0, 1.0, 1.0);
		gl.glRotated((k / 2.0), (k / 3.0), (k / 5.0), 0);
		MYGLUT.glutWireTeapot(0.2);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(0.0, 1.0, 0.0);
		gl.glRotated(90.0, 1.0, 0.0, 0);
		MYGLUT.glutWireTorus(0.1, 0.3, 10, 10);
		////MYGLUT.glutWireTorus(0.1, 0.3, 5 + (k % 123), 5 + (k % 123));
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(1.0, 0.0, 0.0);
		gl.glScaled(0.15, 0.15, 0.15);
		MYGLUT.glutWireDodecahedron();
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(0.0, 1.0, 1.0);
		////MYGLUT.glutWireCube(0.25f);
		gl.glScaled(0.25, 0.25, 0.25);
		MYGLUT.glutWireIcosahedron();
		gl.glPopMatrix();

		GLU		glu = GLU.createGLU();
		GLUquadric	qobj = glu.gluNewQuadric();

		glu.gluQuadricDrawStyle(qobj, GLU.GLU_LINE);
		gl.glPushMatrix();
		gl.glTranslated(0.0, 0.0, 1.0);
		glu.gluCylinder(qobj, 0.2, 0.2, 0.4, 8, 8);
		gl.glPopMatrix();
	}

	private void	drawAxis(GL2 gl, double length)
	{
		gl.glPushMatrix();

		gl.glLineWidth(5.0f);					// Set line width (in pixels)
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3d(0.0, 0.0, 0.0);
		gl.glVertex3d(0.0, 0.0, length);
		gl.glEnd();
		gl.glLineWidth(1.0f);					// Reset line width (in pixels)

		gl.glTranslated(0.0, 0.0, length - 0.2);
		MYGLUT.glutWireCone(0.04, 0.2, 12, 9);

		gl.glPopMatrix();
	}

	// Warning! Text is drawn in unprojected canvas/viewport coordinates.
	// For more on text rendering, the example on this page is long but helpful:
	// jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	private void	drawText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);
		renderer.draw("Stereo", 2, h - 14);
		renderer.endRendering();
	}
}

//******************************************************************************
