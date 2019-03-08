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
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BFILE;
import oracle.sql.BLOB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class plays the role of AbstractProduct of the Abstract Factory pattern.
 * Declares an interface for a type of product object (create method)<BR>
 * <BR>
 * Modified: 4/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander
 * Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 * <LI>JavDoc cleanup</LI>
 * <LI>code cleanup</LI></UL>
 */
public class DownloadRequest {

    private static final Logger log = LogManager.getLogger();

    protected HttpServletRequest req;

    protected String contentType = "application/octet-stream";

    protected long lastModified = 0L;

    protected int contentLength = 0;

    protected DBConnection conn;
    // Connection information to access to the repository

    /**
     * Abstract method of Factory
     */
    public DownloadRequest create(HttpServletRequest request,
            DBConnection repositoryConnection)
            throws IOException, SQLException {
        return new DownloadRequest(request, repositoryConnection);
    }

    public DownloadRequest() {
        // LXG: call to super is generated anyway but put it here for clarity.
        super();
    }

    // LXG: remove IOException and SQLException as they are not thrown
    // public DownloadRequest(HttpServletRequest request, HttpServletResponse response, DBConnection repositoryConnection) throws IOException, SQLException {
    public DownloadRequest(HttpServletRequest request,
            DBConnection repositoryConnection) {
        super();
        if (request == null) {
            throw new IllegalArgumentException("DBPrism: Request cannot be null");
        }
        if (repositoryConnection == null) {
            throw new IllegalArgumentException("DBPrism: Repository Information cannot be null");
        }
        this.req = request;
        this.conn = repositoryConnection;
    }

