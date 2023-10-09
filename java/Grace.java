/* GRACE - Graphical Ruler and Compass Editor
 *
 * Grace.java
 *
 * August 1996 - First version, Aaron Hertzmann
 * May 1997 - Stand-alone capability, JDK 1.1.1 compatibility
 *
 * Build instructions:
 *
 * javac *java # (Or javac <changed files>)
 * # Optional:
 * zip -u Grace.zip *class  # create archive
 * ls -s1 *class > classlist            # create classlist
 */

import java.awt.*;
import java.applet.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 *  Constants for use throughout the program
 */

interface Constants
{
  /** How close the mouse click needs to be to an object.
   * Used in the isPointNearShape() functions.
   * Measured in pixels.   */
  static final int TOLERANCE = 8;

  // some colors
  static final Color FOREGROUND = Color.black;
  static final Color FIELD_BACKGROUND = Color.white;
  static final Color CONTROLS_BACKGROUND = Color.lightGray;
  static final Color SELECTED = Color.magenta;

  // operating modes
  static final int POINT_MODE = 0;
  static final int LINE_SEGMENT_MODE = 1;
  static final int LINE_MODE = 2;
  static final int RAY_MODE = 3;
  static final int COMPL_RAY_MODE = 4;
  static final int CIRCLE_MODE = 5;
  static final int INTERSECT_MODE = 6;
  static final int CONCLUDE_MODE = 7;
  static final int APPLY_MODE = 8;
  static final int DRAG_MODE = 9;
  static final int DEBUG_MODE = 10;
  static final int LABEL_ANGLE_MODE =11;
  static final int LABEL_DISTANCE_MODE = 12;
  static final int ASSUME_CONSTRAINT_MODE = 13;
  static final int TEST_CONSTRAINT_MODE = 14;
  static final int FORCE_CONSTRAINT_MODE = 15;
  static final int DELETE_EXPRESSIONS_MODE = 16;
  static final int LABEL_MODE = 17;
  static final int OUTPUT_MODE = 18;
  static final int PERP_BI_MODE = 19;

  // shape and dependency types
  static final int POINT = 0;
  static final int LINE_SEGMENT = 1;
  static final int LINE = 2;
  static final int RAY = 3;
  static final int COMPL_RAY = 4;
  static final int CIRCLE = 5;
  static final int PERP_BI = 6;

  // more dependency types
  static final int ARBITRARY = 7;
  static final int CONSTRUCTION = 8;
  static final int OUTPUT = 9;
  static final int ANGLE_MEASURE = 10;
  static final int DISTANCE_MEASURE = 11;
  static final int PI = 12;
  static final int RADIUS_MEASURE = 13;
  static final int FORCE = 14;
  static final int INTERSECTION = 15;

  // constants for the expression display
  static final int MEASURE_WIDTH = 200;
  static final int MEASURE_HEIGHT = 20;
  static final int MEASURE_MARGIN = 5;

  // for the drawPanel
  static final int DP_WIDTH = 300;
  static final int DP_HEIGHT = 300;

  static final PiMeasure PI_MEASURE = new PiMeasure();
//  static final Rule      PI_RULE = new Rule(PI_MEASURE);
  static final UniquePi  PI_UNIQUE = new UniquePi();

  static final boolean DEBUG = false;
}

/**
 * The main applet.  Displays a button marked "Start GRACE" in the
 * Applet panel.  When the button is pressed, all of the classes are
 * loaded and the main GRACE window appears.
 *
 * @see Constructions
 */

public class Grace extends Applet 
{
  Constructions c;         // temporary pointer
  String[] autolibraries;  // list of libraries to autoload

  String[] libraryButtons; // list of library button text
  String[] libraryURLs;    // list of library URLs

  boolean doubleBuffer;    // should double-buffering be used?

  boolean classesLoaded = false;  // have all the classes been loaded already?

