package cfh.airdrive.server;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


@SuppressWarnings("restriction")
public class RootHandler implements HttpHandler {

    private final Server.Output output;
    
    public RootHandler(Server.Output output) {
        this.output = output;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        output.printf("Root: %s %s%n", exchange.getRequestMethod(), exchange.getRequestURI());
        // TODO
        byte[] response = "404, not implemented".getBytes();
        exchange.sendResponseHeaders(404, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}
