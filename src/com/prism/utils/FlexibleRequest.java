/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * This class transforms an HttpServeletRequest in a new request which has four parameters, ej:
 * http://www.acme.com/pls/myDAD/!scott.my_pkg.my_proc?x=a&y=b&x=c num_entries ==] 3 name_array ==] (`x', `y', `x');
 * value_array ==] (`a', `b', `c') reserved ==] ()
 */
public class FlexibleRequest extends HttpServletRequestWrapper {
    protected HttpServletRequest req;

    /** This Hastable stores Vectors with the parameter values extracted from the request */
    private Hashtable parameters = new Hashtable();

   

    public FlexibleRequest(HttpServletRequest request) throws IOException {
        super(request);
        // Sanity check values
        if (request == null)
            throw new IllegalArgumentException("request cannot be null");
        this.req = request;
        Vector nameArray = new Vector();
        Vector valueArray = new Vector();
        Vector numEntries = new Vector();
        Vector reserved = new Vector();
        int count = 0;
        Enumeration params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String name = (String)params.nextElement();
            String multi[] = req.getParameterValues(name);
            if (multi != null)
                for (int i = 0; i < multi.length; i++) {
                    nameArray.addElement(name);
                    valueArray.addElement(multi[i]);
                    count++;
                }
            else {
                nameArray.addElement(name);
                valueArray.addElement(req.getParameter(name));
                count++;
            }
        }
        numEntries.addElement("" + count);
        reserved.addElement("none");
        parameters.put("num_entries", numEntries);
        parameters.put("name_array", nameArray);
        parameters.put("value_array", valueArray);
        parameters.put("reserved", reserved);
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
     * for Servlet API 2.2 (dld - 02/05/2001)
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
        return this.req.getHeaders(name);
    }

    /**
     * Returns the current session associated with this request, or if the request does not have a session, creates one.
     * @return          the <code>HttpSession</code> associated with this request
     * @see     #getSession(boolean)
     */
    public HttpSession getSession() {
        return this.req.getSession();
    }

    /**
     * Returns a <code>java.security.Principal</code> object containing
     * the name of the current authenticated user. If the user has not been
     * authenticated, the method returns <code>null</code>.
     * @return          a <code>java.security.Principal</code> containing the name of the user making this request;
     * <code>null</code> if the user has not been authenticated
     */
    public java.security.Principal getUserPrincipal() {
        return this.req.getUserPrincipal();
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
        return this.req.isUserInRole(role);
    }

    /* The ServletRequest interface methods */

    /**
     *********************************************************** added the following ServletRequest interface methods
     * for Servlet API 2.2 (dld - 02/05/2001)
     */

    /**
     * Returns an <code>Enumeration</code> containing the names of the attributes available to this request.
     * This method returns an empty <code>Enumeration</code> if the request has no attributes available to it.
     * @return          an <code>Enumeration</code> of strings containing the names of the request's attributes
     */
    public Enumeration getAttributeNames() {
        return this.req.getAttributeNames();
    }

    /**
     * Returns the preferred <code>Locale</code> that the client will accept content in, based on the Accept-Language header.
     * If the client request doesn't provide an Accept-Language header, this method returns the default locale for the server.
     * @return          the preferred <code>Locale</code> for the client
     */
    public Locale getLocale() {
        return this.req.getLocale();
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
        return this.req.getLocales();
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
        return this.req.getRequestDispatcher(path);
    }

    /**
     * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
     * @return          a boolean indicating if the request was made using a secure channel
     */
    public boolean isSecure() {
        return this.req.isSecure();
    }

    /**
     * Removes an attribute from this request.  This method is not
     * generally needed as attributes only persist as long as the request is being handled.
     * <p>Attribute names should follow the same conventions as package names. Names beginning with <code>java.*</code>,
     * <code>javax.*</code>, and <code>com.sun.*</code>, are reserved for use by Sun Microsystems.
     * @param name                      a <code>String</code> specifying the name of the attribute to remove
     */
    public void removeAttribute(String name) {
        this.req.removeAttribute(name);
    }

    /**
     * Stores an attribute in this request. Attributes are reset between requests.  This method is most
     * often used in conjunction with {@link RequestDispatcher}. <p>Attribute names should follow the same conventions as
     * package names. Names beginning with <code>java.*</code>, <code>javax.*</code>, and <code>com.sun.*</code>, are
     * reserved for use by Sun Microsystems.
     * @param path                      a <code>String</code> specifying the name of the attribute
     * @param o                         the <code>Object</code> to be stored
     */
    public void setAttribute(String path, Object o) {
        this.req.setAttribute(path,o);
    }

    /**  */
    public Object getAttribute(String name) {
        return this.req.getAttribute(name);
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

    public boolean isRequestedSessionIdFromURL() {
        return this.req.isRequestedSessionIdFromURL();
    }

    /** @deprecated		As of Version 2.1 of the Java Servlet API, use {@link #isRequestedSessionIdFromURL} instead. */
    public boolean isRequestedSessionIdFromUrl() {
        return this.req.isRequestedSessionIdFromUrl();
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
        return parameters.keys();
    }

    /**
     * Returns the value of the named parameter as a String, or null if
     * the parameter was not sent or was sent without a value.  The value
     * is guaranteed to be in its normal, decoded form.  If the parameter has multiple values, only the firts one is returned.
     */
    public String getParameter(String name) {
        try {
            Vector values = (Vector)parameters.get(name);
            if (values == null || values.size() == 0) {
                return null;
            }
            String value = (String)values.elementAt(0);
            return value;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the values of the named parameter as a String array, or null if
     * the parameter was not sent.  The array has one entry for each parameter
     * field sent.  If any field was sent without a value that entry is stored
     * in the array as a null.  The values are guaranteed to be in their
     * normal, decoded form.  A single value is returned as a one-element array.
     */
    public String[] getParameterValues(String name) {
      try {
          Vector values = (Vector)parameters.get(name);
          if (values == null) {
              return null;
          }
        return (String[])values.toArray(new String[] {});
      } catch (Exception e) {
          return null;
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
