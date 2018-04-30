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

/**
 * This class is a Wrapper that provides access from XSP pages to the DBPrism instance.
 * A client gets a JDBC connection from the Prism ResourceManger.
 * This connection has the same properties as Prism DBConnections,
 * such as Transactions, DAD information located by request info, and so on.<BR> 
 * <BR>
 * Modified: 2/Dec/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 */
public class ConnectionWrapper {
    /** Get one connection, create it, if there isn't * @param req
     * @param req HttpServletRequest
     * @param usr String
     * @param pass String
     * @return DBConnection
     * @throws SQLException
     */
    public DBConnection getConnection(String alias, String usr, String pass) throws SQLException {
        return DBPrism.cache.get(alias, usr, pass);
    }

//    /** Get one connection, create it, if there isn't take username and password from ConnInfo object (DAD) * @param req
//     * @param req HttpServletRequest
//     * @return DBConnection
//     * @throws SQLException
//     */
//    public DBConnection getConnection(HttpServletRequest req) throws SQLException {
//        ConnInfo cc_tmp = null ; //new ConnInfo(req);
//        return getConnection(req, cc_tmp.usr, cc_tmp.pass);
//    }

    /** Release one Connection * @param req
     * @param req HttpServletRequest
     * @param conn DBConnection
     * @throws SQLException
     */
    public void freeConnection(DBConnection conn) throws SQLException {
        DBPrism.cache.release();
    }
}
