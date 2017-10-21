/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 * Modified by Charly Schmid (charly.schmid@trivadis.com).                   *
 */

package com.prism;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.prism.utils.RequestParameters;
import com.prism.utils.UploadContent;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This class plays the role of AbstractProduct of the Abstract Factory pattern.
 * Declares an interface for a type of product object (create method). 
 * Also, it is an AbstractClass of Template Method pattern.
 * Defines abstract "primitive operations" that concrete subclasses define to implement step of algorithm()<BR> 
 * <BR>
 * Modified: 4/Nov/2003 by <a href="mailto:pyropunk@usa.net">Alexander Graesser</a> (LXG)<BR>
 * Changes : <UL><LI>Added log4j logging</LI>
 *           <LI>JavDoc cleanup</LI>
 *           <LI>code cleanup</LI></UL>
 */
public abstract class UploadRequest extends HttpServletRequestWrapper {
//    private static final java.lang.String UNKNOWN = "unknown";
    protected HttpServletRequest req;
    protected DBConnection conn; // Connection information to access to the repository

    /**
     * This RequestParameters stores Vectors with the parameter values extracted from the multipart/form-data request
     * Files uploaded are stored as parameters names too.
     * Multiples upload are represented in the vector as name={upload1,upload2,...}
     */
    private RequestParameters upreq = new RequestParameters();
    public UploadContent upload = new UploadContent();
    public Random rand = new Random();

    /** Abstract method of Factory */
    public abstract UploadRequest create(HttpServletRequest request, DBConnection repositoryConnection)
        throws IOException, SQLException;

   
    // LXG: removed SQLException as it is not thrown
    // public UploadRequest(HttpServletRequest request, DBConnection repositoryConnection) throws IOException, SQLException {
    public UploadRequest(HttpServletRequest request, DBConnection repositoryConnection) throws IOException {
        super(request);
        // Sanity check values
        if (request == null)
            throw new IllegalArgumentException("request cannot be null");
        if (repositoryConnection == null)
            throw new IllegalArgumentException("Repository Information cannot be null");
        // Save the request, and max size
        this.req = request;
        this.conn = repositoryConnection;
        // Now parse the request saving data to "parameters" and "files";
        // write the file contents to the Content Repository
        readRequest();
    }

/* The HttpServletRequest interface methods */

    public String getAuthType() {
        return this.req.getAuthType();
    }

    public Cookie[] getCookies() {
        return this.req.getCookies();
    }

    public long getDateHeader(String name) {
        return this.req.getDateHeader(name);
    }

    public String getHeader(String name) {
        return this.req.getHeader(name);
    }

    public Enumeration getHeaderNames() {
        return this.req.getHeaderNames();
    }

    public int getIntHeader(String name) {
        return this.req.getIntHeader(name);
    }

    public String getMethod() {
        return this.req.getMethod();
    }

    public String getPathInfo() {
        return this.req.getPathInfo();
    }

    public String getPathTranslated() {
        return this.req.getPathTranslated();
    }

    public String getQueryString() {
        return this.req.getQueryString();
    }

    public String getRemoteUser() {
        return this.req.getRemoteUser();
    }

    public String getRequestedSessionId() {
        return this.req.getRequestedSessionId();
    }

    public String getServletPath() {
        return this.req.getServletPath();
    }

    public HttpSession getSession(boolean create) {
        return this.req.getSession(create);
    }

    public boolean isRequestedSessionIdValid() {
        return this.req.isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return this.req.isRequestedSessionIdFromCookie();
    }

    public String getRequestURI() {
        return this.req.getRequestURI();
    }

    /**
     *********************************************************** added the following HttpServletRequest interface methods
     * for ServletApi 2.2 (dld - 02/05/2001)
     */

    /**
     * Returns the portion of the request URI that indicates the context
     * of the request.  The context path always comes first in a request
     * URI.  The path starts with a "/" character but does not end with a "/"
     * character.  For servlets in the default (root) context, this method returns "".
     * @return          a <code>String</code> specifying the
     * portion of the request URI that indicates the context of the request
     */
    public String getContextPath() {
        return this.req.getContextPath();
    }

