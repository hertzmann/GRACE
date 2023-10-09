/* GRACE - Graphical Ruler and Compass Editor
 *
 * Construction.java
 *
 * This file contains the Construction class and related classes
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.util.*;
import java.io.*;
import java.awt.*;

/** A construction, consisting of a list of rules, a list of input and 
 *  a list output constraints */

public class Construction implements Constants
{
  /** The name of this construction */
  String name = "";

  /** The textual description of this construction */
  String description = "";

  /** The number of inputs in this construction */
  int numberOfInputs = 0;

  /** The list of rules.  The first numberOfInputs rules represent
   *  input points.  The rest (except the last) each corresponds to
   *  a step in the construction.  The last contains a list of output
   *  shapes */
  Vector rules = new Vector();

  /** The list of input constraints, stored as ConstraintRules */
  Vector inputConstraints = new Vector();

  /** The list of output constraints, stored as ConstraintRules */
  Vector outputConstraints = new Vector();


  /**
   * Apply the construction to the given list of inputs
   *
   *
   * There are four cases when the apply() functions are called:
   * 1. applied by user, want all constraints and POs and named outputs
   * 2. during a construction, want POs
   * 3. during a drag, want nothing
   * 4. during a "view", want everything
   *
   * this will generate some dead POs -
   * 1. if an exception occurs
   * 2. a partial order is created for a line and all but one or two points
   *    are discarded
   *
   *  @param inputs   The list of inputs shapes, in order
   *  @param makePOs  Should apply() generate partial orders?
   *  @exception ConstructionError   Thrown when the construction fails
   */

  Shape[] apply(Shape[] inputs,boolean makePOs) throws ConstructionError
  {
    // Check that inputs is the correct length
    if (inputs.length != numberOfInputs)
      throw new ConstructionError("Wrong number of inputs");

    // Create an array for all of the intermediate steps
    // steps[i] represents a list of the outputs of step i.
    Shape[][] steps = new Shape[rules.size()][];

    int i;

    // copy the inputs into steps[][]

    for(i=0;i<inputs.length;i++)
      {
	steps[i] = new Shape[1];
	steps[i][0] = inputs[i];
      }

    // call the basic apply function
    return applyCore(steps,makePOs);
  }

  /** Apply the construction to the given list of inputs, and
   *  generate partial orders and constraints.  The outputs
   *  will be named by apply(), so that the names appear in the
   *  output constraints
   *
   *  @param inputs   The list of inputs shapes, in order
   *  @param cf       The constraint window for adding constraints to
   *  @param names    Null, or a list of names to apply to the outputs
   *  @param firstLabel   The first suffix to try naming the outputs.
   *                  Only used if names == null
   *  @param usedNames  The table of names that have already been used
   *  @param stepNum  The prefix to use when naming constraints ("N: ")
   *  @exception ConstructionError   Thrown when the construction fails
   */

