/* GRACE - Graphical Ruler and Compass Editor
 *
 * LibrayParser.java
 *
 * Code for parsing a library
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.util.*;
import java.io.*;
import java.awt.*;

/** Just what it says, a parser for construction libraries */

public class LibraryParser implements Constants
{
  /** The table of already-defined constructions */
  Hashtable constructions;

  /** List of new construction names read from the library */
  Vector addConsts;

  /** The construction currently being read */
  Construction current;

  /** The lexical analyzer */
  StreamTokenizer st;

  /** The table of rule names */
  Hashtable ruleTable;

  /** Have any duplicate construction names been encounted? */
  boolean overwritten = false;

  /** Parse constructions from a stream
   *
   * @param oldConsts  The table of already-defined constructions, to be
   *                   used and updated.
   * @param input      The input stream to read from
   * @param newConsts  Initially empty, to be filled with a list of the 
   *                   names of the new constructions
   * @return The result or any exception message from parsing
   */

  String parseStream(Hashtable oldConsts,InputStream input,Vector newConsts)
  {
    constructions = oldConsts;
    addConsts = newConsts;

    String result = "Done.";
   
    try
      {
	// parse the input stream
	readStream(input);

	if (overwritten)
	  result = "Warning: Duplicate constructions overwritten";
      }
    // handle the possible exceptions
    catch (IOException ex)
      {
	result = "Network Exception: "+ex.getMessage();
      }
    catch (ParseError ex)
      { 
	result = "Parse Error: "+ex.getMessage();
      }
    catch (SecurityException ex)
      {
	result = "Security Exception: "+ex.getMessage();
      }

    return result;
  }

  /** Helper function that reads the input stream */

  void readStream(InputStream input) 
       throws IOException, ParseError
    {
      // Switch the comment on the following lines for debugging

//      st = (StreamTokenizer)new DebugStreamTokenizer(input);
      st = new StreamTokenizer(input);
    
      // initialize the tokenizer
      st.whitespaceChars(' ',' ');
      st.whitespaceChars('#',',');
      st.whitespaceChars(':','<');
      st.whitespaceChars('>','@');
      st.whitespaceChars('[','`');
      st.whitespaceChars('\t','\t');
      st.whitespaceChars(':',':');
//      st.lowerCaseMode(true);     
      st.eolIsSignificant(true);
      st.quoteChar('\"');
      st.commentChar(';');

      // loop through the input stream
      while(true)
	{
	  while (st.nextToken() == st.TT_EOL)
	    ;

	  // read the construction statement

	  if (st.ttype == st.TT_EOF)
	    break;	  

	  if (st.ttype != st.TT_WORD || !st.sval.equals("Construction"))
	    throw new ParseError(st.lineno(),
				 "Missing \"Construction\" statement");

	  // read the construction name

	  if (st.nextToken() != '\"')
	    throw new ParseError(st.lineno(),
				 "Construction name must be in quotes");

	  if (ConstructionPanel.isPrimitive(st.sval))
	    throw new ParseError(st.lineno(),
				 "Invalid construction name: "+st.sval);

	  current = new Construction();
	  current.name = st.sval;

	  if (st.nextToken() != st.TT_EOL)
	    throw new ParseError(st.lineno(),"End of line missing");

	  while (st.nextToken() == st.TT_EOL)
	    ;

	  // read the construction description

	  while (st.ttype == '\"')
	    {
	      current.description = current.description+st.sval+'\n';

	      while (st.nextToken() == st.TT_EOL)
		;

	    }

	  // read the body of the construction
	  
	  readRules();

	  // add the construction to the hashtable and the new constructions

	  if (constructions.put(current.name,current) == null)
	    addConsts.addElement(current.name);
	  else
	    overwritten = true;
	}
    }

  /** Parse the body of the construction */

