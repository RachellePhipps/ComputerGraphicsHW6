//******************************************************************************
// Copyright (C) 2016-2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Feb 26 15:46:01 2019 by Chris Weaver
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

package edu.ou.cs.cg.application.street;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.*;
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

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;

	private final FPSAnimator			animator;
	private int						counter;	// Frame counter

	private TextRenderer				renderer;

	private float						thickline;		// Line thickness
	private boolean					fillpolys;		// Fill polygons?

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize model (give user-adjustables reasonable default values)
		thickline = 1.0f;
		fillpolys = false;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();

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
		int	k = counter;

		counter++;								// Advance animation counter

		if (k < 180)							// Don't change for 3s at start
			return;

		thickline = 0.5f * ((k / 15) % 12);	// +0.5 per 0.25s max 6, reset
		fillpolys = ((k / 180) % 2 == 0);		// Toggle filling every 3s
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);	// White background
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	// www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glBlendFunc.xml
	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		// See com.jogamp.opengl.GL
		//gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	// Position and orient the default camera to view in 2-D, in pixel coords.
	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(0.0f, 1280.0f, 0.0f, 720.0f);// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		double[]	p =
			Utilities.mapViewToScene(gl, (double)w/2, (double)h/2, 0.0);
		double[]	q = Utilities.mapSceneToView(gl, 0.0, 0.0, 0.0);
		String		svc = ("View center:  [" + FORMAT.format(p[0]) +
						   " , " + FORMAT.format(p[1]) + "]");
		String		sso = ("Scene origin: [" + FORMAT.format(q[0]) +
						   " , " + FORMAT.format(q[1]) + "]");

		renderer.beginRendering(w, h);

		// Draw all text in blue
		renderer.setColor(0.0f, 0.0f, 1.0f, 1.0f);

		renderer.draw(svc, 2, 16);
		renderer.draw(sso, 2, 30);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawHouse1(gl);
		drawHouse2(gl);
		drawHouse3(gl);

		drawFences(gl);
		drawKite(gl);
	}

	// Draw the parts of a house.
	private void	drawHouse1(GL2 gl)
	{
		int	dx = 108;
		int	dy = 132;

		drawChimney(gl,	dx + 114,	dy);
		drawFrame(gl,		dx,		dy);
		drawWindow(gl,		dx + 127,	dy + 127);
		drawDoor(gl,		dx + 39,	dy);
	}

	// Draw the parts of a slightly different house.
	private void	drawHouse2(GL2 gl)
	{
		int	dx = 634;
		int	dy = 158;

		drawChimney(gl,	dx + 30,	dy);
		drawFrame(gl,		dx,		dy);
		drawWindow(gl,		dx + 98,	dy + 64);
		drawWindow(gl,		dx + 144,	dy + 64);
		drawDoor(gl,		dx + 7,	dy);
	}

	// Draw the parts of another slightly different house.
	private void	drawHouse3(GL2 gl)
	{
		int	dx = 1048;
		int	dy = 132;

		drawChimney(gl,	dx + 30,	dy);
		drawFrame(gl,		dx,		dy);
		drawWindow(gl,		dx + 98,	dy + 64);
		drawWindow(gl,		dx + 144,	dy + 64);
		drawDoor(gl,		dx + 7,	dy);
	}

	// Draw entire chimney, not just the visible part. Effect when fill is off?
	private void	drawChimney(GL2 gl, int dx, int dy)
	{
		setColor(gl, 128, 0, 0);				// Firebrick red
		fillRect(gl, dx, dy, 30, 250);

		setColor(gl, 0, 0, 0);					// Black
		edgeRect(gl, dx, dy, 30, 250);
	}

	// Define five corners of a house frame that is shorter on the left side.
	private static final Point[]	OUTLINE_FRAME = new Point[]
	{
		new Point(  0,   0),		// base, left corner
		new Point(176,   0),		// base, right corner
		new Point(176, 162),		// roof, right corner
		new Point( 88, 250),		// roof, apex
		new Point(  0, 162),		// roof, left corner
	};

	// Draw a house frame, given its lower left corner.
	private void	drawFrame(GL2 gl, int dx, int dy)
	{
		setColor(gl, 128, 64, 0);				// Medium brown
		fillPoly(gl, dx, dy, OUTLINE_FRAME);

		setColor(gl, 0, 0, 0);					// Black
		edgePoly(gl, dx, dy, OUTLINE_FRAME);
	}

	// Draw a door, given its lower left corner.
	private void	drawDoor(GL2 gl, int dx, int dy)
	{
		setColor(gl, 192, 128, 0);				// Light brown
		fillRect(gl, dx, dy, 40, 92);

		setColor(gl, 0, 0, 0);					// Black
		edgeRect(gl, dx, dy, 40, 92);
	}

	// Draw a window, given its center.
	private void	drawWindow(GL2 gl, int dx, int dy)
	{
		int	ww = 20;
		int	hh = 20;

		setColor(gl, 255, 255, 128);			// Light yellow
		fillRect(gl, dx - ww, dy - hh, 2 * ww, 2 * hh);

		setColor(gl, 0, 0, 0);					// Black
		edgeRect(gl, dx - ww, dy - hh, 2 * ww, 2 * hh);
	}

	//**********************************************************************
	// Private Methods (Scene, Fence)
	//**********************************************************************

	private void	drawFences(GL2 gl)
	{
		// Draw a zigzag fence with 8 boards
		fillFenceStrip(gl, 856, 132, 8);
		edgeFenceStrip(gl, 856, 132, 8);

		// Draw a zigzag fence with 2 boards
		fillFenceStrip(gl, 1224, 132, 2);
		edgeFenceStrip(gl, 1224, 132, 2);

		// Draw a rightward-increasing jagged fence with 4 boards
		fillFenceBoard(gl, false,  290, 132);
		edgeFenceBoard(gl, false,  290, 132);
		fillFenceBoard(gl, false,  314, 132);
		edgeFenceBoard(gl, false,  314, 132);
		fillFenceBoard(gl, false,  338, 132);
		edgeFenceBoard(gl, false,  338, 132);
		fillFenceBoard(gl, false,  362, 132);
		edgeFenceBoard(gl, false,  362, 132);
	}

	// Fills a left-to-right sequence of fence boards using a QUAD_STRIP.
	private void	fillFenceStrip(GL2 gl, int dx, int dy, int boards)
	{
		if (!fillpolys)
			return;

		setColor(gl, 192, 192, 128);			// Tan

		gl.glBegin(GL2.GL_QUAD_STRIP);

		gl.glVertex2i(dx + 0, dy + 0);		// base, leftmost slat
		gl.glVertex2i(dx + 0, dy + 102);	// peak, leftmost slat

		for (int i=1; i<=boards; i++)
		{
			int	x = i * 24;
			int	y = ((i % 2 == 1) ? 112 : 102);

			gl.glVertex2i(dx + x, dy + 0);	// base, next slat
			gl.glVertex2i(dx + x, dy + y);	// peak, next slat
		}

		gl.glEnd();
	}

	// Edges a left-to-right sequence of fence boards using LINE_LOOPs.
	private void	edgeFenceStrip(GL2 gl, int dx, int dy, int boards)
	{
		setColor(gl, 0, 0, 0);					// Black

		gl.glLineWidth(thickline);

		for (int i=0; i<boards; i++)
		{
			int	xl = i * 24;
			int	xr = xl + 24;
			int	yl = ((i % 2 == 0) ? 102 : 112);
			int	yr = ((i % 2 == 0) ? 112 : 102);

			gl.glBegin(GL2.GL_LINE_LOOP);

			gl.glVertex2i(dx + xl, dy + 0);	// base, left
			gl.glVertex2i(dx + xr, dy + 0);	// base, right
			gl.glVertex2i(dx + xr, dy + yr);	// peak, right
			gl.glVertex2i(dx + xl, dy + yl);	// peak, left

			gl.glEnd();
		}

		gl.glLineWidth(1.0f);
	}

	// Define four corners of a fence board that is shorter on the left side.
	private static final Point[]	OUTLINE_BOARD_L = new Point[]
	{
		new Point(  0,   0),		// base, left
		new Point( 24,   0),		// base, right
		new Point( 24, 112),		// peak, right
		new Point(  0, 102),		// peak, left
	};

	// Definf four corners of a fence board that is shorter on the right side.
	private static final Point[]	OUTLINE_BOARD_R = new Point[]
	{
		new Point(  0,   0),		// base, left
		new Point( 24,   0),		// base, right
		new Point( 24, 102),		// peak, right
		new Point(  0, 112),		// peak, left
	};

	// Fills a single fence slat with bottom left corner at dx, dy.
	// If flip is true, the slat is higher on the left, else on the right.
	private void	fillFenceBoard(GL2 gl, boolean flip, int dx, int dy)
	{
		if (!fillpolys)
			return;

		setColor(gl, 192, 192, 128);			// Tan
		fillPoly(gl, dx, dy, (flip ? OUTLINE_BOARD_R : OUTLINE_BOARD_L));
	}

	// Edges a single fence slat with bottom left corner at dx, dy.
	// If flip is true, the slat is higher on the left, else on the right.
	private void	edgeFenceBoard(GL2 gl, boolean flip, int dx, int dy)
	{
		setColor(gl, 0, 0, 0);					// Black
		edgePoly(gl, dx, dy, (flip ? OUTLINE_BOARD_R : OUTLINE_BOARD_L));
	}

	//**********************************************************************
	// Private Methods (Scene, Kite)
	//**********************************************************************

	private static final int		SIDES_KITE = 18;
	private static final double	ANGLE_KITE = 2.0 * Math.PI / SIDES_KITE;

	// Draws a kite consisting of two fans, one upper blue, one lower red.
	private void	drawKite(GL2 gl)
	{
		int		cx = 956;
		int		cy = 490;
		int		r = 80;

		double		amin =  4.0 * ANGLE_KITE;
		double		amax =  9.0 * ANGLE_KITE;
		double		bmin = 13.0 * ANGLE_KITE;
		double		bmax = 18.0 * ANGLE_KITE;

		int		fans = 5;
		double		astep = (amax - amin) / fans;
		double		bstep = (bmax - bmin) / fans;

		// Fill and edge the lower red fan
		fillKiteFan(gl, cx, cy, fans, r, bmin, bstep);
		edgeKiteFan(gl, cx, cy, fans, r, bmin, bstep);

		for (int i=0; i<fans; i++)
		{
			double	a = amin + astep * i;

			// Fill and edge each upper blue fan blade
			fillKiteBlade(gl, cx, cy, r, a, a + astep);
			edgeKiteBlade(gl, cx, cy, r, a, a + astep);
		}
	}

	// Fills an entire kite fan using a TRIANGLE_FAN.
	private void	fillKiteFan(GL2 gl, int cx, int cy, int fans, int r,
								double min, double step)
	{
		if (!fillpolys)
			return;

		setColor(gl, 224, 80, 48);				// Bright red

		gl.glBegin(GL2.GL_TRIANGLE_FAN);

		gl.glVertex2d(cx, cy);

		for (int i=0; i<=fans; i++)
		{
			double	a = min + step * i;

			gl.glVertex2d(cx + r * Math.cos(a), cy + r * Math.sin(a));
		}

		gl.glEnd();
	}

	// Edges an entire kite fan using a LINE_LOOPs.
	private void	edgeKiteFan(GL2 gl, int cx, int cy, int fans, int r,
								double min, double step)
	{
		setColor(gl, 0, 0, 0);					// Black

		gl.glLineWidth(thickline);

		double	a = min;

		for (int i=0; i<fans; i++)
		{
			gl.glBegin(GL.GL_LINE_LOOP);

			gl.glVertex2d(cx, cy);
			gl.glVertex2d(cx + r * Math.cos(a), cy + r * Math.sin(a));
			a += step;
			gl.glVertex2d(cx + r * Math.cos(a), cy + r * Math.sin(a));

			gl.glEnd();
		}

		gl.glLineWidth(1.0f);
	}

	// Fills a single kite fan blade using a POLYGON.
	private void	fillKiteBlade(GL2 gl, int cx, int cy, int r,
								  double a1, double a2)
	{
		if (!fillpolys)
			return;

		setColor(gl, 48, 80, 224);				// Bright blue

		gl.glBegin(GL2.GL_POLYGON);

		gl.glVertex2d(cx, cy);
		gl.glVertex2d(cx + r * Math.cos(a1), cy + r * Math.sin(a1));
		gl.glVertex2d(cx + r * Math.cos(a2), cy + r * Math.sin(a2));

		gl.glEnd();
	}

	// Edges a single kite fan blade using a LINE_LOOP.
	private void	edgeKiteBlade(GL2 gl, int cx, int cy, int r,
								  double a1, double a2)
	{
		setColor(gl, 0, 0, 0);					// Black

		gl.glLineWidth(thickline);

		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2d(cx, cy);
		gl.glVertex2d(cx + r * Math.cos(a1), cy + r * Math.sin(a1));
		gl.glVertex2d(cx + r * Math.cos(a2), cy + r * Math.sin(a2));

		gl.glEnd();

		gl.glLineWidth(1.0f);
	}

	//**********************************************************************
	// Private Methods (Utility Functions)
	//**********************************************************************

	// Sets color, normalizing r, g, b, a values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b, int a)
	{
		gl.glColor4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	// Sets fully opaque color, normalizing r, g, b values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b)
	{
		setColor(gl, r, g, b, 255);
	}

	// Fills a rectangle having lower left corner at (x,y) and dimensions (w,h).
	private void	fillRect(GL2 gl, int x, int y, int w, int h)
	{
		if (!fillpolys)
			return;

		gl.glBegin(GL2.GL_POLYGON);

		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);

		gl.glEnd();
	}

	// Edges a rectangle having lower left corner at (x,y) and dimensions (w,h).
	private void	edgeRect(GL2 gl, int x, int y, int w, int h)
	{
		gl.glLineWidth(thickline);

		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2i(x+0, y+0);
		gl.glVertex2i(x+0, y+h);
		gl.glVertex2i(x+w, y+h);
		gl.glVertex2i(x+w, y+0);

		gl.glEnd();

		gl.glLineWidth(1.0f);
	}

	// Fills a polygon defined by a starting point and a sequence of offsets.
	private void	fillPoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		if (!fillpolys)
			return;

		gl.glBegin(GL2.GL_POLYGON);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();
	}

	// Edges a polygon defined by a starting point and a sequence of offsets.
	private void	edgePoly(GL2 gl, int startx, int starty, Point[] offsets)
	{
		gl.glLineWidth(thickline);

		gl.glBegin(GL2.GL_LINE_LOOP);

		for (int i=0; i<offsets.length; i++)
			gl.glVertex2i(startx + offsets[i].x, starty + offsets[i].y);

		gl.glEnd();

		gl.glLineWidth(1.0f);
	}
}

//******************************************************************************
