/* GRACE - Graphical Ruler and Compass Editor
 *
 * Expression.java
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 * Building blocks for constraints
 *
 */

import java.awt.*;
import java.util.*;

/** An expression of coefficients and variables.  The coefficients must
 *  be positive integers.  The user builds expressions for building 
 *  constraints */

abstract class Expression implements Cloneable, Constants
{
  /** The expression display where this is being viewed, if any */
  ExpressionDisplay display = null;

  /** The list of variables */
  Vector sources;

  /** The coefficients of each variable (could be combined into a hashtable) */
  Vector weights;

  /** Sum of the weights */
  int numTerms;

  /** Is this expression currently selected? */
  boolean selected;

  /** Are all the sources inputs? */
  boolean input = true;

  Expression()
  {
    sources = new Vector();
    weights = new Vector();
    numTerms = 0;
  }

public Object clone()
  {
    Expression e;

    if (this instanceof AngleExpression)
      e = new AngleExpression();
    else
      e = new DistanceExpression();

    e.sources = (Vector)sources.clone();
    e.weights = (Vector)weights.clone();
    e.numTerms = numTerms;
    e.input = input;

    return e;
  }

  /** Draw a graphical representation of this expression */

  abstract void paint(Graphics g,int width,int height);

  /** Add a variable to the expression, with the weight 1 */
  
  void add(MeasureDependency md)
  {
    numTerms ++;

    int index = sources.indexOf(md);
      
    // is this variable already in the expression?
    if (index < 0)
      {
	// no; add it
	sources.addElement(md);
	weights.addElement(new Integer(1));
      }
    else
      {
	// yes; increment the weight
	Integer i = (Integer)weights.elementAt(index);
	weights.setElementAt(new Integer(i.intValue()+1),index);
      }

    // update input
    if (!md.isInput())
      input = false;

    // change the textual representation
    if (display != null)
      display.topTextField.setText(toString());

    repaint();
  }

  /** Mark this expression as selected and draw a box if necessary */

  void select()
  {
    selected = true;
    if (display != null)
      display.select();
  }

  /** Mark this expression as deselected and redraw if necessary */

  void deselect()
  {
    selected = false;
    if (display != null)
      display.deselect();
  }

  /** Redraw the display if necessary */
     
  void repaint()
  {
    if (display != null)
      display.repaint();
  }

  /** Generate a textual representation of this expression */

public String toString()
  {
    if (sources.size() == 0)
      return "0";

    StringBuffer sb = new StringBuffer();
    for(int i=0;i<sources.size();i++)
      {
	int weight = ((Integer)weights.elementAt(i)).intValue();

	if (i>0)
	  sb.append("+");

	if (weight != 1)
	  sb.append(weight+"*");
	sb.append(sources.elementAt(i).toString());
      }

    return new String(sb);
  }
}

/** An expression that represents the sum of angles measurements */

class AngleExpression extends Expression
{
  /** Draw a pie wedge-display of the angle */

  void paint(Graphics g,int width,int height)
  {
    // values of each variable
    int measures[] = new int[numTerms];

    // current angle from 0 (0 points to the right side of the screen)
    int angle = 0;
    
    // radius of the pie
    int radius = Math.min(width,height)/2 - MEASURE_MARGIN;

    // height and width of the pie
    int x = width/2-radius;
    int y = height/2-radius;

    // draw a line from the center to the right side of the pie
    g.drawLine(width/2,height/2,width/2+radius,height/2);

    // total angle so far
    int angleSum = 0;

    // index of the first line from center to edge that won't get cut
    int firstFullLine = 0;

    // do some precomputation

    int j=0;
    for(int i=0;i<sources.size();i++)
      {
	int weight = ((Integer)weights.elementAt(i)).intValue();
	int measure = ((MeasureDependency)sources.elementAt(i)).measure;

	for(int k=0;k<weight;k++)
	  {
	    measures[j] = measure;
	    angleSum += measures[j];

	    if (angleSum >= 360)
	      {
		angleSum -= 360;
		firstFullLine = j;
	      }

	    j++;
	  }
      }

    while (angleSum >= 360 && firstFullLine < numTerms)
      angleSum -= measures[firstFullLine ++];

    // go through and draw the pie

    for(int i=0;i<numTerms;i++)
      {
	int ang = measures[i];
	  
	while (angle+ang > 360 && radius > 0)
	  {
	    g.drawArc(x,y,radius*2,radius*2,angle,360-angle);

	    ang -= (360-angle);
	    angle = 0;

	    radius -= 4;
	    x += 4;
	    y += 4;
	  }

	g.drawArc(x,y,radius*2,radius*2,angle,ang);
    
	angle += ang;

	tick(g,width/2,height/2,radius,angle,i>=firstFullLine);
      }    
  }

