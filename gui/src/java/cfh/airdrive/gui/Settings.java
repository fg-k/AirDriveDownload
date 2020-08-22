package cfh.airdrive.gui;

import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;


public class Settings {

    private static final String PREF_NODE_PATH = "/cfh/airdrive";

    private static final String DEFAULT_BASE_URL = "http://192.168.4.1/";
    private static String base = null;

    private static final Settings instance = new Settings();


    public static void clear() throws BackingStoreException {
        Preferences.userRoot().node(PREF_NODE_PATH).clear();
    }
    
    public static void baseURL(String base) {
        if (base.endsWith("/")) {
            Settings.base = base;
        } else {
            Settings.base = base + "/";
        }
    }
    
    public static Settings instance() {
        return instance;
    }

    private static final String PREF_FONT_NAME = "font.name";
    private static final String PREF_FONT_STYLE = "font.style";
    private static final String PREF_FONT_SIZE = "font.size";
    private static final String PREF_OUTPUT_COLS = "output.columns";
    private static final String PREF_OUTPUT_ROWS = "output.rows";
    private static final String PREF_BASE_URL = "url";
    private static final String PREF_CHARSET = "charset";
    
    private final Preferences prefs = Preferences.userRoot().node(PREF_NODE_PATH);
    
    private File lastFile = new File("");
    
    private Settings() {
    }
    
    public Font outputFont() {
        return new Font(
            prefs.get(PREF_FONT_NAME, Font.MONOSPACED),
            prefs.getInt(PREF_FONT_STYLE, Font.PLAIN), 
            prefs.getInt(PREF_FONT_SIZE, 14) );
    }

    public int outputColumns() {
        return prefs.getInt(PREF_OUTPUT_COLS, 100);
    }
    
    public int outputRows() {
        return prefs.getInt(PREF_OUTPUT_ROWS, 20);
    }

    public String base() {
        return base!=null ? base : prefs.get(PREF_BASE_URL, DEFAULT_BASE_URL);
    }

    public String downloadURL() {
        return base() + "download.html";
    }
    
    public String actionURL() {
        return downloadURL() + "?spage=%d&npage=%d&action=download";
    }
    
    public String eraseURL() {
        return base() + "?action=erase";
    }

    public String charset() {
        return prefs.get(PREF_CHARSET, "UTF-8");
    }

    public int maxPage() {
        return 9_999;
    }

    public String spinnerFormat() {
        return "#0";
    }

    public File lastFile() {
        return lastFile ;
    }

    public void lastFile(File file) {
        lastFile = file;
    }
    
    public boolean openGUI(Component parent, ActionEvent ev) {
        JTextField url = new JTextField(30);
        url.setText(prefs.get(PREF_BASE_URL, DEFAULT_BASE_URL));
        
        JComboBox<Charset> charset = new JComboBox<>(Charset.availableCharsets().values().toArray(new Charset[0]));
        charset.setSelectedItem(Charset.availableCharsets().get(charset()));
        
        JComboBox<String> fontName = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontName.setSelectedItem(prefs.get(PREF_FONT_NAME, Font.MONOSPACED));
        
        JComboBox<String> fontStyle = new JComboBox<>(new String[] {"plain", "bold", "italic", "bold italic"});
        switch (prefs.getInt(PREF_FONT_STYLE, Font.PLAIN)) {
            case Font.BOLD: fontStyle.setSelectedIndex(1); break;
            case Font.ITALIC: fontStyle.setSelectedIndex(2); break;
            case Font.BOLD|Font.ITALIC: fontStyle.setSelectedIndex(3); break;
            default: fontStyle.setSelectedIndex(0); break;
        }
        
        JSpinner fontSize = new JSpinner(new SpinnerNumberModel(prefs.getInt(PREF_FONT_SIZE, 14), 1, 40, 1));
        
        Insets insets = new Insets(2, 2, 2, 2);
        JPanel panel = new JPanel(new GridBagLayout());

        int c = 0;
        panel.add(new JLabel("URL:"), new GridBagConstraints(       0, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        panel.add(url,                new GridBagConstraints(RELATIVE, c, 3, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        
        c += 1;
        panel.add(new JLabel("Font:"), new GridBagConstraints(       0, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        panel.add(fontName,            new GridBagConstraints(RELATIVE, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        panel.add(fontStyle,           new GridBagConstraints(RELATIVE, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        panel.add(fontSize,            new GridBagConstraints(RELATIVE, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));

        c += 1;
        panel.add(new JLabel("Charset:"), new GridBagConstraints(       0, c, 1, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        panel.add(charset,                new GridBagConstraints(RELATIVE, c, 3, 1, 0.0, 0.0, ABOVE_BASELINE_LEADING, NONE, insets , 0, 0));
        
        if (showConfirmDialog(parent, panel, "Settings", OK_CANCEL_OPTION) == OK_OPTION) {
            String text = url.getText();
            if (!text.endsWith("/")) {
                text += "/";
            }
            prefs.put(PREF_BASE_URL, text);
            prefs.put(PREF_FONT_NAME, (String) fontName.getSelectedItem());
            switch ((String) fontStyle.getSelectedItem()) {
                case "plain": prefs.putInt(PREF_FONT_STYLE, Font.PLAIN); break;
                case "bold": prefs.putInt(PREF_FONT_STYLE, Font.BOLD); break;
                case "italic": prefs.putInt(PREF_FONT_STYLE, Font.ITALIC); break;
                case "bold italic": prefs.putInt(PREF_FONT_STYLE, Font.BOLD|Font.ITALIC); break;
                default: prefs.putInt(PREF_FONT_STYLE, Font.PLAIN); break;
            }
            prefs.putInt(PREF_FONT_SIZE, (Integer)fontSize.getValue());
            prefs.put(PREF_CHARSET, ((Charset)charset.getSelectedItem()).name());
            
            return true;
        } else {
            return false;
        }
    }
}
