package alma.cli.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class AsciiBanner {

    private AsciiBanner() {
    }

    public static String load(String name) {
        String resource = "banners/" + name + ".txt";
        try (InputStream stream = AsciiBanner.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (stream == null) {
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            return "";
        }
    }

    public static void print(PrintStream out, String name) {
        out.println(load(name));
    }
}
