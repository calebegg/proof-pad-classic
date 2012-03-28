package com.calebegg.ide;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PrefsWindow extends JFrame {

	private class Separator extends JComponent {
		private static final long serialVersionUID = 7305509836424390157L;
		@Override
		public void paintComponent(Graphics g) {
			g.setColor(new Color(.4f, .4f, .4f));
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

	private static final long serialVersionUID = -5097145621288246384L;
	private Font font;
	private static Preferences prefs = Preferences.userNodeForPackage(Main.class);
	private static List<FontChangeListener> fontChangeListeners =
			new LinkedList<FontChangeListener>();
	private static List<WidthGuideChangeListener> widthGuideChangeListeners =
			new LinkedList<WidthGuideChangeListener>();
	private static List<ToolbarVisibleListener> toolbarVisibleListeners =
			new LinkedList<ToolbarVisibleListener>();
	
	public PrefsWindow() {
		super("Settings");
		final PrefsWindow that = this;
		getRootPane().setBorder(BorderFactory.createEmptyBorder(2, 25, 2, 25));
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		font = getPrefFont();
		final int widthGuide = prefs.getInt("widthguide", 60);
		setResizable(false);
		// TODO: red jewel button.
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
		
		final JComboBox fontPicker = new JComboBox(monospaced.toArray());
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
		final JComboBox fontSizeComboBox = new JComboBox(new String[] {"8", "10", "12", "14", "16", "20", "24" });
		fontSizeComboBox.setEditable(true);
		fontSizeComboBox.setSelectedItem(Integer.toString(font.getSize()));
		((JTextField) fontSizeComboBox.getEditor().getEditorComponent()).setInputVerifier(
				new InputVerifier() {
					@Override
					public boolean verify(JComponent c) {
						JTextField tf = (JTextField) c;
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
		// TODO: -1?
		final JSpinner guideSpinner = new JSpinner(new SpinnerNumberModel(widthGuide, 40, 120, 10));
		guideSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				fireWidthGuideChangeEvent((Integer) guideSpinner.getValue());
			}
		});
		guideSpinner.setEditor(new JSpinner.NumberEditor(guideSpinner));
		add(guideSpinner, c);
		
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
		if (IdeWindow.isMac) {
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
		acl2Path.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) { }
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
			public void actionPerformed(ActionEvent e) {
				that.setVisible(false);
			}
		});
		add(closeButton, c);
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent arg0) {
				closeButton.requestFocus();
			}
			@Override
			public void windowLostFocus(WindowEvent arg0) {
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
	
	protected void fireWidthGuideChangeEvent(int value) {
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
	protected void fireToolbarVisibleEvent() {
		boolean visible = !prefs.getBoolean("toolbarvisible", true);
		for (ToolbarVisibleListener tvl : toolbarVisibleListeners) {
			tvl.toolbarVisible(visible);
		}
		prefs.putBoolean("toolbarvisible", visible);
	}

	private static Font getPrefFont() {
		int fontSize = prefs.getInt("fontsize", 12);
		String defaultFamily;
		if (IdeWindow.isMac) {
			defaultFamily = "Monaco";
		} else if (IdeWindow.isWindows) {
			defaultFamily = "Consolas";
		} else {
			defaultFamily = "monospaced";
		}
		String fontFamily = prefs.get("fontfamily", defaultFamily);
		return new Font(fontFamily, Font.PLAIN, fontSize);
	}
}