  void readRules() throws IOException, ParseError
    {
      ruleTable = new Hashtable();   // lookup-table for rule names
      int stepNumber = 0;                     // what step are we on?
      Vector ruleNames;              // output names for the current rule
      int numNames;                  // length of ruleNames
      String firstName = new String("");  // the first output name

//      ruleTable.put("PI",PI_RULE);   // add PI to the list of rules

      // read the input points
      readInputRule();

      stepNumber = current.numberOfInputs;

      // read the input constraints
      readAssumptions();

      while(st.nextToken() == st.TT_EOL)
	;
      
      if (st.ttype != st.TT_WORD || !st.sval.equals("Steps"))
	throw new ParseError(st.lineno(),"Missing steps declaration");

      while(true)
	{
	  // read the names of the outputs of this rule
	  ruleNames = readRuleNames();

          numNames = ruleNames.size();

	  	  // check if this is the output rule
          if (numNames > 0)
	    {
	      firstName = new String((String)ruleNames.elementAt(0));

	      if (firstName.equals("Output"))
		break;
	    }

	  // read the rule
	  readRule(stepNumber++,ruleNames);
	}

      // read the output shapes
      readOutputRule(stepNumber++,firstName);

      // the output constraints
      readConclusions();
    }

  /** Read the input constraints */

  void readAssumptions() throws ParseError, IOException
  {
    while(true)
      {
	while (st.nextToken() == st.TT_EOL)
	  ;

	if (st.ttype != st.TT_WORD || !st.sval.equals("Assume"))
	  {
	    st.pushBack();
	    return;
	  }

	current.inputConstraints.addElement(readConstraint());
      }
  }

  /** Read the output constraints */

  void readConclusions() throws ParseError, IOException
  {
    while(true)
      {
	while (st.nextToken() == st.TT_EOL)
	  ;
	
	if (st.ttype != st.TT_WORD || !st.sval.equals("Conclude"))
	  {
	    st.pushBack();
	    return;
	  }

	current.outputConstraints.addElement(readConstraint());
      }
  }

  /** Read the output names for a rule */
  
  Vector readRuleNames()
    throws ParseError, IOException
    {
      Vector names = new Vector();

      while(true)
	{
	  while (st.nextToken() == st.TT_EOL)
	    ;

	  if (st.ttype == st.TT_EOF)
	    throw new ParseError(st.lineno(),
				 "Missing output statement at end of file");

	  // check if a construction name is provided

	  if (st.ttype == '\"')
	    {
	      st.pushBack();
	      return names;
	    }

	  if (st.ttype == '=')
	    break;

	  if (st.ttype != st.TT_WORD)
	    throw new ParseError(st.lineno(),"Invalid rule syntax");

	  // Check for a force rule

	  if (st.sval.equals("Force"))
	    {
	      names.addElement("Force");
	      st.pushBack();
	      return names;
	    }

	  if (st.sval.equals("Output"))
	    {
	      names.addElement("Output");
	      return names;
	    }

	  if (st.sval.equals("Construction"))
	    throw new ParseError(st.lineno(),"Missing output statement or invalid rule name");

	  if(st.sval.equals("PI"))
	    throw new ParseError(st.lineno(),"Illegal use the reserved name PI");

	  // check for a duplicate rule name
	  if (ruleTable.get(st.sval) != null)
	    throw new ParseError(st.lineno(),
				 "Duplicate shape name \""+st.sval+'\"');

	  names.addElement(st.sval);
	}

      for(int i=0;i<names.size();i++)
	{
	  String s1 = (String)names.elementAt(i);

	  for(int j=i+1;j<names.size();j++)
	    {
	      String s2 = (String)names.elementAt(j);

	      if (s1.equals(s2))
		throw new ParseError(st.lineno(),
				     "Duplicate shape name \""+s1+'\"');
	    }
	}

      return names;
    }

  /** Read a constraint, and generate a ConstraintRule */

