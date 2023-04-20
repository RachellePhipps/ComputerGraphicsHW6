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

package edu.ou.cs.cg.assignment.homework06;

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

		// TODO: YOUR KEYBOARD EVENT HANDLING CODE HERE

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_Q:
				//sounds like a threat
				System.out.println("Quitting isn't an option! ;-)");
				return;

			//case:','
			case KeyEvent.VK_COMMA:
				model.setCamera_distance(b, KeyEvent.VK_COMMA);
				return;

			//case: '.'
			case KeyEvent.VK_PERIOD:
				model.setCamera_distance(b, KeyEvent.VK_PERIOD);
				return;

			//case up
			case KeyEvent.VK_UP:
				model.setFocal_point_height(b, KeyEvent.VK_UP);
				return;

			//case down
			case KeyEvent.VK_DOWN:
				model.setFocal_point_height(b, KeyEvent.VK_DOWN);
				return;

			//left
			case KeyEvent.VK_LEFT:
				model.setScene_rotation_rate(b, KeyEvent.VK_LEFT);
				return;

			//right
			case KeyEvent.VK_RIGHT:
				model.setScene_rotation_rate(b, KeyEvent.VK_RIGHT);

		}
	}
}

//******************************************************************************
