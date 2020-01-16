package cfh.airdrive.http;

import java.io.IOException;
import java.net.URL;


public interface HttpService {

    public byte[] read(URL url) throws IOException;
}
