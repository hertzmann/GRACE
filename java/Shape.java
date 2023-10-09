/* GRACE - Graphical Ruler and Compass Editor
 *
 * Shape.java
 *
 * This file contains all the Shape and Dependency classes
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.awt.*;
import java.lang.*;
import java.util.*;

/** The abstract definition of any shape in the draw area.
 *  All shapes must be derived from this class
 */

public abstract class Shape implements Constants
{
  /** The name of this shape */
  String label;

  /** Any dependencies of this shape */
  Vector offspring = new Vector();

  /** Does this shape exist?  False means the parent construction failed */
  boolean valid = true;

  /** The parent of this shape */
  Dependency source;

  /** The color of this shape */
  Color color = FOREGROUND;

  /** Draw this shape
   *
   * @param g  The graphics context to draw in
   * @param t  The transformation matrix
   */
  abstract void drawPrimitive(Graphics g,Transform t);

  /** Draw the label for this shape
   *
   * @param g  The graphics context to draw in
   * @param t  The transformation matrix
   */

  abstract void drawLabel(Graphics g,Transform t);

  /** Fill the contents of the target shape with the values of this
   *  shape.  Used in drag.
   *
   *  @param s  The shape to copy to
   */
  abstract void replaces(Shape s);

  /** Draw the shape in the right color
   *
   * @param g  The graphics context
   * @param t  The transformation matrix
   */

  void draw(Graphics g,Transform t)
    {
      if (valid)
	{
	  g.setColor(color);
	  drawPrimitive(g,t);
	}
    }

  /** Check if a point is within a certain distance of this shape */

  boolean isPointNearShape(double x,double y,double tolerance)
    {
      return (valid && Geometry.distance(this,x,y) < tolerance);
    }

  /** Check if a point is on the shape
   *  Perhaps this could be overridden by descendants for speed */

  boolean isPointOnShape(double x,double y)
    {
      return (valid && Geometry.distance(this,x,y) == 0);
    }

  /** Check if a point is on the shape */

  boolean isPointOnShape(PointShape p)
    {
      return valid && isPointOnShape(p.x,p.y);
    }

  public String toString() { return label; }

  /** Draws the shape's label at (x,y)
   *  A white rectangle is drawn under the text */

  void labelHelper(int x,int y,Graphics g)
  {
    FontMetrics fm = g.getFontMetrics();
    int h =fm.getAscent();

    g.setColor(FIELD_BACKGROUND);
    g.fillRect(x,y-h,fm.stringWidth(label),h);

    g.setColor(color);
    g.drawString(label,x,y);
  }
}

/** A point */

class PointShape extends Shape
{
  /** The point's x-coordinate */
  double x,y;

  /** Partial orders around this point */
  Vector POs = new Vector();

  /** Unique angle measures centered at this point */
  Vector uniques = new Vector();

  /** Is this a free point?   
   * (equivalent to source instanceof ArbitraryDependency)
   * Used by drawPrimitive() */
  boolean free = false;

  PointShape(int x1,int y1)
    {
      x = x1;
      y = y1;
    }

  PointShape(double x1,double y1)
    {
      x = x1;
      y = y1;
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      if (free)
	g.drawRect(t.virtualToScreenX(x)-3,t.virtualToScreenY(y)-3,6,6);
      else
	g.fillOval(t.virtualToScreenX(x)-4,t.virtualToScreenY(y)-4,8,8);
    }

  void drawLabel(Graphics g,Transform t)
    {
      if (!valid)
	return;

      labelHelper(t.virtualToScreenX(x)+4,t.virtualToScreenY(y)-4,g);
    }

  /** get a DistanceMeasure from this point to p2
   * Using this method to retrieve measurements guarantees that
   * only one copy exists for each measurement
   */

  DistanceMeasure getDistanceMeasure(PointShape p2)
    {
      // look through offspring for the right measure
      for(int i=0;i<offspring.size();i++)
	{
	  Dependency d = (Dependency)offspring.elementAt(i);

	  if (d.isDistance(this,p2))
	    return (DistanceMeasure)d;
	}

      // create the new distance measure
      return new DistanceMeasure(this,p2);
    }

