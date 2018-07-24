package deplambda.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.others.SentenceKeys;

/**
 * Servlet implementation class QueryUDP
 */
@WebServlet("/QueryUDP")
public class QueryUDP extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private String charset = "UTF-8";
  private static JsonParser jsonParser = new JsonParser();
  private static Gson gson = new Gson();
  private Map<String, String> udpEndPoints = new HashMap<>();

  /**
   * @see Servlet#init(ServletConfig)
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    Enumeration<String> enm = config.getInitParameterNames();
    while (enm.hasMoreElements()) {
      String name = enm.nextElement();
      String value = config.getInitParameter(name);
      if (name.endsWith("UdpEndPoint")) {
        udpEndPoints.put(name, value);
      }
    }
  }

  /**
   * @see HttpServlet#HttpServlet()
   */
  public QueryUDP() {
    super();
    System.setProperty("java.awt.headless", "true");
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head>");
    out.println("<title>DepLambda Demo</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<p>Sorry, This demo has been discontinued.</p>");
    out.println("<p>See <a href=\"https://github.com/sivareddyg/UDepLambda/issues/18\">issue</a></p>");
    out.println("</body>");
    out.println("</html>");
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }
}