  Shape[] apply(Shape[] inputs,ConstraintFrame cf,int firstLabel,
		String[] names,Hashtable usedNames,int stepNum)
    throws ConstructionError
    {
      // Check that inputs[] is the right length
      if (inputs.length != numberOfInputs)
	throw new ConstructionError("Wrong number of inputs");

      // array for all of the intermediate steps
      Shape[][] steps = new Shape[rules.size()][];

      int i;

      // copy the inputs into steps[][]

      for(i=0;i<inputs.length;i++)
	{
	  steps[i] = new Shape[1];
	  steps[i][0] = inputs[i];
	}

      // Check that the input constraints are met
      testAssumptions(steps,cf.nullspace);

      // Call the basic apply function
      Shape[] outputs = applyCore(steps,true);

      // Have no constraints already been generated?
      boolean firstConstraint = true;

      // Name the output shapes
      for(i=0;i<outputs.length;i++)
	{
	  if (names != null)
	    {
	      outputs[i].label = names[i];
	      usedNames.put(outputs[i].label,outputs[i]);
	    }
	  else
	    outputs[i].label = DrawPanel.uniqueName("A",usedNames,
						    firstLabel+i);
	}

      // Generate the output constraints
      for(i=0;i<outputConstraints.size();i++)
	{
	  // Convert each output ConstraintRule to a Constraint
	  Constraint c = ((ConstraintRule)outputConstraints.
			  elementAt(i)).create(steps);

	  if (DEBUG)
	    {
	      System.out.println("nullspace = "+
				 cf.nullspace.toString());
	      System.out.println("adding "+c.toString());
	    }

	  // Check if this constraint is redundant
	  if (!cf.nullspace.follows(c))
	    {
	      c.name = c.toString();

	      if (firstConstraint)
		{
		  // add a blank line before the first consraint
		  firstConstraint = false;
		  cf.addBlankStep();
		}

	      // add the constraint to the constraintFrame
	      cf.addStep(c,stepNum+": ");

	      // add the constraint to the nullspace
	      cf.addProvenConstraint(c);
	    }

	  if (DEBUG)
	    System.out.println("new nullspace = "+
			       cf.nullspace.toString());
	}

      // Generate automatic constraints (inferred from sidedness)
      for(i=0;i<outputs.length;i++)
	{
	  if (outputs[i] instanceof PointShape)
	    {
	      PointShape ps = (PointShape)outputs[i];

	      // for each PartialOrder
	      for(int j=0;j<ps.POs.size();j++)
		{
		  // generate enough constraints to represent the partial
		  // ordering
		  Vector constraints = 
		    ((PartialOrder)ps.POs.elementAt(j)).
		    makeLineConstraints();

		  // for each new constraint
		  for(int k=0;k<constraints.size();k++)
		    {
		      Constraint c= (Constraint)constraints.elementAt(k);
		      
		      // check if the constraint is redundant
		      if (!cf.nullspace.follows(c))
			{
			  c.name = c.toString();
			  
			  if (firstConstraint)
			    {
			      // add a blank step in the constaintFrame
			      firstConstraint = false;
			      cf.addBlankStep();
			    }
			  
			  // add the constraint to the constraintFrame
			  cf.addStep(c,stepNum+"A:");

			  // add the constraint to the nullspace
			  cf.addProvenConstraint(c);
			}
		    }
		}
	    }
	}

      // return the output shapes
      return outputs;
    }

  /** Generate the output shapes from a construction.
   *
   *  @param steps List of outputs from each step.  Initialized by the
   *               calling function.
   *  @param makePOs Should we generate partial orders?
   *  @exception ConstructionError  Thrown when the construction fails
   */

