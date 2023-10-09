/* GRACE - Graphical Ruler and Compass Editor
 *
 * LoadFrame.java
 *
 * A window that loads the GRACE classes, and displays the progress.
 *
 * To generate the classlist, use "ls -s1 *class > classlist"
 *
 * August 1996 - First version, Aaron Hertzmann
 *
 */

import java.lang.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.io.*;

/** The window that loads the GRACE classes, and displays the progress */

public class LoadFrame extends Frame
{
  /** The progress message */
  TextField message;

  /** The directory to look in for the classes and the classlist */
  URL codebase;

  /** Show the frame.
   *
   *  @param cb The directory to look in for the classes and classlist
   */

  LoadFrame(URL cb)
       throws MalformedURLException, IOException, ParseError
  {
    super("Loading GRACE");
    setLayout(new FlowLayout());
    message = new TextField(50);
    add(message);
 
    message.setEditable(false);
    codebase = cb;

    // read the classlist
    readClassNames();

    pack();
    show();
  }

  /** Load the classes in */

  void loadClasses()
  {
    Class c;

    // amount loaded so far
    int sofar = 0;

    try 
      {
	for(int i=0;i<classNames.length;i++)
	  {
	    message.setText("Loading class: "+classNames[i]+
			    " ("+100*sofar/totalSize+"%)");
	    sofar+=classSizes[i];
	    c = Class.forName(classNames[i]);
	  }

      } 
    catch(ClassNotFoundException cnfe)
      {
      }
    finally
      {
	hide();
	dispose();
      }
  }
  
  /** List of class names */
  String classNames[];

  /** Size of each class */
  int classSizes[];

  /** Sum of classSizes[] */
  int totalSize = 0;

  /** Read the classlist file */
  void readClassNames()
       throws MalformedURLException, IOException, ParseError
  {
    // temporary vectors
    Vector names = new Vector();
    Vector sizes = new Vector();

    StreamTokenizer st = new StreamTokenizer((new URL(codebase,"classlist")).
					     openStream());

    st.whitespaceChars('.','.');
    st.whitespaceChars(' ',' ');
    st.whitespaceChars('\t','\t');

    while (st.nextToken() != st.TT_EOF)
      {
	if (st.ttype != st.TT_NUMBER)
	  throw new ParseError();
	
	int val = (int)st.nval;

	sizes.addElement(new Integer(val));

	totalSize += val;

	if (st.nextToken() != st.TT_WORD)
	  throw new ParseError();

	names.addElement(st.sval);

	if (st.nextToken() != st.TT_WORD)  // consume "class"
	  throw new ParseError();
      }

    // copy the temporary lists
    classNames = new String[names.size()];
    classSizes = new int[names.size()];

    for(int i=0;i<classNames.length;i++)
      {
	classNames[i] = (String)names.elementAt(i);
	classSizes[i] = ((Integer)sizes.elementAt(i)).intValue();
      }
  }
}
