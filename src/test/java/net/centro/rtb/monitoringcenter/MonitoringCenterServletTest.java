package net.centro.rtb.monitoringcenter;

import org.junit.*;
import org.junit.runner.RunWith;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;

import static org.mockito.Mockito.*;

@RunWith(SeparateClassloaderTestRunner.class)
public class MonitoringCenterServletTest {

    private static MonitoringCenterServlet monitoringCenterServlet;

    class CacheOutputStream extends ServletOutputStream {
        private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        private ByteArrayOutputStream getBuffer() throws IOException {
            return byteArrayOutputStream;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int b) throws IOException {
            byteArrayOutputStream.write(b);
        }
    }

    class CacheWriter extends Writer {
        private StringWriter stringWriter = new StringWriter();

        private StringWriter getBuffer() throws IOException {
            return stringWriter;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            stringWriter.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Before
    public void setUp() {
        monitoringCenterServlet = new MonitoringCenterServlet();
    }

    @After
    public void tearDown() {
        monitoringCenterServlet.destroy();
    }

    @Test
    public void init() throws Exception {
        ServletConfig servletConfig = mock(ServletConfig.class);

        ServletContext servletContext = mock(ServletContext.class);

        when(servletConfig.getInitParameter("username")).thenReturn("admin");
        when(servletConfig.getInitParameter("password")).thenReturn("god");
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletContext().getMajorVersion()).thenReturn(1);
        when(servletConfig.getServletContext().getMinorVersion()).thenReturn(2);
        when(servletConfig.getServletContext().getServerInfo()).thenReturn("Mock");

        monitoringCenterServlet.init(servletConfig);

        Assert.assertEquals(1, monitoringCenterServlet.getServletContext().getMajorVersion());
        Assert.assertEquals(2, monitoringCenterServlet.getServletContext().getMinorVersion());
        Assert.assertEquals("admin", monitoringCenterServlet.getInitParameter("username"));
        Assert.assertEquals("god", monitoringCenterServlet.getInitParameter("password"));
    }

    @Test
    public void doGet() throws Exception {

        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletContext().getMajorVersion()).thenReturn(1);
        when(servletConfig.getServletContext().getMinorVersion()).thenReturn(2);
        when(servletConfig.getServletContext().getServerInfo()).thenReturn("Mock");

        monitoringCenterServlet.init(servletConfig);

        CacheOutputStream outputStream = new CacheOutputStream();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getPathInfo()).thenReturn("/metrics");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("{\"gauges\":{},\"counters\":{},\"histograms\":{},\"meters\":{},\"timers\":{}}", outputStream.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/healthChecks");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("{}", outputStream.getBuffer().toString());

        CacheWriter cacheWriter = new CacheWriter();
        PrintWriter printWriter = new PrintWriter(cacheWriter);

        when(request.getPathInfo()).thenReturn("/ping");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getWriter()).thenReturn(printWriter);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("pong\n", cacheWriter.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/threadDump");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertNotNull(outputStream.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/systemInfo");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("null", outputStream.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/nodeInfo");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("null", outputStream.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/serverInfo");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("{\"nameAndVersion\":\"Mock\",\"servletSpecVersion\":\"1.2\"}", outputStream.getBuffer().toString());

        outputStream = new CacheOutputStream();

        when(request.getPathInfo()).thenReturn("/appInfo");
        when(request.getHeader("Authorization")).thenReturn("BASIC bW9uaXRvcmluZ0NlbnRlcjpkM2ZhVWx0flA0U3N3MHJE");
        when(response.getOutputStream()).thenReturn(outputStream);

        monitoringCenterServlet.doGet(request, response);

        Assert.assertEquals("null", outputStream.getBuffer().toString());
    }
}