    /**
     * Returns all the values of the specified request header as an <code>Enumeration</code> of <code>String</code> objects.
     * <p>Some headers, such as <code>Accept-Language</code> can be sent
     * by clients as several headers each with a different value rather than sending the header as a comma separated list.
     * <p>If the request did not include any headers of the specified name, this method returns an empty
     * <code>Enumeration</code>. The header name is case insensitive. You can use this method with any request header.
     * @param name              a <code>String</code> specifying the header name
     * @return                  a <code>Enumeration</code> containing the values of the requested header, or <code>null</code>
     * if the request does not have any headers of that name
     */
    public Enumeration getHeaders(String name) {
        return null;
    }

    /**
     * Returns a <code>java.security.Principal</code> object containing
     * the name of the current authenticated user. If the user has not been
     * authenticated, the method returns <code>null</code>.
     * @return          a <code>java.security.Principal</code> containing the name of the user making this request;
     * <code>null</code> if the user has not been authenticated
     */
    public java.security.Principal getUserPrincipal() {
        return null;
    }

    /**
     * Returns a boolean indicating whether the authenticated user is included
     * in the specified logical "role".  Roles and role membership can be
     * defined using deployment descriptors.  If the user has not been authenticated, the method returns <code>false</code>.
     * @param role              a <code>String</code> specifying the name of the role
     * @return          a <code>boolean</code> indicating whether the user making this request belongs to a given role;
     * <code>false</code> if the user has not been authenticated
     */
    public boolean isUserInRole(String role) {
        return false;
    }

    /* The ServletRequest interface methods */

    /**
     *********************************************************** added the following ServletRequest interface methods
     * for ServletApi 2.2 (dld - 02/05/2001)
     */

    /**
     * Returns the preferred <code>Locale</code> that the client will accept content in, based on the Accept-Language header.
     * If the client request doesn't provide an Accept-Language header, this method returns the default locale for the server.
     * @return          the preferred <code>Locale</code> for the client
     */
    public Locale getLocale() {
        return null;
    }

    /**
     * Returns an <code>Enumeration</code> of <code>Locale</code> objects
     * indicating, in decreasing order starting with the preferred locale, the
     * locales that are acceptable to the client based on the Accept-Language header.
     * If the client request doesn't provide an Accept-Language header,
     * this method returns an <code>Enumeration</code> containing one <code>Locale</code>, the default locale for the server.
     * @return          an <code>Enumeration</code> of preferred <code>Locale</code> objects for the client
     */
    public Enumeration getLocales() {
        return null;
    }

    /**
     * Returns a {@link RequestDispatcher} object that acts as a wrapper for the resource located at the given path.
     * A <code>RequestDispatcher</code> object can be used to forward
     * a request to the resource or to include the resource in a response. The resource can be dynamic or static.
     * <p>The pathname specified may be relative, although it cannot extend
     * outside the current servlet context.  If the path begins with
     * a "/" it is interpreted as relative to the current context root.
     * This method returns <code>null</code> if the servlet container cannot return a <code>RequestDispatcher</code>.
     * <p>The difference between this method and {@link ServletContext#getRequestDispatcher} is that this method
     * can take a relative path.
     * @param path      a <code>String</code> specifying the pathname to the resource
     * @return          a <code>RequestDispatcher</code> object that acts as a wrapper for the resource at the specified path
     * @see             RequestDispatcher
     * @see             ServletContext#getRequestDispatcher
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    /**
     * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
     * @return          a boolean indicating if the request was made using a secure channel
     */
    public boolean isSecure() {
        return false;
    }

    /**
     * Removes an attribute from this request.  This method is not
     * generally needed as attributes only persist as long as the request is being handled.
     * <p>Attribute names should follow the same conventions as package names. Names beginning with <code>java.*</code>,
     * <code>javax.*</code>, and <code>com.sun.*</code>, are reserved for use by Sun Microsystems.
     * @param name a <code>String</code> specifying the name of the attribute to remove
     */
    public void removeAttribute(String name) {
      // TODO: complete coding
    }

    /**  */
    public Object getAttribute(String name) {
        return this.req.getAttribute(name);
    }

    public void setAttribute(String key, Object o) {
        //this.req.setAttribute(key,o);
    }

    public Enumeration getAttributeNames() {
        //return this.req.getAttributeNames();
        return null;
    }

    public HttpSession getSession() {
        //return this.req.getSession();
        return null;
    }

    public String getCharacterEncoding() {
        return this.req.getCharacterEncoding();
    }

    public int getContentLength() {
        return this.req.getContentLength();
    }

    public String getContentType() {
        return this.req.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return this.req.getInputStream();
    }

