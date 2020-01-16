package cfh.airdrive.server;

import static javax.swing.JOptionPane.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import static java.nio.charset.StandardCharsets.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;


@SuppressWarnings("restriction")
public class Server {

    public static void main(String[] args) {
        try {
            new Server();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            String title = ex.getTargetException().getClass().getSimpleName();
            String[] message = { "Unable to create server", ex.getTargetException().getMessage() };
            showMessageDialog(null, message, title, ERROR_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            String title = ex.getClass().getSimpleName();
            String[] message = { "Unable to start server", ex.getMessage() };
            showMessageDialog(null, message, title, ERROR_MESSAGE);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    
    private final Settings settings = Settings.instance();
    
    private final JFrame frame = new JFrame();
    private final JScrollPane scroll = new JScrollPane();
    private final JTextArea output = new JTextArea();
    
    private final List<String> pages = new ArrayList<>();
    
    private HttpServer server;
    
    private final List<String> roots = Collections.unmodifiableList(
        Arrays.asList("/", "/index.html", "/index.htm"));
    
    
    private enum Page {
        BAD_REQUEST("400.xml"),
        BAD_NUMBER("400_2.xml"),
        NOT_FOUND("404.xml"),
        INVALID_METHOD("405.xml"),
        INTERNAL_ERROR("500.xml"),
        ROOT("root.xml"),
        DOWNLOAD("download.xml");
        
        private static final Map<Page, String> cache = new HashMap<>();
        
        private final String file;
        
        private Page(String file) {
            this.file = file;
        }
        
        String format(Object... args) throws IOException {
            String page = cache.get(this);
            if (page == null) {
                try (InputStream stream = Server.class.getResourceAsStream(file)) {
                    if (stream == null) {
                        throw new FileNotFoundException(file);
                    }
                    StringBuilder builder = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        builder.append(new String(buffer, 0, count, UTF_8));
                    }
                    page = builder.toString();
                    cache.put(this, page);
                }
            }
            return String.format(page, args);
        }
    }
    
    
    private Server() throws InvocationTargetException, InterruptedException, IOException {
        SwingUtilities.invokeAndWait(this::initGUI);
        try {
            readData();
            startServer();
        } catch (Exception ex) {
            frame.dispose();
            throw ex;
        }
    }
    
    private void initGUI() {
        output.setRows(settings.rows());
        output.setColumns(settings.columns());
        output.setEditable(false);
        output.setFont(new Font(settings.fontName(), Font.PLAIN, settings.fontSize()));
        
        scroll.setViewportView(output);
        scroll.setHorizontalScrollBarPolicy(scroll.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(scroll.VERTICAL_SCROLLBAR_ALWAYS);
        
        frame.setDefaultCloseOperation(frame.DO_NOTHING_ON_CLOSE);
        frame.setTitle(settings.title());
        frame.setLayout(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            printf("Closing server at %s%n", server.getAddress());
                            server.stop(1);
                            return null;
                        }
                        @Override
                        protected void done() {
                            showMessageDialog(frame, "Server terminated, close window?", "THE END", INFORMATION_MESSAGE);
                            frame.dispose();
                        };
                    }.execute();
                }
            }
        });
        frame.setVisible(true);
    }
    
    private void readData() throws IOException {
        String file = settings.dataFileName();
        pages.clear();
        try (InputStream input = Server.class.getResourceAsStream(file)) {
            if (input == null) {
                throw new FileNotFoundException(file);
            }
            byte buffer[] = new byte[settings.pageSize()];
            int offset = 0;
            int count;
            while ((count = input.read(buffer, offset, buffer.length-offset)) > 0) {
                offset += count;
                if (offset == buffer.length) {
                    pages.add(new String(buffer, UTF_8));
                    offset = 0;
                }
            }
            if (offset > 0) {
                pages.add(new String(buffer, 0, offset, UTF_8));
            }
        }
        printf("Data %d pages from %s%n", pages.size(), file);
    }
    
    private void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(settings.port());
        server = HttpServer.create(address, 0);
        server.createContext("/", this::handleRoot);
        server.start();
        printf("Server started listening at %s%n", server.getAddress());
    }
    
    private void handleRoot(HttpExchange exchange) {
        printHandle("Root", exchange);
        URI uri = exchange.getRequestURI();
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendReply(exchange, 405, Page.INVALID_METHOD, exchange.getRequestMethod(), uri);
            } else if (roots.contains(uri.getPath().toLowerCase()) &&
                    uri.getQuery() == null) {
                printf("sending 200 ROOT%n");
                String data = pages.get(pages.size()-1);
                if (data.length() > settings.preview()) {
                    data = data.substring(0, settings.preview()) + "...";
                }
                data = data.replace("\n", "<br>");
                sendReply(exchange, 200, Page.ROOT, data, pages.size());
            } else if (uri.getPath().equalsIgnoreCase("/download.html")) {
                handleDownload(exchange);
            } else {
                sendReply(exchange, 404, Page.NOT_FOUND, uri);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            printf("%s sending reply: %s%n", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
    
    private void handleDownload(HttpExchange exchange) {
        printHandle("Download", exchange);
        URI uri = exchange.getRequestURI();
        try {
            if (uri.getQuery() == null) {
                sendReply(exchange, 200, Page.DOWNLOAD, 1, pages.size(), Math.min(pages.size(), settings.maxPages()));
            } else {
                Matcher matcher = Pattern.compile(settings.downloadActionPattern()).matcher(uri.getQuery());
                if (matcher.matches()) {
                    int start;
                    int count;
                    try {
                        start = Integer.parseInt(matcher.group(1));
                        count = Integer.parseInt(matcher.group(2));
                        printf("start: %d, count: %d%n", start, count);
                        if (start < 1) {
                            throw new NumberFormatException("spage < 1");
                        }
                        if (start > pages.size()) {
                            throw new NumberFormatException("spage > " + pages.size());
                        }
                        if (count < 1) {
                            throw new NumberFormatException("npage < 1");
                        }
                        if (start + count - 1 > pages.size()) {
                            throw new NumberFormatException("spage+npage > " + pages.size());
                        }
                    } catch (NumberFormatException ex) {
                        sendReply(exchange, 400, Page.BAD_NUMBER, uri, ex.getMessage());
                        return;
                    }
                    sendPages(exchange, start, count);
                } else {
                    sendReply(exchange, 400, Page.BAD_REQUEST, uri);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            printf("%s sending reply: %s%n", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
    
    private void sendPages(HttpExchange exchange, int start, int count) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < start+count; i++) {
            builder.append(pages.get(i-1));
        }
        Headers headers = exchange.getResponseHeaders();
        headers.set("Cache-Control", "store, no-cache, must-revalidate, post-check=0, pre-check=0");
        headers.set("Content-Disposition", "attachment; filename=LOG.TXT");
        headers.set("Content-Transfer-Encoding", "binary");
        headers.set("Content-Type", "application/force-download");
        headers.set("Expires", "0");
        headers.set("Pragma", "public, no-cache");
        sendReply(exchange, 200, builder.toString());
    }
    
    private void sendReply(HttpExchange exchange, int code, Page page, Object... args) throws IOException {
        printf("sending reply %d %s%n", code, page.name());
        try {
            String reply = page.format(args);
            sendReply(exchange, code, reply);
        } catch (Exception ex) {
            sendReply(exchange, 500, Page.INTERNAL_ERROR.format(ex.getClass(), ex.getMessage()));
            throw ex;
        }
    }
    
    private void sendReply(HttpExchange exchange, int code, String reply) throws IOException {
        byte[] data = reply.getBytes(UTF_8);
        System.out.printf("data: %d%n%s%n%s%n", data.length, reply, Arrays.toString(data));
        System.out.flush();
        try (OutputStream stream = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(code, data.length);
            stream.write(data);
        }
    }
    
    private void printHandle(String prefix, HttpExchange exchange) {
        printf("%s: %s %s (%s)%n", prefix, exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRemoteAddress());
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
