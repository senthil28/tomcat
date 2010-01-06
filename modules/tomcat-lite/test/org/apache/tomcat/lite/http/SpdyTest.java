/*
 */
package org.apache.tomcat.lite.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.tomcat.lite.TestMain;
import org.apache.tomcat.lite.io.IOBuffer;
import org.apache.tomcat.lite.http.SpdyConnection.SpdyConnectionManager;

public class SpdyTest extends TestCase {
    HttpConnector http11Con = TestMain.shared().getClient();
    
    static HttpConnector spdyCon = DefaultHttpConnector.get()
        .withConnectionManager(new SpdyConnectionManager());
    
    HttpConnector memSpdyCon = 
        new HttpConnector(null).withConnectionManager(new SpdyConnectionManager());
    
    public void testClient() throws IOException {
        HttpRequest req = 
            spdyCon.request("http://localhost:8802/echo/test1");
        
        HttpResponse res = req.waitResponse();
        
        assertEquals(200, res.getStatus());
        //assertEquals("", res.getHeader(""));
        
        BufferedReader reader = res.getReader();
        String line1 = reader.readLine();
        //assertEquals("", line1);        
    }
    
    // Initial frame generated by Chrome
    public void testParse() throws IOException {
            InputStream is = 
            getClass().getClassLoader().getResourceAsStream("org/apache/tomcat/lite/http/spdyreq0");
        
        IOBuffer iob = new IOBuffer();
        iob.append(is);
        
        SpdyConnection con = (SpdyConnection) memSpdyCon.newConnection();
        
        // By default it has a dispatcher buit-in 
        con.serverMode = true;
        
        con.dataReceived(iob);
        
        HttpChannel spdyChannel = con.channels.get(1);

        assertEquals(1, con.lastFrame.version);
        assertEquals(1, con.lastFrame.type);
        assertEquals(1, con.lastFrame.flags);

        assertEquals(417, con.lastFrame.length);

        // TODO: test req, headers
        HttpRequest req = spdyChannel.getRequest();
        assertTrue(req.getHeader("accept").indexOf("application/xml") >= 0);
        
    }
    
    // Initial frame generated by Chrome
    public void testParseCompressed() throws IOException {
        InputStream is = 
            getClass().getClassLoader().getResourceAsStream("org/apache/tomcat/lite/http/spdyreqCompressed");
        
        IOBuffer iob = new IOBuffer();
        iob.append(is);
        
        SpdyConnection con = (SpdyConnection) memSpdyCon.newConnection();
        
        // By default it has a dispatcher buit-in 
        con.serverMode = true;
        
        con.dataReceived(iob);
        
        HttpChannel spdyChannel = con.channels.get(1);

        assertEquals(1, con.lastFrame.version);
        assertEquals(1, con.lastFrame.type);
        assertEquals(1, con.lastFrame.flags);

        // TODO: test req, headers
        HttpRequest req = spdyChannel.getRequest();
        assertTrue(req.getHeader("accept").indexOf("application/xml") >= 0);
        
    }
    
    // Does int parsing works ?
    public void testLargeInt() throws Exception {
        
        IOBuffer iob = new IOBuffer();
        iob.append(0xFF);
        iob.append(0xFF);
        iob.append(0xFF);
        iob.append(0xFF);

        iob.append(0xFF);
        iob.append(0xFF);
        iob.append(0xFF);
        iob.append(0xFF);

        SpdyConnection con = (SpdyConnection) memSpdyCon.newConnection();
        con.dataReceived(iob);
        assertEquals(0x7FFF, con.currentInFrame.version);
        assertEquals(0xFFFF, con.currentInFrame.type);
        assertEquals(0xFF, con.currentInFrame.flags);
        assertEquals(0xFFFFFF, con.currentInFrame.length);
        
    }
}
