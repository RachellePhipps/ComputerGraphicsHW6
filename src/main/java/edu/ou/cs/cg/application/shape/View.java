//******************************************************************************
// Copyright (C) 2016-2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Wed Apr 24 10:31:13 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160425 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.shape;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.math.Quaternion;
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
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.00");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	// The parameters that the user can cycle through and adjust
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
		"Rotate.X",//"Eye.X",
		"Rotate.Y",//"Eye.Y",
		"Rotate.Z",//"Eye.Z",
		"Scale",
		"Light.Ambient.R",
		"Light.Ambient.G",
		"Light.Ambient.B",
	};

	// The shape types available for rendering
	private static final String[]		SHAPE_NAMES = new String[]
	{
		"Sphere",
		"Tetrahedron",
		"Cube",
		"Octahedron",
		"Dodecahedron",
		"Icosahedron",
		"Cylinder",
		"Cone",
		"Torus",
		"Teapot",
		"Jack",
		"Tie-Fighter",
	};

	// Float buffers used to set emission lighting in several places
	private static final float	NO_EMISSION[] = { 0.0f, 0.0f, 0.0f, 1.0f };
	private static final float	LO_EMISSION[] = { 0.8f, 0.8f, 0.2f, 0.0f };
	private static final float	AX_EMISSION[] = { 0.0f, 0.0f, 0.5f, 0.0f };

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

	private boolean[]				light;		// Draw lights?
	private boolean				edge;		// Draw edges?
	private boolean				fill;		// Draw fills?
	private boolean				axes;		// Draw axes?
	private int					shape;		// Polyhedron type
	private int					slices;	// Polyhedron slices
	private int					stacks;	// Polyhedron stacks
	private int					mode;		// Current parameter
	private float[]				v;			// Parameter values

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize model (give user-adjustables reasonable default values)
		light = new boolean[] { true, false, false, false };
		edge = true;
		fill = true;
		axes = true;
		shape = 0;
		slices = 8;
		stacks = 8;
		mode = 0;
		v = new float[] { 0.7f, 0.6f, 1.0f, 0.5f,		// Materials
						  2.0f, 6.0f, 3.0f, 0.7f,		// Lighting
						  1.5f, 1.5f, 1.5f, 1.5f,		// Eye position & scale
						  0.2f, 0.2f, 0.2f, };			// Ambient RGB

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

	public void	toggleLight(int i)
	{
		light[i] = !light[i];
		canvas.repaint();
	}

	public void	toggleEdge()
	{
		edge = !edge;
		canvas.repaint();
	}

	public void	toggleFill()
	{
		fill = !fill;
		canvas.repaint();
	}

	public void	toggleAxes()
	{
		axes = !axes;
		canvas.repaint();
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

	public void	decrementSlices()
	{
		if (slices > 1)
		{
			slices--;
			canvas.repaint();
		}
	}

	public void	incrementSlices()
	{
		if (slices < 64)
		{
			slices++;
			canvas.repaint();
		}
	}

	public void	decrementStacks()
	{
		if (stacks > 1)
		{
			stacks--;
			canvas.repaint();
		}
	}

	public void	incrementStacks()
	{
		if (stacks < 64)
		{
			stacks++;
			canvas.repaint();
		}
	}

	public void	decrementShape()
	{
		shape = ((shape + SHAPE_NAMES.length - 1) % SHAPE_NAMES.length);
		canvas.repaint();
	}

	public void	incrementShape()
	{
		shape = ((shape + 1) % SHAPE_NAMES.length);
		canvas.repaint();
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

	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glLightModel.xml
	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glShadeModel.xml
	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.8f, 0.8f, 0.8f, 0.0f);	// Light gray background

		// See com.jogamp.opengl.GL
		gl.glEnable(GL.GL_DEPTH_TEST);		// Turn on depth buffer updates

		// See com.jogamp.opengl.fixedfunc.GLLightingFunc
		gl.glEnable(GL2.GL_LIGHTING);		// Turn on lighting
		gl.glShadeModel(GL2.GL_SMOOTH);	// Use smooth (Gouraud) shading
		gl.glEnable(GL2.GL_NORMALIZE);		// Normalize normals before lighting
	}

	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMaterial.xml
	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMatrixMode.xml
	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		// Materials
		float[]	matAmbi = new float[] { v[0], v[0], v[0], 1.0f };
		float[]	matDiff = new float[] { v[1], v[1], v[1], 1.0f };
		float[]	matSpec = new float[] { v[2], v[2], v[2], 1.0f };
		float[]	matShin = new float[] { 100.0f * v[3] };

		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_AMBIENT, matAmbi, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_DIFFUSE, matDiff, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, matSpec, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SHININESS, matShin, 0);

		// Lights

		// Global ambient lighting
		float[]	ambientRGBA = new float[] { v[12], v[13], v[14], 1.0f };

		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, ambientRGBA, 0);

		// Start by reverting all lights to off. The following if blocks turn
		// them on back individually based on the current light toggle settings.
		gl.glDisable(GL2.GL_LIGHT0);
		gl.glDisable(GL2.GL_LIGHT1);
		gl.glDisable(GL2.GL_LIGHT2);
		gl.glDisable(GL2.GL_LIGHT3);

		if (light[0])		// Omnidirectional background lighting
		{
			// position, intensity
			float[]	lp = new float[] { v[4], v[5], v[6], 0.0f };
			float[]	li = new float[] { v[7], v[7], v[7], 1.0f };

			enableLight(gl, GL2.GL_LIGHT0, lp, li, null, null, null);
		}

		if (light[1])		// Blue spotlight from x-axis cone
		{
			// position, intensity, direction, cutoff, exponent
			float[]	lp = new float[] { 5.0f, 0.0f, 0.0f, 1.0f };
			float[]	li = new float[] { 0.0f, 0.0f, 1.0f, 0.5f };
			float[]	ld = new float[] { -1.0f, 0.0f, 0.0f, 1.0f };
			float[]	lc = new float[] { 15.0f };
			float[]	le = new float[] { 2.0f };

			enableLight(gl, GL2.GL_LIGHT1, lp, li, ld, lc, le);
		}

		if (light[2])		// Green spotlight from y-axis cone
		{
			// position, intensity, direction, cutoff, exponent
			float[]	lp = new float[] { 0.0f, 5.0f, 0.0f, 1.0f };
			float[]	li = new float[] { 0.0f, 1.0f, 0.0f, 0.5f };
			float[]	ld = new float[] { 0.0f, -1.0f, 0.0f, 1.0f };
			float[]	lc = new float[] { 15.0f };
			float[]	le = new float[] { 2.0f };

			enableLight(gl, GL2.GL_LIGHT2, lp, li, ld, lc, le);
		}

		if (light[3])		// Red spotlight from z-axis cone
		{
			// position, intensity, direction, cutoff, exponent
			float[]	lp = new float[] { 0.0f, 0.0f, 5.0f, 1.0f };
			float[]	li = new float[] { 1.0f, 0.0f, 0.0f, 0.5f };
			float[]	ld = new float[] { 0.0f, 0.0f, -1.0f, 1.0f };
			float[]	lc = new float[] { 15.0f };
			float[]	le = new float[] { 2.0f };

			enableLight(gl, GL2.GL_LIGHT3, lp, li, ld, lc, le);
		}

		// Camera
		double	a = v[11];

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(-a * w / h, a * w / h, -a, a, 0.1, 100.0);

		// Action!
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		glu.gluLookAt(1.5, 1.5, 1.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
		//glu.gluLookAt(v[8], v[9], v[10], 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
	}

	private void	enableLight(GL2 gl, int light, float[] lp, float[] li,
								float[] ld, float[] lc, float[] le)
	{
		gl.glEnable(light);

		if (lp != null)
			gl.glLightfv(light, GL2.GL_POSITION, lp, 0);

		if (li != null)
			gl.glLightfv(light, GL2.GL_DIFFUSE, li, 0);

		if (ld != null)
			gl.glLightfv(light, GL2.GL_SPOT_DIRECTION, ld, 0);

		if (lc != null)
			gl.glLightfv(light, GL2.GL_SPOT_CUTOFF, lc, 0);

		if (le != null)
			gl.glLightfv(light, GL2.GL_SPOT_EXPONENT, le, 0);
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	// Draws the various setting and keyboard manipulation options along the
	// lefthand side of the canvas
	private void	drawMode(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);

		// Draw all text in black
		renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);

		// Draw the various miscellaneous keyboard options from the bottom of
		// the window up. Each line get 12 pixels of space.
		renderer.draw("left  - prev mode", 2, 170);
		renderer.draw("right - next mode", 2, 158);
		renderer.draw("down  - decr value (shift: 10x)", 2, 146);
		renderer.draw("up    - incr value (shift: 10x)", 2, 134);

		renderer.draw("q - prev shape", 2, 110);
		renderer.draw("w - next shape", 2, 98);
		renderer.draw("a - decr slices (min 1)", 2, 86);
		renderer.draw("s - incr slices (max 64)", 2, 74);
		renderer.draw("z - decr stacks (min 1)", 2, 62);
		renderer.draw("x - incr stacks (max 64)", 2, 50);

		renderer.draw("e - show edges: " +
					  (edge ? "ON" : "OFF"), 2, 26);
		renderer.draw("f - show fills: " +
					  (fill ? "ON" : "OFF"), 2, 14);
		renderer.draw("c - show axes:  " +
					  (axes ? "ON" : "OFF"), 2, 2);

		// Draw the primary keyboard options from the top of the window down.
		// Each line get 12 pixels of space.
		float		v = getValue(mode);
		String		sw = FORMAT.format(new Double(v));

		renderer.draw("Mode   = " + MODE_NAMES[mode], 2, h - 12);
		renderer.draw("Value  = " + sw, 2, h - 24);
		renderer.draw("Shape  = " + SHAPE_NAMES[shape], 2, h - 36);
		renderer.draw("Slices = " + slices, 2, h - 48);
		renderer.draw("Stacks = " + stacks, 2, h - 60);

		// Continuing downward after an empty line, draw the light toggle
		// options, each on its own line.
		renderer.draw("0 - Omnidirectional light: " +
					  (light[0] ? "ON" : "OFF"), 2, h - 84);
		renderer.draw("1 - X spotlight (blue):    " +
					  (light[1] ? "ON" : "OFF"), 2, h - 96);
		renderer.draw("2 - Y spotlight (green):   " +
					  (light[2] ? "ON" : "OFF"), 2, h - 108);
		renderer.draw("3 - Z spotlight (red):     " +
					  (light[3] ? "ON" : "OFF"), 2, h - 120);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawCore(gl);								// Draw the core shape
		drawAxes(gl);								// Draw the X, Y, Z axes
	}

	// Draws the various setting and keyboard manipulation options along the
	// lefthand side of the canvas
	private void	drawCore(GL2 gl)
	{
		gl.glPushMatrix();

		gl.glRotated(360.0 * v[8], 1.0, 0.0, 0.0);
		gl.glRotated(360.0 * v[9], 0.0, 1.0, 0.0);
		gl.glRotated(360.0 * v[10], 0.0, 0.0, 1.0);

		// For the currently chosen shape, fill it using the baseline material
		// characteristics, then edge it using pale yellow emitted light. Draw
		// the shape using current number of slices and stacks, if applicable.

		if (shape == 0)			// Sphere
		{
			if (fill)
				drawSphere(gl, true, 1.0);

			if (edge)
				drawSphere(gl, false, 1.0);
		}
		else if (shape == 1)	// Tetrahedron
		{
			if (fill)
			{
				MYGLUT.glutSolidTetrahedron();
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireTetrahedron();
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 2)	// Cube
		{
			gl.glScaled(1.4, 1.4, 1.4);

			if (fill)
			{
				MYGLUT.glutSolidCube(1.0f);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireCube(1.0f);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 3)	// Octahedron
		{
			if (fill)
			{
				MYGLUT.glutSolidOctahedron();
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireOctahedron();
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 4)	// Dodecahedron
		{
			gl.glScaled(0.6, 0.6, 0.6);

			if (fill)
			{
				MYGLUT.glutSolidDodecahedron();
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireDodecahedron();
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 5)	// Icosahedron
		{
			if (fill)
			{
				MYGLUT.glutSolidIcosahedron();
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireIcosahedron();
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 6)	// Cylinder
		{
			GLU		glu = GLU.createGLU();
			GLUquadric	qobj = glu.gluNewQuadric();

			if (fill)
			{
				glu.gluQuadricDrawStyle(qobj, GLU.GLU_FILL);
				glu.gluCylinder(qobj, 1.0, 1.0, 1.0, slices, stacks);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				glu.gluQuadricDrawStyle(qobj, GLU.GLU_LINE);
				glu.gluCylinder(qobj, 1.0, 1.0, 1.0, slices, stacks);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 7)	// Cone
		{
			if (fill)
			{
				MYGLUT.glutSolidCone(1.0, 1.0, slices, stacks);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireCone(1.0, 1.0, slices, stacks);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 8)	// Torus
		{
			if (fill)
			{
				MYGLUT.glutSolidTorus(0.3, 0.5, stacks, slices);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireTorus(0.3, 0.5, stacks, slices);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 9)	// Teapot
		{
			if (fill)
			{
				MYGLUT.glutSolidTeapot(1.0);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireTeapot(1.0);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}
		}
		else if (shape == 10)	// Jack
		{
			// Make it more interesting with the spotlights by rotating the
			// whole thing by 30 degrees around X=Y=Z.
			gl.glRotated(30.0, 1.0, 1.0, 1.0);

			if (fill)
				drawJack(gl, true);

			if (edge)
				drawJack(gl, false);
		}
		else if (shape == 11)	// Tie-Fighter
		{
			// Cockpit
			if (fill)
				drawSphere(gl, true, 0.25);

			if (edge)
				drawSphere(gl, false, 0.25);

			// Left (+Z) wing
			gl.glTranslated(0.0, 0.0, 0.5);

			if (fill)
			{
				MYGLUT.glutSolidTorus(0.3, 0.5, stacks, slices);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireTorus(0.3, 0.5, stacks, slices);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}

			// Right (-Z) wing
			gl.glTranslated(0.0, 0.0, -1.0);

			if (fill)
			{
				MYGLUT.glutSolidTorus(0.3, 0.5, stacks, slices);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				MYGLUT.glutWireTorus(0.3, 0.5, stacks, slices);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}

			// Axle
			GLU		glu = GLU.createGLU();
			GLUquadric	qobj = glu.gluNewQuadric();

			if (fill)
			{
				glu.gluQuadricDrawStyle(qobj, GLU.GLU_FILL);
				glu.gluCylinder(qobj, 0.05, 0.05, 1.0, slices, stacks);
			}

			if (edge)
			{
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
				glu.gluQuadricDrawStyle(qobj, GLU.GLU_LINE);
				glu.gluCylinder(qobj, 0.05, 0.05, 1.0, slices, stacks);
				gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
			}

		}

		gl.glPopMatrix();
	}

	// Draws the three axes (with a line, arrow, and label for each)
	private void	drawAxes(GL2 gl)
	{
		if (axes)
		{
			gl.glPushMatrix();
			drawAxis(gl, 1.5, "Z");
			gl.glRotated(90.0, 0.0, 1.0, 0);
			drawAxis(gl, 1.5, "X");
			gl.glRotated(-90.0, 1.0, 0.0, 0);
			drawAxis(gl, 1.5, "Y");
			gl.glPopMatrix();
		}
	}

	// Draws an axis (with a line, arrow, and label)
	private void	drawAxis(GL2 gl, double length, String label)
	{
		gl.glPushMatrix();

		// Draw the label in a plane perpendicular to a point along the axis
		// slightly farther out than the apex of the arrow
		renderer.begin3DRendering();
		renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		renderer.draw3D(label, -0.05f, -0.05f, 1.55f, 0.01f);
		renderer.end3DRendering();

		// Use emissive material to highlight the lines and arrows
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, AX_EMISSION, 0);

		// Draw the axis line itself, in both negative and positive directions
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3d(0.0, 0.0, -length);
		gl.glVertex3d(0.0, 0.0, length);
		gl.glEnd();

		// Draw the arrow as a small cone with apex at the end of the axis line
		gl.glTranslated(0.0, 0.0, length - 0.2);
		MYGLUT.glutSolidCone(0.04, 0.2, 12, 9);

		// Revert to non-emissive material for future drawing
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);

		gl.glPopMatrix();
	}

	private void	drawJack(GL2 gl, boolean fill)
	{
		gl.glPushMatrix();

		drawJackPart(gl, fill);
		gl.glRotated(90.0, 0.0, 1.0, 0.0);
		drawJackPart(gl, fill);
		gl.glRotated(90.0, 1.0, 0.0, 0.0);
		drawJackPart(gl, fill);

		gl.glPopMatrix();
	}

	private void	drawJackPart(GL2 gl, boolean fill)
	{
		// Draw the core part (a stretched sphere)
		gl.glPushMatrix();
		gl.glScaled(0.16, 0.16, 0.88);
		drawSphere(gl, fill, 1.0);
		gl.glPopMatrix();

		// Draw the ball on one end
		gl.glPushMatrix();
		gl.glTranslated(0.0, 0.0, 1.0);
		drawSphere(gl, fill, 0.16);
		gl.glPopMatrix();

		// Draw the ball on the other end
		gl.glPushMatrix();
		gl.glTranslated(0.0, 0.0, -1.0);
		drawSphere(gl, fill, 0.16);
		gl.glPopMatrix();
	}

	private void	drawSphere(GL2 gl, boolean fill, double radius)
	{
		if (fill)
		{
			MYGLUT.glutSolidSphere(radius, slices, stacks);
		}
		else
		{
			gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, LO_EMISSION, 0);
			MYGLUT.glutWireSphere(radius, slices, stacks);
			gl.glMaterialfv(GL.GL_FRONT, GL2.GL_EMISSION, NO_EMISSION, 0);
		}
	}
}

//******************************************************************************
