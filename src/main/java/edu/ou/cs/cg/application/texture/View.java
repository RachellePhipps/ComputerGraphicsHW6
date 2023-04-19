//******************************************************************************
// Copyright (C) 2016-2019 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Apr 10 20:31:26 2020 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160425 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
//
//******************************************************************************
// Notes:
//
// Warning! This code uses depricated features of OpenGL, including immediate
// mode vertex attribute specification, for sake of easier classroom learning.
// See www.khronos.org/opengl/wiki/Legacy_OpenGL
//
//******************************************************************************

package edu.ou.cs.cg.application.texture;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.*;
import edu.ou.cs.cg.utilities.*;

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

	private static final String		RSRC = "images/";
	private static final String[]		FILENAMES =
	{
		"Memento.jpg",
		"Serenity.jpg",
		"Zoolander.jpg",
		//"ruins-panorama.jpg",
		"ruins-panorama-small.jpg",	// Reduced size version
		"ruins-foot.jpg",
		"ruins-head.jpg",
	};

	// Eyeballed radial position of the sun in the panorama image.
	private static final double		SUN_THETA = 0.15 * Math.PI;

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;				// Canvas width
	private int						h;				// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						counter;		// Animation counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;

	private Texture[]					textures;		// Loaded from FILENAMES
	private Cylinder					skycan;		// Cylinder+ground+sky

	// Temp variable; turns off to omit sky in down view
	private boolean					topdown = false;

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

	// Called immediately after the GLContext of the GLCanvas is initialized.
	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
		initTextures(drawable);
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

		if (model.getShowRearView())
			renderRearView(drawable);

		if (model.getShowDownView())
			renderDownView(drawable);

		GL2	gl = drawable.getGL().getGL2();

		gl.glFlush();							// Finish and display
	}

	// Called during the first repaint after a resize of the GLCanvas.
	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
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
		gl.glEnable(GL2.GL_LINE_SMOOTH);	// Turn on line anti-aliasing

		// See com.jogamp.opengl.fixedfunc.GLLightingFunc
		gl.glEnable(GL2.GL_LIGHTING);		// Turn on lighting
		gl.glEnable(GL2.GL_NORMALIZE);		// Normalize normals before lighting
		gl.glShadeModel(GL2.GL_SMOOTH);	// Use smooth (Gouraud) shading

		// Global ambient light (bright cool sky)
		float[]	ambient = new float[] { 0.9f, 1.0f, 1.0f, 1.0f };

		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, ambient, 0);
	}

	// Load image files as textures into instances of JOGL's Texture class.
	// www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glTexParameter.xml
	private void	initTextures(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		textures = new Texture[FILENAMES.length];

		for (int i=0; i<FILENAMES.length; i++)
		{
			try
			{
				URL	url = View.class.getResource(RSRC + FILENAMES[i]);

				if (url != null)
				{
					// Create the texture from the JPEG file at the URL.
					textures[i] = TextureIO.newTexture(url, false,
													   TextureIO.JPG);

					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER,
												 GL2.GL_LINEAR);
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER,
												 GL2.GL_LINEAR);
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S,
												 GL2.GL_CLAMP_TO_EDGE);
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T,
												 GL2.GL_CLAMP_TO_EDGE);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				System.exit(1);			// Bail if image file is missing
			}
		}
	}

	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMaterial.xml
	// www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glMatrixMode.xml
	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);	// Black background
		gl.glClear(GL.GL_COLOR_BUFFER_BIT |		// Clear the color buffer
				   GL.GL_DEPTH_BUFFER_BIT);		// Clear the depth buffer

		// Use to adjust the frustum (clipped volume) relative to viewport
		float	aspect = (float)w/(float)h;		// Aspect ratio of viewport

		// Set up a typical perspective projection
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, aspect, 0.1f, 10.0f);

		// Orient the scene relative to the camera.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glViewport(0, 0, w, h);					// Full window

		// Place camera a fixed distance from center, but at variable y
		glu.gluLookAt(0.0, model.getPosition(), 3.0,
					  0.0, 0.0, 0.0, 0.0, 1.0, 0.0);

		// Apply interactively set rotation amount
		gl.glRotated(model.getRotation(), 0.0, 1.0, 0.0);

		// Create skycan with the current number of slices and vertical scale
		int	slices = model.getSlices();
		float	scale = (float)model.getScale();

		skycan = new Cylinder(slices, -1.0f, scale);
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	// Update the scene model for the current animation frame.
	private void	update(GLAutoDrawable drawable)
	{
		counter++;								// Advance animation counter
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		lightSkycan(gl);				// .....
		fillSkycan(gl);

		if (model.getShowSun())
		{
			lightSun(gl);
			drawSun(gl);

			enableSunlight(gl);
		}

		lightCube(gl);
		fillCube(gl);

		if (model.getShowMovers())
			drawMovers(gl);

		if (model.getShowEdges())
		{
			lightEdges(gl);
			edgeCube(gl);
			edgeSkycan(gl);
		}

		disableSunlight(gl);

		drawMode(drawable);					// Draw mode text
	}

	// Draws the various setting and keyboard manipulation options along the
	// lefthand side of the canvas
	private void	drawMode(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);

		// Draw all text in white
		renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);

		// Draw the primary keyboard options from the top of the window down.
		// Each line get 12 pixels of space.
		String	sposition = FORMAT.format(new Double(model.getPosition()));
		String	srotation = FORMAT.format(new Double(model.getRotation()));
		String	sscale = FORMAT.format(new Double(model.getScale()));
		String	sradius = FORMAT.format(new Double(model.getRadius()));

		renderer.draw("Slices     = " + model.getSlices(), 2, h - 12);

		renderer.draw("Position   = " + sposition, 2, h - 36);
		renderer.draw("Rotation   = " + srotation, 2, h - 48);
		renderer.draw("Scale      = " + sscale, 2, h - 60);
		renderer.draw("Radius     = " + sradius, 2, h - 72);

		renderer.draw("Show Edges = " + model.getShowEdges(), 2, h - 96);

		renderer.endRendering();
	}

	//**********************************************************************
	// Private Methods (Lighting)
	//**********************************************************************

	public static final float[]	DEFAULT_AMBIENT =
		new float[] { 0.2f, 0.2f, 0.2f, 1.0f };

	public static final float[]	DEFAULT_DIFFUSE =
		new float[] { 0.8f, 0.8f, 0.8f, 1.0f };

	public static final float[]	DEFAULT_SPECULAR =
		new float[] { 0.0f, 0.0f, 0.0f, 1.0f };

	public static final float[]	DEFAULT_SHININESS =
		new float[] { 0.0f };

	public static final float[]	DEFAULT_EMISSION =
		new float[] { 0.0f, 0.0f, 0.0f, 1.0f };

	// Convenience method for setting all five light parameters.
	// Pass in null to turn off the corresponding parameter.
	public static void	setLight(GL2 gl, int light, float[] lp, float[] li,
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

	// Convenience method for setting all five material parameters.
	// Pass in null to set the parameter to the OpenGL default value.
	public static void	setMaterial(GL2 gl, float[] ambi, float[] diff,
									float[] spec, float[] shin, float[] emit)
	{
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT,
						((ambi == null) ? DEFAULT_AMBIENT : ambi), 0);

		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE,
						((diff == null) ? DEFAULT_DIFFUSE : diff), 0);

		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR,
						((spec == null) ? DEFAULT_SPECULAR : spec), 0);

		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS,
						((shin == null) ? DEFAULT_SHININESS : shin), 0);

		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_EMISSION,
						((emit == null) ? DEFAULT_EMISSION : emit), 0);
	}

	// Light the skycan using only ambient light.
	private void	lightSkycan(GL2 gl)
	{
		float[]	ambi = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

		setMaterial(gl, ambi, null, null, null, null);
	}

	// Light the skycan using only its own emitted pale yellow color.
	private void	lightSun(GL2 gl)
	{
		float[]	emit = new float[] { 1.0f, 1.0f, 0.5f, 1.0f };

		setMaterial(gl, null, null, null, null, emit);
	}

	// Light cubes (central and movers) from all light sources; no emission.
	private void	lightCube(GL2 gl)
	{
		float[]	ambi = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
		float[]	diff = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		float[]	spec = new float[] { 0.8f, 0.8f, 0.8f, 1.0f };
		float[]	shin = new float[] { 70.0f };

		setMaterial(gl, ambi, diff, spec, shin, null);
	}

	// Light edges of the central cube and skycan segments like metallic gold.
	// Constants taken from the very nice gallery of materials at:
	// http://www.it.hiof.no/~borres/j3d/explain/light/p-materials.html
	private void	lightEdges(GL2 gl)
	{
		float[]	ambi = { 0.24725f,  0.1995f,   0.0745f,   1.0f };
		float[]	diff = { 0.75164f,  0.60648f,  0.22648f,  1.0f };
		float[]	spec = { 0.628281f, 0.555802f, 0.366065f, 1.0f };
		float[]	shin = { 51.2f };

		setMaterial(gl, ambi, diff, spec, shin, null);
	}

	// Turn on a high intensity white light at the top edge of the skycan,
	// located roughly where the sun appears in the panorama texture image.
	private void	enableSunlight(GL2 gl)
	{
		gl.glEnable(GL2.GL_LIGHT0);

		float		scale = (float)model.getScale();
		float		radius = (float)model.getRadius();
		float		x = (float)Math.cos(SUN_THETA);
		float		z = (float)Math.sin(SUN_THETA);
		float[]	lp0 = new float[] { x * radius, scale, z * radius, 0.0f };
		float[]	li0 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

		setLight(gl, GL2.GL_LIGHT0, lp0, li0, null, null, null);
	}

	// Turn off the sun.
	private void	disableSunlight(GL2 gl)
	{
		gl.glDisable(GL2.GL_LIGHT0);
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	// Fill the faces of the cube.
	private void	fillCube(GL2 gl)
	{
		Cube.fillFace(gl, 0, textures[0]);
		Cube.fillFace(gl, 1, textures[0]);
		Cube.fillFace(gl, 2, textures[1]);
		Cube.fillFace(gl, 3, textures[1]);
		Cube.fillFace(gl, 4, textures[2]);
		Cube.fillFace(gl, 5, textures[2]);
	}

	// Draw each edge of the cube and put a small sphere at each vertex.
	private void	edgeCube(GL2 gl)
	{
		gl.glPushMatrix();
		gl.glScaled(1.01, 1.01, 1.01);						// Push out slightly
		Cube.edge(gl);
		gl.glPopMatrix();

		for (Quaternion v : Cube.VERTICES)
		{
			gl.glPushMatrix();
			gl.glTranslated(v.getX(), v.getY(), v.getZ());
			MYGLUT.glutSolidSphere(0.025, 12, 12);
			gl.glPopMatrix();
		}
	}

	// Fill the skycan (a cylindrical skycan with slices as number of sides).
	private void	fillSkycan(GL2 gl)
	{
		double	radius = model.getRadius();

		gl.glPushMatrix();

		gl.glScaled(radius, 1.0, radius);

		if (model.getShowHorizon())
			skycan.fill(gl, textures[3]);		// Around the cylinder

		if (model.getShowGround())
			skycan.fillFoot(gl, textures[4]);	// Bottom end of the cylinder

		if (model.getShowSky() && !topdown)
			skycan.fillHead(gl, textures[5]);	// Top end of the cylinder

		gl.glPopMatrix();

		// Fill a plane below the ground, only in the top-down viewport.
		if (topdown)
			drawFloor(gl);
	}

	// Edge the skycan (a cylindrical skycan with slices as number of sides).
	private void	edgeSkycan(GL2 gl)
	{
		double	radius = model.getRadius();

		gl.glPushMatrix();
		gl.glScaled(radius - 0.01, 0.99, radius - 0.01);	// Pull in slightly
		skycan.edge(gl);
		gl.glPopMatrix();
	}

	// Draw a sphere around the sun in the skycan image.
	private void	drawSun(GL2 gl)
	{
		double	scale = model.getScale();
		double	radius = model.getRadius();
		double	x = Math.cos(SUN_THETA);
		double	z = Math.sin(SUN_THETA);

		gl.glPushMatrix();
		gl.glTranslated(x * radius, scale, z * radius);
		MYGLUT.glutSolidSphere(0.20, 16, 16);
		gl.glPopMatrix();
	}

	//**********************************************************************
	// Private Methods (Scene Extras)
	//**********************************************************************

	// Draw objects in the scene, in additional to the central cube.
	private void	drawMovers(GL2 gl)
	{
		// Draw a second cube, revolving around the first one
		gl.glPushMatrix();
		gl.glRotated((counter / 1.5), 0.0, 1.0, 0.0);	// Revolve around center
		gl.glTranslated(1.2, -0.4, 1.2);				// Off center
		gl.glRotated((counter / 2.0), 0.2, 0.3, 0.5);	// Rotate about self
		gl.glScaled(0.3, 0.3, 0.3);					// Smaller
		fillCube(gl);
		gl.glPopMatrix();

		// Draw a third cube, revolving around the first one
		gl.glPushMatrix();
		gl.glRotated((counter / 5.0), 0.0, 1.0, 0.0);	// Revolve around center
		gl.glTranslated(2.3, 0.6, 1.7);				// Off center
		gl.glRotated((counter / 0.2), 0.6, 0.3, 0.4);	// Rotate about self
		gl.glScaled(0.1, 0.4, 0.7);					// Smaller, stretched
		fillCube(gl);
		gl.glPopMatrix();
	}

	// Fill a plane below the ground. Only used in the top-down viewport to
	// occlude (most of) the scene in the main viewport.
	private void	drawFloor(GL2 gl)
	{
		double	extent = 2.0 * model.getRadius();
		double	floor = -1.01;

		gl.glBegin(GL2.GL_QUADS);

		gl.glVertex3d( extent, floor,  extent);
		gl.glVertex3d(-extent, floor,  extent);
		gl.glVertex3d(-extent, floor, -extent);
		gl.glVertex3d( extent, floor, -extent);

		gl.glEnd();
	}

	//**********************************************************************
	// Private Methods (Viewport Extras)
	//**********************************************************************

	// Create a rearview mirror, in the form of a small viewport in the top
	// right corner of the window.
	private void	renderRearView(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		// Use to adjust the frustum (clipped volume) relative to viewport
		float	aspect = (float)w/(float)h;		// Aspect ratio of viewport

		// Set up a typical perspective projection
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, aspect, 0.1f, 10.0f);

		// Orient the scene relative to the camera.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glViewport(w - 8 - w/6, h - 8 - h/6, w/6, h/6);	// Top right corner

		// Place camera in front of main camera, looking back at it
		glu.gluLookAt(0.0, 0.75 * model.getPosition(), 0.75 * 3.0,
					  0.0, 1.50 * model.getPosition(), 1.50 * 3.0,
					  0.0, 1.0, 0.0);

		// Invert around the x axis (viewer's left-to-right) for mirror effect
		gl.glScaled(-1.0, 1.0, 1.0);

		// Apply interactively set rotation amount
		gl.glRotated(model.getRotation(), 0.0, 1.0, 0.0);

		// Draw the entire scene...from the mirror's perspective
		render(drawable);

		// Draw viewport boundary rectangle to visually frame it
		renderViewportBounds(drawable);
	}

	// Create a topdown view, in the form of a small square viewport in the
	// bottom right corner of the window.
	private void	renderDownView(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		// Use to adjust the frustum (clipped volume) relative to viewport
		float	aspect = (float)w/(float)h;		// Aspect ratio of viewport
		float	scale = (float)model.getScale();
		float	sixth = 1.0f / 6.0f;

		// Set up a typical perspective projection
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, 1.0f, 0.1f, 10.0f);

		// Orient the scene relative to the camera.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glViewport(w - 8 - w/6, 8, w/6, w/6);		// Bottom right corner
		// Note that the viewport is square^^^

		// Place camera directly above center of scene, looking down.
		// Uses -z as 'up' to put far side of scene in main view at the top.
		glu.gluLookAt(0.0, scale, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0);

		// Apply scale for viewport
		gl.glScaled(1.0f / 6.0f, 1.0f / 6.0f, 1.0f / 6.0f);

		// Apply interactively set rotation amount
		gl.glRotated(model.getRotation(), 0.0, 1.0, 0.0);

		// Draw the entire scene from the topdown perspective. Turn off the sky
		// to keep it from occluding the rest of the scene.
		topdown = true;
		render(drawable);
		topdown = false;

		// Draw viewport boundary rectangle to visually frame it
		renderViewportBounds(drawable);
	}

	// Draw a rectangle around current viewport boundary to visually frame it.
	private void	renderViewportBounds(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		GLU		glu = GLU.createGLU();

		// Boundary is in the screen's viewing plane rather than the scene.
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(0.0, 1.0, 0.0, 1.0);

		// Drawing a very simple thing, no need to transform the scene.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// Light the boundary using only its own emitted white color.
		// Global ambient light is still on, so it will appear dim.
		float[]	emit = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

		setMaterial(gl, null, null, null, null, emit);

		// Draw the boundary.
		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2d(1.0, 1.0);
		gl.glVertex2d(0.0, 1.0);
		gl.glVertex2d(0.0, 0.0);
		gl.glVertex2d(1.0, 0.0);

		gl.glEnd();
	}
}

//******************************************************************************
