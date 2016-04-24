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
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BFILE;
import oracle.sql.BLOB;

import org.apache.log4j.Logger;

import com.prism.DBConnection;
import com.prism.DownloadRequest;

/**
 * This class implements a ConcreteProduct class of abtract factory patterm.
 * Define a product object (Download8i represent a Download request) to be created by the corresponding concrete factory.
 * Implements the AbstractProduct Interface (DownloadRequest)
 */
public class Download8i extends DownloadRequest {
    private static Logger log = Logger.getLogger(Download8i.class);
    
    public Download8i() {
    }

    public Download8i(HttpServletRequest request,
                      DBConnection repositoryConnection) throws IOException,
                                                                                                                          SQLException {
        super(request, repositoryConnection);
    }

    /** Create a concrete DownloadRequest (Download8i). */
    public DownloadRequest create(HttpServletRequest request,
                                  DBConnection repositoryConnection) throws IOException,
                                                                                                          SQLException {
        return new Download8i(request, repositoryConnection);
    }

    /**
     * @return true if there is a file for downloading
     * if there where errors return false and log.warn the exception message
     */
    public boolean isFileDownload() {
       CallableStatement cs = null;
       int i;
        try {
            cs =
            conn.sqlconn.prepareCall("DECLARE\n" + "FUNCTION b2n(b BOOLEAN) RETURN NUMBER IS\n" +
                                      "BEGIN\n" + "IF (b) THEN\n" +
                                      "  RETURN '1';\n" + " END IF;\n" +
                                      " RETURN '0';\n" + " END;\n" +
                                      "BEGIN ? := b2n(wpg_docload.is_file_download); END;");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            i = cs.getInt(1);
        } catch (SQLException e) {
            log.warn(".isFileDownload exception when calling wpg_docload.is_file_download",e);  
            i=0; 
        } finally {
            try {
              if (cs != null)
                cs.close();
            } catch (SQLException e) {
              log.warn(".isFileDownload exception when closing callable statement",e);  
            } finally {
              cs = null;  
            }
        }
       return (i!=0); 
    }
    
    /**
     * @return an encoded string with the download information
     * @see com.prism.DownloadRequest#getDownloadInfo
     */
    public String getDownloadInfo() {
        CallableStatement cs = null;
        String downloadInfo = "";
         try {
             cs =
                 conn.sqlconn.prepareCall("BEGIN wpg_docload.get_download_file(?); END;");
             cs.registerOutParameter(1, Types.VARCHAR);
             cs.execute();
             downloadInfo = cs.getString(1);
         } catch (SQLException e) {
             log.warn(".getDownloadInfo exception when calling wpg_docload.get_download_file(?)",e);  
             downloadInfo=""; 
         } finally {
             try {
                 if (cs != null)
                    cs.close();
             } catch (SQLException e) {
               log.warn(".getDownloadInfo exception when closing callable statement",e);  
             } finally {
               cs = null;  
             }
         }
        return (downloadInfo!=null) ? downloadInfo : ""; 
    }

    /**
     * @see com.prism.DownloadRequest#callAccessProcedure
     * @throws SQLException
     */
    public void callAccessProcedure() throws SQLException {
        CallableStatement cs = null;
        if (((DBConnPLSQL)conn).toolkitVersion.equalsIgnoreCase("3x"))
            throw new SQLException("DBPrism - 8i Adapter: Can't provide this funcionality through toolkit 3x");
        try {
             cs =
                 conn.sqlconn.prepareCall("BEGIN " + conn.connInfo.docAccessProcedure +
                                           "; END;");
             cs.execute();
         } catch (SQLException e) {
             log.warn(".callAccessProcedure exception when calling '"+conn.connInfo.docAccessProcedure+"' procedure",e);
             throw e;
         } finally {
             try {
                 if (cs != null)
                    cs.close();
             } catch (SQLException e) {
               log.warn(".callAccessProcedure exception when closing callable statement",e);  
             } finally {
               cs = null;  
             }
         }
    }

    public InputStream getStream(String downloadInfo) throws SQLException {
        InputStream in = null;
        int length, i;
        String mime = "application/octet-stream";
        java.sql.Date lastUpdated =
            new java.sql.Date(System.currentTimeMillis());
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
                locator = ((OracleCallableStatement)cs).getBFILE(1);
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
                locator = ((OracleCallableStatement)cs).getBLOB(1);
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
                if (j >
                    0) { // support for filename not in the syntax of nnXfilename
                    i = Integer.parseInt(downloadInfo.substring(0, j));
                    name = downloadInfo.substring(j + 1, j + i + 1);
                } else // strips doc access path if it was in the filename
                    name = (downloadInfo.startsWith(
                           conn.connInfo.docAccessPath + "/")) ?
                           downloadInfo.substring(
                           conn.connInfo.docAccessPath.length() + 1) :
                           downloadInfo;
                // Create a Statement
                stmt = conn.sqlconn.createStatement();
                // Selects the lobs
                rset = stmt.executeQuery(
                  "select mime_type,doc_size,last_updated,blob_content from " +
                  conn.connInfo.documentTable + " where name = '" + name +
                  "'");
                if (rset.next()) {
                    mime = rset.getString(1);
                    length = rset.getInt(2);
                    lastUpdated = rset.getDate(3);
                    locator = ((OracleResultSet)rset).getBLOB(4);
                    in = locator.binaryStreamValue();
                } else {
                    // nothing here ?
                    throw new SQLException("DB Prism - 8i Adapter: can't get download file: " +
                                           name);
                }
            }
        } catch (SQLException e) {
          log.warn("Exception during download",e);
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
}
