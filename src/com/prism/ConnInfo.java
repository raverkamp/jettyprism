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
 * This class stores the connection information, such as user, password, and so on
 * This class has the factory instance according to the current Database.<BR>
 * <BR>
 * Modified: 18/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a> (JHK)<BR>
 * Changes : <UL><LI>More logging</LI>
 *           <LI>connectString default now obvious</LI></UL>
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 * <BR>
 * Modified: 2/Feb/2004 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavDoc cleanup</LI></UL>
 */
public class ConnInfo {
    private static Logger log = Logger.getLogger(ConnInfo.class);

    public static final int CONN_FREE = 1;
    public static final int CONN_TX = 2;
    public static final int CONN_DIR = 3;
    public java.lang.String connectString;
    public java.lang.String usr;
    public java.lang.String pass;
    public java.lang.String connAlias;
    public int errorLevel;
    public java.lang.String errorPage;
    public java.lang.String dynamicLoginRealm;
    public java.lang.String documentTable;
    public java.lang.String docAccessPath;
    public java.lang.String docAccessProcedure;
    public java.lang.String defaultPage;
    public boolean alwaysCallDefaultPage;
    public java.lang.String customAuthentication;
    public boolean proxyUser; 
    public DBFactory factory;
    public java.lang.String clientCharset = "iso-8859-1";
    public java.lang.String dbCharset = "iso-8859-1";
    private static Configuration properties;
    public int status = CONN_FREE;
    // owa_public.owa_util.ident_arr information for 7x support
    public java.lang.String type_owner;
    public java.lang.String type_name;
    public java.lang.String type_subname;
    // Customized values for ! and ^ characters
    public java.lang.String flexible_escape_char;
    public java.lang.String xform_escape_char;
    public java.lang.String xform_param_name;
    

    /**
     * This method is called once by the ResourceManager.
     * It stores the properties information in a class variable to be available
     * for all objects of DBConnection.
     * @param props Configuration
     */
    public static synchronized void init(Configuration props) {
        properties = props;
    }

    /**
     * This method is called once by the ResourceManager.
     * It cleanup the properties information in the class variable.
     */
    public static synchronized void release() {
        properties = null;
    }

  
    /**
     * This method makes a ConnInfo objects through the aliasdef Connection alias is retrieved from the URL information
     * Example: plsql => the URL http://server:port/servlet/plsql/xx.yy?arg=val
     * Example: www_dba => the URL http://server:port/servlet/www_dba/xx.yy?arg=val
     * The aliasdef works with multiples zones definition in servlet properties
     * The servlet zones args must be pointed to the same prism.properties and Prism.jar
     * See servlet.properties for more details Params: global.alias=plsql xml demo servlet xmld
     * This method retrieves the factory that has to be created (the database to be used)
     * from the properties file (prism.properties)
     * @param aliasdef String
     * @throws Exception
     */
    public ConnInfo(String aliasdef) throws Exception {
	//JHK who are we initializing
	if ( log.isDebugEnabled() ) {
            log.debug("Initializing connection: " + aliasdef );
	}
        connAlias = aliasdef;
        usr =
           properties.getProperty("dbusername","","DAD_"+aliasdef);
        pass =
           properties.getProperty("dbpassword","","DAD_"+aliasdef);
        errorPage =
           properties.getProperty("errorPage","/404.html","DAD_"+aliasdef);
        dynamicLoginRealm =
           properties.getProperty("dynamicLoginRealm",aliasdef,"DAD_"+aliasdef);
        documentTable =
           properties.getProperty("documentTable","owa_public.wpg_document","DAD_"+aliasdef);
        docAccessPath =
           properties.getProperty("docAccessPath","docs","DAD_"+aliasdef);
        docAccessProcedure =
           properties.getProperty("docAccessProcedure","owa_public.wpg_testdoc.process_download","DAD_"+aliasdef);
        defaultPage =
           properties.getProperty("defaultPage","wwwIndex.html","DAD_"+aliasdef);
        alwaysCallDefaultPage =
           properties.getBooleanProperty("alwaysCallDefaultPage",false,"DAD_"+aliasdef);
        customAuthentication =
           properties.getProperty("customAuthentication","none","DAD_"+aliasdef);
        connectString =
           properties.getProperty("connectString","****no_connect_string***","DAD_"+aliasdef); //JHK
        errorLevel =
           properties.getIntProperty("errorLevel",0,"DAD_"+aliasdef);
        flexible_escape_char =
           properties.getProperty("flexibleEscapeChar","!","DAD_"+aliasdef);
        xform_escape_char =
           properties.getProperty("xformEscapeChar","^","DAD_"+aliasdef);
        xform_param_name =
           properties.getProperty("xformParamName","post_xml","DAD_"+aliasdef);
        proxyUser = properties.getBooleanProperty("useProxyUser",false,"DAD_"+aliasdef);
        if ( log.isDebugEnabled() ) {
            log.debug("User: " + usr );
            log.debug("pass:                  "  + pass );
            log.debug("errorPage:             "  + errorPage );
            log.debug("dynamicLoginRealm:     "  + dynamicLoginRealm );
            log.debug("documentTable:         "  + documentTable );
            log.debug("docAccessPath:         "  + docAccessPath  );
            log.debug("docAccessProcedure:    "  + docAccessProcedure );
            log.debug("defaultPage:           "  + defaultPage );
            log.debug("alwaysCallDefaultPage: "  + alwaysCallDefaultPage );
            log.debug("customAuthentication:  "  + customAuthentication );
            log.debug("connectString:         "  + connectString );
            log.debug("errorLevel:            "  + errorLevel );
            log.debug("flexibleEscapeChar:    "  + flexible_escape_char );
            log.debug("xformEscapeChar:       "  + xform_escape_char );
            log.debug("xformParamName:        "  + xform_param_name );
            log.debug("useProxyUser:          "  + proxyUser );
        }

       
        factory = new com.prism.DBFactory();
        
        clientCharset =
           properties.getProperty("clientcharset","iso-8859-1","DAD_"+aliasdef);
        dbCharset =
           properties.getProperty("dbcharset","iso-8859-1","DAD_"+aliasdef);
        type_owner =
           properties.getProperty("type_owner","OWA_PUBLIC","DAD_"+aliasdef);
        type_name =
           properties.getProperty("type_name","OWA_UTIL","DAD_"+aliasdef);
        type_subname =
           properties.getProperty("type_subname","IDENT_ARR","DAD_"+aliasdef);
        status = CONN_FREE;
    }

