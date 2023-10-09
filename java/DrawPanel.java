/* GRACE - Graphical Ruler and Compass Editor
 *
 * DrawPanel.java
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.util.*;
import java.awt.*;
import java.lang.*;

/** The main canvas where all the construction display occurs */
public class DrawPanel extends Canvas implements Constants
{
  /** The current system mode */
  int mode = POINT_MODE;

  /** List of all shapes that exist (including invalid shapes) */
  Vector shapes = new Vector();       // all the shapes 
  Selected selected = new Selected(); // currently selected shapes
  Hashtable names = new Hashtable();  // a list of shape names in use

  PointShape dragPoint;               // which point is being dragged
  boolean showLabels = false;         // Do we show shape text labels?
  boolean doubleBuffer = false;

  // used to mark dependencies as dependent on the drag point
  Vector dragDeps = new Vector();

  TextField messageBox;               // field for displaying messages    
  ConstructionPanel cp;               // where the constructions are stored
  ExpressionFrame expressionFrame;    // The expressions window
  ConstraintFrame constraintFrame;    // The constraint window
  Editor editor;                      // The text window
  Undo undo;                          // The undo data

  Transform transform = new Transform();   // The view-plane transformation

  // The constructor
  public DrawPanel(TextField messages,ConstructionPanel c,Editor e,Undo u,
		   boolean db)
    {
      // initialize a bunch of data
      setBackground(FIELD_BACKGROUND);
      messageBox = messages;
      cp = c;
      dragPoint = null;
      editor = e;
      undo = u;
      resize(DP_WIDTH,DP_HEIGHT);
      doubleBuffer = db;
      transform.resize(size());
    }

  // change the current drawing mode
  public void setDrawMode(int newMode)
    {
      // change the mode
      mode = newMode;

      if (newMode != APPLY_MODE && newMode != LINE_SEGMENT_MODE && 
	  newMode != CIRCLE_MODE && newMode != INTERSECT_MODE &&
	  newMode != LINE_MODE && newMode != RAY_MODE &&
	  newMode != COMPL_RAY_MODE && newMode != PERP_BI_MODE)
	cp.deselect();   // deselect any constructions

      // deselect any other selections
      expressionFrame.deselectAll();
      expressionFrame.editExpr = null;
      constraintFrame.deselectAll();
      selected.clear();

      // reset the drag point
      dragPoint = null;

      // clear the undo data
      undo.setCantUndo();

      // display an appropriate prompt
      switch (newMode) 
	{
	case CONCLUDE_MODE:
	  message("Select output constraint or two graphical expression displays");
	  break;

	case OUTPUT_MODE:
	  message("Select output shapes");
	  for(int i=0;i<editor.outputParents.size();i++)
	    {
	      Dependency d = (Dependency)editor.outputParents.elementAt(i);
	      int j=((Integer)editor.outputChildren.elementAt(i)).intValue();

	      selected.add(d.children[j]);
	    }
	  break;

	case LABEL_MODE:
	  message("Select a graphical expression display to edit");
	  break;

	case DELETE_EXPRESSIONS_MODE:
	  message("Select graphical expression displays to delete");
	  break;

	case FORCE_CONSTRAINT_MODE:
	case TEST_CONSTRAINT_MODE:
	case ASSUME_CONSTRAINT_MODE:
	  message("Select two graphical expression displays");
	  break;
	  
	case LABEL_DISTANCE_MODE:
	  message("Select first point");
	  break;

	case LABEL_ANGLE_MODE:
	  message("Select first point");
	  break;

	case DEBUG_MODE:
	  message("Select object to identify.");
	  break;

	case DRAG_MODE:
	  message("Select free point to drag.");
	  break;

	case APPLY_MODE:
//	  cp.select();
	  break;

	case CIRCLE_MODE:
	  message("Select circle center.");
	  break;

	case PERP_BI_MODE:
	  message("Select first point.");
	  break;

	case LINE_MODE:
	  message("Select point on line.");
	  break;

	case LINE_SEGMENT_MODE:
	  message("Select first endpoint.");
	  break;

	case RAY_MODE:
	case COMPL_RAY_MODE:
	  message("Select endpoint.");
	  break;

	case POINT_MODE:
	  message("Place free points.");
	  break;

	case INTERSECT_MODE:
	  message("Select first shape.");
	  break;

	default:
	  throw new IllegalArgumentException();
	}

      // redraw in case anything is selected
      redraw();
    }
  
