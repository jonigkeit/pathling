/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

package au.csiro.clinsight;

import static au.csiro.clinsight.utilities.Configuration.setStringPropsUsingEnvVar;

import au.csiro.clinsight.fhir.AnalyticsServer;
import au.csiro.clinsight.fhir.AnalyticsServerConfiguration;
import java.util.HashMap;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

/**
 * Creates an embedded Jetty server and mounts the servlets that are required to deliver the
 * functionality of the FHIR analytics server. Also knows how to configure the servlets using a set
 * of environment variables.
 *
 * @author John Grimes
 */
public class FhirServerContainer {

  public static void main(String[] args) throws Exception {
    AnalyticsServerConfiguration config = new AnalyticsServerConfiguration();

    setStringPropsUsingEnvVar(config, new HashMap<String, String>() {{
      put("CLINSIGHT_SPARK_MASTER_URL", "sparkMasterUrl");
      put("CLINSIGHT_WAREHOUSE_DIRECTORY", "warehouseDirectory");
      put("CLINSIGHT_METASTORE_URL", "metastoreUrl");
      put("CLINSIGHT_METASTORE_USER", "metastoreUser");
      put("CLINSIGHT_METASTORE_PASSWORD", "metastorePassword");
      put("CLINSIGHT_DATABASE_NAME", "databaseName");
      put("CLINSIGHT_EXECUTOR_MEMORY", "executorMemory");
      put("CLINSIGHT_TERMINOLOGY_SERVER_URL", "terminologyServerUrl");
    }});

    // This is required to force the use of the Woodstox StAX implementation. If you don't use
    // Woodstox, parsing falls over when reading in resources (e.g. StructureDefinitions) that
    // contain HTML entities.
    //
    // See: http://hapifhir.io/download.html#_toc_stax__woodstox
    System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
    System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
    System.setProperty("javax.xml.stream.XMLEventFactory", "com.ctc.wstx.stax.WstxEventFactory");

    start(config);
  }

  private static void start(AnalyticsServerConfiguration configuration) throws Exception {
    final int maxThreads = 100;
    final int minThreads = 10;
    final int idleTimeout = 120;

    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

    Server server = new Server(threadPool);
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(8090);
    server.setConnectors(new Connector[]{connector});

    ServletHandler servletHandler = new ServletHandler();

    // Add analytics server to handle all other requests.
    ServletHolder analyticsServletHolder = new ServletHolder(new AnalyticsServer(configuration));
    servletHandler.addServletWithMapping(analyticsServletHolder, "/fhir/*");

    // Add proxy servlet to forward terminology and profile-related requests to the configured
    // terminology server.
    ProxyServlet terminologyProxyServlet = new ProxyServlet();
    ServletHolder terminologyProxyHolder = new ServletHolder(terminologyProxyServlet);
    terminologyProxyHolder.setInitParameter("targetUri",
        configuration.getTerminologyServerUrl() + "/StructureDefinition");
    terminologyProxyHolder.setInitParameter("log", "true");
    servletHandler.addServletWithMapping(terminologyProxyHolder, "/fhir/StructureDefinition/*");

    server.setHandler(servletHandler);

    server.start();
  }

}
