package cfh.airdrive.gui;

import java.awt.Font;
import java.io.File;


public class Settings {

    private static String base = "http://192.168.4.1/";
    private static final Settings instance = new Settings();
    

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
        return base + "download.html";
    }
    
    public String actionURL() {
        return downloadURL() + "?spage=%d&npage=%d&action=download";
    }
    
    public String eraseURL() {
        return base + "?action=erase";
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
