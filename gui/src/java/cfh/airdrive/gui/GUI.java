package cfh.airdrive.gui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GUI {

    public static void main(String[] args) {
        new GUI();
    }
    
    
    private final JFrame frame = new JFrame();
    
    
    private GUI() {
        SwingUtilities.invokeLater(this::initGUI);
    }
    
    private void initGUI() {
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        frame.pack();  // TODO prefs
        frame.setLocationRelativeTo(null);  // TODO prefs
        frame.setVisible(true);
    }
}
