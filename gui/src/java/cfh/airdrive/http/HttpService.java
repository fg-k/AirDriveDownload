package cfh.airdrive.http;

import java.io.IOException;
import java.net.URL;


public interface HttpService {

    /** GET page data from given URL. */
    public byte[] read(URL url) throws IOException;
}
