/* GRACE - Graphical Ruler and Compass Editor
 *
 * Constraint.java
 *
 * Constraints, constraint window, and the nullspace
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.util.*;
import java.awt.*;
import java.lang.*;

/** A symbolic constraint, consisting of a list of sources, each
 *  with a coefficient.  The sums of the coefficients times sources
 *  is constrained to be zero.
 *
 *  The term source here is used to mean the same as variable.
*/

public class Constraint implements Cloneable, Constants
{
  /** Is this constraint an assumption?  (An input constraint) */
  boolean assumption;

  /** The name of this constraint */
  String name = null;

  /** List of MeasureDependencies */
  Vector sources;

  /** List of the coefficients for the corresponding sources */
  Vector weights;

  /** I wonder what this variable is for? */
//  String label = null;

  Constraint()
    {
      sources = new Vector();
      weights = new Vector();
      assumption = false;
    }

  /** Create a new constraint by setting two expressions equal */

  Constraint(Expression e1, Expression e2)
    {
      sources = (Vector)e1.sources.clone();
      weights = (Vector)e1.weights.clone();

      // merge the sources and weights vectors

      for(int i=0;i<e2.sources.size();i++)
	{
	  Object src = e2.sources.elementAt(i);
	  int weight = ((Integer)e2.weights.elementAt(i)).intValue();
	  add(src,-weight);
	}

      name = e1.toString() + " = " + e2.toString();
      assumption = true;
    }

  /** Add a term to the constraint
   *
   * @param src  The variable
   * @param weight   The coefficient
   */

  void add(Object src,int weight)
    {
      int index = sources.indexOf(src);
      
      // check if this source is already in the constraint
      if (index < 0)
	{
	  // just add the term
	  sources.addElement(src);
	  weights.addElement(new Integer(weight));
	}
      else
	{
	  // combine the terms
	  int oldWeight = ((Integer)weights.elementAt(index)).intValue();
	  int newWeight = oldWeight+weight;

	  if (newWeight != 0)
	    weights.setElementAt(new Integer(newWeight),index);
	  else
	    {
	      sources.removeElementAt(index);
	      weights.removeElementAt(index);
	    }
	}
    }

  /** Check if any sources in the are the same, and combine their
   *  weights, if necessary */

  void joinDuplicates()
  {
    for(int i=sources.size()-1;i>=0;i--)
      {
	Object src = sources.elementAt(i);
	int index = sources.indexOf(src);

	if (index >= i)
	  continue;

	int weight1 = ((Integer)weights.elementAt(i)).intValue();
	int weight2 = ((Integer)weights.elementAt(index)).intValue();

	int newWeight = weight1+weight2;

	sources.removeElementAt(i);
	weights.removeElementAt(i);
	      
	if (newWeight == 0)
	  {
	    sources.removeElementAt(index);
	    weights.removeElementAt(index);

	    i--;
	  }
	else
	  weights.setElementAt(new Integer(newWeight),index);
      }
  }

  /** Check if this constraint is a tautology */

  boolean isTautology()
    {
      return sources.size() == 0;
    }

  /** Check if this constraint sets PI = 0 */

  boolean isInvalid()
    {
      return sources.size() == 1 && 
	sources.firstElement() instanceof PiMeasure;
    }

  /** Generate a textual representation for this constraint */