  void erase()
    // clear the drawing area and delete all shapes
    {
      names.clear();
      shapes.removeAllElements();
      selected.clear();
      cp.setStatus(true);

      Dimension s = size();

      getGraphics().clearRect(0,0,s.width,s.height);

      transform.scaleFactor = 1;
      transform.resize(s);
    }

  // find a shape near a mouse click, if any
  public Shape findShape(int x,int y)
    {
      // start by looking for a point near the shape
      Shape s = findPointShape(x,y);

      if (s != null)
	return s;

      // convert screen coordinates to virtual coordinates
      double vX = transform.screenToVirtualX(x);
      double vY = transform.screenToVirtualY(y);

      // determine the virtual tolerance
      double tolerance = TOLERANCE / transform.scaleFactor;

      // search in reverse order of shape creation
      for(int i=shapes.size()-1;i>=0;i--)
	{
	  Shape next = (Shape)shapes.elementAt(i);
	  if (next.isPointNearShape(vX,vY,tolerance))
	    return next;
	}
      
      return null;
    }

  // find a point near a mouse click, if any
  public PointShape findPointShape(int x,int y)
    {
      // convert screen coordinates to virtual coordinates
      double vX = transform.screenToVirtualX(x);
      double vY = transform.screenToVirtualY(y);

      // determine the virtual tolerance
      double tolerance = TOLERANCE / transform.scaleFactor;

      // search in reverse order of point creation
      for(int i=shapes.size()-1;i>=0;i--)
	{
	  Shape next = (Shape)shapes.elementAt(i);
	  if (next instanceof PointShape &&
	      next.isPointNearShape(vX,vY,tolerance))
	    return (PointShape)next;
	}

      return null;
    }

  // find an input point near a mouse click, if any
  public PointShape findFreePointShape(int x,int y)
    {
      // convert screen coordinates to virtual coordinates
      double vX = transform.screenToVirtualX(x);
      double vY = transform.screenToVirtualY(y);

      // determine the virtual tolerance
      double tolerance = TOLERANCE / transform.scaleFactor;

      // search in reverse order of piont placement
      for(int i=shapes.size()-1;i>=0;i--)
	{
	  Shape next = (Shape)shapes.elementAt(i);
	  if (next.source instanceof ArbitraryDependency &&
	      next.isPointNearShape(vX,vY,tolerance))
	    return (PointShape)next;
	}

      return null;
    }

