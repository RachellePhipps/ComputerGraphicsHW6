//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Mon Mar 28 14:53:41 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20220328 [weaver]:	Added skew transforms.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.application.transform2;

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

	public void		keyPressed(KeyEvent e)
	{
		Point2D.Double	p = model.getOrigin();
		boolean		b = Utilities.isShiftDown(e);
		double			a = (b ? 0.01 : 0.1);

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_NUMPAD5:
				p.x = 0.0;	p.y = 0.0;	break;

			case KeyEvent.VK_NUMPAD4:
			case KeyEvent.VK_LEFT:
				p.x -= a;		p.y += 0.0;	break;

			case KeyEvent.VK_NUMPAD6:
			case KeyEvent.VK_RIGHT:
				p.x += a;		p.y += 0.0;	break;

			case KeyEvent.VK_NUMPAD2:
			case KeyEvent.VK_DOWN:
				p.x += 0.0;	p.y -= a;		break;

			case KeyEvent.VK_NUMPAD8:
			case KeyEvent.VK_UP:
				p.x += 0.0;	p.y += a;		break;

			case KeyEvent.VK_NUMPAD1:
				p.x -= a;		p.y -= a;		break;

			case KeyEvent.VK_NUMPAD7:
				p.x -= a;		p.y += a;		break;

			case KeyEvent.VK_NUMPAD3:
				p.x += a;		p.y -= a;		break;

			case KeyEvent.VK_NUMPAD9:
				p.x += a;		p.y += a;		break;

			// TSR = translate, scale, rotate
			// Lowercase tsr for transform, uppercase TSR for inverse
			case KeyEvent.VK_T:
				model.pushTransform((b ? "T" : "t"));
				return;
			case KeyEvent.VK_S:
				model.pushTransform((b ? "S" : "s"));
				return;
			case KeyEvent.VK_R:
				model.pushTransform((b ? "R" : "r"));
				return;
			case KeyEvent.VK_K:
				model.pushTransform((b ? "K" : "k"));
				return;

			case KeyEvent.VK_CLEAR:
			case KeyEvent.VK_D:
			case KeyEvent.VK_DELETE:
				model.popTransform();
				return;
		}

		model.setOriginInSceneCoordinates(p);
	}
}

//******************************************************************************