  public String toString()
    {
      // a list of the sources on the left side
      Vector leftSide = new Vector();
      // list of the weights on the left side
      int[] leftWeights = new int[sources.size()];

      // a list of the sources on the left side
      Vector rightSide = new Vector();
      // list of the weights on the left side
      int[] rightWeights = new int[sources.size()];

      // partition the sources to the leftSide and rightSide vectors

      for(int i=0;i<sources.size();i++)
	{
	  int weight = ((Integer)weights.elementAt(i)).intValue();

	  if (weight > 0)
	    {
	      leftWeights[leftSide.size()] = weight;
	      leftSide.addElement(sources.elementAt(i));
	    }
	  else
	    {
	      rightWeights[rightSide.size()] = -weight;
	      rightSide.addElement(sources.elementAt(i));
	    }
	}

      StringBuffer sb = new StringBuffer();

      // print the left side of the equation

      if (leftSide.size() == 0)
	sb.append('0');
      else
	for(int i=0;i<leftSide.size();i++)
	  {
	    if (i>0)
	      sb.append('+');
	    if (leftWeights[i] != 1)
	      sb.append(leftWeights[i]+"*");
	      sb.append(leftSide.elementAt(i).toString());
	  }

      sb.append('=');

      // print the right side of the equation

      if (rightSide.size() == 0)
	sb.append('0');
      else
	for(int i=0;i<rightSide.size();i++)
	  {
	    if (i>0)
	      sb.append('+');
	    if (rightWeights[i] != 1)
	      sb.append(rightWeights[i]+"*");
	      sb.append(rightSide.elementAt(i).toString());
	  }

      return new String(sb);
    }

  public Object clone()
    {
      Constraint c1 = new Constraint();

      c1.assumption = assumption;
      c1.sources = (Vector)sources.clone();
      c1.weights = (Vector)weights.clone();
      
      return c1;
    }

  /** Check that this constraint is not dependent on any intermediate
   *  points.  The output points are specified by the arguments.
   *
   * @param outputParents  A list of the parents of the output shapes
   * @param outputChildren  Which child of each output parent is each output?
   *
   * @return  True if this constraint does not depend on intermediates
   */

  boolean validOutput(Vector outputParents,Vector outputChildren)
  {
    Vector outputs = new Vector();

    // create a list of output shapes

    for(int i=0;i<outputParents.size();i++)
      {
	Dependency d = (Dependency)outputParents.elementAt(i);
	int j = ((Integer)outputChildren.elementAt(i)).intValue();
	outputs.addElement(d.children[j]);
      }

    // check that each of the sources is a valid output

    for(int i=0;i<sources.size();i++)
      {
	MeasureDependency md = (MeasureDependency)sources.elementAt(i);

	if (!md.isValidOutput(outputs))
	  return false;
      }

    return true;
  }

  /** Make a constraint rule from this constraint.  The parents shapes
   *  to this rule should have the editorCopy variable set, so that 
   *  their corresponding rules can be determined.
   */

  ConstraintRule makeRule()
  {
    ConstraintRule cr = new ConstraintRule();

    // for each source
    for(int j=0;j<sources.size();j++)
      {
	MeasureDependency md = (MeasureDependency)sources.elementAt(j);

	if (md.type == PI)
	  {
	    cr.addPi(((Integer)weights.elementAt(j)).intValue());
	    continue;
	  }

	// create a rule for this MeasureDependency

	MeasureRule mr = new MeasureRule();

	mr.type = md.type;
	mr.weight = ((Integer)weights.elementAt(j)).intValue();
	mr.parents = new Rule[mr.type == ANGLE_MEASURE ? 3 : 2];
	mr.childNum = new int[mr.type == ANGLE_MEASURE ? 3 : 2];

	for(int k=0;k<mr.parents.length;k++)
	  {
	    Shape s = md.parents[k];

	    mr.parents[k] = s.source.editorCopy;

	    int m = 0;
	    while(s.source.children[m] != s)
	      m++;

	    mr.childNum[k] = m;
	  }
	    
	cr.add(mr);
      }

    return cr;
  }
}

/** The constraints window.  This structure also stores all constraints
 *  and the nullspace */
	  
class ConstraintFrame extends Frame implements Constants
{
  /** The current nullspace */
  Nullspace nullspace = new Nullspace();

  /** The display of input constraints */
  java.awt.List inputList;

  /** The display of intermediate constraints */
  java.awt.List stepsList;

  /** The display of output constraints */
  java.awt.List outputList;

  /** The assumptions */
  Vector inputConstraints = new Vector();