  /** Helper function to draw a tick mark on the pie */

  void tick(Graphics g,int x,int y,int radius,int angle,boolean drawRadius)
  {
    double ang = angle * Math.PI / 180;

    if (drawRadius)
      {
	int xloc = x + (int)(radius * Math.cos(ang));
	int yloc = y - (int)(radius * Math.sin(ang));

	g.drawLine(x,y,xloc,yloc);
      }
    else
      {
	int xloc = x + (int)((radius-2) * Math.cos(ang));
	int yloc = y - (int)((radius-2) * Math.sin(ang));

	int xloc2 = x + (int)((radius+2) * Math.cos(ang));
	int yloc2 = y - (int)((radius+2) * Math.sin(ang));

	g.drawLine(xloc,yloc,xloc2,yloc2);
      }
  }
}

/** An expression for representing the sum of  distances */

class DistanceExpression extends Expression
{
  /** Draw a line-representation of the expression */
  
  void paint(Graphics g,int width,int height)
  {
    g.setColor(FOREGROUND);

    int left = MEASURE_MARGIN;
    int top = MEASURE_MARGIN;
    int right = width-MEASURE_MARGIN*2;

    int x = left;
    int y = top+2;

    for(int i=0;i<sources.size();i++)
      {
	int weight = ((Integer)weights.elementAt(i)).intValue();

	for(int j=0;j<weight;j++)
	  {
	    int length = ((MeasureDependency)sources.elementAt(i)).measure;
	      
	    g.drawLine(x,y-2,x,y+2);

	    while(length+x > right)
	      {
		g.drawLine(x,y,right,y);
		length -= (right-x);
		x = left;
		y += 6;
	      }

	    g.drawLine(x,y,x+length,y); 
	  
	    x += length;
	  }
      }

    g.drawLine(x,y-2,x,y+2);
  }
}

/** A panel for displaying a graphical and textual representation of an
 *  expression */

class ExpressionDisplay extends Panel implements Constants
{
  /** The current expression displayed */
  Expression expression;

  /** The expression frame that holds the display */
  ExpressionFrame parent;

  /** The text field that displays the textual representation */
  TextField topTextField;

  /** The canvas that displays the graphical representation */
  ExpressionCanvas display;

  /** Create the ExpressionDisplay */

  ExpressionDisplay(ExpressionFrame p,int w,int h)
  {
    setBackground(FIELD_BACKGROUND);
      
    expression = null;
    display = new ExpressionCanvas();
    display.parent = this;
    parent = p;
      
    setLayout(new BorderLayout());
    add("Center",display);
    display.resize(w,h);
      
    topTextField = new TextField();
    topTextField.setEditable(false);
    add("North",topTextField);
  }

  /** Add a variable to the current expression */

  void add(MeasureDependency d)
  {
    if (expression != null)
      expression.add(d);
  }
  
  /** Display a new expression
   *
   * @param newExpr  The new expression to display
   */

  void set(Expression newExpr)
  {
    if(expression != null)
      expression.display = null;
    expression = newExpr;
    expression.display = this;
    topTextField.setText(newExpr.toString());
    repaint();
  }

  /** Clear the display, and don't display any expression */

  void clear()
  {
    if (expression != null)
      expression.display = null;
    expression = null;
    topTextField.setText("");
    repaint();
  }

  /** Draw the current expression as selected */

  void select()
  {
    Graphics g = display.getGraphics();
      
    g.setColor(SELECTED);
    g.drawRect(1,1,display.bounds().width-2,display.bounds().height-2);
  }

  /** Draw the current expression as selected */

  void deselect()
  {
    Graphics g = display.getGraphics();
    g.setColor(FIELD_BACKGROUND);
    g.drawRect(1,1,display.bounds().width-2,display.bounds().height-2);
  }

  /** Handle a mouse click to de/select the expression */

public boolean mouseDown(Event e,int x,int y)
  {
    if (expression != null && expression != parent.editExpr)
      {
	if (expression.selected)
	  {
	    parent.handleDeselect(expression);
	  }
	else
	  {
	    parent.handleSelect(expression);
	  }
      }

    return true;
  }

public void repaint()
  {
    super.repaint();
    display.repaint();
  }
}

/** A canvas for displaying an expression */

