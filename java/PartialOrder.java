/* GRACE - Graphical Ruler and Compass Editor
 *
 * PartialOrder.java
 *
 * The partial order classes, for representing the known orderings of
 * points on a line.
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.util.Vector;

/** A node within a partial order.  (This class name is a bit misleading;
 *  there is no overall data structure that "contains" the partial order.)
 *  Each point on a line shape has a pointer to a partial order node on
 *  that line.  The terms "left" and "right" are used to indicate order
 *
 *  Partial orders are used to represent the known ordering of points along
 *  a line.
 *
 *  Each point will have a pointer to a different PartialOrder, for each
 *  line that the point is on.
 *
 *  Since Lines and PerpBi contain no ordering information whatsoever, they
 *  do not generate any partial order information.
 */

public class PartialOrder implements Constants
{
  /** The point corresponding to this node. */
  PointShape p;

  /** The list of PartialOrders on the left side of this node */
  Vector leftSide = new Vector();

  /** The list of PartialOrders on the right side of this node */
  Vector rightSide = new Vector();

  /** Is this a new PartialOrder node?  If so, then it will be ignored
   *  when making new constraints */
  boolean isNew = true;

  PartialOrder(PointShape p1)
    {
      p = p1;
      p1.POs.addElement(this);
    }

  /** Connect two partial order nodes together.  L is on the left side of R */

  static void link(PartialOrder L,PartialOrder R)
    {
      L.rightSide.addElement(R);
      R.leftSide.addElement(L);
    }

  /** Disconnect two partial order nodes.  L was on the left side of R */

  static void unlink(PartialOrder L,PartialOrder R)
    {
      L.rightSide.removeElement(R);
      R.leftSide.removeElement(L);
    }

  /** Delete this point from the partial order, by unlinking it from its
   *  neighbors, and linking all pairs of left side nodes and right side
   *  nodes */
  
  void delete()
    // delete this point from the partial order
    {
      for(int i=0;i<leftSide.size();i++)
	{
	  PartialOrder L = (PartialOrder)leftSide.elementAt(i);

	  for(int j=0;j<rightSide.size();j++)
	    {
	      PartialOrder R = (PartialOrder)rightSide.elementAt(j);

	      link(L,R);
	    }
	}

      for(int i=0;i<leftSide.size();i++)
	{
	  PartialOrder L = (PartialOrder)leftSide.elementAt(i);

	  L.rightSide.removeElement(this);
	}

      for(int j=0;j<rightSide.size();j++)
	{
	  PartialOrder R = (PartialOrder)rightSide.elementAt(j);

	  R.leftSide.removeElement(this);
	}

      leftSide.removeAllElements();
      rightSide.removeAllElements();

      // there is a bit of a memory leak here - 
      // this will leave PartialOrders that have no outgoing nodes
      // however, we can't just delete them, because they might be
      // visible from a LineShape, if the LineShape and _one_ of the
      // points used to create it remains from a construction
    }

  /** Compute the transitive closure consisting of all points on the right
   *  side of this one */

  Vector computeRightTC()
    {
      Vector ret = new Vector();

      for(int i=0;i<rightSide.size();i++)
	{
	  PartialOrder PO = (PartialOrder)rightSide.elementAt(i);
	  
	  ret.addElement(PO);

	  mergeVectors(ret,PO.computeRightTC());
	}

      return ret;
    }

  /** Compute the transitive closure consisting of all points on the left
   *  side of this one */

  Vector computeLeftTC()
    {
      Vector ret = new Vector();

      for(int i=0;i<leftSide.size();i++)
	{
	  PartialOrder PO = (PartialOrder)leftSide.elementAt(i);
	  
	  ret.addElement(PO);

	  mergeVectors(ret,PO.computeLeftTC());
	}

      return ret;
    }

  /** Find the partial order node for a point on the same line as this one,
   *  or null if none exists */

  PartialOrder findPoint(PointShape ps)
    {
      PartialOrder r = findOnLeft(ps);

      return (r == null ? findOnRight(ps) : r);
    }

  /** Find the partial order node for a point on the same line as this, to
   *  the left of this node.  Null is returned if the point is not found. */

  PartialOrder findOnLeft(PointShape ps)
    {
      if (p == ps)
	return this;

      for(int i=0;i<leftSide.size();i++)
	{
	  PartialOrder r = ((PartialOrder)leftSide.elementAt(i)).findOnLeft(ps);
	  if (r != null)
	    return r;
	}

      return null;
    }

  /** Find the partial order node for a point on the same line as this, to
   *  the right of this node.  Null is returned if the point is not found. */

  PartialOrder findOnRight(PointShape ps)
    {
      if (p == ps)
	return this;

      for(int i=0;i<rightSide.size();i++)
	{
	  PartialOrder r = ((PartialOrder)rightSide.elementAt(i)).findOnRight(ps);
	  if (r != null)
	    return r;
	}
 
      return null;
    }

  /** Check if a point is on the given side of this point */

  boolean isOnSameSide(PointShape ps,boolean lookRight)
    {
      return (lookRight ? isOnRight(ps) : isOnLeft(ps) );
    }

  /** Check if the point is on the left side of this point */

  boolean isOnLeft(PointShape ps)
    {
      return findOnLeft(ps) != null;
    }

  /** Check if the point is on the right side of this point */

  boolean isOnRight(PointShape ps)
    {
      return findOnRight(ps) != null;
    }

  /** Construct a list of points to the immediate left of this one, pretending
   *  that New nodes do not exist yet.
   *
   * i.e. let F(p) = p  if p is not new
   *                 notNewLeftSide(p)  if p is new
   *
   *  notNewLeftSide(p) = map(F,p.leftSide)
   *
   *  This may include duplicates.
   */

