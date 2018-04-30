/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * This class stores the cgi environment to be passed to the DB.
 * <P>
 * @author Marcelo F. Ochoa<BR>
 * <BR>
 * Modified: 17/Mar/2004 by <a href="mailto:jhking@airmail.net">Jason King</a>
 * (LXG)<BR>
 * Changes : <UL><LI>Added Authorizaion cgiVar</LI></ul>
 * Modified: 4/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 */
public class CgiVars {

    Logger log = Logger.getLogger(CgiVars.class);

    public ArrayList<String> names = new ArrayList<>();

    public ArrayList<String> values = new ArrayList<String>();

    private void add(String name, String val) {
        if (null != val) {
            this.names.add(name);
            this.values.add(val);
        }
    }

    /**
     * Creates the class and stores all cgi environment variables in two arrays.
     *
     * @param req HttpServletRequest - the request that initiated this call
     * @param connInfo ConnInfo - connection info
     * @param name String - username
     * @param pass String - password
     */
    public CgiVars(HttpServletRequest req, ConnInfo connInfo, String name,
            String pass) {
        this.add("REQUEST_METHOD", req.getMethod());
        this.add("PATH_INFO", req.getPathInfo());
        this.add("PATH_TRANSLATED", req.getPathTranslated());
        this.add("QUERY_STRING", req.getQueryString());
        this.add("REMOTE_USER", name);
        this.add("AUTH_TYPE", ((name.equals("") && pass.equals("")) ? req.getAuthType() : "Basic"));
        this.add("SCRIPT_NAME", "" + req.getContextPath() + req.getServletPath());
        String argValue;
        if ((argValue = req.getServletPath()) != null
                && argValue.indexOf(connInfo.connAlias) > 0) {
            argValue = req.getContextPath() + argValue;
            this.add("SCRIPT_PREFIX", argValue.substring(0, argValue.indexOf(connInfo.connAlias)));
        }
        this.add("SERVER_SOFTWARE", DBPrism.NAME);
        this.add("CONTENT_LENGTH", "" + req.getContentLength());
        this.add("CONTENT_TYPE", req.getContentType());
        this.add("SERVER_PROTOCOL", req.getProtocol());
        this.add("REQUEST_PROTOCOL", req.getScheme());
        this.add("SERVER_NAME", req.getServerName());
        this.add("SERVER_PORT", "" + req.getServerPort());
        this.add("REMOTE_ADDR", req.getRemoteAddr());
        this.add("REMOTE_HOST", req.getRemoteHost());
        this.add("REMOTE_PORT", "" + req.getRemotePort());
        this.add("HTTP_REFERER", req.getHeader("Referer"));
        this.add("HTTP_USER_AGENT", req.getHeader("User-Agent"));
        this.add("HTTP_PRAGMA", req.getHeader("Pragma"));
        this.add("HTTP_HOST", req.getHeader("Host"));
        this.add("HTTP_ACCEPT", req.getHeader("Accept"));
        this.add("HTTP_ACCEPT_ENCODING", req.getHeader("Accept-Encoding"));
        this.add("HTTP_ACCEPT_LANGUAGE", req.getHeader("Accept-Language"));
        this.add("HTTP_ACCEPT_CHARSET", req.getHeader("Accept-Charset"));
        this.add("HTTP_IF_MODIFIED_SINCE", req.getHeader("If-Modified-Since"));
        this.add("HTTP_COOKIE", req.getHeader("Cookie"));

        this.add("DAD_NAME", connInfo.connAlias);
        this.add("DOC_ACCESS_PATH", connInfo.docAccessPath);
        this.add("DOCUMENT_TABLE", connInfo.documentTable);
        this.add("PLSQL_GATEWAY", DBPrism.NAME);
        this.add("GATEWAY_IVERSION", DBPrism.VERSION);
        this.add("REQUEST_IANA_CHARSET", connInfo.clientCharset);

        Enumeration<String> hn = req.getHeaderNames();
        while (hn.hasMoreElements()) {
            String headerName = hn.nextElement();
            this.add(headerName, req.getHeader(headerName));
        }
        if (log.isDebugEnabled()) {
            log.debug(".CgiVars dump cgi array start.");
            for (int i = 0; i < this.names.size(); i++) {
                log.debug(" '" + names.get(i) + "' = '" + values.get(i) + "'");
            }
            log.debug(".CgiVars dump cgi array end.");
        }
    }
}
