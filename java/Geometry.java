/* GRACE - Graphical Ruler and Compass Editor
 *
 * Geometry.java
 *
 * Useful functions for analytical geometric computation
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.lang.*;
import java.util.*;
import java.awt.*;

// static functions used for geometric operations
final class Geometry implements Constants
{
  /** Distance between two points */

  static double distance(PointShape p1,double x,double y)
    {
      return distance(p1.x,p1.y,x,y);
    }

  /** Distance between two points */

  static double distance(PointShape p1,int x,int y)
    {
      return distance(p1.x,p1.y,(double)x,(double)y);
    }

  /** Distance between two points */

  static double distance(PointShape p1,PointShape p2)
    {
      return distance(p1.x,p1.y,p2.x,p2.y);
    }

  /** Distance between two points */

  static double distance(double x1,double y1,double x2,double y2)
    {
      double dx = x1-x2;
      double dy = y1-y2;

      return Math.sqrt(dx*dx + dy*dy);
    }

  /** Distance from a point to a circle */

  static double distance(Circle c,PointShape p)
    {
      return Math.abs(c.radius - distance(p,c.x,c.y));
    }

  /** Distance from a point to a circle */

  static double distance(Circle c,double x,double y)
    {
      return Math.abs(c.radius - distance(c.x,c.y,x,y));
    }

  /** Distance from a line shape to a point */

  static double distance(LineShape ls,double x,double y)
    {
      double v1 = x - ls.x1;
      double v2 = y - ls.y1;

      double mag = Math.sqrt(ls.A*ls.A + ls.B*ls.B);

      double k1 = ls.A / mag;
      double k2 = ls.B / mag;

      double distToLine = k1*v1 + k2 *v2;

      double projx = x + distToLine * k1;
      double projy = y + distToLine * k2;

      if (!ls.isPointOnLine(projx,projy))
	{
	  if (ls instanceof LineSegment)
	    distToLine = Math.min(distance(ls.x1,ls.y1,x,y),
				  distance(ls.x2,ls.y2,x,y));
	  else
	    distToLine = distance(ls.x1,ls.y1,x,y);
	}

      return Math.abs(Math.round(distToLine));
    }

  /**  The distance from (x,y) to the line <I>containing</I> ls */

  static double lineDistance(LineShape ls,double x,double y)
    {
      double v1 = x - ls.x1;
      double v2 = y - ls.y1;

      double mag = Math.sqrt(ls.A*ls.A + ls.B*ls.B);

      double k1 = ls.A / mag;
      double k2 = ls.B / mag;

      return k1*v1 + k2 *v2;
    }

  /**  The distance from (x,y) to the line <I>containing</I> ls */

  static double lineDistance(LineShape ls,PointShape p)
    {
      return lineDistance(ls,p.x,p.y);
    }

  /** Compute the angle of three points */

  static double angle(PointShape p1,PointShape apex,PointShape p2)
    {
      // Math.atan2() takes a vector from the origin and computes the
      // polar angle.  atan2(0,y) = 0, atan2(+x,0) = pi/2, atan2(-x,0)=-pi/2
      // atan2(0,-y) = -pi

      // check for a null angle
      if ((p1.x == apex.x && p1.y == apex.y) ||
	  (p2.x == apex.x && p2.y == apex.y))
	return 0;

      double theta1 = Math.atan2(p1.x-apex.x,p1.y-apex.y);
      double theta2 = Math.atan2(p2.x-apex.x,p2.y-apex.y);

      if (theta1 < 0)
	theta1 += 2 * Math.PI;

      if (theta2 < 0)
	theta2 += 2 * Math.PI;

      double diff = Math.abs(theta2 - theta1);

      return Math.min(diff,2*Math.PI-diff);
    }

  /** Generic distance from a shape to a point */

  static double distance(Shape s,PointShape p)
    {
      return distance(s,p.x,p.y);
    }

  /** Generic distance between two shapes */

  static double distance(Shape s1,Shape s2)
    {
      if (s2 instanceof PointShape)
	return distance(s1,((PointShape)s2).x,((PointShape)s2).y);

      Shape first,second;

      if (s2 instanceof LineSegment)
	{
	  first = s1;
	  second = s2;
	}
      else
	{
	  second = s1;
	  first = s2;
	}

      if (second instanceof LineSegment && first instanceof LineSegment)
	{
	  LineSegment l1 = (LineSegment) first;
	  LineSegment l2 = (LineSegment) second;

	  double d1 = distance(l1,l2.x1,l2.y1);
	  double d2 = distance(l1,l2.x2,l2.y2);

	  return Math.min(d1,d2);
	}

      if (second instanceof LineSegment)
	{
	  LineSegment l1 = (LineSegment) second;
	  Circle c1 = (Circle) first;
  
	  double d = distance(l1,c1.x,c1.y) - c1.radius;

	  return Math.max(d,0);
	}

      Circle c1 = (Circle)first;
      Circle c2 = (Circle)second;

      double d= distance(c1.x,c1.y,c2.x,c2.y) - c1.radius - c2.radius;

      return Math.max(d,0);
    }

  /** Generic distance from a shape to a point */

  static double distance(Shape s,double x,double y)
    {
      if (s instanceof Circle)
	return distance((Circle)s,x,y);

      if (s instanceof LineSegment)
	return distance((LineSegment)s,x,y);

      if (s instanceof Line)
	return distance((Line)s,x,y);

      if (s instanceof Ray)
	return distance((Ray)s,x,y);

      if (s instanceof ComplRay)
	return distance((ComplRay)s,x,y);

      //if (s instanceof PointShape)
      return distance((PointShape)s,x,y);
    }

  /** Check if two points are on the same side of a third.
   *
   * @return
   * Let P0 = (x0,x0), P1 = (x1,y1), P2 = (x2,y2)
   * P0, P1, and P2 are assumed to be collinear
   * Returns true if P1 and P2 are on the same side of P0
   */
  static boolean sameSide(double x0,double y0,double x1,double y1,
			  double x2,double y2)
    {
      // dot product (P2-P0) dot (P1-P0)
      return (x1-x0)*(x2-x0)+(y1-y0)*(y2-y0) > 0 ;
    }

  /** Compute the intersection of two shapes, and create partial orders
   *
   * There are four places where Intersection is called:
   * 1. when an intersection is created by the user - both constraints
   *    and POs should be generated
   * 2. when an intersection is created by a construction - only POs
   *    should be generated
   * 3. during drag - neither generated
   * 4. in view; want everything
   */
  static PointShape[] IntersectionPO(Shape shape1,Shape shape2)
    {
      PointShape[] ps = Intersection(shape1,shape2);
      LineShape ls1 = (shape1 instanceof LineShape ? (LineShape)shape1 : null);
      LineShape ls2 = (shape2 instanceof LineShape ? (LineShape)shape2 : null);
      PartialOrder R,S;

      if (ps.length == 1)
	{
	  if (ls1 != null)
	    {
	      R = new PartialOrder(ps[0]);
	      ls1.addPointOnShape(R);
	    }

	  if (ls2 != null)
	    {
	      R = new PartialOrder(ps[0]);
	      ls2.addPointOnShape(R);
	    }
	}
      else
	if (ps.length == 2)
	  {
	    if (ls1 != null)
	      {
		R = new PartialOrder(ps[0]);
		S = new PartialOrder(ps[1]);
		ls1.addPointsOnShape(R,S);
	      }

	    if (ls2 != null)
	      {
		R = new PartialOrder(ps[0]);
		S = new PartialOrder(ps[1]);
		ls2.addPointsOnShape(R,S);
	      }
	  }

      return ps;
    }

  /** Intersect two shapes, generate partial orders, generate
   *  constraints, and name the output shapes
   *
   * @param shape1  The first input shape
   * @param shape2  The second input shape
   * @param cf      The constraint frame, to add new constraints to
   * @param names   List of names for output points, or null
   * @param prefix    Prefix for naming output shapes
   * @param firstLabel  The first index to try for naming output points
   * @param usedNames   The table names that are already used; to be
   *                updated
   * @return An array of intersection points
   */

  static PointShape[] Intersection(Shape shape1,Shape shape2,
				   ConstraintFrame cf,
				   int firstLabel,String[] names,
				   Hashtable usedNames,String prefix)
    {
      PointShape[] ps = Intersection(shape1,shape2);

      // name the output points

      for(int i=0;i<ps.length;i++)
	{
	  if (names != null)
	    {
	      ps[i].label = names[i];
	      usedNames.put(ps[i].label,ps[i]);
	    }
	  else
	    ps[i].label = DrawPanel.uniqueName("I",usedNames,firstLabel+i);
	}

      PartialOrder R, S;
      LineShape ls1 = (shape1 instanceof LineShape ? (LineShape)shape1 : null);
      LineShape ls2 = (shape2 instanceof LineShape ? (LineShape)shape2 : null);

      Vector v1 = new Vector();
      Vector v2 = new Vector();
      Vector v3 = null;

      // generate POs for the new points

      if (ps.length == 1)
	{
	  if (ls1 != null)
	    {
	      R = new PartialOrder(ps[0]);
	      ls1.addPointOnShape(R);
	      v1 = R.makeLineConstraints();
	    }

	  if (ls2 != null)
	    {
	      R = new PartialOrder(ps[0]);
	      ls2.addPointOnShape(R);
	      v2 = R.makeLineConstraints();
	    }
	}
      else
	if (ps.length == 2)
	  {
	    if (ls1 != null)
	      {
		R = new PartialOrder(ps[0]);
		S = new PartialOrder(ps[1]);
		ls1.addPointsOnShape(R,S);
		v1 = R.makeLineConstraints();
		v2 = S.makeLineConstraints();
		R.mergeVectors(v1,v2);
	      }

	    if (ls2 != null)
	      {
		R = new PartialOrder(ps[0]);
		S = new PartialOrder(ps[1]);
		ls2.addPointsOnShape(R,S);
		v2 = R.makeLineConstraints();
		v3 = S.makeLineConstraints();
		R.mergeVectors(v2,v3);
	      }
	  }
	  
      PartialOrder.mergeVectors(v1,v2);

      // generate some more automatic constraints

      for(int i=0;i<ps.length;i++)
	{
	  if (shape1 instanceof Circle)
	    {
	      Constraint c = makeCircleConstraint(ps[i],(Circle)shape1); 

	      if (c != null)
		v1.addElement(c);
	    }
	  else if (shape1 instanceof PerpBi)
	    {
	      Constraint c = makePerpBiConstraint(ps[i],(PerpBi)shape1);
	      
	      if (c != null)
		v1.addElement(c);
	    }

	  if (shape2 instanceof Circle)
	    {
	      Constraint c = makeCircleConstraint(ps[i],(Circle)shape2);

	      if (c != null)
		v1.addElement(c);
	    }
	  else if (shape2 instanceof PerpBi)
	    {
	      Constraint c = makePerpBiConstraint(ps[i],(PerpBi)shape2);
	      
	      if (c != null)
		v1.addElement(c);
	    }
	}

      boolean firstNew = true;

      // add the new constraints to the constaint frame
      
      for(int i=0;i<v1.size();i++)
	{
	  Constraint c = (Constraint)v1.elementAt(i);

	  if (!cf.nullspace.follows(c))
	    {
	      if (firstNew)
		{
		  firstNew = false;
		  cf.addBlankStep();
		}
	      cf.addStep(c,prefix);
	      cf.addProvenConstraint(c);
	    }
	}

      return ps;
    }

  /** Base function to generate the intersectio points of two shapes
   *  Only points can be produced as output.
   */

  static PointShape[] Intersection(Shape shape1,Shape shape2)
    {
      PointShape[] returnValue = new PointShape[0];

      Shape s1, s2;

      // rearrange the inputs a bit

      if ((shape2 instanceof LineShape && shape1 instanceof Circle) ||
	  (shape2 instanceof PointShape))
	{
	  s1 = shape2;
	  s2 = shape1;
	}
      else
	{
	  s1 = shape1;
	  s2 = shape2;
	}

      if (s1 instanceof PointShape)
	{
	  if (s2.isPointOnShape(((PointShape)s1).x,
				((PointShape)s1).y))
	    {
	      returnValue = new PointShape[1];

	      returnValue[0] = new PointShape(((PointShape)s1).x,
					      ((PointShape)s1).y);
	    }

	  return returnValue;
	}

      if (s1 instanceof LineShape && s2 instanceof LineShape)
	{
	  LineShape l1 = (LineShape)s1;
	  LineShape l2 = (LineShape)s2;

	  double x = l1.B * l2.C - l1.C * l2.B;
	  double y = l2.A * l1.C - l1.A * l2.C;
	  double z = l1.A * l2.B - l2.A * l1.B;

	  if (z == 0)          // don't handle collinear yet
	    return returnValue;

	  x /= z;
	  y /= z;

	  if (!l1.isPointOnLine(x,y) || !l2.isPointOnLine(x,y))
	    return returnValue;

	  returnValue = new PointShape[1];

	  returnValue[0] = new PointShape(x,y);

	  return returnValue;
	}

      if (s1 instanceof LineShape && s2 instanceof Circle)
	{
	  LineShape ls = (LineShape)s1;
	  Circle cir = (Circle)s2;

	  PointShape p1, p2;

	  double v1 = cir.x - ls.x1;
	  double v2 = cir.y - ls.y1;

	  double magsq = ls.A*ls.A + ls.B*ls.B;

	  double k1mag = ls.A;
	  double k2mag = ls.B;

	  double dmag = Math.abs(k1mag*v1 + k2mag*v2);

	  if (dmag*dmag > cir.radius*cir.radius*magsq)
	    return returnValue;

	  double dk1 = dmag*k1mag/magsq;
	  double dk2 = dmag*k2mag/magsq;

	  p1 = new PointShape(cir.x + dk1,cir.y + dk2);

	  p2 = new PointShape(cir.x - dk1,cir.y - dk2);

	  PointShape pointOnLine;

	  if (lineDistance(ls,p1) < 2)
//	  if (p1.p.x * ls.A + p1.p.y * ls.B + ls.C < 2)
	    pointOnLine = p1;
	  else
	    pointOnLine = p2;

	  if (dmag*dmag == cir.radius*cir.radius*magsq)
	    {
	      if (ls.isPointOnShape(pointOnLine))
		
{
		  returnValue = new PointShape[1];
		  
		  returnValue[0] = pointOnLine;
		}
	      
	      return returnValue;
	    }
	  
	  double dist = Math.sqrt((cir.radius*cir.radius - 
				   dmag*dmag/magsq)/magsq);
	  dk1 = dist*k1mag;
	  dk2 = dist*k2mag;

	  if (ls instanceof ComplRay)
	    // swap the order of the points for the compl. ray
	    // so that the point nearest to the endpoint of the ray
	    // will be i1.
	    {
	      dk1 *= -1;
	      dk2 *= -1;
	    }

	  // these are two points on the line and circle, but
	  // they might not be on the line shape.

	  PointShape i1 = new PointShape(pointOnLine.x + dk2,
					 pointOnLine.y - dk1);

	  PointShape i2 = new PointShape(pointOnLine.x - dk2,
					 pointOnLine.y + dk1);

	  boolean i1OnShape = ls.isPointOnLine(i1);
	  boolean i2OnShape = ls.isPointOnLine(i2);

	  int count = (i1OnShape ? 1 : 0);

	  if (i2OnShape)
	    count++;

	  returnValue = new PointShape[count];

	  if (count > 0)
	    returnValue[0] = (i1OnShape? i1 : i2);

	  if (count == 2)
	    returnValue[1] = i2;

	  return returnValue;
	}

      // otherwise, we've got two circles

      Circle c1 = (Circle)s1;
      Circle c2 = (Circle)s2;
      double dist = distance(c1.x,c1.y,c2.x,c2.y);

      double r1 = c1.radius;
      double r2 = c2.radius;

      if (dist == 0)
	return returnValue;

      // check for no intersection
      if (r1 + r2 < dist ||
	  r1 > r2 + dist ||
	  r2 > r1 + dist)
	return returnValue;

      double a = dist/2 + (r1*r1 - r2*r2)/(2*dist);
      double c = Math.sqrt(r1*r1 - a*a);

      double v1 = (c2.x - c1.x) / dist;
      double v2 = (c2.y - c1.y) / dist;

      double p1 = c1.x + a*v1;
      double p2 = c1.y + a*v2;

      if (r1 + r2 == dist ||
	  r1 + dist == r2 ||
	  r2 + dist == r1)
	{
	  returnValue = new PointShape[1];

	  returnValue[0] = new PointShape(p1,p2);

	  return returnValue;
	}

      v1 = v1 * c;
      v2 = v2 * c;

      returnValue = new PointShape[2];
      returnValue[0] = new PointShape(p1+v2,p2-v1);
      returnValue[1] = new PointShape(p1-v2,p2+v1);

      return returnValue;
    }

  /** Generate a constraint for a point that is on a circle, that
   *  states that the distance from the new point to the center
   *  is the equal to the distance from the center to another point
   *  on the circle.
   *
   *  If no other point on the circle is known, then then set
   *  pointOnCircle to be this point
   *
   *  @return The new constraint, or null if it could not be created
   */
  
  static Constraint makeCircleConstraint(PointShape p,Circle c)
    {
      if (c.centerPoint == null)
	return null;

      if (c.pointOnCircle == null)
	{
	  c.pointOnCircle = p;
	  return null;
	}

      DistanceMeasure dm = c.centerPoint.getDistanceMeasure(p);
      DistanceMeasure rm = c.centerPoint.getDistanceMeasure(c.pointOnCircle);

      Constraint ct = new Constraint();

      ct.add(dm,1);
      ct.add(rm,-1);

      return ct;
    }

  /** Generate a new constraint from a point point on a perpendicular
   *  bisector.
   *
   * @return The new constraint, or null if it could not be generated */

  static Constraint makePerpBiConstraint(PointShape p,PerpBi b)
    {
      if (b.origins[0] == null || b.origins[1] == null)
	return null;

      DistanceMeasure dm1 = b.origins[0].getDistanceMeasure(p);
      DistanceMeasure dm2 = b.origins[1].getDistanceMeasure(p);

      Constraint ct = new Constraint();

      ct.add(dm1,1);
      ct.add(dm2,-1);

      return ct;
    }
}

