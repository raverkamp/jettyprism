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
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.prism.UploadException;

public class UploadContent {

    protected byte[] m_binArray;
    protected HttpServletRequest m_request;
    protected HttpServletResponse m_response;
    private int m_totalBytes;
    private int m_currentIndex;
    private int m_startData;
    private int m_endData;
    private java.lang.String m_boundary;
    private long m_totalMaxFileSize;
    private long m_maxFileSize;
    private Vector m_deniedFilesList;
    private Vector m_allowedFilesList;
    private boolean m_denyPhysicalPath;
    private boolean m_forcePhysicalPath;
    private java.lang.String m_contentDisposition;
    public static final int SAVE_AUTO = 0;
    public static final int SAVE_VIRTUAL = 1;
    public static final int SAVE_PHYSICAL = 2;
    private UploadedFiles m_files;
    public RequestParameters m_formRequest;

    public UploadContent() {
        m_totalBytes = 0;
        m_currentIndex = 0;
        m_startData = 0;
        m_endData = 0;
        m_boundary = new String();
        m_totalMaxFileSize = 0L;
        m_maxFileSize = 0L;
        m_deniedFilesList = new Vector();
        m_allowedFilesList = new Vector();
        m_denyPhysicalPath = false;
        m_forcePhysicalPath = false;
        m_contentDisposition = new String();
        m_files = new UploadedFiles();
        m_formRequest = new RequestParameters();
    }

    public final void initialize(HttpServletRequest request) throws ServletException {
        m_request = request;
    }

    public void upload() throws UploadException, IOException, ServletException {
        int totalRead = 0;
        int readBytes = 0;
        long totalFileSize = 0L;
        boolean found = false;
        String dataHeader = new String();
        String fieldName = new String();
        String fileName = new String();
        String fileExt = new String();
        String filePathName = new String();
        String contentType = new String();
        String contentDisp = new String();
        String typeMIME = new String();
        String subTypeMIME = new String();
        boolean isFile = false;
        m_totalBytes = m_request.getContentLength();
        m_binArray = new byte[m_totalBytes];
        for (; totalRead < m_totalBytes; totalRead += readBytes) {
            try {
                m_request.getInputStream();
                readBytes = m_request.getInputStream().read(m_binArray, totalRead, m_totalBytes - totalRead);
            } catch (Exception e) {
                throw new UploadException("Unable to upload.");
            }
        }
        for (; !found && m_currentIndex < m_totalBytes; m_currentIndex++) {
            if (m_binArray[m_currentIndex] == 13) {
                found = true;
            } else {
                m_boundary = m_boundary + (char) m_binArray[m_currentIndex];
            }
        }
        if (m_currentIndex == 1) {
            return;
        }
        m_currentIndex++;
        do {
            if (m_currentIndex >= m_totalBytes) {
                break;
            }
            dataHeader = getDataHeader();
            m_currentIndex = m_currentIndex + 2;
            isFile = dataHeader.indexOf("filename") > 0;
            fieldName = getDataFieldValue(dataHeader, "name");
            if (isFile) {
                filePathName = getDataFieldValue(dataHeader, "filename");
                fileName = getFileName(filePathName);
                fileExt = getFileExt(fileName);
                contentType = getContentType(dataHeader);
                contentDisp = getContentDisp(dataHeader);
                typeMIME = getTypeMIME(contentType);
                subTypeMIME = getSubTypeMIME(contentType);
            }
            getDataSection();
            if (isFile && fileName.length() > 0) {
                if (m_deniedFilesList.contains(fileExt)) {
                    throw new SecurityException("The extension of the file is denied to be uploaded (1015).");
                }
                if (!m_allowedFilesList.isEmpty() && !m_allowedFilesList.contains(fileExt)) {
                    throw new SecurityException("The extension of the file is not allowed to be uploaded (1010).");
                }
                if (m_maxFileSize > (long) 0 && (long) ((m_endData - m_startData) + 1) > m_maxFileSize) {
                    throw new SecurityException(String.valueOf((new StringBuffer("Size exceeded for this file : ")).append(fileName).append(" (1105).")));
                }
                totalFileSize += (m_endData - m_startData) + 1;
                if (m_totalMaxFileSize > (long) 0 && totalFileSize > m_totalMaxFileSize) {
                    throw new SecurityException("Total File Size exceeded (1110).");
                }
            }
            if (isFile) {
                UploadedFile newFile = new UploadedFile();
                newFile.setParent(this);
                newFile.setFieldName(fieldName);
                newFile.setFileName(fileName);
                newFile.setFileExt(fileExt);
                newFile.setFilePathName(filePathName);
                newFile.setIsMissing(filePathName.length() == 0);
                newFile.setContentType(contentType);
                newFile.setContentDisp(contentDisp);
                newFile.setTypeMIME(typeMIME);
                newFile.setSubTypeMIME(subTypeMIME);
                if (contentType.indexOf("application/x-macbinary") > 0) {
                    m_startData = m_startData + 128;
                }
                newFile.setSize((m_endData - m_startData) + 1);
                newFile.setStartData(m_startData);
                newFile.setEndData(m_endData);
                m_files.addFile(newFile);
            } else {
                String value = new String(m_binArray, m_startData, (m_endData - m_startData) + 1);
                m_formRequest.putParameter(fieldName, value);
            }
            if ((char) m_binArray[m_currentIndex + 1] == '-') {
                break;
            }
            m_currentIndex = m_currentIndex + 2;
        } while (true);
    }

