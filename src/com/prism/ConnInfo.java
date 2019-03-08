/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spinat.jettyprism.Configuration;

/**
 * This class stores the connection information, such as user, password, and so
 * on This class has the factory instance according to the current Database.<BR>
 * <BR>
 * Modified: 18/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a>
 * (JHK)<BR>
 * Changes : <UL><LI>More logging</LI>
 * <LI>connectString default now obvious</LI></UL>
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 * <BR>
 * Modified: 2/Feb/2004 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>JavDoc cleanup</LI></UL>
 */
public class ConnInfo {

    private static final Logger log = LogManager.getLogger();

    public java.lang.String connectString;
    public java.lang.String usr;
    public java.lang.String pass;
    public java.lang.String connAlias;
    public int errorLevel;
    public java.lang.String dynamicLoginRealm;
    public java.lang.String documentTable;
    public java.lang.String docAccessPath;
    public java.lang.String docAccessProcedure;
    public java.lang.String customAuthentication;
    public boolean proxyUser;
    // Customized values for ! and ^ characters
    public java.lang.String flexible_escape_char;
    public java.lang.String xform_escape_char;
    public java.lang.String xform_param_name;

    /**
     * This method makes a ConnInfo objects through the aliasdef Connection
     * alias is retrieved from the URL information Example: plsql => the URL
     * http://server:port/servlet/plsql/xx.yy?arg=val Example: www_dba => the
     * URL http://server:port/servlet/www_dba/xx.yy?arg=val The aliasdef works
     * with multiples zones definition in servlet config The servlet zones args
     * must be pointed to the same prism.config and Prism.jar See servlet.config
     * for more details Params: global.alias=plsql xml demo servlet xmld This
     * method retrieves the factory that has to be created (the database to be
     * used) from the config file (prism.config)
     *
     * @param aliasdef String
     * @throws Exception
     */
    public ConnInfo(Configuration config, String aliasdef) {
        //JHK who are we initializing
        if (log.isDebugEnabled()) {
            log.debug("Initializing connection: " + aliasdef);
        }
        connAlias = aliasdef;
        usr
                = config.getProperty("dbusername", "", "DAD_" + aliasdef);
        pass
                = config.getProperty("dbpassword", "", "DAD_" + aliasdef);
        dynamicLoginRealm
                = config.getProperty("dynamicLoginRealm", aliasdef, "DAD_" + aliasdef);
        documentTable
                = config.getProperty("documentTable", "owa_public.wpg_document", "DAD_" + aliasdef);
        docAccessPath
                = config.getProperty("docAccessPath", "docs", "DAD_" + aliasdef);
        docAccessProcedure
                = config.getProperty("docAccessProcedure", "owa_public.wpg_testdoc.process_download", "DAD_" + aliasdef);
        customAuthentication
                = config.getProperty("customAuthentication", "none", "DAD_" + aliasdef);
        connectString
                = config.getProperty("connectString", "****no_connect_string***", "DAD_" + aliasdef); //JHK
        errorLevel
                = config.getIntProperty("errorLevel", 0, "DAD_" + aliasdef);
        flexible_escape_char
                = config.getProperty("flexibleEscapeChar", "!", "DAD_" + aliasdef);
        xform_escape_char
                = config.getProperty("xformEscapeChar", "^", "DAD_" + aliasdef);
        xform_param_name
                = config.getProperty("xformParamName", "post_xml", "DAD_" + aliasdef);
        proxyUser = config.getBooleanProperty("useProxyUser", false, "DAD_" + aliasdef);
        if (log.isDebugEnabled()) {
            log.debug("User: " + usr);
            if (pass == null || pass.isEmpty()) {
                log.debug("pass:                  " + "not given");
            } else {
                log.debug("pass:                  " + "is given");
            }
            log.debug("dynamicLoginRealm:     " + dynamicLoginRealm);
            log.debug("documentTable:         " + documentTable);
            log.debug("docAccessPath:         " + docAccessPath);
            log.debug("docAccessProcedure:    " + docAccessProcedure);
            log.debug("customAuthentication:  " + customAuthentication);
            log.debug("connectString:         " + connectString);
            log.debug("errorLevel:            " + errorLevel);
            log.debug("flexibleEscapeChar:    " + flexible_escape_char);
            log.debug("xformEscapeChar:       " + xform_escape_char);
            log.debug("xformParamName:        " + xform_param_name);
            log.debug("useProxyUser:          " + proxyUser);
        }
    }
}