  /** Get an angle measure with this as the apex and p1 and p2 as the
   *  endpoints
   * Using this method to retrieve measurements guarantees that
   * only one copy exists for each measurement
   */

  AngleMeasure getAngleMeasure(PointShape p1, PointShape p2)
    {
      // look through the offspring for the right measure
      for(int i=0;i<offspring.size();i++)
	{
	  Dependency d = (Dependency)offspring.elementAt(i);

	  if (d.isAngle(p1,this,p2))
	    return (AngleMeasure)d;
	}

      // create a new angle measure
      return new AngleMeasure(p1,this,p2);
    }

  void replaces(Shape s)
    {
      PointShape ps =(PointShape)s;

      ps.x = x;
      ps.y = y;
    }

  /** Delete all the partial orders for this pointShape */

  void deletePOs()
    {
      for(int i=0;i<POs.size();i++)
	((PartialOrder)POs.elementAt(i)).delete();

      POs.removeAllElements();
    }
}

/** The abstract class that all lines (rays, perp bi, etc) are derived from */

abstract class LineShape extends Shape
{
  // points on the line
  // for a ray, (x1,y1) is the endpoint, (x2,y2) is on the ray
  // for a compRay, (x1,y1) is the endpoint, (x2,y2) is opposite the ray
  // for a line segment, the points are the endpoints

  double x1,y1,x2,y2;

  // Another line parameterization

  double A,B,C;

  // for use in creating sidedness constraints
  PartialOrder P = null;    // endpoint of this line shape
  PartialOrder Q = null;    // second point of this line shape
  boolean QonLeft;          // is P to the left of Q?

  /** Change the line to that given by a and b */

  void move(PointShape a,PointShape b)
    {
      x1 = a.x;
      y1 = a.y;
      x2 = b.x;
      y2 = b.y;

      A = y2 - y1;
      B = x1 - x2;
      C = x2*y1 - x1*y2;
    }

  /** given (x,y) assumed to be collinear with (x1,y1), (x2,y2)
   return true is (x,y) is on the LineShape */
  abstract boolean isPointOnLine(double x,double y);
  boolean isPointOnLine(PointShape ps) { return isPointOnLine(ps.x,ps.y); }

  /** Add a point on the line to the partial order */
  abstract void addPointOnShape(PartialOrder R);


  /** add two points to the partial order containing this line;
   * R is the point nearer to P */
  abstract void addPointsOnShape(PartialOrder R,PartialOrder S);

  void replaces(Shape s)
  {
    LineShape ls = (LineShape)s;
    
    ls.x1 = x1;
    ls.y1 = y1;
    ls.x2 = x2;
    ls.y2 = y2;

    ls.A = A;
    ls.B = B;
    ls.C = C;

  }

  /** Make a partial order for this line and the endpoints */

  void makePO(PointShape p,PointShape q)
    {
      // check to see if an existing PO already contains these points
      for(int i=0;i<p.POs.size();i++)
	{
	  P = (PartialOrder)p.POs.elementAt(i);

	  Q = P.findOnLeft(q);

	  if (Q != null)
	    {
	      QonLeft = true;
	      return;
	    }

	  Q = P.findOnRight(q);

	  if (Q != null)
	    {
	      QonLeft = false;
	      return;
	    }
	}

      P = new PartialOrder(p);
      Q = new PartialOrder(q);
      PartialOrder.link(P,Q);
      QonLeft = false;

      P.isNew = false;
      Q.isNew = false;
    }

  void drawLabel(Graphics g,Transform t)
    {
      if (!valid)
	return;

      labelHelper(t.virtualToScreenX((-x1+4*x2)/3+4),
		  t.virtualToScreenY((-y1+4*y2)/3-4),g);
    }
}  

/** A line segment connecting the endpoints */

class LineSegment extends LineShape
{
  LineSegment(PointShape a,PointShape b)
    {
      move(a,b);
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      g.drawLine(t.virtualToScreenX(x1),
		 t.virtualToScreenY(y1),
		 t.virtualToScreenX(x2),
		 t.virtualToScreenY(y2));
    }

  boolean isPointOnLine(double x,double y)
    {
      return (Geometry.sameSide(x1,y1,x,y,x2,y2) &&
	      Geometry.sameSide(x2,y2,x,y,x1,y1));
    }

