package org.proofpad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PrefsWindow extends JFrame {

	private class Separator extends JComponent {
		private static final long serialVersionUID = 7305509836424390157L;
		public Separator() { }
		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(0, 0, getWidth(), 0);
		}
	}

	public interface WidthGuideChangeListener {
		void widthGuideChanged(int value);
	}

	public interface FontChangeListener {
		void fontChanged(Font font);
	}
	
	public interface ToolbarVisibleListener {
		void toolbarVisible(boolean visible);
	}

	public interface ShowLineNumbersListener {
		void lineNumbersVisible(boolean boolean1);
	}
	
	private static final long serialVersionUID = -5097145621288246384L;
	Font font;
	static Preferences prefs = Preferences.userNodeForPackage(Main.class);
	private static List<FontChangeListener> fontChangeListeners =
			new LinkedList<FontChangeListener>();
	private static List<WidthGuideChangeListener> widthGuideChangeListeners =
			new LinkedList<WidthGuideChangeListener>();
	private static List<ToolbarVisibleListener> toolbarVisibleListeners =
			new LinkedList<ToolbarVisibleListener>();
	private static List<ShowLineNumbersListener> showLineNumbersListeners =
			new LinkedList<PrefsWindow.ShowLineNumbersListener>();
	
	public PrefsWindow() {
		super("Settings");
		final PrefsWindow that = this;
		getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 25, 4, 25));
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		font = getPrefFont();
		final int widthGuide = prefs.getInt("widthguide", 60);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		//setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		final int formSpacing = 5;
		c.insets = new Insets(formSpacing, formSpacing, formSpacing, formSpacing);
		
		String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
		
		List<String> monospaced = new LinkedList<String>();
		for (String family : fontFamilies) {
		    FontMetrics fm = getFontMetrics(new Font(family, Font.PLAIN, 128));
		    if (fm.charWidth('.') == fm.charWidth('m')) {
		    	monospaced.add(family);
		    }
		}
		
		final JComboBox fontPicker =
				new JComboBox(monospaced.toArray(new String[0]));
		fontPicker.setSelectedItem(font.getFamily());
		fontPicker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newFamily = (String) fontPicker.getSelectedItem();
				font = new Font(newFamily, font.getStyle(), font.getSize());
				fireFontChangeEvent();
			}
		});
		
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("Font:"), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		add(fontPicker, c);
		
		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("Font size:"), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		//final JSpinner fontSpinner = new JSpinner(new SpinnerNumberModel(font.getSize(), 6, 24, 1));
		final JComboBox fontSizeComboBox =
				new JComboBox(new String[] {"8", "10", "12", "14", "16", "20", "24" });
		fontSizeComboBox.setEditable(true);
		fontSizeComboBox.setSelectedItem(Integer.toString(font.getSize()));
		((JTextField) fontSizeComboBox.getEditor().getEditorComponent()).setInputVerifier(
				new InputVerifier() {
					@Override
					public boolean verify(JComponent cmp) {
						JTextField tf = (JTextField) cmp;
						try {
							Float.parseFloat(tf.getText());
							return true;
						} catch (NumberFormatException e) {
							return false;
						}
					}
				});
		fontSizeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float newSize;
				try {
					newSize = Float.parseFloat(fontSizeComboBox.getSelectedItem().toString());
				} catch (NumberFormatException ex) {
					return;
				}
				font = font.deriveFont(newSize);
				fireFontChangeEvent();
			}
		});
		add(fontSizeComboBox, c);
		
		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("Width guide:"), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(formSpacing, formSpacing, 0, formSpacing);
		final JCheckBox showGuide = new JCheckBox("Show a width guide");
		final JSpinner guideSpinner = new JSpinner(
				new SpinnerNumberModel(widthGuide == -1 ? 60 : widthGuide, 40, 120, 10));
		guideSpinner.setEnabled(widthGuide != -1);
		showGuide.setSelected(widthGuide != -1);
		showGuide.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (showGuide.isSelected()) {
					fireWidthGuideChangeEvent(60);
					guideSpinner.setEnabled(true);	
					guideSpinner.setValue(60);
				} else {
					fireWidthGuideChangeEvent(-1);
					guideSpinner.setEnabled(false);
				}
			}
		});
		add(showGuide, c);
		c.gridy++;
		guideSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				fireWidthGuideChangeEvent((Integer) guideSpinner.getValue());
			}
		});
		guideSpinner.setEditor(new JSpinner.NumberEditor(guideSpinner));
		add(guideSpinner, c);
		
		c.gridx = 0;
		c.gridy++;
		
		add(new JLabel("Usage data:"), c);
		c.gridx = 1;
		final String[] opts = { "Ask every time", "Always send", "Never send" };
		final JComboBox usageData = new JComboBox(opts);
		usageData.setSelectedIndex(prefs.getInt("alwaysSend", 0));
		usageData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO: Clean this up, use constants.
				prefs.putInt("alwaysSend", usageData.getSelectedIndex());
			}
		});
		add(usageData, c);
				
		c.gridy++;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(new Separator(), c);
		c.gridwidth = GridBagConstraints.NONE;
		
		c.gridx = 1;
		c.gridy++;
		final JCheckBox incSearch = new JCheckBox("Find as you type");
		incSearch.setSelected(prefs.getBoolean("incsearch", true));
		incSearch.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				prefs.putBoolean("incsearch", incSearch.isSelected());
			}
		});
		add(incSearch, c);
		
		c.gridx = 1;
		c.gridy++;
		final JCheckBox showLineNums = new JCheckBox("Show line numbers");
		showLineNums.setSelected(prefs.getBoolean("linenums", false));
		showLineNums.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				fireShowLineNumbersEvent(showLineNums.isSelected());
			}
		});
		add(showLineNums, c);
		
		c.gridx = 1;
		c.gridy++;
		final JCheckBox showToolbar = new JCheckBox("Show the toolbar");
		showToolbar.setSelected(prefs.getBoolean("toolbarvisible", true));
		showToolbar.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				fireToolbarVisibleEvent();
			}
		});
		add(showToolbar, c);
		
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		Separator sep = new Separator();
		add(sep, c);
		c.fill = GridBagConstraints.NONE;
		
		final JButton acl2Browse = new JButton("Browse...");
		final JTextField acl2Path = new JTextField();
		ButtonGroup whichAcl2 = new ButtonGroup();
		final JRadioButton useBuiltinAcl2 = new JRadioButton("Use built-in ACL2");
		final JRadioButton useCustomAcl2 = new JRadioButton("Use custom ACL2");
		whichAcl2.add(useBuiltinAcl2);
		whichAcl2.add(useCustomAcl2);
		final String htmlTemplate = "<html><div width=\"350\" color=\"%s\">%s</div></html>";
		final String infoText = "A custom ACL2 build can allow you to use special features or " +
						"modifications, but may result in unexpected behavior and some " +
						"loss of functionality.";
		final JLabel info = new JLabel();
		
		
		info.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
		if (IdeWindow.OSX) {
			info.putClientProperty("JComponent.sizeVariant", "small");
		} else {
			info.setFont(info.getFont().deriveFont(10f));
		}
		useBuiltinAcl2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				prefs.putBoolean("customacl2", false);
				acl2Browse.setEnabled(false);
				acl2Path.setEnabled(false);
				info.setText(String.format(htmlTemplate, "gray", infoText));
			}
		});
		
		useCustomAcl2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				prefs.putBoolean("customacl2", true);
				acl2Browse.setEnabled(true);
				acl2Path.setEnabled(true);
				info.setText(String.format(htmlTemplate, "black", infoText));
			}
		});
		
		boolean customAcl2Enabled = prefs.getBoolean("customacl2", false);
		acl2Browse.setEnabled(customAcl2Enabled);
		acl2Path.setEnabled(customAcl2Enabled);
		useBuiltinAcl2.setSelected(!customAcl2Enabled);
		useCustomAcl2.setSelected(customAcl2Enabled);
		info.setText(String.format(htmlTemplate, customAcl2Enabled ? "black" : "grey", infoText));
		acl2Path.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				prefs.put("acl2Path", acl2Path.getText());
			}
		});
		acl2Browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String path = null;
				FileDialog fc = new FileDialog((Frame) null, "Choose ACL2 executable");
				fc.setVisible(true);
				try {
					path = new File(fc.getDirectory(), fc.getFile())
							.getAbsolutePath();
					prefs.put("acl2Path", path);
					acl2Path.setText(path);
				} catch (Exception e) { }
			}
		});
		c.gridx = 1;
		c.gridy++;
		c.insets = new Insets(formSpacing, formSpacing, 0, formSpacing);
		add(useBuiltinAcl2, c);
		c.gridy++;
		c.insets = new Insets(0, formSpacing, 0, formSpacing);
		add(useCustomAcl2, c);
		c.gridy++;
		c.insets = new Insets(0, formSpacing, 0, formSpacing);
		c.fill = GridBagConstraints.HORIZONTAL;
		JPanel acl2Picker = new JPanel();
		acl2Picker.setLayout(new BoxLayout(acl2Picker, BoxLayout.X_AXIS));
		acl2Picker.add(acl2Path);
		acl2Picker.add(Box.createHorizontalStrut(2));
		acl2Picker.add(acl2Browse);
		add(acl2Picker, c);
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(0, formSpacing, formSpacing, formSpacing);
		c.gridx = 1;
		c.gridy++;
		add(info, c);
		
		///// Close button /////
		c.gridx = 1;
		c.gridy++;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		final JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				that.setVisible(false);
			}
		});
		add(closeButton, c);
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent arg0) {
				closeButton.requestFocus();
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		});
		setMinimumSize(new Dimension(500, 1));
		pack();
		acl2Path.setText(prefs.get("acl2Path", ""));
		setLocationRelativeTo(null);
	}


	public static void addFontChangeListener(FontChangeListener fcl) {
		fontChangeListeners.add(fcl);
		fcl.fontChanged(getPrefFont());
	}
	public static void addWidthGuideChangeListener(WidthGuideChangeListener wgcl) {
		widthGuideChangeListeners.add(wgcl);
		wgcl.widthGuideChanged(prefs.getInt("widthguide", 60));
	}
	public static void addToolbarVisibleListener(ToolbarVisibleListener tvl) {
		toolbarVisibleListeners.add(tvl);
		tvl.toolbarVisible(prefs.getBoolean("toolbarvisible", true));
	}
	public static void addShowLineNumbersListener(ShowLineNumbersListener showLineNumbersListener) {
		showLineNumbersListeners .add(showLineNumbersListener);
		showLineNumbersListener.lineNumbersVisible(prefs.getBoolean("linenums", false));
	}
	
	protected static void fireWidthGuideChangeEvent(int value) {
		for (WidthGuideChangeListener wgcl : widthGuideChangeListeners) {
			wgcl.widthGuideChanged(value);
		}
		prefs.putInt("widthguide", value);
	}
	protected void fireFontChangeEvent() {
		for (FontChangeListener fcl : fontChangeListeners) {
			fcl.fontChanged(font);
		}
		prefs.putInt("fontsize", font.getSize());
		prefs.put("fontfamily", font.getFamily());
	}
	protected static void fireToolbarVisibleEvent() {
		boolean visible = !prefs.getBoolean("toolbarvisible", true);
		for (ToolbarVisibleListener tvl : toolbarVisibleListeners) {
			tvl.toolbarVisible(visible);
		}
		prefs.putBoolean("toolbarvisible", visible);
	}

	protected static void fireShowLineNumbersEvent(boolean selected) {
		for (ShowLineNumbersListener slnl : showLineNumbersListeners) {
			slnl.lineNumbersVisible(selected);
		}
		prefs.putBoolean("linenums", selected);
	}

	private static Font getPrefFont() {
		int fontSize = prefs.getInt("fontsize", 12);
		String defaultFamily;
		if (IdeWindow.OSX) {
			defaultFamily = "Monaco";
		} else if (IdeWindow.WIN) {
			defaultFamily = "Consolas";
		} else {
			defaultFamily = "monospaced";
		}
		String fontFamily = prefs.get("fontfamily", defaultFamily);
		return new Font(fontFamily, Font.PLAIN, fontSize);
	}

}