  /** Initialize the applet and read applet parameters */
  
public void init()
  {
    // place the button

    add(new Button("Start GRACE"));

    doubleBuffer = (getParameter("Double Buffer") != null);

    classesLoaded = (getParameter("Dont Show Progress") != null);

    // read the parameter lists

    autolibraries = readParams("Auto Library ");
    libraryButtons = readParams("Library Name ");
    libraryURLs = readParams("Library URL ");

    // make sure the length of libraryButtons and libraryURLs is the same

    if (libraryButtons.length > libraryURLs.length)
      {
	String[] q = libraryButtons;
	libraryButtons = new String[libraryURLs.length];

	for(int i=0;i<libraryURLs.length;i++)
	  libraryButtons[i] = q[i];
      }
    else
      if (libraryURLs.length > libraryButtons.length)
	{
	  String[] q = libraryURLs;
	  libraryURLs = new String[libraryButtons.length];
	  
	  for(int i=0;i<libraryURLs.length;i++)
	    libraryURLs[i] = q[i];
	}
  }

  /** Handle a button-press */

  public boolean action(Event e, Object what)
  {
    if (e.target instanceof Button)
     {
       // have all the program classes been loaded already?

       if (!classesLoaded)
	 {
	   try
	     {
	       // open a progress window to load the classes
	       LoadFrame lf = new LoadFrame(getCodeBase());

	       // load all the classes
	       lf.loadClasses();

	       classesLoaded = true;
	     }
	   catch (ParseError pe)
	     {
	       System.out.println("ParseError: "+pe.getMessage());
	     }
	   catch (MalformedURLException mue)
	     {
	       System.out.println("MalformedURLException: "+mue.getMessage());
	     }
	   catch (IOException ioe)
	     {
	       System.out.println("IOException: "+ioe.getMessage());
	     }
	 }

       // create a new constructions window
       c = new Constructions(autolibraries,libraryButtons,libraryURLs,
			     doubleBuffer,true);

       return true;
     }

    return false;
  }

  /** Return some textual information about the applet */

  public String getAppletInfo()
  {
    return "Graphical Ruler and Compass Editor\n"+
      "Department of Computer Science, Rice University";
  }

  /** Read a list of parameters.  All parameters of the form prefix+n,
   *  where n is an integer, will be found.  The parameters must be numbered
   *  sequentially, starting at 1.
   *
   * @param prefix The string prefix for parameters
   *
   * @return List of parameters
   */

  String[] readParams(String prefix)
  {
    // temp vector to hold the parameters
    Vector vals = new Vector();

    int i = 1;
    String name = getParameter(prefix + i++);  // get the first prefix

    while (name != null)             // does the prefix exist?
      {
	vals.addElement(name);       // add it to the vector
	name = getParameter(prefix + i++);  // get the next prefix
      }

    // copy the vector into an array

    String[] s = new String[vals.size()];
    
    for(i=0;i<vals.size();i++)
      s[i] = (String)vals.elementAt(i);

      return s;
  }

  static public void main(String args[])
  {
    String[] autolibraries = new String[0];
    String[] libraryButtons = new String[0];
    String[] libraryPaths = new String[0];
    boolean doubleBuffer = false;

    String configName = (args.length > 0 ? args[0] : "grace.config");

    try
      {
	Vector autoload = new Vector();
	Vector lb = new Vector();
	Vector lp = new Vector();
	
	StreamTokenizer st= new 
	  StreamTokenizer(new FileInputStream(new File(configName)));

	st.commentChar(';');
	st.quoteChar('\"');
	
	while (st.nextToken() != st.TT_EOF)
	  {
	    if (st.sval.equals("Autoload"))
	      {
		st.nextToken();
		autoload.addElement(st.sval);
		continue;
	      }

	    if (st.sval.equals("Library"))
	      {
		st.nextToken();
		lb.addElement(st.sval);
		st.nextToken();
		lp.addElement(st.sval);
		continue;
	      }
	    
	    if (st.sval.equals("DoubleBuffer"))
	      {
		doubleBuffer = true;
		continue;
	      }

	    System.out.println("Warning: Ignoring configuration option "+
			       st.sval);
	  }

	autolibraries = new String[autoload.size()];
	for(int i=0;i<autolibraries.length;i++)
	  autolibraries[i] = (String) autoload.elementAt(i);
	
	libraryButtons = new String[lb.size()];
	for(int i=0;i<libraryButtons.length;i++)
	  libraryButtons[i] = (String) lb.elementAt(i);
	

	libraryPaths = new String[lp.size()];
	for(int i=0;i<libraryPaths.length;i++)
	  libraryPaths[i] = (String) lp.elementAt(i);
	

      }
    catch(FileNotFoundException fnfe)
      {
	System.out.println("Unable to open "+configName+
			   ": File not found -"+fnfe.getMessage());
      }
    catch(IOException ioe)
      {
	System.out.println("Unable to open "+configName+": IO exception -"+
			   ioe.getMessage());
      }

    Constructions c = new Constructions(autolibraries,libraryButtons,
					libraryPaths,doubleBuffer,false);
  }
}

