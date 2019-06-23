package com.github.distanteye.pdf_book.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.github.distanteye.pdf_book.swing_panel_extensions.*;
import com.github.distanteye.pdf_book.ui_helpers.ComponentDefocuser;
import com.github.distanteye.pdf_book.ui_helpers.ControlFieldListener;
import com.github.distanteye.pdf_book.ui_helpers.PanelMouseScroll;
import com.github.distanteye.pdf_book.spe_wrappers.*;
import com.github.distanteye.pdf_book.spe_validators.*;

/**
 * Main UI class organizing application interface for the user. Interfaced to
 * allow for other UI possibilities, but is currently the only one
 * 
 * @author Vigilant
 *
 */
public class PDF_Book implements UI {
	private ImageRenderer renderer;
	
	private JMenuBar menuBar;
	private JMenu fileMenu, settingsMenu, helpMenu;
	private JMenuItem save, load, changeDefault, openPDF, fallBackNotice, keyboardShortcuts, installingRenderers;

	private JLabel confirmLabelPage, confirmLabelDpi;
	
	protected JFrame mainWindow;

	protected JScrollPane mainScroll, leftScroll;

	protected GBagPanel mainPanel, leftPanel, controlPanel;
	protected JPanel outputBox;
	private BorderLayout windowLayout;

	protected boolean updateEnabled;

	protected int defaultDPI;
	protected int selectedTab;

	protected int mainTextSize;

	protected ArrayList<Tab> tabs;
	
	// this could be defined just during construction but there's the possibility file menu might be rebuilt someday
	// have it as part of the state allows for handling stuff like that
	protected boolean fallBackActive; 
	
	protected boolean firstTabSwitch;
	
	// referential aids to keep code easily restructurable
	protected GBagPanel tabPanel;
	protected JScrollPane tabScroll;
	
	protected ProgressMonitor progressMonitor;

