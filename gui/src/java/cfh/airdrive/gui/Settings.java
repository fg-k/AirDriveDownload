package cfh.airdrive.gui;

import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URL;

public class Settings {

    private static final Settings instance = new Settings();
    
    public static Settings instance() {
        return instance;
    }
    
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

    public URL downloadURL() throws MalformedURLException {
        return new URL("http://127.0.0.1:8000/download.html");
    }
}
