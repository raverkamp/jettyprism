/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */
package com.prism;

public class NotAuthorizedException extends java.lang.Exception {

    java.lang.String msg = null;

    public NotAuthorizedException(String str) {
        msg = str;
    }

    public String getMessage() {
        return msg;
    }
}