/** The main GRACE frame. */
class Constructions extends Frame implements Constants
{
  DrawPanel dp;               // the drawing canvas at the center of the window
  TextField messageBox;       // field for prompts and messages
  ConstructionPanel cp;       // the list of constructions/name
  ExpressionFrame lf;         // the expressions window
  ConstraintFrame cf;         // the constraint window
  Editor editor;              // the text window
  Undo undo;                  // data for undo
  TextField statusField;      // construction status field

  // for looking up library URLs from the menu item text
  Hashtable libraryPaths = new Hashtable();

  // Some of the items in the menu bar are stored here, so they can be
  // enabled and disabled as necessary.

  // the "Labels" menu item
  CheckboxMenuItem labelsItem;

  // The "View" and "Delete" menu items
  MenuItem[] cits;

  // The "Undo" menu item
  MenuItem undoButton;

  /** Constructor for the window.
   *
   * @param libs List of library URLs to autoload
   * @param libPaths List of library paths for the "Library" menu
   * @param libButtons List of names for the libPaths libraries
   */

  Constructions(String[] libs,String[] libButtons,
		String[] libPaths,boolean doubleBuffer,boolean isApplet)
    {
      super("Graphical Ruler and Compass Editor");

      // INITIALIZE DATA AND WINDOW COMPONENTS

      setLayout(new BorderLayout());

      undoButton = new MenuItem("Undo");
      undoButton.disable();

      cits = new MenuItem[2];
      cits[0] = new MenuItem("View selected");
      cits[0].disable();
      cits[1] = new MenuItem("Delete selected");
      cits[1].disable();

      statusField = new TextField("Successful",12);
      statusField.setEditable(false);

      undo = new Undo(undoButton);
      messageBox = new TextField("Place free points.");
      messageBox.setEditable(false);
      cp = new ConstructionPanel(messageBox,statusField,cits,isApplet);
      editor = new Editor(cp);
      dp = new DrawPanel(messageBox,cp,editor,undo,doubleBuffer);
      cf = new ConstraintFrame(dp,editor,undo);
      lf = new ExpressionFrame(dp,cf,editor,undo);
      Panner panner = new Panner(dp);

      undo.editor = editor;
      undo.drawPanel = dp;
      undo.constraintFrame = cf;
      cp.panel = dp;
      dp.expressionFrame = lf;
      dp.constraintFrame = cf;
      editor.drawPanel = dp;
      editor.cf = cf;

      // LAYOUT THE WINDOW

      // controls at the bottom of the window

      Panel controls = new Panel();
      controls.setLayout(new FlowLayout());

      Panel ml = new Panel();
      ml.setLayout(new GridLayout(2,1));
      ml.add(new Label("Point"));
      ml.add(new Label("operations"));

      controls.add(ml);

      Panel tb = new Panel();
      tb.setLayout(new GridLayout(3,1));
      tb.add(new Button("Place input"));
      tb.add(new Button("Drag input"));
      tb.add(new Button("Select output"));

      controls.add(tb);

      ml = new Panel();
      ml.setLayout(new GridLayout(2,1));
      ml.add(new Label("View"));
      ml.add(new Label("Controls"));

      controls.add(ml);

      controls.add(panner);

      // the constructions on the right

      ml = new Panel();
      ml.setLayout(new FlowLayout(FlowLayout.LEFT));
      ml.add(new Label("Status"));
      ml.add(statusField);

      Panel cp1 = new Panel();
      cp1.setLayout(new BorderLayout());
      cp1.add("Center",cp);
      cp1.add("South",ml);

      Panel a = new Panel();
      a.setLayout(new BorderLayout());
      a.add("Center",dp);
      a.add("South",controls);

      // the whole window

      add("Center", a);
      add("East",cp1);
      add("North",messageBox);

      // CREATE THE MENU BAR

      MenuBar mb = new MenuBar();

      Menu m = new Menu("Edit");
      labelsItem = new CheckboxMenuItem("Labels");
      m.add(labelsItem);
      m.add(undoButton);
      m.add("Clear workspace");
      m.add("-");
      m.add("Quit");
      mb.add(m);

      m = new Menu("Construction");
      m.add(cits[0]);
      m.add(cits[1]);
      m.add("-");
      m.add("Name current...");
      mb.add(m);

      // add the libraries

      m = new Menu("Libraries");
      for(int i=0;i<libPaths.length;i++)
	{
	  libraryPaths.put(libButtons[i],libPaths[i]);
	  m.add(libButtons[i]);
	}
      if (libPaths.length > 0)
	m.add("-");

      if (isApplet)
	m.add("Enter URL...");
      else
	m.add("Enter filename...");
      m.add("Enter library text");
      mb.add(m);

      m = new Menu("Windows");
      m.add("Text");
      m.add("Expressions");
      m.add("Constraints");
      mb.add(m);

      setMenuBar(mb);

      // PACK AND SHOW THE FRAME

      pack();
      show();

      // LOAD LIBRARIES

      for(int i=0;i<libs.length;i++)
	cp.getLibrary(libs[i]);
    }