  ConstraintRule readConstraint()
    throws ParseError, IOException
    {
      ConstraintRule cr = new ConstraintRule();

      // temporary variables
      int nextWeight;        // weight for the next constraint
      Object nextArg;        
      Rule nextRule;

      int type = ARBITRARY;  // the constraint type

      // which side of the constraint are we reading?
      boolean leftSide = true;
      
      while(st.nextToken() != st.TT_EOL)
	{
	  if (st.ttype == st.TT_EOF)
	    throw new ParseError(st.lineno(),"Missing output statement");

	  if (st.ttype == '=')
	    {
	      if (!leftSide)
		throw new ParseError(st.lineno(),"Extra '=' in constraint");

	      // read right side of the constraint

	      leftSide = false;

	      continue;
	    }

	  if (st.ttype == st.TT_NUMBER)
	    {
	      nextWeight = (int)st.nval;   // read the coefficient
	      st.nextToken();
	      
	      if (nextWeight < 0)
		throw new ParseError(st.lineno(),
				     "Coefficients must be positive");

	      if (nextWeight == 0)
		{
		  if (st.ttype != '=')
		    throw new ParseError(st.lineno(),
					 "Coefficients must be positive");

		  continue;
		}
	    }
	  else
	    nextWeight = 1;                // default weight
	  
	  if (st.ttype != st.TT_WORD)
	    throw new ParseError(st.lineno(),"Missing \"dist\" or \"angle\"");

	  int newType;

	  // read the measure type

	  if (st.sval.equals("dist"))
	    newType = DISTANCE_MEASURE;
	  else
	    if (st.sval.equals("angle"))
	      newType = ANGLE_MEASURE;
	    else
	      if (st.sval.equals("PI"))
		{
		  newType = PI;

		  if (type == DISTANCE_MEASURE)
		    throw new ParseError(st.lineno(),
					 "Cannot mix expression types");

		  cr.addPi(leftSide,nextWeight);

		  type = ANGLE_MEASURE;

		  continue;
		}
	      else
		throw new ParseError(st.lineno(),
				   "Missing \"dist\" or \"angle\"");

	  if (type != ARBITRARY && newType != type)
	    throw new ParseError(st.lineno(),"Cannot mix expression types");

	  type = newType;
	  
	  Rule[] args = new Rule[type == ANGLE_MEASURE ? 3 : 2];
	  int[] childNum = new int[type == ANGLE_MEASURE ? 3 : 2];

	  // read the arguments to the measurement

	  for(int i=0;i<args.length;i++)
	    {
	      if (st.nextToken() != st.TT_WORD)
		throw new ParseError(st.lineno(),
				     "\"dist\" or \"angle\" missing argument");

	      nextArg = ruleTable.get(st.sval);

	      if (nextArg == null)
		throw new ParseError(st.lineno(),"Unknown argument name \""+
				     st.sval+"\"");

	      if (!(nextArg instanceof Rule))
		throw new ParseError(st.lineno(),"Improper argument \""+
				     st.sval+'\"');

	      nextRule = (Rule)nextArg;
	      
	      int ruleType = nextRule.type;

	      // a bit of type checking (only points can be arguments)

	      if (ruleType != ARBITRARY && ruleType != INTERSECTION &&
		  ruleType != CONSTRUCTION)
		throw new ParseError(st.lineno(),"Improper argument \""+
				     st.sval+'\"');

	      args[i] = nextRule;
	      childNum[i] = nextRule.findChild(st.sval);
	    }

	  // create a MeasureRule for this measurement

	  MeasureRule mr = new MeasureRule();
	  
	  mr.parents = args;
	  mr.childNum = childNum;
	  mr.type = type;
	  mr.weight = nextWeight;

	  cr.add(mr,leftSide);
	}

      if (leftSide)
	throw new ParseError(st.lineno(),"Constraint missing '='");

      cr.type = type;

      return cr;
    }

  /** Read the right side of a rule 
   * 
   * @param stepNumber   The number of this step
   * @param ruleNames    The list of output names for this rule
   */

