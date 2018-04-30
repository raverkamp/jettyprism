/**
 ********************************************************************************
 * Copyright (C) Charly Schmid (charly.schmid@trivadis.com). All rights reserved.*
 * ------------------------------------------------------------------------------*
 * This software is published under the terms of the Apache Software License     *
 * version 1.1, a copy of which has been included  with this distribution in     *
 * the LICENSE file.                                                             *
 */
package com.prism.utils;

import java.util.Enumeration;
import java.util.Hashtable;

public class RequestParameters {

    private Hashtable m_parameters;
    private int m_counter;

    public RequestParameters() {
        m_parameters = new Hashtable();
        m_counter = 0;
    }

    public void putParameter(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("The name of an element cannot be null.");
        }
        if (m_parameters.containsKey(name)) {
            Hashtable values = (Hashtable) m_parameters.get(name);
            values.put(new Integer(values.size()), value);
        } else {
            Hashtable values = new Hashtable();
            values.put(new Integer(0), value);
            m_parameters.put(name, values);
            m_counter++;
        }
    }

    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Form's name is invalid or does not exist (1305).");
        }
        Hashtable values = (Hashtable) m_parameters.get(name);
        if (values == null) {
            return null;
        } else {
            return (String) values.get(new Integer(0));
        }
    }

    public Enumeration getParameterNames() {
        return m_parameters.keys();
    }

    public String[] getParameterValues(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Form's name is invalid or does not exist (1305).");
        }
        Hashtable values = (Hashtable) m_parameters.get(name);
        if (values == null) {
            return null;
        }
        String strValues[] = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            strValues[i] = (String) values.get(new Integer(i));
        }
        return strValues;
    }
}