    public int getSize() {
        return m_totalBytes;
    }

    public byte getBinaryData(int index) {
        byte retval;
        try {
            retval = m_binArray[index];
        } catch (Exception e) {
            throw new ArrayIndexOutOfBoundsException("Index out of range (1005).");
        }
        return retval;
    }

    public UploadedFiles getFiles() {
        return m_files;
    }

    public RequestParameters getRequest() {
        return m_formRequest;
    }

    private String getDataFieldValue(String dataHeader, String fieldName) {
        String token = new String();
        String value = new String();
        int pos = 0;
        int i = 0;
        int start = 0;
        int end = 0;
        token = String.valueOf((new StringBuffer(String.valueOf(fieldName))).append("=").append('"'));
        pos = dataHeader.indexOf(token);
        if (pos > 0) {
            i = pos + token.length();
            start = i;
            token = "\"";
            end = dataHeader.indexOf(token, i);
            if (start > 0 && end > 0) {
                value = dataHeader.substring(start, end);
            }
        }
        return value;
    }

    private String getFileExt(String fileName) {
        String value = new String();
        int start = 0;
        int end = 0;
        if (fileName == null) {
            return null;
        }
        start = fileName.lastIndexOf(46) + 1;
        end = fileName.length();
        value = fileName.substring(start, end);
        if (fileName.lastIndexOf(46) > 0) {
            return value;
        } else {
            return "";
        }
    }

    private String getContentType(String dataHeader) {
        String token = new String();
        String value = new String();
        int start = 0;
        int end = 0;
        token = "Content-Type:";
        start = dataHeader.indexOf(token) + token.length();
        if (start != -1) {
            end = dataHeader.length();
            value = dataHeader.substring(start, end);
        }
        return value;
    }

    private String getTypeMIME(String ContentType) {
        String value = new String();
        int pos = 0;
        pos = ContentType.indexOf("/");
        if (pos != -1) {
            return ContentType.substring(1, pos);
        } else {
            return ContentType;
        }
    }

    private String getSubTypeMIME(String ContentType) {
        String value = new String();
        int start = 0;
        int end = 0;
        start = ContentType.indexOf("/") + 1;
        if (start != -1) {
            end = ContentType.length();
            return ContentType.substring(start, end);
        } else {
            return ContentType;
        }
    }

