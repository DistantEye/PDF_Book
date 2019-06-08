package com.github.distanteye.pdf_book.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.github.distanteye.pdf_book.swing_panel_extensions.*;
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
	private JMenuBar menuBar;
	private JMenu fileMenu, settingsMenu;
	private JMenuItem save, load, changeDefault, openPDF;

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

	/**
	 * Creates and initializes the UI, setting up the full layout
	 * 
	 * @throws HeadlessException
	 */
	public PDF_Book() throws HeadlessException {
		tabs = new ArrayList<Tab>();
		selectedTab = -1; // unselected

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
		leftScroll.setPreferredSize(new Dimension(200, 850));
		
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
		
		menuBar.add(fileMenu);
		menuBar.add(settingsMenu);
		
		mainWindow.setJMenuBar(menuBar);
		save.addActionListener(new ClickListener());
		load.addActionListener(new ClickListener());
		changeDefault.addActionListener(new ClickListener());
		openPDF.addActionListener(new TabAddListener(leftPanel, leftScroll)); 

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

		// adds a keyboard shortcut Ctrl + [0-9] to switch between the first
		// through nine entries in the chat as combobox
		// we have to use Key Bindings to get around the focus issues, we want
		// this to be global issuable
		// note that a lot of components can have issues hosting an inputmap,
		// but textboxes are very cooperative, so we use one of the textboxes
		// with window focus event
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("1"),
				new TabSwitchAction(1));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("2"),
				new TabSwitchAction(2));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 3"),
				new TabSwitchAction(3));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 4"),
				new TabSwitchAction(4));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 5"),
				new TabSwitchAction(5));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 6"),
				new TabSwitchAction(6));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 7"),
				new TabSwitchAction(7));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 8"),
				new TabSwitchAction(8));
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 9"),
				new TabSwitchAction(9));	
		
		renderTabBar(leftPanel, 0, leftScroll); // needs to be last

	}
	
	/**
	 * Creates (or recreates) the Controls section of the UI, returning the new y
	 * offset based on the number of rows used. Should not be called until a Tab is active and selected
	 * 
	 * @param parent
	 *            The Panel that will contain the Controls Section this is creating
	 * @param y
	 *            The current y offset of Parent to start adding things at
	 * @return Returns the new y offset based on the original y plus the number
	 *         of rows used by this method
	 */
	protected int renderControlBar(GBagPanel parent, int y) {
		if (selectedTab < 0 || selectedTab >= tabs.size() )
		{
			throw new IllegalArgumentException("Can't render control bar without an active tab");
		}
		EditState nf = EditState.NOTFIXED;
		parent.removeAll();
		
		Tab activeTab = tabs.get(selectedTab);
		
		int pageNum = activeTab.getCurrentPage();
		int maxPages = activeTab.getMaxPages();
		
		parent.addLabel(1, 0, "Page ("+pageNum+"/"+maxPages+")");
		JTextField pageNumF = parent.addMappedTF(nf, 1, y, "", "currTabPageNum", 4, ""+pageNum, null, this, new TabPageNumWrapper(activeTab));
		pageNumF.setInputVerifier(new NumericValidator());
		parent.addButton(0, y, "<").addActionListener(new ButtonNudgeListener(pageNumF, -1));
		parent.addButton(2, y, ">").addActionListener(new ButtonNudgeListener(pageNumF, 1));
		
		parent.addLabel(4, 0, "DPI");
		JTextField dpiF = parent.addMappedTF(nf, 4, y, "", "currTabDPI", 4, ""+activeTab.getDpi(), null, this, new TabDpiWrapper(activeTab));
		dpiF.setInputVerifier(new NumericValidator());
		parent.addButton(3, y, "<").addActionListener(new ButtonNudgeListener(dpiF, -8));
		parent.addButton(5, y, ">").addActionListener(new ButtonNudgeListener(dpiF, 8));

		//parent.endVertical(0, y);

		parent.revalidate();
		parent.repaint();
			

		return y+1;
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
		
		int i = 0;
		for (Tab t : tabs) {
			y = addTabRow(parent, y, t, i++);
			outputBox.add(t, ""+tabs.indexOf(t));
		}

		parent.addButton(0, y++, "+").addActionListener(new TabAddListener(parent, optionalScroll));

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

		if (optionalScroll != null) {
			JScrollBar verticalBar = optionalScroll.getVerticalScrollBar();
			verticalBar.setValue(verticalBar.getMaximum());
			optionalScroll.revalidate();
			optionalScroll.repaint();

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
	 * @param poiIndex
	 *            The current row number, starting at 0
	 * @return Returns the new y offset based on the original y plus the number
	 *         of rows used by this method
	 */
	protected int addTabRow(GBagPanel parent, int y, Tab tab, int poiIndex) {
		int x = 0;

		parent.addButton(x++, y, tab.getDisplayName()).addActionListener(new TabSwitchAction(tabs.indexOf(tab)));
		parent.addButton(x++, y, "##X").addActionListener(new TabRemoveListener(parent, poiIndex, leftScroll));
		parent.addButton(x++, y, "##Save").addActionListener(new TabRenameListener(tab, parent, leftScroll));
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
	 * 
	 * Note update should respect updateEnabled, and not act if
	 * updateEnabled=false
	 */
	public void update() {
		mainPanel.updateAllComps(true);
	}

	/**
	 * Tells the UI to refresh all components, pulling new values from the
	 * backend for everything. Typically called when the underlying character
	 * has had a massive change.
	 */
	public void refreshAll() {
		updateEnabled = false; // we don't want to trigger any updates during
								// this rebuild phase, and listeners CAN trigger

		mainPanel.refreshAllComps(true);

		updateEnabled = true;
		update();
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
			tabs.remove(tabIdx);

			renderTabBar(parent, 0, scroll);
			refreshAll();
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
				
				try {
					tabs.add(new Tab(filePath, filePath, 0, defaultDPI));
				} catch (InvalidPasswordException e1) {
					handleError(e1.getMessage());
				} catch (IOException e1) {
					handleError(e1.getMessage());
				}
				renderTabBar(parent, 0, scroll);
			}				
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

	}
	
	public void switchTab(int idx)
	{
		try {
			if (tabs.size() > idx && idx >= 0) {
				if (selectedTab < 0 || selectedTab >= tabs.size()) {
					selectedTab = 0;
				}
				
				int oldScrollX = mainScroll.getHorizontalScrollBar().getValue();
				int oldScrollY = mainScroll.getVerticalScrollBar().getValue();
				tabs.get(selectedTab).setScrollPositionX(oldScrollX);
				tabs.get(selectedTab).setScrollPositionY(oldScrollY);			
				
				try {
					tabs.get(selectedTab).onLeaving();
				} catch (IOException e) {
					handleError(e.getMessage());
					return;
				}
				
				CardLayout cLayout = (CardLayout) outputBox.getLayout();
				cLayout.show(outputBox, ""+idx);
				selectedTab = idx;
				
				int scrollX = tabs.get(selectedTab).getScrollPositionX();
				int scrollY = tabs.get(selectedTab).getScrollPositionY();
				
				mainScroll.getHorizontalScrollBar().setValue(scrollX);
				mainScroll.getVerticalScrollBar().setValue(scrollY);
				
				renderControlBar(controlPanel, 1);
				
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

		doc.getRootElement().addContent(bookSettings);

		Element tabs = new Element("Tabs");

		for (Tab t : this.tabs) {
			Element tab = new Element("Tab");
			tab.addContent(new Element("DisplayName").setText(t.getDisplayName()));
			tab.addContent(new Element("FilePath").setText("" + t.getFilePath()));
			tab.addContent(new Element("Page").setText("" + t.getCurrentPage()));
			tab.addContent(new Element("DPI").setText("" + t.getDpi()));
			tab.addContent(new Element("ScrollX").setText("" + t.getScrollPositionX()));
			tab.addContent(new Element("ScrollY").setText("" + t.getScrollPositionY()));

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
		// this.setToDefaults();

		Document document = Utils.getXMLDoc(xml);

		Element root = document.getRootElement();

		Utils.verifyTag(root, "Book");
		Utils.verifyChildren(root, new String[] { "BookSettings", "Tabs" });

		Element tabSettings = root.getChild("BookSettings");
		Utils.verifyChildren(tabSettings,
				new String[] { "DefaultDPI", "CurrentTab" });
		if (!Utils.isInteger(tabSettings.getChildText("DefaultDPI"))) {
			handleError("Load error : DefaultDPI isn't a number");
			return;
		} else {
			this.defaultDPI = Integer.parseInt(tabSettings.getChildText("DefaultDPI"));
		}
		
		if (!Utils.isInteger(tabSettings.getChildText("CurrentTab"))) {
			handleError("Load error : CurrentTab isn't a number");
			return;
		} else {
			this.selectedTab = Integer.parseInt(tabSettings.getChildText("CurrentTab"));
		}

		Element tabs = root.getChild("Tabs");
		Utils.verifyChildren(tabs, new String[] { "Tab" });	

		this.tabs.clear();

		for (Element e : tabs.getChildren()) {
			if (e.getName().equals("Tab")) {
				Utils.verifyChildren(e,
						new String[] { "DisplayName", "FilePath", "Page", "DPI", "ScrollX", "ScrollY" });

				String displayName = e.getChildText("DisplayName");
				String filePath = e.getChildText("FilePath");
				
				int page = 0;
				if (!Utils.isInteger(e.getChildText("Page"))) {
					handleError("Load error : Page isn't a number");
					return;
				} else {
					page = Integer.parseInt(e.getChildText("Page"));
				}
				
				int dpi = 0;
				if (!Utils.isInteger(e.getChildText("DPI"))) {
					handleError("Load error : DPI isn't a number");
					return;
				} else {
					dpi = Integer.parseInt(e.getChildText("DPI"));
				}
				
				int scrollX = 0;
				if (!Utils.isInteger(e.getChildText("ScrollX"))) {
					handleError("Load error : ScrollX isn't a number");
					return;
				} else {
					scrollX = Integer.parseInt(e.getChildText("ScrollX"));
				}
				
				int scrollY = 0;
				if (!Utils.isInteger(e.getChildText("ScrollY"))) {
					handleError("Load error : ScrollY isn't a number");
					return;
				} else {
					scrollY = Integer.parseInt(e.getChildText("ScrollY"));
				}
				
				
				try {
					Tab t = new Tab(displayName, filePath, page, dpi);
					t.setScrollPositionX(scrollX);
					t.setScrollPositionY(scrollY);
					this.tabs.add(t);
				} catch (InvalidPasswordException e1) {
					handleError("Load error: " + e1.getMessage());
				} catch (IOException e1) {
					handleError("Load error: " + e1.getMessage());
				}	
			}
		}

		renderTabBar(leftPanel, 0, leftScroll);
		this.refreshAll();

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
			}
			
		}
	}
	
	

	/**
	 * Action listener meant to work with the chatAs box : when actionPerformed
	 * is triggered (based on InputMap), switches the chatAs combobox to select
	 * a particular indexed name
	 * 
	 * @author Vigilant
	 */
	private class TabSwitchAction extends AbstractAction {

		private static final long serialVersionUID = 14624L;
		private int selectedIdx;

		public TabSwitchAction(int idx) {
			this.selectedIdx = idx;
		}

		public void actionPerformed(ActionEvent e) {
			switchTab(selectedIdx);
		}
	}
	
	// adapted from https://stackoverflow.com/a/31173371/2934891
	private class PanelMouseScroll extends MouseAdapter {
		private Point origin;
		private JPanel panel;
		
		public PanelMouseScroll(JPanel panel)
		{
			this.panel = panel;
		}

		@Override
        public void mousePressed(MouseEvent e) {
            origin = new Point(e.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin != null) {
                JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, panel);
                if (viewPort != null) {
                    int deltaX = origin.x - e.getX();
                    int deltaY = origin.y - e.getY();

                    Rectangle view = viewPort.getViewRect();
                    view.x += deltaX;
                    view.y += deltaY;

                    panel.scrollRectToVisible(view);
                }
            }
        }
	}

}
