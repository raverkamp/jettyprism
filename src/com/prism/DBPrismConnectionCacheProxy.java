/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import spinat.jettyprism.Configuration;

/**
 * This class is a Singleton that provides access to one or many connection pools defined in a Property file. A client gets
 * access to the single instance through the static getInstance().
 * Also acts as proxy for the implementation of the CacheScheme defined by the user
 * using prism.properties's Manager.class.
 * When the client shuts down it should call the release() method to close all open connections and do other clean up.<BR>
 * <BR>
 * Modified: 20/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a> (JHK)<BR>
 * Changes : <UL><LI>Converted more System.out.println's to log.debug's</LI>
 *           /UL>

 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 * <BR>
 * Modified: 13/Apr/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavaDoc - fixed parameters</LI></UL>
 */
public class DBPrismConnectionCacheProxy {
    private static Logger log = Logger.getLogger(DBPrismConnectionCacheProxy.class);

    private static int clients = 0;
    private static DBPrismConnectionCacheProxy instance = null;
    public DBFactory theDBFactory;
   
    public DBPrismConnectionCache manager;

    /**
     * create a Singleton
     * @param props Configuration
     * @return DBPrismConnectionCacheProxy
     * @throws Exception
     */
    public static synchronized DBPrismConnectionCacheProxy getInstance(Configuration props) throws Exception {
        if (log.isDebugEnabled())
          log.debug(".getInstance entered.");
        if (instance == null) {
            if (log.isDebugEnabled())
              log.debug(".getInstance Initializing Transaction manager...");
            instance = new DBPrismConnectionCacheProxy(props);
        }
        clients++;
        if (log.isDebugEnabled())
          log.debug(".getInstance exited client="+clients);
        return instance;
    }

    /**
     * A private constructor since this is a Singleton
     * @param props Configuration
     * @throws Exception
     */
    private DBPrismConnectionCacheProxy(Configuration props) throws Exception {
        init(props);
    }

    /**
     * Initialization part of getInstance method Initialize TxDicc (Dictionary of Transaction Definition)
     * Initialize ConnInfo (Storing Properties File) Initialize DBConnection (Dicctionary of Connection Definition)
     * Set manager parameter. Creates an instance of DBPrismConnectionCache which implements
     * the methods forwarded by the DBPrismConnectionCacheProxy
     * @param props Configuration
     * @throws Exception
     */
    private void init(Configuration props) throws Exception {
        if (log.isDebugEnabled())
          log.debug(".init entered.");
        
        ConnInfo.init(props);
        DBConnection.init(props);
        // Load and register DBPrismConnectionCache implementation class.
        String managerClass = props.getProperty("class",null,"Manager");
        if (managerClass == null) {
            log.warn("Warning: 'class' property is not set in category 'Manager'");
            log.warn("----> Using com.prism.utils.JdbcDBPrismConnectionCacheImpl");
            managerClass = "com.prism.utils.JdbcDBPrismConnectionCacheImpl";
        }
        if (log.isDebugEnabled())
          log.debug("DBPrismConnectionCacheProxy: using "+managerClass+" class");
        manager = (DBPrismConnectionCache)Class.forName(managerClass).newInstance();
        manager.init(props);
        if (log.isDebugEnabled())
          log.debug(".init exited.");
    }

    /**
     * Get one Connection using dynamic login
     * @param req HttpServletRequest
     * @param usr String
     * @param pass String
     * @return DBConnection
     * @throws SQLException
     */
    public DBConnection get(HttpServletRequest req, String usr, String pass) throws SQLException {
        return manager.get(req, usr, pass);
    }

    /**
     * Shutdown DBPrismConnectionCacheProxy
     * @throws Exception
     */
    public void release() throws Exception {
        if (manager != null) {
          manager.release();
          DBConnection.release();
          ConnInfo.release();
         
          manager = null;
        }
        if (log.isDebugEnabled())
          log.debug(".release exited client="+clients);
        clients--;
    }

    /**
     * Release one Connection
     * @param req HttpServletRequest
     * @param connection DBConnection
     * @throws SQLException
     * @throws NullPointerException
     */
    public void release(HttpServletRequest req, DBConnection connection) throws SQLException, NullPointerException {
        manager.release(req, connection);
    }
}