    /**
     * @return true if there is a file for downloading if there where errors
     * return false and log.warn the exception message
     */
    public boolean isFileDownload() {
        CallableStatement cs = null;
        int i;
        try {
            cs
                    = conn.sqlconn.prepareCall("DECLARE\n" + "FUNCTION b2n(b BOOLEAN) RETURN NUMBER IS\n"
                            + "BEGIN\n" + "IF (b) THEN\n"
                            + "  RETURN '1';\n" + " END IF;\n"
                            + " RETURN '0';\n" + " END;\n"
                            + "BEGIN ? := b2n(wpg_docload.is_file_download); END;");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            i = cs.getInt(1);
        } catch (SQLException e) {
            log.warn(".isFileDownload exception when calling wpg_docload.is_file_download", e);
            i = 0;
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                log.warn(".isFileDownload exception when closing callable statement", e);
            } finally {
                cs = null;
            }
        }
        return (i != 0);
    }

    /**
     * @return an encode string with the download information in PLSQL it calls
     * to the procedure wpg_docload.get_download_file which return a VARCHAR
     * string with this format 12XNOT_MODIFIED => this file is not modified
     * according to the client modification time F => the download is in a BFILE
     * format B => the download is in a BLOB format nnXfilename => the download
     * is a <i>documentTable</i>
     */
    /**
     * @return an encoded string with the download information
     * @see com.prism.DownloadRequest#getDownloadInfo
     */
    public String getDownloadInfo() {
        CallableStatement cs = null;
        String downloadInfo = "";
        try {
            cs
                    = conn.sqlconn.prepareCall("BEGIN wpg_docload.get_download_file(?); END;");
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.execute();
            downloadInfo = cs.getString(1);
        } catch (SQLException e) {
            log.warn(".getDownloadInfo exception when calling wpg_docload.get_download_file(?)", e);
            downloadInfo = "";
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                log.warn(".getDownloadInfo exception when closing callable statement", e);
            } finally {
                cs = null;
            }
        }
        return (downloadInfo != null) ? downloadInfo : "";
    }

    /**
     * call to DAD <i>docAccessProcedure</i> to provide to the application the
     * functionality of convert the servlet request information into a download
     * information
     *
     * @throws SQLException
     */
    /**
     * @see com.prism.DownloadRequest#callAccessProcedure
     * @throws SQLException
     */
    public void callAccessProcedure() throws SQLException {
        CallableStatement cs = null;
        try {
            cs
                    = conn.sqlconn.prepareCall("BEGIN " + conn.connInfo.docAccessProcedure
                            + "; END;");
            cs.execute();
        } catch (SQLException e) {
            log.warn(".callAccessProcedure exception when calling '" + conn.connInfo.docAccessProcedure + "' procedure", e);
            throw e;
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                log.warn(".callAccessProcedure exception when closing callable statement", e);
            } finally {
                cs = null;
            }
        }
    }

    public InputStream getStream(String downloadInfo) throws SQLException {
        InputStream in = null;
        int length, i;
        String mime = "application/octet-stream";
        java.sql.Date lastUpdated
                = new java.sql.Date(System.currentTimeMillis());
        CallableStatement cs = null;
        Statement stmt = null;
        ResultSet rset = null;
        // Get generated page in one call via stream
        try {
            if ("F".equalsIgnoreCase(downloadInfo)) {
                BFILE locator;
                // Handle download file as BFile
                cs = conn.sqlconn.prepareCall("BEGIN wpg_docload.get_download_bfile(?); END;");
                cs.registerOutParameter(1, OracleTypes.BFILE);
                cs.execute();
                locator = ((OracleCallableStatement) cs).getBFILE(1);
                in = locator.binaryStreamValue();
                cs.close();
                cs = null;
                cs = conn.sqlconn.prepareCall(
                        "BEGIN ? := wpg_docload.get_content_length; END;");
                cs.registerOutParameter(1, OracleTypes.INTEGER);
                cs.execute();
                length = cs.getInt(1);
            } else if ("B".equalsIgnoreCase(downloadInfo)) {
                BLOB locator = null;
                // Handle download file as BLOB
                cs = conn.sqlconn.prepareCall(
                        "BEGIN wpg_docload.get_download_blob(?); END;");
                cs.registerOutParameter(1, OracleTypes.BLOB);
                cs.execute();
                locator = ((OracleCallableStatement) cs).getBLOB(1);
                in = locator.binaryStreamValue();
                cs.close();
                cs = null;
                cs = conn.sqlconn.prepareCall(
                        "BEGIN ? := wpg_docload.get_content_length; END;");
                cs.registerOutParameter(1, OracleTypes.INTEGER);
                cs.execute();
                length = cs.getInt(1);
            } else {
                BLOB locator = null;
                String name = null;
                int j = downloadInfo.indexOf('X');
                if (j
                        > 0) { // support for filename not in the syntax of nnXfilename
                    i = Integer.parseInt(downloadInfo.substring(0, j));
                    name = downloadInfo.substring(j + 1, j + i + 1);
                } else // strips doc access path if it was in the filename
                {
                    name = (downloadInfo.startsWith(
                            conn.connInfo.docAccessPath + "/"))
                                    ? downloadInfo.substring(
                                            conn.connInfo.docAccessPath.length() + 1)
                                    : downloadInfo;
                }
                // Create a Statement
                stmt = conn.sqlconn.createStatement();
                // Selects the lobs
                rset = stmt.executeQuery(
                        "select mime_type,doc_size,last_updated,blob_content from "
                        + conn.connInfo.documentTable + " where name = '" + name
                        + "'");
                if (rset.next()) {
                    mime = rset.getString(1);
                    length = rset.getInt(2);
                    lastUpdated = rset.getDate(3);
                    locator = ((OracleResultSet) rset).getBLOB(4);
                    in = locator.binaryStreamValue();
                } else {
                    // nothing here ?
                    throw new SQLException("DB Prism - 8i Adapter: can't get download file: "
                            + name);
                }
            }
        } catch (SQLException e) {
            log.warn("Exception during download", e);
            throw e;
        } finally {
            if (cs != null) {
                cs.close();
                cs = null;
            }
            if (rset != null) {
                rset.close();
                rset = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
        }
        // get PrintWriter after ser content type and content lenght
        this.setContentLength(length);
        this.setContentType(mime);
        this.setLastModified(lastUpdated.getTime() / 1000 * 1000);
        // Round Last modified date to seconds, discard mili-seconds
        return in;
    }

    /**
     * Template method, give to the subclass the responsability of implementing
     * each step in the algorithm
     */
    public void doDownloadFromDB(HttpServletResponse res) throws SQLException {
        ServletOutputStream out = null;
        InputStream in = null;
        int i;
        String downloadInfo;
        // Get generated page in one call via stream
        callAccessProcedure();
        if (!isFileDownload()) {
            log.warn(".doDownloadFromDB - Nothing to download");
            throw new SQLException("DBPrism - DownloadRequest: Nothing to download");
        }
        downloadInfo = getDownloadInfo();
        if (downloadInfo == null || downloadInfo.length() == 0) {
            throw new SQLException("DBPrism - DownloadRequest: No Download Information");
        }
        if (downloadInfo.indexOf("12XNOT_MODIFIED") > 0) {
            // Browser time is equal to Db time and DB cached is true
            // don't return a new upload
            if (log.isDebugEnabled()) {
                log.debug(".doDownloadFromDB - returning SC_NOT_MODIFIED, download information is: " + downloadInfo);
            }
            res.setStatus(res.SC_NOT_MODIFIED);
            return;
        }
        in = getStream(downloadInfo);
        if (log.isDebugEnabled()) {
            log.debug(".doDownloadFromDB - content-type: " + this.contentType);
        }
        res.setContentType(this.getContentType());
        if (log.isDebugEnabled()) {
            log.debug(".doDownloadFromDB - content-length: " + this.contentLength);
        }
        res.setContentLength(this.getContentLength());
        if (log.isDebugEnabled()) {
            log.debug(".doDownloadFromDB - Last-Modified: " + this.lastModified);
        }
        res
                .setDateHeader("Last-Modified", this.getLastModified());
        try {
            out = res.getOutputStream();
            byte[] buff_out = new byte[65000];
            // send it without pay attention to new lines
            while ((i = in.read(buff_out)) > 0) {
                //System.out.println("out bytes=>"+i);
                out.write(buff_out, 0, i);
            }
            out.flush();
            in.close();
        } catch (IOException e) {
            log.warn(".doDownloadFromDB - Error coping InputStream", e);
            throw new SQLException("DBPrism - 8i Adapter Internal Error:\n"
                    + e.getMessage());
        }
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getLastModified() {
        return lastModified;
    }
}