  /** Handle window events */

  public boolean handleEvent(Event e) 
    {
      switch (e.id) 
	{
	case Event.WINDOW_DESTROY:   // clean-up if the window is destroyed
	  quit();
	  return true;
	default:
	  return super.handleEvent(e);
	}
    }

  /** Handle action events, such as buttons or menu items selected */
  public boolean action(Event e, Object arg) 
    {
      if (e.target instanceof Button)
	{
	  String choice = (String)arg;
      
	  if (choice.equals("Place input")) 
	    {
	      dp.setDrawMode(POINT_MODE);
	    }
	  else if (choice.equals("Select output"))
	    {
	      dp.setDrawMode(OUTPUT_MODE);
	    }
	  else if (choice.equals("Drag input"))
	    {
	      dp.setDrawMode(DRAG_MODE);
	    }

	  return true;
	}

      if (e.target instanceof CheckboxMenuItem)
	{
	  dp.toggleLabels(labelsItem.getState());

	  return true;
	}

      if (e.target instanceof MenuItem)
	{
	  String choice = (String)arg;
      
	  if (choice.equals("Name current..."))
	    {
	      NameFrame nf = new NameFrame(editor);
	    }
	  else if (choice.equals("Clear workspace"))
	    {
	      undo.setCantUndo();
	      dp.erase();
	      editor.clear();
	      lf.clear();
	      cf.clear();
	      dp.setDrawMode(POINT_MODE);
	    }
	  else if (choice.equals("Undo"))
	    {
	      if (undo.lastStepType == Undo.CANT_UNDO)
		message("Can't undo.");
	      else
		{
		  undo.undo();
		  message("Undone.");
		}
	    }
	  else if (choice.equals("Expressions"))
	    {
	      lf.show();
	    }
	  else if (choice.equals("Constraints"))
	    {
	      cf.show();
	    }
	  else if (choice.equals("Text"))
	  {
	    editor.show();
	  }
	  else if (choice.equals("Debug"))
	    {
	      dp.setDrawMode(DEBUG_MODE);
	    }
	  else
	  if (choice.equals("Enter URL...") || 
	      choice.equals("Enter filename..."))
	    {
	      LibraryFrame ld = new LibraryFrame(cp);
	      undo.setCantUndo();
	    }
	  else 
	    if (choice.equals("Enter library text"))
	      {
		PasteFrame pf = new PasteFrame(cp);
		undo.setCantUndo();
	      }
	  else if (choice.equals("Delete selected"))
	    {
	      if (cp.current != null)
		{
		  cp.delete();
		  undo.setCantUndo();
		}
	      else
		message("Select a construction first");
	    }
	  else if (choice.equals("View selected"))
	    {
	      if (cp.current != null)
		editor.view(cp.current);
	      else
		message("Select a construction first");
	    }
	  else if (choice.equals("Quit"))
	    {
	      quit();
	    }
	  else
	    {
	      String libURL = (String)libraryPaths.get(choice);

	      if (libURL != null)
		cp.getLibrary(libURL);
	    }
	  
	  return true;
	} 

      return false;
    }

