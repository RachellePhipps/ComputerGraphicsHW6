//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Mon Mar 28 14:53:41 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20220328 [weaver]:	Added skew transforms.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.transform2;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import edu.ou.cs.cg.utilities.Utilities;

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
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						counter;	// Frame counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;

	private final Deque<Point2D.Double>			special;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);

		// Initialize container polygons
		special = createSpecialPolygon();

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public GLJPanel	getCanvas()
	{
		return canvas;
	}

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
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

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

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

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);	// Black background
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2			gl = drawable.getGL().getGL2();
		GLU			glu = GLU.createGLU();
		Point2D.Double	origin = model.getOrigin();

		float			xmin = (float)(origin.x - 1.0);
		float			xmax = (float)(origin.x + 1.0);
		float			ymin = (float)(origin.y - 1.0);
		float			ymax = (float)(origin.y + 1.0);

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(xmin, xmax, ymin, ymax);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		double[]	p = Utilities.mapViewToScene(gl, 0.5 * w, 0.5 * h, 0.0);
		double[]	q = Utilities.mapSceneToView(gl, 0.0, 0.0, 0.0);
		String		svc = ("View center in scene: [" + FORMAT.format(p[0]) +
						   " , " + FORMAT.format(p[1]) + "]");
		String		sso = ("Scene origin in view: [" + FORMAT.format(q[0]) +
						   " , " + FORMAT.format(q[1]) + "]");

		renderer.beginRendering(w, h);

		// Draw all text in yellow
		renderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);

		Point2D.Double	cursor = model.getCursor();

		if (cursor != null)
		{
			String		sx = FORMAT.format(new Double(cursor.x));
			String		sy = FORMAT.format(new Double(cursor.y));
			String		s = "Pointer at (" + sx + "," + sy + ")";

			renderer.draw(s, 2, 2);
		}
		else
		{
			renderer.draw("No Pointer", 2, 2);
		}

		String		sx = ("Transforms = " + model.getTransforms());
		String		st = ("[T|t] Push translate/untranslate");
		String		ss = ("[S|s] Push scale/unscale");
		String		sr = ("[R|r] Push rotate/unrotate");
		String		sk = ("[K|k] Push skew/unskew");
		String		sc = ("[clear/d/del]  Pop transform");
		String		sn = ("[numpad keys]  Pan coordinate system");

		renderer.draw(sx, 2, 16);
		renderer.draw(st, 2, 30);
		renderer.draw(ss, 2, 44);
		renderer.draw(sr, 2, 58);
		renderer.draw(sk, 2, 72);
		renderer.draw(sc, 2, 86);
		renderer.draw(sn, 2, 100);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		// Set ModelView matrix to identity (no transform) to draw the scene.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		drawBounds(gl);							// Unit bounding box
		drawAxes(gl);								// X and Y axes
		drawCursor(gl);							// Crosshairs at mouse point

		applyModelViewTransform(gl);				// Apply transformations
		drawPolygon(gl);							// Draw polygon, transformed
	}

	private void	drawBounds(GL2 gl)
	{
		gl.glColor3f(0.1f, 0.1f, 0.1f);
		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2d(1.0, 1.0);
		gl.glVertex2d(-1.0, 1.0);
		gl.glVertex2d(-1.0, -1.0);
		gl.glVertex2d(1.0, -1.0);

		gl.glEnd();
	}

	private void	drawAxes(GL2 gl)
	{
		gl.glBegin(GL.GL_LINES);

		gl.glColor3f(0.25f, 0.25f, 0.25f);
		gl.glVertex2d(-10.0, 0.0);
		gl.glVertex2d(10.0, 0.0);

		gl.glVertex2d(0.0, -10.0);
		gl.glVertex2d(0.0, 10.0);

		gl.glEnd();
	}

	private void	drawCursor(GL2 gl)
	{
		Point2D.Double	cursor = model.getCursor();

		if (cursor == null)
			return;

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(0.5f, 0.5f, 0.5f);

		for (int i=0; i<32; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cursor.x + 0.05 * Math.cos(theta),
						  cursor.y + 0.05 * Math.sin(theta));
		}

		gl.glEnd();
	}

	// Use pre-defined factors for translate, scale, rotate. All are invertible.
	private static final	double		dx = 0.050;		// x translate factor
	private static final	double		dy = 0.075;		// y translate factor
	private static final	double		sx = 0.900;		// x scale factor
	private static final	double		sy = 0.700;		// y scale factor
	private static final	double		beta = 15.0;	// rotate angle

	// Use pre-defined factors for skew/shear.
	private static final	double		kx = 0.5;		// x skew factor
	private static final	double		ky = 0.3;		// y skew factor

	// Warning: Skew/shear matrix is not invertible if kx * ky = 1.0.
	private static final	double		kinv = 1.0 / (1.0 - kx * ky);

	// Forward 2D matrix: [a b / c d] -> [1.0 kx / ky 1.0]
	private static final	double[]	kforward =
	{
	   1.0, kx, 0.0, 0.0,
	   ky, 1.0, 0.0, 0.0,
	   0.0, 0.0, 1.0, 0.0,
	   0.0, 0.0, 0.0, 1.0
	};

	// Inverse 2D matrix: [a b / c d]^-1 = (1 / (ad-bc))[d -b / -c a]
	private static final	double[]	kinverse =
	{
	   kinv, -kx * kinv, 0.0, 0.0,
	   -ky * kinv, kinv, 0.0, 0.0,
	   0.0, 0.0, 1.0, 0.0,
	   0.0, 0.0, 0.0, 1.0
	};

	// Apply the sequence of transformations specified by keypresses.
	private void	applyModelViewTransform(GL2 gl)
	{
		//System.out.println("Applying transform sequence: ");

		for (String s : model.getTransforms())
		{
			//System.out.print(s);

			switch (s)
			{
				case "t":	gl.glTranslated(dx, dy, 0.0);			break;
				case "s":	gl.glScaled(sx, sy, 1.0);				break;
				case "r":	gl.glRotated(beta, 0.0, 0.0, 1.0);		break;
				case "k":	gl.glMultMatrixd(kforward, 0);			break;

				case "T":	gl.glTranslated(-dx, -dy, 0.0);			break;
				case "S":	gl.glScaled(1.0 / sx, 1.0 / sy, 1.0);	break;
				case "R":	gl.glRotated(-beta, 0.0, 0.0, 1.0);		break;
				case "K":	gl.glMultMatrixd(kinverse, 0);			break;
			}
		}

		//System.out.println();
	}

	private void	drawPolygon(GL2 gl)
	{
		Deque<Point2D.Double>	polygon = special;

		// Don't fill...polygon isn't convex!
		//gl.glColor3f(0.3f, 0.3f, 0.3f);			// Dark gray
		//fillPolygon(gl, polygon);

		gl.glColor3f(1.0f, 1.0f, 1.0f);			// White
		edgePolygon(gl, polygon);
	}

	//**********************************************************************
	// Private Methods (Polygons)
	//**********************************************************************

	// Custom polygon for the snowflake branch. Not convex!
	// Built counterclockwise from bottom left vertex.
	// Bounding box is x from 0 to 1, y from -1 to 1.
	private Deque<Point2D.Double>	createSpecialPolygon()
	{
		Deque<Point2D.Double>	polygon = new ArrayDeque<Point2D.Double>(23);

		polygon.add(new Point2D.Double( 0.000, -0.176));
		polygon.add(new Point2D.Double( 0.326, -0.176));
		polygon.add(new Point2D.Double( 0.448, -1.000));
		polygon.add(new Point2D.Double( 0.536, -1.000));
		polygon.add(new Point2D.Double( 0.409, -0.176));
		polygon.add(new Point2D.Double( 0.591, -0.156));
		polygon.add(new Point2D.Double( 0.646, -0.588));
		polygon.add(new Point2D.Double( 0.746, -0.588));
		polygon.add(new Point2D.Double( 0.663, -0.118));
		polygon.add(new Point2D.Double( 0.812, -0.157));
		polygon.add(new Point2D.Double( 0.917, -0.294));

		polygon.add(new Point2D.Double( 1.000,  0.000));

		polygon.add(new Point2D.Double( 0.917,  0.294));
		polygon.add(new Point2D.Double( 0.812,  0.157));
		polygon.add(new Point2D.Double( 0.663,  0.118));
		polygon.add(new Point2D.Double( 0.746,  0.588));
		polygon.add(new Point2D.Double( 0.646,  0.588));
		polygon.add(new Point2D.Double( 0.591,  0.156));
		polygon.add(new Point2D.Double( 0.409,  0.176));
		polygon.add(new Point2D.Double( 0.536,  1.000));
		polygon.add(new Point2D.Double( 0.448,  1.000));
		polygon.add(new Point2D.Double( 0.326,  0.176));
		polygon.add(new Point2D.Double( 0.000,  0.176));

		return polygon;
	}

	// Draws the sides of the specified polygon.
	private void	edgePolygon(GL2 gl, Deque<Point2D.Double> polygon)
	{
		gl.glBegin(GL.GL_LINE_LOOP);

		for (Point2D.Double p : polygon)
			gl.glVertex2d(p.x, p.y);

		gl.glEnd();
	}

	// Draws the interior of the specified polygon.
	private void	fillPolygon(GL2 gl, Deque<Point2D.Double> polygon)
	{
		gl.glBegin(GL2.GL_POLYGON);

		for (Point2D.Double p : polygon)
			gl.glVertex2d(p.x, p.y);

		gl.glEnd();
	}
}

//******************************************************************************
