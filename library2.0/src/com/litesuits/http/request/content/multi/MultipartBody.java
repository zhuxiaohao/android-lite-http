package com.litesuits.http.request.content.multi;

import com.litesuits.http.data.Consts;
import com.litesuits.http.request.content.HttpBody;

import java.io.*;
import java.util.LinkedList;

/**
 * @author MaTianyu
 * @date 14-7-29
 */
public class MultipartBody extends HttpBody {
    private LinkedList<AbstractPart> httpParts = new LinkedList<AbstractPart>();
    private String boundary;
    private byte[] boundaryLine;
    private byte[] boundaryEnd;
    private long bytesWritten;
    private long totalSize;

    public MultipartBody() {
        BoundaryCreater boundaryCreater = new BoundaryCreater();
        boundary = boundaryCreater.getBoundary();
        boundaryLine = boundaryCreater.getBoundaryLine();
        boundaryEnd = boundaryCreater.getBoundaryEnd();
        contentType = Consts.MIME_TYPE_FORM_DATA + Consts.BOUNDARY_PARAM + boundary;
    }

    public LinkedList<AbstractPart> getHttpParts() {
        return httpParts;
    }

    public MultipartBody setHttpParts(LinkedList<AbstractPart> httpParts) {
        this.httpParts = httpParts;
        return this;
    }

    public MultipartBody addPart(String key, String string, String charset, String mimeType) throws
            UnsupportedEncodingException {
        return addPart(new StringPart(key, string, charset, mimeType));
    }

    public MultipartBody addPart(String key, byte[] bytes, String mimeType) {
        return addPart(new BytesPart(key, bytes, mimeType));
    }

    public MultipartBody addPart(String key, File file, String mimeType) throws FileNotFoundException {
        return addPart(new FilePart(key, file, mimeType));
    }

    public MultipartBody addPart(String key, InputStream inputStream, String fileName, String mimeType) {
        return addPart(new InputStreamPart(key, inputStream, fileName, mimeType));
    }

    public MultipartBody addPart(AbstractPart part) {
        if (part == null) {
            return this;
        }
        // note that: set multibody to every part, so that we can get progress of these part.
        part.setMultipartBody(this);
        part.createHeader(boundaryLine);
        httpParts.add(part);
        return this;
    }

    public long getContentLength() {
        long contentLen = -1;
        try {
            for (AbstractPart part : httpParts) {
                long len = 0;
                len = part.getTotalLength();
                if (len < 0) {
                    return -1;
                }
                contentLen += len;
            }
            contentLen += boundaryEnd.length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentLen;
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        bytesWritten = 0;
        totalSize = (int) getContentLength();
        for (AbstractPart part : httpParts) {
            part.writeToServer(outstream);
        }
        outstream.write(boundaryEnd);
        updateProgress(boundaryEnd.length);
    }

    @SuppressWarnings("unchecked")
    protected void updateProgress(long count) {
        bytesWritten += count;
        if (httpListener != null) {
            httpListener.uploading(request, totalSize, bytesWritten);
        }
    }

    public String getBoundary() {
        return boundary;
    }

    public byte[] getBoundaryLine() {
        return boundaryLine;
    }

    public byte[] getBoundaryEnd() {
        return boundaryEnd;
    }
}
