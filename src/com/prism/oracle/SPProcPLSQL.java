/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 * <BR>
 * Modified: 3/Nov/2003 by <a href="mailto:jhking@airmail.net">Jason King</a> (JHK)<BR>
 * Changes : <UL><LI>Added get method to handle overloaded pl/sql functions</LI>
 *               <LI>Added add method to handle overloaded pl/sql functions</li>
 * </ul>
 * <BR>
 * Modified: 24/Mar/2005 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>fixed missing }</LI></UL>
 * <BR>
 */

package com.prism.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.prism.ConnInfo;
import com.prism.SPProc;

/**
 * This class implements a ConcreteProduct class of abtract factory patterm.
 * Define a product object (SPProcPLSQL represent a PLSQL Stored Procedure)
 * to be created by the corresponding concrete factory. Implements the AbstractProduct Interface (SPProc)
 */
public class SPProcPLSQL extends SPProc {
    Logger log = Logger.getLogger(SPProcPLSQL.class);

    /**
     * Create a concrete SPProc (8i and 7x). Find the Stored Procedure in the table all_arguments to get public definitios
     * If there are public Stored Procedures add this definition to the Hashtable
     * of the superclass, and store all overloaded ocurrence of the same StoreProcedure
     * @param conn ConnInfo
     * @param procname String
     * @param sqlconn Connection
     * @return SPProc
     * @throws SQLException 
     */
    public SPProc create(ConnInfo conn, String procname, Connection sqlconn) throws SQLException {
        if (log.isDebugEnabled())
          log.debug(".create overload for: '"+procname+"'");
        SPProcPLSQL plp = new SPProcPLSQL();
        CallableStatement css = sqlconn.prepareCall("BEGIN \n dbms_utility.name_resolve(?,1,?,?,?,?,?,?); \nEND;");
        css.setString(1, procname);
        css.registerOutParameter(2, Types.VARCHAR);
        css.registerOutParameter(3, Types.VARCHAR);
        css.registerOutParameter(4, Types.VARCHAR);
        css.registerOutParameter(5, Types.VARCHAR);
        css.registerOutParameter(6, Types.VARCHAR);
        css.registerOutParameter(7, Types.VARCHAR);
        css.execute();
        String owner = css.getString(2);
        String plpackage = css.getString(3);
        String plprocedure = css.getString(4);
        css.close();
        String columnNames = "argument_name, overload, data_type, type_owner, type_name, type_subname";
        if (plpackage == null) {
            throw new RuntimeException("procedure must be member of package");
        }
        PreparedStatement cs = sqlconn.prepareStatement("SELECT " + columnNames + " FROM all_arguments WHERE " +
                " owner = ? AND package_name = ? AND object_name = ? " + " ORDER BY overload,sequence");
        if (log.isDebugEnabled()) {
            log.debug("Resolving package.procedure call");
            log.debug("Excuting: SELECT " + columnNames + " FROM all_arguments WHERE " +
                    " owner = ? AND package_name = ? AND object_name = ? " + " ORDER BY overload,sequence");
            log.debug("With arg 1: "+owner);
            log.debug("With arg 2: "+plpackage);
            log.debug("With arg 3: "+plprocedure);
        }
        cs.setString(1, owner);
        cs.setString(2, plpackage);
        cs.setString(3, plprocedure);
        
        ResultSet rs = cs.executeQuery();
        String old_overload = "something";
        while (rs.next()) {
            String argument_name = rs.getString(1);
            String overload = rs.getString(2);
            if (overload == null) { // There is no overloading
                overload = "1";
            }
            if (!old_overload.equals(overload)) {
                plp.addProcedure(overload);
                old_overload = overload;
            }
            // if procedure has no argument, empty row is returned
            if (argument_name == null) {
              if (log.isDebugEnabled())
                log.debug("            overload: "+overload+" no argument");
               continue;
            }
            argument_name = argument_name.toLowerCase();
            String data_type = rs.getString(3);
            if ("PL/SQL TABLE".equals(data_type)) { // argument is ARRAY variable
                
                    String category = rs.getString(3).toUpperCase();
                    String type_owner = rs.getString(4);
                    String type_name = rs.getString(5);
                    String type_subname = rs.getString(6);
                    plp.add(overload, argument_name, type_owner + "." + type_name + "." + type_subname , category );
                   if (log.isDebugEnabled())
                     log.debug("            overload: "+overload + " arg: "+argument_name + " data_type: " + data_type + " type_name: " + type_owner + "." + type_name + "." + type_subname); 
                rs.next();
            } else { // argument is SCALAR variable
                plp.add(overload, argument_name, data_type , "SCALAR");
                if (log.isDebugEnabled())
                  log.debug("            overload: "+overload + " arg: "+argument_name + " data_type: " + data_type + " category: SCALAR");
            }
        }
        rs.close(); //don't wait for garbage collector
        cs.close(); //don't wait for garbage collector
        return plp;
    }

    /**
     * Customize get to handle pl/sql overloaded procedure names
     * 
     * @return the type name for this argument name 
     */
    public String get(String argumentName , int elementCnt) {
        int i = 1;
        String type;
        if ( log.isDebugEnabled() )
            log.debug("finding " + argumentName + " Elements: " + elementCnt);
        do {
            String overloadIndex = (new Integer(i)).toString();
            if ( log.isDebugEnabled() )
   				     log.debug("trying " + overloadIndex);
            Hashtable procedure = (Hashtable)procedures.get(overloadIndex);
            if (procedure == null) return null;
            type = (String)procedure.get(argumentName);
            /*					 
            *  multivalue elements map to a declared type e.g. owa_util.ident_arr,
            *  wsgl.typString240Table all of which contain (.).  Scalars like
            *  varchar2 and date don't contain (.).
            */
            if ( type != null ) {
                log.debug("Where's the period in " + type + " ( " + type.indexOf(".") + ") Elements("+elementCnt+")");
                if ( elementCnt > 1 && (type.indexOf(".") == -1) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug("scalar to multivalue rollback");
                    type = null ;
                }
               } // this was missing LXG
               if ( log.isDebugEnabled() )
                   log.debug("found " + type);
            }
            i++;
        } while (type == null);
        return type;
    }

    /**
     * add a new argument name for this procedure definition
     * for every overloaded procedure entry there are entrys for each argument name
     * argumentCategory is "PL/SQL Table" for multivalued items
     * Changed data part of procedure to be an array instead of simple String
     */
    public void add(String overloadIndex, String argumentName, String argumentType , String argumentCategory) {
        Hashtable procedure = (Hashtable)procedures.get(overloadIndex);
        Vector args = (Vector)arguments.get(overloadIndex);
				// String[] argsig = {argumentType, argumentCategory}; // LXG
        procedure.put(argumentName,  argumentType);
        args.addElement(argumentName);
        if ( log.isDebugEnabled() )   //JHK
            log.debug("into : " + overloadIndex + " arg_name: " +argumentName+ " inserted with type: " + argumentType + " category: " + argumentCategory) ;
    }
}
