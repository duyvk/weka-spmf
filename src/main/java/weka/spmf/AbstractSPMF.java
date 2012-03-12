/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    AbstractAssociator.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.spmf;

import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Utils;

import java.io.Serializable;

/** 
 * Abstract scheme for learning associations. All schemes for learning
 * associations extend this class
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 5503 $ 
 */
public abstract class AbstractSPMF 
  implements Cloneable, SPMF, Serializable, CapabilitiesHandler, RevisionHandler {
 
  /** for serialization */
  private static final long serialVersionUID = -3017644543382432070L;
  
  /**
   * Creates a new instance of a associator given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * associator implements OptionHandler and the options parameter is
   * non-null, the associator will have it's options set.
   *
   * @param associatorName the fully qualified class name of the associator
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created associator, ready for use.
   * @exception Exception if the associator name is invalid, or the options
   * supplied are not acceptable to the associator
   */
  public static SPMF forName(String associatorName,
				   String [] options) throws Exception {

    return (SPMF)Utils.forName(SPMF.class,
				     associatorName,
				     options);
  }

  /**
   * Creates a deep copy of the given associator using serialization.
   *
   * @param model the associator to copy
   * @return a deep copy of the associator
   * @exception Exception if an error occurs
   */
  public static SPMF makeCopy(SPMF model) throws Exception {
    return (SPMF) new SerializedObject(model).getObject();
  }

  /**
   * Creates copies of the current associator. Note that this method
   * now uses Serialization to perform a deep copy, so the Associator
   * object must be fully Serializable. Any currently built model will
   * now be copied as well.
   *
   * @param model an example associator to copy
   * @param num the number of associators copies to create.
   * @return an array of associators.
   * @exception Exception if an error occurs 
   */
  public static SPMF[] makeCopies(SPMF model,
					 int num) throws Exception {

    if (model == null) {
      throw new Exception("No model associator set");
    }
    SPMF [] associators = new SPMF [num];
    SerializedObject so = new SerializedObject(model);
    for(int i = 0; i < associators.length; i++) {
      associators[i] = (SPMF) so.getObject();
    }
    return associators;
  }

  /** 
   * Returns the Capabilities of this associator. Maximally permissive
   * capabilities are allowed by default. Derived associators should
   * override this method and first disable all capabilities and then
   * enable just those capabilities that make sense for the scheme.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);
    result.enableAll();
    
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return            the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 5503 $");
  }
  
  /**
   * runs the associator with the given commandline options
   * 
   * @param associator	the associator to run
   * @param options	the commandline options
   */
  protected static void runAssociator(SPMF associator, String[] options) {
    try {
      System.out.println(
	  AssociatorEvaluation.evaluate(associator, options));
    }
    catch (Exception e) {
      if (    (e.getMessage() != null)
	   && (e.getMessage().indexOf("General options") == -1) )
	e.printStackTrace();
      else
	System.err.println(e.getMessage());
    }
  }
}
