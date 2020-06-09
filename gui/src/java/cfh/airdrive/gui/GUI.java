package cfh.airdrive.gui;

import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.text.BadLocationException;

import cfh.airdrive.http.HttpService;


public class GUI {

    public static void main(String[] args) {
        if (args.length > 0) {
            Settings.baseURL(args[0]);
        }
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
    private final SpinnerNumberModel startModel = new SpinnerNumberModel(1, 1, settings.maxPage(), 1);
    private final SpinnerNumberModel endModel = new SpinnerNumberModel(settings.maxPage(), 1, settings.maxPage(), 1);

    private final JButton eraseButton = new JButton();
    
    private final JTextArea output = new JTextArea();
    
    private final Pattern PAGE_EMPTY = Pattern.compile("<b>The data log is empty.</b>");
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
        
        startModel.addChangeListener(this::doStart);
        startPage.setModel(startModel);
        startPage.setEditor(new JSpinner.NumberEditor(startPage, settings.spinnerFormat()));
        
        endPage.addChangeListener(this::doEnd);
        endPage.setModel(endModel);
        endPage.setEditor(new JSpinner.NumberEditor(endPage, settings.spinnerFormat()));
        
        downloadButton.setText("Download");
        downloadButton.addActionListener(this::doDownload);
        
        eraseButton.setText("Erase");
        eraseButton.addActionListener(this::doErase);
        
        Insets insets = new Insets(2, 2, 2, 2);
        
        JComponent refresh = createTitledPanel("Refresh");
        refresh.setLayout(new GridBagLayout());
        addGridBag("Pages:", pageCount, refresh, 0, 0, insets);
        addGridBag(refreshButton, refresh, 0, RELATIVE, insets);
        
        JComponent download = createTitledPanel("Download");
        download.setLayout(new GridBagLayout());
        addGridBag("Start: ", startPage, download, 0, 0, insets);
        addGridBag("End: ", endPage, download, 0, RELATIVE, insets);
        addGridBag(downloadButton, download, 0, RELATIVE, insets);
        
        JComponent log = createTitledPanel("Log");
        log.setLayout(new GridBagLayout());
        addGridBag(eraseButton, log, 0, RELATIVE, insets);
        
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
        frame.setTitle(String.format("AirDrive Downloader %s - ©CFH",
                getClass().getPackage().getImplementationVersion()
                )); 
        frame.setVisible(true);
        
        enable(false);
        refreshButton.setEnabled(true);
        
        Package servicePackage = httpService.getClass().getPackage();
        String serviceTitle = servicePackage.getImplementationTitle();
        if (serviceTitle == null) {
            info("HTTP service: %s%n", httpService.getClass().getSimpleName());
        } else {
            info("HTTP service: %s %s%n", serviceTitle, servicePackage.getImplementationVersion());
        }
        
        doRefresh(null);
    }
    
