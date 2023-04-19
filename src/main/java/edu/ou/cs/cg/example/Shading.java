//******************************************************************************
// Copyright (C) 2016-2020 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Dec 15 12:30:41 2020 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160425 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200121 [weaver]:	Modified to set up OpenGL and UI on the Swing thread.
// 20201215 [weaver]:	Added setIdentifyPixelScale() to canvas setup.
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
 * The <CODE>Shading</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class Shading
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
		SwingUtilities.invokeLater(new Shading(args));
	}

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public Shading(String[] args)
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
		JFrame			frame = new JFrame("Shading");

		// Rectify display scaling issues when in Hi-DPI mode on macOS.
		edu.ou.cs.cg.utilities.Utilities.setIdentityPixelScale(canvas);

		// Specify the starting width and height of the canvas itself
		canvas.setPreferredSize(new Dimension(1024, 768));

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
		//FPSAnimator	animator = new FPSAnimator(canvas, 60);

		//animator.start();
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
		updatePipeline(drawable);

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
		k++;									// Advance animation counter
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.9f, 0.9f, 0.9f, 0.0f);	// Light gray background
		gl.glClear(GL.GL_COLOR_BUFFER_BIT |		// Clear the color buffer
				   GL.GL_DEPTH_BUFFER_BIT);		// Clear the depth buffer

		// Draw the scene
		drawThings(gl);							// Draw...things!
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
		gl.glEnable(GL.GL_DEPTH_TEST);		// Turn on depth buffer updates

		// See com.jogamp.opengl.fixedfunc.GLLightingFunc
		gl.glEnable(GL2.GL_LIGHTING);		// Turn on lighting
		gl.glShadeModel(GL2.GL_SMOOTH);	// Use smooth (Gouraud) shading
		gl.glEnable(GL2.GL_NORMALIZE);		// Normalize normals before lighting
		gl.glEnable(GL2.GL_LIGHT0);		// Include light 0 in lighting
	}

	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMaterial.xml
	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMatrixMode.xml
	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		// Materials.
		float[]	matAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
		float[]	matDiffuse = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
		float[]	matSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		float[]	matShininess = new float[] { 50.0f };

		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_AMBIENT, matAmbient, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_DIFFUSE, matDiffuse, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, matSpecular, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SHININESS, matShininess, 0);

		// Lights. (One light, behind, above, and slightly left of camera.)
		float[]	lightPosition = new float[] { 2.0f, 6.0f, 3.0f, 0.0f };
		float[]	lightIntensity = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };

		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightIntensity, 0);

		// Camera. (View scene in 3-D, in front from an angle.)
		double		a = 1.2;	// Scaling factor

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(-a * w / h, a * w / h, -a, a, 0.1, 100.0);

		// Action! (Orient the scene relative to the camera.)
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		glu.gluLookAt(2.3, 1.3, 2.0, 0.0, 0.25, 0.0, 0.0, 1.0, 0.0);
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawThings(GL2 gl)
	{
		//gl.glColor3d(0, 0, 0);

		// Jack
		gl.glPushMatrix();
		gl.glTranslated(0.4, 0.4, 0.6);
		gl.glRotated(45.0, 0.0, 0.0, 1.0);
		gl.glScaled(0.08, 0.08, 0.08);
		drawJack(gl);
		gl.glPopMatrix();

		// Teapot
		gl.glPushMatrix();
		gl.glTranslated(0.6, 0.38, 0.5);
		gl.glRotated(30.0, 0.0, 1.0, 0.0);
		MYGLUT.glutSolidTeapot(0.08);
		gl.glPopMatrix();

		// Sphere
		gl.glPushMatrix();
		gl.glTranslated(0.25, 0.42, 0.35);
		MYGLUT.glutSolidSphere(0.1, 15, 15);
		gl.glPopMatrix();

		// Table
		gl.glPushMatrix();
		gl.glTranslated(0.4, 0.0, 0.4);
		drawTable(gl, 0.6, 0.02, 0.02, 0.3);
		gl.glPopMatrix();

		// Walls
		gl.glPushMatrix();
		gl.glRotated(0.0, 0.0, 1.0, 0.0);
		drawWall(gl, 0.02);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotated(90.0, 0.0, 0.0, 1.0);
		drawWall(gl, 0.02);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glRotated(-90.0, 1.0, 0.0, 0.0);
		drawWall(gl, 0.02);
		gl.glPopMatrix();
	}

	private void	drawWall(GL2 gl, double thickness)
	{
		gl.glPushMatrix();

		gl.glTranslated(0.5, 0.5 * thickness, 0.5);
		gl.glScaled(1.0, thickness, 1.0);
		MYGLUT.glutSolidCube(1.0f);

		gl.glPopMatrix();
	}

	private void	drawTable(GL2 gl, double topWidth, double topThickness,
							  double legThickness, double legLength)
	{
		gl.glPushMatrix();

		gl.glTranslated(0.0, legLength, 0.0);
		gl.glScaled(topWidth, topThickness, topWidth);
		MYGLUT.glutSolidCube(1.0f);

		gl.glPopMatrix();

		double	distance = 0.95 * (0.5 * topWidth) - (0.5 * legThickness);

		gl.glPushMatrix();

		gl.glTranslated(distance, 0.0, distance);
		drawTableLeg(gl, legThickness, legLength);

		gl.glTranslated(0.0, 0.0, -2.0 * distance);
		drawTableLeg(gl, legThickness, legLength);

		gl.glTranslated(-2.0 * distance, 0, 2.0 * distance);
		drawTableLeg(gl, legThickness, legLength);

		gl.glTranslated(0.0, 0.0, -2.0 * distance);
		drawTableLeg(gl, legThickness, legLength);

		gl.glPopMatrix();
	}

	private void	drawTableLeg(GL2 gl, double thickness, double length)
	{
		gl.glPushMatrix();

		gl.glTranslated(0.0, 0.5 * length, 0.0);
		gl.glScaled(thickness, length, thickness);
		MYGLUT.glutSolidCube(1.0f);

		gl.glPopMatrix();
	}

	private void	drawJack(GL2 gl)
	{
		gl.glPushMatrix();

		drawJackPart(gl);
		gl.glRotated(90.0, 0.0, 1.0, 0.0);
		drawJackPart(gl);
		gl.glRotated(90.0, 1.0, 0.0, 0.0);
		drawJackPart(gl);

		gl.glPopMatrix();
	}

	private void	drawJackPart(GL2 gl)
	{
		gl.glPushMatrix();
		gl.glScaled(0.2, 0.2, 1.0);
		MYGLUT.glutSolidSphere(1.0, 15, 15);	// Core part (stretched sphere)
		gl.glPopMatrix();

		gl.glPushMatrix();

		gl.glTranslated(0.0, 0.0, 1.2);
		MYGLUT.glutSolidSphere(0.2, 15, 15);	// Ball on one end

		gl.glTranslated(0.0, 0.0, -2.4);
		MYGLUT.glutSolidSphere(0.2, 15, 15);	// Ball on other end

		gl.glPopMatrix();
	}

	// Warning! Text is drawn in unprojected canvas/viewport coordinates.
	// For more on text rendering, the example on this page is long but helpful:
	// jogamp.org/jogl-demos/src/demos/j2d/FlyingText.java
	private void	drawText(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);
		renderer.setColor(0.75f, 0.75f, 0.75f, 1.0f);
		renderer.draw("Shading", 2, h - 14);
		renderer.endRendering();
	}
}

//******************************************************************************