  /** Close all the windows and quit */
  void quit()
  {
    lf.hide();
    lf.dispose();
    cf.hide();
    cf.dispose();
    editor.closeFrames();
    editor.hide();
    editor.dispose();
    hide();
    dispose();
  }

  /** Display a message in the prompt field */
  void message(String m)
    {
      messageBox.setText(m);
    }
}

/** This panel displays the following:
 * "Available constructions", followed by a list of primitives and 
 * constructions.  A description for the currently selected 
 * construction/primitive, and a current-construction status
 */
class ConstructionPanel extends Panel implements Constants
{
  /** The currently selected construction */
  Construction current = null;
  java.awt.List cList;                 // the list of constructions
  Hashtable constructions = new Hashtable();  // Construction lookup table
  TextField messageBox;            // prompt field
  TextArea description;        // the construction description area
  TextField statusField;       // the current construction status field
  boolean successful = true;   // what status is currently displayed?

  DrawPanel panel;             // the main draw canvas
  int lastSelected = -1;       // the index of the last selected construction

  MenuItem consItems[];        // menu items to enable when (current != null)

  boolean isApplet;            // is GRACE running as an applet?

  // the locations of the primitives in cList
  static final int CIRCLE_BUTTON = 0;
  static final int LINE_BUTTON = 1;
  static final int LINE_SEGMENT_BUTTON = 2;
  static final int RAY_BUTTON = 3;
  static final int COMPL_RAY_BUTTON = 4;
  static final int PERP_BI_BUTTON = 5;
  static final int INTERSECT_BUTTON = 6;
  static final int NUM_PRIMITIVES = 7;

  /** Constructor for the panel
   *
   * @param msg Field for prompts and messages
   * @param sf  Status field
   * @param cits Menu items to enable/disable
   */
  public ConstructionPanel(TextField msg,TextField sf,MenuItem[] cits,
			   boolean isApp)
    {
      // initialize the construction list
      cList = new java.awt.List();
      cList.setMultipleSelections(false);
      cList.addItem("Circle",CIRCLE_BUTTON);
      cList.addItem("Line",LINE_BUTTON);
      cList.addItem("Line Segment",LINE_SEGMENT_BUTTON);
      cList.addItem("Ray",RAY_BUTTON);
      cList.addItem("Complementary Ray",COMPL_RAY_BUTTON);
      cList.addItem("Perpendicular Bisector",PERP_BI_BUTTON);
      cList.addItem("Intersect",INTERSECT_BUTTON);

      setLayout(new BorderLayout());
      Panel p = new Panel();
      p.setLayout(new BorderLayout());
      p.add("North",new Label("Available constructions"));
      p.add("Center",new Label("(Click to apply)"));

      description = new TextArea(4,25);
      description.setEditable(false);

      Panel p2 = new Panel();
      p2.setLayout(new BorderLayout());
      p2.add("Center",description);
      p2.add("North",new Label("Description"));

      add("South",p2);
      add("North",p);
      add("Center",cList);

      // copy the argument data
      messageBox = msg;
      consItems = cits;
      statusField = sf;

      isApplet = isApp;
    }

  /** Set the status field
   *
   * @param success Indicates successful or failed
   */
  void setStatus(boolean success)
  {
    if (successful == success)
      return;

    successful = success;

    statusField.setText(successful ? "Successful" : "Failed");
  }

  public void paint(Graphics g) 
    {
      Rectangle r = bounds();

      g.setColor(CONTROLS_BACKGROUND);
      g.draw3DRect(0, 0, r.width, r.height, false);
    }

