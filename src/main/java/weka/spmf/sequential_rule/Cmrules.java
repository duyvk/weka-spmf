package weka.spmf.sequential_rule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.spmf.AbstractSPMF;
import weka.spmf.sequential_rule.cmrules.AlgoCMRules;

public class Cmrules 
extends AbstractSPMF
implements OptionHandler{

	  /** the minimum support threshold */
	  protected double m_MinSupport; 
	  
	  /**the minimum confidence threshold  */
	  protected double m_MinConf;
	  
	  /** number indicating the attribute holding the data sequence ID */
	  protected int m_DataSeqID;

	  /** original sequential data set to be used for sequential patterns extraction */
	  protected Instances m_OriginalDataSet;
	  
	  /** all generated frequent sequences, i.e. sequential patterns */
	  protected FastVector m_AllSequentialPatterns;
	  
	  /** number of cycles performed until termination */
	  protected int m_Cycles;
	  
	  /** String indicating the starting time of an cycle. */
	  protected String m_CycleStart;
	  
	  /** String indicating the ending time of an cycle. */
	  protected String m_CycleEnd;
	  
	  /** String indicating the starting time of the algorithm. */
	  protected String m_AlgorithmStart;
	  
	  /** String containing the attribute numbers that are used for result 
	   * filtering; -1 means no filtering */
	  protected String m_FilterAttributes;
	  
	  /** Vector containing the attribute numbers that are used for result 
	   * filtering; -1 means no filtering */
	  protected FastVector m_FilterAttrVector;
	  
	  /** Whether the classifier is run in debug mode. */
	  protected boolean m_Debug = false;
		  
	  /**
	   * Constructor
	   */
	  public Cmrules(){
		  resetOptions();
	  }
	  
	  protected void resetOptions(){
		  m_MinSupport = 0.75;
		  m_MinConf = 0.5;
		  m_DataSeqID = 0;
		  m_FilterAttributes = "-1";
	  }
	    
	/**
	 * @param args
	 * @throws IOException 
	 */



	@Override
	public Enumeration listOptions() {
		Vector result = new Vector();

	    result.addElement(new Option(
		"\tIf set, algorithm is run in debug mode and\n"
		+ "\tmay output additional info to the console",
		"D", 0, "-D"));
	    
	    result.addElement(new Option(
		"\tThe miminum support threshold.\n"
		+ "\t(default: 0.75)",
		"S", 1, "-S <minimum support threshold>"));
	    
	    result.addElement(new Option(
		"\tThe miminum confidence threshold.\n"
		+ "\t(default: 0.5)",
		"C", 1, "-C <minimum confidence threshold>"));
	    
	    result.addElement(new Option(
		"\tThe attribute number representing the data sequence ID.\n"
		+ "\t(default: 0)",
		"I", 1, "-I <attribute number representing the data sequence ID"));

	    result.addElement(new Option(
		"\tThe attribute numbers used for result filtering.\n"
		+ "\t(default: -1)",
		"F", 1, "-F <attribute numbers used for result filtering"));

	    return result.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {
		String tmpStr;
		resetOptions();
		setDebug(Utils.getFlag('D', options));
		
		tmpStr = Utils.getOption('S', options);
		if(tmpStr.length()!=0){
			setMinSupport(Double.parseDouble(tmpStr));
		}
		
		tmpStr = Utils.getOption('C', options);
		if(tmpStr.length()!=0){
			setMinSupport(Double.parseDouble(tmpStr));
		}
		
		tmpStr = Utils.getOption('I', options);
	    if (tmpStr.length() != 0)
	      setDataSeqID(Integer.parseInt(tmpStr));

	    tmpStr = Utils.getOption('F', options);
	    if (tmpStr.length() != 0)
	      setFilterAttributes(tmpStr);
	}
	
	public void setMinSupport(double value){
		m_MinSupport = value;
	}
	public void setMinConf (double value){
		m_MinConf = value;
	}
	
	public void setDataSeqID (int value){
		m_DataSeqID = value;
	}
	
	public void setFilterAttributes(String value){
		m_FilterAttributes = value;
	}
	
	public void setDebug (boolean value){
		m_Debug = value;
	}
	
	@Override
	public String[] getOptions() {
		Vector<String> result;
		
		result = new Vector<String>();
		
		if(getDebug())
			result.add("-D");
		
		result.add("-S");
	    result.add("" + getMinSupport());
	    
	    result.add("-C");
	    result.add(""+getMinConf());
	    
	    result.add("-I");
	    result.add("" + getDataSeqID());
	    
	    result.add("-F");
	    result.add(getFilterAttributes());
		
		return result.toArray(new String [result.size()]);
	}
	
	public double getMinSupport(){
		return m_MinSupport;
	}
	
	public double getMinConf(){
		return m_MinConf;
	}
	public int getDataSeqID(){
		return m_DataSeqID;
	}
	public String getFilterAttributes (){
		return m_FilterAttributes;
	}
	public boolean getDebug(){
		return m_Debug;
	}
	
	
	
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = Cmrules.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}

	public Capabilities getCapabilities(){
		Capabilities result = super.getCapabilities();
		result.disableAll();
		
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NO_CLASS);
		return result;
	}
	
	
	@Override
	public void buildAssociations(Instances data) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) throws IOException {
		String input = fileToPath("contextPrefixSpan.txt");
		String output = "C://cmrules.txt";
		double minSup = 0.75;
		double minConf = 0.5;
		
		AlgoCMRules algo = new AlgoCMRules();
		algo.runAlgorithm(input, output, minSup, minConf);
	}
	
	public String toString(){
		
		
		return null;
	}
	
	
}
