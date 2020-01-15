package cfh.airdrive.server;

import com.sun.net.httpserver.HttpServer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

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
    private boolean stopped = false;
    
    
    private Server() throws InvocationTargetException, InterruptedException, IOException {
        SwingUtilities.invokeAndWait(this::initGUI);
        startServer();
    }
    
    private void initGUI() {
        output = new JTextArea(20, 60);
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
                if (stopped) {
                    frame.dispose();
                } else if (server != null) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            printf("Closing server at %s%n", server.getAddress());
                            server.stop(1);
                            return null;
                        }
                        @Override
                        protected void done() {
                            printf("Server closed - Close Window again%n%n");
                            stopped = true;
                        };
                    }.execute();
                }
            }
        });
    }
    
    private void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(8000);
        server = HttpServer.create(address, 0);
        server.createContext("/", new RootHandler(this::printf));
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
    
    
    //------------------------------------------------------------------------------------------------------------------
    
    interface Output {
        void printf(String format, Object... args);
    }
}
