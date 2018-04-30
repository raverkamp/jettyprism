/**
 ********************************************************************************
 * Copyright (C) Charly Schmid (charly.schmid@trivadis.com). All rights reserved.*
 * ------------------------------------------------------------------------------*
 * This software is published under the terms of the Apache Software License     *
 * version 1.1, a copy of which has been included  with this distribution in     *
 * the LICENSE file.                                                             *
 */
package com.prism;

/*
 * Exception to be trap if an upload problem occurs
 */
public class UploadException extends Exception {

    public UploadException(String desc) {
        super(desc);
    }
}
