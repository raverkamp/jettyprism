/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in *
 * the LICENSE file. *
 */
package com.prism;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.log4j.Logger;
import spinat.jettyprism.Configuration;

/**
 * This class is a Singleton that provides access to one or many connection DB
 * defined in a Property file. A client gets access to the single instance
 * through the static getInstance()<BR>
 * <BR>
 * Modified: 01/Apr/2005 by <a href="mailto:jhking@airmail.net">Jason King</a>
 * (JHK)<BR>
 * Changes : <UL><LI>More debug call-outs/LI></ul>
 *
 *
 * Modified: 18/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a>
 * (JHK)<BR>
 * Changes : <UL><LI>Made configuration name a static</LI></ul>
 *
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 *
 */
public class DBPrism {

    final private static Logger log = Logger.getLogger(DBPrism.class);
    final public static java.lang.String NAME = "DBPrism";
    final public static java.lang.String VERSION = "2018-04-20-production";
    final private static java.lang.String CONFIGURATION = "prism.xconf";  // JHK this string should only appear once in this file.
    final public static java.lang.String PROPERTIES = "/" + CONFIGURATION;
    private ProcedureCache procedureCache = null;
    private Configuration properties = null;

    /**
     * private connection which hold the connection betwen makePage and getPage
     * steps
     */
    //private DBConnection connection = null;
    /**
     * Makes a page from Request If the request has not user/pass information
     * and the connection is with dymanic login throw NotAuthorizedException. If
     * it is not possible to establish the connection throw
     * NotAuthorizedException. If there are errors in the page generation,
     * according to the kind of errors the responsability is forwarded to the
     * wrappers If there aren't errors the connection is not free, this
     * connection will be free in getPage step
     *
     * @param req HttpServletRequest
     * @throws Exception
     */
    public Content makePage(HttpServletRequest req, ConnInfo cc_tmp) throws Exception {
        log.debug(".makePage entered.");

        DBConnection connection = null;
        String name;
        String password;
        boolean success = false;
        try {
            int i;
            String str;
            try {
                log.debug("Auto " + req.getHeader("Authorization"));
                String s = req.getHeader("Authorization").substring(6);
                byte[] bytes = DatatypeConverter.parseBase64Binary(s);
                str = new String(bytes, cc_tmp.clientCharset);
                log.debug("ist: " + str);
            } catch (Exception e) {
                str = ":";
            }
            i = str.indexOf(':');
            if (i != -1) {
                name = str.substring(0, i);
                password = str.substring(i + 1);
            } else {
                name = "";
                password = "";
            }
            boolean dLogin = "".equals(cc_tmp.usr);
            if (!dLogin) {
                // if DAD username is not null, log to database using DAD username and password 
                connection = this.createDBConnection(cc_tmp, cc_tmp.usr, cc_tmp.pass);
                if (log.isDebugEnabled()) {
                    log.debug("Using a " + connection.getClass().getName() + " class");  // JHK
                }        // Copy DAD username and password from DAD info
            } else if ("".equals(name) || "".equals(password)) {
                // if DAD username is null, and no user/pass is given into the B64 string 
                throw new NotAuthorizedException(cc_tmp.dynamicLoginRealm);
            } else {
                try { // DAD username is null, try to connect using B64 user/pass values
                    connection = this.createDBConnection(cc_tmp, name, password);
                    if (log.isDebugEnabled()) {
                        log.debug("Using a " + connection.getClass().getName() + " class");  // JHK
                    }
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1017) {
                        //ORA-01017: invalid username/password; logon denied. 
                        throw new NotAuthorizedException(cc_tmp.dynamicLoginRealm);
                    }
                    log.error("connect failed", e);
                    throw e;
                }
            }
            connection.doCall(procedureCache, req);
            Content pg = connection.getGeneratedStream(req);
            if (log.isDebugEnabled()) {
                log.debug(".makePage doCall success on " + connection);
            }
            success = true;
            return pg;
        } catch (Exception e) {
            // try to free the connection
            log.error(".makePage exception: " + connection, e);
            // throw the exception as is
            throw e;
        } finally {
            // if there is a failure, clear the cache
            //   the reason might be a changed procedure 
            //   we must get rid of it
            if (!success) {
                this.procedureCache.clear();
            }
            if (connection != null) {
              connection.releasePage();
            }
        }
    }

    /**
     * Returns DB Prism version info
     *
     * @return String
     */
    public String getVersion() {
        return NAME + VERSION + " (C) Marcelo F. Ochoa (2000-2008)";
    }

    /**
     * Makes a download from Request If the request has no user/pass information
     * and the connection is with dynamic login throw NotAuthorizedException. If
     * it is not possible to establish the connection throw
     * NotAuthorizedException. If there is error in the page generation,
     * according to the kind of errors the responsibility is forwarded to the
     * wrappers This step free the connection, different from makePage which
     * free the connection in getPage step
     *
     * @param req
     * @param res
     * @throws Exception
     */
    public void downloadDocumentFromDB(HttpServletRequest req, HttpServletResponse res, ConnInfo cc_tmp) throws Exception {
        String name;
        String password;
        DBConnection connection = null;
        try {
            int i;
            String str;
            try {

                String s = req.getHeader("Authorization").substring(6);
                byte[] bytes = DatatypeConverter.parseBase64Binary(s);
                str = new String(bytes, cc_tmp.clientCharset);
            } catch (Exception e) {
                str = ":";
            }
            i = str.indexOf(':');
            if (i != -1) {
                name = str.substring(0, i);
                password = str.substring(i + 1);
            } else {
                name = "";
                password = "";
            }
            boolean dLogin = "".equals(cc_tmp.usr);
            if (!dLogin) {
                // if DAD username is not null, log to database using DAD username and password 
                connection = this.createDBConnection(cc_tmp, cc_tmp.usr, cc_tmp.pass);
                if (log.isDebugEnabled()) {
                    log.debug("Using a " + connection.getClass().getName() + " class");  // JHK
                }        // Copy DAD username and password from DAD info
                if ("".equalsIgnoreCase(name)) {
                    name = cc_tmp.usr;
                }
                if ("".equalsIgnoreCase(password)) {
                    password = cc_tmp.pass;
                }
            } else if ("".equals(name) || "".equals(password)) {
                // if DAD username is null, and no user/pass is given into the B64 string 
                throw new NotAuthorizedException(cc_tmp.dynamicLoginRealm);
            } else {
                try { // DAD username is null, try to connect using B64 user/pass values
                    connection = this.createDBConnection(cc_tmp, name, password);
                } catch (SQLException e) {
                    throw new NotAuthorizedException(cc_tmp.dynamicLoginRealm);
                }
            }
            connection.doDownload(req, res, name, password);
            if (log.isDebugEnabled()) {
                log.debug("DBPrism: doDownload success on " + connection);
            }
        } finally {
            connection.releasePage();
        }
    }

    /**
     * A public constructor to manage multiple connections
     */
    public DBPrism() {
        if (log.isDebugEnabled()) {
            log.debug("DBPrism()");
        }
    }

    public void init(Configuration properties) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(".init entered.");
        }
        this.properties = properties;
        boolean cachep = properties.getBooleanProperty("cacheprocedure", true);
        procedureCache = new ProcedureCache(cachep);

        if (log.isDebugEnabled()) {
            log.debug(".init exited.");
        }
    }

    /**
     * Free all resources
     *
     * @throws Exception
     */
    public synchronized void release() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(".release entered.");
        }
        for (Map.Entry<String, OracleDataSource> e : this.datasources.entrySet()) {
            e.getValue().close();
        }
        this.datasources.clear();
        if (log.isDebugEnabled()) {
            log.debug(".release DBPrism shutdown complete.");
        }
    }

    private final HashMap<String, OracleDataSource> datasources = new HashMap<>();

    private synchronized OracleDataSource getDataSource(ConnInfo ci) throws SQLException {
        if (datasources.containsKey(ci.connAlias)) {
            return datasources.get(ci.connAlias);
        }
        OracleDataSource ds = new OracleDataSource();
        ds.setURL(ci.connectString);
        ds.setConnectionCachingEnabled(true);
        datasources.put(ci.connAlias, ds);
        return ds;

    }

    DBConnection createDBConnection(ConnInfo ci, String user, String pw) throws SQLException {
        OracleDataSource ds = getDataSource(ci);
        OracleConnection con = (OracleConnection) ds.getConnection(user, pw);
        con.setAutoCommit(false);
        return new DBConnection(this.properties, ci, con);
    }
}
