//******************************************************************************
// Copyright (C) 2016 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Wed Apr 24 13:54:51 2019 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.texture;

//import java.lang.*;
import java.awt.Component;
import java.awt.event.*;
import java.awt.geom.Point2D;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>KeyHandler</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class KeyHandler extends KeyAdapter
{
	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final View		view;
	private final Model	model;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public KeyHandler(View view, Model model)
	{
		this.view = view;
		this.model = model;

		Component	component = view.getCanvas();

		component.addKeyListener(this);
	}

	//**********************************************************************
	// Override Methods (KeyListener)
	//**********************************************************************

	public void	keyPressed(KeyEvent e)
	{
		boolean	b = Utilities.isShiftDown(e);
		int		slices = model.getSlices();
		double		position = model.getPosition();
		double		rotation = model.getRotation();
		double		scale = model.getScale();
		double		radius = model.getRadius();

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_Q:
				model.setSlices(slices - 1);
				break;

			case KeyEvent.VK_W:
				model.setSlices(slices + 1);
				break;

			case KeyEvent.VK_LEFT:
				model.setRotation((b ? rotation - 0.5 : rotation - 5.0));
				break;

			case KeyEvent.VK_RIGHT:
				model.setRotation((b ? rotation + 0.5 : rotation + 5.0));
				break;

			case KeyEvent.VK_UP:
				model.setPosition((b ? position + 0.02 : position + 0.2));
				break;

			case KeyEvent.VK_DOWN:
				model.setPosition((b ? position - 0.02 : position - 0.2));
				break;

			case KeyEvent.VK_A:
				model.setScale((b ? scale - 0.01 : scale - 0.1));
				break;

			case KeyEvent.VK_S:
				model.setScale((b ? scale + 0.01 : scale + 0.1));
				break;

			case KeyEvent.VK_Z:
				model.setRadius((b ? radius - 0.05 : radius - 0.5));
				break;

			case KeyEvent.VK_X:
				model.setRadius((b ? radius + 0.05 : radius + 0.5));
				break;

			case KeyEvent.VK_E:
				model.toggleShowEdges();
				break;

			case KeyEvent.VK_1:
				model.toggleShowSun();
				break;
			case KeyEvent.VK_2:
				model.toggleShowMovers();
				break;
			case KeyEvent.VK_3:
				model.toggleShowGround();
				break;
			case KeyEvent.VK_4:
				model.toggleShowSky();
				break;
			case KeyEvent.VK_5:
				model.toggleShowHorizon();
				break;
			case KeyEvent.VK_6:
				model.toggleShowRearView();
				break;
			case KeyEvent.VK_7:
				model.toggleShowDownView();
				break;
		}
	}
}

//******************************************************************************