  void readRule(int stepNumber,Vector ruleNames)
    throws ParseError, IOException
    {
      // allocate the rule
      Rule newRule = new Rule();
      newRule.stepNumber = stepNumber;

      newRule.childName = new String[ruleNames.size()];

      // copy the rule names
      
      for(int i=0;i<ruleNames.size();i++)
	{
	  String name = (String)ruleNames.elementAt(i);
	  
          newRule.childName[i] = name;

	  ruleTable.put(name,newRule);
	}

      current.rules.addElement(newRule);  // place it in the construction
//      ruleTable.put(ruleName,newRule);    // put it in the lookup table

      int numArgs = 0;              // the number of arguments required
      
      // Determine what kind of rule this is

      // is it a primitive
      if (st.nextToken() == st.TT_WORD)
	{
	  if (st.sval.equals("Circle"))
	    newRule.type = CIRCLE;
	  else if (st.sval.equals("PerpBi"))
	    newRule.type = PERP_BI;
	  else if (st.sval.equals("Line"))
	    newRule.type = LINE;
	  else if (st.sval.equals("Intersect"))
	    newRule.type = INTERSECTION;
	  else if (st.sval.equals("LineSegment"))
	    newRule.type = LINE_SEGMENT;
	  else if (st.sval.equals("Ray"))
	    newRule.type = RAY;
	  else if (st.sval.equals("CompRay"))
	    newRule.type = COMPL_RAY;
	  else if (st.sval.equals("Force"))
	    newRule.type = FORCE;
	  else	    
	    throw new ParseError(st.lineno(),
				 "Construction name requires quotes");

	  numArgs = 2;  // each of these requires two arguments
	}
      else
	{
	  // it must be a construction
	  if (st.ttype != '\"')
	    throw new ParseError(st.lineno(),
				 "Construction name requires quotes");

	  if (current.name.equals(st.sval))
	    throw new ParseError(st.lineno(),"Recursion not allowed");

	  newRule.type = CONSTRUCTION;
	  newRule.construction = (Construction)constructions.get(st.sval);
	  
	  if (newRule.construction == null)
	    throw new ParseError(st.lineno(),"Unknown construction ("+
				 st.sval+")");

	  numArgs = newRule.construction.numberOfInputs;
	}

      // read the rest of the rule

      if (newRule.type == FORCE)
	readForce(newRule);
      else
	readRuleArguments(newRule,numArgs); 

      return;
    }

  /** Read a Force rule
   *
   * @param newRule  Rule to store the data in
   */

  void readForce(Rule newRule) throws ParseError, IOException
  {
    newRule.parents = new Rule[0];
    newRule.childNumber = new int[0];
    newRule.childName = new String[0];
    newRule.force = readConstraint();
  }

  /** Read the list of inputs points */

  void readInputRule() throws ParseError, IOException
  {
    if (st.ttype != st.TT_WORD || !st.sval.equals("Input"))
      throw new ParseError(st.lineno(),
			   "Missing input list at beginning of construction");
    
    while (st.nextToken() == st.TT_EOL)
      ;

    while (true)
      {
	while(st.ttype == st.TT_EOL)
	  st.nextToken();

	if (st.ttype != st.TT_WORD)
	  throw new ParseError(st.lineno(),"Syntax error in input list");

	if (st.sval.equals("Assume") || st.sval.equals("Steps"))
	  {
	    st.pushBack();
	    return;
	  }

	if (ruleTable.get(st.sval) != null)
	  throw new ParseError(st.lineno(),"Duplicate input name \""
			       +st.sval+'\"');

	Rule newRule = new Rule();
	newRule.stepNumber = current.numberOfInputs++;
	newRule.childName = new String[1];
	newRule.childName[0] = st.sval;
	newRule.type = ARBITRARY;
	newRule.parents = new Rule[0];
	newRule.childNumber = new int[0];
	
	current.rules.addElement(newRule);
	ruleTable.put(st.sval,newRule);

	if (st.nextToken() == '\"')
	  {
	    newRule.inputName = st.sval;
	    st.nextToken();
	  }
	else
	  newRule.inputName = null;

	// read the default coordinates for this point

	if (st.ttype == st.TT_NUMBER)
	  {
	    newRule.hasDefaults = true;

	    newRule.defaultX = st.nval;

	    if (st.nextToken() != st.TT_NUMBER)
	      throw new ParseError(st.lineno(),
				   "Default coordinates must be pairs");

	    newRule.defaultY = st.nval;

	    st.nextToken();
	  }
      }
  }

