/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

import com.prism.utils.OraUtil.ResolvedProcedure;
import java.sql.SQLException;
import java.util.HashMap;
import oracle.jdbc.OracleConnection;

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
    public synchronized SPProc get(ConnInfo conn, ResolvedProcedure rp, OracleConnection sqlconn) 
            throws SQLException, ProcedureNotFoundException {
        String hashKey = conn.connAlias + "/" + rp.fullName;
        SPProc plp = this.cache.get(hashKey);
        if (plp == null) { // plp is not in cache yet
            plp = new SPProc(conn, rp, sqlconn);
            if (shouldCache) {
                this.cache.put(hashKey, plp);
            }
        }
        return plp;
    }
}
