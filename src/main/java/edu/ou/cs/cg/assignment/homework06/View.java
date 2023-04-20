//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Sat Apr 16 14:56:23 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160425 [weaver]:	Original file.
// 20190129 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190203 [weaver]:	Additional cleanup and more extensive comments.
// 20200416 [weaver]:	Extensive modifications and additions for HW06.
// 20210416 [weaver]:	More cleanup and comments.
// 20220416 [weaver]:	Modified to incorporate Transforms and updates to Node.
//
//******************************************************************************
// Notes:
//
// Warning! This code uses depricated features of OpenGL, including immediate
// mode vertex attribute specification, for sake of easier classroom learning.
// See www.khronos.org/opengl/wiki/Legacy_OpenGL
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework06;

//import java.lang.*;
import java.awt.Font;
import java.awt.event.*;
//import java.awt.geom.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
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

	// Added texture files to the images/ directory then edit the filenames
	// below. *Important*: Make sure all of the images for textures are in the
	// images/ directory before submitting. All filenames are case sensitive. 
	private static final String		RSRC = "images/";
	private static final String[]		FILENAMES =
	{
		//Replaced the default skybox images with your own.
		"mountain-walls.jpg",	// Image #0 used to texture walls of the skybox
		"moon-ceiling.jpg",	// Image #1 used to texture ceiling of the skybox
		"rock-floor.jpg",	// Image #2 used to texture floor of the skybox

		// Leave these two alone! They're the actual textures for log objects.
		"log-ends.jpg",	// Image #3 used to texture ends of logs/branches
		"log-side.jpg",	// Image #4 used to texture sides of logs/branches

		// TODO: Uncomment, rename, add for any other texture images you need
		//"foo.jpg",		// Image #5 used to texture the...
		//"bar.jpg",		// Image #6 used to texture the...
		//"baz.jpg",		// Image #7 used to texture the...
		// etc.
	};

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						k;			// Animation counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;

	private Texture[]					textures;	// Loaded from FILENAMES
	private Node						root;		// Root node of scene graph

	// TODO: Add members to represent your particle systems

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		k = 0;
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

		root = new Node();

		initScene(drawable);
	}

	// Notification to release resources for the GLContext.
	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;

		disposeScene(drawable);
	}

	// Called to initiate rendering of each frame into the GLCanvas.
	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);

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
	// www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glBlendFunc.xml
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

		//Adjusted global ambient light for a campfire under a starlit sky.
		float[]	ambient = new float[] { 0.80f, 0.80f, 0.80f, 0.80f };

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
				System.exit(1);		// Bail if any image file is missing!
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

		// ****************************************
		// Zeroth step: Determine which part of the screen/window to draw into
		// ****************************************

		// Let JOGL take care of this (usually)
		//gl.glViewport(0, 0, w, h);					// Full window

		// ****************************************
		// First step: Position and orient the default camera
		// ****************************************

		// Use to adjust the frustum (clipped volume) relative to viewport
		float	aspect = (float)w/(float)h;		// Aspect ratio of viewport

		// Set up a typical perspective projection with 45 degree field-of-view
		// The closest z shown is 0.1, the farthest z shown is 50.0
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, aspect, 0.1f, 50.0f);

		// ****************************************
		// Second step: Position and orient the actual camera
		// www.opengl.org/archives/resources/faq/technical/viewing.htm
		// ****************************************

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// TODO: Place the camera to account for the position of the user and
		// where they're looking along the y axis (above the fire).
		glu.gluLookAt(0.0, 1.0, 3.0,		// Camera/eyes x, y, z
					  0.0, 0.0, 0.0,		// Focal point x, y, z
					  0.0, 1.0, 0.0);		// Above the head/"up"

		// ****************************************
		// Third step: position and orient the scene
		// ****************************************

		// TODO: Update the current viewing angle around the campfire based on
		// the interactively set angular rate of change.

		// TODO: Apply the current viewing angle to the rotation amount for
		// the entire scene. Currently rotates the scene by a fixed 30 degrees.
		gl.glRotated(30.0, 0.0, 1.0, 0.0);
		
		// Note: The user sets the *rate* of angular change. As the animation
		// runs, the scene should revolve around the fire at the current rate.
		// The rotation angle in the method above should change in each frame.
	}

	//**********************************************************************
	// Public Methods (Building)
	//**********************************************************************

	// TODO: Build your scene graph (permanent objects only here). Three example
	// Logs and a lighted Thing are provided to help you get started.
	public void	initScene(GLAutoDrawable drawable)
	{
		// Set the basic stage for the scene (don't touch these!)
		root.add(new Skycan(textures));				// Add a skycan
		root.add(new Spotlight());					// Add a spotlight


		// Creates an example log (glowing!) and add it to the scene.
		float[]	emit1 = new float[] { 0.8f, 0.6f, 0.0f, 1.0f };	// Yellow-ish
		Log		log1 = new Log(textures, 16, emit1);	// Log with 16 sides

		// Scale it short (y) and thick (x and z) like a trunk
		log1.pushTransform(new Transform.Scale(0.2f, 0.2f, 0.2f));
		// Rotate it a quarter circle around xz to be parallel to the ground
		log1.pushTransform(new Transform.Rotate(1.0f, 0.0f, 1.0f, 90.0f));
		// Translate it slightly embedded in ground (y) but still centered (xz).
		log1.pushTransform(new Transform.Translate(0.0f, 0.15f, 0.0f));

		root.add(log1);


		// Creates an example log (glowing!) and add it to the scene.
		float[]	emit2 = new float[] { 0.4f, 1.0f, 0.4f, 1.0f };	// Green-ish
		Log		log2 = new Log(textures, 7, emit2);		// Log with 7 sides

		// Scale it medium (y) and less thick (x and z) like a branch
		log2.pushTransform(new Transform.Scale(0.12f, 0.5f, 0.11f));
		// Rotate it 70 degrees around xy to be angled around and tilted upward
		log2.pushTransform(new Transform.Rotate(1.0f, 1.0f, 0.0f, 70.0f));
		// Translate it upward to lay on trunk log (y) and near center (xz).
		log2.pushTransform(new Transform.Translate(-0.1f, 0.45f, 0.0f));

		root.add(log2);


		// Creates an example log (glowing!) and add it to the scene.
		float[]	emit3 = new float[] { 0.3f, 0.3f, 1.0f, 1.0f };	// Blue-ish
		Log		log3 = new Log(textures, 4, emit3);		// Log with 4 sides

		// Scale it long (y) and skinny (x and z) like a stick
		log3.pushTransform(new Transform.Scale(0.05f, 0.7f, 0.07f));
		// Rotate it a quarter circle around z to be parallel to the ground
		log3.pushTransform(new Transform.Rotate(0.0f, 0.0f, 1.0f, 90.0f));
		// Translate it floating above the ground (y), offset (x), and out (z).
		log3.pushTransform(new Transform.Translate(0.2f, 0.6f, 1.2f));

		root.add(log3);


		// Create an example thing, make it a light, and add it to the scene.
		Thing	thing = new Thing(0.5f, 0.05f);				

		thing.setLight(GL2.GL_LIGHT1);

		root.add(thing);


		// TODO: Create other objects and add them to the scene graph

	}

	public void	disposeScene(GLAutoDrawable drawable)
	{
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	// Update the scene model for the current animation frame.
	private void	update(GLAutoDrawable drawable)
	{
		k++;								// Advance animation counter

		GL2	gl = drawable.getGL().getGL2();

		root.update(gl);

		// TODO: Update the object/nodes in your particle systems
	}

	// Render the scene model and display the current animation frame.
	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		root.enable(gl);				// Turn on lights in scene graph

		// TODO: Enable any light sources in your particle systems

		root.render(gl);				// Draw the scene graph

		// TODO: Draw the object/nodes in your particle systems

		root.disable(gl);				// Turn off lights in scene graph

		// TODO: Disable any light sources in your particle systems

		drawMode(drawable);			// Draw mode text (do this last)
	}

	// TODO: Draw text for the various interactively controlled model parameters
	// along the lefthand side of the canvas.
	private void	drawMode(GLAutoDrawable drawable)
	{
		renderer.beginRendering(w, h);

		// Draw all text in white
		renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);

		// Draw the primary keyboard options from the top of the window down.
		// Each line (including empty ones for spacing) gets 12 pixels of space.
		String	sfoo = FORMAT.format(new Double(model.getFoo()));

		renderer.draw("Foo     = " + sfoo, 2, h - 12);

		renderer.endRendering();
	}

	//**********************************************************************
	// Public Class Methods (Helpful Math for Dynamics Calculations)
	//**********************************************************************

	// Generates a random Gaussian, scales it, cuts it in a range.
	public static float	cutGaussian(float v, float s, float lo, float hi)
	{
		float	d = (float)(s * RANDOM.nextGaussian());

		return Math.max(lo, Math.min(v + d, hi));
	}

	// Repeatedly generates random numbers between 0.0 and 1.0 until a number
	// is below the threshold. Returns the count of numbers generated. Useful
	// for simulating natural phenomena that involve cascade effects.
	public static int	randomCascade(double threshold)
	{
		int	n = 0;

		while (RANDOM.nextDouble() > threshold)
			n++;

		return n;
	}

	//**********************************************************************
	// Inner Classes (Skycan Node)
	//**********************************************************************

	public static final class Skycan extends Node
	{
		//**************************************************************
		// Private Members
		//**************************************************************

		private final float[]	ambi = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
		private final float[]	diff = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
		private final float[]	spec = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

		private final Cylinder	skycan;	// Cylinder + ground + sky

		//**************************************************************
		// Constructors and Finalizer
		//**************************************************************

		public Skycan(Texture[] textures)
		{
			super(textures);

			// Scale horizontally (in x and z) but not vertically (in y).
			pushTransform(new Transform.Scale(4.0f, 1.0f, 4.0f));

			skycan = new Cylinder(32, 0.0f, 2.0f);
		}

		//**************************************************************
		// Override Methods (Node)
		//**************************************************************

		protected void	depict(GL2 gl)
		{
			Lighting.setMaterial(gl, ambi, diff, spec, null, null);

			skycan.fill(gl, 	getTexture(0));	// Around the cylinder
			skycan.fillFoot(gl, getTexture(2));	// Bottom end of the cylinder
			skycan.fillHead(gl, getTexture(1));	// Top end of the cylinder
		}
	}

	//**********************************************************************
	// Inner Classes (Spotlight Node)
	//**********************************************************************

	public static final class Spotlight extends Node
	{
		//**************************************************************
		// Private Members
		//**************************************************************

		private float	sdx;		// Running direction x amount
		private float	sdz;		// Running direction y amount
		private float	sdr;		// Running red intensity
		private float	sdg;		// Running green intensity

		//**************************************************************
		// Constructors and Finalizer
		//**************************************************************

		public void Spot()
		{
			sdx = 0.0f;
			sdz = 0.0f;
			sdr = 0.85f;
			sdg = 0.75f;
		}

		//**************************************************************
		// Override Methods (Node)
		//**************************************************************

		protected void	change(GL2 gl)
		{
			sdx = cutGaussian(sdx, 0.02f, -0.3f, 0.03f);
			sdz = cutGaussian(sdz, 0.02f, -0.3f, 0.03f);
			sdr = cutGaussian(sdr, 0.01f,  0.7f, 1.00f);
			sdg = cutGaussian(sdg, 0.01f,  0.5f, 1.00f);
		}

		protected void	enableLighting(GL2 gl)
		{
			gl.glEnable(GL2.GL_LIGHT0);

			// Add variable ruddy yellow spotlight pointing up from origin
			// Position, Intensity, Direction, Cutoff, Exponent
			float[]	lp1 = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
			float[]	li1 = new float[] {  sdr,  sdg, 0.0f, 0.5f };
			float[]	ld1 = new float[] {  sdx, 1.0f,  sdz, 1.0f };
			float[]	lc1 = new float[] { 15.0f };
			float[]	le1 = new float[] { 2f };

			Lighting.setLight(gl, GL2.GL_LIGHT0, lp1, li1, ld1, lc1, le1);
		}

		protected void	disableLighting(GL2 gl)
		{
			gl.glDisable(GL2.GL_LIGHT0);
		}
	}

	//**********************************************************************
	// Inner Classes (Various Object and Particle Nodes)
	//**********************************************************************

	// TODO: Implement your scene objects as subclasses of Node. Feel free to
	// reuse the example subclasses below. Add new ones to implement various
	// objects for your scene as needed.

	// Provides some basic functionality. Adapt to meet all of your Log needs!
	public static final class Log extends Node
	{
		//**************************************************************
		// Private Members
		//**************************************************************

		private final int		sides;			// Allow logs to look jagged
		private final float[]	emit;			// Allow logs to glow dimly

		private final Cylinder	cylinder;		// Geometry for the log

		//**************************************************************
		// Constructors and Finalizer
		//**************************************************************

		public Log(Texture[] textures, int sides, float[] emit)
		{
			super(textures);

			this.sides = sides;
			this.emit = emit;

			cylinder = new Cylinder(sides, -1.0f, 1.0f);
		}

		//**************************************************************
		// Override Methods (Node)
		//**************************************************************

		protected void	change(GL2 gl)
		{
			// The log just sits there, unchanging. Nothing to do...yet?
		}

		protected void	depict(GL2 gl)
		{
			Lighting.setMaterial(gl, null, null, null, null, emit);

			cylinder.fill(gl, 	  getTexture(4));	// Around sides
			cylinder.fillFoot(gl, getTexture(3));	// Bottom end
			cylinder.fillHead(gl, getTexture(3));	// Top end
		}
	}

	// An example of some ascending non-textured spherical thing that is also
	// a glowing light source. Lots of random changes happening each frame!
	public static final class Thing extends Node
	{
		//**************************************************************
		// Private Members
		//**************************************************************

		private float	start;	// Starting y position of the thing
		private float	scale;	// Baseline scale of the thing

		private float	dx;		// Current x drift of the thing
		private float	dy;		// Current y drift of the thing
		private float	dz;		// Current z drift of the thing
		private float	sf;		// Current uniform shrinking factor of the thing
		private float	b;		// Emission and diffuse light brightness scalar

		//**************************************************************
		// Constructors and Finalizer
		//**************************************************************

		public Thing(float start, float scale)
		{
			this.start = start;
			this.scale = scale;

			// Calculate initial drift and shrink factors for the thing.
			dx = 0.0f;//0.4f * (RANDOM.nextFloat() - 0.5f);
			dy = 0.0f;//0.1f * (RANDOM.nextFloat() - 0.5f);
			dz = 0.0f;//0.4f * (RANDOM.nextFloat() - 0.5f);
			sf = 1.0f;

			// Scale and translate the thing to its baseline size and position.
			// Scale and translate it by the initial shrink and drift factors
			pushTransform(new Transform.Scale(scale, scale, scale));
			pushTransform(new Transform.Scale(sf, sf, sf));
			pushTransform(new Transform.Translate(0.0f, start, 0.0f));
			pushTransform(new Transform.Translate(dx, dy, dz));
		}

		//**************************************************************
		// Override Methods (Node)
		//**************************************************************

		protected void	change(GL2 gl)
		{
			// Remove the old transforms.
			popTransform();
			popTransform();
			popTransform();
			popTransform();

			// Randomly drifts position (variation translation amounts).
			dx += 0.006f * (RANDOM.nextFloat() - 0.5f);		// Nudge x
			dy += 0.003f + 0.002f * RANDOM.nextFloat();		// Nudge y
			dz += 0.006f * (RANDOM.nextFloat() - 0.5f);		// Nudge z

			// Calculate a new shrinking factor, slightly smaller than before.
			sf = Math.max(0.0f, sf - 0.00005f * RANDOM.nextFloat());

			// Teleports back to baseline position when the drift is too high.
			if (dy > 2.0f)
				dy = 0.0f;

			// Scale and translate the thing to its baseline size and position.
			// Scale and translate it by the current shrink and drift factors.
			// Add the new transforms.
			pushTransform(new Transform.Scale(scale, scale, scale));
			pushTransform(new Transform.Scale(sf, sf, sf));
			pushTransform(new Transform.Translate(0.0f, start, 0.0f));
			pushTransform(new Transform.Translate(dx, dy, dz));

			// Randomly generates new brightness scalar.
			b = 0.5f + 0.05f * RANDOM.nextFloat();
		}

		protected void	depict(GL2 gl)
		{
			float[]	emit =
				new float[] { 1.0f * b, 0.8f * b, 0.3f * b, 1.0f };

			Lighting.setMaterial(gl, null, null, null, null, emit);
			MYGLUT.glutSolidSphere(1.0, 8, 8);
		}

		protected void	enableLighting(GL2 gl)
		{
			float[]	lightColor =
				new float[] { 1.0f * b, 0.5f * b, 0.2f * b, 1.0f };

			enableLightDiffuse(gl, lightColor, dx, dy, dz);
		}

		protected void	disableLighting(GL2 gl)
		{
			disableLightDiffuse(gl);
		}
	}
}

//******************************************************************************