  /** Read the arguments to a rule */

  void readRuleArguments(Rule newRule,int numArgs)
    throws ParseError, IOException
    {
      // reset the parents/childNum arrays
      Vector parents = new Vector();
      Vector childNum = new Vector();

      // read in the rule arguments
      while(st.nextToken() != st.TT_EOL && st.ttype != st.TT_EOF)
	{
	  if (st.ttype != st.TT_WORD)
	    throw new ParseError(st.lineno(),"Invalid argument name");

	  if (st.sval.equals("PI"))
	    throw new ParseError(st.lineno(),
				  "Pi may not be a construction argument");

	  Object argRule = ruleTable.get(st.sval);
	  
	  if (argRule == null)
	    throw new ParseError(st.lineno(),
				 "Unknown rule name \""+st.sval+"\"");

	  if (!(argRule instanceof Rule))
	    throw new ParseError(st.lineno(),"Invalid argument type");

	  parents.addElement(argRule);

	  int cnum = ((Rule)argRule).findChild(st.sval);

	  childNum.addElement(new Integer(cnum));

	}

      if (numArgs != parents.size())
	throw new ParseError(st.lineno(),
			     "Exactly "+numArgs+" arguments required");

      // copy the arguments into the rule
      newRule.parents = new Rule[parents.size()];
      newRule.childNumber = new int[parents.size()];

      for(int i=0;i<parents.size();i++)
	{
	  newRule.parents[i] = (Rule)parents.elementAt(i);
	  newRule.childNumber[i] = ((Integer)childNum.elementAt(i)).
	    intValue();
	}
    }

  /** Read the list of output shapes */

  void readOutputRule(int stepNumber,String ruleName)
    throws ParseError, IOException
    {
      // allocate the rule
      Rule newRule = new Rule();
      newRule.stepNumber = stepNumber;
      newRule.type = OUTPUT;
      newRule.construction = null;

      current.rules.addElement(newRule);  // place it in the construction
      ruleTable.put(ruleName,newRule);    // put it in the lookup table

      Vector parents = new Vector();
      Vector childNum = new Vector();

      int ocCtr = 0;

      // read in the rule arguments
      while(true)
	{
	  while (st.nextToken() == st.TT_EOL)
	    ;

	  if (st.ttype == st.TT_EOF)
	    break;

	  if (st.sval.equals("Construction") || st.sval.equals("Conclude"))
	    {
	      st.pushBack();
	      break;
	    }

	  if (st.ttype != st.TT_WORD)
	    throw new ParseError(st.lineno(),"Invalid argument name");

	  Object nextArg = ruleTable.get(st.sval);
	  
	  if (nextArg == null)
	    throw new ParseError(st.lineno(),
				 "Unknown rule name ("+st.sval+")");

//	  if (nextArg instanceof Rule)
//	    {
	      Rule argRule = (Rule)nextArg;

	      if (argRule.type == ARBITRARY)
		throw new ParseError(st.lineno(),"Inputs may not be outputs");
	      /*
	      if (argRule.type == ANGLE_MEASURE || 
		  argRule.type == DISTANCE_MEASURE)
		throw new ParseError(st.lineno(),
				     "Measures may not be outputs");
				     */
	      parents.addElement(argRule);

	      int cnum = ((Rule)argRule).findChild(st.sval);

	      childNum.addElement(new Double(cnum));
//	    }
//	  else
//	    if(nextArg instanceof ConstraintRule)
//	      current.outputConstraints.addElement(nextArg);
	}

      // copy the arguments into the rule
      newRule.parents = new Rule[parents.size()];
      newRule.childNumber = new int[parents.size()];

      for(int i=0;i<parents.size();i++)
	{
	  newRule.parents[i] = (Rule)parents.elementAt(i);
	  newRule.childNumber[i] = ((Double)childNum.elementAt(i)).
	    intValue();
	}
    }