  Shape[] applyCore(Shape[][] steps,boolean makePOs) throws ConstructionError
  {
    /** The parent shapes of the current step */
    Shape[] parents;

    /** A list of all intermediate (not input or output) points */
    Vector points = new Vector();

    // for each step
    for(int i=numberOfInputs;i<rules.size();i++)
      {
	// get the Rule for this step
	Rule currentRule = (Rule)rules.elementAt(i);

	// fetch the parents of the current step
	parents = new Shape[currentRule.parents.length];

	for(int j=0;j<currentRule.parents.length;j++)
	  {
	    int parentNum = currentRule.parents[j].stepNumber;

	    if (steps[parentNum].length <= currentRule.childNumber[j])
	      throw new ConstructionError(points,
					  "Construction impossible - Step "+j);

	    parents[j] = steps[parentNum][currentRule.childNumber[j]];
	  }

	// apply the step

	switch(currentRule.type)
	  {
	  case LINE:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to line primitive");

	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new Line((PointShape)parents[0],
				   (PointShape)parents[1]);

	    break;

	  case PERP_BI:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to perp bi primitive");

	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new PerpBi((PointShape)parents[0],
				     (PointShape)parents[1]);

	    break;

	  case RAY:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to ray primitive");


	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new Ray((PointShape)parents[0],
				  (PointShape)parents[1]);

	    // create a partial order for this shape
	    if (makePOs)
	      ((LineShape)steps[i][0]).makePO((PointShape)parents[0],
					      (PointShape)parents[1]);

	    break;

	  case COMPL_RAY:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to inverse ray primitive");

	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new ComplRay((PointShape)parents[0],
				       (PointShape)parents[1]);

	    // create a partial order for this shape
	    if (makePOs)
	      ((LineShape)steps[i][0]).makePO((PointShape)parents[0],
					      (PointShape)parents[1]);

	    break;

	  case LINE_SEGMENT:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to line segment primitive");

	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new LineSegment((PointShape)parents[0],
					  (PointShape)parents[1]);

	    // create a partial order for this shape
	    if (makePOs)
	      ((LineShape)steps[i][0]).makePO((PointShape)parents[0],
					      (PointShape)parents[1]);

	    break;

	  case CIRCLE:
	    // check that the inputs are the right type
	    if (!(parents[0] instanceof PointShape &&
		  parents[1] instanceof PointShape))
	      throw new ConstructionError(points,
					  "Bad inputs to circle primitive");


	    // create a new shape
	    steps[i] = new Shape[1];
	    steps[i][0] = new Circle((PointShape)parents[0],
				     (PointShape)parents[1]);
	    break;
	      
	  case INTERSECTION:
	    // compute the intersection
	    if (makePOs)
	      steps[i] = Geometry.IntersectionPO(parents[0],parents[1]);
	    else
	      steps[i] = Geometry.Intersection(parents[0],parents[1]);

	    // add all the outputs to the list of intermediates
	    for(int k=0;k<steps[i].length;k++)
	      if (steps[i][k] instanceof PointShape)
		points.addElement(steps[i][k]);

	    break;      

	  case CONSTRUCTION:
	    // apply the construction
	    steps[i] = currentRule.construction.apply(parents,makePOs);

	    // add all output points to the list of intermediates
	    for(int k=0;k<steps[i].length;k++)
	      if (steps[i][k] instanceof PointShape)
		points.addElement(steps[i][k]);
	    
	    break;
	    
	  case OUTPUT:
	    steps[i] = new Shape[currentRule.parents.length];
	    // copy pointers to the output shapes
	    for(int j=0;j<currentRule.parents.length;j++)
	      {
		steps[i][j] = steps[currentRule.parents[j].stepNumber]
		  [currentRule.childNumber[j]];

		// remove the output points from the list of intermediates
		points.removeElement(steps[i][j]);
	      }
	    break;

	  case FORCE:
	    // do nothing on a Force Rule
	    break;

	  default:
	    System.out.println("Unknown shape type!");
	  }
	
	// check that the number of outputs is correct
	if (currentRule.type != OUTPUT && currentRule.type != FORCE &&
	    steps[i].length != currentRule.childName.length)
	  throw new ConstructionError("Construction failed - Step "+
				      (i-numberOfInputs));
      }

    Shape[] outputs = steps[rules.size()-1];

    // Remove intermediate points from Circle.centerPoint and 
    // Circle.pointOnCircle in any output circles
    // Remove intermediate points from PerpBi.origins[] in any output
    // PerpBis

    for(int i=0;i<outputs.length;i++)
      if (outputs[i] instanceof Circle)
	{
	  Circle c= (Circle)outputs[i];

	  if (c.centerPoint != null && points.indexOf(c.centerPoint) >= 0)
	    c.centerPoint = null;

	  if (c.pointOnCircle != null && points.indexOf(c.pointOnCircle) >= 0)
	    c.pointOnCircle = null;
	}
      else
	if (outputs[i] instanceof PerpBi)
	  {
	    PerpBi b = (PerpBi)outputs[i];
	    
	    if (b.origins[0] != null && points.indexOf(b.origins[0]) >= 0)
	      b.origins[0] = null;
	    
	    if (b.origins[1] != null && points.indexOf(b.origins[1]) >= 0)
	      b.origins[1] = null;
	  }


    // delete partial orders for intermediate points
    if (makePOs)
      {
	for(int i=0;i<points.size();i++)
	  ((PointShape)points.elementAt(i)).deletePOs();
      }

    return outputs;
  }
  
  /** Test that the input constraints are met.
   *
   * @param steps  The input points in a 2-d array
   * @param nullspace  The nullspace to test against
   * @exception ConstructionError   Thrown when an input constraint is
   *    not met. 
   */
  void testAssumptions(Shape[][] steps,Nullspace nullspace)
    throws ConstructionError
      {
	// check that the input constraints are met
	for(int i=0;i<inputConstraints.size();i++)
	  {
	    ConstraintRule c1 = (ConstraintRule)inputConstraints.elementAt(i);
	    Constraint c = c1.create(steps);

	    if (DEBUG)
	      {
		System.out.println("Checking "+c.toString());
		System.out.println("nullspace = "+nullspace.toString());
	      }

	    if (c.isTautology())
	      continue;

	    if (!nullspace.follows(c))
	      throw new ConstructionError("Input constraint \""+c.toString()+
					  "\" not met");
	  }
      }

