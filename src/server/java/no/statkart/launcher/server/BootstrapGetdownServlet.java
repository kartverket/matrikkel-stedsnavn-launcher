package no.statkart.launcher.server;

import com.threerings.getdown.tools.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Servlet ansvarlig for å sette URLen i den utvidede launcher-webappkonfigurasjonen,
 * og deretter lage digests (sjekksummer) av alle artifaktene.
 * <p/>
 * Dette gjøres bare ved initial load av servleten.
 */
public class BootstrapGetdownServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapGetdownServlet.class);

    @Override
    public final void init() throws ServletException {
        try {
            Path mappe = finnMappe();
            patchGetDownTxt(mappe, patchAppBase());
            opprettDigests(mappe);
            LOG.info("Patchet getdown.txt appbase og opprettet digests i " + mappe);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Path finnMappe() {
        String mappe = getServletContext().getRealPath("/vault");
        return Paths.get(mappe);
    }

    private static void patchGetDownTxt(Path mappe, Function<String, String> function) throws IOException {
        Path source = mappe.resolve("getdown.txt");
        Path temp = mappe.resolve("___getdown___.txt");
        Files.move(source, temp);
        streamCopy(Files.newInputStream(temp), Files.newOutputStream(source), function);
        Files.delete(temp);
    }

    private Function<String, String> patchAppBase() {
        return (String s) -> s.replace("@@url@@", getURL());
    }

    private static void streamCopy(InputStream input, OutputStream output, Function<String, String> function) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = function.apply(line);
                bw.write(line);
                bw.newLine();
            }
        }
    }

    private void opprettDigests(Path mappe) throws Exception {
        Digester.createDigests(mappe.toFile(), null, null, null);
    }

    private String getURL() {
        String classname = getInitParameter("urlSupplierClass");
        Object o1;
        try {
            o1 = Class.forName(classname).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create instace of " + classname, e);
        }
        if (!(o1 instanceof Supplier)) {
            throw new IllegalStateException(classname + " must be instance of Supplier");
        }
        Supplier<?> supplier = (Supplier<?>) o1;
        Object o2 = supplier.get();
        if (!(o2 instanceof String)) {
            throw new IllegalStateException(classname + " must supply String");
        }
        return (String) o2;
    }

}
