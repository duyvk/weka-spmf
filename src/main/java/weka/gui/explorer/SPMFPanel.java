

package weka.gui.explorer;

import sun.security.jca.GetInstance.Instance;
import weka.associations.Associator;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.PropertyPanel;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeEvent;
import weka.gui.explorer.Explorer.CapabilitiesFilterChangeListener;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.spmf.SPMF;
import wekaexamples.classifiers.WekaDemo;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Attr;

/** 
 * This panel allows the user to select, configure, and run a scheme
 * that learns sequential pattern mining framework.
 *
 * @editor duyvk (duyvk@vnu.edu.vn)
 * @version $Revision: 7059 $
 */
public class SPMFPanel extends JPanel 
implements CapabilitiesFilterChangeListener, ExplorerPanel, LogHandler{
	
	
	protected Logger m_Log = new SysErrLog();
	protected Explorer m_Explorer = null;
	protected Instances m_Instances;
	
	protected Thread m_RunThread; //a thread that spmf runs in
	
	protected JButton m_StartBut = new JButton("Start");
	protected JButton m_StopBut = new JButton("Stop");
	
	protected GenericObjectEditor m_SPMFEditor = new GenericObjectEditor();
	
	/** The output area for associations */
	protected JTextArea m_OutText = new JTextArea(20,40);
	
	/** A panel controlling results viewing */
	protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);
	
	/** The buffer saving object for saving output */
	protected SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);
	
	/** The panel showing the current associator selection */
	  protected PropertyPanel m_CEPanel = new PropertyPanel(m_SPMFEditor);
	
	public SPMFPanel(){
		// connect / configure the components
		
		// textArea inside history panel
		m_OutText.setEditable(false);
		m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
		m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		m_OutText.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				if((e.getModifiers() & InputEvent.BUTTON1_MASK) 
						!= InputEvent.BUTTON1_MASK){ // click left button
					m_OutText.selectAll();
				}
			}
		});
		
		m_History.setBorder(BorderFactory.createTitledBorder("Result list (right-click for options)"));
		m_History.setHandleRightClicks(false);
		
		// see if we can popup a menu for the selected result
		m_History.getList().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e){
				if(((e.getModifiers() & InputEvent.BUTTON1_MASK)
						!=InputEvent.BUTTON1_MASK) || e.isAltDown()){
					int index = m_History.getList().locationToIndex(e.getPoint());
					if(index != 1){
						String name = m_History.getNameAtIndex(index);
						historyRightClickPopup(name, e.getX(), e.getY());
					}else 
						historyRightClickPopup(null, e.getX(), e.getY());
				}
			}
		});
		
		// m_SPMFEditor
		m_SPMFEditor.setClassType(SPMF.class);//?????????????????????????????????????
		m_SPMFEditor.setValue(new weka.spmf.GeneralizedSequentialPatterns());
		m_SPMFEditor.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				m_StartBut.setEnabled(true);
				//Check Capabilities
				Capabilities currentFilter = m_SPMFEditor.getCapabilitiesFilter();
				SPMF associator = (SPMF) m_SPMFEditor.getValue();
				Capabilities currentSchemeCapabilities =  null;
				if (associator != null && currentFilter != null && 
			            (associator instanceof CapabilitiesHandler)) {
			          currentSchemeCapabilities = ((CapabilitiesHandler)associator).getCapabilities();
			          
			          if (!currentSchemeCapabilities.supportsMaybe(currentFilter) &&
			              !currentSchemeCapabilities.supports(currentFilter)) {
			            m_StartBut.setEnabled(false);
			          }
			     }
				repaint();
			}
		});
		
		// Start button
		m_StartBut.setToolTipText("Starts the SMPF");
		m_StopBut.setToolTipText("Stops the SMPF");
		m_StartBut.setEnabled(false);
		m_StopBut.setEnabled(false);
		
		m_StartBut.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				startAssociator();
			}
		});
		
		m_StopBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopAssociator();
			}
		});
		
		// Layout the GUI
		
		// algorithm choosing panel
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("SPMF"),
			BorderFactory.createEmptyBorder(0,5,5,5)		
			));
		p1.setLayout(new BorderLayout());
		p1.add(m_CEPanel,BorderLayout.NORTH );
		
		// panel for start and stop button
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(1, 2)); // 1 row, 2 col
		JPanel ssButs =  new JPanel();
		ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		ssButs.setLayout(new GridLayout(1, 2, 5, 5));
		ssButs.add(m_StartBut);
		ssButs.add(m_StopBut);
		buttons.add(ssButs);
		
		// result displaying panel
		JPanel p3 = new JPanel();
		p3.setBorder(BorderFactory.createTitledBorder("SPMF output"));
		p3.setLayout(new BorderLayout());
		final JScrollPane js = new JScrollPane(m_OutText);
		p3.add(js,BorderLayout.CENTER);
		js.getViewport().addChangeListener(new ChangeListener() {
			private int lastHeight;
			@Override
			public void stateChanged(ChangeEvent e) {
				JViewport vp = (JViewport)e.getSource();
				int h = vp.getViewSize().height;
				if(h!=lastHeight){
					lastHeight =h;
					int x = h - vp.getExtentSize().height;
					vp.setViewPosition(new Point(0, x));
				}
			}
		});
		
		// global
		GridBagLayout gbL = new GridBagLayout();
		GridBagConstraints gbC = new GridBagConstraints();
		
		JPanel mondo = new JPanel();
		
		gbL = new GridBagLayout();
		mondo.setLayout(gbL);
		
		gbC = new GridBagConstraints();
		gbC.anchor = GridBagConstraints.NORTH;
		gbC.fill = GridBagConstraints.HORIZONTAL;
		gbC.gridy = 1; gbC.gridx = 0;
		gbL.setConstraints(buttons, gbC); // set constrain for Buttons panel in this layout
		mondo.add(buttons);
		
		// set constain for m_history panel
		gbC = new GridBagConstraints();
	    gbC.fill = GridBagConstraints.BOTH;
	    gbC.gridy = 2;     gbC.gridx = 0; gbC.weightx = 0;
	    gbL.setConstraints(m_History, gbC);
	    mondo.add(m_History);
		
		// set constrain for output panel
	    gbC = new GridBagConstraints();
	    gbC.fill = GridBagConstraints.BOTH;
	    gbC.gridy = 0;     gbC.gridx = 1;
	    gbC.gridheight = 3;
	    gbC.weightx = 100; gbC.weighty = 100;
	    gbL.setConstraints(p3, gbC);
	    mondo.add(p3);
		
		setLayout(new BorderLayout());
		add(p1,BorderLayout.NORTH);
		add(mondo,BorderLayout.CENTER);
		
		
	}
	
	/**
	   * Starts running the currently configured associator with the current
	   * settings. This is run in a separate thread, and will only start if
	   * there is no associator already running. The associator output is sent
	   * to the results history panel.
	   */
	protected void startAssociator(){
		if(m_RunThread == null){
			m_StartBut.setVisible(false);
			m_StopBut.setVisible(true);
			m_RunThread = new Thread(){
				public void run(){
					// copy the current state of things
					m_Log.statusMessage("Setting up...");
					Instances inst = new Instances(m_Instances);
					
					SPMF associator =(SPMF) m_SPMFEditor.getValue();
					StringBuffer outBuff = new StringBuffer();
					String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
					String cname = associator.getClass().getName();
					if(cname.startsWith("weka.associations.")){
						name += cname.substring("weka.associations.".length());
					}else{
						name+=cname;
					}
					
					String cmd = m_SPMFEditor.getValue().getClass().getName(); // return the name of class of an specific Associator
					if(m_SPMFEditor.getValue() instanceof OptionHandler)
						cmd += " " + Utils.joinOptions(((OptionHandler)m_SPMFEditor.getValue()).getOptions());
						System.out.println("cmd: "+cmd);
					try {
						// output some header information
						m_Log.logMessage("Started "+cname);
						m_Log.logMessage("Command: "+cmd);
						if(m_Log instanceof TaskLogger){
							((TaskLogger)m_Log).taskStarted();
						}
						outBuff.append("=== Run information ===\n\n");
						outBuff.append("Scheme:");
						
						if(associator instanceof OptionHandler){
							String [] o = ((OptionHandler)associator).getOptions();
							outBuff.append(" "+Utils.joinOptions(o));
						}

						outBuff.append("\n");
						outBuff.append("Relation: ");
						outBuff.append("Instances: ");
						outBuff.append("Attributes:");
						
						if(inst.numAttributes() < 100){
							for(int i =0 ; i<inst.numAttributes();i++)
								outBuff.append("          "+inst.attribute(i).name()+'\n');	
							}else 
								outBuff.append("[list of attributes omitted]\n");
						m_History.addResult(name, outBuff);
						m_History.setSingle(name);
						
						
						// build the model and output it
						m_Log.statusMessage("Building model on training data...");
////////////////////////***************************////////////////////						
						associator.buildAssociations(inst);
				
						outBuff.append("=== Associator model (full training set) ===\n\n");
						outBuff.append(associator.toString()+'\n');
	//*************************************************************		
						m_History.updateResult(name);
						m_Log.logMessage("Finished "+cname);
						m_Log.statusMessage("OK");
					}catch (Exception e) {
						m_Log.logMessage(e.getMessage());
						m_Log.statusMessage("See error log");
					}finally{
						if(isInterrupted()){
							m_Log.logMessage("Interrupted");
							m_Log.statusMessage("See error log");
						}
						m_RunThread = null;
						m_StartBut.setEnabled(true);
						m_StopBut.setEnabled(false);
						if(m_Log instanceof TaskLogger){
							((TaskLogger)m_Log).taskFinished();
						}
					}
				}
			};
			m_RunThread.setPriority(Thread.MIN_PRIORITY);
			m_RunThread.start();
		}
	}
	
	  /**
	   * Stops the currently running Associator (if any).
	   */
	@SuppressWarnings("deprecation")
	protected void stopAssociator(){
		if(m_RunThread !=null){
			m_RunThread.interrupt();
			// deprecated
			m_RunThread.stop();
		}
	}
	
	
	/**
	   * Handles constructing a popup menu with visualization options.
	   * @param name the name of the result history list entry clicked on by
	   * the user
	   * @param x the x coordinate for popping up the menu
	   * @param y the y coordinate for popping up the menu
	   */
	protected void historyRightClickPopup(String name, int x, int y){
		final String selectedName = name;
		JPopupMenu resultListMenu = new JPopupMenu();
		
		// add first item
		JMenuItem visMainBuffer = new JMenuItem("View in main window");
		if(selectedName != null){
			visMainBuffer.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					m_History.setSingle(selectedName);
				}
			});
		}else {
			visMainBuffer.setEnabled(false);
		}
		resultListMenu.add(visMainBuffer);
		
		// add 2nd item
		JMenuItem visSepBuffer = new JMenuItem("View in separate window");
		if(selectedName!=null){
			visSepBuffer.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					m_History.openFrame(selectedName);
				}
			});
		}else {
			visSepBuffer.setEnabled(false);
		}
		resultListMenu.add(visSepBuffer);
		
		JMenuItem saveOutput = new JMenuItem("Save result buffer");
	    if (selectedName != null) {
	      saveOutput.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
		    saveBuffer(selectedName);
		  }
		});
	    } else {
	      saveOutput.setEnabled(false);
	    }
	    resultListMenu.add(saveOutput);

	    JMenuItem deleteOutput = new JMenuItem("Delete result buffer");
	    if (selectedName != null) {
	      deleteOutput.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  m_History.removeResult(selectedName);
		}
	      });
	    } else {
	      deleteOutput.setEnabled(false);
	    }
	    resultListMenu.add(deleteOutput);
	    
	    resultListMenu.show(m_History.getList(), x, y);
	}
	
	/**
	   * Save the currently selected associator output to a file.
	   * @param name the name of the buffer to save
	   */
	protected void saveBuffer(String name){
		StringBuffer sb = m_History.getNamedBuffer(name);
		if(sb!=null){
			if(m_SaveOut.save(sb)){
				m_Log.logMessage("Save successful.");
			}
		}
	}
	
	
	  /**
	   * Tests out the SPMF panel from the command line.
	   *
	   * @param args may optionally contain the name of a dataset to load.
	   */
	public static void main (String[] args){
		try {
			//add a root panel
			final javax.swing.JFrame jf =
				new javax.swing.JFrame("Weka Explorer: Sequential Patern Mining Framework");
			jf.getContentPane().setLayout(new BorderLayout());
			
			//add SPMF panel
			final SPMFPanel sp = new SPMFPanel();
			jf.getContentPane().add(sp, BorderLayout.CENTER);
			
			//add log Panel
			weka.gui.LogPanel lp = new weka.gui.LogPanel();
			sp.setLog(lp);
			jf.getContentPane().add(lp, BorderLayout.SOUTH);
			
			// window closing
			jf.addWindowListener(new java.awt.event.WindowAdapter(){
				public void windowClosing(java.awt.event.WindowEvent e){
					jf.dispose();
					System.exit(0);
				}
			});
			
			// config
			jf.pack();
			jf.setVisible(true);
			
			// get Instance from arguments and pass it to SPMF panel
			if(args.length == 1){
				System.err.println("Loading instances from ");
				java.io.Reader r= new java.io.BufferedReader(
						new java.io.FileReader(args[0]));
				Instances i = new Instances(r);
				sp.setInstances(i);
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

// required method	
	
	@Override
	public void setLog(Logger newLog) {
		// TODO Auto-generated method stub
		m_Log = newLog;
	}

	@Override
	public void setExplorer(Explorer parent) {
		// TODO Auto-generated method stub
		m_Explorer = parent;
	}

	@Override
	public Explorer getExplorer() {
		// TODO Auto-generated method stub
		return m_Explorer;
	}

	@Override
	public void setInstances(Instances inst) {
		// TODO Auto-generated method stub
		m_Instances = inst;
		String [] attribNames = new String [m_Instances.numAttributes()];
		for(int i = 0 ; i < attribNames.length ; i++ ){
			String type="";
			switch (m_Instances.attribute(i).type()){
			case Attribute.NOMINAL:
				type = "Nom";
				break;
			case Attribute.NUMERIC:
				type = "Num";
				break;
			case Attribute.STRING:
				type = "Str";
				break;
			case Attribute.DATE:
				type = "Dat";
				break;
			case Attribute.RELATIONAL:
				type = "Rel";
				break;
			default:
				type = "???";
			}
			attribNames[i]=type+m_Instances.attribute(i).name();
		}
		m_StartBut.setEnabled(m_RunThread==null);
		m_StopBut.setEnabled(m_RunThread!=null);
	}

	@Override
	public String getTabTitle() {
		// TODO Auto-generated method stub
		return "SPMF";
	}

	@Override
	public String getTabTitleToolTip() {
		// TODO Auto-generated method stub
		return "Discover sequential rules";
	}

	/**
   * method gets called in case of a change event
   * 
   * @param e		the associated change event
   */
	@Override
	public void capabilitiesFilterChanged(CapabilitiesFilterChangeEvent e) {
		// TODO Auto-generated method stub
		if(e.getFilter()==null)
			updateCapabilitiesFilter(null);
		else 
			updateCapabilitiesFilter((Capabilities)e.getFilter().clone());
	}
	
	 /**
	   * updates the capabilities filter of the GOE
	   * 
	   * @param filter	the new filter to use
	   */
	protected void updateCapabilitiesFilter (Capabilities filter){
		Instances tempInst;
		Capabilities filterClass;
		
		if (filter==null){
			m_SPMFEditor.setCapabilitiesFilter(new Capabilities(null));
			return;
		}
		
		if(!ExplorerDefaults.getInitGenericObjectEditorFilter())
			tempInst = new Instances(m_Instances,0);
		else 
			tempInst = new Instances(m_Instances);
		tempInst.setClassIndex(-1);
		try {
			filterClass = Capabilities.forInstances(tempInst);
		}catch (Exception e) {
			filterClass = new Capabilities(null);
		}
		m_SPMFEditor.setCapabilitiesFilter(filterClass);
		m_StartBut.setEnabled(true);
		
		//Check capabilities
		Capabilities currentFilter = m_SPMFEditor.getCapabilitiesFilter();
		SPMF associator = (SPMF) m_SPMFEditor.getValue();
		Capabilities currentSchemeCapabilities = null;
		if (associator != null && currentFilter != null &&
				(associator instanceof CapabilitiesHandler)){
			currentSchemeCapabilities = ((CapabilitiesHandler)associator).getCapabilities();
			if(!currentSchemeCapabilities.supportsMaybe(currentFilter)&&
					!currentSchemeCapabilities.supports(currentFilter))
				m_StartBut.setEnabled(false);
		}
	}
}