/** A transformation matrix for converting screen coordinates to
 *  virtual geometry coordinates and vice-versa */

class Transform implements Constants
{
  /** The proportion by which to enlarge images (zoom in) */
  double scaleFactor = 1;

  /** Where the center of the viewpoint should appear in the virtual
   *  plane (x-coord) */
  double virtualOriginX = 0;

  /** Where the center of the viewpoint should appear in the virtual
   *  plane (y-coord) */
  double virtualOriginY = 0;

  /** Coordinates of the center of the draw panel.  Maps to the
   *  virtual origin in the virtual plane (x-coord) */
  int screenOriginX = DP_WIDTH/2;

  /** Coordinates of the center of the draw panel.  Maps to the
   *  virtual origin in the virtual plane (x-coord) */
  int screenOriginY = DP_HEIGHT/2;

  /** Width of the drawpanel */
  int screenWidth;

  /** Height of the drawpanel */
  int screenHeight;

  /** Precomputed value, dependent only on the transformation matrix */
  double xoffset;

  /** Precomputed value, dependent only on the transformation matrix */
  double yoffset;

  public String toString()
  {
    return "["+scaleFactor+"x,("+virtualOriginX+","+virtualOriginY+"),("
      +xoffset+","+yoffset+"),("+screenOriginX+","+screenOriginY+")]";
  }

