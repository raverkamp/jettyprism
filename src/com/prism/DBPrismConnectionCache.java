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

import spinat.jettyprism.Configuration;

/**
 * This interface defines a contract for the ResourceManager
 * DBPrism engine calls these methods to get and release connections
 * for an specific Database Resource Manager.
 * By default there are three Resource Manager implementation:
 * - com.utils.JdbcDBPrismConnectionCacheImpl which works with JDBC 1.x specification 
 * - com.oracle.JTADBPrismConnectionCacheImpl which works with JDBC 2.0 drivers
 * - com.utils.JndiDBPrismConnectionCacheImpl which works with JDBC 2.0 drivers
 * the last two drivers must be implement JDBC 2.0 Standard Extension API
 * @author Marcelo F. Ochoa<BR>
 * <BR>
 * Modified: 17/Jan/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavDoc cleanup</LI></UL>
 */
public interface DBPrismConnectionCache {
    /** 
     * Initialization part 
     * @param props Configuration
     * @throws Exception
     */
    public abstract void init(Configuration props) throws Exception;

    /** 
     * Get one Connection 
     * @param req HttpServletRequest
     * @param usr String
     * @param pass String
     * @return DBConnection
     * @throws SQLException
     */
    public abstract DBConnection get(HttpServletRequest req, String usr, String pass) throws SQLException;

    /** 
     * Shutdown ResourceManager 
     * @throws SQLException
     */
    public abstract void release() throws SQLException;

    /** 
     * Release one Connection 
     * @param req HttpServletRequest
     * @param connection DBConnection
     * @throws SQLException
     */
    public abstract void release(HttpServletRequest req, DBConnection connection) throws SQLException;
}