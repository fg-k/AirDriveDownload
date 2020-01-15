package cfh.airdrive.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            String title = ex.getClass().getSimpleName();
            String[] message = { "Unable to start server", ex.getMessage() };
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    
    private JFrame frame;
    private JScrollPane scroll;
    private JTextArea output;
    
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
                        builder.append(new String(buffer, 0, count));
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
        startServer();
    }
    
    private void initGUI() {
        output = new JTextArea(20, 80);
        output.setEditable(false);
        output.setFont(new Font("monospaced", Font.PLAIN, 12));
        
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
        frame.setVisible(true);
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
                            JOptionPane.showMessageDialog(frame, "Server closed", "THE END", JOptionPane.INFORMATION_MESSAGE);
                            frame.dispose();
                        };
                    }.execute();
                }
            }
        });
    }
    
    private void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(8000);
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
                sendReply(exchange, 200, Page.ROOT.text());
            } else {
                printf("sending 404 NOT_FOUND%n");
                sendReply(exchange, 404, Page.NOT_FOUND.text());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            printf("%s sending reply: %s%n", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
    
    private void printHandle(String prefix, HttpExchange exchange) {
        printf("%s: %s %s (%s)%n", prefix, exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRemoteAddress());
    }
    
    private void sendReply(HttpExchange exchange, int code, String reply) throws IOException {
        byte[] message = reply.getBytes();
        try (OutputStream stream = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(code, message.length);
            stream.write(message);
        }
    }
}
