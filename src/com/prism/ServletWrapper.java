/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spinat.jettyprism.Configuration;

/**
 * Servlet wrapper class for Prism.<BR>
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 *
 */
public class ServletWrapper extends HttpServlet {

    private static final Logger log = LogManager.getLogger();

    private DBPrism dbprism = null;
    private Configuration config;
    private int behavior;
    private int errorLevel;
    private java.lang.String UnauthorizedText;

    public ServletWrapper() {
        // LXG: call to super is generated anyway but put it here for clarity.
        super();
    }

    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);

        try {
            String propsString = sc.getInitParameter("properties_string");
            if (propsString != null) {
                // this is the main method (internal Jetty hosting) branch,
                // the config file is read into a string and supplied 
                //as string in an init parameter
                this.config = Configuration.loadFromPropertiesString(propsString);
            } else {
                String props = sc.getInitParameter("properties");
                try (InputStream stream = this.getServletContext().getResourceAsStream("/WEB-INF/" + props)) {
                    this.config = Configuration.loadFromStream(stream);
                };
            }
            log.debug("<<<<<<<<<<<<<<< DBPrism Servlet init >>>>>>>>>>>>>>>");
            log.debug("servlet initialised.");
            // LXG: now intialise DBPrism
            this.dbprism = new DBPrism();
            this.dbprism.init(config);
            this.behavior = config.getIntProperty("behavior", 0);
            this.errorLevel = config.getIntProperty("errorLevel", 0);
            this.UnauthorizedText = config.getProperty("UnauthorizedText",
                    "You must be enter DB username and password to access at the system");
        } catch (IOException e) {
            log.error("Error Loading " + sc.getInitParameter("properties"), e);
            throw new ServletException(e);
        }
    }

    // LXG: removed ServletException since it is not actully thrown by the method
    // LXG: just put it in again if the complier complains.
    // LXG: public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    public void service(HttpServletRequest req,
            HttpServletResponse res) throws IOException {
        log.debug(".service entered.");
        String errorPage = "/error.html";
        String alias = getAliasFromURI(req);
        try {
            ConnInfo cc_tmp = new ConnInfo(this.config, alias);
            Content r = this.dbprism.makePage(req, cc_tmp);
            showPage(req, res, r);

        } catch (NotAuthorizedException ne) {
            sendUnauthorized(res, ne.getMessage());
        } catch (ProcedureNotFoundException e) {
            sendFailureMsg(res, e.getMessage());
        } catch (ExecutionException e) {
            sendFailureMsg(res, e.getMessage());
        } catch (Exception e) {
            log.error(".service unhandled exception.", e);
            if (errorLevel == 1) {
                sendFailurePage(res, errorPage);
            } else if (errorLevel == 2) {
                sendFailureMsg(res, e.getMessage());
            } else {
                sendErrorPage(res);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(".service exited.");
        }
    }

    public void destroy() {
        try {
            if (log.isDebugEnabled()) {
                log.debug(".destroy entered.");
            }
            this.dbprism.release();
        } catch (Exception e) {
            log.error("Could not destroy DB Prism", e);
        }
        if (log.isDebugEnabled()) {
            log.debug(".destroy before super.destroy().");
        }
        super.destroy();
    }

    public String getServletInfo() {
        return this.dbprism.getVersion();
    }

    public void sendUnauthorized(HttpServletResponse res,
            String msg) throws IOException {
        PrintWriter out = res.getWriter();
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // put your realm here,
        // set the charset like defined in RFC 7617, we expect UTF-8!
        res.setHeader("WWW-authenticate", "basic realm=\"" + msg + "\",  charset=\"UTF-8\"");
        out.print(this.UnauthorizedText);
        out.flush();
        if (log.isDebugEnabled()) {
            log.debug(".sendUnauthorized basic realm='" + msg + "'");
        }
    }

    public void sendFailurePage(HttpServletResponse res,
            String url) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(".sendFailurePage redirect to: " + url);
        }
        res.sendRedirect(url);
    }

    public void sendErrorPage(HttpServletResponse res) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(".sendErrorPage Not Found");
        }
        // LXG: changed to static access
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public void sendFailureMsg(HttpServletResponse res,
            String reason) throws IOException {
        PrintWriter out = res.getWriter();
        if (log.isDebugEnabled()) {
            log.debug(".sendFailureMsg generated error page " + reason);
        }
        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.println("<html><head></head><body BGCOLOR=\"#ffffff\">");
        out.println("<h3>Servlet Wrapper Error Page</h3><pre>");
        out.println(reason);

        out
                .println("<br><A HREF=\"javascript:window.location.reload()\">Reload Page</A></br>");
        out.println("<br><A HREF=\"/\">Home Page</A></br>");
        out.println("</body></html>");
        out.flush();
    }

    /**
     * Process cookie definition from Jxtp.sendCookie or Jxtp.removeCookie
     * Syntax definition from
     * http://www.netscape.com/newsref/std/cookie_spec.html See Jxtp package
     * Doesn't work with cookie definition greater than 255 char's (muti-line
     * cookies)
     */
    public Cookie Make_Cookie(String s) {
        Cookie choc_chip;
        long age;
        // Divide the string cookie format in fields by the ;
        StringTokenizer st = new StringTokenizer(s, ";");
        // the name = value pairs is required
        String s1 = (String) st.nextElement();
        choc_chip
                = new Cookie(s1.substring(0, s1.indexOf("=")), s1.substring(s1
                        .indexOf("=")
                        + 1));
        //System.out.println("Name =>" + choc_chip.getName());
        //System.out.println("Value =>" + choc_chip.getValue());
        while (st.hasMoreElements()) {
            s1 = (String) st.nextElement();
            // Proccess the expires field
            if (s1.startsWith(" expires=")) {
                s1 = s1.substring(s1.indexOf("=") + 1);
                try {
                    // Convert the Date specification to Age format of Servlets Cookie
                    // Acording to non deprected api of JDK 1.1
                    DateFormat df
                            = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz",
                                    java.util.Locale.US);
                    Date expire = df.parse(s1);
                    Calendar cal
                            = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    cal.setTime(expire);
                    if (cal.get(1) == 1990 && cal.get(2) == 0
                            && cal.get(0) == 1) {
                        // Option remove cookie
                        age = 0;
                    } else {
                        // Session cookie or expire in the future
                        java.util.Date now = new java.util.Date();
                        age = (expire.getTime() - now.getTime()) / 1000;
                        age = (age < 0 ? -1 : age);
                    }
                    choc_chip.setMaxAge((int) age);
                    //System.out.println("Age =>" + choc_chip.getMaxAge());
                } catch (Exception e) {
                    System.out
                            .println("Invalid Date format en cookie String :" + s);
                }
            }
            // end if expires=
            // Proccess the path field
            if (s1.startsWith(" path=")) {
                // Set Path
                choc_chip.setPath(s1.substring(s1.indexOf("=") + 1));
                //System.out.println("Path =>" + choc_chip.getPath());
            }
            // end if path=
            // Proccess the domain field
            if (s1.startsWith(" domain=")) {
                // Set Domain
                choc_chip.setDomain(s1.substring(s1.indexOf("=") + 1));
                //System.out.println("Domain =>" + choc_chip.getDomain());
            }
            // end if domain=
            // Proccess the secure flags
            if (s1.startsWith(" secure")) {
                // Set Secure
                choc_chip.setSecure(true);
                //System.out.println("Secure");
            }
            // end if secure
        }
        // end while st.hasMoreElements
        // Return the cookie to the caller
        return choc_chip;
    }

    /**
     * Returns the generated page (Reader) to the browser check for the header
     * to set the response object
     */
    private void showPage(HttpServletRequest req, HttpServletResponse res,
            Content page) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(".showPage entered.");
        }
        char[] buff_out = new char[8192];
        BufferedReader in = new BufferedReader(page.getPage(), 8192);
        boolean contentType = false;
        int i;
        String s = in.readLine();
        if (s != null
                && (s.startsWith("Location: ") || s.startsWith("Set-Cookie: ")
                || s.startsWith("Content-type: ")
                || s.startsWith("X-DB-Content-length: ")
                || s.startsWith("Status: "))) {
            // Verify if the position 1..n have the Syntax "xxx : yyy"
            // handle special case of Cookie definition or Content-type, or redirect
            // generated by owa_cookie.send or owa_util.mime_header
            // other header definitions are pased as is
            do {
                // Process each line of header
                //System.out.println("header: "+s);
                if (s.startsWith("Location: ")) {
                    // Sent redirect
                    s = s.substring(10);
                    if (!s.startsWith("/") && !s.startsWith("http://")) // Convert relative path to absolute, fix warkaround with HTMLDB
                    {
                        s
                                = req.getContextPath() + "/" + getAliasFromURI(req) + "/"
                                + s;
                    }
                    // LXG: changed to static access
                    res.sendRedirect(s);
                    res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                    if (log.isInfoEnabled()) {
                        log.info(".showPage redirect to Location: " + s);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(".showPage exited.");
                    }
                    return;
                } else if (s.startsWith("Set-Cookie: ")) {
                    // Makes cookies
                    // Parse the cookie line
                    if (log.isInfoEnabled()) {
                        log.info(".showPage output cookie: " + s);
                    }
                    Cookie choc_chip = Make_Cookie(s.substring(12));
                    res.addCookie(choc_chip);
                } else if (s.startsWith("X-DB-Content-length: ")) {
                    // Set Content length
                    // Parse the cookie line
                    s = s.substring(21);
                    if (log.isInfoEnabled()) {
                        log.info(".showPage XDB setContentLength: " + s);
                    }
                    res.setContentLength(Integer.parseInt(s));
                } else if (s.startsWith("Content-type: ")) {
                    // Set content type
                    if (log.isInfoEnabled()) {
                        log
                                .info(".showPage setting Content-type: " + s.substring(14)
                                        .trim());
                    }
                    res.setContentType(s.substring(14).trim());
                    contentType = true;
                } else if (s.startsWith("Status: 304 Not Modified")) {
                    // new apex_util functionality to tell not modified content
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                } else {
                    // if not Cookie definition translate as is
                    try {
                        // if it isn't a cookie it's another header info
                        if (log.isInfoEnabled()) {
                            log.info(".showPage setting other header: " + s);
                        }
                        res
                                .setHeader(s.substring(0, s.indexOf(':')), s.substring(s
                                        .indexOf(':')
                                        + 2));
                    } catch (Exception e) {
                        log
                                .warn(".showPage failed to parse the header '" + s + "'",
                                        e);
                    }
                }
                // End if cookie
            } while ((s = in.readLine()) != null && s.length() > 0);
            // End while header lines
            if (!contentType) {
                res.setContentType("text/html");
            }
            // this is new in DBPrism 2.1.1 (support for HTMLDB 2.0 inline download functionality
            InputStream is = page.getInputStream();
            if (is != null) { // inline download support
                // copy binary content, HTTP content made with htp calls will be ignored
                byte[] bin_in = new byte[65000];
                ServletOutputStream bin_out = res.getOutputStream();
                while ((i = is.read(bin_in)) > 0) {
                    //System.out.println("out bytes=>"+i);
                    bin_out.write(bin_in, 0, i);
                }
                bin_out.flush();
                is.close();
            } else {
                // Output the rest of generated page in htp.htbuf
                // send it without paying attention to new lines
                // Get the writer
                StringBuilder sb = new StringBuilder();
                int sz;
                while ((sz = in.read(buff_out)) > 0) {
                    sb.append(buff_out, 0, sz);
                }
                in.close();

                byte[] b = sb.toString().getBytes(StandardCharsets.UTF_8);
                //  content length from database is no reliable
                res.setContentLength(b.length);
                res.setCharacterEncoding("UTF-8");
                ServletOutputStream bin_out = res.getOutputStream();
                bin_out.write(b);
                bin_out.flush();

            }
        } else {
            // if not header syntax, print it as is
            // Set default Content-type
            res.setContentType("text/html");
            // Get the writer
            PrintWriter out = null;
            out = res.getWriter();
            if (log.isDebugEnabled()) {
                log.debug(".showPage =>\n" + s);
            }
            out.println(s);
            while ((i = in.read(buff_out)) > 0) {
                if (log.isDebugEnabled()) {
                    log.debug(".showPage =>\n" + new String(buff_out, 0, i));
                }
                out.write(buff_out, 0, i);
            }
            out.flush();
            in.close();
        }
        if (log.isDebugEnabled()) {
            log.debug(".showPage exited.");
        }
    }

    private String getAliasFromURI(HttpServletRequest req) {
        String alias = "";
        int pos;
        if (log.isDebugEnabled()) {
            log.debug(".getURI finding alias in Servlet Path='" + req.getServletPath() + "' Path Info='" + req.getPathInfo() + "'");
        }
        try {
            if (this.behavior == 0) {
                // This behavior will work perfectly with Apache Jserv/mod_jk/Tomcat
                // configured as standalone servlet
                // extracts the DAD from the last part of the servlet path
                alias = req.getServletPath();
                if ((pos = alias.lastIndexOf('/')) >= 0) {
                    alias = alias.substring(pos + 1);
                }
            } else if (this.behavior == 1) {
                // extracts the DAD from the first part of the servlet path
                alias = req.getServletPath();
                if (alias.startsWith("/")) {
                    alias = alias.substring(1);
                }
                if ((pos = alias.indexOf('/')) > 0) {
                    alias = alias.substring(0, pos);
                }
            } else {
                alias = req.getPathInfo();
                alias = alias.substring(1, alias.lastIndexOf('/'));
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't extract DAD Information for '" + alias + "' behavior=" + this.behavior);
        }
        if (log.isDebugEnabled()) {
            log.debug(".getURI returning alias '" + alias + "' behaviour set to '" + this.behavior + "'");
        }
        return alias;
    }

}
