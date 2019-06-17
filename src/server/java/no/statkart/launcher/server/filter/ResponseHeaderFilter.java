package no.statkart.launcher.server.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Filter som setter ekstra HTTP-headere, spesifisert i web.xml.
 */
public class ResponseHeaderFilter implements Filter {

   private FilterConfig filterConfig;

   @Override
   public void init(FilterConfig filterConfig) {
      this.filterConfig = filterConfig;
   }

   @Override
   public void destroy() {
      this.filterConfig = null;
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      Enumeration<String> parameters = filterConfig.getInitParameterNames();
      while (parameters.hasMoreElements()) {
         String parameter = parameters.nextElement();
         String value = filterConfig.getInitParameter(parameter);
         httpResponse.setHeader(parameter, value);
      }
      chain.doFilter(request, response);
   }

}