	/**
	 * Creates and initializes the UI, setting up the full layout
	 * 
	 * @throws HeadlessException
	 */
	public PDF_Book() throws HeadlessException {
		
		
		if (SimpleMuToolRenderer.IsAvailible())
		{
			renderer = new SimpleMuToolRenderer();
			fallBackActive = false;
		}
		else
		{
			renderer = new PdfBoxRenderer(new ByteArrayDataManager());
			fallBackActive = true;
		}
		
		tabs = new ArrayList<Tab>();
		selectedTab = -1; // unselected
		firstTabSwitch = true; // will be unset after the first time

		mainTextSize = 24;
		defaultDPI = 128; // can be modified later

		// Begin Top Bar

		mainWindow = new JFrame();
		windowLayout = new BorderLayout();

		mainWindow.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		mainWindow.setLayout(windowLayout);
		mainPanel = new GBagPanel();

		mainWindow.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		mainWindow.add(mainPanel);

		updateEnabled = true;
		
		// left bar - tablist			
		// create page controls if a selected Tab exists

		controlPanel = new GBagPanel();
		controlPanel.addC(new JLabel("Page"), 0, 0, 1, 1);
		controlPanel.addC(new JLabel("DPI"), 1, 0, 1, 1);
		
		// this is less than ideal : making a non scrolling ScrollPane so it syncs with the other ScrollPane borders
		JScrollPane controlPanelScroll = new JScrollPane(controlPanel);
		controlPanelScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		controlPanelScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		
		mainPanel.setNextWeights(1.0, 0.5);
		mainPanel.addC(controlPanelScroll, 0, 0, 1, 1, GridBagConstraints.BOTH);	
		mainPanel.addSpecialChild(controlPanel);

		// finish creating page controls
		
		leftPanel = new GBagPanel();		
		
		leftScroll = new JScrollPane(leftPanel);
		leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		leftScroll.setPreferredSize(new Dimension(200, 650));
		
		// enable drag scrolling
		MouseAdapter leftPanelMouseAdapter = new PanelMouseScroll(leftPanel);
		leftPanel.addMouseListener(leftPanelMouseAdapter);
		leftPanel.addMouseMotionListener(leftPanelMouseAdapter);
		
		mainPanel.addC(leftScroll, 0, 1, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
		mainPanel.addSpecialChild(leftPanel);
		
			// the rest of the leftPanel is rendered at the end at y+1 since it interacts with the main display 
		
		
		// end bar

		// setup menu -- this is done after left bar because of dependencies

		mainWindow.setSize(1600, 900);

		mainWindow.setVisible(true);
		mainWindow.setExtendedState(mainWindow.getExtendedState() | JFrame.MAXIMIZED_BOTH);

		save = new JMenuItem("Save Tabset");
		load = new JMenuItem("Load Tabset");		
		changeDefault = new JMenuItem("Change Default PPI");
		openPDF = new JMenuItem("Open PDF in new Tab");

		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(save);
		fileMenu.add(load);
		fileMenu.add(openPDF);
		
		settingsMenu = new JMenu("Settings");
		settingsMenu.add(changeDefault);

		helpMenu = new JMenu("Help");
		keyboardShortcuts = new JMenuItem("Keyboard Shortcuts");
		installingRenderers = new JMenuItem("Installing Renderer");
		helpMenu.add(keyboardShortcuts);
		helpMenu.add(installingRenderers);
		
		menuBar.add(fileMenu);
		menuBar.add(settingsMenu);
		menuBar.add(helpMenu);
		
		// message/instructiosn to display if the desired renderer isn't found
		fallBackNotice = new JMenuItem("WARN: External PDF Render Not Found! Fallback renderer will perform poorly!");
		
		if (fallBackActive)
		{			
			menuBar.add(fallBackNotice);
		}
		
		mainWindow.setJMenuBar(menuBar);
		save.addActionListener(new ClickListener());
		load.addActionListener(new ClickListener());
		changeDefault.addActionListener(new ClickListener());
		fallBackNotice.addActionListener(new ClickListener());
		openPDF.addActionListener(new TabAddListener(leftPanel, leftScroll)); 
		keyboardShortcuts.addActionListener(new ClickListener());
		installingRenderers.addActionListener(new ClickListener());
		// end menu

		

		// start main display (right)

		outputBox = new JPanel(new CardLayout());
		// mainPanel.addC(outputBox, 0, 0, 8, 12, GridBagConstraints.RELATIVE);

		

		// outputBox.addTextArea(0, 0, 30, 90);

		mainScroll = new JScrollPane(outputBox);
		mainScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		mainScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		//mainScroll.setMinimumSize(mainOut.getPreferredSize());
		
		mainScroll.setPreferredSize(new Dimension(1300, 850));
		
		// enable drag scrolling
		MouseAdapter mouseAdapter = new PanelMouseScroll(outputBox);
		outputBox.addMouseListener(mouseAdapter);
        outputBox.addMouseMotionListener(mouseAdapter);
        
        
		// add mainScroll to window, then add mainPanel to that
		mainPanel.addC(mainScroll, 1, 0, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER,
				GridBagConstraints.BOTH);
		mainPanel.addSpecialChild(outputBox); // the scroll hides bottomPanel from being registered properly by the normal infrastructure

				
		renderTabBar(leftPanel, 0, leftScroll); // needs to be last

	}
	
	protected void addKeyBindings(JTextComponent pageNumTBox, JButton pageNumLeftB, JButton pageNumRightB,
								  JTextComponent dpiTBox, JButton dpiLeftB, JButton dpiRightB)
	{
		// adds a keyboard shortcut Ctrl + [1-9] to switch between the first
		// through nine entries in the chat as combobox
		// we have to use Key Bindings to get around the focus issues, we want
		// this to be global issuable
		// note that a lot of components can have issues hosting an inputmap,
		// but textboxes are very cooperative, so we use one of the textboxes
		// with window focus event
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 1"),
				new TabSwitchAction(0));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 2"),
				new TabSwitchAction(1));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 3"),
				new TabSwitchAction(2));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 4"),
				new TabSwitchAction(3));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 5"),
				new TabSwitchAction(4));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 6"),
				new TabSwitchAction(5));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 7"),
				new TabSwitchAction(6));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 8"),
				new TabSwitchAction(7));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 9"),
				new TabSwitchAction(8));	
		
		// keyboard shortcuts for Ctrl + UpArrow/Down Arrow for switching up or down in the tab list
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK),
				new TabSwitchAction(-1,true));
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK),
				new TabSwitchAction(1,true));
		
		// keyboard shortcuts for Ctrl + Left/Right arrows changing pages - the action accesses the same action the buttons were given
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)pageNumLeftB.getActionListeners()[0]).getAction());
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)pageNumRightB.getActionListeners()[0]).getAction());
		
		// keyboard shortcuts for Ctrl + [-,+] changing DPI - the action accesses the same action the buttons were given
		// plus and minus are weird on keyboards so we do multiple for each
		
		dpiTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)dpiLeftB.getActionListeners()[0]).getAction());
		dpiTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)dpiLeftB.getActionListeners()[0]).getAction());
		
		dpiTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)dpiRightB.getActionListeners()[0]).getAction());
		dpiTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK),
				((ButtonNudgeListener)dpiRightB.getActionListeners()[0]).getAction());
		dpiTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				((ButtonNudgeListener)dpiRightB.getActionListeners()[0]).getAction());
		
		// Ctrl + G blanks the pagenum box and focuses it, allowing for easy entry of new pages to go to
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
				new AbstractAction() {
					private static final long serialVersionUID = 8165551563623502005L;

					@Override
					public void actionPerformed(ActionEvent arg0) {
						pageNumTBox.requestFocus();
						pageNumTBox.setText("");
					}
			
		});	
		
		// Ctrl + R brings up the rename prompt for the current tab
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK),
				new TabRenameListener(tabs.get(selectedTab), tabPanel, tabScroll).getAction());	
		
		// Ctrl + C clones the current tab and switches to it
				pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK),
						new TabCloneListener(tabs.get(selectedTab), tabPanel, tabScroll).getAction());	
				
		// Ctrl + O prompts the user for a new position for the current tab and executes the move
		pageNumTBox.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK),
				new TabReorderListener(selectedTab, tabPanel, tabScroll).getAction());	
	}
	
	/**
	 * Creates (or refreshes) the Controls section of the UI, returning the new y
	 * offset based on the number of rows used. Should not be called until a Tab is active and selected
	 * 
	 * @param parent
	 *            The Panel that will contain the Controls Section this is creating
	 */
	protected void renderControlBar(GBagPanel parent) {
		parent.removeAll();
		MouseListener[] toRemove = parent.getMouseListeners(); 
		for (MouseListener l : toRemove)
		{
			parent.removeMouseListener(l);
		}
		
		if (selectedTab < 0 || selectedTab >= tabs.size() )
		{
			parent.revalidate();
			parent.repaint();
			return; // don't render if there isn't a selected tab
		}
		EditState nf = EditState.NOTFIXED;
		
		
		Tab activeTab = tabs.get(selectedTab);
		
		int pageNum = activeTab.getCurrentPage();
		int maxPages = activeTab.getMaxPages();
		
		String tabName = activeTab.getDisplayName();
		String displayName = tabName;
		
		// we need to truncate some names so they fit on screen
		if (displayName.length() > 45)
		{
			displayName = displayName.substring(0, 45) + "..";
		}
		
		// put a header for the thing selected
		parent.setNextGridWidth(5);
		parent.addLabel(0, 0, "-> " + displayName);
		
		parent.addLabel(1, 1, "Page ("+pageNum+"/"+maxPages+")");
		JTextField pageNumF = parent.addMappedTF(nf, 1, 2, "", "currTabPageNum", 4, ""+pageNum, null, null, new TabPageNumWrapper(activeTab));
		pageNumF.setInputVerifier(new NumericValidator());
		
		JButton pageNumLeft = parent.addButton(0, 2, "<");
		JButton pageNumRight = parent.addButton(2, 2, ">");	
		pageNumLeft.addActionListener(new ButtonNudgeListener(pageNumF, -1));
		pageNumRight.addActionListener(new ButtonNudgeListener(pageNumF, 1));
		
		parent.addLabel(4, 1, "DPI");
		JTextField dpiF = parent.addMappedTF(nf, 4, 2, "", "currTabDPI", 4, ""+activeTab.getDpi(), null, null, new TabDpiWrapper(activeTab));
		dpiF.setInputVerifier(new NumericValidator());
		JButton dpiLeft = parent.addButton(3, 2, "<");
		JButton dpiRight = parent.addButton(5, 2, ">");
		dpiLeft.addActionListener(new ButtonNudgeListener(dpiF, -8));
		dpiRight.addActionListener(new ButtonNudgeListener(dpiF, 8));

		
		// set up update behavior for the two textboxes : they should trigger an update whenever someone hits enter or if they leave focus on the textBox
		// upon a change to the text there should be a label indicating that Enter is pressed to confirm changes
		pageNumF.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new UpdateAction() );
		dpiF.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new UpdateAction() );
		
		parent.addMouseListener(new ComponentDefocuser(pageNumF));
		parent.addMouseListener(new ComponentDefocuser(dpiF)); 
		
		// The next part are two labels that should prompt the user to hit enter to confirm changes when they're typing things in the text fields
		parent.addLabel(0, 3, " "); // this is just so the space gets occupied
		parent.setNextGridWidth(2);
		confirmLabelPage = parent.addLabel(1, 3, "");
		parent.setNextGridWidth(2);
		confirmLabelDpi = parent.addLabel(4, 3, "");
		// add the special listeners that manage the display
		pageNumF.getDocument().addDocumentListener(new ControlFieldListener(pageNumF, confirmLabelPage, "(Enter to confirm)"));
		dpiF.getDocument().addDocumentListener(new ControlFieldListener(dpiF, confirmLabelDpi, "(Enter to confirm)"));
		
		
		//parent.endVertical(0, y);

		// try and hook in the keyboard shortcuts
		addKeyBindings(pageNumF, pageNumLeft, pageNumRight, dpiF, dpiLeft, dpiRight);
		
		parent.revalidate();
		parent.repaint();
	}

	/**
	 * Creates (or recreates) the Tab section of the UI, returning the new y
	 * offset based on the number of rows used
	 * 
	 * @param parent
	 *            The Panel that will contain the Tab Section this is creating
	 * @param y
	 *            The current y offset of Parent to start adding things at
	 * @param optionalScroll
	 *            An optional reference to a scrollbar being used by this
	 *            section so the scroll position can be updated after
	 *            (re)initialization is finished
	 * @return Returns the new y offset based on the original y plus the number
	 *         of rows used by this method
	 */
	protected int renderTabBar(GBagPanel parent, int y, JScrollPane optionalScroll) {
		parent.removeAll();
		outputBox.removeAll();	
		
		tabPanel = parent;
		tabScroll = optionalScroll;
		
		int i = 0;
		for (Tab t : tabs) {
			y = addTabRow(parent, y, t, i++);
			outputBox.add(t, ""+tabs.indexOf(t));
		}

		parent.addButton(1, y++, "+").addActionListener(new TabAddListener(parent, optionalScroll));

		parent.endVertical(0, y);

		parent.revalidate();
		parent.repaint();
		
		if (selectedTab != -1)
		{
			switchTab(selectedTab);
		}
		else if (tabs.size() > 0 )
		{
			switchTab(0);
		}
		
		outputBox.repaint();					

		return y;
	}

	/**
	 * Adds a row in the table for a particular Poi, filling in values to match
	 * the stored values for that object
	 * 
	 * @param parent
	 *            The Panel that will contain the Poi subsection this is
	 *            creating
	 * @param y
	 *            The current y offset of Parent to start adding things at
	 * @param tab
	 *            The linked Tab object to use
	 * @param tabIndex
	 *            The current row number, starting at 0
	 * @return Returns the new y offset based on the original y plus the number
	 *         of rows used by this method
	 */
	protected int addTabRow(GBagPanel parent, int y, Tab tab, int tabIndex) {
		int x = 0;
				
		String tabName = tab.getDisplayName();
		String displayName = tabName;
		boolean addToolTip = false;
		
		// we need to truncate some names so they fit on screen
		if (displayName.length() > 28)
		{
			displayName = displayName.substring(0, 28) + "..";
			addToolTip = true;
		}
		
		parent.addLabel(x++, y, (tabIndex+1)+"."); // adds a 1. , 2. , 3. , etc notation
		
		JButton tabButton = parent.addButton(x++, y, displayName);
		tabButton.addActionListener(new TabSwitchAction(tabs.indexOf(tab)));
		
		if (addToolTip)
		{
			tabButton.setToolTipText(tabName);
		}				
		
		JButton renameButton = parent.addButton(x++, y, "Rename");
		renameButton.setMargin(new Insets(0,0,0,0));
		renameButton.addActionListener(new TabRenameListener(tab, parent, leftScroll));
		
		JButton reorderButton = parent.addButton(x++, y, "Reorder");
		reorderButton.setMargin(new Insets(0,0,0,0));
		reorderButton.addActionListener(new TabReorderListener(tabIndex, parent, leftScroll));
		
		JButton removeButton = parent.addButton(x++, y, "X");
		removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.addActionListener(new TabRemoveListener(parent, tabIndex, leftScroll));
		
		//parent.endRow(x, y);
		
		return y + 1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				@SuppressWarnings("unused")
				PDF_Book ui = new PDF_Book();
			}
		});

	}

	/**
	 * Updates all relevant display fields for the character.
	 */
	public void update() {
		mainPanel.updateAllComps(true);
		renderControlBar(controlPanel); // most things don't need to be nuked post update, but Control bar is finicky
	}

	@Override
	public String promptUser(String message, String extraContext) {
		String prompt = message + "\n" + extraContext;

		return JOptionPane.showInputDialog(prompt);
	}

	@Override
	public boolean handleError(String message) {
		JOptionPane.showMessageDialog(null, "Error Resulted: \n" + message, "Error", JOptionPane.ERROR_MESSAGE);

		return true;
	}

	@Override
	public void statusUpdate(String message) {
		// Unused

	}

	@Override
	public void end() {
		// No ending logic needed

	}
	
	/**
	 * Should reset the UI to a clean state
	 */
	public void reset() {
		tabs.clear();
		firstTabSwitch = true; // need to reset this flag

		renderTabBar(leftPanel, 0, leftScroll);
		renderControlBar(controlPanel);
	}
	
	public void switchTab(int idx)
	{
		try {
			if (idx < tabs.size() && idx >= 0) {
				if (selectedTab < 0 || selectedTab >= tabs.size()) {
					selectedTab = 0;
				}
				
				// we don't need to do anything with old values on first switch because old values don't/shouldn't exist
				if (!firstTabSwitch) {
					int oldScrollX = mainScroll.getHorizontalScrollBar().getValue();
					int oldScrollY = mainScroll.getVerticalScrollBar().getValue();
					tabs.get(selectedTab).setScrollPositionX(oldScrollX);
					tabs.get(selectedTab).setScrollPositionY(oldScrollY);
				}
				
				CardLayout cLayout = (CardLayout) outputBox.getLayout();
				cLayout.show(outputBox, ""+idx);
				selectedTab = idx;
				
				int scrollX = tabs.get(selectedTab).getScrollPositionX();
				int scrollY = tabs.get(selectedTab).getScrollPositionY();
				
				// something weird happens the first time around wherein scrollbars may refuse to set
				if (!firstTabSwitch) {
					mainScroll.getHorizontalScrollBar().setValue(scrollX);
					mainScroll.getVerticalScrollBar().setValue(scrollY);
				}
				else
				{
					firstTabSwitch = false;
					SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	mainScroll.getHorizontalScrollBar().setValue(scrollX);
							mainScroll.getVerticalScrollBar().setValue(scrollY);
				        }
				    });
				}							

				renderControlBar(controlPanel);

				outputBox.revalidate();
				outputBox.repaint();
				outputBox.requestFocus();
			} else {
				// silent fail is preferable for this since misskeys may be
				// common
			}
		}
		// for more general errors that can occur during this block
		catch(HandledUIException e) 
		{			
			handleError(e.getMessage());
			return;
		}
	}

	/**
	 * Converts the UI data to XML, valid for storing/saving This includes all
	 * Pois as well as most labels in text fields and the results of the chatAs
	 * box
	 * 
	 * @return Returns a String containing an XML representation of all UI data
	 */
	public String getXML() {
		String result = "<Book>\n";

		result += this.getInnerXML();

		result += "\n</Book>";

		return result;
	}

	/**
	 * Collects the UI data into a set of XML tags, not enclosed in a greater
	 * tag, this less subclasses redefine saving while still being able to draw
	 * off the superclass
	 * 
	 * @return Returns a String containing the UI vital data in a list of XML
	 *         tags
	 */
	protected String getInnerXML() {
		StringWriter result = new StringWriter();
		Element root = new Element("Book");
		Document doc = new Document(root);

		Element bookSettings = new Element("BookSettings");
		bookSettings.addContent(new Element("DefaultDPI").setText(""+defaultDPI));
		bookSettings.addContent(new Element("CurrentTab").setText(""+selectedTab));
		bookSettings.addContent(new Element("ScrollMaxX").setText(""+mainScroll.getHorizontalScrollBar().getMaximum()));
		bookSettings.addContent(new Element("ScrollMaxY").setText(""+mainScroll.getVerticalScrollBar().getMaximum()));

		doc.getRootElement().addContent(bookSettings);

		Element tabs = new Element("Tabs");

		for (Tab t : this.tabs) {
			
			int scrollPositionX = t.getScrollPositionX();
			int scrollPositionY = t.getScrollPositionY();
			
			// extra checks to make sure we get most up to date data from the currently selected tab
			if (t.equals(this.tabs.get(selectedTab)))
			{
				scrollPositionX = mainScroll.getHorizontalScrollBar().getValue();
				scrollPositionY = mainScroll.getVerticalScrollBar().getValue();
			}
			
			Element tab = new Element("Tab");
			tab.addContent(new Element("DisplayName").setText(t.getDisplayName()));
			tab.addContent(new Element("FilePath").setText("" + t.getFilePath()));
			tab.addContent(new Element("Page").setText("" + t.getCurrentPage()));
			tab.addContent(new Element("DPI").setText("" + t.getDpi()));
			tab.addContent(new Element("ScrollX").setText("" + scrollPositionX));
			tab.addContent(new Element("ScrollY").setText("" + scrollPositionY));

			tabs.addContent(tab);
		}

		doc.getRootElement().addContent(tabs);

		XMLOutputter xmlOut = new XMLOutputter();
		xmlOut.setFormat(Format.getPrettyFormat().setEncoding("UTF-8").setOmitEncoding(false));

		try {
			xmlOut.outputElementContent(root, result);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();
	}

	/**
	 * Discards the current UI data/display and replaces it with the data
	 * encoded into the xml string passed
	 * 
	 * @param xml
	 *            a validly formatted XML string as returned by getXML()
	 */
	public void loadXML(String xml) {
		reset();
		
		// this.setToDefaults();		
		progressMonitor = new ProgressMonitor(mainPanel, "", "", 0, 100);
		progressMonitor.setProgress(0);
		progressMonitor.setMillisToDecideToPopup(2);
		progressMonitor.setMillisToPopup(2);
		
		LoadTask task = new LoadTask(xml);
		task.addPropertyChangeListener(new LoadTaskListener(task));
		task.execute();
	}
	
	private class LoadTask extends SwingWorker<Void, Void> {
		private String xml;
		private boolean abortExec;
		
		public LoadTask(String xml)
		{
			this.xml = xml;
			this.abortExec = false;
		}
		
        // Doesn't occur on the Swing Graphical Thread
        @Override
        public Void doInBackground() {
        	Document document = Utils.getXMLDoc(xml);

    		Element root = document.getRootElement();

    		Utils.verifyTag(root, "Book");
    		Utils.verifyChildren(root, new String[] { "BookSettings", "Tabs" });

    		Element tabSettings = root.getChild("BookSettings");
    		Utils.verifyChildren(tabSettings,
    				new String[] { "DefaultDPI", "CurrentTab", "ScrollMaxX", "ScrollMaxY" });
    		if (!Utils.isInteger(tabSettings.getChildText("DefaultDPI"))) {
    			throw new IllegalArgumentException("Load error : DefaultDPI isn't a number");
    		} else {
    			defaultDPI = Integer.parseInt(tabSettings.getChildText("DefaultDPI"));
    		}
    		
    		if (!Utils.isInteger(tabSettings.getChildText("CurrentTab"))) {
    			throw new IllegalArgumentException("Load error : CurrentTab isn't a number");
    		} else {
    			selectedTab = Integer.parseInt(tabSettings.getChildText("CurrentTab"));
    		}
    		
    		if (!Utils.isInteger(tabSettings.getChildText("ScrollMaxX"))) {
    			throw new IllegalArgumentException("Load error : ScrollMaxX isn't a number");
    		} else {
    			int scrollMaxX = Integer.parseInt(tabSettings.getChildText("ScrollMaxX"));
    			mainScroll.getHorizontalScrollBar().setMaximum(scrollMaxX);
    		}
    		
    		if (!Utils.isInteger(tabSettings.getChildText("ScrollMaxY"))) {
    			throw new IllegalArgumentException("Load error : ScrollMaxY isn't a number");
    		} else {
    			int scrollMaxY = Integer.parseInt(tabSettings.getChildText("ScrollMaxY"));
    			mainScroll.getVerticalScrollBar().setMaximum(scrollMaxY);
    		}

    		Element tabsXML = root.getChild("Tabs");
    		Utils.verifyChildren(tabsXML, new String[] { "Tab" });	    		

    		int currentProgress = 1;
    		setProgress(currentProgress);
    		int numTabs = tabsXML.getChildren().size();
    		int progressEach = 99/numTabs;
    		
    		
    		for (Element e : tabsXML.getChildren()) {
    			// another hard catch to prevent execution after a stop is called for
    			if (abortExec) {
    				return null;
    			}
    			
    			if (e.getName().equals("Tab")) {
    				Utils.verifyChildren(e,
    						new String[] { "DisplayName", "FilePath", "Page", "DPI", "ScrollX", "ScrollY" });

    				String displayName = e.getChildText("DisplayName");
    				String filePath = e.getChildText("FilePath");
    				
    				int page = 0;
    				if (!Utils.isInteger(e.getChildText("Page"))) {
    					throw new IllegalArgumentException("Load error : Page isn't a number");    					
    				} else {
    					page = Integer.parseInt(e.getChildText("Page"));
    				}
    				
    				int dpi = 0;
    				if (!Utils.isInteger(e.getChildText("DPI"))) {
    					throw new IllegalArgumentException("Load error : DPI isn't a number");
    				} else {
    					dpi = Integer.parseInt(e.getChildText("DPI"));
    				}
    				
    				int scrollX = 0;
    				if (!Utils.isInteger(e.getChildText("ScrollX"))) {
    					throw new IllegalArgumentException("Load error : ScrollX isn't a number");
    				} else {
    					scrollX = Integer.parseInt(e.getChildText("ScrollX"));
    				}
    				
    				int scrollY = 0;
    				if (!Utils.isInteger(e.getChildText("ScrollY"))) {
    					throw new IllegalArgumentException("Load error : ScrollY isn't a number");
    				} else {
    					scrollY = Integer.parseInt(e.getChildText("ScrollY"));
    				}
    				
    				
    				try {
    					Tab t = new Tab(displayName, filePath, page, dpi, renderer);
    					t.setScrollPositionX(scrollX);
    					t.setScrollPositionY(scrollY);
    					tabs.add(t);
    				} catch (InvalidPasswordException e1) {
    					throw new IllegalArgumentException("Load error: " + e1.getMessage());
    				} catch (IOException e1) {
    					throw new IllegalArgumentException("Load error: " + e1.getMessage());
    				}	
    			}
    			
    			currentProgress += progressEach;
    			setProgress(currentProgress);
    		}
        	
            return null;
        }
 
        // occurs on the Swing Graphical Thread
        @Override
        public void done() {
        	try {
        		get();
        	}
        	catch( CancellationException e) 
        	{
        		abortExec = true;
        		handleError("Load canceled.");
        		return;
        	}
        	catch (ExecutionException | InterruptedException e) {
        		handleError(e.getMessage());
        		reset();
        		return;
        	}
        	
        	renderTabBar(leftPanel, 0, leftScroll);
    		update();
    		
    		progressMonitor.close();
        }
    }	
	
	private class LoadTaskListener implements PropertyChangeListener {
		private LoadTask task;
		
		public LoadTaskListener(LoadTask task) {
			this.task = task;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName() ) {
	            int progress = (int) evt.getNewValue();
	            progressMonitor.setProgress(progress);
	            String message = String.format("Loaded %d%%.\n", progress);
	            progressMonitor.setNote(message);
	            
	            if (progressMonitor.isCanceled())
	            {
	            	task.cancel(true);
	            	reset();	            	
	            }	            
	        }
		}
	}
	

	public void save(String fileName) {
		try {
			PrintWriter fileO = new PrintWriter(new FileOutputStream(fileName));
			update();
			fileO.println(getXML());
			fileO.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File : \"" + fileName + "\" could not be created");
		}
	}

	public void load(String fileName) {
		try {
			File f = new File(fileName);
			FileInputStream fin = new FileInputStream(f);
			byte[] buffer = new byte[(int) f.length()];
			new DataInputStream(fin).readFully(buffer);
			fin.close();
			String temp = new String(buffer, "UTF-8");

			loadXML(temp);

		} catch (FileNotFoundException e) {
			handleError("File : \"" + fileName + "\" could not be found");
		} catch (IOException e) {
			handleError("File : \"" + fileName + "\" exists but could not be loaded");
		}
	}
	
	private class ButtonNudgeListener implements ActionListener {
		private JTextField target;
		private int nudgeAmount;

		/**
		 * ActionListener for a button linked to an integer containing textfield. Will increment/decrement the text field on click
		 * @param target The JTextField to modify
		 * @param nudgeAmount the amount to modify by on click
		 */
		public ButtonNudgeListener(JTextField target, int nudgeAmount) {
			super();
			this.target = target;
			this.nudgeAmount = nudgeAmount;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String initialValueStr = target.getText();
			if (!Utils.isInteger(initialValueStr))
			{
				// this should never happen in proper use since validators should be used to force integers only on the target field
				throw new IllegalArgumentException("Value is not an integer: " + initialValueStr); 
			}
			
			int initialValue = Integer.parseInt(initialValueStr);
			int newValue = initialValue + nudgeAmount;
			
			target.setText(""+newValue);
			update();
		}
		
		// aliases the actionPerformed so getAction can execute it
		protected void parentAction()
		{
			actionPerformed(null);
		}

		@SuppressWarnings("serial")
		public AbstractAction getAction()
		{
			return new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parentAction();
				}				
			};
		}
	}
	
	private class TabRemoveListener implements ActionListener {
		private GBagPanel parent;
		private int tabIdx;
		private JScrollPane scroll;

		public TabRemoveListener(GBagPanel parent, int tabIdx, JScrollPane scroll) {
			this.parent = parent;
			this.tabIdx = tabIdx;
			this.scroll = scroll;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Tab t = tabs.remove(tabIdx);
			
			t.onLeaving();
						
			if (selectedTab == tabIdx)
			{
				selectedTab--; // if we're deleting the tab currently in view, fall back to the previous one
			}			

			renderTabBar(parent, 0, scroll);	
			//renderControlBar(controlPanel); // is now implied by update
			update();
		}

	}

	private class TabAddListener implements ActionListener {
		private GBagPanel parent;
		private JScrollPane scroll;

		public TabAddListener(GBagPanel parent, JScrollPane scroll) {
			this.parent = parent;
			this.scroll = scroll;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF Documents", "pdf");
			chooser.setFileFilter(filter);
			int returnVal = chooser.showOpenDialog(mainWindow);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String filePath = chooser.getSelectedFile().getAbsolutePath();
				
				Cursor old = mainWindow.getCursor();
				mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				try {
					tabs.add(new Tab(filePath, filePath, 1, defaultDPI, renderer));
				} catch (InvalidPasswordException e1) {
					handleError(e1.getMessage());
				} catch (IOException e1) {
					handleError(e1.getMessage());
				}
				renderTabBar(parent, 0, scroll);
				switchTab(tabs.size()-1);
				
				mainWindow.setCursor(old);
			}				
		}
	}
	
	private class TabCloneListener implements ActionListener {
		private GBagPanel parent;
		private JScrollPane scroll;
		private Tab orig;

		public TabCloneListener(Tab orig, GBagPanel parent, JScrollPane scroll) {
			this.parent = parent;
			this.scroll = scroll;
			this.orig = orig;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int origPosition = tabs.indexOf(orig);
			
			try {
				// what's stored in orig may not be 100% up to date since normally it doesn't commit scroll positions until you change tabs
				int oldScrollX = mainScroll.getHorizontalScrollBar().getValue();
				int oldScrollY = mainScroll.getVerticalScrollBar().getValue();
				
				//create a new tab based on as many of the old one's settings as possible
				Tab t = new Tab(orig.getDisplayName()+"_", orig.getFilePath(), orig.getCurrentPage(), orig.getDpi(), renderer);
				t.setScrollPositionX(oldScrollX);
				t.setScrollPositionY(oldScrollY);
				
				tabs.add(origPosition+1, t);
				
			} catch (InvalidPasswordException e1) {
				handleError(e1.getMessage());
			} catch (IOException e1) {
				handleError(e1.getMessage());
			}
			renderTabBar(parent, 0, scroll);
			switchTab(origPosition+1);			
		}
		
		// aliases the actionPerformed so getAction can execute it
		protected void parentAction()
		{
			actionPerformed(null);
		}

		@SuppressWarnings("serial")
		public AbstractAction getAction()
		{
			return new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parentAction();
				}				
			};
		}
	}
	
	private class TabRenameListener implements ActionListener {
		private GBagPanel parent;
		private JScrollPane scroll;
		private Tab tab;

		public TabRenameListener(Tab t, GBagPanel parent, JScrollPane scroll) {
			this.tab = t;
			this.parent = parent;
			this.scroll = scroll;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String newName = promptUser("Choose new displayName:","");
			
			if (newName == null || newName.trim().length() == 0)
			{
				return;
			}
			
			tab.setDisplayName(newName);
			renderTabBar(parent, 0, scroll);						
		}
		
		// aliases the actionPerformed so getAction can execute it
		protected void parentAction()
		{
			actionPerformed(null);
		}

		@SuppressWarnings("serial")
		public AbstractAction getAction()
		{
			return new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parentAction();
				}				
			};
		}

	}
	
	private class TabReorderListener implements ActionListener {
		private GBagPanel parent;
		private JScrollPane scroll;
		private int tabIdx;

		public TabReorderListener(int tabIdx, GBagPanel parent, JScrollPane scroll) {
			this.tabIdx = tabIdx;
			this.parent = parent;
			this.scroll = scroll;
		}

		@Override
		public void actionPerformed(ActionEvent e) {			
			Tab selectedTabObj = tabs.get(selectedTab);
			
			String range = "1-" + tabs.size();
			String promptMsg = "Enter the new position (" + range + "):";
			
			String newIdxStr = promptUser(promptMsg,"");
			
			while (!Utils.isInteger(newIdxStr) || Integer.parseInt(newIdxStr) < 1 || Integer.parseInt(newIdxStr) > tabs.size())
			{
				newIdxStr = promptUser(promptMsg,"");
			}

			
			int newIdx = Integer.parseInt(newIdxStr)-1;
			
			Tab toAdd = tabs.remove(tabIdx);
			
			tabs.add(newIdx,toAdd);
			selectedTab = tabs.indexOf(selectedTabObj); // make sure selectedTab still points to the same tab it was pointing to before
			
			renderTabBar(parent, 0, scroll);						
		}
		
		// aliases the actionPerformed so getAction can execute it
		protected void parentAction()
		{
			actionPerformed(null);
		}

		@SuppressWarnings("serial")
		public AbstractAction getAction()
		{
			return new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parentAction();
				}				
			};
		}

	}

	/**
	 * For changing the active tab : when actionPerformed
	 * is triggered (based on InputMap), switches tabs to go to either a predefined location, or a relative one
	 * 
	 * @author Vigilant
	 */
	private class TabSwitchAction extends AbstractAction {

		private static final long serialVersionUID = 14624L;
		private int selectedIdx;
		private boolean relative;

		public TabSwitchAction(int idx) {
			this.selectedIdx = idx;
			this.relative = false;
		}
		
		public TabSwitchAction(int idx, boolean relative) {
			this.selectedIdx = idx;
			this.relative = relative;
		}

		public void actionPerformed(ActionEvent e) {
			int target = selectedIdx;
			if (relative)
			{
				target = selectedTab+selectedIdx;
			}
			
			switchTab(target);
		}
	}
	
	/**
	 * Whenever we want to force a UI update (Push and pull events for text fields execute
	 * 
	 * @author Vigilant
	 */
	@SuppressWarnings("serial")
	private class UpdateAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			update();
		}
	}	
	
	private class ClickListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(save)) {
				JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Data Files", "xml", "txt", "config");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(mainWindow);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					save(chooser.getSelectedFile().getName());
				}
			} else if (e.getSource().equals(load)) {
				JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Data Files", "xml", "txt", "config");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(mainWindow);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					load(chooser.getSelectedFile().getName());
				}
			} else if (e.getSource().equals(changeDefault)) {
				String newDPI = promptUser("Choose new default DPI:","");
				
				if (newDPI == null || !Utils.isInteger(newDPI) && Integer.parseInt(newDPI) <= 0)
				{
					handleError("New DPI wasn't a valid positive number");
				}
				else
				{
					defaultDPI = Integer.parseInt(newDPI);
				}
			} else if (e.getSource().equals(fallBackNotice) || e.getSource().equals(installingRenderers)) {
				String title = "Renderer Not Found";
				int dialogType = JOptionPane.ERROR_MESSAGE;
				
				// installingRenderers shares the message but not the error indicators
				if (e.getSource().equals(installingRenderers))
				{
					title = "Renderer Install";
					dialogType = JOptionPane.INFORMATION_MESSAGE;
				}
				
				String message = "PDF Book looks for the presence of /external/mutool for the primary PDF Rendering engine.\n\n" +
									"Obtain the appropriate executable from https://mupdf.com/ and copy or symbolic link it to that location.\n\n" +
									"The fallback renderer will not be able to open all PDFs, is slower, and consumes a great deal of memory.\n\n" +
									"( /external/pageCount.js also needs to be present but this is there by default unless the user removes it )";
				
				// we use a text box so the text (particularly the url) becomes selectable
				JTextArea textBox = new JTextArea(message,8,52);              
				textBox.setEditable(false);
				textBox.setLineWrap(true);
				textBox.setOpaque(false);
				textBox.setBackground(new Color(0,0,0,0));
				
				JOptionPane.showMessageDialog(null, textBox, title, dialogType);
			} else if (e.getSource().equals(keyboardShortcuts)) {
				String message = "Ctrl + [1-9] will switch to the first through ninth tab.\n\n" +
						"Ctrl + [Up/Down] will switch one up or one down.\n\n" +
						"Ctrl + [Left/Right] will change the current page on the selected tab.\n\n" +
						"Ctrl + [-/+] will change the DPI (image size) on the selected tab.\n\n" +
						"Ctrl + c will clone the current tab and switch to the clone.\n\n" +
						"Ctrl + r will bring up the rename prompt\n\n" +
						"Ctrl + g will focus the page num text field and clear it, so a new page can be entered.\n\n" +
						"Ctrl + o will bring up a prompt to change the current tab's position, moving it higher/lower in the list.\n\n" +
						"Tip: The PageNum and DPI textboxes may block shortcuts when active. Click outside of them to break focus";
				JOptionPane.showMessageDialog(null, message, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
			}
			
		}
	}
	
}