  void addPointOnShape(PartialOrder R)
    {
      if (QonLeft)
	{
	  PartialOrder.link(Q,R);
	  PartialOrder.link(R,P);
	  PartialOrder.unlink(Q,P);
	}
      else
	{
	  PartialOrder.link(P,R);
	  PartialOrder.link(R,Q);
	  PartialOrder.unlink(P,Q);
	}
    }

  void addPointsOnShape(PartialOrder R,PartialOrder S)
    {
      if (QonLeft)
	{
	  PartialOrder.link(Q,S);
	  PartialOrder.link(S,R);
	  PartialOrder.link(R,P);
	  PartialOrder.unlink(Q,P);
	}
      else
	{
	  PartialOrder.link(P,R);
	  PartialOrder.link(R,S);
	  PartialOrder.link(S,Q);
	  PartialOrder.unlink(P,Q);
	}
    }

  void drawLabel(Graphics g,Transform t)
    {
      if (!valid)
	return;

      labelHelper(t.virtualToScreenX((2*x1+x2)/3+4),
		  t.virtualToScreenY((2*y1+y2)/3-4),g);
    }
}

/** A ray extending from the first endpoint through the other */

class Ray extends LineShape
{
  Ray(PointShape a,PointShape b)
    {
      move(a,b);
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      Point p = t.extendRay(x1,y1,x2,y2);

      if (p != null)
	g.drawLine(t.virtualToScreenX(x1),t.virtualToScreenY(y1),p.x,p.y);
    }

  boolean isPointOnLine(double x,double y)
    {
      return (Geometry.sameSide(x1,y1,x,y,x2,y2));
    }

  void addPointOnShape(PartialOrder R)
    {
      if (QonLeft)
	PartialOrder.link(R,P);
      else
	PartialOrder.link(P,R);
    }

  void addPointsOnShape(PartialOrder R,PartialOrder S)
    {
      if (QonLeft)
	{
	  PartialOrder.link(S,R);
	  PartialOrder.link(R,P);
	}
      else
	{
	  PartialOrder.link(P,R);
	  PartialOrder.link(R,S);
	}
    }
}

/** A ray extending from the first endpoint opposite the other */

class ComplRay extends LineShape
{
  ComplRay(PointShape a,PointShape b)
    {
      move(a,b);
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      Point p = t.extendRay(x1,y1,2*x1-x2,2*y1-y2);

      if (p != null)
	g.drawLine(t.virtualToScreenX(x1),t.virtualToScreenY(y1),p.x,p.y);

    }

  boolean isPointOnLine(double x,double y)
    {
      return (!Geometry.sameSide(x1,y1,x,y,x2,y2));
    }

  void addPointOnShape(PartialOrder R)
    {
      if (QonLeft)
	PartialOrder.link(P,R);
      else
	PartialOrder.link(R,P);
    }

  void addPointsOnShape(PartialOrder R,PartialOrder S)
    {
      if (!QonLeft)
	{
	  PartialOrder.link(S,R);
	  PartialOrder.link(R,P);
	}
      else
	{
	  PartialOrder.link(P,R);
	  PartialOrder.link(R,S);
	}
    }

  void drawLabel(Graphics g,Transform t)
    {
      if (!valid)
	return;

      labelHelper(t.virtualToScreenX((4*x1+-x2)/3+4),
		  t.virtualToScreenY((4*y1-y2)/3-4),g);
    }
}

/** A line through the endpoints */

class Line extends LineShape
{
  Line(PointShape a,PointShape b)
    {
      move(a,b);
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      Point p1 = t.extendRay(x1,y1,x2,y2);
      Point p2 = t.extendRay(x2,y2,x1,y1);

      if (p1 == null)
	p1 = new Point(t.virtualToScreenX(x2),t.virtualToScreenY(y2));

      if (p2 == null)
	p2 = new Point(t.virtualToScreenX(x1),t.virtualToScreenY(y1));

      g.drawLine(p1.x,p1.y,p2.x,p2.y);
    }

  boolean isPointOnLine(double x,double y) { return true; }

  void addPointOnShape(PartialOrder R) {}
    // do nothing

  void addPointsOnShape(PartialOrder R,PartialOrder S) {}
    // do nothing

