package cfh.airdrive.server;

import java.awt.Font;

public class Settings {

    private static final Settings instance = new Settings();
    
    public static Settings instance() {
        return instance;
    }
    
    private Settings() {
    }

    public String title() {
        return "Test Server - 1.0";
    }

    public int outputColumns() {
        return 80;
    }
    
    public int outputRows() {
        return 20;
    }

    public Font outputFont() {
        return new Font("monospaced", Font.PLAIN, 14);
    }
    
    public String dataFileName() {
        return "test.txt";
    }

    public int pageSize() {
        return 2048;
    }

    public int port() {
        return 8000;
    }

    public int preview() {
        return 240;
    }

    public int maxPages() {
        return 5;
    }
    
    public String downloadActionPattern() {
        return "spage=(.*)&npage=(.*)&action=download";
    }
}
