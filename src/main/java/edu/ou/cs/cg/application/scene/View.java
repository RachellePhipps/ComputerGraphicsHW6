//******************************************************************************
// Copyright (C) 2016-2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Apr 16 20:45:02 2021 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.scene;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

//******************************************************************************

/**
 * The <CODE>View</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class View
	implements GLEventListener
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final int			DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.000");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final String[]		MODE_NAMES = new String[]
	{
		"Material.Ambient",
		"Material.Diffuse",
		"Material.Specular",
		"Material.Shininess",
		"Light0.SourceX",
		"Light0.SourceY",
		"Light0.SourceZ",
		"Light0.Intensity",
		"Eye.X",
		"Eye.Y",
		"Eye.Z",
		"Scale",
	};

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel			canvas;
	private int					w;			// Canvas width
	private int					h;			// Canvas height

	private final KeyHandler		keyHandler;
	private final MouseHandler		mouseHandler;

	private final FPSAnimator		animator;
	private int					counter;	// Frame counter

	private TextRenderer			renderer;

	private int					sides;		// Sphere sides
	private int					mode;		// Parameters
	private float[]				v;			// Parameter values

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize model (give user-adjustables reasonable default values)
		sides = 15;
		mode = 0;
		v = new float[] { 0.7f, 0.6f, 1.0f, 0.5f,		// Materials
						  2.0f, 6.0f, 3.0f, 0.7f,		// Lighting
						  2.3f, 1.3f, 2.0f, 0.75f };	// Eye position & scale

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		//animator.start();

		// Initialize interaction
		keyHandler = new KeyHandler(this);
		mouseHandler = new MouseHandler(this);
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
	}

	public int	getMode()
	{
		return mode;
	}

	public void	setMode(int mode)
	{
		this.mode = mode % v.length;

		canvas.repaint();
	}

	public float	getValue(int mode)
	{
		return v[mode % v.length];
	}

	public void	setValue(int mode, float w)
	{
		v[mode % v.length] = w;

		canvas.repaint();
	}

	public void	decrementSides()
	{
		if (sides > 1)
		{
			sides--;
			canvas.repaint();
		}
	}

	public void	incrementSides()
	{
		if (sides < 64)
		{
			sides++;
			canvas.repaint();
		}
	}

	//**********************************************************************
	// Public Methods
	//**********************************************************************

	public Component	getComponent()
	{
		return (Component)canvas;
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
	}

	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	private void	update(GLAutoDrawable drawable)
	{
		counter++;									// Advance animation counter
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT |		// Clear the color buffer
				   GL.GL_DEPTH_BUFFER_BIT);		// Clear the depth buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.9f, 0.9f, 0.9f, 0.0f);	// Light gray background

		// See com.jogamp.opengl.GL
		gl.glEnable(GL.GL_DEPTH_TEST);		// Turn on depth buffer updates

		// See com.jogamp.opengl.fixedfunc.GLLightingFunc
		gl.glEnable(GL2.GL_LIGHTING);		// Turn on lighting
		gl.glShadeModel(GL2.GL_SMOOTH);	// Use smooth (Gouraud) shading
		gl.glEnable(GL2.GL_NORMALIZE);		// Normalize normals before lighting
		gl.glEnable(GL2.GL_LIGHT0);		// Include light 0 in lighting
		gl.glEnable(GL2.GL_LIGHT1);		// Include light 1 in lighting
		gl.glEnable(GL2.GL_LIGHT2);		// Include light 2 in lighting
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		float[]	matAmbi = new float[] { v[0], v[0], v[0], 1.0f };
		float[]	matDiff = new float[] { v[1], v[1], v[1], 1.0f };
		float[]	matSpec = new float[] { v[2], v[2], v[2], 1.0f };
		float[]	matShin = new float[] { 100.0f * v[3] };

		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_AMBIENT, matAmbi, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_DIFFUSE, matDiff, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, matSpec, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SHININESS, matShin, 0);

		// Lights
		float[]	lightPosition = new float[] { v[4], v[5], v[6], 0.0f };
		float[]	lightIntensity = new float[] { v[7], v[7], v[7], 1.0f };

		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightIntensity, 0);

		float[]	lightPosition1 = new float[] { 3.5f, 0.5f, 0.5f, 0.0f };
		float[]	lightIntensity1 = new float[] { 0.5f, 0.4f, 0.2f, 1.0f };

		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPosition1, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightIntensity1, 0);

		float[]	lightPosition2 = new float[] { 0.5f, 3.5f, 0.5f, 0.0f };
		float[]	lightIntensity2 = new float[] { 0.4f, 0.3f, 0.1f, 1.0f };

		gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_POSITION, lightPosition2, 0);
		gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_DIFFUSE, lightIntensity2, 0);

		// Camera
		double	a = v[11];

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(-a * w / h, a * w / h, -a, a, 0.1, 100.0);

		// Action!
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		glu.gluLookAt(v[8], v[9], v[10], 0.0, 0.25, 0.0, 0.0, 1.0, 0.0);
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);

		float		u = getValue(mode);
		String		su = FORMAT.format(new Double(u));
		String		s = MODE_NAMES[mode] + " = " + su;

		renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		renderer.draw(s, 2, 2);

		renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		renderer.draw("Sides = " + sides, 2, h - 12);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		// Jack
		gl.glPushMatrix();
		gl.glTranslated(0.4, 0.4, 0.6);
		gl.glRotated(45.0, 0.0, 0.0, 1.0);
		gl.glScaled(0.08, 0.08, 0.08);
		drawJack(gl);
		gl.glPopMatrix();

		float	no_emission[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		float	lo_emission[] = { 0.3f, 0.05f, 0.05f, 0.0f };

		// Teapot
		gl.glPushMatrix();
		gl.glTranslated(0.6, 0.38, 0.5);
		gl.glRotated(30.0, 0.0, 1.0, 0.0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, lo_emission, 0);
		MYGLUT.glutSolidTeapot(0.08);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, no_emission, 0);
		gl.glPopMatrix();

		// Sphere
		gl.glPushMatrix();
		gl.glTranslated(0.25, 0.42, 0.35);
		MYGLUT.glutSolidSphere(0.1, sides, sides);
		gl.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
		MYGLUT.glutWireSphere(0.101, sides, sides);
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
}

//******************************************************************************
