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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import spinat.jettyprism.Configuration;

import com.prism.utils.FlexibleRequest;
import com.prism.utils.FlexibleRequestCompact;

import java.util.Properties;

import oracle.jdbc.OracleConnection;

/**
 * This class plays the role of AbstractProduct of Abstract Factory pattern.
 * Declares an interface for a type of product object (create method).
 * Also, it is an AbstractClass of Template Method pattern.
 * Defines abstract "primitive operations" that concrete subclasses define to
 * implement step of algorithm (doCall & getGeneratedStream)<BR>
 * <BR>
 * Modified: 21/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a> (JHK)<BR>
 * Changes : <UL><LI>Added debug messages</LI>
 *           </UL>
 *
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 * <BR>
 * Modified: 2/Feb/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavDoc cleanup</LI></UL>
 */
public abstract class DBConnection {
  Logger log = Logger.getLogger(DBConnection.class);

  // From Package definition OWA_SEC
  /* PL/SQL Agent's authorization schemes                            */
  // NO_CHECK    constant integer := 1;
  // GLOBAL      constant integer := 2;
  // PER_PACKAGE constant integer := 3;
  static public final int NO_CHECK = 1; /* no authorization check             */
  static public final int GLOBAL = 2; /* global check by a single procedure */
  static public final int PER_PACKAGE = 3; /* use auth procedure in each package */
  static public final int CUSTOM = 4;
  public Connection sqlconn;
  public ConnInfo connInfo;
  private static Hashtable dicc = null;
  protected static Configuration properties = null;
  protected static boolean flexibleCompact = false;

  public DBConnection() {
    // LXG: call to super is generated anyway but put it here for clarity.
    super();
  }

  /**
   * Template method calls<BR>
   * 1. resetPackages without parameters<BR>
   * 2. setCGIVars with the req of doCall, name, pass<BR>
   * 3. getAuthMode without parameters returns a mode<BR>
   * 4. doAuthorize with mode, and conninfo,getPackage<BR>
   * 4.1. getRealm<BR>
   * 5. doIt with the req of doCall and the servletname<BR>
   * @param req HttpServletRequest
   * @param usr String
   * @param pass String
   * @throws SQLException
   * @throws NotAuthorizedException
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  // LXG: removed exceptions ExecutionErrorPageException and ExecutionErrorMsgException since they are not thrown
  // public void doCall(HttpServletRequest req, String usr, String pass) throws SQLException, NotAuthorizedException, UnsupportedEncodingException, IOException {
  public void doCall(HttpServletRequest req, String usr, String pass) throws SQLException, NotAuthorizedException, UnsupportedEncodingException, IOException {
    if (log.isDebugEnabled())
      log.debug(".doCall entered.");
    String connectedUsr = usr;
    if (connInfo.proxyUser) { // Sanity checks
        String proxyUserName = (req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : req.getRemoteUser();
        if (proxyUserName == null || proxyUserName.length() == 0) {
            String realms = getRealm();
            throw new NotAuthorizedException(realms);
        }
        if (((OracleConnection)sqlconn).isProxySession()) // may be was a failed page, close the session first
            ((OracleConnection)sqlconn).close(((OracleConnection)sqlconn).PROXY_SESSION);
        Properties proxyUserInfo = new Properties();
        proxyUserInfo.put("PROXY_USER_NAME",proxyUserName);
        ((OracleConnection)sqlconn).openProxySession(OracleConnection.PROXYTYPE_USER_NAME,proxyUserInfo);
        log.debug(".doCall - Proxy user: "+proxyUserName);
        connectedUsr = proxyUserName;
    }
    String ppackage = getPackage(req);
    // LXG: removed - unused.
    // String pprocedure = getProcedure(req);
    String command = getSPCommand(req);
    if (log.isDebugEnabled())
      log.debug("SP command: "+command);
    if (!connInfo.txEnable && !connInfo.stateLess) {
      resetPackages();
    }
    setCGIVars(req, connectedUsr, pass);
    int authStatus = doAuthorize(connInfo.customAuthentication, ppackage);
    if (authStatus != 1) {
      String realms = getRealm();
      throw new NotAuthorizedException(realms);
    }
    // Check the content type to make sure it's "multipart/form-data"
    // Access header two ways to work around WebSphere oddities
    String type = null;
    String type1 = req.getHeader("Content-Type");
    String type2 = req.getContentType();
    // If one value is null, choose the other value
    if (type1 == null && type2 != null) {
      type = type2;
    } else if (type2 == null && type1 != null) {
      type = type1;
    }
    // If neither value is null, choose the longer value
    else if (type1 != null && type2 != null) {
      type = (type1.length() > type2.length() ? type1 : type2);
    }
    if (type != null && type.toLowerCase().startsWith("multipart/form-data")) {
      // Handle multipart post, sent it as binary stream in a BLOB argument
      UploadRequest multi = connInfo.factory.createUploadRequest(req, this);
      // Calls the stored procedures with the new request
      doIt(multi, getSPCommand(multi));
    } else if (command.startsWith(connInfo.flexible_escape_char))
      // Calls the stored procedures with the wrapper request
      if (flexibleCompact)
        doIt(new FlexibleRequestCompact(req), command.substring(connInfo.flexible_escape_char.length()));
      else
        doIt(new FlexibleRequest(req), command.substring(connInfo.flexible_escape_char.length()));
    else if (command.startsWith(connInfo.xform_escape_char))
      // Calls the stored procedures with the wrapper request
        throw new RuntimeException("not implemented: XFormsRequest");
      //doIt(new XFormsRequest(req,connInfo), command.substring(1));
    else
      // Calls the stored procedures with the actual request
      doIt(req, command);
    if (log.isDebugEnabled())
      log.debug(".doCall exited.");
  }

  /**
   * Abstract method of Template Method
   * @throws SQLException
   */
  public abstract void resetPackages() throws SQLException;

