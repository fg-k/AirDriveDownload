package cfh.airdrive.http8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import cfh.airdrive.http.HttpService;


public class Http8Service implements HttpService {

    @Override
    public byte[] read(URL url) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try (InputStream input = url.openStream()) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                result.write(buffer, 0, count);
            }
        }
        return result.toByteArray();
    }

}
