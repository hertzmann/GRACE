/* GRACE - Graphical Ruler and Compass Editor
 *
 * Editor.java
 *
 * This file contains the Editor class for displaying the Text window
 * and recording the construction-in-progress
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.awt.*;
import java.util.*;

/** The text window.  This class also contains the steps of the
 *  current construction, to be used when saving and naming the 
 * construction. */

public class Editor extends Frame implements Constants
{
  /** The name of the current construction */
  String currentName = "New Construction";

  /** The display of inputs */
  List inputsList;

  /** The display of intermediate steps */
  List stepsList;

  /** The display of outputs */
  List outputList;

  /** The number of inputs */
  int inputCounter = 0;

  // other display areas
  DrawPanel drawPanel;
  ConstructionPanel cp;
  ConstraintFrame cf;

  /** A list of input points */
  Vector inputs = new Vector();

  /** A list of the intermediate dependencies and forced constraints */
  Vector steps = new Vector();
  
  /** The parent dependency of each output */
  Vector outputParents = new Vector();

  /** Which child number each output is of it's parent */
  Vector outputChildren = new Vector();

  /** Each item in the inputs and steps list.  For decoding selections */
  Hashtable inputsNsteps = new Hashtable();  

  /** Text of the "Close" button - expanded to widen the window */
  static final String closeText = "            Close            ";

  /** A list of all the ViewFrames currently visible */
  Vector ViewFrames = new Vector();

  /** Add a point to the list of inputs */
  void addInput(PointShape p)
  {
    inputsList.addItem(p.label,inputCounter);
    inputs.addElement(p);
    inputsNsteps.put(p.label,p.source);
    inputCounter ++;
  }

  /** Remove the last point from the list of inputs */
  void removeLastInput()
  {
    Object p = inputs.lastElement();

    inputCounter --;
    inputsList.delItem(inputCounter);
    inputs.removeElement(p);
    inputsNsteps.remove(((PointShape)p).label);
  }

  /** Add a dependency to the list of intermediate steps */
  void addStep(Dependency d)
  {
    String name = stepsList.countItems()+": "+d.toString();

    steps.addElement(d);
    stepsList.addItem(name);
    inputsNsteps.put(name,d);
  }

  /** Remove the last dependency from the list of intermediate steps */
  void removeLastStep()
  {
    Object d = steps.lastElement();

    steps.removeElement(d);
    stepsList.delItem(stepsList.countItems()-1);
    inputsNsteps.remove(stepsList.countItems()+": "+d.toString());
  }
  
  /** Add a shape to the list of outputs
   *
   * @return True if the output was added, false if it's already an output
   */
  boolean addOutput(Shape s)
  {
    // determine which child this is of it's parent
    int j=0;
    while(s != s.source.children[j])
      j++;

    // check if this shape is already an output
    for(int i=0;i<outputParents.size();i++)
      if (outputParents.elementAt(i) == s.source &&
	  ((Integer)outputChildren.elementAt(i)).intValue() == j)
	return false;

    // add it to the outputs
    
    outputList.addItem(s.label,outputParents.size());

    outputParents.addElement(s.source);
    outputChildren.addElement(new Integer(j));

    return true;
  }

  /** Remove the last output */

  void removeLastOutput()
  {
    outputList.delItem(outputList.countItems()-1);
    int i = outputParents.size()-1;
    outputParents.removeElementAt(i);
    outputChildren.removeElementAt(i);
  }

  /** Add a forced constraint to the list of intermediate steps */

  void addForced(Constraint c)
  {
    steps.addElement(c);
  }

  /** 
   *  Clear the current construction, and load in a new construction
   *  
   *  @param  c  The new construction to view
   */