  /** Get the annotation for an input rule */

  String ruleName(int rule)
    {
      String s = ((Rule)rules.elementAt(rule)).inputName;

      return (s == null ? "Input "+rule : s);
    }
}

/** One step in a construction */

class Rule implements Constants, Cloneable
{
  /** Indicates what type (e.g. LINE, INTERSECTION, etc */
  int type;

  /** The rules that generate the parents to this step */
  Rule[] parents;

  /** For entry in the parents[] array, the child number of the
   *  corresponding parent shape */
  int[] childNumber;

  /** The step number of this rule in the construction */
  int stepNumber;

  /** The construction this rule, if any */
  Construction construction;

  /** The annotation for this step, if any */
  String inputName = null;

  /** The name of each child of this rule */
  String[] childName;

  /** The forced constaint, if this is a FORCE rule */
  ConstraintRule force = null;

  /** If this is an input rule, are there default coordinates? 
   *  Default coordindates are used when viewing a construction -
   *  they determine where the input points are placed.
   */

  boolean hasDefaults = false;

  // default coordinates for input rules
  double defaultX;
  double defaultY;

  Rule() { childName = new String[0]; };     // dummy constructor

  /** Create this step from the given dependency */
  Rule(Dependency dep)
    {
      if (dep instanceof ConstructionDependency)
	construction = ((ConstructionDependency)dep).construction;	
      type = dep.type;

      childName = new String[dep.children.length];
      for(int i=0;i<childName.length;i++)
	childName[i] = new String(dep.children[i].label);
    }

  /** Generate a textual representation of this step */
  public String toString()
    {
      StringBuffer sb = new StringBuffer();

      if (childName.length > 0)
	{
	  for(int i=0;i<childName.length;i++)
	    sb.append(childName[i]+' ');

	  sb.append("= ");
	}
      
      switch(type)
	{
	case CIRCLE:
	  sb.append("Circle");
	  break;

	case PERP_BI:
	  sb.append("PerpBi");
	  break;

	case LINE:
	  sb.append("Line");
	  break;

	case RAY:
	  sb.append("Ray");
	  break;
	      
	case COMPL_RAY:
	  sb.append("CompRay");
	  break;

	case LINE_SEGMENT:
	  sb.append("LineSegment");
	  break;

	case INTERSECTION:
	  sb.append("Intersect");
	  break;

	case CONSTRUCTION:
	  sb.append("\""+construction.name+"\"");
	  break;

	case FORCE:
	  sb.append("Force "+force);
	  return new String(sb);

	case OUTPUT:
	  break;

	default:
	  return "Error!";
	}

      sb.append("(");

      for(int i=0;i<parents.length;i++)
	{
	  if (i>0)
	    sb.append(",");
	  sb.append(parents[i].childName[childNumber[i]]);
	}
      sb.append(")");
      return new String(sb);
    }

  /** Look for a specified child by name among the offspring
   *
   * @param name  The name of the child to look for 
   * @returns the index of that child
   */

  int findChild(String name)
  {
    for(int i=0;i<childName.length;i++)
      if (childName[i].equals(name))
	return i;

    System.out.println("Didn't find child "+name);
    System.out.println("this is "+toString());

    for(int i=0;i<childName.length;i++)
      System.out.println(childName[i]+" ");

    System.out.println("");

    return -1;
  }
}

/** Indicates that the construction failed */

class ConstructionError extends Throwable
{
  ConstructionError(String message)
    {
      super(message);
    }

  /** Create the construction error and delete partial orders
   *  for all intermediate points */
  ConstructionError(Vector intPoints,String message)
    {
      super(message);

      for(int i=0;i<intPoints.size();i++)
	((PointShape)intPoints.elementAt(i)).deletePOs();
    }
}

/** A rule corresponding to a MeasureDependency */

class MeasureRule implements Constants
{
  /** The rules that generate the parents to this step */
  Rule[] parents;

  /** For entry in the parents[] array, the child number of the
   *  corresponding parent shape */
  int[] childNum;

  /** angle measure or distance measure */
  int type;

  /** The coefficient of this variable */
  int weight;

