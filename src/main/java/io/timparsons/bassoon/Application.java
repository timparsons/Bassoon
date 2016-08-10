package io.timparsons.bassoon;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogManager;

import javax.faces.webapp.FacesServlet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.faces.config.ConfigureListener;

import io.dropwizard.util.Generics;

public abstract class Application<T extends Configuration> {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	public static final String SERVER_REFERENCE = "jettyInMemory";

	private Server server;

	public void run(String... args) {
		run(null, args);
	}

	public <J extends GuiceInjectionProvider> void run(Class<J> injectionProvider, String... args) {
		T config;
		try {
			config = getConfig(args);
		} catch (IOException e) {
			log.error("Could not parse configuration file", e);
			throw new RuntimeException(e);
		}

		WebAppContext context = initJetty(config);

		initJSF(context, config, injectionProvider);

		initSecurity(context);

		try {
			server.start();
			log.info("Startup completed");
		} catch (Exception e) {
			log.error("Could not start the server", e);
			throw new RuntimeException(e);
		}

		run(config);

		listen();
	}

	protected abstract void run(T config);

	private void listen() {
		try {
			server.join();
		} catch (InterruptedException e) {
			log.error("Runtime exception in server", e);
		}
	}

	private void initSecurity(WebAppContext context) {
		ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();

		Constraint xhtmlConstraint = new Constraint();
		xhtmlConstraint.setName("JSF Source Code Security Constraint");
		xhtmlConstraint.setAuthenticate(true);
		xhtmlConstraint.setRoles(null);

		ConstraintMapping xhtmlConstraintMapping = new ConstraintMapping();
		xhtmlConstraintMapping.setPathSpec("*.xhtml");
		xhtmlConstraintMapping.setConstraint(xhtmlConstraint);
		securityHandler.setConstraintMappings(Collections.singletonList(xhtmlConstraintMapping));

		securityHandler.setHandler(context);

		server.setHandler(securityHandler);
	}

	private <J extends GuiceInjectionProvider> void initJSF(WebAppContext context, T config,
			Class<J> injectionProvider) {
		log.info("initializing JSF");

		context.setInitParameter("com.sun.faces.forceLoadConfiguration", "true");
		context.setInitParameter("com.sun.faces.enableRestoreView11Compatibility", "true");

		context.setInitParameter("javax.faces.PROJECT_STAGE", config.getProjectStage());
		context.setInitParameter("javax.faces.FACELETS_SKIP_COMMENTS", "true");
		context.setInitParameter("javax.faces.STATE_SAVING_METHOD", "server");
		context.setInitParameter("javax.faces.DEFAULT_SUFFIX", ".xhtml");

		context.setInitParameter("defaultHtmlEscape", "true");

		if (injectionProvider != null) {
			GuiceInjectionProvider.setConfiguration(config);
			context.setInitParameter("com.sun.faces.injectionProvider", injectionProvider.getName());
		}

		if (config.getInitParams() != null) {
			for (Entry<String, String> initParam : config.getInitParams().entrySet()) {
				context.setInitParameter(initParam.getKey(), initParam.getValue());
			}
		}

		context.addEventListener(new ConfigureListener());

		ServletHolder jsfServlet = new ServletHolder(FacesServlet.class);
		jsfServlet.setDisplayName("Faces Servlet");
		jsfServlet.setName("Faces_Servlet");
		jsfServlet.setInitOrder(0);

		context.addServlet(jsfServlet, "*.jsf");
		context.setWelcomeFiles(new String[] { "index.jsf" });
	}

	private WebAppContext initJetty(T config) {
		LogManager.getLogManager().reset();
		SLF4JBridgeHandler.install();

		log.info("Starting Jetty");

		QueuedThreadPool connectionThreadPool = new QueuedThreadPool();
		connectionThreadPool.setName("JETTY_CONNECTIONS");
		connectionThreadPool.setMinThreads(20);
		connectionThreadPool.setMaxThreads(500);
		connectionThreadPool.setStopTimeout(30 * 1000);

		server = new Server(connectionThreadPool);

		server.addBean(new ScheduledExecutorScheduler());
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		ServerConnector httpConnector = new ServerConnector(server);
		if (config.getHost() != null) {
			httpConnector.setHost(config.getHost());
		}

		httpConnector.setPort(config.getPort());
		httpConnector.setIdleTimeout(config.getIdleTimeout());
		server.addConnector(httpConnector);
		log.info("HTTP Connector started on port: " + config.getPort());

		String webappDir = this.getClass().getClassLoader().getResource("webapp").toExternalForm();
		WebAppContext context = new WebAppContext(webappDir, config.getPath()) {
			@Override
			public String getResourceAlias(String alias) {
				final Map<String, String> resourceAliases = getResourceAliases();

				if (resourceAliases == null) {
					return null;
				} else {
					for (Entry<String, String> aliasEntry : resourceAliases.entrySet()) {
						if (alias.startsWith(aliasEntry.getKey())) {
							return alias.replace(aliasEntry.getKey(), aliasEntry.getValue());
						}
					}

					return null;
				}
			}
		};

		try {
			context.setBaseResource(new ResourceCollection(new String[] { webappDir, "./target" }));
			context.setResourceAlias("/WEB-INF/classes/", "/classes/");
		} catch (Exception e) {
			log.info("error setting base resource/alias");
		}

		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

		context.setAttribute(SERVER_REFERENCE, this);

		log.info("Serving app from: " + webappDir);

		return context;
	}

	private T getConfig(String[] args) throws JsonParseException, JsonMappingException, IOException {
		if (args.length == 0) {
			throw new IllegalArgumentException("Configuration file not specified");
		}
		File configFile = new File(args[0]);

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		return mapper.readValue(configFile, Generics.getTypeParameter(getClass(), Configuration.class));
	}
}
