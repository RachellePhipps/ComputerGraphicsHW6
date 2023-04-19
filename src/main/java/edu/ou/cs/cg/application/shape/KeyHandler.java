//******************************************************************************
// Copyright (C) 2016 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Tue Apr 26 20:30:51 2016 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160425 [weaver]:	Original file.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.shape;

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
		int	mode = view.getMode();
		float	v = view.getValue(mode);

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_0:
				view.toggleLight(0);
				break;

			case KeyEvent.VK_1:
				view.toggleLight(1);
				break;

			case KeyEvent.VK_2:
				view.toggleLight(2);
				break;

			case KeyEvent.VK_3:
				view.toggleLight(3);
				break;

			case KeyEvent.VK_E:
				view.toggleEdge();
				break;

			case KeyEvent.VK_F:
				view.toggleFill();
				break;

			case KeyEvent.VK_C:
				view.toggleAxes();
				break;

			case KeyEvent.VK_Q:
				view.decrementShape();
				break;

			case KeyEvent.VK_W:
				view.incrementShape();
				break;

			case KeyEvent.VK_A:
				view.decrementSlices();
				break;

			case KeyEvent.VK_S:
				view.incrementSlices();
				break;

			case KeyEvent.VK_Z:
				view.decrementStacks();
				break;

			case KeyEvent.VK_X:
				view.incrementStacks();
				break;

			case KeyEvent.VK_LEFT:
				view.setMode((mode + 14) % 15);
				break;

			case KeyEvent.VK_RIGHT:
				view.setMode((mode + 1) % 15);
				break;

			case KeyEvent.VK_DOWN:
				view.setValue(mode, v - a);
				break;

			case KeyEvent.VK_UP:
				view.setValue(mode, v + a);
				break;
		}
	}
}

//******************************************************************************