  void view(Construction c)
  {
    Constraint cs;            // a temp Constraint
    ConstraintRule cr;        // a temp ConstraintRule

    drawPanel.erase();        // clear the draw panel and delete all shapes
    clear();                  // clear the text frame and all steps
    drawPanel.expressionFrame.clear();  // clear the list of expressions
    cf.clear();               // clear the current constraints

    currentName = c.name;     // get the name of the current construction

    int i;                    // construction step index
    int badStep = -1;         // index of the first failed step
    String errMsg = null;     // exception message, if any

    boolean successful = true;  // was the construction successful?

    // Check that each input point has default coordinates
    for(i=0;i<c.numberOfInputs;i++)
      {
	Rule r = (Rule)c.rules.elementAt(i);

	if (!r.hasDefaults)
	  {
	    message("Input "+r.childName[0]+
		    " does not have default coordinates");
	    return;
	  }
      }

    Shape steps[][] = new Shape[c.rules.size()][];

    // add all the input points
    for(i=0;i<c.numberOfInputs;i++)
      {
	Rule r = (Rule)c.rules.elementAt(i);
	PointShape ps = new PointShape(r.defaultX,r.defaultY);
	ArbitraryDependency ad = new ArbitraryDependency();

	ps.free = true;
	ps.label = r.childName[0];
	drawPanel.names.put(ps.label,ps);
	ps.source = ad;

	drawPanel.shapes.addElement(ps);
    
	addInput(ps);

	steps[i] = new Shape[1];
	steps[i][0] = ps;

	ad.parents = new Shape[0];
	ad.children = new Shape[1];
	ad.children[0] = ps;
      }
    
    // create all the input constraints
    for(i=0;i<c.inputConstraints.size();i++)
      {
	cr = (ConstraintRule)c.inputConstraints.elementAt(i);

	cs = cr.create(steps);

	cs.name = cs.toString();

	cf.addInput(cs);
      }

    // create all the intermediate steps
    for(;i<c.rules.size()-1;i++)
      {
	Rule r = (Rule)c.rules.elementAt(i);   // the next rule
	Shape[] parents = new Shape[r.parents.length];  // this step's parents
	boolean valid = true;                  // is this step successful?

	// temp variables:
	Dependency d;                          // a new dependency
	LineShape newLine;                     // a new line
	Circle newCircle;                      // a new Circle
	Shape[] result;

	// get each of the parents
	for(int j=0;j<parents.length;j++)
	  {
	    parents[j] = steps[r.parents[j].stepNumber][r.childNumber[j]];

	    if (!parents[j].valid)
	      valid = false;
	  }
	
	switch(r.type)
	  {
	  case FORCE:
	    // force a constraint
	    cs = r.force.create(steps);
	    cs.name = cs.toString();
	    addForced(cs);
	    cf.addBlankStep();
	    cf.addStep(cs,"F: ");
	    continue;

	  case CONSTRUCTION:
	    d = new ConstructionDependency();

	    try
	      {
		// attempt the construction
		result = r.construction.apply(parents,cf,0,r.childName,
					      drawPanel.names,
					      stepsList.countItems());
	      }
	    catch (ConstructionError ce)
	      {
		// the construction failed
		result = new Shape[0];
		if (successful)
		  {
		    successful = false;
		    badStep = i-c.numberOfInputs;
		    errMsg = ce.getMessage();
		  }
	      }

	    ((ConstructionDependency)d).construction = r.construction;
	    break;

	  case INTERSECTION:
	    d = new IntersectionDependency();
	    result = Geometry.Intersection(parents[0],parents[1],
					   cf,0,r.childName,drawPanel.names,
					   stepsList.countItems()+"A: ");
	    break;

	  case PERP_BI:
	    d = new PerpBiDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }

	    newLine = new PerpBi((PointShape)parents[0],
				 (PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newLine;
	    break;

	  case LINE_SEGMENT:
	    d = new LineSegmentDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }

	    newLine = new LineSegment((PointShape)parents[0],
				      (PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newLine;
	    newLine.makePO((PointShape)parents[0],
			   (PointShape)parents[1]);
	    break;

	  case LINE:
	    d = new LineDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }
	    newLine = new Line((PointShape)parents[0],
			       (PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newLine;
	    break;

	  case RAY:
	    d = new RayDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }

	    newLine = new Ray((PointShape)parents[0],
				(PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newLine;
	    newLine.makePO((PointShape)parents[0],(PointShape)parents[1]);
	    break;

	  case COMPL_RAY:
	    d = new ComplRayDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }

	    newLine = new ComplRay((PointShape)parents[0],
				     (PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newLine;
	    newLine.makePO((PointShape)parents[0],(PointShape)parents[1]);
	    break;

	  case CIRCLE:
	    d = new CircleDependency();
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      {
		result = new Shape[0];
		break;
	      }

	    newCircle = new Circle((PointShape)parents[0],
				   (PointShape)parents[1]);

	    result = new Shape[1];
	    result[0] = newCircle;

	    break;

	  default:
	    continue;
	  }

	// link the new dependency

	d.parents = parents;

	for(int j=0;j<parents.length;j++)
	  d.parents[j].offspring.addElement(d);

	// copy the children to the dependency's list of children

	Shape[] children = new Shape[r.childName.length];
	
	for(int j=0;j<children.length;j++)
	  {
	    // make sure the right number of outputs were returned
	    if (j<result.length)
	      {
		children[j] = result[j];
		children[j].valid = valid;
	      }
	    else
	      {
		children[j] = new PointShape(0,0);
		children[j].valid = false;
	      }

	    if (!children[j].valid && successful)
	      {
		successful = false;
		badStep = i-c.numberOfInputs;
	      }

	    children[j].source = d;
	    children[j].label = r.childName[j];

	    drawPanel.shapes.addElement(children[j]);
	    drawPanel.names.put(children[j].label,children[j]);
	  }

	if (!valid && successful)
	  {
	    badStep = i-c.numberOfInputs;
	    successful = false;
	  }

	d.children = children;
	steps[i] = children;
	addStep(d);         // add this step to the Text window
      }

    Rule outputRule = (Rule)c.rules.lastElement();

    // create the final rule
    for(i=0;i<outputRule.parents.length;i++)
      addOutput(steps[outputRule.parents[i].stepNumber]
		[outputRule.childNumber[i]]);

    // create the output constraints
    for(i=0;i<c.outputConstraints.size();i++)
      {
	cr = (ConstraintRule)c.outputConstraints.elementAt(i);

	cs = cr.create(steps);

	cs.name = cs.toString();

	cf.addOutput(cs);
      }

    // redraw the screen
    drawPanel.recenter();
    drawPanel.redraw();

    message("Viewing \""+c.name+'\"');

    // display the construction status
    cp.setStatus(successful);
  }

  /** Handle a button press */
 
  public boolean action(Event e,Object what)
  {
    if (e.target instanceof Button)
      {
	String choice  = (String)e.arg;

	if (choice.equals("Pure Text"))
	  {
	    ViewFrames.addElement(new ViewFrame("Pure Text: "+currentName,
						toString()));
	    
	    return true;
	  }

	if (choice.equals(closeText))
	  {
	    hide();
	    return true;
	  }
      }

    return false;
  }
  
  /** Handle a list selection event */

public boolean handleEvent(Event e)
  {
    // check if the event target is the input or steps list
    if (e.target != stepsList && e.target != inputsList)
      return super.handleEvent(e);   // no; maybe another type of event

    switch (e.id)
      {
      case Event.LIST_SELECT:
	// get which list was hit
	List target = (List)e.target;

	// get the index of the selected item
	int index = ((Integer)e.arg).intValue();

	// deselect that item
	target.deselect(index);

	// get the dependency for the selected step
	Dependency d = (Dependency)inputsNsteps.get(target.getItem(index));

	// check if one exists
	if (d == null)
	  return true;

	// bring the children of that construction to the front
	for(int i=0;i<d.children.length;i++)
	  drawPanel.bringToFront(d.children[i]);

	// redraw the shapes
	drawPanel.redraw();

	return true;
      default:
	return super.handleEvent(e); // handle some other kind of event
      }
  }

  /** clear out the current construction */
  void clear()
  {
    inputsList.clear();
    stepsList.clear();
    outputList.clear();

    inputsNsteps.clear();

    inputCounter = 0;
    inputs.removeAllElements();
    steps.removeAllElements();
    outputParents.removeAllElements();
    outputChildren.removeAllElements();

    currentName = "New Construction";
  }

  /** Close all of the ViewFrames */

  void closeFrames()
  {
    for(int i=0;i<ViewFrames.size();i++)
      {
	ViewFrame tf = (ViewFrame)ViewFrames.elementAt(i);

	tf.hide();
	tf.dispose();
      }
  }

  /** Generate a textual representation of the current construction */

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("Construction \""+currentName+"\"\n\nInput:\n");

    // generate a list of inputs

    for(int i=0;i<inputs.size();i++)
      {
	PointShape ps = (PointShape)inputs.elementAt(i);

	sb.append(ps.label);

	sb.append(" ("+ps.x+','+ps.y+')');

	sb.append("\n");
      }

    // list the input constraints

    for(int i=0;i<cf.inputConstraints.size();i++)
      sb.append("Assume "+((Constraint)cf.inputConstraints.elementAt(i)).
		toString()+'\n');
					       
    sb.append("\nSteps:\n");

    // copy the list of intermediate steps from the window

    for(int i=0;i<steps.size();i++)
      {	
	Object step = steps.elementAt(i);

	if (step instanceof Constraint)
	  sb.append("Force "+step.toString()+'\n');
	else
	  sb.append(step.toString()+'\n');
      }

    sb.append("\nOutput:\n");

    // copy the list of outputs from the window

    for(int i=0;i<outputList.countItems();i++)
      sb.append(outputList.getItem(i)+'\n');

    // list the output constraints

    for(int i=0;i<cf.outputConstraints.size();i++)
      sb.append("Conclude "+((Constraint)cf.outputConstraints.elementAt(i)).
		toString()+'\n');

    return new String(sb);
  }

  /** Save the current construction into the construction panel
   *
   *  @param name The name for the current construction
   */

  void define(String name)
  {
    // check if the name is valid
    if (cp.isConstruction(name) || cp.isPrimitive(name))
      {
	message("That name is already taken");
	return;
      }

    Construction c = new Construction();
    c.numberOfInputs = inputs.size();
    c.name = name;

    currentName = name;

    int stepNumber;

    // create the input rules

    for(stepNumber = 0;stepNumber < inputs.size();stepNumber ++)
      {
	PointShape p = (PointShape)inputs.elementAt(stepNumber);
	Rule newRule = new Rule(p.source);

	p.source.editorCopy = newRule;

	newRule.parents = new Rule[0];
	newRule.childNumber = new int[0];
	newRule.stepNumber = stepNumber;

	newRule.hasDefaults = true;
	newRule.defaultX = p.x;
	newRule.defaultY = p.y;

	c.rules.addElement(newRule);
      }

    for(;stepNumber-c.numberOfInputs< steps.size();stepNumber ++)
      {
	Object step = steps.elementAt(stepNumber-c.numberOfInputs);

	// create a forced constraint rule
	if (step instanceof Constraint)
	  {
	    Rule newRule = new Rule();
	    newRule.type = FORCE;
	    newRule.parents = new Rule[0];
	    newRule.childNumber = new int[0];
	    newRule.stepNumber = stepNumber;
	    newRule.childName = new String[0];
	    newRule.force = ((Constraint)step).makeRule();

	    c.rules.addElement(newRule);

	    continue;
	  }

	// create a new intermediate step rule

	Dependency d = (Dependency)step;
	Rule newRule = new Rule(d);

	// keep a temporary pointer to the rule in the corresponding dependency
	d.editorCopy = newRule;

	// fill the fields in the new rule
	newRule.stepNumber = stepNumber;
	newRule.parents = new Rule[d.parents.length];
	newRule.childNumber = new int[d.parents.length];

	// point to each of the rule's parents
	for(int i=0;i<d.parents.length;i++)
	  {
	    Dependency thisParent = d.parents[i].source;

	    newRule.parents[i] = thisParent.editorCopy;
	      
	    int j = 0;
	    while(thisParent.children[j] != d.parents[i])
	      j++;

	    newRule.childNumber[i] = j;
	  }

	c.rules.addElement(newRule);
      }

    // create the output rule
    
    Rule outputRule = new Rule();
    outputRule.type = OUTPUT;
    outputRule.parents = new Rule[outputParents.size()];
    outputRule.childNumber = new int[outputParents.size()];
    outputRule.stepNumber = stepNumber;
    
    for(int i=0;i<outputParents.size();i++)
      {
	Dependency output = (Dependency)outputParents.elementAt(i);

	outputRule.parents[i] = output.editorCopy;
	outputRule.childNumber[i] = ((Integer)outputChildren.elementAt(i)).
	  intValue();
      }

    c.rules.addElement(outputRule);

    // create input constraints

    for(int i=0;i<cf.inputConstraints.size();i++)
      {
	Constraint cs = (Constraint)cf.inputConstraints.elementAt(i);
	c.inputConstraints.addElement(cs.makeRule());
      }
    
    // create output constraints

    for(int i=0;i<cf.outputConstraints.size();i++)
      {
	Constraint cs = (Constraint)cf.outputConstraints.elementAt(i);
	c.outputConstraints.addElement(cs.makeRule());
      }

    // null the editor copy pointers (aids garbage collection)

    for(int i=0;i<inputs.size();i++)
      ((Shape)inputs.elementAt(i)).source.editorCopy = null;

    for(int i=0;i<steps.size();i++)
      {
	Object step = steps.elementAt(i);
	if (step instanceof Dependency)
	  ((Dependency)step).editorCopy = null;
      }

    cp.add(c,c.name);

    message("Defined "+c.name);
  }

  /** Create the text window (but don't show it yet) */

  Editor(ConstructionPanel c)
  {
    super("Text");

    // This layout is kinda one big hack.

    // Lists have some bad habits that this works around.

    // Specifically, they tend to overflow into whatever component
    // is below the list after a few items are added to it.  Adding
    // the other component first the Container, then the list, seems
    // to solve the problem.  Using panels also helps.

    // Also, you can't specify the width of a list, so the window has
    // to be widened by putting two wide buttons next to each other.

    setLayout(new BorderLayout());

    Panel p = new Panel();
    p.add(new Button("Pure Text"));
    p.add(new Button(closeText));
    add("South",p);

    Panel p1 = new Panel();

    p1.setLayout(new BorderLayout());

    p1.add("North",new Label("Input shapes"));
    
    inputsList = new List(5,false);
    p1.add("Center",inputsList);

    add("North",p1);

    p1 = new Panel();
    p1.setLayout(new BorderLayout());
    p1.add("North",new Label("Intermediate steps"));

    Panel p2 = new Panel();
    p2.setLayout(new BorderLayout());
    p2.add("North",new Label("Output shapes"));
    
    outputList = new List(5,false);
    p2.add("Center",outputList);
    p1.add("South",p2);
    
    stepsList = new List(10,false);
    p1.add("Center",stepsList);

    add("Center",p1);

    pack();
    
    cp = c;
  }

  /** Display a text message in the main window */

  void message(String s)
  {
    drawPanel.message(s);
  }
}

/** A prompt for the name of a construction, when saving it
 *
 *  This really should be a modal Dialog, but Netscape doesn't
 *  support Dialogs.
 */

class NameFrame extends Frame
{
  /** The field where the new name is entered */
  TextField inputTF;

  /** A pointer to the text frame */
  Editor editor;

  /** Create the window */
  public NameFrame(Editor e)
  {
    super("Construction name");
    
    editor = e;

    setLayout(new BorderLayout());

    add("North",new Label("Enter a name for the new construction"));

    inputTF = new TextField(15);

    add("Center",inputTF);

    Panel buttons = new Panel();

    buttons.add(new Button("OK"));
    buttons.add(new Button("Cancel"));

    add("South",buttons);

    pack();
    show();
  }

  /** Handle the result */
  public boolean action(Event e, Object arg)
  {
    if (e.target instanceof TextField)
      {
	// the user hit "Return" in the text field

	editor.define(inputTF.getText());
	hide();
	dispose();
	return true;
      }

    if (e.target instanceof Button)
      {
	String choice = (String)e.arg;

	if (choice.equals("OK"))
	  editor.define(inputTF.getText());

	hide();
	dispose();

	return true;
      }

    return false;
  }
}

/** A frame used to display the pure text of a construction. */

class ViewFrame extends Frame
{
  /** The close button */
  Button close;

  /** Create the window */

  ViewFrame(String title,String text)
    {
      super(title);
      TextArea ta = new TextArea(text,20,30);
      ta.setEditable(false);
      this.add("Center",ta);

      close = new Button("Close");
      this.add("South",close);
      this.pack();
      this.show();
    }

  /** Handle a Close button press */

  public boolean action(Event e,Object what)
    {
      if (e.target == close)
	{
	  this.hide();
	  this.dispose();
	  return true;
	}
      return false;
    }
}