  /** Add a construction to the list
    *
    * @param c The new construction
    * @param name The name to use in the construction list
    */
  void add(Construction c,String name)
    {
      cList.addItem(name);    // add it to the list
      constructions.put(name,c); // add it to the hashtable
    }

  /** Enable or disable the "View" and "Delete" menu items,
    * depending if there is a current construction
    */

  void enableMenuItems()
  {
    for(int i=0;i<consItems.length;i++)
      consItems[i].enable(current != null);
  }

  /** Handle a list select or deselect */

  public boolean handleEvent(Event e) 
    {
      // which item is selected, if any
      int index = cList.getSelectedIndex();

      switch (e.id)
	{
	case Event.LIST_SELECT:

	  // check if a primitive was selected

	  if (index == LINE_BUTTON)
	    {
	      panel.setDrawMode(LINE_MODE);
	      description.setText("Place a line containing two points");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == LINE_SEGMENT_BUTTON)
	    {
	      panel.setDrawMode(LINE_SEGMENT_MODE);
	      description.setText("Place a line segment connecting two points");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == RAY_BUTTON)
	    {
	      panel.setDrawMode(RAY_MODE);
	      description.setText("Place a ray from the endpoint through the sceond point");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == COMPL_RAY_BUTTON)
	    {
	      panel.setDrawMode(COMPL_RAY_MODE);
	      description.setText("Place a complementary ray from the endpoint opposite the second point");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == CIRCLE_BUTTON)
	    {
	      panel.setDrawMode(CIRCLE_MODE);
	      description.setText("Place a circle");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == INTERSECT_BUTTON)
	    {
	      panel.setDrawMode(INTERSECT_MODE);
	      description.setText("Intersect two shapes");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  if (index == PERP_BI_BUTTON)
	    {
	      panel.setDrawMode(PERP_BI_MODE);
	      description.setText("Place a perpendicular bisector");
	      lastSelected = index;
	      current = null;
	      enableMenuItems();
	      return true;
	    }

	  // A construction may have been selected

	  // Get the construction's name
	  String newItem = cList.getSelectedItem();
	  if (newItem == null)
	    {
	      current = null;
	    }
	  else
	    {
	      // look up the construction
	      lastSelected = index;
	      current = (Construction)constructions.get(newItem);
	      panel.setDrawMode(APPLY_MODE);
	    }

	  if (current != null)
	    {
	      message("Using "+newItem+". Select "+
		      current.ruleName(0));
	      description.setText(current.description);
	    }
	    
	  enableMenuItems();

	  return true;
	case Event.LIST_DESELECT:
	  // no current construction
	  current = null;
	  description.setText("");
	  enableMenuItems();
	  return true;

	default:
	  return false;
	}

    }    

  /** Check if a construction by a given name already exists */
  boolean isConstruction(String name)
    { 
      return (constructions.get(name) != null);
    }

  /** Check if a name is the name of a primitive */
  static boolean isPrimitive(String name)
    {
      return (name.equals("Line Segment") ||
	      name.equals("Ray") ||
	      name.equals("Complementary Ray") ||
	      name.equals("Line") ||
	      name.equals("Circle") ||
	      name.equals("Perpendicular Bisector") ||
	      name.equals("Intersect"));
    }

  /** Check how many constructions are defined */
  int numElements() { return constructions.size(); }

  /** Check if any constructions are defined */
  boolean isEmpty() { return constructions.isEmpty(); }

  /** Return a list of the constructions (for debugging) */
  public String toString() { return constructions.toString();}

  /** Show a message in the prompt field */
  void message(String msg)
    {
      messageBox.setText(msg);
    }

  /** Deselect any selected construction */
  void deselect()
    {
      int index = cList.getSelectedIndex();

      if (index >= 0)
	cList.deselect(index);

      current = null;
      enableMenuItems();

      lastSelected = index;

      description.setText("");
    }
/*
  void select()
    {
      int index = cList.getSelectedIndex();

      if (index == lastSelected)
	return;

      if (index >= 0)
	cList.deselect(index);

      if (lastSelected < 0)
	{
	  current = null;
	  return;
	}

      cList.select(lastSelected);

      String newName = cList.getItem(lastSelected);

      current = (Construction)constructions.get(newName);
      enableMenuItems();
    }
*/

  /** Read constructions from a library
   *
   * @param location Library URL to read from
   */
  void getLibrary(String location)
    {
      message("Loading "+location);

      InputStream is;
      
      try
	{
	  // attempt to open a connection
	  if (isApplet)
	    is = (new URL(location)).openStream();
	  else
	    is = (new FileInputStream(new File(location)));
	}
      catch (FileNotFoundException ex)
	{
	  message("File Not Found: "+ex.getMessage());
	  return;
	}
      catch (MalformedURLException ex)
	{
	  message("Malformed URL: "+ex.getMessage());
	  return;
	}
      catch (IOException ex)
	{
	  message("IO Exception: "+ex.getMessage());
	  return;
	}
      catch (SecurityException se)
	{
	  message("Security Exception: "+se.getMessage());
	  return;
	}

      LibraryParser LP = new LibraryParser();
      Vector newNames = new Vector();

      // parse the library (new constructions will be placed in
      // the constructions table)

      String result = LP.parseStream(constructions,is,newNames);

      // add the new constructions to the list

      for(int i=0;i<newNames.size();i++)
	cList.addItem((String)newNames.elementAt(i));

      // display the result message
      message(result);
    }

  /** Read a library from a string */

  void parseString(String data)
  {
    LibraryParser LP = new LibraryParser();
    Vector newNames = new Vector();  // the new construction names
    InputStream is = new StringBufferInputStream(data);

    String result = LP.parseStream(constructions,is,newNames);

    for(int i=0;i<newNames.size();i++)
      cList.addItem((String)newNames.elementAt(i));
    
    message(result);
  }

  /** Delete the currently selected construction */
  void delete()
    {
      if (current == null)
	return;

      String cName = current.name;
      int index = cList.getSelectedIndex();
      lastSelected = -1;
      cList.delItem(index);
      constructions.remove(cName);
      current = null;
      enableMenuItems();

      message(cName+" deleted");
    }

  /** Delete all the constructions */
  void deleteAll()
    {
      constructions.clear();
      current = null;
      enableMenuItems();
      cList.delItems(NUM_PRIMITIVES,cList.countItems()-1);
      message("Bye-bye!");
    }
}

/** The view controls, for adjusting the orientation of the view plane */

class Panner extends Panel
{
  DrawPanel drawPanel;
  Transform transform;