  /** Adjust the matrix to a new screen size
   *
   * @param d  The new dimensions of the drawPanel */

  void resize(Dimension d)
  {
    screenWidth = d.width;
    screenHeight = d.height;

    reset();
  }

  /** Precompute the values of xoffset and yoffset */

  void reset()
  {
    xoffset = screenOriginX-scaleFactor*virtualOriginX;
    yoffset = screenOriginY-scaleFactor*virtualOriginY;
  }

  /** Convert a virtual X-coordinate to a screen X-coordinate */
  int virtualToScreenX(double X)
  {
    return (int)(scaleFactor*X+xoffset);
  }

  /** Convert a virtual Y-coordinate to a screen Y-coordinate */
  int virtualToScreenY(double Y)
  {
    return (int)(scaleFactor*Y+yoffset);
  }

  /** Convert a screen X-coordinate to a virtual X-coordinate */
  double screenToVirtualX(int X)
  {
    return (X-screenOriginX)/scaleFactor + virtualOriginX;
  }

  /** Convert a screen Y-coordinate to a virtual Y-coordinate */
  double screenToVirtualY(int Y)
  {
    return (Y-screenOriginY)/scaleFactor + virtualOriginY;
  }

  /** Extend a ray to the border of the drawing area
   *
   *  @param vx1  The virtual x-coordinate of the ray endpoint
   *  @param vy1  The virtual y-coordinate of the ray endpoint
   *  @param vx2  The virtual x-coordinate of a point on the ray
   *  @param vy2  The virtual y-coordinate of a point on the ray
   *  @return   A point on the ray, off the screen */
  Point extendRay(double vx1,double vy1,double vx2,double vy2)
    {
      // convert the inputs to screen coordinates
      int x1 = virtualToScreenX(vx1);
      int x2 = virtualToScreenX(vx2);
      int y1 = virtualToScreenY(vy1);
      int y2 = virtualToScreenY(vy2);
      
      double dx = x2 - x1;
      double dy = y2 - y1;

      if (dx == 0 && dy == 0)
	return null;

      double k = -1;

      // D is a parameter to the parameterized form of this ray
      // (i.e. pointOnRay(D) = (x1,y1) + D * (dx, dy);  D >= 0)

      double D[] = new double[4];

      // compute the parameter of the intersection of the ray
      // with each screen boundary

      if (dx != 0)
	{
	  D[0] = -x1/dx;
	  D[1] = D[0] + screenWidth/dx;
	}
      else
	{
	  D[0] = -1;
	  D[1] = -1;
	}

      if (dy != 0)
	{
	  D[2] = -y1/dy;
	  D[3] = D[2] + screenHeight/dy;
	}
      else
	{
	  D[2] = -1;
	  D[3] = -1;
	}

      // We want the smallest positive value of D[] if the point (x1,y1)
      // is within the viewing rectangle.
      // If the point is outside and the ray is horizontal or vertical,
      // we want the largest D[].
      // Otherwise, we want the second largest D[]
      if (x1 >= 0 && x1 < screenWidth && y1 >= 0 && y1 < screenHeight)
	{
	  for(int i=0;i<4;i++)
	    if (D[i] > 0 && (D[i] < k || k < 0))
	      k = D[i];
	}
      else
	{
	  if (dx * dy == 0)
	    {
	      for(int i=0;i<4;i++)
		if (D[i] > k)
		  k = D[i];
	    }
	  else
	    {
	      for(int i=0;i<3;i++)
		{
		  for(int j=i+1;j<4;j++)
		    {
		      if (D[i] < 0)
			break;
		      
		      if (D[j] < D[i])
			{
			  double t = D[j];
			  D[j] = D[i];
			  D[i] = t;
			}
		    }
		}

	      k = D[2];
	    }
	}

      if (k < 0)
	return null;

      // choose the largest value of D, in case the ray begins outside
      // the view area

      Point p = new Point((int)(x1+k*dx),(int)(y1+k*dy));

      return p;
    }
}