    /**
     * This method returns a concrete factory for a concrete DataBase
     * @return DBFactory
     */
    public DBFactory getFactory() { return factory; }

    public static String getURI(HttpServletRequest req) throws SQLException {
        String alias = "";
        int pos;
        if (log.isDebugEnabled())
          log.debug(".getURI finding alias in Servlet Path='"+req.getServletPath()+"' Path Info='"+req.getPathInfo()+"'");
        try {
            if (DBPrism.BEHAVIOR == 0) {
                // This behavior will work perfectly with Apache Jserv/mod_jk/Tomcat
                // configured as standalone servlet
                // extracts the DAD from the last part of the servlet path
                alias = req.getServletPath();
                if ((pos = alias.lastIndexOf('/')) >= 0)
                    alias = alias.substring(pos + 1);
            } else if (DBPrism.BEHAVIOR == 1) {
                // extracts the DAD from the first part of the servlet path
                alias = req.getServletPath();
                if (alias.startsWith("/"))
                    alias = alias.substring(1);
                if ((pos = alias.indexOf('/')) > 0)
                    alias = alias.substring(0, pos);
            } else {
                alias = req.getPathInfo();
                alias = alias.substring(1, alias.lastIndexOf('/'));
            }
        } catch (Exception e) {
            throw new SQLException("Can't extract DAD Information for '" + alias + "' behavior=" + DBPrism.BEHAVIOR);
        }
        if (log.isDebugEnabled())
          log.debug(".getURI returning alias '"+alias+"' behaviour set to '"+DBPrism.BEHAVIOR+"'");
        return alias;
    }

    /**
     * Same as ConnInfo(aliasdef) but the information is retrieved from HttpServeleRequest
     * @param req HttpServletRequest
     * @throws SQLException
     */
    public ConnInfo(HttpServletRequest req) throws SQLException {
        String alias = getURI(req);
        connAlias = alias;
        ConnInfo cc_tmp = DBConnection.getConnInfo(connAlias);
        //System.out.println("copy ConnInfo with alias = "+connAlias);
        usr = cc_tmp.usr;
        pass = cc_tmp.pass;
        errorPage = cc_tmp.errorPage;
        errorLevel = cc_tmp.errorLevel;
        dynamicLoginRealm = cc_tmp.dynamicLoginRealm;
        documentTable = cc_tmp.documentTable;
        docAccessPath = cc_tmp.docAccessPath;
        docAccessProcedure = cc_tmp.docAccessProcedure;
        defaultPage = cc_tmp.defaultPage;
        alwaysCallDefaultPage = cc_tmp.alwaysCallDefaultPage;
        customAuthentication = cc_tmp.customAuthentication;
        factory = cc_tmp.factory;
        status = cc_tmp.status;
        connectString = cc_tmp.connectString;
        clientCharset = cc_tmp.clientCharset;
        dbCharset = cc_tmp.dbCharset;
        type_owner = cc_tmp.type_owner;
        type_name = cc_tmp.type_name;
        type_subname = cc_tmp.type_subname;
        flexible_escape_char = cc_tmp.flexible_escape_char;
        xform_escape_char = cc_tmp.xform_escape_char;
        xform_param_name = cc_tmp.xform_param_name;
        proxyUser = cc_tmp.proxyUser;
    }
}
