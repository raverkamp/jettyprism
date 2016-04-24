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
import java.sql.SQLException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * This class plays the role of AbstractProduct of the Abstract Factory pattern.
 * Declares an interface for a type of product object (create method)<BR> 
 * <BR>
 * Modified: 4/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 */
public abstract class DownloadRequest {
    private static Logger log = Logger.getLogger(DownloadRequest.class);

    protected HttpServletRequest req;

    protected String contentType = "application/octet-stream";
    
    protected long lastModified = 0L;
    
    protected int contentLength  = 0;
    
    protected DBConnection conn;
    // Connection information to access to the repository

    /** Abstract method of Factory */
    public abstract DownloadRequest create(HttpServletRequest request,
                                           DBConnection repositoryConnection) throws IOException,
                                                                                                                   SQLException;

    public DownloadRequest() {
        // LXG: call to super is generated anyway but put it here for clarity.
        super();
    }

    // LXG: remove IOException and SQLException as they are not thrown
    // public DownloadRequest(HttpServletRequest request, HttpServletResponse response, DBConnection repositoryConnection) throws IOException, SQLException {

    public DownloadRequest(HttpServletRequest request,
                           DBConnection repositoryConnection) {
        super();
        if (request == null)
            throw new IllegalArgumentException("DBPrism: Request cannot be null");
        if (repositoryConnection == null)
            throw new IllegalArgumentException("DBPrism: Repository Information cannot be null");
        this.req = request;
        this.conn = repositoryConnection;
    }

    /**
     * @return true if there is file for downloading, otherwhise false
     * In PLSQL it calls to the function
     * <i>wpg_docload.is_file_download</i>
     * If there is exception, return false and log.warn the error message
     */
    public abstract boolean isFileDownload();

    /**
     * @return an encode string with the download information
     * in PLSQL it calls to the procedure wpg_docload.get_download_file which
     * return a VARCHAR string with this format
     * 12XNOT_MODIFIED => this file is not modified according to the client modification time
     * F               => the download is in a BFILE format
     * B               => the download is in a BLOB format
     * nnXfilename     => the download is a <i>documentTable</i>
     */
    public abstract String getDownloadInfo();

    /**
     * call to DAD <i>docAccessProcedure</i> to provide to the application
     * the functionality of convert the servlet request information into a 
     * download information
     * @throws SQLException
     */
    public abstract void callAccessProcedure() throws SQLException;

    public abstract InputStream getStream(String downloadInfo) throws SQLException;
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
         if (downloadInfo == null || downloadInfo.length()==0)
             throw new SQLException("DBPrism - DownloadRequest: No Download Information");
         if (downloadInfo.indexOf("12XNOT_MODIFIED") > 0) {
             // Browser time is equal to Db time and DB cached is true
             // don't return a new upload
             if (log.isDebugEnabled())
               log.debug(".doDownloadFromDB - returning SC_NOT_MODIFIED, download information is: "+downloadInfo);
             res.setStatus(res.SC_NOT_MODIFIED);
             return;
         }
         in = getStream(downloadInfo);
         if (log.isDebugEnabled())
           log.debug(".doDownloadFromDB - content-type: "+this.contentType);
         res.setContentType(this.getContentType());
         if (log.isDebugEnabled())
           log.debug(".doDownloadFromDB - content-length: "+this.contentLength);
         res.setContentLength(this.getContentLength());
         if (log.isDebugEnabled())
           log.debug(".doDownloadFromDB - Last-Modified: "+this.lastModified);
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
             log.warn(".doDownloadFromDB - Error coping InputStream",e);
             throw new SQLException("DBPrism - 8i Adapter Internal Error:\n" +
                                    e.getMessage());
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