    private String getContentDisp(String dataHeader) {
        String value = new String();
        int start = 0;
        int end = 0;
        start = dataHeader.indexOf(":") + 1;
        end = dataHeader.indexOf(";");
        value = dataHeader.substring(start, end);
        return value;
    }

    private void getDataSection() {
        boolean found = false;
        String dataHeader = new String();
        int searchPos = m_currentIndex;
        int keyPos = 0;
        int boundaryLen = m_boundary.length();
        m_startData = m_currentIndex;
        m_endData = 0;
        do {
            if (searchPos >= m_totalBytes) {
                break;
            }
            if (m_binArray[searchPos] == (byte) m_boundary.charAt(keyPos)) {
                if (keyPos == boundaryLen - 1) {
                    m_endData = ((searchPos - boundaryLen) + 1) - 3;
                    break;
                }
                searchPos++;
                keyPos++;
            } else {
                searchPos++;
                keyPos = 0;
            }
        } while (true);
        m_currentIndex = m_endData + boundaryLen + 3;
    }

    private String getDataHeader() {
        int start = m_currentIndex;
        int end = 0;
        int len = 0;
        boolean found = false;
        while (!found) {
            if (m_binArray[m_currentIndex] == 13 && m_binArray[m_currentIndex + 2] == 13) {
                found = true;
                end = m_currentIndex - 1;
                m_currentIndex = m_currentIndex + 2;
            } else {
                m_currentIndex++;
            }
        }
        String dataHeader = new String(m_binArray, start, (end - start) + 1);
        return dataHeader;
    }

    private String getFileName(String filePathName) {
        String token = new String();
        String value = new String();
        int pos = 0;
        int i = 0;
        int start = 0;
        int end = 0;
        pos = filePathName.lastIndexOf(47);
        if (pos != -1) {
            return filePathName.substring(pos + 1, filePathName.length());
        }
        pos = filePathName.lastIndexOf(92);
        if (pos != -1) {
            return filePathName.substring(pos + 1, filePathName.length());
        } else {
            return filePathName;
        }
    }

    public void setDeniedFilesList(String deniedFilesList) throws IOException, ServletException {
        String ext = "";
        if (deniedFilesList != null) {
            ext = "";
            for (int i = 0; i < deniedFilesList.length(); i++) {
                if (deniedFilesList.charAt(i) == ',') {
                    if (!m_deniedFilesList.contains(ext)) {
                        m_deniedFilesList.addElement(ext);
                    }
                    ext = "";
                } else {
                    ext = ext + deniedFilesList.charAt(i);
                }
            }
            if (ext != "") {
                m_deniedFilesList.addElement(ext);
            }
        } else {
            m_deniedFilesList = null;
        }
    }

    public void setAllowedFilesList(String allowedFilesList) {
        String ext = "";
        if (allowedFilesList != null) {
            ext = "";
            for (int i = 0; i < allowedFilesList.length(); i++) {
                if (allowedFilesList.charAt(i) == ',') {
                    if (!m_allowedFilesList.contains(ext)) {
                        m_allowedFilesList.addElement(ext);
                    }
                    ext = "";
                } else {
                    ext = ext + allowedFilesList.charAt(i);
                }
            }
            if (ext != "") {
                m_allowedFilesList.addElement(ext);
            }
        } else {
            m_allowedFilesList = null;
        }
    }

    public void setDenyPhysicalPath(boolean deny) {
        m_denyPhysicalPath = deny;
    }

    public void setForcePhysicalPath(boolean force) {
        m_forcePhysicalPath = force;
    }

    public void setContentDisposition(String contentDisposition) {
        m_contentDisposition = contentDisposition;
    }

    public void setTotalMaxFileSize(long totalMaxFileSize) {
        m_totalMaxFileSize = totalMaxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        m_maxFileSize = maxFileSize;
    }

    public void setParameter(String name, String value) {
        m_formRequest.putParameter(name, value);
    }
}
