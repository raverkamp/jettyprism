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

import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.BLOB;
import oracle.sql.CLOB;
//import oracle.xdb.XMLType;

import org.apache.log4j.Logger;

import com.prism.DBConnection;
import com.prism.DownloadRequest;


// Oracle extensions


/**
 * This class implements a ConcreteProduct class of abtract factory patterm.
 * Define a product object (DownloadJava represent a Download request) to be created by the corresponding concrete factory.
 * Implements the AbstractProduct Interface (DownloadRequest)
 */
public class DownloadJava extends DownloadRequest {
    private static Logger log = Logger.getLogger(DownloadJava.class);
    
    public DownloadJava() {
    }

    public DownloadJava(HttpServletRequest request,
                        DBConnection repositoryConnection) throws IOException,
                                                                                                SQLException {
        super(request, repositoryConnection);
    }

    /** Create a concrete DownloadRequest (DownloadJava). */
    public DownloadRequest create(HttpServletRequest request,
                                  DBConnection repositoryConnection) throws IOException,
                                                                                                          SQLException {
        return new DownloadJava(request, repositoryConnection);
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
                conn.sqlconn.prepareCall("{ ? = call JwpgDocLoad.ISFILEDOWNLOAD() }");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            i = cs.getInt(1);
            cs.close();
        } catch (SQLException e) {
            log.warn(".isFileDownload exception when calling wpg_docload.is_file_download",e);  
            i=0; 
        } finally {
            try {
              cs.close();
            } catch (SQLException e) {
              log.warn(".isFileDownload exception when closing callable statement",e);  
            } finally {
              cs = null;  
            }
        }
       return (i!=0); 
    }
    
    public InputStream getStream(String downloadInfo) throws SQLException {
        InputStream in = null;
        int i, length;
        String mime = "application/octet-stream";
        java.sql.Date lastUpdated =
            new java.sql.Date(System.currentTimeMillis());
        CallableStatement cs = null;
        if (downloadInfo.startsWith("Error:"))
            throw new SQLException("DBPrism - DownloadJava Adapter JwpgDocLoad.getDownloadFile " +
                                   downloadInfo);
        if ("B".equalsIgnoreCase(downloadInfo)) {
            BLOB locator = null;
            // Handle download file as BLOB
            cs =
                conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETDOWNLOADBLOB()}");
            cs.registerOutParameter(1, OracleTypes.BLOB);
            cs.execute();
            locator = ((OracleCallableStatement)cs).getBLOB(1);
            in = locator.binaryStreamValue();
            cs.close();
            //cs =
            //    conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETCONTENTLENGTH()}");
            //cs.registerOutParameter(1, OracleTypes.INTEGER);
            //cs.execute();
            length = (int)locator.getLength();
            //cs.close();
        } else if ("C".equalsIgnoreCase(downloadInfo)) {
            CLOB locator = null;
            // Handle download file as CLOB
            cs =
                conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETDOWNLOADCLOB()}");
            cs.registerOutParameter(1, OracleTypes.CLOB);
            cs.execute();
            locator = ((OracleCallableStatement)cs).getCLOB(1);
            in = locator.binaryStreamValue();
            cs.close();
            //cs =
            //    conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETCONTENTLENGTH()}");
            //cs.registerOutParameter(1, OracleTypes.INTEGER);
            //cs.execute();
            length = (int)locator.getLength();
            //cs.close();
//        } else if ("X".equalsIgnoreCase(downloadInfo)) {
//                XMLType locator = null;
//                // Handle download file as XMLType
//                cs = conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETDOWNLOADXML()}");
//                //ps = conn.sqlconn.prepareStatement("select res from resource_view where equals_path(res,'/xdbconfig.xml')=1");
//                cs.registerOutParameter (1, OracleTypes.OPAQUE,"SYS.XMLTYPE");
                //cs.execute();
//                locator = XMLType.createXML(((OracleCallableStatement)cs).getOPAQUE(1));
//                in = locator.getClobVal().binaryStreamValue();
                //cs.close();
//                //cs =
//                //    conn.sqlconn.prepareCall("{? = call JwpgDocLoad.GETCONTENTLENGTH()}");
//                //cs.registerOutParameter(1, OracleTypes.INTEGER);
//                //cs.execute();
//                length = (int)locator.getLength();
//                //cs.close();
        } else {
            BLOB locator = null;
            ResultSet rset = null;
            Statement stmt = null;
            i =
                Integer.parseInt(downloadInfo.substring(0, downloadInfo.indexOf('X')));
            String name =
                downloadInfo.substring(downloadInfo.indexOf('X') + 1, downloadInfo
                                                 .indexOf('X') + i + 1);
            // Create a Statement
            stmt = conn.sqlconn.createStatement();
            // Select the lobs
            rset =
                stmt.executeQuery("select mime_type,doc_size,last_updated,blob_content from " +
                                     conn.connInfo.documentTable +
                                     " where name = '" + name + "'");
            if (rset.next()) {
                mime = rset.getString(1);
                length = rset.getInt(2);
                lastUpdated = rset.getDate(3);
                locator = ((OracleResultSet)rset).getBLOB(4);
                in = locator.binaryStreamValue();
                rset.close();
                stmt.close();
            } else {
                // nothing here ?
                rset.close();
                stmt.close();
                throw new SQLException("DBPrism - DownloadJava Adapter: can't get download file: " +
                                       name);
            }
        }
        // Caller will use these values
        this.setContentLength(length);
        this.setContentType(mime);
        // Round Last modified date to seconds, discard mili-seconds
        this.setLastModified(lastUpdated.getTime() / 1000 * 1000);
        return in;
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
                 conn.sqlconn.prepareCall("{ ? = call JwpgDocLoad.GETDOWNLOADFILE() }");
             cs.registerOutParameter(1, Types.VARCHAR);
             cs.execute();
             downloadInfo = cs.getString(1);
             cs.close();
         } catch (SQLException e) {
             log.warn(".getDownloadInfo exception when calling JwpgDocLoad.GETDOWNLOADFILE()",e);  
             downloadInfo=""; 
         } finally {
             try {
               cs.close();
             } catch (SQLException e) {
               log.warn(".getDownloadInfo exception when closing callable statement",e);  
             } finally {
               cs = null;  
             }
         }
        return downloadInfo; 
    }

    /**
     * @see com.prism.DownloadRequest#callAccessProcedure
     * @throws SQLException
     */
    public void callAccessProcedure() throws SQLException {
        CallableStatement cs = null;
         try {
             cs =
                 conn.sqlconn.prepareCall("{ call " + conn.connInfo.docAccessProcedure +
                                           "() }");
             cs.execute();
             cs.close();
         } catch (SQLException e) {
             log.warn(".getDownloadInfo exception when calling '"+conn.connInfo.docAccessProcedure+"' procedure",e);
             throw e;
         } finally {
             try {
               cs.close();
             } catch (SQLException e) {
               log.warn(".getDownloadInfo exception when closing callable statement",e);  
             } finally {
               cs = null;  
             }
         }
    }
}