  /*

  void readAssertions()
    throws ParseError, IOException
    {
      int crCtr = 0;

      while (st.nextToken() != st.TT_EOL && st.ttype != st.TT_EOF)
	{
	  Object outObj = ruleTable.get(st.sval);

	  if (outObj == null)
	    throw new ParseError(st.lineno(),"Unknown rule name");

	  if (!(outObj instanceof ConstraintRule))
	    throw new ParseError(st.lineno(),
				 "Only constraints may be asserted");

	  ConstraintRule c = (ConstraintRule)outObj;

	  current.inputConstraints.addElement(c);
	}
    }
*/
}

  /** A StreamTokenizer for use in debugging.  Prints each token out
   *  to stdout as they are read. */

class DebugStreamTokenizer extends StreamTokenizer
{
  DebugStreamTokenizer(InputStream i)
    {
      super(i);
    }
  
  public int nextToken() throws IOException
    {
      int nt = super.nextToken();
      
      switch (nt)
	{
	case TT_EOL:
	  System.out.println("EOL");
	  break;

	case TT_EOF:
	  System.out.println("EOF");
	  break;

	case TT_NUMBER:
	  System.out.println("Number = "+nval);
	  break;

	case TT_WORD:
	  System.out.println("Word = "+sval);
	  break;

	case '\"':
	  System.out.println("Quoted = "+sval);
	  break;

	default:
	  System.out.println("Token = "+nt);
	}
	  
      return nt;
    }
}

class ParseError extends Throwable
{
  ParseError(String message)
    {
      super(message);
    }

  ParseError(int lineno,String message)
    {
      super("Line "+lineno+": "+message);
    }

  ParseError()
  {
    super();
  }
}

/** Prompt for the URL for a library.  This should be a Dialog, but
 *  Netscape doesn't support Dialogs */

class LibraryFrame extends Frame
{
  /** The default URL path */
  static final String serverName = "http://jqest.acm.org/";

  /** The field for entering the URL */
  TextField inputTF;

  ConstructionPanel constructionPanel;

  public LibraryFrame(ConstructionPanel cp)
  {
    super("Construction Library");

    setLayout(new GridLayout(3,1));

    add(new Label("Enter a library URL"));
      
    inputTF = new TextField(serverName,25);
    inputTF.setEditable(true);

    add(inputTF);

    Panel btns = new Panel();

    btns.add(new Button("Load"));
    btns.add(new Button("Cancel"));
    
    add(btns);

    pack();
    show();

    constructionPanel = cp;
  }

  public boolean action(Event e, Object arg)
  {
    if (e.target instanceof TextField)
      {
	// the user hit return in the enter field
	
	// load the library
	constructionPanel.getLibrary(inputTF.getText());
	hide();
	dispose();
	return true;
      }

    if (e.target instanceof Button)
      {
	String choice = (String)e.arg;

	if (choice.equals("Load"))
	  {
	    // load the library
	    constructionPanel.getLibrary(inputTF.getText());
	  }

	hide();
	dispose();

	return true;
      }

    return false;
  }
}

/** A Frame for pasting or typing in a library.  */

class PasteFrame extends Frame
{
  ConstructionPanel constructionPanel;
  TextArea ta;

  PasteFrame(ConstructionPanel cp)
    {
      super("Enter or paste library");

      constructionPanel = cp;

      ta = new TextArea(20,30);
      ta.setEditable(true);
      this.add("Center",ta);

      Panel controls = new Panel();

      controls.add(new Button("OK"));
      controls.add(new Button("Cancel"));

      this.add("South",controls);

      this.pack();
      this.show();
    }

  public boolean action(Event e,Object what)
    {
      if (e.target instanceof Button)
	{
	  String choice = (String)e.arg;

	  // parse the text field
	  if (choice.equals("OK"))
	    constructionPanel.parseString(ta.getText());
 
	  hide();
	  dispose();

	  return true;
	}

      return false;
    }
}