  Vector notNewLeftSide()
       // note: may include duplicates
  {
    Vector r = new Vector(leftSide.size());

    for(int i=0;i<leftSide.size();i++)
      {
	PartialOrder L = (PartialOrder)leftSide.elementAt(i);

	if (L.isNew)
	  mergeVectors(r,L.notNewLeftSide());
	else
	  r.addElement(L);
      }

    return r;
  }

  /** Construct a list of points to the immediate right of this one, pretending
   *  that New nodes do not exist yet.
   *
   * i.e. let F(p) = p  if p is not new
   *                 notNewRightSide(p)  if p is new
   *
   *  notNewRightSide(p) = map(F,p.rightSide)
   *
   *  This may include duplicates.
   */

  Vector notNewRightSide()
  {
    Vector r = new Vector(rightSide.size());

    for(int i=0;i<rightSide.size();i++)
      {
	PartialOrder R = (PartialOrder)rightSide.elementAt(i);

	if (R.isNew)
	  mergeVectors(r,R.notNewRightSide());
	else
	  r.addElement(R);
      }

    return r;
  }

  /** Generate all necessary constraints for the partial order node.
   *
   *  This method assumes that all possible constraints for the current
   *  partial order have been generated, except for those involving this
   *  point.
   *
   *  New nodes are treated as not yet existing within the partial order,
   *  and will not be used in any constraints.  (This is not quite true.
   *  They will be included in the transitive closures, which means some
   *  redundant constraints. )
   *
   *  Let X < Y mean X is immediately to the left of Y (not counting new
   *  nodes).  Let X <* Y mean that X is somewhere to the left of Y.
   *
   *  The constraints that will be added will all constraints be of the form:
   *  dist(P,Q) + dist(Q,R) = dist(P,R), where:
   *
   *  1.  Q is this node, and P < Q < R
   *  2.  P is this node, and P < Q <* R
   *  3.  R is this node, and P <* Q < R
   *
   *  The proof that this correctly generates all necessary constraints
   *  is left as an exercise to the reader.
   */

  // make all the necessary contraints for this partial order
  Vector makeLineConstraints()
    {
      isNew = false;

      Vector left = notNewLeftSide();
      Vector right = notNewRightSide();

      Vector constraints = new Vector();

      for(int i=0;i<left.size();i++)
	{
	  PartialOrder L = (PartialOrder)left.elementAt(i);

	  for(int j=0;j<right.size();j++)
	    {
	      PartialOrder R = (PartialOrder)right.elementAt(j);

	      constraints.addElement(newDistanceConstraint(L,R));
	    }
	}

      for(int i=0;i<left.size();i++)
	{
	  PartialOrder L = (PartialOrder)left.elementAt(i);
	  Vector TC = L.computeLeftTC();

	  for(int j=0;j<TC.size();j++)
	    {
	      PartialOrder A = (PartialOrder)TC.elementAt(j);

	      // we could probably skip this if A is new

	      constraints.addElement(L.newDistanceConstraint(A,this));
	    }
	}

      for(int i=0;i<right.size();i++)
	{
	  PartialOrder R = (PartialOrder)right.elementAt(i);
	  Vector TC = R.computeRightTC();

	  for(int j=0;j<TC.size();j++)
	    {
	      PartialOrder B = (PartialOrder)TC.elementAt(j);

	      // we could probably skip this if B is new

	      constraints.addElement(R.newDistanceConstraint(B,this));
	    }
	}

      return constraints;
    }

  /** Generate a distance constraint:
   *  dist(L,R) = dist(L,this) + dist(R,this)
   */

  Constraint newDistanceConstraint(PartialOrder L,PartialOrder R)
    {
      DistanceMeasure dl,dr,dm;

      dl = p.getDistanceMeasure(L.p);
      dr = p.getDistanceMeasure(R.p);
      dm = R.p.getDistanceMeasure(L.p);

      Constraint c = new Constraint();
      c.add(dl,1);
      c.add(dr,1);
      c.add(dm,-1);

      return c;

    }

  /** Copy the contents of one vector into another.
   *
   * @param v2  The source vector
   * @param v1  The destination vector
   */

  static void mergeVectors(Vector v1, Vector v2)
    {
      for(int i=0;i<v2.size();i++)
	v1.addElement(v2.elementAt(i));
    }


  /** For debugging. */
  int lastSearch = 0;

  /** For debugging. */
  static int searchCounter = 0;

  /** For debugging. */
  public String toString() 
    { 
      searchCounter ++;

      StringBuffer sb = new StringBuffer();

      traverse(sb);

      return new String(sb);
    }

  /** For debugging. */
  void traverse(StringBuffer sb)
    {
      if (lastSearch >= searchCounter)
	return;

      lastSearch = searchCounter;

      connections(sb);

      for(int i=0;i<leftSide.size();i++)
	((PartialOrder)leftSide.elementAt(i)).traverse(sb);

      for(int i=0;i<rightSide.size();i++)
	((PartialOrder)rightSide.elementAt(i)).traverse(sb);
    }
     
  /** For debugging. */
  void connections(StringBuffer sb)
    {
      sb.append("Point "+p.toString());

      if (isNew)
	sb.append(" (New)\n");
      else
	sb.append("\n");

      for(int i=0;i<leftSide.size();i++)
	sb.append("  <-- "+((PartialOrder)leftSide.elementAt(i)).p.toString()
		  +"\n");
	  
      for(int i=0;i<rightSide.size();i++)
	sb.append("  --> "+((PartialOrder)rightSide.elementAt(i)).p.toString()
		  +"\n");
	  
      sb.append("\n");
    }
}
