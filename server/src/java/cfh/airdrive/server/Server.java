package cfh.airdrive.server;

import static javax.swing.JOptionPane.*;

import com.sun.net.httpserver.HttpServer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private JFrame frame;
    private JScrollPane scroll;
    private JTextArea output;
    
    private List<String> pages;
    
    private HttpServer server;
    
    private static final Map<Page, String> cache = new HashMap<>();
    private enum Page {
        NOT_FOUND("404.xml"),
        ROOT("root.xml");
        
        private final String file;
        
        private Page(String file) {
            this.file = file;
        }
        
        String text() throws IOException {
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
            return page;
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
        output = new JTextArea(settings.lines(), settings.columns());
        output.setEditable(false);
        output.setFont(new Font(settings.fontName(), Font.PLAIN, settings.fontSize()));
        
        scroll = new JScrollPane(output);
        scroll.setHorizontalScrollBarPolicy(scroll.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(scroll.VERTICAL_SCROLLBAR_ALWAYS);
        
        frame = new JFrame();
        frame.setDefaultCloseOperation(frame.DO_NOTHING_ON_CLOSE);
        frame.setTitle("Test Server");
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
        List<String> tmp = new ArrayList<>();
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
                    tmp.add(new String(buffer, UTF_8));
                    offset = 0;
                }
            }
            if (offset > 0) {
                tmp.add(new String(buffer, 0, offset, UTF_8));
            }
        }
        printf("Data %d pages from %s%n", tmp.size(), file);
        pages = tmp;
    }
    
    private void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(settings.port());
        server = HttpServer.create(address, 0);
        server.createContext("/", this::handleRoot);
        server.start();
        printf("Server started listening at %s%n", server.getAddress());
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
    
    private void handleRoot(HttpExchange exchange) {
        printHandle("Root", exchange);
        URI uri = exchange.getRequestURI();
        try {
            if (uri.getPath().equals("/") &&
                uri.getQuery() == null &&
                uri.getFragment() == null) {
                printf("sending 200 ROOT%n");
                String data = pages.get(pages.size()-1);
                if (data.length() > settings.preview()) {
                    data = data.substring(0, settings.preview()) + "...";
                }
                data = data.replace("\n", "<br>");
                String reply = String.format(Page.ROOT.text(), data, pages.size());
                sendReply(exchange, 200, reply);
            } else {
                printf("sending 404 NOT_FOUND%n");
                sendReply(exchange, 404, Page.NOT_FOUND.text());
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            printf("%s sending reply: %s%n", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
    
    private void printHandle(String prefix, HttpExchange exchange) {
        printf("%s: %s %s (%s)%n", prefix, exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRemoteAddress());
    }
    
    private void sendReply(HttpExchange exchange, int code, String reply) throws IOException {
        byte[] data = reply.getBytes(UTF_8);
        try (OutputStream stream = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(code, data.length);
            stream.write(data);
        }
    }
}