  /** Table for looking up intermediate constraints by name */
  Hashtable steps = new Hashtable();

  /** The conclusions */
  Vector outputConstraints = new Vector();

  DrawPanel drawPanel;
  Editor editor;
  Undo undo;

  /** Create the window (but don't display it yet) */

  ConstraintFrame(DrawPanel dp,Editor e,Undo u)
    {
      super("Constraints");
      
      drawPanel = dp;
      editor = e;
      undo = u;

      setLayout(new BorderLayout());

      Panel controls = new Panel();
      Panel middle = new Panel();
      Panel top = new Panel();
      controls.setLayout(new BorderLayout());
      
      top.add(new Button("Create input"));
      top.add(new Button("Force intermediate"));
      middle.add(new Button("Create output"));
      middle.add(new Button("Test intermediate"));
      controls.add("North",top);
      controls.add("Center",middle);
      controls.add("South",new Button("Close"));

      add("South",controls);

      Panel op = new Panel();
      op.setLayout(new BorderLayout());
      op.add("North",new Label("Output constraints"));
      outputList = new java.awt.List(4,false);
      op.add("Center",outputList);

      Panel mp = new Panel();
      mp.setLayout(new BorderLayout());
      mp.add("North",new Label("Intermediate constraints"));
      stepsList = new java.awt.List(9,false);
      mp.add("Center",stepsList);

      Panel ip = new Panel();
      ip.setLayout(new BorderLayout());
      ip.add("North",new Label("Input constraints"));
      inputList = new java.awt.List(4,false);
      ip.add("Center",inputList);
      
      Panel p = new Panel();
      p.setLayout(new BorderLayout());
      p.add("South",op);
      p.add("Center",mp);
      p.add("North",ip);
      add("Center",p);

      pack();
    }

  /** Clear out all existing constraints and reset the nullspace */

  void clear()
    {
      inputList.clear();
      outputList.clear();
      stepsList.clear();
      steps.clear();
      nullspace.clear();
      inputConstraints.removeAllElements();
      outputConstraints.removeAllElements();
    }

  /** Handle a list event */

  public boolean handleEvent(Event e) 
    {
      // only care when an intermediate constraint is selected in 
      // conclude mode
      if (drawPanel.mode != CONCLUDE_MODE || e.target != stepsList)
	return super.handleEvent(e);

      switch (e.id)
	{
	case Event.LIST_SELECT:
	  // get the index of the selected item
	  int index = ((Integer)e.arg).intValue();

	  // get the selected item
	  Constraint c = (Constraint)steps.get(stepsList.getItem(index));

	  // check that the item exists
	  if (c == null)
	    return true;

	  // make sure that this is a valid output constraint

	  if (!c.validOutput(editor.outputParents,editor.outputChildren))
	    message("Cannot make output constraint on dependent points");
	  else
	    {
	      // make this an output constraint
	      undo.saveState();
	      undo.saveStep(Undo.CONCLUDE);
	      addOutput(c);
	    }

	  return true;
	default:
	  return super.handleEvent(e);
	}
    }

