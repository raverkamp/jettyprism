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
public abstract class DBFactory {
  public DBFactory() {
    // LXG: call to super is generated anyway but put it here for clarity.
    super();
  }

  /** 
   * This method returns a concrete SPProc instance. 
   * @param conn ConnInfo
   * @param procname String
   * @param sqlconn Connection
   * @return SPProc
   * @throws SQLException
   */
  public abstract SPProc createSPProc(ConnInfo conn, String procname, Connection sqlconn) throws SQLException;

  /** 
   * This method returns a concrete DBConnection instance. 
   * @param connInfo ConnInfo
   * @return DBConnection
   */
  public abstract DBConnection createDBConnection(ConnInfo connInfo);

  /** 
   * This method returns a concrete UploadRequest instance. 
   * @param request HttpServletRequest
   * @param repositoryConnection DBConnection
   * @return UploadRequest
   * @throws IOException
   * @throws SQLException
   */
  public abstract UploadRequest createUploadRequest(HttpServletRequest request, DBConnection repositoryConnection) throws IOException, SQLException;

  /** 
   * This method returns a concrete DownloadRequest instance. 
   * @param request HttpServletRequest
   * @param repositoryConnection DBConnection
   * @return DownloadRequest
   * @throws IOException
   * @throws SQLException
   */
  public abstract DownloadRequest createDownloadRequest(HttpServletRequest request, DBConnection repositoryConnection) throws IOException, SQLException;
}
