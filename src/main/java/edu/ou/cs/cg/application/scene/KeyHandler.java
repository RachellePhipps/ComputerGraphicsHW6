//******************************************************************************
// Copyright (C) 2016 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Mon Apr 25 16:10:23 2016 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.scene;

//import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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
	private final View	view;

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public KeyHandler(View view)
	{
		this.view = view;

		Component	component = view.getComponent();

		component.addKeyListener(this);
	}

	//**********************************************************************
	// Override Methods (KeyListener)
	//**********************************************************************

	public void		keyPressed(KeyEvent e)
	{
		float	a = (Utilities.isShiftDown(e) ? 0.1f : 0.01f);
		int		mode = view.getMode();
		float	v = view.getValue(mode);

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_LEFT:
				view.setMode((mode + 11) % 12);
				break;

			case KeyEvent.VK_RIGHT:
				view.setMode((mode + 1) % 12);
				break;

			case KeyEvent.VK_DOWN:
				view.setValue(mode, v - a);
				break;

			case KeyEvent.VK_UP:
				view.setValue(mode, v + a);
				break;

			case KeyEvent.VK_A:
				view.decrementSides();
				break;

			case KeyEvent.VK_S:
				view.incrementSides();
				break;
		}
	}
}

//******************************************************************************