class ExpressionCanvas extends Canvas
{
  ExpressionDisplay parent;

public void paint(Graphics g)
  {
    Rectangle b = bounds();

    g.clearRect(0,0,b.width,b.height);
    if (parent.expression != null)
      {
	if (parent.expression.selected)
	  parent.select();
	parent.expression.paint(g,b.width,b.height);
      }
  }
}

/** The frame for displaying the expressions.  There is a column of
 *  angle expressions and a column of distance expressions, and a
 *  scrollbar for each. */

class ExpressionFrame extends Frame implements Constants
{
  /** Array of the angle expression displays */
  ExpressionDisplay[] angleDisplay;

  /** Array of the distance expression displays */
  ExpressionDisplay[] distDisplay;

  /** The expression currently being edited, if any */
  Expression editExpr = null;

  /** List of all the currently existing angle expressions */
  Vector angleExpressions = new Vector();

  /** List of all the currently existing distance expressions */
  Vector distanceExpressions = new Vector();

  /** The currently selected expression */
  Expression selected = null;

  /** The number of angle expressions to display at once */
  static final int angleHeight = 3;

  /** The numbe of distance expressions to display at once.  Must be
   *  twice distHeight */
  static final int distHeight = angleHeight * 2;

  //  int distanceIndex = -2;
  //  int angleIndex = -1;

  /** The scrollbar for distance expressions */
  Scrollbar distScroll;

  /** The scrollbar for angle expressions */
  Scrollbar angleScroll;

  /** The index of the first angle expression being displayed */
  int angleTop = 0;
  
  /** The index of the first distance expression being displayed */
  int distTop = 0;

  /** The main drawing panel */
  DrawPanel drawPanel;

  /** The frame where constraints are displayed */
  ConstraintFrame constraintFrame;

  /** The text frame */
  Editor editor;

  /** The undo structure */
  Undo undo;

  /** Create the frame and initialize the expression displays */

  ExpressionFrame(DrawPanel dp,ConstraintFrame cf,Editor e,Undo u)
  {
    super("Expressions");
    drawPanel = dp;
    editor = e;
    undo = u;

    setLayout(new BorderLayout());

    Panel controls = new Panel();
    controls.setLayout(new BorderLayout());

    Panel frameControls = new Panel();
    frameControls.add(new Button("New Distance"));
    frameControls.add(new Button("New Angle"));
    frameControls.add(new Button("Add Pi"));
    frameControls.add(new Button("Edit"));
    frameControls.add(new Button("Delete"));
    controls.add("Center",frameControls);
    controls.add("South",new Button("Close"));
    add("South",controls);

    Panel measures = new Panel();
    GridBagLayout gb= new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    measures.setLayout(gb);

    distScroll = new Scrollbar(Scrollbar.VERTICAL,0,distHeight,0,0);

    gc.gridx = 0;
    gc.gridy = 0;
    gc.gridheight = 2*distHeight;
    gc.fill = gc.VERTICAL;
//    gc.weightx = 1;
//    gc.weighty = 1;
    gb.setConstraints(distScroll,gc);
    measures.add(distScroll);

    int i;
    distDisplay = new ExpressionDisplay[distHeight];
    for(i=0;i<distHeight;i++)
      {
	distDisplay[i] = new ExpressionDisplay(this,
					       MEASURE_WIDTH,MEASURE_HEIGHT);
	gc.fill = gc.NONE;
	gc.gridheight = 1;
	gc.gridx = 1;
	gc.gridy = i;
	gb.setConstraints(distDisplay[i],gc);
	measures.add(distDisplay[i]);
      }

    angleScroll = new Scrollbar(Scrollbar.VERTICAL,0,angleHeight,0,0);

    gc.gridx = 2;
    gc.gridy = 0;
    gc.gridheight = 2*distHeight;
    gc.fill = gc.VERTICAL;
    gb.setConstraints(angleScroll,gc);
    measures.add(angleScroll);

    angleDisplay = new ExpressionDisplay[angleHeight];
    for(i=0;i<angleHeight;i++)
      {
	angleDisplay[i] = new ExpressionDisplay(this,
					       MEASURE_WIDTH,2*MEASURE_HEIGHT);
	gc.fill = gc.VERTICAL;
	gc.gridheight = 2;
	gc.gridx = 3;
	gc.gridy = 2*i;
	gb.setConstraints(angleDisplay[i],gc);
	measures.add(angleDisplay[i]);
      }


    add("Center",measures);
    pack();

    constraintFrame = cf;
  }

  /** Delete all the expressions */

