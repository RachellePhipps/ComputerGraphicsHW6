//******************************************************************************
// Copyright (C) 2016-2023 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Sat Apr  1 13:44:38 2023 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160225 [weaver]:	Original file.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20190318 [weaver]:	Modified for homework04.
// 20230401 [weaver]:	Added TODO comment.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework05;

//import java.lang.*;
import java.awt.Component;
import java.awt.event.*;
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
		// Hint: You will need a case that calls a Model.set() method
		// for each of the keyboard interactions you support.

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_Q:
				System.out.println("Why would you want to quit Blobster? :(");
				return;
		}
	}
}

//******************************************************************************