  /**
   * Abstract method of Template Method
   * @param req HttpServletRequest
   * @param servletname String
   * @throws SQLException
   * @throws UnsupportedEncodingException
   */
  public abstract void doIt(HttpServletRequest req, String servletname) throws SQLException, UnsupportedEncodingException;

  /**
   * Abstract method of Template Method
   * @param cc ConnInfo
   * @return DBConnection
   */
  public abstract DBConnection create(ConnInfo cc);

  /**
   * Replace char '
   * @return A new String with the original String without char '
   * @param s A URL String with char '
   */
  public String replace2(String s) {
    int i = 0;
    while ((i = s.indexOf('\'', i)) != -1) // "'" char
      {
      s = s.substring(0, i + 1) + s.substring(i);
      i += 2;
    }
    return s;
  }

  /**
   * Replace char spaces
   * @return A new String with the original String without char '
   * @param s A URL String with char '
   */
  public String replace3(String s) {
    int i = 0;
    while ((i = s.indexOf(' ', i)) != -1) // " " char
      {
      s = s.substring(0, i) + s.substring(i + 1);
    }
    return s;
  }

  /**
   * Abstract method of Template Method
   * @param req HttpServletRequest
   * @param name String
   * @param pass String
   * @throws SQLException
   */
  public abstract void setCGIVars(HttpServletRequest req, String name, String pass) throws SQLException;

  /**
   * Abstract method of Template Method authMode specifies whether to enable custom authentication.
   * If specified, the application authenticates users in its own level
   * and not within the database level. This parameter can be set to one of the following values : none (Default)
   * global custom perPackage
   * @param authMode String
   * @param ppackage String
   * @return int
   * @throws SQLException
   */
  public abstract int doAuthorize(String authMode, String ppackage) throws SQLException;

  /**
   * Abstract method of Template Method
   * @return String
   * @throws SQLException
   */
  public abstract String getRealm() throws SQLException;

  /**
   * Abstract method of Template Method
   * @return StringReader
   * @throws Exception
   */
  public abstract Content getGeneratedStream(HttpServletRequest req) throws Exception;

  /**
    * Template method calls<BR>
    * 1. commit the connection if not part of transaction
    * 1. close a proxy connection if is using Oracle proxy user support
    * @throws Exception
    */
  public void releasePage() {
      if (!connInfo.txEnable) {
        // if the transaction is not enable, make the modifications.
        if (log.isDebugEnabled())
          log.debug("Commit normal connection");
        try {
          sqlconn.commit();
        } catch (SQLException e) {
          log.warn(".releasePage - exception on commit due: ",e);
        } finally {
          try {
            if (((OracleConnection)sqlconn).isProxySession())
              ((OracleConnection)sqlconn).close(((OracleConnection)sqlconn).PROXY_SESSION);
          } catch (SQLException s) {
            log.warn(".releasePage - exception closing proxy session: ",s);
          }
        }
      }
  }
  