  void clear()
  {
    for(int i=0;i<angleDisplay.length;i++)
      angleDisplay[i].clear();
    for(int i=0;i<distDisplay.length;i++)
      distDisplay[i].clear();
    angleExpressions.removeAllElements();
    distanceExpressions.removeAllElements();
    selected = null;
    editExpr = null;
    angleScroll.setValues(0,angleHeight,0,0);
    distScroll.setValues(0,distHeight,0,0);
    angleTop = 0;
    distTop = 0;
  }

  /** Delete a given expression */

  void delete(Expression e)
  {
    boolean angle = e instanceof AngleExpression;
    Vector expressions = (angle ? angleExpressions : distanceExpressions);
    Scrollbar sb = (angle ? angleScroll : distScroll);
    int height = (angle ? angleHeight : distHeight);
    ExpressionDisplay[] display = (angle ? angleDisplay : distDisplay);

    // remove the expression from the list
    expressions.removeElement(e);

    int scrollTop = sb.getValue();

    // adjust the scrollTop, if necessary
    if (scrollTop >= expressions.size() && scrollTop > 0)
      scrollTop = (angle ? --angleTop : --distTop);

    // adjust the scrollbar
    sb.setValues(scrollTop,height,0,expressions.size());

    // set all the expression displays
    for(int i=0;i<height;i++)
      if (scrollTop + i < expressions.size())
	display[i].set((Expression)expressions.elementAt(scrollTop+i));
      else
	display[i].clear();
  }

  /** Handle an expression selection operation */

  void handleSelect(Expression e)
  {
    if (drawPanel.mode == DELETE_EXPRESSIONS_MODE)
      {
	// delete the expression
	delete(e);
	return;
      }

    if (drawPanel.mode == LABEL_MODE)
      {
	// edit the expression

	if (e instanceof AngleExpression)
	  drawPanel.setDrawMode(LABEL_ANGLE_MODE);
	else
	  drawPanel.setDrawMode(LABEL_DISTANCE_MODE);

	editExpr = e;

	selected = e;
	e.select();

	return;
      }

    if (drawPanel.mode == ASSUME_CONSTRAINT_MODE ||
	drawPanel.mode == TEST_CONSTRAINT_MODE ||
	drawPanel.mode == FORCE_CONSTRAINT_MODE ||
	drawPanel.mode == CONCLUDE_MODE)
      {
	// generate a constraint from two expressions
	
	if (selected == null)
	  {
	    selected = e;
	    e.select();

	    return;
	  }

	Expression e1 = selected;

	deselectAll();

	if ((e1 instanceof AngleExpression && 
	     e instanceof DistanceExpression) ||
	    (e instanceof AngleExpression && 
	     e1 instanceof DistanceExpression))
	  {
	    message("Expressions of different type");
	    return;
	  }

	if (drawPanel.mode == ASSUME_CONSTRAINT_MODE &&
	    (!e.input || !e1.input))
	  {
	    message("Cannot make assumption on dependent points");
	    return;
	  }

	Constraint c = new Constraint(e1,e);

	if (DEBUG)
	  {
	    System.out.println("New constraint = "+c.toString());
	  }

	if (c.isInvalid())
	  {
	    message("Constraint sets PI = 0");
	    return;
	  }

	if (c.isTautology())
	  {
	    message("Constraint is a tautology");
	    return;
	  }

	if (drawPanel.mode == CONCLUDE_MODE)
	  {
	    // create an output constraint

	    if (!c.validOutput(editor.outputParents,editor.outputChildren))
	      {
		message("Cannot make output constraint on dependent points");
		return;
	      }

	    undo.saveState();
	    undo.saveStep(Undo.CONCLUDE);

	    message("Output added");
	    constraintFrame.addOutput(c);
	    return;
	  }

	if (drawPanel.mode == ASSUME_CONSTRAINT_MODE)
	  {
	    // create an input constraint

	    undo.saveState();
	    undo.saveStep(Undo.ASSUME);

	    if (constraintFrame.addInput(c))
	      message("New constraint follows from existing constraints");
	    else
	      message("New assumption added");

	    return;
	  }

	if (drawPanel.mode == TEST_CONSTRAINT_MODE)
	  {
	    // test if a constraint follows from the nullspace

	    if (DEBUG)
	      System.out.println("Nullspace = "+constraintFrame.nullspace);
	      
	    if(constraintFrame.nullspace.follows(c))
	      {
		message("New constraint proven");
		constraintFrame.addBlankStep();
		constraintFrame.addStep(c,"T: ");
		undo.saveStep(Undo.TEST);
	      }
	    else
	      message("Test constraint does not directly follow from existing constraints"); 

	    return;
	  }

	// force a constraint

	message("New constraint forced");
	undo.saveState();
	editor.addForced(c);
	constraintFrame.addBlankStep();
	constraintFrame.addStep(c,"F: ");
	constraintFrame.addProvenConstraint(c);
	undo.saveStep(Undo.FORCE);
      }
  }

