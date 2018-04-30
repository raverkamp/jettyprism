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
import java.io.OutputStream;
import java.math.BigInteger;

/*
 * This class stores the information of an upload file
 * This has all the information of the Multipart header of the file and
 * setter and getter methos for all the privates fields
 * Also returns a byte array with the information to be upload as BLOB
 * This class could be reused by any adapter which requires a byte array
 * to be upload to the database content repository
 */
public class UploadedFile {

    private UploadContent m_parent;
    private int m_startData;
    private int m_endData;
    private int m_size;
    private java.lang.String m_fieldname;
    private java.lang.String m_filename;
    private java.lang.String m_fileExt;
    private java.lang.String m_filePathName;
    private java.lang.String m_contentType;
    private java.lang.String m_contentDisp;
    private java.lang.String m_typeMime;
    private java.lang.String m_subTypeMime;
    private java.lang.String m_contentString;
    private boolean m_isMissing;
    public static final int SAVEAS_AUTO = 0;
    public static final int SAVEAS_VIRTUAL = 1;
    public static final int SAVEAS_PHYSICAL = 2;

    /*
 * Public Constructor
     */
    UploadedFile() {
        m_startData = 0;
        m_endData = 0;
        m_size = 0;
        m_fieldname = new String();
        m_filename = new String();
        m_fileExt = new String();
        m_filePathName = new String();
        m_contentType = new String();
        m_contentDisp = new String();
        m_typeMime = new String();
        m_subTypeMime = new String();
        m_contentString = new String();
        m_isMissing = true;
    }

    /*
 * Returns a byte array with the content to be upload to the repository
     */
    public byte[] fileToBlob() throws IOException {
        long numBlocks = 0L;
        int blockSize = 0x10000;
        int leftOver = 0;
        int pos = 0;
        OutputStream stream;
        numBlocks = BigInteger.valueOf(m_size).divide(BigInteger.valueOf(blockSize)).longValue();
        leftOver = BigInteger.valueOf(m_size).mod(BigInteger.valueOf(blockSize)).intValue();
        byte[] binByte2 = new byte[m_size];
        System.arraycopy(m_parent.m_binArray, m_startData, binByte2, 0, m_size);
        return binByte2;
    }

    /*
 *
     */
    public boolean isMissing() {
        return m_isMissing;
    }

    /*
 *
     */
    public String getFieldName() {
        return m_fieldname;
    }

    /*
 *
     */
    public String getFileName() {
        return m_filename;
    }

    /*
 *
     */
    public String getFilePathName() {
        return m_filePathName;
    }

    /*
 *
     */
    public String getFileExt() {
        return m_fileExt;
    }

    /*
 *
     */
    public String getContentType() {
        return m_contentType;
    }

    /*
 *
     */
    public String getContentDisp() {
        return m_contentDisp;
    }

    /*
 *
     */
    public String getContentString() {
        String strTMP = new String(m_parent.m_binArray, m_startData, m_size);
        return strTMP;
    }

    /*
 *
     */
    public String getTypeMIME() throws IOException {
        return m_typeMime;
    }

    /*
 *
     */
    public String getSubTypeMIME() {
        return m_subTypeMime;
    }

    /*
 *
     */
    public int getSize() {
        return m_size;
    }

    /*
 *
     */
    protected int getStartData() {
        return m_startData;
    }

    /*
 *
     */
    protected int getEndData() {
        return m_endData;
    }

    /*
 *
     */
    protected void setParent(UploadContent parent) {
        m_parent = parent;
    }

    /*
 *
     */
    protected void setStartData(int startData) {
        m_startData = startData;
    }

    /*
 *
     */
    protected void setEndData(int endData) {
        m_endData = endData;
    }

    /*
 *
     */
    protected void setSize(int size) {
        m_size = size;
    }

    /*
 *
     */
    protected void setIsMissing(boolean isMissing) {
        m_isMissing = isMissing;
    }

    /*
 *
     */
    protected void setFieldName(String fieldName) {
        m_fieldname = fieldName;
    }

    /*
 *
     */
    protected void setFileName(String fileName) {
        m_filename = fileName;
    }

    /*
 *
     */
    protected void setFilePathName(String filePathName) {
        m_filePathName = filePathName;
    }

    /*
 *
     */
    protected void setFileExt(String fileExt) {
        m_fileExt = fileExt;
    }

    /*
 *
     */
    protected void setContentType(String contentType) {
        m_contentType = contentType;
    }

    /*
 *
     */
    protected void setContentDisp(String contentDisp) {
        m_contentDisp = contentDisp;
    }

    /*
 *
     */
    protected void setTypeMIME(String TypeMime) {
        m_typeMime = TypeMime;
    }

    /*
 *
     */
    protected void setSubTypeMIME(String subTypeMime) {
        m_subTypeMime = subTypeMime;
    }

    /*
 *
     */
    public byte getBinaryData(int index) {
        if (m_startData + index > m_endData) {
            throw new ArrayIndexOutOfBoundsException("Index Out of range.");
        }
        if (m_startData + index <= m_endData) {
            return m_parent.m_binArray[m_startData + index];
        } else {
            return 0;
        }
    }
}
