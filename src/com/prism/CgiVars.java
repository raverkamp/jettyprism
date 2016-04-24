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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * This class stores the cgi environment to be passed to the DB. <P>
 * @author Marcelo F. Ochoa<BR>
 * <BR>
 * Modified: 17/Mar/2004 by <a href="mailto:jhking@airmail.net">Jason King</a> (LXG)<BR>
 * Changes : <UL><LI>Added Authorizaion cgiVar</LI></ul>
 * Modified: 4/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 */
public class CgiVars {
    Logger log = Logger.getLogger(CgiVars.class);

    public String[] names = new String[50];

    public String[] values = new String[50];

    public int size = 0;

    /**
   * Creates the class and stores all cgi environment variables in two arrays.
   * @param req HttpServletRequest - the request that initiated this call
   * @param connInfo ConnInfo - connection info
   * @param name String - username
   * @param pass String - password
   */
    public CgiVars(HttpServletRequest req, ConnInfo connInfo, String name,
                   String pass) {
        int n_size = 0;
        String argValue;
        if ((argValue = req.getMethod()) != null) {
            names[n_size] = "REQUEST_METHOD";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getPathInfo()) != null) {
            names[n_size] = "PATH_INFO";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getPathTranslated()) != null) {
            names[n_size] = "PATH_TRANSLATED";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getQueryString()) != null) {
            names[n_size] = "QUERY_STRING";
            values[n_size++] = argValue;
        }
        names[n_size] = "REMOTE_USER";
        values[n_size++] = name;
        if ((argValue = req.getAuthType()) != null) {
            names[n_size] = "AUTH_TYPE";
            values[n_size++] =
                ((name.equals("") && pass.equals("")) ? argValue : "Basic");
        }
        if ((argValue = req.getHeader("AUTHORIZATION")) != null) {
            names[n_size] = "AUTHORIZATION";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getServletPath()) != null) {
            names[n_size] = "SCRIPT_NAME";
            values[n_size++] = req.getContextPath() + argValue;
        }
        if ((argValue = req.getServletPath()) != null &&
            argValue.indexOf(connInfo.connAlias) > 0) {
            names[n_size] = "SCRIPT_PREFIX";
            argValue = req.getContextPath() + argValue;
            values[n_size++] =
                argValue.substring(0, argValue.indexOf(connInfo.connAlias));
        }
        names[n_size] = "SERVER_SOFTWARE";
        values[n_size++] = DBPrism.NAME;
        names[n_size] = "CONTENT_LENGTH";
        values[n_size++] = "" + req.getContentLength();
        if ((argValue = req.getContentType()) != null) {
            names[n_size] = "CONTENT_TYPE";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getProtocol()) != null) {
            names[n_size] = "SERVER_PROTOCOL";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getScheme()) != null) {
            names[n_size] = "REQUEST_PROTOCOL";
            values[n_size++] = argValue.toUpperCase();
        }
        if ((argValue = req.getServerName()) != null) {
            names[n_size] = "SERVER_NAME";
            values[n_size++] = argValue;
        }
        names[n_size] = "SERVER_PORT";
        values[n_size++] = "" + req.getServerPort();
        if ((argValue = req.getRemoteAddr()) != null) {
            names[n_size] = "REMOTE_ADDR";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getRemoteHost()) != null) {
            names[n_size] = "REMOTE_HOST";
            values[n_size++] = argValue;
        }
        if ((argValue = "" +req.getRemotePort()) != null) {
            names[n_size] = "REMOTE_PORT";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Referer")) != null) {
            names[n_size] = "HTTP_REFERER";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("User-Agent")) != null) {
            names[n_size] = "HTTP_USER_AGENT";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Pragma")) != null) {
            names[n_size] = "HTTP_PRAGMA";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Host")) != null) {
            names[n_size] = "HTTP_HOST";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Accept")) != null) {
            names[n_size] = "HTTP_ACCEPT";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Accept-Encoding")) != null) {
            names[n_size] = "HTTP_ACCEPT_ENCODING";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Accept-Language")) != null) {
            names[n_size] = "HTTP_ACCEPT_LANGUAGE";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("Accept-Charset")) != null) {
            names[n_size] = "HTTP_ACCEPT_CHARSET";
            values[n_size++] = argValue;
        }
        if ((argValue = req.getHeader("If-Modified-Since")) != null) {
            names[n_size] = "HTTP_IF_MODIFIED_SINCE";
            values[n_size++] = argValue;
            DateFormat df =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                                                 java.util.Locale.US);
            try {
                java.util.Date lastClientMod = df.parse(argValue);
                if (log.isDebugEnabled()) {
                    log.debug("If-Modified-Since:" + lastClientMod.getTime() / 1000 *
                                    1000);
                }
            } catch (ParseException e) {
                log.warn(".CgiVars - Error parsing If-Modified-Since header", e);
            }
        }
        if ((argValue = req.getHeader("Cookie")) != null) {
            names[n_size] = "HTTP_COOKIE";
            values[n_size++] = argValue;
        }
        names[n_size] = "DAD_NAME";
        values[n_size++] = connInfo.connAlias;
        names[n_size] = "DOC_ACCESS_PATH";
        values[n_size++] = connInfo.docAccessPath;
        names[n_size] = "REQUEST_CHARSET";
        values[n_size++] = connInfo.dbCharset;
        names[n_size] = "DOCUMENT_TABLE";
        values[n_size++] = connInfo.documentTable;
        names[n_size] = "PLSQL_GATEWAY";
        values[n_size++] = DBPrism.NAME;
        names[n_size] = "GATEWAY_IVERSION";
        values[n_size++] = DBPrism.VERSION;
        names[n_size] = "REQUEST_IANA_CHARSET";
        values[n_size++] = connInfo.clientCharset;
        size = n_size;
        if (log.isDebugEnabled()) {
            log.debug(".CgiVars dump cgi array start.");
            for (int i = 0; i < size; i++) {
                log.debug(" '" + names[i] + "' = '" + values[i] + "'");
            }
            log.debug(".CgiVars dump cgi array end.");
        }
    }
}
