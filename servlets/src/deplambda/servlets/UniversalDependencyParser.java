package deplambda.servlets;

import java.io.IOException;
import java.io.PrintWriter;
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

import deplambda.others.NlpPipeline;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet implementation class UniversalDependencyParser
 */
@WebServlet("/UniversalDependencyParser")
public class UniversalDependencyParser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private NlpPipeline pipeline = null;
  private Gson gson = null;
  private JsonParser jsonParser = new JsonParser();

  /**
   * @see HttpServlet#HttpServlet()
   */
  public UniversalDependencyParser() {
    super();
  }

  /**
   * @see Servlet#init(ServletConfig)
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    Enumeration<String> enm = config.getInitParameterNames();
    Map<String, String> options = new HashMap<>();
    while (enm.hasMoreElements()) {
      String name = enm.nextElement();
      String value = config.getInitParameter(name);
      if (value.startsWith("/WEB-INF")) {
        value = config.getServletContext().getRealPath(value);
      }
      options.put(name, value);
    }
    pipeline = new NlpPipeline(options);
    gson = new Gson();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    String queryText = request.getParameter("query");
    if (queryText == null) {
      queryText = "{\"sentence\" : \"Give me an input sentence.\"}";
    }
    JsonObject sentObj = jsonParser.parse(queryText).getAsJsonObject();
    pipeline.processSentence(sentObj);
    out.print(gson.toJson(sentObj));
  }
}
