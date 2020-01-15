package cfh.airdrive.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class GenerateTest {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("no file given");
        } else {
            try (Writer writer = new FileWriter(args[0])) {
                for (int lines = 0; lines < 6 * 2_048 / 120; lines++) {
                    for (int i = 0; i < 120; i++) {
                        switch (i) {
                            case 0:   writer.write('['); break;
                            default:  writer.write(Character.forDigit(lines % 10, 10)); break;
                            case 118: writer.write(']'); break;
                            case 119: writer.write('\n'); break;
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.printf("\"%s\" created%n", args[0]);
        }
    }
}