  Panner(DrawPanel dp)
  {
    setLayout(new GridLayout(3,3));
    add(new Label(""));
    add(new Button("Up"));
    add(new Label(""));
    add(new Button("Left"));
    add(new Button("Center"));
    add(new Button("Right"));
    add(new Button("In"));
    add(new Button("Down"));
    add(new Button("Out"));

    drawPanel = dp;
    transform = dp.transform;
  }

  /** Handle a button press, and adjust the transform matrix */

  public boolean action(Event e,Object what)
  {
    if (e.target instanceof Button)
      {
	String choice = (String)e.arg;

	if (choice.equals("Up"))
	  transform.virtualOriginY -= (50/transform.scaleFactor);
	else
	  if (choice.equals("Down"))
	    transform.virtualOriginY += (50/transform.scaleFactor);
	  else
	    if (choice.equals("Right"))
	      transform.virtualOriginX += (50/transform.scaleFactor);
	    else
	      if (choice.equals("Left"))
		transform.virtualOriginX -= (50/transform.scaleFactor);
	else
	  if (choice.equals("In"))
	    {
	      if (transform.scaleFactor <= 2048)
		transform.scaleFactor *= 2;
	      else drawPanel.message("Can't zoom in any further");
	    }
	else
	  if (choice.equals("Out"))
	    {
	      if (transform.scaleFactor*2048 >= 1)
		transform.scaleFactor /= 2;
	      else drawPanel.message("Can't zoom out any further");
	    }
	else if (choice.equals("Center"))
	  drawPanel.recenter();

	// recompute values
	transform.reset();

	// redraw the screen
	drawPanel.redraw();
	
	return true;
      }
    
    return false;
  }
}