  /** Handle the deselection of an expression */

  void handleDeselect(Expression e)
  {
    selected = null;
    e.selected = false;
    if (e.display != null)
      e.display.repaint();
  }

  /** Create a new distance expression */

  void newDistance()
  {
    DistanceExpression newExpression = new DistanceExpression();

    drawPanel.setDrawMode(LABEL_DISTANCE_MODE);
    distanceExpressions.addElement(newExpression);
    show(newExpression);
    selected = newExpression;
    newExpression.select();
    editExpr = newExpression;
    distScroll.setValues(distScroll.getValue(),distHeight,0,
			 distanceExpressions.size()-1);
  }

  /** Create a new angle expression */

  void newAngle()
  {
    AngleExpression newExpression = new AngleExpression();

    drawPanel.setDrawMode(LABEL_ANGLE_MODE);
    angleExpressions.addElement(newExpression);
    show(newExpression);
    selected = newExpression;
    newExpression.select();
    editExpr = newExpression;
    angleScroll.setValues(angleScroll.getValue(),angleHeight,0,
			  angleExpressions.size()-1);
  }

  /** Handle a button-press */

public boolean action(Event e,Object what)
  {
    if (e.target instanceof Button)
      {
	String choice = (String)e.arg;

	if (choice.equals("Close"))
	  {
	    hide();
	    return true;
	  }

	if (choice.equals("New Distance"))
	  {
	    newDistance();
	    return true;
	  }

	if (choice.equals("New Angle"))
	  {
	    newAngle();
	    return true;
	  }

	if (choice.equals("Add Pi"))
	  {
	    if (editExpr != null && !(editExpr instanceof DistanceExpression))
	      editExpr.add(PI_MEASURE);
	  }

	if (choice.equals("Edit"))
	  {
	    drawPanel.setDrawMode(LABEL_MODE);
	    return true;
	  }

	if (choice.equals("Delete"))
	  {
	    drawPanel.setDrawMode(DELETE_EXPRESSIONS_MODE);
	    return true;
	  }

	return true;
      }

    return false;
  }

  /** Handle a scrollbar event */

public boolean handleEvent(Event e) 
  {
    if (e.target instanceof Scrollbar)
      {
	scroll(e.target == angleScroll);
	return true;
      }
    else
      return super.handleEvent(e); // not a scrollbar event
  }

  void message(String m)
  {
    drawPanel.message(m);
  }

  void deselectAll()
  {
    if (selected != null)
      {
	selected.deselect();
	selected = null;
      }
  }

  /** Scroll so that a specified expression is visible */
  
  ExpressionDisplay show(Expression e)
  {
    boolean angle = e instanceof AngleExpression;
    Vector expressions = (angle ? angleExpressions : distanceExpressions);
    ExpressionDisplay[] displays = (angle ? angleDisplay : distDisplay);
    int height = (angle ? angleHeight : distHeight);

    int index = expressions.indexOf(e);
    int scrollTop = (angle ? angleTop : distTop);

    if (scrollTop + height - 1 >= index)
      {
	displays[index-scrollTop].set(e);
	return displays[index-scrollTop];
      }
      
    scroll(angle,index);

    scrollTop = (angle ? angleTop : distTop);

    return displays[index-scrollTop];
  }

  /** Scroll to a specific expression */

  void scroll(boolean angle,int index)
  {
    (angle ? angleScroll : distScroll).setValue(index);

    scroll(angle);
  }

  /** Adjust the expression displays based on the scrollbar */

  void scroll(boolean angle)
  {
    Vector expressions = (angle ? angleExpressions : distanceExpressions);
    ExpressionDisplay[] displays = (angle ? angleDisplay : distDisplay);
    Scrollbar sb = (angle ? angleScroll : distScroll);  
    int scrollTop = (angle ? angleTop : distTop);
    int height = (angle ? angleHeight : distHeight);
      
    int index = sb.getValue();

    if (scrollTop == index)
      return;

    scrollTop = index;

    if (angle)
      angleTop = index;
    else
      distTop = index;

    for(int i=0;i<height;i++)
      {
	if (scrollTop + i < expressions.size())
	  {
	    Expression e = (Expression)expressions.elementAt(scrollTop+i);
	    displays[i].set(e);
	  }
	else
	  displays[i].clear();
      }
  }
}
