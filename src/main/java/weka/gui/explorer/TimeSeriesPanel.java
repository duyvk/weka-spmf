package weka.gui.explorer;

import weka.gui.*;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.timeseries.TimeSeries;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TimeSeriesPanel.java
 * Implementation of class
 * 
 * @author anilkpatro
 * @date Mar 22, 2004
 */

/**
 * This panel allows the user to select, configure, and run a scheme
 * that learns associations.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.16 $
 */
public class TimeSeriesPanel extends JPanel implements ExplorerPanel {

  /** Lets the user configure the associator */
  protected GenericObjectEditor m_AssociatorEditor = new GenericObjectEditor();

  /** The panel showing the current associator selection */
  protected PropertyPanel m_CEPanel = new PropertyPanel(m_AssociatorEditor);

  /** The output area for associations */
  protected JTextArea m_OutText = new JTextArea(20, 40);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** The buffer saving object for saving output */
  protected SaveBuffer m_SaveOut = new SaveBuffer(m_Log, this);

  /** A panel controlling results viewing */
  protected ResultHistoryPanel m_History = new ResultHistoryPanel(m_OutText);

  /** Click to start running the associator */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to stop a running associator */
  protected JButton m_StopBut = new JButton("Stop");

  /** The main set of instances we're playing with */
  protected Instances m_Instances;

  /** The user-supplied test set (if any) */
  protected Instances m_TestInstances;

  /** A thread that associator runs in */
  protected Thread m_RunThread;

