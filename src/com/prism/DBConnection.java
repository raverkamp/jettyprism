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
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import spinat.jettyprism.Configuration;

import com.prism.utils.FlexibleRequest;
import com.prism.utils.FlexibleRequestCompact;
import com.prism.utils.OraUtil;
import java.io.StringReader;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import java.util.Properties;
import oracle.jdbc.OracleCallableStatement;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.sql.CLOB;

/**
 * This class plays the role of AbstractProduct of Abstract Factory pattern.
 * Declares an interface for a type of product object (create method). Also, it
 * is an AbstractClass of Template Method pattern. Defines abstract "primitive
 * operations" that concrete subclasses define to implement step of algorithm
 * (doCall & getGeneratedStream)<BR>
 * <BR>
 * Modified: 21/Mar/2005 by <a href="mailto:jhking@airmail.net">Jason King</a>
 * (JHK)<BR>
 * Changes : <UL><LI>Added debug messages</LI>
 * </UL>
 *
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
public class DBConnection {

    Logger log = Logger.getLogger(DBConnection.class);

    // From Package definition OWA_SEC
    /* PL/SQL Agent's authorization schemes                            */
    // NO_CHECK    constant integer := 1;
    // GLOBAL      constant integer := 2;
    // PER_PACKAGE constant integer := 3;
    //static public final int NO_CHECK = 1; /* no authorization check             */
    //static public final int GLOBAL = 2; /* global check by a single procedure */
    //static public final int PER_PACKAGE = 3; /* use auth procedure in each package */
    //static public final int CUSTOM = 4;
    public OracleConnection sqlconn;
    public ConnInfo connInfo;

    final private boolean flexibleCompact = true;

    protected java.lang.String toolkitVersion;

    protected java.lang.String nlsLanguage = null;

    protected java.lang.String nlsTerritory = null;

    protected java.lang.String includeList;

    private final int behavior;
    private final int maxUploadSize;

    static final int MAX_PL_LINES = 127;

    /**
     * Template method calls<BR>
     * 1. resetPackages without parameters<BR>
     * 2. setCGIVars with the req of doCall, name, pass<BR>
     * 3. getAuthMode without parameters returns a mode<BR>
     * 4. doAuthorize with mode, and conninfo,getPackage<BR>
     * 4.1. getRealm<BR>
     * 5. doIt with the req of doCall and the servletname<BR>
     *
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
    public void doCall(ProcedureCache procedureCache, HttpServletRequest req) throws
            SQLException,
            NotAuthorizedException,
            UnsupportedEncodingException,
            IOException,
            ProcedureNotFoundException,
            ExecutionException {
        if (log.isDebugEnabled()) {
            log.debug(".doCall entered.");
        }
        if (connInfo.proxyUser) { // Sanity checks
            String proxyUserName = (req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : req.getRemoteUser();
            if (proxyUserName == null || proxyUserName.length() == 0) {
                String realms = getRealm();
                throw new NotAuthorizedException(realms);
            }
            if (((OracleConnection) sqlconn).isProxySession()) // may be was a failed page, close the session first
            {
                ((OracleConnection) sqlconn).close(((OracleConnection) sqlconn).PROXY_SESSION);
            }
            Properties proxyUserInfo = new Properties();
            proxyUserInfo.put("PROXY_USER_NAME", proxyUserName);
            ((OracleConnection) sqlconn).openProxySession(OracleConnection.PROXYTYPE_USER_NAME, proxyUserInfo);
            log.debug(".doCall - Proxy user: " + proxyUserName);
            //connectedUsr = proxyUserName;
        }
        ServletCommand sc = getServletCommand(req);
        String ppackage = sc.package_;
        
        resetPackages();

        setCGIVars(req, "", ""); // the empty strings are user/pwd parameter
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
        } // If neither value is null, choose the longer value
        else if (type1 != null && type2 != null) {
            type = (type1.length() > type2.length() ? type1 : type2);
        }
        if (type != null && type.toLowerCase().startsWith("multipart/form-data")) {
            // Handle multipart post, sent it as binary stream in a BLOB argument
            UploadRequest multi = new UploadRequest(req, this, this.maxUploadSize);

            // Calls the stored procedures with the new request
            doIt(procedureCache, multi, sc.package_ + "." + sc.procedure);
        } else if (sc.escapeChar.equals(connInfo.flexible_escape_char)) // Calls the stored procedures with the wrapper request
        {
            if (flexibleCompact) {
                doIt(procedureCache, new FlexibleRequestCompact(req), sc.package_ + "." + sc.procedure);
            } else {
                doIt(procedureCache, new FlexibleRequest(req), sc.package_ + "." + sc.procedure);
            }
        } else if (sc.escapeChar.equals(connInfo.xform_escape_char)) // Calls the stored procedures with the wrapper request
        {
            throw new RuntimeException("not implemented: XFormsRequest");
        } else if (sc.escapeChar != null && !sc.escapeChar.isEmpty()) {
            throw new RuntimeException("unknown escape char: " + sc.escapeChar);
        } else // Calls the stored procedures with the actual request
        {
            doIt(procedureCache, req, sc.package_ + "." + sc.procedure);
        }
        if (log.isDebugEnabled()) {
            log.debug(".doCall exited.");
        }
    }

    /**
     * Concrete operation of Template Method pattern. Reset packages state if
     * the connection is statefull
     *
     */
    public void resetPackages() throws SQLException {
        try (CallableStatement cs = sqlconn.prepareCall("BEGIN dbms_session.reset_package; END;")) {
            cs.execute();
        }
    }

    /**
     * Concrete opeejration of Template Method pattern. Call to the concrete
     * Stored Procedure in the DB
     *
     * @throws java.sql.SQLException
     * @throws java.io.UnsupportedEncodingException
     */
    public void doIt(ProcedureCache procedureCache,
            HttpServletRequest req,
            String procedureName) throws SQLException,
            UnsupportedEncodingException,
            ProcedureNotFoundException,
            ExecutionException {
        if (log.isDebugEnabled()) {
            log.debug(".doIt entered.");
        }

        final OraUtil.ResolvedProcedure rp = OraUtil.resolveProcedure(sqlconn, procedureName);
        if (rp == null) {
            throw new ProcedureNotFoundException("package/procedure not found: " + procedureName);
        }
        if (rp.package_ == null) {
            throw new ProcedureNotFoundException("no calling of stand alone procedures");
        }
        String realProcedureName = rp.owner + "." + rp.package_ + "." + rp.procedure;
        String realPackage = rp.owner + "." + rp.package_;
        StringTokenizer st = new StringTokenizer(includeList, " ");
        if (log.isDebugEnabled()) {
            log.debug(".doIt - Real Procedure Name: '" + realProcedureName + "'");
        }
        boolean found = false;
        while (st.hasMoreElements()) {
            if (realPackage.equals(st.nextToken())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ProcedureNotFoundException("Package not allowed: " + realPackage);
        }

        // parse all FORM input parameters and arrays set as PL/SQL arrays
        // Calling with constants - no prepared calls
        // Handling Case Insensitive args in PL/SQL and owa_image.point
        // Eg:
        // http://server:port/servlet/plsql/example.print?a=b
        // http://server:port/servlet/plsql/example.print?A=b
        // make the same call to the procedure example.print('b')
        // PLSQL runtime choose the correct procedure to call
        // Work with overload procedure and in/out parameters to.
        // Eg:
        // http://server:port/servlet/plsql/example.print?A=b
        // http://server:port/servlet/plsql/example.print?A=b&c=d
        // Build procedure call
        StringBuilder command = new StringBuilder(procedureName + "(");
        // Main calling command
        StringBuilder decvar = new StringBuilder("DECLARE dummy integer; \n");
        //we will declare array variables here
        StringBuilder setvar = new StringBuilder("BEGIN \n");
        ArrayList<String> params = new ArrayList<>();
        ArrayList<Boolean> isClob = new ArrayList<>();
        //we will set array variables here
        int foundcount = 0;
        SPProc plp
                = procedureCache.get(connInfo, rp, sqlconn);
        //JHK, to use overloaded get
        // Build procedure call parameter by parameter
        Enumeration real_args = req.getParameterNames();
        while (real_args.hasMoreElements()) {
            String name_args = (String) real_args.nextElement();
            String multi_vals[] = req.getParameterValues(name_args);
            if (log.isDebugEnabled()) {
                log.debug("argument: " + name_args + " elements: " + multi_vals.length);
            }
            final String argumentType;
            if (name_args.indexOf(".") > 0) {
                argumentType
                        = plp.get(name_args.substring(0, name_args.indexOf("."))
                                .toLowerCase(), multi_vals.length);
                //JHK
            } else {
                argumentType
                        = plp.get(name_args.toLowerCase(), multi_vals.length);
            }
            //JHK
            if (argumentType == null) {
                log
                        .warn("Warning: argument " + name_args + " not in procedure description "
                                + procedureName);
                throw new ProcedureNotFoundException(procedureName
                        + ": MANY PROCEDURES MATCH NAME, BUT NONE MATCHES SIGNATURE (parameter name '"
                        + name_args + "')");
            }
            if (log.isDebugEnabled()) {
                log.debug("Arg. name:" + name_args + " found type: " + argumentType);
            }
            if (argumentType.indexOf(".") > 0) {
                // ARRAY variable syntax: owner.type.subtype
                if (name_args.indexOf(".") > 0) {
                    // must be owa_image.point
                    if (name_args.toLowerCase().endsWith(".x")) {
                        // Use only name.x definition and ignore name.y
                        // handle owa_image.point data type
                        name_args
                                = name_args.substring(0, name_args.indexOf("."));
                        decvar.append("dbp$_").append(foundcount)
                                .append(" owa_image.point;\n");
                        String val_x = req.getParameter(name_args + ".x");
                        String val_y = req.getParameter(name_args + ".y");
                        // the owa_image.point data type is a array of varchar index by binary integer
                        // Position 1 is args.x value
                        // Position 2 is args.y value
                        String s
                                = new String(val_x.getBytes(connInfo.clientCharset));

                        setvar.append("dbp$_").append(foundcount)
                                .append("(1):='").append(s.replace("'", "''")).append("'; ");
                        if (log.isDebugEnabled()) {
                            log.debug("point " + name_args + ".x=" + val_x);
                        }
                        s = new String(val_y.getBytes(connInfo.clientCharset));
                        setvar.append("dbp$_").append(foundcount)
                                .append("(2):='").append(s.replace("'", "''")).append("'; ");
                        if (log.isDebugEnabled()) {
                            log.debug("point " + name_args + ".y=" + val_y);
                        }
                        command.append(name_args).append("=>dbp$_")
                                .append(foundcount).append(",");
                    } else {
                        // Skip .y definition
                        continue;
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log
                                .debug(name_args + " argumentType =" + argumentType);
                    }
                    for (int i = 0; i < multi_vals.length; i++) {
                        String s = multi_vals[i];

                        setvar.append("dbp$_").append(foundcount).append("(")
                                .append((i + 1)).append("):='").append(s.replace("'", "''"))
                                .append("'; ");
                    }
                    // end for make array variable
                    command.append(name_args).append("=>dbp$_")
                            .append(foundcount).append(",");
                    // Oracle 10g replace SYS by PUBLIC when object where installed on sys schema and granted to public.
                    // Remove PUBLIC and use short version (package.type) for the argument type.
                    decvar.append("dbp$_").append(foundcount).append(" ");
                    decvar.append((argumentType.startsWith("PUBLIC.")
                            ? argumentType.substring(7)
                            : argumentType));
                    decvar.append(";\n");
                }
            } else { // rav: no "." in type name
                // otherwise, must be scalar type or cast to scalar
                String s;
                if (name_args.indexOf(".") > 0) {
                    if (name_args.toLowerCase().endsWith(".x")) {
                        // Use only name.x definition and ignore name.y
                        s = req.getParameter(name_args);
                        name_args
                                = name_args.substring(0, name_args.indexOf("."));
                        if (log.isDebugEnabled()) {
                            log.debug("Casting from owa_image.point to varchar2");
                        }
                    } else {
                        // Skip .y definition
                        continue;
                    }
                } else if (multi_vals != null) {
                    s = multi_vals[0];
                    if (log.isDebugEnabled()) {
                        log.debug("Casting from owa_ident.arr to varchar2");
                    }
                } else {
                    s = req.getParameter(name_args);
                    if (log.isDebugEnabled()) {
                        log
                                .debug("single " + name_args + "=" + req.getParameter(name_args));
                    }
                }
                if ("CLOB".equalsIgnoreCase(argumentType)) {
                    params.add(s);
                    isClob.add(true);
                    command.append(name_args).append("=>?,");
                } else {
                    int slen = s.length();
                    if (slen > 32767) {
                        throw new SQLException("Argument length of '"
                                + name_args
                                + "' is longer than 32767");
                    }
                    params.add(s);
                    isClob.add(false);
                    command.append(name_args).append("=>?,");
                }
            }
            // end if muti valued args
            foundcount++;
        }
        command
                = new StringBuilder(decvar.toString() + setvar.toString() + command
                        .toString()
                        .substring(0, command.length() - 1));
        if (foundcount == 0) {
            command.append("; END;");
        } else {
            command.append("); END;");
        }
        if (log.isInfoEnabled()) {
            log.info(".doIt command:\n" + command);
        }
        // Exec procedure in DB

        ArrayList clobPassed = new ArrayList();
        try (CallableStatement cs = sqlconn.prepareCall(command.toString())) {
            for (int i = 0; i < params.size(); i++) {
                if (isClob.get(i)) {
                    CLOB tmpClob
                            = CLOB.createTemporary(this.sqlconn, false, CLOB.DURATION_SESSION);
                    clobPassed.add(tmpClob);
                    try (Writer iow = tmpClob.setCharacterStream(1L)) {
                        iow.write(params.get(i)); //.toCharArray());
                    } catch (IOException ioe) {
                        throw new SQLException("DBConnPLSQL: Failed to write temporary CLOB:\n"
                                + ioe.getMessage());
                    }
                    cs.setClob(i + 1, tmpClob);
                } else {
                    cs.setString(i + 1, params.get(i));
                }
            }
            cs.execute();
        } catch (SQLException e) {
            throw new ExecutionException("PLSQL Adapter - PLSQL Error\n"
                    + e.getMessage() + MsgArgumentCallError(req));
        } finally {
            for (Object clobPassed1 : clobPassed) {
                CLOB tmpClob = (CLOB) clobPassed1;
                if (tmpClob != null) {
                    CLOB.freeTemporary(tmpClob);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(".doIt exited.");
        }
    }

    /**
     * Create a concrete DBConnection (DBConnPLSQL). Find extra properties
     * attributes of this connection and return a concrete connection object.
     */
    public DBConnection(Configuration properties, ConnInfo cc, OracleConnection sqlcon) throws SQLException {

        this.sqlconn = sqlcon;
        this.connInfo = cc;
        String currentSchema = properties.getProperty("current_schema", "", "DAD_" + cc.connAlias);
        if (!currentSchema.equals("")) {
            try (Statement stm = this.sqlconn.createStatement()) {
                stm.execute("alter session set current_schema=" + currentSchema);
            }
        }

        this.toolkitVersion
                = properties.getProperty("toolkit", "4x", "DAD_" + cc.connAlias);
        this.includeList
                = properties.getProperty("allowed_packages", "#",
                        "DAD_" + cc.connAlias);
        if (this.includeList.equals("#")) {
            log.error("no allowed_packages property given for DAD " + cc.connAlias + " set property " + "DAD_" + cc.connAlias + ".allowed_packages");
            throw new Error("no allowed_packages given for DAD " + cc.connAlias);
        }
        this.behavior = properties.getIntProperty("behavior", 0);
        this.maxUploadSize = properties.getIntProperty("maxUploadSize", 8388608);
        String nlsSetting
                = properties.getProperty("nls_lang", null, "DAD_" + cc.connAlias);
        if (nlsSetting != null) {
            try {
                String langSetting
                        = nlsSetting.substring(0, nlsSetting.indexOf("."));
                this.nlsLanguage
                        = langSetting.substring(0, langSetting.indexOf("_"));
                this.nlsTerritory
                        = langSetting.substring(langSetting.indexOf("_") + 1);
            } catch (IndexOutOfBoundsException e) {
                log.warn("Incorrect syntax on nls_lang parameter: " + nlsSetting);
            }
        }
    }

    /**
     * Concrete operation of Template Method pattern. Pass CGI Enviroment to the
     * DB
     */
    public void setCGIVars(HttpServletRequest req, String name,
            String pass) throws SQLException {
        //  OracleCallableStatement cs = null;

        StringBuffer command = new StringBuffer("alter session set ");
        if (this.nlsLanguage != null) {
            command.append("NLS_LANGUAGE='").append(this.nlsLanguage).append("' NLS_TERRITORY='").append(this.nlsTerritory).append("' NLS_LENGTH_SEMANTICS=CHAR");
        } else {
            command.append("NLS_LENGTH_SEMANTICS=CHAR");
        }
        try (OracleCallableStatement cs = (OracleCallableStatement) sqlconn.prepareCall(command.toString())) {
            cs.execute();
            if (log.isDebugEnabled()) {
                log.debug(".setCGIVars - " + command + " for DAD: "
                        + this.connInfo.connAlias + " - Done.");
            }
        } catch (SQLException sqe) {
            log.warn("Warning, can't alter session: ", sqe);
        }
        command
                = new StringBuffer("DECLARE var_val owa.vc_arr;\n");
        ArrayList<String> params = new ArrayList<>();
        command.append("  var_name owa.vc_arr;\n");
        command.append("  dummy_num_vals integer; \nBEGIN ");
        // Get dummy val, force to execute init code of the package
        // if not execute this call the global vars of packages of owa_init
        // and owa_cookie have null vals
        command.append("dummy_num_vals := owa.initialize;\n");
        String hostaddr = req.getRemoteAddr();
        if (log.isDebugEnabled()) {
            log.debug("hostaddr: " + hostaddr);
        }
        // fixme : I do not care about the IP address yet
        //          if working with localhost we might get an IP V6 address.
        if (hostaddr.contains(":")) {
            hostaddr = "0.0.0.0";
        }
        StringTokenizer st = new StringTokenizer(hostaddr, ".");
        for (int i = 1; st.hasMoreElements(); i++) {
            command.append("   owa.ip_address(" + i + "):=?;\n");
            params.add((String) st.nextElement());
        }
        // Set the owa.cgi_var_val and owa.cgi_var_name used by owa package
        // for example owa.get_service_path use the CGI var SCRIPT_NAME
        command.append("   owa.user_id:=?;\n");
        params.add(name);
        command.append("   owa.password:=?;\n");
        params.add(pass);
        command.append("   owa.hostname:=?;\n");
        params.add(req.getRemoteHost());

        if ("3x".equalsIgnoreCase(toolkitVersion)) {
            command.append("   htp.rows_in:=0; htp.rows_out:=0;\n");
        }
        CgiVars env = new CgiVars(req, this.connInfo, name, pass);

        command.append("   owa.init_cgi_env(?,?,?);");

        if ("4x".equalsIgnoreCase(toolkitVersion)) {
            command.append("  htp.init;\n");
        }
        // get authorization mode
        command.append("END;");

        log.debug(command.toString());
        try (OracleCallableStatement cs = (OracleCallableStatement) sqlconn.prepareCall(command.toString())) {
            for (int i = 0; i < params.size(); i++) {
                cs.setString(i + 1, params.get(i));
            }
            int k = params.size();
            int len = env.names.size();
            cs.setInt(k + 1, env.names.size());
            cs.setPlsqlIndexTable(k + 2, env.names.toArray(new String[0]), len, len, OracleTypes.VARCHAR, 200);
            cs.setPlsqlIndexTable(k + 3, env.values.toArray(new String[0]), len, len, OracleTypes.VARCHAR, 2048);

            for (int i = 0; i < params.size(); i++) {
                cs.setString(i + 1, params.get(i));
            }
            cs.execute();
        }
    }

    /**
     * Concrete operation of Template Method pattern. authMode specifies whether
     * to enable custom authentication. If specified, the application
     * authenticates users in its own level and not within the database level.
     * This parameter can be set to one of the following values : none (Default)
     * global custom perPackage
     */
    public int doAuthorize(String authMode,
            String ppackage) throws SQLException {
        int authStatus;
        if (authMode.equalsIgnoreCase("none")) {
            // Authorization none
            // always return true
            authStatus = 1;
        } else {
            CallableStatement cs = null;
            StringBuffer command
                    = new StringBuffer("DECLARE\n" + "FUNCTION b2n(b BOOLEAN) RETURN NUMBER IS\n"
                            + "BEGIN\n"
                            + "IF (b) THEN\n"
                            + "  RETURN '1';\n"
                            + " END IF;\n"
                            + " RETURN '0';\n"
                            + " END;\n" + "BEGIN\n");
            if (authMode.equalsIgnoreCase("global")) // Authorization global
            // In global authentication all executes are validates by the owa_init.authorize function
            {
                if (toolkitVersion.equalsIgnoreCase("3x")) {
                    command.append("? := b2n(owa_init.authorize); END;");
                } else {
                    command
                            .append("? := b2n(owa_public.owa_custom.authorize); END;");
                }
            } else if (authMode.equalsIgnoreCase("custom")) {
                if (toolkitVersion.equalsIgnoreCase("3x")) {
                    throw new SQLException("DBConnPLSQL: custom authentication is not valid for toolkit 3x");
                } else // In custom authentication all executes are validates by the owa_custom.authorize function
                // in the user schema or in owa_public schema
                {
                    command.append("? := b2n(owa_custom.authorize); END;");
                }
            } else // Authorization per package
            if (ppackage.equals("")) // if anonymous procedure, anonymous function authorize
            {
                command.append("? := b2n(authorize); END;");
            } else // else call to function authorize on this package
            {
                command.append("? := b2n(").append(ppackage)
                        .append(".authorize); END;");
            }
            //System.out.println("cm="+command);
            try {
                cs = sqlconn.prepareCall(command.toString());
                cs.registerOutParameter(1, Types.INTEGER);
                cs.execute();
                // Check if password is valid
                authStatus = cs.getInt(1);
                cs.close();
                //don't wait for garbage collector
            } catch (SQLException e) {
                if (cs != null) {
                    cs.close();
                }
                throw new SQLException("DBConnPLSQL: Can't execute authorize function for mode = "
                        + authMode + "\n\n" + e.getMessage());
            }
        }
        return authStatus;
    }

    /**
     * Concrete operation of Template Method pattern. Return the realms sent
     * back to the browser if the authorization fail This realms is set by
     * calling to owa_sec.set_protection_realm procedure
     */
    public String getRealm() throws SQLException {
        // if the autorization fail get the realm from the package owa_sec
        // The realm is set by the use owa_sec.set_protection_realm
        // A good place for call the owa_sec.set_protection_realm is in the function authorize
        // Ej
        // package body example is
        // function authorize return boolean is
        // begin
        //    owa_sec.set_protection_realm('Sample App.');
        //    if (owa_sec.get_user_id=="marcelo") then
        //        return true;
        //    else
        //        return false;
        //    end if;
        // end authorize;
        // end example;
        CallableStatement cs
                = sqlconn.prepareCall("BEGIN \n ? := owa.protection_realm; \nEND;");
        cs.registerOutParameter(1, Types.VARCHAR);
        cs.execute();
        // Get Protection realm
        String Realm = cs.getString(1);
        cs.close();
        //don't wait for garbage collector
        return Realm;
    }

    /**
     * Concrete operation of Template Method pattern. Return generated page as
     * StringReader
     */
    public // LXG: remove exception UnsupportedEncodingException since it is not thrown
            // public StringReader getGeneratedStream() throws SQLException, UnsupportedEncodingException {
            Content getGeneratedStream(HttpServletRequest req) throws SQLException {
        DownloadRequest download = null;
        Content generatedContent = new Content();

        String s = getDataBlock();

        generatedContent.setPage(new StringReader(s));
        if (log.isDebugEnabled()) {
            log.debug("buff=" + s);
        }

        download = new DownloadRequest(req, this);
        if (download != null && download.isFileDownload()) {
            generatedContent.setInputStream(download.getStream(download.getDownloadInfo()));
        }

        return generatedContent;
    }

    /**
     * Template method calls<BR>
     * 1. commit the connection if not part of transaction 1. close a proxy
     * connection if is using Oracle proxy user support
     *
     * @throws Exception
     */
    public void releasePage() {

        // if the transaction is not enable, make the modifications.
        if (log.isDebugEnabled()) {
            log.debug("Commit normal connection");
        }
        try {
            sqlconn.commit();
        } catch (SQLException e) {
            log.warn(".releasePage - exception on commit due: ", e);
        } finally {
            try {
                sqlconn.close();
            } catch (SQLException s) {
                log.warn(".releasePage - exception closing proxy session: ", s);
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
     * This method is similar to doCall but has a HttpServletResponse object to
     * directly sent to the output stream the downloaded document, bypassing the
     * call of getGeneratedPage to improve performance
     *
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
    public void doDownload(HttpServletRequest req, HttpServletResponse res, String usr, String pass) throws SQLException, NotAuthorizedException, UnsupportedEncodingException, IOException, ProcedureNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug(".doDownload entered.");
        }
        String connectedUsr = usr;
        if (connInfo.proxyUser) { // Sanity checks
            String proxyUserName = (req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : req.getRemoteUser();
            if (proxyUserName == null || proxyUserName.length() == 0) {
                String realms = getRealm();
                throw new NotAuthorizedException(realms);
            }
            Properties proxyUserInfo = new Properties();
            proxyUserInfo.put("PROXY_USER_NAME", proxyUserName);
            if (((OracleConnection) sqlconn).isProxySession()) // may be was a failed download
            {
                ((OracleConnection) sqlconn).close(((OracleConnection) sqlconn).PROXY_SESSION);
            }
            ((OracleConnection) sqlconn).openProxySession(OracleConnection.PROXYTYPE_USER_NAME, proxyUserInfo);
            log.debug(".doDownload - Proxy user: " + proxyUserName);
            connectedUsr = proxyUserName;
        }
        String ppackage = getServletCommand(req).package_;

        resetPackages();

        setCGIVars(req, connectedUsr, pass);
        int authStatus = doAuthorize(connInfo.customAuthentication, ppackage);
        if (authStatus != 1) {
            String realms = getRealm();
            throw new NotAuthorizedException(realms);
        }
        DownloadRequest downloadRequest = new DownloadRequest(req, this);
        downloadRequest.doDownloadFromDB(res);
    }
    
     /**
     * Returns the Servlet command  stringfrom the URL Ej:
     * http://server:port/servlet/demo/pkg.sp?arg1=val1&arg2=val2 return pkg.sp
     * i.e. everything in the path before the last /, no query params this can
     * include the escape char
     * excluded is the DAD
     * @param req HttpServletRequest
     * @return String
     */
    private String getServletCommandString(HttpServletRequest req) {
        final String cmdString;
        if (connInfo.alwaysCallDefaultPage) {
            return connInfo.defaultPage;
        }
        final String tail;
        if (req.getPathInfo() == null || req.getPathInfo().isEmpty()) {
            tail = req.getServletPath();
        } else {
            tail = req.getPathInfo();
        }
        if (tail.contains("/")) {
            cmdString = tail.substring(tail.lastIndexOf('/') + 1);
        } else {
            cmdString = tail;
        }
        if (log.isDebugEnabled()) {
            log.debug("servletname = " + cmdString);
            log.debug("servletpath = " + req.getServletPath());
            log.debug("pathInfo = " + req.getPathInfo());
        }

        return cmdString;
    }


    private static class ServletCommand {

        public final String package_;
        public final String procedure;
        public final String escapeChar;

        public ServletCommand(String escapeChar, String package_, String procedure) {
            this.escapeChar = escapeChar;
            this.package_ = package_;
            this.procedure = procedure;
        }
    }

    private ServletCommand getServletCommand(HttpServletRequest req) throws ProcedureNotFoundException {
        String s = getServletCommandString(req);
        if (s.length() < 3) {
            throw new ProcedureNotFoundException("this is not a procedure: " + s);
        }
        final String escapeChar;
        final String rest;
        if (!(Character.isAlphabetic(s.charAt(0)) || Character.isDigit(s.charAt(0)))) {
            escapeChar = s.substring(0, 1);
            rest = s.substring(1);
        } else {
            escapeChar = "";
            rest = s;
        }

        int p1 = rest.lastIndexOf('.');
        if (p1 < 0 || p1 != rest.indexOf('.')) {
            throw new ProcedureNotFoundException("this is not a procedure: " + s);
        }
        return new ServletCommand(escapeChar, rest.substring(0, p1), rest.substring(p1 + 1));
    }


    /**
     * Format the Error Message that will be returned to the browser when an
     * error happens
     *
     * @param req HttpServletRequest
     * @return String
     * @throws UnsupportedEncodingException
     */
    public String MsgArgumentCallError(HttpServletRequest req) throws UnsupportedEncodingException {
        StringBuffer text_error = new StringBuffer();
        text_error.append("\n\n\n While try to execute ").append(getServletCommandString(req));
        text_error.append("\n with args\n");
        Enumeration real_args = req.getParameterNames();
        while (real_args.hasMoreElements()) {
            String name_args = (String) real_args.nextElement();
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
                name_args = (String) real_args.nextElement();
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
     * return response form DB this is done by:
     *
     * Fetch a block of data from the OWA and return it as a StringBuffer; size
     * of each piece of generated page is 128x256 bytes change this value
     * according to max size of generated page and HTBUF_LEN HTBUF_LEN = 256 (in
     * htp public spec)
     */
    private String getDataBlock() throws SQLException {

        String s_GetPageSql
                = "declare nlns number;\n"
                + "  buf_t varchar2(32767);\n"
                + "  lines htp.htbuf_arr;\n"
                + "begin\n"
                + "  nlns := ?;\n" // the maximum of lines
                + "  OWA.GET_PAGE(lines, nlns);\n"
                + "  if (nlns < 1) then\n"
                + "    buf_t := null;\n"
                + "  else \n"
                + "    for i in 1..nlns loop\n"
                + "      buf_t:=buf_t||lines(i);\n"
                + "    end loop;\n"
                + "  end if;\n"
                + "  ? := buf_t;\n"
                + "  ? := nlns;\n"
                + "end;";

        try (CallableStatement cs = sqlconn.prepareCall(s_GetPageSql)) {

            cs.setInt(1, MAX_PL_LINES); // 127*256 = 32768 
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.registerOutParameter(3, Types.BIGINT);
            StringBuilder sb = new StringBuilder();
            while (true) {
                cs.execute();
                String s = cs.getString(2);
                int linecount = cs.getInt(3);
                if (linecount < 1) {
                    break;
                }
                sb.append(s);
                if (linecount < MAX_PL_LINES) {
                    break;
                }
            }
            return sb.toString();
        }
    }

}
