package cfh.airdrive.gui;

import java.awt.Font;
import java.io.File;


public class Settings {

    private static final Settings instance = new Settings();
    
    public static Settings instance() {
        return instance;
    }

    private File lastFile = new File("");
    
    private Settings() {
    }

    public String title() {
        return "AirDrive Keylogger Download - 0.1";
    }
    
    public Font outputFont() {
        return new Font("monospaced", Font.PLAIN, 14);
    }

    public int outputColumns() {
        return 100;
    }
    
    public int outputRows() {
        return 20;
    }

    public String downloadURL() {
        return "http://127.0.0.1:8000/download.html";
    }
    
    public String actionURL() {
        return "http://127.0.0.1:8000/download.html?spage=%d&npage=%d&action=download";
    }

    public String charset() {
        return "UTF-8";
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
}