  /** Handle a button press */

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
	  else
	    if (choice.equals("Create input"))
	      {
		drawPanel.setDrawMode(ASSUME_CONSTRAINT_MODE);
		return true;
	      }
	  else
	    if (choice.equals("Test intermediate"))
	      {
		drawPanel.setDrawMode(TEST_CONSTRAINT_MODE);
		return true;
	      }
	  else
	    if (choice.equals("Force intermediate"))
	      {
		drawPanel.setDrawMode(FORCE_CONSTRAINT_MODE);
		return true;
	      }
	  else if (choice.equals("Create output"))
	    {
	      drawPanel.setDrawMode(CONCLUDE_MODE);
	      return true;
	    }
	  
	}

      return false;
    }

  void message(String text)
    {
       drawPanel.message(text);
     }

  /** Add an input constraint
   *
   * @return  Returns true if the constraint is already true
   */

  boolean addInput(Constraint c)
  {
    if (nullspace.add(c))
      {
	c.assumption = false;
	return true;
      }

    inputConstraints.addElement(c);
    inputList.addItem(c.toString());

    return false;
  }

  /** Add a constraint to the intermediate steps */

  void addStep(Constraint c)
  {
    addStep(c,"");
  }

  /** Add a constraint to the intermediate steps, with the given prefix */

  void addStep(Constraint c,String prefix)
  {
    stepsList.addItem(prefix+c.toString());
    steps.put(prefix+c.toString(),c);
  }

  /** Remove the last intermediate constraints
   *
   * @param label  The last prefix
   */

  void removeSteps(String label)
  {
    for(int i=stepsList.countItems()-1;i>=0;i--)
      {
	String s = stepsList.getItem(i);

	// check if this is a blank line
	if (s.length() == 0)
	  {
	    stepsList.delItem(i);
	    continue;
	  }

	StringTokenizer st = new StringTokenizer(s,"A:",true);

	// check that the line starts with label
	if (!st.nextToken().equals(label))
	  break;

	stepsList.delItem(i);
      }
  }

  /** Add an output constraint to the display */

  void addOutput(Constraint c)
  {
    outputList.addItem(c.toString());
    outputConstraints.addElement(c);
  }

  /** Add a constraint to the nullspace */

  void addProvenConstraint(Constraint c)
    {
      nullspace.add(c);
      c.assumption = false;

      if (DEBUG)
	System.out.println("New Nullspace = \n"+nullspace);
    }

  /** Add a blank line to the intermediate steps */

  void addBlankStep()
  {
    if (stepsList.countItems() > 0)
      stepsList.addItem("");
  }

  /** Deselect any selected step */

  void deselectAll()
  {
    stepsList.deselect(stepsList.getSelectedIndex());
  }

  /** Remove the last input constraint */

  void removeInput()
  {
    inputList.delItem(inputList.countItems()-1);
    inputConstraints.removeElementAt(inputConstraints.size()-1);
  }

  /** Remove the last output constraint */

  void removeOutput()
  {
    outputList.delItem(outputList.countItems()-1);
    outputConstraints.removeElementAt(outputConstraints.size()-1);
  }
}

/** The nullspace, for recording the space of proven constraints
 *
 *  Constraints can be added to the nullspace, and a constraint
 *  can be tested if it is consistent with the nullspace.  A
 *  constraint is consistent with the nullspace if there exists a
 *  linear combination of other constraints that generates the
 *  desired constraint
 *  
 *  The nullspace is stored as a sparse matrix.  Each row is a list
 *  of variables and the nonzero coefficients for each one.  Variables
 *  not listed in a row have a coefficient of zero.
 *  
 *  The nullspace conceptually starts out as a giant identity matrix,
 *  with an entry for every possible variable.  In practice, the
 *  nullspace begins empty.  When a constraint containing a new variable
 *  is added to the matrix, a new row is first added to the nullspace
 *  corresponding to that variable.
 */

class Nullspace implements Constants, Cloneable
{
  /** The rows of the nullspace */
  Vector rows = new Vector();

  /** A list of the variables currently contained in the nullspace */
  Vector variables = new Vector();

  /** Add a constraint to the nullspace
   *
   *  @param  c  The new constraint
   *  @return  True if the new constraint is already consistent with the
   *           existing nullspace
   */ 

