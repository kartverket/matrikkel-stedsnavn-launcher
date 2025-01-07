package no.statkart.launcher.server;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateServlet extends HttpServlet {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(-SNAPSHOT|\\.\\d+)?$");
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletConfig servletConfig = getServletConfig();
        String klientVersjon = request.getParameter("version");
        String eldsteVersjonTillatt = servletConfig.getInitParameter("oldestAllowedClientVersion");
        if (eldsteVersjonTillatt == null || klientVersjon == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else if (godkjentVersjon(eldsteVersjonTillatt, klientVersjon)) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

    private static boolean godkjentVersjon(String required, String provided) {
        if (provided.equals(required)) {
            // Identiske versjoner er alltid godkjent
            return true;
        }
        Matcher mProvided = VERSION.matcher(provided);
        Matcher mRequired = VERSION.matcher(required);
        if (!mProvided.matches() || !mRequired.matches()) {
            // Versjonene er ulike og følger ikke det oppsatte versjoneringsmønsteret
            return false;
        }
        int providedMajor = Integer.parseInt(mProvided.group(1));
        int providedMinor = Integer.parseInt(mProvided.group(2));
        int requiredMajor = Integer.parseInt(mRequired.group(1));
        int requiredMinor = Integer.parseInt(mRequired.group(2));
        if (providedMajor != requiredMajor) {
            // Ulik hovedversjon aksepteres ikke
            return false;
        }
        // Nyere minorversjon aksepteres
        return providedMinor >= requiredMinor;
    }

}