  /* Register the property editors we need */
  static {
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.Filter.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.timeseries.TimeSeries.class,
		      weka.gui.GenericObjectEditor.class);
  }

  /**
   * Creates the associator panel
   */
  public TimeSeriesPanel() {

    // Connect / configure the components
    m_OutText.setEditable(false);
    m_OutText.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_OutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_OutText.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
	if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
	    != InputEvent.BUTTON1_MASK) {
	  m_OutText.selectAll();
	}
      }
    });
    m_History.setBorder(BorderFactory.createTitledBorder("Result list (right-click for options)"));
    m_History.setHandleRightClicks(false);
    // see if we can popup a menu for the selected result
    m_History.getList().addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e) {
	  if (((e.getModifiers() & InputEvent.BUTTON1_MASK)
	       != InputEvent.BUTTON1_MASK) || e.isAltDown()) {
	    int index = m_History.getList().locationToIndex(e.getPoint());
	    if (index != -1) {
	      String name = m_History.getNameAtIndex(index);
	      historyRightClickPopup(name, e.getX(), e.getY());
	    } else {
	      historyRightClickPopup(null, e.getX(), e.getY());
	    }
	  }
	}
      });

    m_AssociatorEditor.setClassType(TimeSeries.class);
    m_AssociatorEditor.setValue(new weka.timeseries.SimilarityAnalysis());
    m_AssociatorEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });

    m_StartBut.setToolTipText("Starts the analysis");
    m_StopBut.setToolTipText("Stops the analysis");
    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StartBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	startAnalysis();
      }
    });
    m_StopBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	stopAnalysis();
      }
    });

    // Layout the GUI
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createCompoundBorder(
		 BorderFactory.createTitledBorder("Time Series Analysis"),
		 BorderFactory.createEmptyBorder(0, 5, 5, 5)
		 ));
    p1.setLayout(new BorderLayout());
    p1.add(m_CEPanel, BorderLayout.NORTH);

    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1,2));
    JPanel ssButs = new JPanel();
    ssButs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ssButs.setLayout(new GridLayout(1, 2, 5, 5));
    ssButs.add(m_StartBut);
    ssButs.add(m_StopBut);
    buttons.add(ssButs);

    JPanel p3 = new JPanel();
    p3.setBorder(BorderFactory.createTitledBorder("Analysis output"));
    p3.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_OutText);
    p3.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;
      public void stateChanged(ChangeEvent e) {
	JViewport vp = (JViewport)e.getSource();
	int h = vp.getViewSize().height;
	if (h != lastHeight) { // i.e. an addition not just a user scrolling
	  lastHeight = h;
	  int x = h - vp.getExtentSize().height;
	  vp.setViewPosition(new Point(0, x));
	}
      }
    });

    GridBagLayout gbL = new GridBagLayout();
    GridBagConstraints gbC = new GridBagConstraints();
    JPanel mondo = new JPanel();
    gbL = new GridBagLayout();
    mondo.setLayout(gbL);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.NORTH;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;     gbC.gridx = 0;
    gbL.setConstraints(buttons, gbC);
    mondo.add(buttons);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;     gbC.gridx = 0; gbC.weightx = 0;
    gbL.setConstraints(m_History, gbC);
    mondo.add(m_History);
    gbC = new GridBagConstraints();
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;     gbC.gridx = 1;
    gbC.gridheight = 3;
    gbC.weightx = 100; gbC.weighty = 100;
    gbL.setConstraints(p3, gbC);
    mondo.add(p3);

    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    add(mondo, BorderLayout.CENTER);
  }

  /**
   * Sets the Logger to receive informational messages
   *
   * @param newLog the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }

  /**
   * Tells the panel to use a new set of instances.
   *
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {

    m_Instances = inst;
    String [] attribNames = new String [m_Instances.numAttributes()];
    for (int i = 0; i < attribNames.length; i++) {
      String type = "";
      switch (m_Instances.attribute(i).type()) {
      case Attribute.NOMINAL:
	type = "(Nom) ";
	break;
      case Attribute.NUMERIC:
	type = "(Num) ";
	break;
      case Attribute.STRING:
	type = "(Str) ";
	break;
      default:
	type = "(???) ";
      }
      attribNames[i] = type + m_Instances.attribute(i).name();
    }
    m_StartBut.setEnabled(m_RunThread == null);
    m_StopBut.setEnabled(m_RunThread != null);
  }

  /**
   * Starts running the currently configured associator with the current
   * settings. This is run in a separate thread, and will only start if
   * there is no associator already running. The associator output is sent
   * to the results history panel.
   */
  protected void startAnalysis() {

    if (m_RunThread == null) {
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      m_RunThread = new Thread() {
	public void run() {
	  // Copy the current state of things
	  m_Log.statusMessage("Setting up...");
	  Instances inst = new Instances(m_Instances);

	  TimeSeries timeseries = (TimeSeries) m_AssociatorEditor.getValue();
	  StringBuffer outBuff = new StringBuffer();
	  String name = (new SimpleDateFormat("HH:mm:ss - "))
	  .format(new Date());
	  String cname = timeseries.getClass().getName();
	  if (cname.startsWith("weka.timeseries.")) {
	    name += cname.substring("weka.timeseries.".length());
	  } else {
	    name += cname;
	  }
	  try {

	    // Output some header information
	    m_Log.logMessage("Started " + cname);
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskStarted();
	    }
	    outBuff.append("=== Run information ===\n\n");
	    outBuff.append("Scheme:       " + cname);
	    if (timeseries instanceof OptionHandler) {
	      String [] o = ((OptionHandler) timeseries).getOptions();
	      outBuff.append(" " + Utils.joinOptions(o));
	    }
	    outBuff.append("\n");
	    outBuff.append("Relation:     " + inst.relationName() + '\n');
	    outBuff.append("Instances:    " + inst.numInstances() + '\n');
	    outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
	    if (inst.numAttributes() < 100) {
	      for (int i = 0; i < inst.numAttributes(); i++) {
		outBuff.append("              " + inst.attribute(i).name()
			       + '\n');
	      }
	    } else {
	      outBuff.append("              [list of attributes omitted]\n");
	    }
	    m_History.addResult(name, outBuff);
	    m_History.setSingle(name);

	    // Build the model and output it.
	    m_Log.statusMessage("Building model on training data...");
	    timeseries.analyze(inst);
	    outBuff.append("=== TimeSeries model (full training set) ===\n\n");
	    outBuff.append(timeseries.toString() + '\n');
	    m_History.updateResult(name);
	    m_Log.logMessage("Finished " + cname);
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    m_Log.logMessage(ex.getMessage());
	    m_Log.statusMessage("See error log");
	  } finally {
	    if (isInterrupted()) {
	      m_Log.logMessage("Interrupted " + cname);
	      m_Log.statusMessage("See error log");
	    }
	    m_RunThread = null;
	    m_StartBut.setEnabled(true);
	    m_StopBut.setEnabled(false);
	    if (m_Log instanceof TaskLogger) {
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
  protected void stopAnalysis() {

    if (m_RunThread != null) {
      m_RunThread.interrupt();

      // This is deprecated (and theoretically the interrupt should do).
      //m_RunThread.stop();

    }
  }

  /**
   * Save the currently selected associator output to a file.
   * @param name the name of the buffer to save
   */
  protected void saveBuffer(String name) {
    StringBuffer sb = m_History.getNamedBuffer(name);
    if (sb != null) {
      if (m_SaveOut.save(sb)) {
	m_Log.logMessage("Save successful.");
      }
    }
  }

  /**
   * Handles constructing a popup menu with visualization options.
   * @param name the name of the result history list entry clicked on by
   * the user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  protected void historyRightClickPopup(String name, int x, int y) {
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem visMainBuffer = new JMenuItem("View in main window");
    if (selectedName != null) {
      visMainBuffer.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    m_History.setSingle(selectedName);
	  }
	});
    } else {
      visMainBuffer.setEnabled(false);
    }
    resultListMenu.add(visMainBuffer);

    JMenuItem visSepBuffer = new JMenuItem("View in separate window");
    if (selectedName != null) {
      visSepBuffer.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_History.openFrame(selectedName);
	}
      });
    } else {
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

    resultListMenu.show(m_History.getList(), x, y);
  }

  /**
   * Tests out the Associator panel from the command line.
   *
   * @param args may optionally contain the name of a dataset to load.
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame("Weka Knowledge Explorer: Associator");
      jf.getContentPane().setLayout(new BorderLayout());
      final TimeSeriesPanel sp = new TimeSeriesPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      weka.gui.LogPanel lp = new weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      if (args.length == 1) {
	System.err.println("Loading instances from " + args[0]);
	java.io.Reader r = new java.io.BufferedReader(
			   new java.io.FileReader(args[0]));
	Instances i = new Instances(r);
	sp.setInstances(i);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
  protected Explorer m_Explorer = null;
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
public String getTabTitle() {
	// TODO Auto-generated method stub
	return "TimeSeries";
}

@Override
public String getTabTitleToolTip() {
	// TODO Auto-generated method stub
	return "Discovering Timeseries";
}
}