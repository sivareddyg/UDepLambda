package deplambda.servlets;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.others.NlpPipeline;

/**
 * Servlet implementation class UniversalDependencyParser
 */
@WebServlet("/UniversalDependencyParser")
public class UniversalDependencyParser extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private List<NlpPipeline> pipelines = new ArrayList<>();
  private Gson gson = null;
  private JsonParser jsonParser = new JsonParser();

  /**
   * @see HttpServlet#HttpServlet()
   */
  public UniversalDependencyParser() {
    super();
    System.setProperty("java.awt.headless", "true");
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
    int pipelineCount =
        Integer.parseInt(options.getOrDefault("pipelineCount", "0"));
    
    if (pipelineCount > 0) {
      // Multiple pipelines.
      for (int i = 0; i < pipelineCount; i++) {
        Map<String, String> currentOptions = new HashMap<>();
        for (Entry<String, String> entrySet : options.entrySet()) {
          if (entrySet.getKey().startsWith(String.format("%d-", i))) {
            // Pipeline key starts with pipeline index
            currentOptions.put(
                entrySet.getKey().replaceFirst(String.format("%d-", i), ""),
                entrySet.getValue());
          }
        }
        try {
          pipelines.add(new NlpPipeline(currentOptions));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } else {
      try {
        pipelines.add(new NlpPipeline(options));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    gson = new Gson();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");
    DataOutputStream responseToClient = new DataOutputStream(response.getOutputStream());

    String queryText = request.getParameter("query");
    if (queryText == null) {
      queryText = "{\"sentence\" : \"Give me an input sentence.\"}";
    }
    JsonObject sentObj = jsonParser.parse(queryText).getAsJsonObject();
    for (NlpPipeline pipeline : pipelines) {
      pipeline.processSentence(sentObj);
    }
    String jsonString = gson.toJson(sentObj);
    byte[] utf8JsonString = jsonString.getBytes("UTF8");
    responseToClient.write(utf8JsonString, 0, utf8JsonString.length);
  }
}