  boolean add(Constraint c)
    {
      // Convert the Constraint into a Row
      // do this first to combine equivalent angles; might zero
      Row newRow = new Row(c);

      // Check if c contains any variables that the nullspace doesn't
      // If so, add them
      for(int i=0;i<newRow.sources.size();i++)
	{
	  Object src = newRow.sources.elementAt(i);

	  if (src instanceof AngleMeasure)
	    src = ((AngleMeasure)src).getEquivalent();

	  if (variables.indexOf(src) < 0)
	    addVariable(src);
	}

      int nk = rows.size();    // n-k = num vars - num constraints
      int[] Nx = new int[nk];
      boolean zero = true;

      int pivot = -1;

      // compute N * x, where N is the nullspace and x is the new row

      for(int i=0;i<nk;i++)
	{
	  Nx[i] = Row.dot((Row)rows.elementAt(i),newRow);
	  if (Nx[i] != 0)
	    {
	      zero = false;
	      pivot = i;
	    }
	}

      // test if the new constraint was already consistent

      if (zero)
	return true;

      // select the entry with the smallest nonzero magnitude
      // to minimize stability problems 
      for(int i=0;i<nk;i++)
	if (Nx[i] != 0 &&
	    (Math.abs(Nx[i]) < Math.abs(Nx[pivot])))
	  pivot = i;

      if (DEBUG)
	{
	  System.out.print("pivot = "+pivot+", Nx=[");
	  for(int i=0;i<nk;i++)
	    System.out.print(Nx[i]+" ");
	  System.out.println("]");
	}

      // the new nullspace
      Vector newRows = new Vector();
      Row pivotRow = (Row)rows.elementAt(pivot);

      // compute the new nullspace from linear combinations of pairs
      // of rows from the old nullspace

      for(int i=0;i<nk;i++)
	{
	  if (i==pivot)
	    continue;

	  Row r;

	  if (Nx[i] == 0)
//	    r = (Row)((Row)rows.elementAt(i)).clone();
	    r = (Row)rows.elementAt(i);
	  else
	    r = Row.linComb(Nx[i],pivotRow,
			    -Nx[pivot],(Row)rows.elementAt(i));

	  newRows.addElement(r);
	}

      rows = newRows;

      return false;
    }

  /** Check if the given constraint is consistent with the nullspace
   *
   *  @return True if the constraint is consistent
   */

  boolean follows(Constraint c)
    {
      // do this first to combine equivalent angles; might be zero
      Row newRow = new Row(c);

      if (DEBUG)
	System.out.println("Testing "+newRow.toString());

      // Check if c contains any variables that N doesn't
      for(int i=0;i<newRow.sources.size();i++)
	{
	  Object src = newRow.sources.elementAt(i);

	  if (src instanceof AngleMeasure)
	    src = ((AngleMeasure)src).getEquivalent();

	  if (variables.indexOf(src) < 0)
	    return false;
	}

      int nk = rows.size();    // n-k = num vars - num constraints

      // compute N*x

      for(int i=0;i<nk;i++)
	{
	  if (Row.dot(newRow,(Row)rows.elementAt(i)) != 0)
	    return false;
	}

      return true;
    }      

  /** Add a variable to the nullspace, by adding a row with a coefficient
   *  of 1 for the new variable */

  void addVariable(Object variable)
    {
      Row r = new Row();
      r.add(variable,1);
      rows.addElement(r);
      variables.addElement(variable);
    }

  /** Reset the nullspace */

  void clear()
    {
      rows.removeAllElements();
      variables.removeAllElements();
    }

  public String toString()
    {
      StringBuffer sb = new StringBuffer("(");

      for(int i=0;i<rows.size();i++)
	{
	  sb.append(rows.elementAt(i).toString());
	  if (i+1<rows.size())
	    sb.append(",\n   ");
	}

      sb.append(")");

      return new String(sb);
    }

  public Object clone()
  {
    Nullspace n = new Nullspace();
    n.rows = (Vector)rows.clone();
    n.variables = (Vector)variables.clone();

    return n;
  }
}

/** One row in a matrix.  Variables not explicitly listed in the
 *  list of sources have a coefficient of zero in the row.
 *
 *  Angle measurements have a generic representation in order
 *  to guarantee that identical angles with different names will
 *  be treated as the same variable.
 */

class Row
{
  /** The list of variables in the row */
  Vector sources = new Vector();

  /** The list of coefficients for each variable */
  Vector weights = new Vector();

  Row() {}
  
  /** Generate a new row from a constraint */

