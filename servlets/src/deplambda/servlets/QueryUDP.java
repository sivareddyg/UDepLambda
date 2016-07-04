package deplambda.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import deplambda.others.SentenceKeys;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet implementation class QueryUDP
 */
@WebServlet("/QueryUDP")
public class QueryUDP extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private String charset = "UTF-8";
  private JsonParser jsonParser = new JsonParser();
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
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head>");
    out.println("<title>DepLambda Demo</title>");
    out.println(
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
            + "    <link rel=\"stylesheet\" href=\"http://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css\">\n"
            + "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>\n"
            + "    <script src=\"http://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js\"></script>");
    out.println("</head>");
    out.println("<body style=\"padding-left:10; padding-right:10\">");
    out.println("<font color=#006400 size=6>DepLambda Demo</font>");
    String languageCode = request.getParameter("language");
    if (languageCode == null)
      languageCode = "en";
    String udpEndPoint = udpEndPoints.get(languageCode + "UdpEndPoint");

    String queryText = request.getParameter("query");
    if (queryText == null || queryText.trim().equals("")) {
      if (languageCode.equals("es")) {
        queryText =
            "Washington D.C. es la capital de los Estados Unidos de América.";
      } else if (languageCode.equals("de")) {
        queryText = "Pixar, die Firma die Disney kaufte, machte Ratatouille.";
      } else {
        queryText =
            "Pixar, the company which Disney acquired, made Ratatouille.";
      }
    }

    out.println("<P>");
    out.print("<form action=QueryUDP method=POST id=mainForm>");
    out.println("<font size=\"3\">");
    out.println("Please enter the input text to be parsed. <br>");
    out.println(
        "<textarea name=query style=\"width: 500px; height: 8em\" rows=31 cols=7>");
    out.print(queryText);
    out.println("</textarea>");
    out.println("<br>");
    out.println("<br>");
    out.println("<select name=language form=mainForm>");

    if (languageCode.equals("en")) {
      out.println("<option value=en selected=selected>English</option>");
    } else {
      out.println("<option value=en>English</option>");
    }

    if (languageCode.equals("es")) {
      out.println("<option value=es  selected=selected>Spanish</option>");
    } else {
      out.println("<option value=es>Spanish</option>");
    }

    if (languageCode.equals("de")) {
      out.println("<option value=de selected=selected>German</option>");
    } else {
      out.println("<option value=de>German</option>");
    }

    out.println("</select>");
    out.println("<input type=submit>");
    out.println("</font>");
    out.println("</form>");

    String queryTextJson = String.format("{\"sentence\": \"%s\"}", queryText);
    String requestQuery =
        String.format("query=%s", URLEncoder.encode(queryTextJson, charset));

    URLConnection connection =
        new URL(udpEndPoint + "?" + requestQuery).openConnection();
    connection.setRequestProperty("Accept-Charset", charset);
    InputStream responseRecieved = connection.getInputStream();
    String result = IOUtils.toString(responseRecieved, charset);
    JsonObject jsonSentence = jsonParser.parse(result).getAsJsonObject();

    if (jsonSentence.has(SentenceKeys.SVG_TREES)) {
      out.print("<h3><font color=#006400>Dependency Parses:</font></h3>");
      out.println(jsonSentence.get(SentenceKeys.SVG_TREES).getAsString());
      jsonSentence.remove(SentenceKeys.SVG_TREES);
    }

    if (jsonSentence.has(SentenceKeys.DEPENDENCY_LAMBDA)) {
      out.print("<h3><font color=#006400>Simplified Logical Form:</font></h3>");
      out.println("<font size=\"4\">");
      try {
        out.println(StringEscapeUtils.escapeHtml4(
            jsonSentence.get(SentenceKeys.DEPENDENCY_LAMBDA).toString()));
      } catch (Exception e) {
        //pass.
      }
      out.println("</font>");
      
      out.print("<h3><font color=#006400>Lambda Expression:</font></h3>");
      out.println("<font size=\"3\">");
      out.println(StringEscapeUtils.escapeHtml4(
          jsonSentence.get(SentenceKeys.DEPLAMBDA_EXPRESSION).getAsString()));
      out.println("</font>");

      out.print("<h3><font color=#006400>Composition Order:</font></h3>");
      out.println("<font size=\"3\">");
      out.println(StringEscapeUtils.escapeHtml4(
          jsonSentence.get(SentenceKeys.DEPLAMBDA_OBLIQUE_TREE).getAsString()));
      out.println("</font>");
    }

    out.println("<h3><font color=#006400>Json Format:</font></h3>");
    out.println("<pre>");
    out.println(StringEscapeUtils.escapeHtml4(jsonSentence.toString()));
    out.println("</pre>");

    out.println("</body>");
    out.println("</html>");
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
