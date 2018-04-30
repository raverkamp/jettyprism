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

import java.io.InputStream;
import java.io.StringReader;

/**
 * This class is container for inline downloading functionality Since DBPrism
 * 2.1.1 you can use inline downloading functionlity the following example:
 * procedure custom_image_display (p_image_id in number) as l_mime
 * varchar2(255); l_length number; l_file_name varchar2(2000); lob_loc BLOB;
 * begin select mime_type, image, image_name, dbms_lob.getlength(image) into
 * l_mime, lob_loc, l_file_name, l_length from demo_images where image_id =
 * p_image_id; owa_util.mime_header(nvl(l_mime,'application/octet'),FALSE );
 * download htp.p('Content-length: ' || l_length); owa_util.http_header_close;
 * wpg_docload.download_file( Lob_loc ); end; The example show an stored
 * procedure for downlading an image with an URL like this
 * /htmdb/SCOTT.custom_image_display?p_image_id=2 Note: Tha htp/owa calls are
 * only used to set the HTTP header, the content of the page is replaced by the
 * content of the BLOB setted by wpg_docload package.
 */
public class Content {

    /**
     * HTTP header and page
     */
    private StringReader page = null;

    /**
     * Download binary content using inline downloading and wpg_docload
     * procedures
     */
    private InputStream inputStream = null;

    public Content() {
    }

    /**
     * Setter for HTTP Header/Content
     *
     * @param page an StringReader with the content
     */
    public void setPage(StringReader page) {
        this.page = page;
    }

    /**
     * @return an StringReader object to consume the HTTP header/content
     */
    public StringReader getPage() {
        return page;
    }

    /**
     * If wpg_docload procedure was used, the
     *
     * @param inputStream is setted to hold the BLOB/BFILE downloaded
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return a binary InputStream to read the BLOB/BFILE content setted using
     * wpg_docload procedure
     */
    public InputStream getInputStream() {
        return inputStream;
    }
}