  Row(Constraint c)
    { 
      sources = (Vector)c.sources.clone();
      weights = (Vector)c.weights.clone();

      int i=0;
      int index;
      int weight;

      // convert angles to their generic equivalent, and consolidate
      // any duplicates
      while (i<sources.size())
	{
	  Object src = sources.elementAt(i);

	  if (src instanceof AngleMeasure)
	    {
	      // get the generic equivalent of this angle
	      Unique newSrc = ((AngleMeasure)src).getEquivalent();

	      index = sources.indexOf(newSrc);
	      
	      // check if this equivalent is already in the list of variables
	      if (index < 0)
		{
		  sources.setElementAt(newSrc,i);
		  i++;
		}
	      else
		{
		  // combine the two identical angles
		  weight = ((Integer)weights.elementAt(i)).intValue()+
		    ((Integer)weights.elementAt(index)).intValue();

		  weights.setElementAt(new Integer(weight),index);

		  sources.removeElementAt(i);
		  weights.removeElementAt(i);

		  if (weight == 0)
		    {
		      sources.removeElementAt(index);
		      weights.removeElementAt(index);

		      if (index < i)
			i--;
		    }
		}
	    }
	  else
	    i++;
	}
    }

  /** Add a variable to the row, with a given coefficient */

  void add(Object source,int weight)
    {
      sources.addElement(source);
      weights.addElement(new Integer(weight));
    }

  /** Get the weight of a variable */

  int getWeight(Object source)
    {
      int index = sources.indexOf(source);
      return (index >= 0 ? ((Integer)weights.elementAt(index)).intValue() : 0);
    }

  /** Compute the dot product of two rows */

  static int dot(Row r1,Row r2)
    {
      int sum = 0;
      for(int i=0;i<r1.sources.size();i++)
	{
	  Object s = r1.sources.elementAt(i);
	  int w1 = ((Integer)r1.weights.elementAt(i)).intValue();
	  int w2 = r2.getWeight(s);

	  sum += (w1 * w2);
	}

      return sum;
    }

  /** Generate the linear combination of two rows
   *
   *  Produces w1*r1 + w2 * r2
   */

  static Row linComb(int w1,Row r1,int w2,Row r2)
    {
      Row newRow = new Row();

      for(int i=0;i<r1.size();i++)
	{
	  Object src = r1.sources.elementAt(i);
	  int wa = ((Integer)r1.weights.elementAt(i)).intValue();
	  int wb = r2.getWeight(src);

	  int newWeight = w1*wa+w2*wb;

	  if (newWeight != 0)
	    newRow.add(src,newWeight);
	}

      for(int i=0;i<r2.size();i++)
	{
	  Object src = r2.sources.elementAt(i);
	  if (r1.sources.indexOf(src) >= 0)
	    continue;

	  int wb = ((Integer)r2.weights.elementAt(i)).intValue();

	  if (wb*w2 != 0)
	    newRow.add(src,wb*w2);
	}

      return newRow;
    }

  /** Return the number of variables in the row */

  int size()
    {
      return sources.size();
    }

  public String toString()
    {
      StringBuffer sb = new StringBuffer("R(");
      for(int i=0;i<sources.size();i++)
	{
	  sb.append(((Integer)weights.elementAt(i)).intValue());
	  sb.append(sources.elementAt(i).toString());
	  if (i<sources.size()-1)
	    sb.append(",");
	}
      sb.append(")");

      return new String(sb);
    }

  protected Object clone()
    {
      Row nr = new Row();

      nr.weights = (Vector)weights.clone();
      nr.sources = (Vector)sources.clone();

      return nr;
    }
}

/** A generic representation for a variable that may have
 *  more than one name.  Two angles may have the same symbolic
 *  value but may be represented by different points. 
 *  Angle measurements are converted to the generic representation
 *  before they are used by Nullspace
 */
abstract class Unique
{
  /** Is this representation equivalent to the given non-generic object? */
  abstract boolean isEquivalent(Object s);
}