  /**
   * Template method calls<BR>
   * 1. resetPackages without parameters<BR>
   * 2. setCGIVars with the req of doCall, name, pass<BR>
   * 3. getAuthMode without parameters returns a mode<BR>
   * 4. doAuthorize with mode, and conninfo,getPackage<BR>
   * 4.1. getRealm<BR>
   * 5. doDownloadFromDB with the req and res of doDownload<BR>
   * This method is similar to doCall but has a HttpServletResponse
   * object to directly sent to the output stream the downloaded document, bypassing
   * the call of getGeneratedPage to improve performance
   * @param req HttpServletRequest
   * @param res HttpServletResponse
   * @param usr String
   * @param pass String
   * @throws SQLException
   * @throws NotAuthorizedException
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  // LXG: removed ExecutionErrorPageException and ExecutionErrorMsgException since they are not thrown
  // public void doDownload(HttpServletRequest req, HttpServletResponse res, String usr, String pass) throws SQLException, NotAuthorizedException, ExecutionErrorPageException, ExecutionErrorMsgException, UnsupportedEncodingException, IOException {
  public void doDownload(HttpServletRequest req, HttpServletResponse res, String usr, String pass) throws SQLException, NotAuthorizedException, UnsupportedEncodingException, IOException {
    if (log.isDebugEnabled())
      log.debug(".doDownload entered.");
    String connectedUsr = usr;
    if (connInfo.proxyUser) { // Sanity checks
      String proxyUserName = (req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : req.getRemoteUser();
      if (proxyUserName == null || proxyUserName.length() == 0) {
        String realms = getRealm();
        throw new NotAuthorizedException(realms);
      }
      Properties proxyUserInfo = new Properties();
      proxyUserInfo.put("PROXY_USER_NAME",proxyUserName);
      if (((OracleConnection)sqlconn).isProxySession()) // may be was a failed download
          ((OracleConnection)sqlconn).close(((OracleConnection)sqlconn).PROXY_SESSION);
      ((OracleConnection)sqlconn).openProxySession(OracleConnection.PROXYTYPE_USER_NAME,proxyUserInfo);
      log.debug(".doDownload - Proxy user: "+proxyUserName);
      connectedUsr = proxyUserName;
    }
    String ppackage = getPackage(req);
    /* String pprocedure = */
    getProcedure(req);
    if (!connInfo.txEnable && !connInfo.stateLess) {
      resetPackages();
    }
    setCGIVars(req, connectedUsr, pass);
    int authStatus = doAuthorize(connInfo.customAuthentication, ppackage);
    if (authStatus != 1) {
      String realms = getRealm();
      throw new NotAuthorizedException(realms);
    }
    DownloadRequest downloadRequest = connInfo.factory.createDownloadRequest(req,  this);
    downloadRequest.doDownloadFromDB(res);
  }

  /**
   * Returns a Stored Procedure to call from the URL
   * Eg: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 return pkg.sp
   * @param req HttpServletRequest
   * @return String
   */
  public String getSPCommand(HttpServletRequest req) {
    // removes blank spaces because Oracle's dbms_utility.name_resolve
    // will ignore it and them the security system for exlcusion_list do not work
    return replace3(getServletName(req));
  }

  /**
   * Returns a Package name from the URL Eg: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 return pkg
   * @param req HttpServletRequest
   * @return String
   */
  public String getPackage(HttpServletRequest req) {
    String ppackage;
    int i;
    try {
      String servletname = getServletName(req);
      if (log.isDebugEnabled())
         log.debug("servletname = " + servletname);
      i = servletname.lastIndexOf('.');
      // Handle anonymous Procedure
      if (i < 0)
        ppackage = "";
      else
        ppackage = servletname.substring(0, i);
    } catch (Exception e) {
      i = connInfo.defaultPage.lastIndexOf('.');
      // Handle anonymous Procedure
      if (i < 0)
        ppackage = "";
      else
        ppackage = connInfo.defaultPage.substring(0, i);
    }
    // check for flexible request and xforms request
    if (ppackage.startsWith(connInfo.flexible_escape_char))
        return ppackage.substring(connInfo.flexible_escape_char.length());
    else if (ppackage.startsWith(connInfo.xform_escape_char))
        return ppackage.substring(connInfo.xform_escape_char.length());
    else
      return ppackage;
  }

  /**
   * Returns the Servlet name from the URL Ej: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 return pkg.sp
   * @param req HttpServletRequest
   * @return String
   */
  public String getServletName(HttpServletRequest req) {
    String servletpath, servletname;
    if (connInfo.alwaysCallDefaultPage)
      return connInfo.defaultPage;
    try {
      if (DBPrism.BEHAVIOR == 0 || DBPrism.BEHAVIOR == 2) {
        servletpath = replace2(req.getPathInfo());
      } else {
        servletpath = replace2(req.getServletPath());
      }
      if (log.isDebugEnabled())
        log.debug("servletpath = " + servletpath);
      servletname = servletpath.substring(servletpath.lastIndexOf('/') + 1);
      if (servletname.length() == 0)
        servletname = connInfo.defaultPage;
      if (log.isDebugEnabled()){
   			 log.debug("servletname = " + servletname);
         log.debug("servletpath = " + replace2(req.getServletPath()));
			}
    } catch (Exception e) {
      servletname = connInfo.defaultPage;
    }
    return servletname;
  }

  /**
   * Return the Stored Procedure to call from the URL
   * Ej: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 return sp
   * @param req HttpServletRequest
   * @return String
   */
  public String getProcedure(HttpServletRequest req) {
    String pprocedure;
    int i;
    try {
      String servletname = getServletName(req);
      if (log.isDebugEnabled())
        log.debug("servletname = " + servletname);
      i = servletname.lastIndexOf('.');
      // Handle anonymous Procedure
      if (i < 0)
        pprocedure = servletname;
      else
        pprocedure = servletname.substring(i + 1);
    } catch (Exception e) {
      i = connInfo.defaultPage.lastIndexOf('.');
      // Handle anonymous Procedure
      if (i < 0)
        pprocedure = connInfo.defaultPage;
      else
        pprocedure = connInfo.defaultPage.substring(i + 1);
    }
    return pprocedure;
  }

  /**
   * Format the Error Message that will be returned to the browser when an error happens
   * @param req HttpServletRequest
   * @return String
   * @throws UnsupportedEncodingException
   */
  public String MsgArgumentCallError(HttpServletRequest req) throws UnsupportedEncodingException {
    StringBuffer text_error = new StringBuffer();
    text_error.append("\n\n\n While try to execute ").append(getServletName(req));
    text_error.append("\n with args\n");
    Enumeration real_args = req.getParameterNames();
    while (real_args.hasMoreElements()) {
      String name_args = (String)real_args.nextElement();
      String multi_vals[] = req.getParameterValues(name_args);
      if (multi_vals != null && multi_vals.length > 1) { // must be owa_util.ident_array type
        text_error.append("\n").append(name_args).append(":");
        for (int i = 0; i < multi_vals.length; i++) {
          text_error.append("\n\t").append(new String(multi_vals[i].getBytes(connInfo.clientCharset)));
        }
      } else if (name_args.indexOf('.') > 0) {
        // image point data type
        text_error.append("\n").append(name_args.substring(0, name_args.indexOf('.'))).append(":");
        text_error.append("\n\t(").append(req.getParameter(name_args));
        name_args = (String)real_args.nextElement();
        text_error.append(":").append(req.getParameter(name_args) + ")");
      } else {
        // scalar data type
        text_error.append("\n").append(name_args).append(":");
        text_error.append("\n\t").append(req.getParameter(name_args));
      }
    }
    return text_error.toString();
  }

  /**
   * This class has a dictionary for all connection defs readed from properties file.
   * This method return the Enumeration object to search in all connections
   * @return Enumeration
   */
  public static Enumeration getAll() {
    return dicc.elements();
  }

  /**
   * Returns a ConnInfo object corresponding to a defintion string
   * Ej: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 
   * @param conndef String
   * @return ConnInfo object "demo"
   * @throws SQLException
   */
  public static synchronized ConnInfo getConnInfo(String conndef) throws SQLException {
    ConnInfo cc_tmp = (ConnInfo)dicc.get(conndef);
    if (cc_tmp == null)
      throw new SQLException("Can't find connection Information for '" + conndef + "'");
    return cc_tmp;
  }

  /**
   * Returns a ConnInfo object corresponding to a particular HttpServletRequest
   * Ej: http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2
   * @param req HttpServletRequest
   * @return ConnInfo object "demo"
   * @throws SQLException
   */
  public static synchronized ConnInfo getConnInfo(HttpServletRequest req) throws SQLException {
    return getConnInfo(ConnInfo.getURI(req));
  }

  /**
   * Init method Find the definition of alias (global.alias) to get connection information
   * and put into dicc to store ConnInfo information
   * @param props Configuration
   * @throws Exception
   */
  public static void init(Configuration props) throws Exception {
    String flexibleRequest, connAlias;
    flexibleRequest = props.getProperty("flexibleRequest", "old");
    flexibleCompact = flexibleRequest.equalsIgnoreCase("compact");
    connAlias = props.getProperty("alias", "");
    if (dicc == null) {
      dicc = new Hashtable();
      properties = props;
      StringTokenizer st = new StringTokenizer(connAlias, " ");
      while (st.hasMoreElements()) {
        String aliasdef = (String)st.nextElement();
        dicc.put(aliasdef, new ConnInfo(aliasdef));
      }
    }
  }

  /**
   * Release method free all resources
   * @throws Exception
   */
  public static void release() throws Exception {
    if (dicc != null) {
      properties = null;
      dicc.clear();
      dicc = null;
    }
  }
}
