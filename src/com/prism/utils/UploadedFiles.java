/**
 ********************************************************************************
 * Copyright (C) Charly Schmid (charly.schmid@trivadis.com). All rights reserved.*
 * ------------------------------------------------------------------------------*
 * This software is published under the terms of the Apache Software License     *
 * version 1.1, a copy of which has been included  with this distribution in     *
 * the LICENSE file.                                                             *
 */
package com.prism.utils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/*
 * This class stores the information of the all files uploaded
 */
public class UploadedFiles {

    private UploadContent m_parent;
    private Hashtable m_files;
    private int m_counter;

    UploadedFiles() {
        m_files = new Hashtable();
        m_counter = 0;
    }

    protected void addFile(UploadedFile newFile) {
        if (newFile == null) {
            throw new IllegalArgumentException("newFile cannot be null.");
        } else {
            m_files.put(new Integer(m_counter), newFile);
            m_counter++;
            return;
        }
    }

    public UploadedFile getFile(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("File's index cannot be a negative value.");
        }
        UploadedFile retval = (UploadedFile) m_files.get(new Integer(index));
        if (retval == null) {
            throw new IllegalArgumentException("Files' name is invalid or does not exist.");
        } else {
            return retval;
        }
    }

    public int getCount() {
        return m_counter;
    }

    public long getSize() throws IOException {
        long tmp = 0L;
        for (int i = 0; i < m_counter; i++) {
            tmp += getFile(i).getSize();
        }
        return tmp;
    }

    public Enumeration getEnumeration() {
        return m_files.elements();
    }
}