    private JComponent createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }
    
    private void addGridBag(String title, JComponent comp, JComponent parent, int gridx, int gridy, Insets insets) {
        parent.add(new JLabel(title), new GridBagConstraints(gridx,    gridy, 1, 1, 0.0, 0.0, BASELINE_TRAILING, NONE, insets, 0, 0));
        parent.add(comp,              new GridBagConstraints(RELATIVE, gridy, REMAINDER, 1, 1.0, 0.0, BASELINE_LEADING,  NONE, insets, 0, 0));
    }
    
    private void addGridBag(JComponent comp, JComponent parent, int gridx, int gridy, Insets insets) {
        parent.add(comp, new GridBagConstraints(gridx, gridy, REMAINDER, 1, 1.0, 1.0, LAST_LINE_START, NONE, insets, 0, 0));
    }
    
    private void enable(boolean enabled) {
        refreshButton.setEnabled(enabled);
        startPage.setEnabled(enabled);
        endPage.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        eraseButton.setEnabled(enabled);
    }
    
    private void doRefresh(ActionEvent ev) {
        enable(false);
        pageCount.setText(null);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                URL url = new URL(settings.downloadURL());
                info("URL: %s%n", url);
                byte[] page = httpService.read(url);
                info("Got download page: %d bytes%n", page.length);
                return new String(page, settings.charset());
            }
            @Override
            protected void done() {
                String body;
                try {
                    body = get();
                } catch (InterruptedException ex) {
                    handleException("reading download page", ex);
                    refreshButton.setEnabled(true);
                    return;
                } catch (ExecutionException ex) {
                    handleException("reading download page", ex.getCause());
                    refreshButton.setEnabled(true);
                    return;
                }
                parseDownloadPage(body);
            }
        };
        worker.execute();
    }
    
    private void doStart(ChangeEvent ev) {
        if (endModel.getNumber().intValue() < startModel.getNumber().intValue()) {
            endModel.setValue(startModel.getValue());
        }
    }
    
    private void doEnd(ChangeEvent ev) {
        if (startModel.getNumber().intValue() > endModel.getNumber().intValue()) {
            startModel.setValue(endModel.getValue());
        }
    }
    
    private void doDownload(ActionEvent ev) {
        int first = startModel.getNumber().intValue();
        int last = endModel.getNumber().intValue();
        info("Download pages %d to %d%n", first, last);

        File lastFile = settings.lastFile();
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setCurrentDirectory(lastFile.getAbsoluteFile().getParentFile());
        chooser.setSelectedFile(lastFile);
        if (chooser.showSaveDialog(frame) != chooser.APPROVE_OPTION) {
            info("  canceled%n%n");
            return;
        }
        File file = chooser.getSelectedFile();
        settings.lastFile(file);
        info("  selected %s%n", file.getAbsolutePath());
        
        if (file.exists()) {
            Object message = new String[] {
                file.getName(),
                "File already exists.",
                "Overwrite?"
            };
            if (showConfirmDialog(frame, message, "Confirm", YES_NO_OPTION) != YES_OPTION) {
                info("  canceled%n%n");
                return;
            }
        }
        
        enable(false);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (file.exists()) {
                    String name = file.getName();
                    int index = name.lastIndexOf('.');
                    if (index != -1) {
                        name = name.substring(0, index);
                    }
                    File bak = new File(file.getParentFile(), name + ".bak");
                    if (bak.exists()) {
                        if (bak.delete()) {
                            info("  deleted %s%n", bak);
                        }
                    }
                    if (file.renameTo(bak)) {
                        info("  %s renamed to %s%n", file, bak.getName());
                    } else {
                        throw new IOException("unable to rename " + file + " to " + bak.getName());
                    }
                }
                
                int total = 0;
                try (OutputStream output = new FileOutputStream(file)) {
                    for (int page = first; page <= last; ) {
                        int count = Math.min(last-page+1, 5);;
                        info("  page %d - %d%n", page, page+count-1);
                        URL url = new URL(String.format(settings.actionURL(), page, count));
                        byte[] data = httpService.read(url);
                        total += data.length;
                        output.write(data);
                        page += count;
                    }
                }
                info("  %d bytes saved%n", total);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException ex) {
                    handleException("downloading data", ex);
                } catch (ExecutionException ex) {
                    handleException("downloading data", ex.getCause());
                } finally {
                    enable(true);
                }
            }
        };
        worker.execute();
    }
    
    private void doErase(ActionEvent ev) {
        int opt = showConfirmDialog(frame, "Erase the entire data log?", "AirDrive - Erase Log", OK_CANCEL_OPTION);
        if (opt != OK_OPTION) 
            return;
        enable(false);
        pageCount.setText(null);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                URL url = new URL(settings.eraseURL());
                info("URL: %s%n", url);
                byte[] page = httpService.read(url);
                info("Data log erased - got download page: %d bytes%n", page.length);
                return new String(page, settings.charset());
            }
            @Override
            protected void done() {
                String body;
                try {
                    body = get();
                } catch (InterruptedException ex) {
                    handleException("erasing data log", ex);
                    refreshButton.setEnabled(true);
                    return;
                } catch (ExecutionException ex) {
                    handleException("erasing data log", ex.getCause());
                    refreshButton.setEnabled(true);
                    return;
                }
                parseDownloadPage(body);
            }
        };
        worker.execute();
    }
    
    private void parseDownloadPage(String body) {
        enable(false);
        String count = "empty";
        int first = 1;
        int last = settings.maxPage();
        try {
            if (!PAGE_EMPTY.matcher(body).find()) {
                Matcher matcher = PAGE_RANGE.matcher(body);
                if (!matcher.find() || matcher.group(1) == null) {
                    handleException("parsing download page", new IOException("unrecognized page format"));
                } else {
                    first = Integer.parseInt(matcher.group(1));
                    info("first page: %d%n", first);
                    if (matcher.group(2) == null) {
                        last = first;
                    } else {
                        last = Integer.parseInt(matcher.group(2));
                    }
                    info("last page: %d%n", last);
                    count = Integer.toString(last-first+1);
                    enable(true);
                }
            }
            pageCount.setText(count);
            startModel.setValue(first);
            startModel.setMinimum(first);
            startModel.setMaximum(last);
            endModel.setMinimum(first);
            endModel.setMaximum(last);
            endModel.setValue(last);
            eraseButton.setEnabled(true);
        } finally {
            refreshButton.setEnabled(true);
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