  void makePOs(PointShape p, PointShape q) {}
  // prevent subclass method from being called; to protect code in 
  // PartialOrder.delete()
}

/** All points equidistant from the endpoints */

class PerpBi extends Line
{
  /** The two PointShapes that generated this line.
   *  For use when generating constraints from intersection */
  PointShape origins[];

  PerpBi(PointShape a,PointShape b)
    {
      super(a,b);

      origins = new PointShape[2];
      origins[0] = a;
      origins[1] = b;
    }

  void move(PointShape a,PointShape b)
    {
      x1 = (a.x+b.x)/2;
      y1 = (a.y+b.y)/2;
      x2 = x1 + b.y - a.y;
      y2 = y1 - b.x + a.x;

      A = y2 - y1;
      B = x1 - x2;
      C = x2*y1 - x1*y2;
    }
}

/** A circle. */

class Circle extends Shape
{
  double x,y;  // the center
  double radius;

  // for creating constraints after intersections:

  /** The point at the center of the circle, if it is available */
  PointShape centerPoint = null;

  /** A point on the circle, if available */
  PointShape pointOnCircle = null;

  Circle(double x1,double y1,double r1)
    {
      x = x1;
      y = y1;
      radius = r1;
    }

  Circle(PointShape c,PointShape e)
    {
      move(c,e);

      centerPoint = c;
      pointOnCircle = e;
    }

  void move(PointShape c,PointShape e)
    {
      x = c.x;
      y = c.y;

      radius = Geometry.distance(c,e);
    }

  void drawPrimitive(Graphics g,Transform t)
    {
      g.drawOval(t.virtualToScreenX(x-radius),
		 t.virtualToScreenY(y-radius),
		 (int)(t.scaleFactor*radius*2),
		 (int)(t.scaleFactor*radius*2));
    }

  // place a label above the upper right corner of the circle
  void drawLabel(Graphics g,Transform t)
    {
      if (valid)
	labelHelper(t.virtualToScreenX(x+.7071*radius)+4,
		    t.virtualToScreenY(y-.7071*radius)-4,g);
    }

  void replaces(Shape s)
  {
    Circle c= (Circle)s;
    
    c.x = x;
    c.y = y;
    c.radius = radius;
  }
}

/** A object for indicating how shapes were created.
 *  The shapes and dependencies comprise a DAG.  
 *  Dependencies represent constructions and link parent shapes
 *  to offspring shapes.  To recompute the shapes, recompute the
 *  dependencies in topological order.
 */
abstract class Dependency implements Constants
{
  /** The inputs to this construction */
  Shape[] parents;

  /** The outputs generated by this construction */
  Shape[] children;
    
  /** What type of dependency this is */
  int type;

  /** Was the construction successful last time it was computed? */
  boolean successful = true;

  /** A temporary variable used by drag point; should be kept false by
   *  default. */
  boolean mark = false;

  /** A pointer to the rule used to generate this dependency.
   *  Temporary variable used by editorCopy. */
  Rule editorCopy = null;

  /** Recompute the children in place.  Sets succesful and adjusts the
   *  childrens' data. */
  abstract void recompute(Vector shapes);

  /** Is this an angleMeasure? */
  boolean isAngle(Shape s1, Shape s2, Shape s3) { return false; }

  /** Is this a distanceMeasure? */
  boolean isDistance(Shape s1, Shape s2) { return false; }

  /** Give a textual representation for this dependency */
  public String toString()
    {
      StringBuffer sb = new StringBuffer();
      if (children.length > 0)
	{
	  for(int i=0;i<children.length;i++)
	    sb.append(children[i].label+' ');

	  sb.append("= ");
	}

      sb.append(ruleType()+'(');
      for(int i=0;i<parents.length;i++)
	{
	  if (i>0)
	    sb.append(",");
	  sb.append(parents[i].toString());
	}
      sb.append(")");
      return new String(sb);
    }

  /** Give a string representation for the dependency type */
  abstract String ruleType();
}

/** A dependency that represents a measurement.  It has no children. */

abstract class MeasureDependency extends Dependency
{
  /** All expressions that use this measure */
  Vector measures = new Vector();

  /** The value of the measurement */
  int measure;

  /** Compute the value of the measurement */
  abstract void compute();

