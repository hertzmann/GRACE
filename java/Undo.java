/* GRACE - Graphical Ruler and Compass Editor
 *
 * Undo.java
 *
 * This file contains the Undo class for storing enough information
 * to undo the last step.
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.awt.MenuItem;
import java.util.*;

public class Undo
{
  // type of steps
  static final int CANT_UNDO = 0;
  static final int CONSTRUCTION = 1;
  static final int PLACE_POINT = 2;
  static final int DRAG = 3;
  static final int FORCE = 4;
  static final int TEST = 5;
  static final int ASSUME = 6;
  static final int OUTPUT = 7;
  static final int CONCLUDE = 8;
  static final int SELECT = 9;

  /** The type of the last step */
  int lastStepType = CANT_UNDO;

  /** The nullspace before the last step */
  Nullspace lastNullspace = null;

  /** A dependency associated with the last step */
  Dependency lastStep = null;

  /** The input location before drag */
  double x,y;

  // some other windows
  DrawPanel drawPanel;
  Editor editor;
  ConstraintFrame constraintFrame;

  /** The undo menu item in the main window.  We can enable and disable it */
  MenuItem undoButton;

  Undo(MenuItem ub)
  {
    undoButton = ub;
  }

  /** Set it so the previous step can't be undone */
  void setCantUndo()
  {
    lastStepType = CANT_UNDO;
    lastNullspace = null;
    lastStep = null;
    undoButton.disable();
  }

  /** Save the nullspace */

  void saveState()
  {
    lastNullspace = (Nullspace)constraintFrame.nullspace.clone();
  }

  /** Save an intermediate step */

  void saveStep(Dependency d)
  {
    lastStep = d;
    lastStepType = CONSTRUCTION;
    undoButton.enable();
  }

  /** Save a selection step */

  void saveSelectStep()
  {
    lastStepType = SELECT;
    undoButton.enable();
  }

  /** Save a select input step */

  void saveStep(Shape input)
  {
    lastNullspace = null;
    lastStep = input.source;
    lastStepType = PLACE_POINT;
    undoButton.enable();
  }

  /** Save a drag step */

  void saveDragStep(PointShape input)
  {
    lastStepType = DRAG;
    lastStep = input.source;
    x = input.x;
    y = input.y;
    undoButton.enable();
  }

  /** Save the step of another type */

  void saveStep(int type)
  {
    lastStepType = type;
    undoButton.enable();
  }

  /** Undo the previous step */

  void undo()
  {
    PointShape ps;
    Shape s;

    switch (lastStepType)
      {
      case CANT_UNDO:
	return;

      case SELECT:
	break;

      case DRAG:
	ps = (PointShape)lastStep.children[0];
	drawPanel.mark(ps);
	drawPanel.dragPoint(x,y,ps);
	break;

      case PLACE_POINT:
	ps = (PointShape)lastStep.children[0];
	editor.removeLastInput();
	drawPanel.shapes.removeElement(ps);
	drawPanel.names.remove(ps.label);
	break;

      case CONSTRUCTION:
	// remove new shapes
	for(int i=0;i<lastStep.children.length;i++)
	  {
	    // delete partial order info for new points
	    if (lastStep.children[i] instanceof PointShape)
	      ((PointShape)lastStep.children[i]).deletePOs();

	    drawPanel.shapes.removeElement(lastStep.children[i]);
	    drawPanel.names.remove(lastStep.children[i].label);
	  }
	for(int i=0;i<lastStep.parents.length;i++)
	  lastStep.parents[i].offspring.removeElement(lastStep);
	editor.removeLastStep();
//	editor.removeStep(lastStep);

	if (lastNullspace == null)
	  System.out.println("Warning: lastNullspace == null");

	constraintFrame.nullspace = lastNullspace;

	if (lastStep instanceof ConstructionDependency ||
	    lastStep instanceof IntersectionDependency)
	  {
	    // remove any new constraints
	    constraintFrame.removeSteps(editor.stepsList.countItems()+"");

	    // check for circles that got their pointOnCircle created
	    for(int i=0;i<lastStep.parents.length;i++)
	      {
		if (lastStep.parents[i] instanceof Circle)
		  {
		    Circle c = (Circle)lastStep.parents[i];

		    if (c.pointOnCircle != null)
		      for(int j=0;j<lastStep.children.length;j++)
			if (lastStep.children[j] == c.pointOnCircle)
			  {
			    c.pointOnCircle = null;
			    break;
			  }
		  }
	      }
	  }
	
	break;

      case FORCE:
	if (lastNullspace == null)
	  System.out.println("Warning: lastNullspace == null");

	constraintFrame.nullspace = lastNullspace;
	constraintFrame.removeSteps("F");
	break;

      case TEST:
	constraintFrame.removeSteps("T");
	break;

      case ASSUME:
	if (lastNullspace == null)
	  System.out.println("Warning: lastNullspace == null");

	constraintFrame.nullspace = lastNullspace;
	constraintFrame.removeInput();
	break;

      case OUTPUT:
	drawPanel.selected.removeLast();
	editor.removeLastOutput();
	drawPanel.redraw();
	setCantUndo();
	return;

      case CONCLUDE:
	if (lastNullspace == null)
	  System.out.println("Warning: lastNullspace == null");

	constraintFrame.nullspace = lastNullspace;
	constraintFrame.removeOutput();
	break;
    
      }

    // reset and redraw
    drawPanel.selected.clear();
    drawPanel.redraw();
    setCantUndo();
  }
}

