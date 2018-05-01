/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * This class stores the Stored Procedures information called to increase
 * performance in the following calls.
 */
public class ProcedureCache {

    private boolean shouldCache = false;

    private HashMap<String, SPProc> cache = new HashMap<>();

    /**
     * If the parameter is true works as cache
     */
    public ProcedureCache(boolean s) {
        shouldCache = s;
    }

    public synchronized void clear() {
        this.cache.clear();
    }

    /**
     * Gets or creates a instance of ProcedureCache objects from cache.
     */
    public synchronized SPProc get(ConnInfo conn, String procname, Connection sqlconn) throws SQLException, ProcedureNotFoundException {
        SPProc plp = this.cache.get(conn.connAlias + "." + conn.usr.toLowerCase() + "." + procname);
        if (plp == null) { // plp is not in cache yet
            plp = new SPProc(conn, procname, sqlconn);
            if (shouldCache) {
                this.cache.put(conn.connAlias + "." + conn.usr.toLowerCase() + "." + procname, plp);
            }
        }
        return plp;
    }
}