  /** Compute the measurement and redraw any expressions if necessary */
  void recompute(Vector shapes)
    {
      compute();

      // perhaps the redraw should be done in dragPoint()
      for(int i=0;i<measures.size();i++)
	((Expression)measures.elementAt(i)).repaint();
    }

  /** Does this measurement depend only on inputs? */
  boolean isInput()
    {
      for(int i=0;i<parents.length;i++)
	if (!(parents[i].source instanceof ArbitraryDependency))
	  {
	    return false;
	  }

      return true;
    }

  /** Does this measurement depend on any intermediate shapes?
   *  
   *  @param outputShapes  A list of the output shapes
   */
  boolean isValidOutput(Vector outputShapes)
    {
      for(int i=0;i<parents.length;i++)
	if (!(parents[i].source instanceof ArbitraryDependency) &&
	    outputShapes.indexOf(parents[i]) < 0)
	  return false;

      return true;
    }

  public String toString()
    { 
      StringBuffer sb = new StringBuffer(ruleType());
      sb.append("(");

      for(int i=0;i<parents.length;i++)
	{
	  sb.append(parents[i].toString());
	  if (i<parents.length-1)
	    sb.append(",");
	}
      sb.append(")");
      return new String(sb);
    }
}

/** The measurement "PI" */

class PiMeasure extends AngleMeasure
{
  PiMeasure() 
  { 
    type = PI; 
    measure = 180; 
    parents = new Shape[0]; 
    children = new Shape[0];
  }
  void compute() {}
  boolean isAngle(Shape s1, Shape s2, Shape s3) { return false; }
  public String toString() { return "PI"; }
  Unique getEquivalent() { return PI_UNIQUE; }
  String ruleType() { return "PI"; }
}

/** The parent of an input point.  Has no parents. */

class ArbitraryDependency extends Dependency
{
  ArbitraryDependency() { type = ARBITRARY;}
  void recompute(Vector shapes) {}
  String ruleType() { return "Free"; }
}

/** An angle measurement.  Has three points as parents */

class AngleMeasure extends MeasureDependency
{
  AngleMeasure() { type = ANGLE_MEASURE; }

  AngleMeasure(PointShape p1,PointShape p2,PointShape p3)
    {
      type = ANGLE_MEASURE;
      parents = new Shape[3];
      parents[0] = p1;
      parents[1] = p2;
      parents[2] = p3;
      children = new Shape[0];
      compute();
      p1.offspring.addElement(this);
      p2.offspring.addElement(this);
      p3.offspring.addElement(this);
    }

  void compute()
    {
      measure = (int)(Geometry.angle((PointShape)parents[0],
				  (PointShape)parents[1],
				  (PointShape)parents[2])
		   *180 / Math.PI);

    }

  boolean isAngle(Shape s1, Shape s2, Shape s3)
    {
      return (s2 == parents[1] &&
	      ((s1 == parents[0] && s3 == parents[2]) ||
	       (s3 == parents[0] && s1 == parents[2])));
    }

  /** unique representation of this measure within the nullspace
   *  Multiple angles have the same unique representation if they
   *  represent the same angle */
  Unique unique = null;

  /** Get the unique representation of this measure, or create it */
  Unique getEquivalent()
    {
      if (unique != null)
	return unique;

      Vector u = ((PointShape)parents[1]).uniques;

      for(int i=0;i<u.size();i++)
	{
	  Unique ua = (Unique)u.elementAt(i);

	  if (ua.isEquivalent(this))
	    {
	      unique = ua;
	      return ua;
	    }
	}

      unique = new UniqueAngle(this);
      u.addElement(unique);

      return unique;
    }

  String ruleType() { return "angle"; }
}

/** A measurement of the distance between two points */

class DistanceMeasure extends MeasureDependency
{
  DistanceMeasure() { type = DISTANCE_MEASURE; }

  DistanceMeasure(PointShape p1,PointShape p2)
    {
      type = DISTANCE_MEASURE;
      parents = new Shape[2];
      parents[0] = p1;
      parents[1] = p2;
      children = new Shape[0];
      p1.offspring.addElement(this);
      p2.offspring.addElement(this);
      compute();
    }

