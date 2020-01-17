package cfh.airdrive.gui;

import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private final Pattern PAGE_RANGE = Pattern.compile(""
            + "<b>START PAGE</b><br>"
            + "<br>This sets the starting page of the download\\.<br>"
            + "Range (\\d+)(?:\\.\\.\\.(\\d+))? ");

    
    
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
        enable(false);
        pageCount.setText(null);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                URL url = settings.downloadURL();
                byte[] page = httpService.read(url);
                info("Download page: %d bytes%n", page.length);
                return new String(page, settings.charset());
            }
            @Override
            protected void done() {
                String body;
                try {
                    body = get();
                } catch (InterruptedException ex) {
                    handleException("reading download page", ex);
                    return;
                } catch (ExecutionException ex) {
                    handleException("reading download page", ex.getCause());
                    return;
                }
                Matcher matcher = PAGE_RANGE.matcher(body);
                if (!matcher.find() || matcher.group(1) == null) {
                    handleException("reading page range", new IOException("unrecognized page format"));
                    return;
                }
                int first = Integer.parseInt(matcher.group(1));
                int last;
                info("first page: %d%n", first);
                if (matcher.group(2) == null) {
                    last = first;
                } else {
                    last = Integer.parseInt(matcher.group(2));
                }
                info("last page: %d%n", last);
                pageCount.setText(Integer.toString(last-first+1));
                // TODO
                enable(true);
            }
        };
        worker.execute();
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
