package org.proofpad;

import org.proofpad.Prefs.BooleanPref;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class PrefWindow extends PPDialog {

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
	private static PrefWindow instance = null;
	private static List<FontChangeListener> fontChangeListeners =
			new LinkedList<FontChangeListener>();
	private static List<WidthGuideChangeListener> widthGuideChangeListeners =
			new LinkedList<WidthGuideChangeListener>();
	private static List<ToolbarVisibleListener> toolbarVisibleListeners =
			new LinkedList<ToolbarVisibleListener>();
	private static List<ShowLineNumbersListener> showLineNumbersListeners =
			new LinkedList<PrefWindow.ShowLineNumbersListener>();
	
	public static PrefWindow getInstance() {
		if (instance == null) {
			instance = new PrefWindow();
		}
		return instance;
	}

	private PrefWindow() {
		super(null, "Settings");
		final PrefWindow that = this;
		getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 25, 4, 25));
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		final int widthGuide = Prefs.widthGuide.get();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		final int formSpacing = 5;
		c.insets = new Insets(formSpacing, formSpacing, formSpacing, formSpacing);
		
		final JComboBox fontPicker = new JComboBox();
		fontPicker.setEnabled(false);
		fontPicker.addItem("Loading...");
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment()
						.getAvailableFontFamilyNames();
				for (String family : fontFamilies) {
					FontMetrics fm = getFontMetrics(new Font(family, Font.PLAIN, 128));
					if (fm.charWidth('.') == fm.charWidth('m')) {
						fontPicker.addItem(family);
					}
				}
				//monospaced.toArray(new String[0]);
				fontPicker.setSelectedItem(Prefs.font.get().getFamily());
				fontPicker.setEnabled(true);
			}
		});
		fontPicker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newFamily = (String) fontPicker.getSelectedItem();
				Prefs.font.set(new Font(newFamily, Font.PLAIN, Prefs.font.get().getSize()));
				fireFontChangeEvent();
			}
		});
		
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_END;
		add(new JLabel("Font:"), c);
		c.gridx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.LINE_START;
		add(fontPicker, c);
		
		c.gridwidth = 1;
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
		fontSizeComboBox.setSelectedItem(Integer.toString(Prefs.font.get().getSize()));
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
				Prefs.font.set(Prefs.font.get().deriveFont(newSize));
				fireFontChangeEvent();
			}
		});
		addFontChangeListener(new FontChangeListener() {
			@Override public void fontChanged(Font font) {
				fontSizeComboBox.setSelectedItem(Integer.toString(font.getSize()));
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
		JCheckBox showGuide = new JCheckBox("Show a width guide at");
		final JSpinner guideSpinner = new JSpinner(
				new SpinnerNumberModel(widthGuide == -1 ? 60 : widthGuide, 40, 120, 10));
		guideSpinner.setEnabled(widthGuide != -1);
		showGuide.setSelected(widthGuide != -1);
		showGuide.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
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

		c.gridx++;
		guideSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				fireWidthGuideChangeEvent((Integer) guideSpinner.getValue());
			}
		});
		guideSpinner.setEditor(new JSpinner.NumberEditor(guideSpinner));
		add(guideSpinner, c);
		c.gridx++;
		add(new JLabel("characters."), c);
		
		c.gridx = 1;
		c.gridy++;
		final JCheckBox showToolbar = new JCheckBox("Show the toolbar");
		showToolbar.setSelected(Prefs.showToolbar.get());
		showToolbar.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				toggleToolbarVisible();
			}
		});
		addToolbarVisibleListener(new ToolbarVisibleListener() {
			@Override public void toolbarVisible(boolean visible) {
				showToolbar.setSelected(visible);
			}
		});
		add(showToolbar, c);
		
		c.gridy++;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(new Separator(), c);
		c.gridwidth = GridBagConstraints.NONE;
		c.fill = GridBagConstraints.NONE;
		
		c.gridx = 1;
		c.gridy++;
		JCheckBox showErrors = new JCheckBox("Highlight potential errors with a red underline");
		showErrors.setSelected(Prefs.showErrors.get());
		showErrors.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				Prefs.showErrors.set(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		add(showErrors, c);

		c.gridx = 1;
		c.gridy++;
		JCheckBox incSearch = new JCheckBox("Find as you type");
		incSearch.setSelected(Prefs.incSearch.get());
		incSearch.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				Prefs.incSearch.set(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		add(incSearch, c);
		
		c.gridx = 1;
		c.gridy++;
		JCheckBox showLineNums = new JCheckBox("Show line numbers");
		showLineNums.setSelected(Prefs.showLineNumbers.get());
		showLineNums.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				fireShowLineNumbersEvent(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		add(showLineNums, c);
		
		c.gridx = 1;
		c.gridy++;
		final JCheckBox autoMatch = makeCheckboxForPref(Prefs.autoClose, "Smart match parentheses");
		add(autoMatch, c);
		
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
		if (Main.OSX) {
			info.putClientProperty("JComponent.sizeVariant", "small");
		} else {
			info.setFont(info.getFont().deriveFont(10f));
		}
		useBuiltinAcl2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Prefs.customAcl2.set(false);
				acl2Browse.setEnabled(false);
				acl2Path.setEnabled(false);
				info.setText(String.format(htmlTemplate, "gray", infoText));
			}
		});
		
		useCustomAcl2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Prefs.customAcl2.set(true);
				acl2Browse.setEnabled(true);
				acl2Path.setEnabled(true);
				info.setText(String.format(htmlTemplate, "black", infoText));
			}
		});
		
		boolean customAcl2Enabled = Prefs.customAcl2.get();
		acl2Browse.setEnabled(customAcl2Enabled);
		acl2Path.setEnabled(customAcl2Enabled);
		useBuiltinAcl2.setSelected(!customAcl2Enabled);
		useCustomAcl2.setSelected(customAcl2Enabled);
		info.setText(String.format(htmlTemplate, customAcl2Enabled ? "black" : "grey", infoText));
		acl2Path.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				Prefs.acl2Path.set(acl2Path.getText());
			}
		});
		acl2Browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String path;
				FileDialog fc = new FileDialog((Frame) null, "Choose ACL2 executable");
				fc.setVisible(true);
				try {
					path = new File(fc.getDirectory(), fc.getFile()).getAbsolutePath();
					Prefs.acl2Path.set(path);
					acl2Path.setText(path);
				} catch (RuntimeException ignored) { }
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
		
		c.gridy++;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(new Separator(), c);
		c.gridwidth = GridBagConstraints.NONE;
		c.fill = GridBagConstraints.NONE;
		
		c.gridx = 0;
		c.gridy++;
		add(new JLabel("Usage data:"), c);
		c.gridx = 1;
		final String[] opts = { "Ask every time", "Always send", "Never send" };
		final JComboBox usageData = new JComboBox(opts);
		usageData.setSelectedIndex(Prefs.alwaysSend.get());
		usageData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				switch (usageData.getSelectedIndex()) {
				case 0:
					Prefs.alwaysSend.set(Prefs.Codes.ASK_EVERY_TIME);
					break;
				case 1:
					Prefs.alwaysSend.set(Prefs.Codes.ALWAYS_SEND);
					break;
				case 2:
					Prefs.alwaysSend.set(Prefs.Codes.NEVER_SEND);
					break;
				}
				Prefs.alwaysSend.set(usageData.getSelectedIndex());
			}
		});
		add(usageData, c);
				
		
		///// Close button /////
		c.gridx = 1;
		c.gridy++;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_END;
		final JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				that.setVisible(false);
			}
		});
		if (!Main.OSX) { 
			add(closeButton, c);
		}
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
		acl2Path.setText(Prefs.acl2Path.get());
		setLocationRelativeTo(null);
	}

	private static JCheckBox makeCheckboxForPref(final BooleanPref pref, String text) {
		JCheckBox checkBox = new JCheckBox(text);
		checkBox.setSelected(pref.get());
		checkBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				pref.set(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		return checkBox;
	}


	public static void addFontChangeListener(FontChangeListener fcl) {
		fontChangeListeners.add(fcl);
		fcl.fontChanged(Prefs.font.get());
	}
	public static void addWidthGuideChangeListener(WidthGuideChangeListener wgcl) {
		widthGuideChangeListeners.add(wgcl);
		wgcl.widthGuideChanged(Prefs.widthGuide.get());
	}
	public static void addToolbarVisibleListener(ToolbarVisibleListener tvl) {
		toolbarVisibleListeners.add(tvl);
		tvl.toolbarVisible(Prefs.showToolbar.get());
	}
	public static void addShowLineNumbersListener(ShowLineNumbersListener showLineNumbersListener) {
		showLineNumbersListeners .add(showLineNumbersListener);
		showLineNumbersListener.lineNumbersVisible(Prefs.showLineNumbers.get());
	}
	
	protected static void fireWidthGuideChangeEvent(int value) {
		for (WidthGuideChangeListener wgcl : widthGuideChangeListeners) {
			wgcl.widthGuideChanged(value);
		}
		Prefs.widthGuide.set(value);
	}
	
	protected static void fireFontChangeEvent() {
		for (FontChangeListener fcl : fontChangeListeners) {
			fcl.fontChanged(Prefs.font.get());
		}
	}
	
	protected static void toggleToolbarVisible() {
		boolean visible = !Prefs.showToolbar.get();
		for (ToolbarVisibleListener tvl : toolbarVisibleListeners) {
			tvl.toolbarVisible(visible);
		}
		Prefs.showToolbar.set(visible);
	}

	protected static void fireShowLineNumbersEvent(boolean selected) {
		for (ShowLineNumbersListener slnl : showLineNumbersListeners) {
			slnl.lineNumbersVisible(selected);
		}
		Prefs.showLineNumbers.set(selected);
	}

}
