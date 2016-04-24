/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved. *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in *
 * the LICENSE file. *
 */
package com.prism.oracle;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import com.prism.ConnInfo;
import com.prism.DBConnection;
import com.prism.DBFactory;
import com.prism.DownloadRequest;
import com.prism.SPProc;
import com.prism.UploadRequest;

/**
 * This class plays the role of a ConcreteFactory of the Abstract Factory
 * pattern Implements the operations to create concrete products (Databases)<BR>
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 */
public class DB8iFactory extends DBFactory {

    public DB8iFactory() {
        // LXG: call to super is generated anyway but put it here for clarity.
        super();
    }

    /**
     * This method returns a concrete SPProcPLSQL instance.
     */
    public SPProc createSPProc(ConnInfo conn, String procname, Connection sqlconn) throws SQLException {
        SPProcPLSQL cc = new SPProcPLSQL();
        return cc.create(conn, procname, sqlconn);
    }

    /**
     * This method returns a concrete DBConnPLSQL instance.
     */
    public DBConnection createDBConnection(ConnInfo connInfo) {
        DBConnPLSQL cc = new DBConnPLSQL();
        return cc.create(connInfo);
    }

    /**
     * This method returns a concrete UploadRequest instance.
     */
    public UploadRequest createUploadRequest(HttpServletRequest request, DBConnection repositoryConnection)
            throws IOException, SQLException {
        Upload8i cc = new Upload8i();
        return cc.create(request, repositoryConnection);
    }

    /**
     * This method returns a concrete DownloadRequest instance.
     */
    public DownloadRequest createDownloadRequest(HttpServletRequest request,
            DBConnection repositoryConnection) throws IOException, SQLException {
        Download8i cc = new Download8i();
        return cc.create(request, repositoryConnection);
    }
}
