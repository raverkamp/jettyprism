/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 * Modified: 3/Nov/2003 by <a href="mailto:jhking@airmail.net">Jason King</a> (JHK)<BR>
 * Changes : <UL><LI>Added logger reference</LI>
 * <LI>Converted System.out.printlns to log.debugs</LI>
 * <LI>Made Hashtable procedures protected, not private</LI>
 * <LI>Made Hashtable arguments protected, not private</LI>
 * </ul>
 * <BR>

 */

package com.prism;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;


public abstract class SPProc {
    private Logger log = Logger.getLogger(SPProc.class); //JHK

    protected Hashtable procedures; //JHK
    protected Hashtable arguments;  //JHK

    public SPProc() {
        procedures = new Hashtable();
        arguments = new Hashtable();
    }

    /** create a new entry for this procedure definition every procedure definition is knew by the overload index value */
    public void addProcedure(String index) {
        procedures.put(index, new Hashtable());
        arguments.put(index, new Vector());
    }

    /**
     * return all overload definitions for this procedure every definition of a procedure is a Vector with the arguments names
     */
    public Enumeration getAll() {
        return arguments.elements();
    }

    /**
     * gets the argument list for this overload version of the Stored Procedure
     * the argument list is a Hashtable with argument_name / type_name values
     */
    public Vector getArgumentList(String overloadIndex) {
        return (Vector)arguments.get(overloadIndex);
    }

    /**
     * add a new argument name for this procedure definition
     * for every overloaded procedure entry there are entrys for each argument name
     */
    public void add(String overloadIndex, String argumentName, String argumentType) {
        Hashtable procedure = (Hashtable)procedures.get(overloadIndex);
        Vector args = (Vector)arguments.get(overloadIndex);
        procedure.put(argumentName, argumentType);
        args.addElement(argumentName);
				if ( log.isDebugEnabled() )   //JHK
				   log.debug("into : " + overloadIndex + " arg_name: " +argumentName+ " inserted with type: " + argumentType);
    }

    /** return the type name for this argument name */
    public String get(String argumentName) {
        int i = 1;
        String type;
				if ( log.isDebugEnabled() )  //JHK
   				 log.debug("finding " + argumentName);
        do {
            String overloadIndex = (new Integer(i)).toString();
            if ( log.isDebugEnabled() )  //JHK
   				     log.debug("trying " + overloadIndex);
            Hashtable procedure = (Hashtable)procedures.get(overloadIndex);
            if (procedure == null) return null;
            type = (String)procedure.get(argumentName);
            if ( log.isDebugEnabled() )  //JHK
   				     log.debug("found " + type);
            i++;
        } while (type == null);
        return type;
    }

    // must be redefined by SubClass (ConcreteProduct of Abstract Factory Patterns.
    public abstract SPProc create(ConnInfo conn, String procname, Connection sqlconn) throws SQLException;
}
