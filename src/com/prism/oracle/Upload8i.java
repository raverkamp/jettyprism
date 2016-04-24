/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism.oracle;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.sql.BLOB;

import com.prism.DBConnection;
import com.prism.UploadRequest;
import java.util.Collection;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * This class implements a ConcreteProduct class of abtract factory patterm.
 * Define a product object (Upload8i represent a Uploaded request) to be created by the corresponding concrete factory.
 * Implements the AbstractProduct Interface (UploadRequest)
 */
public class Upload8i extends UploadRequest {
    public Upload8i() { };

    public Upload8i(HttpServletRequest request, DBConnection repositoryConnection) throws IOException, SQLException {
        super(request, repositoryConnection);
    }

    /** Create a concrete SPProc (Upload8i). */
    public UploadRequest create(HttpServletRequest request, DBConnection repositoryConnection)
        throws IOException, SQLException {
            return new Upload8i(request, repositoryConnection);
    }

    public void saveFile(HttpServletRequest req) throws SQLException {
        String m_dir = null;
        String m_field_name;
        String m_field_value;
        BLOB myblob;
        OraclePreparedStatement ps = null;
        OracleResultSet rs = null;
        try {
            for (int u = 0; u < upload.getFiles().getCount(); u++) {
                m_field_name = upload.getFiles().getFile(u).getFieldName();
                if (!upload.getFiles().getFile(u).isMissing()) {
                    m_dir = "F" + String.valueOf(Math.abs(rand.nextInt()));
                    m_field_value = m_dir + "/" + upload.getFiles().getFile(u).getFileName();
                    setParameter(m_field_name, m_field_value);
                    try {
                        ps = (OraclePreparedStatement)conn.sqlconn.prepareStatement("insert into " +
                            conn.connInfo.documentTable + "  (name,mime_type,doc_size,dad_charset,last_updated,blob_content) values(?,?,?,?,?,empty_blob())");
                        ps.setString(1, m_field_value);
                        ps.setString(2, upload.getFiles().getFile(u).getTypeMIME() + "/" +
                            upload.getFiles().getFile(u).getSubTypeMIME());
                        ps.setInt(3, upload.getFiles().getFile(u).getSize());
                        ps.setString(4, conn.connInfo.clientCharset);
                        ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                        ps.execute();
                        ps.close();
                        ps = null;
                        ps = (OraclePreparedStatement)conn.sqlconn.prepareStatement("select blob_content from " +
                            conn.connInfo.documentTable + " where name=? for update");
                        ps.setString(1, m_field_value);
                        rs = (OracleResultSet)ps.executeQuery();
                        if (rs.next()) {
                            myblob = rs.getBLOB(1);
                            OutputStream outstream = myblob.getBinaryOutputStream();
                            outstream.write(upload.getFiles().getFile(u).fileToBlob());
                            outstream.flush();
                            outstream.close();
                        }
                    } catch (Exception e) {
                        throw new SQLException(e.toString());
                    } finally {
                        if (rs != null) {
                            rs.close();
                            rs = null;
                        }
                        if (ps != null) {
                            ps.close();
                            ps = null;
                        }
                    }
                }
            }
            Enumeration p_names = upload.getRequest().getParameterNames();
            while (p_names.hasMoreElements()) {
                String name = (String)p_names.nextElement();
                String vals[] = upload.getRequest().getParameterValues(name);
                for (int i = 0; i < vals.length; i++) {
                    String val = new String(vals[i].getBytes(conn.connInfo.clientCharset));
                    setParameter(name, val);
                }
            }
        } catch (Exception e) {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void login(String string, String string1) throws ServletException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Part getPart(String string) throws IOException, IllegalStateException, ServletException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