    public String getProtocol() {
        return this.req.getProtocol();
    }

    public String getScheme() {
        return this.req.getScheme();
    }

    public String getServerName() {
        return this.req.getServerName();
    }

    public int getServerPort() {
        return this.req.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return this.req.getReader();
    }

    public String getRemoteAddr() {
        return this.req.getRemoteAddr();
    }

    public String getRemoteHost() {
        return this.req.getRemoteHost();
    }

    /** @deprecated		As of Version 2.1 of the Java Servlet API, use {@link #isRequestedSessionIdFromURL} instead. */
    public boolean isRequestedSessionIdFromUrl() {
        return this.req.isRequestedSessionIdFromUrl();
    }

    public boolean isRequestedSessionIdFromURL() {
        //return this.req.isRequestedSessionIdFromURL();
        return false;
    }

    /** @deprecated 	As of Version 2.1 of the Java Servlet API, use {@link ServletContext#getRealPath} instead. */
    public String getRealPath(String path) {
        return this.req.getRealPath(path);
    }

    /**
     *********************************************************** added the following HttpServletRequest interface methods
     * for ServletApi 2.3 (mfo - 11/04/2002)
     */
    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
        this.req.setCharacterEncoding(enc);
    }

    public Map getParameterMap() {
        return this.req.getParameterMap();
    }

    public StringBuffer getRequestURL() {
        return this.req.getRequestURL();
    }

  /**
    * @return HttpServletRequest getLocalPort
    */
    public int getLocalPort()
    {
      return this.req.getLocalPort();
    }
    
  /**
    * @return HttpServletRequest getRemotePort
    */
    public int getRemotePort()
    {
      return this.req.getRemotePort();
    }
    
  /**
    * @return HttpServletRequest getRemoteAddr
    */
    public String getLocalAddr()
    {
      return this.req.getLocalAddr();
    }
    
  /**
    * @return HttpServletRequest getLocalName
    */
    public String getLocalName()
    {
      return this.req.getLocalName();
    }
    
    /**
     * Returns the names of all the parameters as an Enumeration of
     * Strings.  It returns an empty Enumeration if there are no parameters.
     */
    public Enumeration getParameterNames() {
        return upreq.getParameterNames();
    }

    /**
     * Returns the value of the named parameter as a String, or null if
     * the parameter was not sent or was sent without a value.  The value
     * is guaranteed to be in its normal, decoded form.  If the parameter has multiple values, only the firts one is returned.
     */
    public String getParameter(String name) {
        return upreq.getParameter(name);
    }

    /**
     * Returns the values of the named parameter as a String array, or null if
     * the parameter was not sent.  The array has one entry for each parameter
     * field sent.  If any field was sent without a value that entry is stored
     * in the array as a null.  The values are guaranteed to be in their
     * normal, decoded form.  A single value is returned as a one-element array.
     */
    public String[] getParameterValues(String name) {
        return upreq.getParameterValues(name);
    }

    public void setParameter(String name, String value) {
        upreq.putParameter(name, value);
    }

    /**
     * The workhorse method that actually parses the request.  A subclass
     * can override this method for a better optimized or differently behaved implementation.
     */
    // LXG: removed SQLException as it is not thrown
    // protected void readRequest() throws IOException, SQLException {
    protected void readRequest() throws IOException {
        // Check the content length to prevent denial of service attacks
        int length = req.getContentLength();
        if (length > DBPrism.maxUploadSize)
            throw new IOException("Posted content is greater than global.maxUploadSize parameter (" +
                DBPrism.maxUploadSize / 1024 + " KB)");
        // Check the content type to make sure it's "multipart/form-data"
        // Access header two ways to work around WebSphere oddities
        String type = null;
        String type1 = req.getHeader("Content-Type");
        String type2 = req.getContentType();
        // If one value is null, choose the other value
        if (type1 == null && type2 != null) {
            type = type2;
        }
        else if (type2 == null && type1 != null) {
            type = type1;
        }
        // If neither value is null, choose the longer value
        else if (type1 != null && type2 != null) {
            type = (type1.length() > type2.length() ? type1 : type2);
        }
        if (type == null || !type.toLowerCase().startsWith("multipart/form-data")) {
            throw new IOException("Posted content type isn't multipart/form-data");
        }
        try {
            upload.initialize(req);
            upload.upload();
            saveFile(req);
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }

    public abstract void saveFile(HttpServletRequest preq) throws SQLException;
}