class UniquePi extends Unique implements Constants
{
  boolean isEquivalent(Object a)
    {
      return a == PI_MEASURE;
    }
  
  public String toString() { return "Pi"; }
}  

/** The generic representation of an angle
 *
 *  The representation consists of two rays.  Each ray is represented
 *  by a partial order node, and a boolean to represent the direction of 
 *  the ray along the partial order.
 */

class UniqueAngle extends Unique
{
  /** The first partial order node */
  PartialOrder apex1 = null;
  boolean apexOnLeft1;

  /** The first partial order node */
  PartialOrder apex2 = null;
  /** The direction of the first ray along this partial order */
  boolean apexOnLeft2;

  // note that if apex1 == apex2 and apexOnLeft1 == apexOnLeft2
  // then this is a 0 degree angle
  // if apex1 == apex2 and apexOnLeft1 != apexOnLeft2
  // then this is a 180 degree angle

  /** Generate the generic representation from a given AngleMeasure */

  UniqueAngle(AngleMeasure ad)
    {
      PointShape apexPoint = (PointShape)ad.parents[1];
      PointShape p1 = (PointShape)ad.parents[0];
      PointShape p2 = (PointShape)ad.parents[2];

      // check if appropriate partial orders exist for each ray

      for(int i=0;i<apexPoint.POs.size();i++)
	{
	  PartialOrder PO = (PartialOrder)apexPoint.POs.elementAt(i);

	  if (apex1 == null && (p1 == apexPoint || PO.isOnLeft(p1)))
	    {
	      apex1 = PO;
	      apexOnLeft1 = false;
	    }

	  if (apex1 == null && PO.isOnRight(p1))
	    {
	      apex1 = PO;
	      apexOnLeft1 = true;
	    }
	  
	  if (apex2 == null && (p2 == apexPoint || PO.isOnLeft(p2)))
	    {
	      apex2 = PO;
	      apexOnLeft2 = false;
	    }

	  if (apex2 == null && PO.isOnRight(p2))
	    {
	      apex2 = PO;
	      apexOnLeft2 = true;
	    }

	  if (apex1 != null && apex2 != null)
	    break;
	}

      // create new partial orders, if necessary

      if (apex1 == null)
	{
	  apex1 = new PartialOrder(apexPoint);
	  PartialOrder temp = new PartialOrder(p1);
	  
	  PartialOrder.link(apex1,temp);

	  apexOnLeft1 = true;
	  apex1.isNew = false;
	  temp.isNew = false;
	}

      if (apex2 == null)
	{
	  apex2 = new PartialOrder(apexPoint);
	  PartialOrder temp = new PartialOrder(p2);
	  
	  PartialOrder.link(apex2,temp);

	  apexOnLeft2 = true;
	  apex2.isNew = false;
	  temp.isNew = false;
	}
    }

  /** test if the given non-generic object is equivalent to this one
   *
   *  @param  a  The object to test
   *  @return   True if the object is equivalent to this
   */

  boolean isEquivalent(Object a)
    {
      if (!(a instanceof AngleMeasure))
	return false;

      AngleMeasure ad = (AngleMeasure) a;

      PointShape adApex = (PointShape)ad.parents[1];
      PointShape adp1 = (PointShape)ad.parents[0];
      PointShape adp2 = (PointShape)ad.parents[2];

      // check that the apex is the same

      if (adApex != apex1.p)
	return false;

      // match up the rays (two possibilities)

      boolean p11 = apex1.isOnSameSide(adp1,apexOnLeft1);
      boolean p22 = apex2.isOnSameSide(adp2,apexOnLeft2);

      if (p11 && p22)
	return true;

      boolean p12 = apex1.isOnSameSide(adp2,apexOnLeft1);
      boolean p21 = apex2.isOnSameSide(adp1,apexOnLeft2);

      return (p12 && p21);
    }

  public String toString()
    {
      return "Angle("+apex1.p+")";
    }
}
