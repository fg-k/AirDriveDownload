package cfh.airdrive.gui;

import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;

import cfh.airdrive.http.HttpService;


public class GUI {

    public static void main(String[] args) {
        try {
            new GUI();
        } catch (Exception ex) {
            ex.printStackTrace();
            showMessageDialog(null, ex.getMessage(), ex.getClass().getSimpleName(), ERROR_MESSAGE);
        }
    }
    
    
    private final Settings settings = Settings.instance();
    
    private final HttpService httpService;

    private final JFrame frame = new JFrame();
    
    private final JTextField pageCount = new JTextField();
    private final JButton refreshButton = new JButton();
    
    private final JSpinner startPage = new JSpinner();
    private final JSpinner endPage = new JSpinner();
    private final JButton downloadButton = new JButton();
    
    private final JButton clearButton = new JButton();
    
    private final JTextArea output = new JTextArea();
    
    
    private GUI() {
        httpService = loadHttpService();
        SwingUtilities.invokeLater(this::initGUI);
    }
    
    private HttpService loadHttpService() {
        ServiceLoader<HttpService> loader = ServiceLoader.load(HttpService.class);
        Iterator<HttpService> iter = loader.iterator();
        if (iter.hasNext()) {
            HttpService service = iter.next();
            if (iter.hasNext()) {
                throw new RuntimeException("Http Service is not unique");
            }
            return service;
        } else {
            throw new RuntimeException("no Http Service found");
        }
    }

    private void initGUI() {
        pageCount.setEditable(false);
        pageCount.setColumns(5);
        
        refreshButton.setText("Refresh");
        refreshButton.addActionListener(this::doRefresh);
        
        Insets insets = new Insets(2, 2, 2, 2);
        
        JComponent refresh = createPanel("Refresh");
        refresh.setLayout(new GridBagLayout());
        refresh.add(new JLabel("Pages:"), new GridBagConstraints(0,        0,        1, 1, 0.0, 0.0, BASELINE_TRAILING, NONE, insets, 0, 0));
        refresh.add(pageCount,            new GridBagConstraints(RELATIVE, 0,        1, 1, 1.0, 0.0, BASELINE_LEADING,  NONE, insets, 0, 0));
        refresh.add(refreshButton,        new GridBagConstraints(0,        RELATIVE, 2, 1, 1.0, 1.0, LAST_LINE_START,   NONE, insets, 0, 0));
        
        JComponent download = createPanel("Download");
        
        JComponent log = createPanel("Log");
        
        Box panel = Box.createHorizontalBox();
        panel.add(refresh);
        panel.add(download);
        panel.add(log);
        
        output.setEditable(false);
        output.setFont(settings.outputFont());
        output.setColumns(settings.outputColumns());
        output.setRows(settings.outputRows());
        
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(output), BorderLayout.CENTER);
        
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        frame.pack();  // TODO prefs
        frame.setLocationRelativeTo(null);  // TODO prefs
        frame.setVisible(true);
        
        info("HTTP service: %s%n", httpService.getClass().getSimpleName());
    }
    
    private JComponent createPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }
    
    private void enable(boolean enabled) {
        refreshButton.setEnabled(enabled);
        startPage.setEnabled(enabled);
        endPage.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }
    
    private void doRefresh(ActionEvent ev) {
        URL url;
        try {
            url = settings.downloadURL();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            handleException("loading download page", ex);
            return;
        }
        SwingWorker<String, Void> worker = new 
        
        try (InputStream input = url.openStream()) {
            // TODO
        } catch (IOException ex) {
            handleException("reading download page", ex);
        }
    }
    
    private void handleException(String message, Throwable ex) {
        ex.printStackTrace();
        error("%s %s: %s%n", ex.getClass().getSimpleName(), message, ex.getMessage());
    }

    private void info(String format, Object... args) {
        printf("I  " + format, args);
    }
    
    private void error(String format, Object... args) {
        printf("%nE  " + format, args);
    }

    private void printf(String format, Object... args) {
        Runnable run = () -> {
            boolean atEnd;
            try {
                int currentLine = output.getLineOfOffset(output.getCaretPosition());
                atEnd = currentLine == output.getLineCount();
            } catch (BadLocationException ex) {
                ex.printStackTrace();
                atEnd = true;
            }
            output.append(String.format(format, args));
            if (atEnd) {
                output.setCaretPosition(output.getText().length());
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InvocationTargetException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
