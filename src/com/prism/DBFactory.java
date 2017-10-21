/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism;




import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

/**
 * This class plays the role of an AbstractFactory of the Abstract Factory pattern.
 * It has as many subclasses as databases are supported. For each new database a new 
 * subclass must be created. It declares an interface for operations that create 
 * Abstract Product objects.<BR>
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 * <BR>
 * Modified: 3/Feb/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavDoc cleanup</LI></UL>
 */
public  class DBFactory {
  public DBFactory() {
    // LXG: call to super is generated anyway but put it here for clarity.
    super();
  }
public SPProc createSPProc(ConnInfo conn, String procname, Connection sqlconn) throws SQLException {
        SPProc cc = new SPProc();
        return cc.create(conn, procname, sqlconn);
    }

    /**
     * This method returns a concrete DBConnPLSQL instance.
     */
    public DBConnection createDBConnection(ConnInfo connInfo) {
        return new DBConnection(connInfo);
    }

    /**
     * This method returns a concrete UploadRequest instance.
     */
    public UploadRequest createUploadRequest(HttpServletRequest request, DBConnection repositoryConnection)
            throws IOException, SQLException {
        UploadRequest cc = new UploadRequest(request, repositoryConnection);
        return cc;
    }

    /**
     * This method returns a concrete DownloadRequest instance.
     */
    public DownloadRequest createDownloadRequest(HttpServletRequest request,
            DBConnection repositoryConnection) throws IOException, SQLException {
        DownloadRequest cc = new DownloadRequest();
        return cc.create(request, repositoryConnection);
    }
}