  // handle a mouse click in the drawing area
  public boolean mouseDown(Event e,int x,int y)
    {
      // some temporary variables
      PointShape newPoint,firstPoint;
      Shape firstShape,newShape;
      Dependency link;
      LineShape newLine;
      MeasureDependency mlink;
      Shape finalShape;
      int stepNum;

      switch (mode)
	{
	case APPLY_MODE:
	  Construction c = cp.current;  // the current construction to apply

	  // check if a construction is selected
	  if (c == null)
	    break;

	  // look for input point near the click
	  newPoint = findPointShape(e.x,e.y);

	  // was one found?
	  if (newPoint == null)
	    break;

	  // select the new point
	  selected.add(newPoint);

	  if (selected.size() < c.numberOfInputs)
	    {
	      // put a message for the next input
	      message("Select "+c.ruleName(selected.size()));
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  // all the input points have been selected

	  // save the nullspace, etc.
	  undo.saveState();
	  
	  Shape[] inputs = new Shape[c.numberOfInputs];
	  
	  // copy the selected points into an array
	  for(int i=0;i<c.numberOfInputs;i++)
	    inputs[i] = (Shape)selected.shapes.elementAt(i);
	  
	  // clear the list of selected points
	  selected.clear();
	    
	  Shape[] result;
	  
	  // count the number of steps so far
	  stepNum = editor.stepsList.countItems();
	  
	  try
	    {
	      // apply the construction
	      result = c.apply(inputs,constraintFrame,
			       shapes.size(),null,names,stepNum);
	    }
	  catch (ConstructionError ce)
	    {
	      // display the exception message
	      message(ce.getMessage());
	      redraw();
	      break;
	    }

	  // create a new dependency and link it all up
	  link = new ConstructionDependency();

	  for(int i=0;i<result.length;i++)
	    {
	      shapes.addElement(result[i]);
	      result[i].source = link;
	    }

	  link.children = result;
	  link.parents = inputs;

	  ((ConstructionDependency)link).construction = c;

	  for(int i=0;i<inputs.length;i++)
	    inputs[i].offspring.addElement(link);

	  editor.addStep(link);

	  undo.saveStep(link);

	  // redraw the screen
	  redraw();

	  // give a message
	  message(result.length + " new shapes created, "+
		  c.outputConstraints.size() + " output constraints");
	  break;

	case OUTPUT_MODE:

	  // look for a shape near the mouse click
	  finalShape = findShape(e.x,e.y);

	  // is one there?
	  if (finalShape == null)
	    break;

	  if (finalShape.source instanceof ArbitraryDependency)
	    {
	      message("Input points may not be outputs");
	      break;
	    }

	  // add the shape to the outputs
	  if (editor.addOutput(finalShape))
	    {
	      undo.saveStep(undo.OUTPUT);
	      selected.add(finalShape);
	      redraw();
	    }
	  else
	    message(finalShape+" is already an output");

	  break;

	case INTERSECT_MODE:

	  // shape intersection mode
	  newShape = findShape(e.x,e.y);

	  // if no shape, deselect all
	  if (newShape == null)
	    {
	      message("Select first shape.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      
	      break;
	    }

	  // first shape; prompt for second
	  if (selected.size() == 0)
	    {
	      selected.add(newShape);
	      message("Select second shape.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select first shape.");

	  // get first shape and deselect
	  firstShape = (Shape) selected.shapes.elementAt(0);
	  selected.clear();
	  
	  // check if they're the same
	  if (newShape == firstShape)
	  {
	    redraw();
	    undo.setCantUndo();
	    break;
	  }

	  // save the nullspace, etc.
	  undo.saveState();

	  stepNum = editor.stepsList.countItems();

	  // compute the intersection point(s)
	  Shape[] intersection = 
	    Geometry.Intersection(firstShape,newShape,constraintFrame,
				  shapes.size(),null,names,
				  stepNum+"A: ");

	  // create a dependency and link everything
	  link = new IntersectionDependency();

	  for(int i=0;i<intersection.length;i++)
	    {
	      if (DEBUG)
		System.out.println(intersection[i].label + " becomes I"+shapes.size());

	      shapes.addElement(intersection[i]);
	      intersection[i].source = link;
	    }

	  link.parents = new Shape[2];
	  link.parents[0] = firstShape;
	  link.parents[1] = newShape;
	  link.children = intersection;

	  firstShape.offspring.addElement(link);
	  newShape.offspring.addElement(link);

	  // display the step
	  editor.addStep(link);

	  undo.saveStep(link);

	  message(intersection.length + " new shapes");

	  redraw();
	  break;

	case LINE_SEGMENT_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  if (newPoint == null)
	    {
	      message("Select first endpoint.");
	      selected.clear();
	      redraw();

	      break;
	    }

	  // save the selected point
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select second endpoint.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  // both points selected; deselect all
	  message("Select first endpoint.");

	  firstPoint = (PointShape) selected.shapes.elementAt(0);

	  selected.clear();

	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // save the nullspace
	  undo.saveState();

	  // create the new shape and link it all in
	  newLine = new LineSegment(firstPoint,newPoint);
	  newLine.label = uniqueName("LS",names,shapes.size());

	  shapes.addElement(newLine);

	  link = new LineSegmentDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newLine;
	  newLine.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);

	  // set up the new partial order
	  newLine.makePO(firstPoint,newPoint);
	  editor.addStep(link);

	  undo.saveStep(link);

	  // display the change
	  redraw();
	  break;

	case LINE_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  // if none, deselect all
	  if (newPoint == null)
	    {
	      message("Select first point on line.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first point selected, select it
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select second point on line.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select first point on line.");

	  firstPoint = (PointShape) selected.shapes.elementAt(0);

	  selected.clear();
	  
	  // check if the two points are the same
	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  undo.saveState();

	  // create the new shape
	  newLine = new Line(firstPoint,newPoint);
	  newLine.label = uniqueName("L",names,shapes.size());

	  shapes.addElement(newLine);

	  // link everything together
	  link = new LineDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newLine;
	  newLine.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);
	  editor.addStep(link);

	  undo.saveStep(link);
	  redraw();
	  break;

	case PERP_BI_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  // does one exist?
	  if (newPoint == null)
	    {
	      message("Select first point.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first selected point, select it
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select second point.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select first point.");

	  firstPoint = (PointShape) selected.shapes.elementAt(0);

	  // clear the selection
	  selected.clear();

	  // are the two points the same point?
	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  undo.saveState();

	  // create the new shape
	  newLine = new PerpBi(firstPoint,newPoint);
	  newLine.label = uniqueName("B",names,shapes.size());

	  // link everything together
	  shapes.addElement(newLine);

	  link = new PerpBiDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newLine;
	  newLine.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);
	  editor.addStep(link);

	  undo.saveStep(link);
	  redraw();
	  break;

	case RAY_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  // if no point, deselect others
	  if (newPoint == null)
	    {
	      message("Select endpoint.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first point, select it
	  if (selected.size() == 0)
	    {
              selected.add(newPoint);
	      message("Select point on ray.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select endpoint.");

	  firstPoint = (PointShape) selected.shapes.elementAt(0);

	  // deselect all
	  selected.clear();

	  // if the points are the same, ignore
	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  undo.saveState();

	  // create the new shape
	  newLine = new Ray(firstPoint,newPoint);
	  newLine.label = uniqueName("R",names,shapes.size());

	  shapes.addElement(newLine);

	  // link the everything together
	  link = new RayDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newLine;
	  newLine.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);

	  newLine.makePO(firstPoint,newPoint);
	  editor.addStep(link);

	  undo.saveStep(link);
	  redraw();
	  break;

	case COMPL_RAY_MODE:
	  
	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  // if no point, deselect any others
	  if (newPoint == null)
	    {
	      message("Select endpoint.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first point, select it
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select point opposite ray.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select endpoint.");

	  firstPoint = (PointShape) selected.shapes.elementAt(0);

	  // deselect all
	  selected.clear();

	  // check if the first and second point are the same
	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // save the nullspace
	  undo.saveState();

	  // create the new shape
	  newLine = new ComplRay(firstPoint,newPoint);
	  newLine.label = uniqueName("CR",names,shapes.size());

	  shapes.addElement(newLine);

	  // link everything together
	  link = new ComplRayDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newLine;
	  newLine.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);
	  newLine.makePO(firstPoint,newPoint);
	  editor.addStep(link);

	  // redraw
	  undo.saveStep(link);
	  redraw();
	  break;

	case CIRCLE_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  // if none, deselect any points
	  if (newPoint == null)
	    {
	      message("Select circle center.");

	      selected.clear();
	      redraw();

	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first, select it
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select point on circle.");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  message("Select circle center.");

	  firstPoint = (PointShape) selected.shapes.firstElement();

	  // deselect all
	  selected.clear();

	  // check if the points are the same
	  if (newPoint == firstPoint)
	    {
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  undo.saveState();

	  // create the new shape
	  Circle newCircle = new Circle(firstPoint,newPoint);
	  newCircle.label = uniqueName("C",names,shapes.size());
	  
	  shapes.addElement(newCircle);

	  // link everything together
	  link = new CircleDependency();
	  link.parents = new Shape[2];
	  link.parents[0] = firstPoint;
	  link.parents[1] = newPoint;
	  link.children = new Shape[1];
	  link.children[0] = newCircle;
	  newCircle.source = link;
	  firstPoint.offspring.addElement(link);
	  newPoint.offspring.addElement(link);

	  editor.addStep(link);
	  undo.saveStep(link);

	  // redraw the screen
	  redraw();
	  break;

	case DRAG_MODE:

	  // find an input point near the click
	  dragPoint = findFreePointShape(e.x,e.y);

	  if (dragPoint == null)
	    {
	      message("No input point there");
	      break;
	    }

	  message("Drag.");

	  // set up the dragDeps list for dragging
	  mark(dragPoint);     

	  undo.saveDragStep(dragPoint);
	  break;

	case DEBUG_MODE:

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  if (newPoint == null)
	    break;

	  // print out some information about the selected point

	  System.out.println(newPoint.toString() + ":");
	  System.out.println(newPoint.source.parents.length + " parents");
	  System.out.println(newPoint.offspring.size() + " offspring");
	  
	  for(int i=0;i<newPoint.POs.size();i++)
	    {
	      System.out.println("PARTIAL ORDER "+i);
	      System.out.print(((PartialOrder)newPoint.POs.elementAt(i)).toString());
	    }

	  for(int i=0;i<newPoint.uniques.size();i++)
	    {
	      System.out.println("UNIQUES");
	      System.out.println(newPoint.uniques.elementAt(i).toString());
	    }

	  break;

	case POINT_MODE:
	default:
	  
	  // create a new point at the mouse click location
	  newPoint = new PointShape(transform.screenToVirtualX(e.x),
				    transform.screenToVirtualY(e.y));
	  newPoint.free = true;
	  newPoint.label = "P" + shapes.size();
	  shapes.addElement(newPoint);

	  link = new ArbitraryDependency();
	  newPoint.source = link;

	  link.parents = new Shape[0];
	  link.children = new Shape[1];
	  link.children[0] = newPoint;

	  message(newPoint.label + ": " +e.x+","+e.y);
	  editor.addInput(newPoint);   // add it to the list of inputs

	  undo.saveStep(newPoint);

	  // redraw
	  redraw();
	  break;

	case LABEL_ANGLE_MODE:

	  // check for a right click
	  if ((e.modifiers & Event.META_MASK) == Event.META_MASK)
	    {
	      // start a new angle expression
	      expressionFrame.newAngle();

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // find a point near the mouse click
	  newPoint = findPointShape(e.x,e.y);

	  if (newPoint == null)
	    break;

	  // select the new point
	  selected.add(newPoint);

	  // if less than three points are selected, give a message

	  if (selected.size() == 1)
	    {
	      message("Select apex");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  if (selected.size() == 2)
	    {
	      message("Select final point");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  undo.setCantUndo();

	  mlink = null;

	  PointShape p1 = (PointShape)selected.shapes.elementAt(0);
	  PointShape p2 = (PointShape)selected.shapes.elementAt(1);

	  mlink = p2.getAngleMeasure(p1,newPoint);

	  selected.clear();
	  redraw();

	  /*
	  if (mlink == null)
	    {
	      mlink = new AngleMeasure(p1,p2,newPoint);
	      mlink.measures.addElement(expressionFrame.editExpr);
	    }
	  else
	  */
	  // add the angle expression to this angle's list of expressions
	    if (mlink.measures.indexOf(expressionFrame.editExpr) < 0)
	      mlink.measures.addElement(expressionFrame.editExpr);

	    // add the angle to the expression
	  expressionFrame.editExpr.add(mlink);
	  expressionFrame.editExpr.repaint();
	  message("Select first point");
	  break;

	case LABEL_DISTANCE_MODE:

	  // check for a right click
	  if ((e.modifiers & Event.META_MASK) == Event.META_MASK)
	    {
	      // start a new distance expression
	      expressionFrame.newDistance();

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // find a point near the mous click
	  newPoint = findPointShape(e.x,e.y);

	  // if none, deselect any points
	  if (newPoint == null)
	    {
	      message("Select first point.");

	      selected.clear();
	      redraw();
	      undo.setCantUndo();
	      break;
	    }

	  // if this is the first point, save it
	  if (selected.size() == 0)
	    {
	      selected.add(newPoint);
	      message("Select second point");
	      redraw();
	      undo.saveSelectStep();
	      break;
	    }

	  undo.setCantUndo();
	  message("Select first point");

	  firstPoint = (PointShape)selected.shapes.elementAt(0);

	  // deselect all
	  selected.clear();
	  redraw();

	  if (newPoint == firstPoint)
	    break;

	  // get a distance measure
	  mlink = firstPoint.getDistanceMeasure(newPoint);

	  /*
	  if (mlink == null)
	    {
	      mlink = new DistanceMeasure(firstPoint,newPoint);
	      mlink.measures.addElement(expressionFrame.editExpr);
	    }
	  else*/

	  // add this expression to the measure
	    if (mlink.measures.indexOf(expressionFrame.editExpr) < 0)
	      mlink.measures.addElement(expressionFrame.editExpr);

	    // add this measure to the expression
	  expressionFrame.editExpr.add(mlink);
	  expressionFrame.editExpr.repaint();
	  break;

	}
      return true;
    }

  /** Determine a unique label for the next point, and add it to
   *  the hashtable
   *
   *  The label will be of the form prefixN, 
   *  where N is an integer >= startNum
   *
   * @param prefix     String to begin the name
   * @param names      Table of used names
   * @param startNum   Minimum start integer
   */

  static String uniqueName(String prefix,Hashtable names,int startNum)
  {
    int i=startNum;

    while (names.get(prefix+i) != null)
      i++;

    names.put(prefix+i,new Boolean(true));

    return prefix+i;
  }

  /** Clear the dragPoint when the mouse is released */

  public boolean mouseUp(Event e,int x,int y)
    {
      dragPoint = null;

      return true;
    }

  /** Handle a mouse drag */
  
  public boolean mouseDrag(Event e,int x,int y)
    {
      // check if we are in drag mode and a point is selected
      if (mode == DRAG_MODE && dragPoint != null)
	{
	  // move the point to its new location
	  dragPoint(transform.screenToVirtualX(x),
		    transform.screenToVirtualY(y),dragPoint);

	  redraw();
	}
      return true;
    }

  // the following functions override the default Component methods
  // in various ways.
  // This guarantees that redraws caused by the system and caused
  // by a call to redraw() do the same thing.
  // update() is overridden to prevent flickering.

  public void update(Graphics g) 
    {
      paint(g);
    }

  /** Redraw the screen */

  void redraw()
  {
    redraw(getGraphics());
  }

  void redraw(Graphics g)
  {
    paint(g);
  }

  Image offscreen = null;    // offscreen buffer
  int imageheight;           // current width of offscreen
  int imagewidth;            // current height of offscreen

  public void paint(Graphics g)
  {
    Dimension d = size();    // get the current canvas dimensions
    transform.resize(d);     // reset the transform matrix

    // are we in double-buffer mode?
    if (doubleBuffer)
      {
	// check if an appropriate offscreen buffer exists
	if (offscreen == null || 
	    imagewidth != d.width || imageheight != d.height)
	  {
	    // create a new offscreen buffer
	    if (offscreen != null)
	      offscreen.flush();
	    offscreen = createImage(d.width,d.height);
	    imagewidth = d.width;
	    imageheight = d.height;
	  }

	Graphics g1 = offscreen.getGraphics();

	// clear the offscreen buffer
	g1.setColor(FIELD_BACKGROUND);
	g1.fillRect(0,0,d.width,d.height);

	// draw the labels, if necessary
	if (showLabels)
	  for(int i=0;i<shapes.size();i++)
	    ((Shape)shapes.elementAt(i)).drawLabel(g1,transform);

	// draw all the shapes
	for(int i=0;i<shapes.size();i++)
	  ((Shape)shapes.elementAt(i)).draw(g1,transform);

	// copy the offscreen buffer to the canvas
	g.drawImage(offscreen,0,0,this);
      }
    else
      {
	// clear the canvas
	g.setColor(FIELD_BACKGROUND);
	g.fillRect(0,0,d.width,d.height);

	// draw the labels, if necessary
	if (showLabels)
	  for(int i=0;i<shapes.size();i++)
	    ((Shape)shapes.elementAt(i)).drawLabel(g,transform);

	// draw the shapes
	for(int i=0;i<shapes.size();i++)
	  ((Shape)shapes.elementAt(i)).draw(g,transform);
      }
  }

  /** Adjust the viewing plane so that all points are visible */

  void recenter()
  {
    boolean points = false;  // are there any points?

    // bounding box around all the points
    double minX = 0;
    double minY = 0;
    double maxX = 0;
    double maxY = 0;
      
    for(int i=0;i<shapes.size();i++)
      {
	if (shapes.elementAt(i) instanceof PointShape)
	  {
	    PointShape ps = (PointShape)shapes.elementAt(i);

	    // update the bounding box
	    if (!points)
	      {
		minX = maxX = ps.x;
		minY = maxY = ps.y;
	      }
	    else
	      {
		if (ps.x < minX)
		  minX = ps.x;
		
		if (ps.x > maxX)
		  maxX = ps.x;

		if (ps.y < minY)
		  minY = ps.y;

		if (ps.y > maxY)
		  maxY = ps.y;
	      }

	    points = true;
	  }
      }

    Dimension d = size();         // get the size of the screen

    transform.screenOriginX = d.width/2;   // compute the origin
    transform.screenOriginY = d.height/2;

    if (!points)
      return;

    // compute the origin in the virtual plane: the center of the bounding box
    transform.virtualOriginX = (int)((minX+maxX)/2);
    transform.virtualOriginY = (int)((minY+maxY)/2);

    // width and height of the bounding box
    double dx = maxX - minX;
    double dy = maxY - minY;

    // compute the scale factor so that the bounding box is 2/3 the size
    // of the screen in the longer dimension

    if (dx != 0 && dy != 0)
      {
	double sf1 = .66*d.width / dx;
	double sf2 = .66*d.height / dy;
	
	transform.scaleFactor = Math.min(sf1,sf2);
      }
    else
      if (dx != 0)
	transform.scaleFactor = .66*d.width/dx;
    else
      if (dy != 0)
	transform.scaleFactor = .66*d.height/dy;
  }

  /** Show a message in the message box */

  void message(String msg)
    {
      messageBox.setText(msg);
    }

  /** Move a point to a new location on the screen 
   *  
   * @param newX   The new virtual x-coordinate of the point
   * @param newY   The new virtual y-coordinate of the point
   * @param p      The input point to drag
   */

  void dragPoint(double newX,double newY,PointShape p)
  {
    // check if there's no change
    if (p.x == newX && p.y == newY)
      return;

    // update the new point
    p.x = newX;
    p.y = newY;

    boolean success = true;

    // recompute all affected shapes
    for(int i=0;i<dragDeps.size();i++)
      {
	Dependency d = (Dependency)dragDeps.elementAt(i);

	d.recompute(shapes);

	if (!d.successful)
	  success = false;
      }

    // update the construction status
    cp.setStatus(success && nonDragAreSuccessful);
  }

  /** List of measureDependencies affected by the dragPoint */

  Vector measures = new Vector();

  /** Recursive helper function for mark()
   *  
   * @param s   A shape to mark, and add to measures
   */ 

  void markHelper(Shape s)
    {
      for(Enumeration e = s.offspring.elements();e.hasMoreElements();)
	{
	  Dependency d = (Dependency)e.nextElement();

	  if (!d.mark)
	    {
	      d.mark = true;

	      if (d instanceof MeasureDependency)
		measures.addElement(d);

	      for(int i=0;i<d.children.length;i++)
		markHelper(d.children[i]);
	    }
	}
    }

  /** are the constructions unaffected by the drag successful? */
  boolean nonDragAreSuccessful;

  /** Prepares to drag the input point
   *
   * 1.  Fills the dragDeps list with all affected dependencies in
   *     topological order.
   * 2.  Sets the nonDragAreSuccessful variable
   */

  void mark(PointShape s)
  {
    // clear the measures and dragDeps list
    measures.removeAllElements();
    dragDeps.removeAllElements();

    // mark the affected dependencies
    markHelper(s);

    nonDragAreSuccessful = true;

    // go through the list of dependencies in order of their creation

    for(int i=0;i<editor.steps.size();i++)
      {
	Object src = editor.steps.elementAt(i);

	if (src instanceof Dependency)
	  {
	    Dependency d = (Dependency)src;

	    // if the dependency is marked, add it to dragDeps
	    // otherwise, check if it's successful

	    if (d.mark)
	      {
		dragDeps.addElement(d);
		d.mark = false;
	      }
	    else
	      if (!d.successful)
		nonDragAreSuccessful = false;
	  }
      }

    // add all the affectd measure dependencies to the end of the list

    for(int i=0;i<measures.size();i++)
      {
	Dependency d = (Dependency)measures.elementAt(i);

	dragDeps.addElement(d);
	d.mark = false;
      }
  }

  /** Turn labels on or off
   *
   * @param sl On or off
   */

  void toggleLabels(boolean sl)
    {
      showLabels = sl;

      redraw();
    }

  /** Bring a shape to the front of the list of shapes */

  void bringToFront(Shape s)
  {
    shapes.removeElement(s);
    shapes.addElement(s);
  }
}

/** A structure to hold all the selected shapes, and update appropriately */

class Selected implements Constants
// all the shapes on the DrawPanel that are selected
{
  /** The list of selected shapes */
  Vector shapes = new Vector();
  
  /** Select a shape */
  void add(Shape s)
  {
    shapes.addElement(s);
    s.color = SELECTED;
  }

  /** Deselect all shapes */

  void clear()
  {
    for(int i=0;i<shapes.size();i++)
      {
	Shape s = (Shape)shapes.elementAt(i);
	s.color = FOREGROUND;
      }

    shapes.removeAllElements();
  }

  /** How many shapes are selected */

  int size() { return shapes.size(); }

  /** Is a specifc shape selected? */

  boolean isSelected(Shape s) { return shapes.indexOf(s) >= 0; }

  /** Are any shapes selected? */

  boolean isEmpty() { return shapes.isEmpty(); }
  
  /** Deselect the last selected shape */

  void removeLast()
  {
    int index = shapes.size() - 1;
    Shape lastShape = (Shape)shapes.elementAt(index);
    shapes.removeElementAt(index);
    lastShape.color = FOREGROUND;
  }
}
