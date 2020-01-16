package cfh.airdrive.gui;


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
}
