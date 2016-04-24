/**
 ****************************************************************************
 * Copyright (C) Marcelo F. Ochoa. All rights reserved.                      *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 */

package com.prism;

public class ExecutionErrorPageException extends java.lang.Exception {
    java.lang.String msg;

    public ExecutionErrorPageException(String m) {
        msg = m;
    }

    public String getMessage() {
        return msg;
    }
}
