package spinat.jettyprism;

//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

    private static void msg(String s) {
        System.out.println("### " + s);
    }

    public static void main(String[] args) throws Exception {
        // it would be better nicer use ROOT, Oracle JDBC does not like that
        Locale.setDefault(Locale.US);

        if (args.length < 1) {
            throw new RuntimeException("Expecting one argument, the name of the configuration file");
        }
        String propFileName = args[0];
        java.util.Properties props = new java.util.Properties();
        File apf = new File(propFileName).getAbsoluteFile();
        if (!apf.canRead()) {
            throw new RuntimeException("can not read file: " + apf);
        }
        msg("using property file: " + apf);
        File dpf = apf.getParentFile();
        msg("setting user.dir to " + dpf.toString());
        System.setProperty("user.dir", dpf.toString());

        FileReader fr = new FileReader(apf);
        props.load(fr);

        String log4jprops = props.getProperty("log4jconfig", "");
        if (log4jprops.equals("")) {
            log4jprops = "log4j.properties";
        }
        File l4jf = new File(log4jprops).getAbsoluteFile();
        URL l4ju = l4jf.toURI().toURL();
        msg("log4j config file " + l4jf);
        PropertyConfigurator.configure(l4ju);

        String port = props.getProperty("port", "");
        int intPort;
        if (port.equals("")) {
            String portEnv = System.getenv().get("JETTYPRISM_PORT").trim();
            if (portEnv == null || portEnv.isEmpty()) {
                intPort = 8080;
            } else {
                intPort = Integer.parseInt(portEnv);
            }
        } else {
            intPort = Integer.parseInt(port);
        }
        msg("using port " + intPort);

        final Server server;
        String only_local = props.getProperty("only_local", "1");
        if (only_local.equalsIgnoreCase("N") || only_local.equalsIgnoreCase("NO")
                || only_local.equalsIgnoreCase("F") || only_local.equalsIgnoreCase("FALSE")
                || only_local.equalsIgnoreCase("0")) {
            msg("using all addressses");
            server = new Server(intPort);
        } else {

            InetAddress iadr = InetAddress.getLoopbackAddress();
            msg("using address " + iadr);
            InetSocketAddress siadr = new InetSocketAddress(iadr, intPort);
            server = new Server(siadr);
        }

        // Create a basic jetty server object that will listen on port 8080.  Note that if you set this to port 0
        // then a randomly available port will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        HandlerList handlers = new HandlerList();

        Handler dadHandler = createDADHandler(props, apf);

        ArrayList<Handler> l = createStaticHandler(props);
        for (Handler h : l) {
            handlers.addHandler(h);
        }
        // if dadHandler is added first, the static handlers are not visible
        handlers.addHandler(dadHandler);
        server.setHandler(handlers);
        // Start things up! By using the server.join() the server thread will join with the current thread.
        // See "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html#join()" for more details.
        try {
            server.start();
        } catch (java.net.BindException bex) {
            msg("ERROR: the port " + intPort + " is already in use. Exiting ...");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            msg("Exiting ...");
            System.exit(1);
        }
        server.join();
    }

    static Handler createDADHandler(java.util.Properties props, File propertyFile) {
        ServletContextHandler handler = new ServletContextHandler();
        // Passing in the class for the servlet allows jetty to instantite an instance of that servlet and mount it
        // on a given context path.

        // !! This is a raw Servlet, not a servlet that has been configured through a web.xml or anything like that !!
        ServletHolder holder = new ServletHolder(com.prism.ServletWrapper.class);

        File pc = propertyFile; //new File(prismconf).getAbsoluteFile();
        if (pc.canRead()) {
            holder.setInitParameter("properties", pc.toString());
        } else {
            msg("ERROR: can not read config file: " + pc.toString());
            System.exit(1);
        }
        msg("dbprism config " + pc.toString());
        String dads = props.getProperty("dads", "/dads");
        handler.addServlet(holder, dads + "/*");
        return handler;
    }

    static ArrayList<Handler> createStaticHandler(java.util.Properties props) {
        ArrayList<Handler> res = new ArrayList<>();
        HashSet<String> set = new HashSet<>();
        for (String s : props.stringPropertyNames()) {
            if (s.startsWith("static.")) {
                String part = s.substring("static.".length());
                if (part.equals("")) {
                    continue;
                }
                int p = part.indexOf(".");
                if (p == 0) {
                    continue;
                }
                if (p < 0) {
                    set.add(part);
                } else {
                    set.add(part.substring(0, p));
                }
            }
        }
        for (String staticc : set) {
            String prefix = "static." + staticc + ".";
            String dir = props.getProperty(prefix + "dir", "");
            if (dir.equals("")) {
                // obviously nonsens
                continue;
            }
            File f = new File(dir).getAbsoluteFile();
            if (!f.isDirectory()) {
                throw new RuntimeException("not a dir " + f);
            }
            String targetDir = f.getAbsolutePath();

            String mount = props.getProperty(prefix + "wdir", "");
            if (mount.equals("")) {
                continue;
            }

            ResourceHandler resource_handler = new ResourceHandler();
            resource_handler.setDirectoriesListed(true);
            resource_handler.setResourceBase(targetDir);

            String cc = props.getProperty(prefix + "cache-control", "");
            if (!cc.equals("")) {
                resource_handler.setCacheControl(cc);
            }
            ContextHandler contextHandler = new ContextHandler(mount);
            contextHandler.setHandler(resource_handler);
            msg("adding static dir " + mount + " -> " + targetDir);
            res.add(contextHandler);
        }
        return res;

    }

}