  void compute()
    {
      measure = (int)Geometry.distance(parents[0],parents[1]);  
    }

  boolean isDistance(Shape s1, Shape s2)
    {
      return
	(s1 == parents[0] && s2 == parents[1]) ||
	(s2 == parents[0] && s1 == parents[1]);
    }

  String ruleType() { return "dist"; }
}

/** The parent of any line shape */

abstract class LineShapeDependency extends Dependency
{
  void recompute(Vector shapes)
    {
      LineShape ls = (LineShape)children[0];

      if (!((parents[0] instanceof PointShape) &&
	    (parents[1] instanceof PointShape)) ||
	  parents[0].valid == false || 
	  parents[1].valid == false)
	{
	  ls.valid = false;
	  successful = false;
	}
      else
	{
	  ls.valid = true;
	  successful = true;
	  ls.move((PointShape)parents[0],(PointShape)parents[1]);
	}
    }
}

class LineSegmentDependency extends LineShapeDependency
{
  LineSegmentDependency() { type = LINE_SEGMENT; }
  String ruleType() { return "LineSegment"; }
}

class LineDependency extends LineShapeDependency
{
  LineDependency() { type = LINE; }
  String ruleType() { return "Line"; }
}

class PerpBiDependency extends LineShapeDependency
{
  PerpBiDependency() { type = PERP_BI; }
  String ruleType() { return "PerpBi"; }
}

class RayDependency extends LineShapeDependency
{
  RayDependency() { type = RAY; }
  String ruleType() { return "Ray"; }
}

class ComplRayDependency extends LineShapeDependency
{
  ComplRayDependency() { type = COMPL_RAY; }
  String ruleType() { return "CompRay";}
}

class CircleDependency extends Dependency
{
  CircleDependency() { type = CIRCLE; }

  void recompute(Vector shapes)
    {
      Circle cir = (Circle)children[0];

      if (!((parents[0] instanceof PointShape) &&
	    (parents[1] instanceof PointShape)) ||
	  parents[0].valid == false || 
	  parents[1].valid == false)
	{
	  successful = false;
	  cir.valid = false;
	}
      else
	{
	  successful = true;
	  cir.valid = true;
	  cir.move((PointShape)parents[0],(PointShape)parents[1]);
	}
    }
  String ruleType() { return "Circle";}
}

class IntersectionDependency extends Dependency
{
  IntersectionDependency() { type = INTERSECTION; }

  /** Recompute the children of the intersection */

  void recompute(Vector shapes)
    {
      int i;

      // check that all parents are valid 
      if (parents[0] == null || parents[1] == null ||
	  parents[0].valid == false || parents[1].valid == false)
	{
	  for(i=0;i<children.length;i++)
	    children[i].valid = false;
	  successful = false;

	  return;
	}

      // Compute the intersection
      PointShape[] ret = Geometry.Intersection(parents[0],parents[1]);

      // check that the correct number of intersection points was returned
      successful = (ret.length == children.length);

      // copy the results
      for(i=0;i<ret.length && i<children.length;i++)
	{
	  ret[i].replaces(children[i]);
	  children[i].valid = true;
	}
      for(;i<children.length;i++)
	children[i].valid = false;
    }

  String ruleType() { return "Intersect"; }
}

class ConstructionDependency extends Dependency
{
  /** The construction that this step represents */
  Construction construction;

  ConstructionDependency() { type = CONSTRUCTION; }

  /** Recompute the children of the construction */
  void recompute(Vector shapes)
    {
      int i;

      // check if all the parents are valid 
      for(i=0;i<parents.length;i++)
	{
	  if (parents[i].valid == false)
	    {
	      successful = false;
	      for(i=0;i<children.length;i++)
		children[i].valid = false;
	      return;
	    }
	}

      try
	{
	  // apply the construction
	  Shape[] s = construction.apply(parents,false);

	  successful = true;

	  for(i=0;i<s.length;i++)
	    {
	      s[i].replaces(children[i]);
	      children[i].valid = true;
	    }
	}
      catch (ConstructionError ce)
	{
	  // the construction failed.
	  successful = false;

	  for(i=0;i<children.length;i++)
	    children[i].valid = false;
	}
    }

  String ruleType() { return '\"'+construction.name+'\"'; }
}