  /** Create a MeasureDependency from this Rule
   *
   * @param steps  The shapes output by each step of the construction
   */
  MeasureDependency makeDependency(Shape[][] steps)
  {
    // the parent shapes
    PointShape[] ps = new PointShape[parents.length];

    for(int j=0;j<ps.length;j++)
      ps[j] = (PointShape)steps[parents[j].stepNumber][childNum[j]];
	
    MeasureDependency md;

    if (type == DISTANCE_MEASURE)
      {
	md = ps[1].getDistanceMeasure(ps[0]);
      }
    else
      {
	md = ps[1].getAngleMeasure(ps[0],ps[2]);
      }

    return md;
  }

  /** Create a textual representation of this measurement */
  public String toString()
  {
    String w = (weight == 1 ? "" : weight+"*");

    if (type == ANGLE_MEASURE)
      return w+"angle("+parents[0].childName[childNum[0]]+','+
	parents[1].childName[childNum[1]]+','+
	parents[2].childName[childNum[2]]+')';
    else
      return w+"dist("+parents[0].childName[childNum[0]]+','+
	parents[1].childName[childNum[1]]+')';
  }
}

/** A rule representing a constraint */

class ConstraintRule implements Constants
{
  /** The MeasureRules on the left side of the constraint */
  Vector leftInputs = new Vector();

  /** The MeasureRules on the rightside of the constraint */
  Vector rightInputs = new Vector();

  /** The type, angle or distance constraint */
  int type;

  /** The coefficient of PI in this constaint (<0 = right side) */
  int numPi = 0 ;
  
  /** Add a measure rule to the constraint */
  void add(MeasureRule m,boolean leftSide)
  {
    if (leftSide)
      {
	leftInputs.addElement(m);
      }
    else
      {
	rightInputs.addElement(m);
      }
  }

  /** Add a measure rule to the constraint */

  void add(MeasureRule m)
  {
    if (m.weight < 0)
      {
	m.weight *= -1;
	rightInputs.addElement(m);
      }
    else
      leftInputs.addElement(m);
  }

  /** Add PI to the constraint
   *
   *  @param leftSide   Which side?
   *  @param weight     The coefficient
   */

  void addPi(boolean leftSide,int weight)
  {
    if (leftSide)
      numPi += weight;
    else
      numPi -= weight;
  }

  /** Add PI to the constraint */

  void addPi(int weight)
  {
    numPi += weight;
  }

  /** Make a constraint from this rule
   *
   * @param steps The output shapes from each step of the construction
   */

  Constraint create(Shape[][] steps)
  {
    Constraint c = new Constraint();

    MeasureDependency md;

    // for each parent measure rule, generate a measure dependency
    // and add it to the constraint

    for(int i=0;i<leftInputs.size();i++)
      {
	MeasureRule m = (MeasureRule)leftInputs.elementAt(i);

	md = m.makeDependency(steps);

	c.add(md,m.weight);
      }

    for(int i=0;i<rightInputs.size();i++)
      {
	MeasureRule m = (MeasureRule)rightInputs.elementAt(i);

	md = m.makeDependency(steps);

	c.add(md,-m.weight);
      }

    if (numPi != 0)
      c.add(PI_MEASURE,numPi);

    return c;
  }

  /** Generate a textual representation for this constraint */

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    for(int i=0;i<leftInputs.size();i++)
      {
	if (i > 0)
	  sb.append('+');
	sb.append(((MeasureRule)leftInputs.elementAt(i)).toString());
      }

    if (numPi > 0)
      {
	if (leftInputs.size() > 0)
	  sb.append('+');

	if (numPi > 1)
	  sb.append(numPi+'*');

	sb.append("PI");
      }

    if (leftInputs.size() == 0 && numPi <= 0)
      sb.append('0');

    sb.append(" =");

    for(int i=0;i<rightInputs.size();i++)
      sb.append(' '+((MeasureRule)rightInputs.elementAt(i)).toString());

    if (numPi < 0)
      {
	if (rightInputs.size() > 0)
	  sb.append('+');

	if (numPi < -1)
	  sb.append((-numPi)+'*');
	sb.append("PI");
      }
    
    if (rightInputs.size() == 0 && numPi <= 0)
      sb.append(" 0");

    return new String(sb);
  }
}
  

