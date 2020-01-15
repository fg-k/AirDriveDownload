package cfh.airdrive.server;

public class Settings {

    private static final Settings instance = new Settings();
    
    public static Settings instance() {
        return instance;
    }
    
    private Settings() {
    }

    int lines() {
        return 20;
    }

    int columns() {
        return 80;
    }

    public String fontName() {
        return "monospaced";
    }

    public int fontSize() {
        return 14;
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
}
