/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism.utils;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import spinat.jettyprism.Configuration;

import com.prism.ConnInfo;
import com.prism.DBConnection;
import com.prism.DBFactory;
import com.prism.DBPrismConnectionCache;

/**
 * This class is a Singleton that provides access to one or many connection
 * pools defined in a Property file. A client gets access to the single instance
 * through the static getInstance() method and can then check-out and check-in
 * connections from a pool. When the client shuts down it should call the
 * release() method to close all open connections and do other clean up. Need a
 * java enabled 8.1.6+ database to run. <BR>
 * 
 * @author Marcelo F. Ochoa <BR>
 * <BR>
 * Modified: 14/Oct/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser
 * </a> (LXG) <BR>
 * Changes :
 * <UL>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI>
 * </UL>
 */
public class JndiDBPrismConnectionCacheImpl implements DBPrismConnectionCache {
    private static Logger log = Logger.getLogger(JndiDBPrismConnectionCacheImpl.class);

    private static JndiDBPrismConnectionCacheImpl instance = null;

    private static int clients = 0;

    private Hashtable dataSources = null;

    /**
     * Singleton
     * 
     * @param prop Configuration
     * @return JndiDBPrismConnectionCacheImpl
     * @throws Exception
     */
    public static synchronized JndiDBPrismConnectionCacheImpl getInstance(Configuration prop) throws Exception {
        if (instance == null) {
            if (log.isInfoEnabled())
                log.info(".getInstance - Initializing Transaction manager...");
            instance = new JndiDBPrismConnectionCacheImpl(prop);
        }
        clients++;
        return instance;
    }

    /**
     * A private constructor since this is a Singleton
     * 
     * @param prop Configuration
     * @throws Exception
     */
    private JndiDBPrismConnectionCacheImpl(Configuration prop) throws Exception {
        init(prop);
    }

    /**
     * Initialize BusyList (Busy Connection) Set JTADBPrismConnectionCacheImpl
     * parameters. Start a Thread wich control age of connections and
     * transacctions
     * 
     * @param prop Configuration
     * @throws Exception
     */
    public synchronized void init(Configuration prop) throws Exception {
        dataSources = new Hashtable();
        InitialContext ic1 = new InitialContext();
        Hashtable env = new Hashtable();
        env.put("dedicated.connection", "true");
        InitialContext ic2 = new InitialContext(env);
        try {
          Context envCtx1 = (Context)ic1.lookup("java:comp/env");
          Context envCtx2 = (Context)ic2.lookup("java:comp/env");
          // Create a DataSource for every DAD
          // using the syntax jdbc/DAD
          Enumeration diccList = DBConnection.getAll();
          while (diccList.hasMoreElements()) {
            ConnInfo connInfo = (ConnInfo) diccList.nextElement();
            DataSource ds = null;
            try {
                if (connInfo.pass.equalsIgnoreCase("")) {
                    // Creates a Jndi Datasource using Jndi Datasource username
                    // and password
                    ds = (DataSource) envCtx1.lookup("jdbc/" + connInfo.connAlias);
                    log.debug("Datasource added 'jdbc/"+ connInfo.connAlias+"' with username extracted from JNDI");
                } else {
                    // Creates a Jndi Datasource using dedicated connection mode
                    ds = (DataSource) envCtx2.lookup("jdbc/" + connInfo.connAlias);
                    log.debug("Datasource added 'jdbc/"+ connInfo.connAlias+"' with dynamic username and password");
                }
                dataSources.put(connInfo.connAlias, ds);
            } catch (NamingException ne) {
              log.warn("Warning: error when trying to lookup DataSource 'jdbc/" + connInfo.connAlias + " due to :" + ne.getExplanation());
            }
         }
       } catch (NamingException ne) {
        log.warn("Warning: error when trying to lookup DataSources due to :" + ne.getExplanation());
       }
    }

    /** A public constructor nothing to do here. */
    public JndiDBPrismConnectionCacheImpl() {
        super();
    }

    /**
     * Implements method get from interface DBPrismConnectionCache Get one
     * Connection from Jndi Datasource using dynamic username and password
     * 
     * @param req HttpServletRequest
     * @param usr String
     * @param pass String
     * @return DBConnection
     * @throws SQLException
     */
    public DBConnection get(HttpServletRequest req, String usr, String pass) throws SQLException {
        if (log.isDebugEnabled())
            log.debug(".get entered.");
        ConnInfo cc = new ConnInfo(req);
        DBFactory factory = cc.getFactory();
        DataSource ds = (DataSource) dataSources.get(cc.connAlias);
        // Sanity checks
        if (ds == null) {
            log.warn("JndiDBPrismConnectionCacheImpl: DataSource not found 'jdbc/" + cc.connAlias + "'");
            throw new SQLException("JndiDBPrismConnectionCacheImpl: DataSource not found 'jdbc/" + cc.connAlias + "'");
        }
        DBConnection connection = factory.createDBConnection(cc); // Allocated
                                                                  // DBConnection
        connection.connInfo = cc; // Set default values for ConnInfo object
        connection.connInfo.txEnable = false; // set as direct connection
        connection.connInfo.status = ConnInfo.CONN_DIR;
        connection.connInfo.usr = cc.usr;
        connection.connInfo.pass = cc.pass;
        connection.connInfo.connectString = cc.connectString;
        if (cc.usr.equalsIgnoreCase("")) {
            // if usr is not null, try to connect using dynamic usr/pass 
            connection.sqlconn = ds.getConnection(usr, pass);
            if (log.isDebugEnabled())
              log.debug(".get returning a datasource connection with dynamic user/pass.");
        } else {
            // use DataSource stored user/pass
            connection.sqlconn = ds.getConnection();
            if (log.isDebugEnabled())
              log.debug(".get returning a datasource connection with JNDI user/pass.");
        }
        connection.sqlconn.setAutoCommit(false);
        return connection;
    }

    /**
     * Implements method release from interface DBPrismResource Shutdown
     * JTADBPrismConnectionCacheImpl
     * 
     * @throws SQLException
     */
    public void release() throws SQLException {
        clients--;
        if (clients == 0) {
            dataSources.clear();
            dataSources = null;
        }
        if (log.isDebugEnabled())
            log.debug(".release exiting clients= " + clients);
    }

    /**
     * Implements method release from interface DBPrismResource Release one
     * Connection
     * 
     * @param req HttpServletRequest
     * @param connection DBConnection
     * @throws SQLException
     * @throws NullPointerException
     */
    public void release(HttpServletRequest req, DBConnection connection) throws SQLException, NullPointerException {
        connection.sqlconn.close();
        connection.sqlconn = null;
        connection.connInfo.status = ConnInfo.CONN_FREE;
        if (log.isDebugEnabled())
            log.debug(".release closed datasource connection.");
    }
}