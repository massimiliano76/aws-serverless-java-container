package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

public class AwsProxyHttpServletRequestTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "Custom-Header-Value";
    private static final String FORM_PARAM_NAME = "name";
    private static final String FORM_PARAM_NAME_VALUE = "Stef";
    private static final String FORM_PARAM_TEST = "test_cookie_param";
    private static final String QUERY_STRING_NAME_VALUE = "Bob";
    private static final String REQUEST_SCHEME_HTTP = "http";
    private static final String USER_AGENT = "Mozilla/5.0 (Android 4.4; Mobile; rv:41.0) Gecko/41.0 Firefox/41.0";
    private static final String REFERER = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent/Firefox";
    private static ZonedDateTime REQUEST_DATE = ZonedDateTime.now();

    private static final AwsProxyRequest REQUEST_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_INVALID_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "GET")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_FORM_URLENCODED_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_SINGLE_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MULTIPLE_COOKIES = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .cookie(FORM_PARAM_TEST, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MALFORMED_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.COOKIE, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MULTIPLE_FORM_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_TEST, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_USER_AGENT_REFERER = new AwsProxyRequestBuilder("/hello", "POST")
            .userAgent(USER_AGENT)
            .referer(REFERER).build();
    private static final AwsProxyRequest REQUEST_WITH_DATE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.DATE, AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE))
            .build();
    private static final AwsProxyRequest REQUEST_WITH_LOWERCASE_HEADER = new AwsProxyRequestBuilder("/hello", "POST")
            .header(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault()), MediaType.APPLICATION_JSON).build();

    private static final AwsProxyRequest REQUEST_NULL_QUERY_STRING;
    static {
        AwsProxyRequest awsProxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        awsProxyRequest.setQueryStringParameters(null);
        REQUEST_NULL_QUERY_STRING = awsProxyRequest;
    }

    private static final AwsProxyRequest REQUEST_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE).build();


    @Test
    public void headers_getHeader_validRequest() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
    }

    @Test
    public void headers_getRefererAndUserAgent_returnsContextValues() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_USER_AGENT_REFERER, null, null);
        assertNotNull(request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("referer"));

        assertNotNull(request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("user-agent"));
    }

    @Test
    public void formParams_getParameter_validForm() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getParameter(FORM_PARAM_NAME));
        assertEquals(FORM_PARAM_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_INVALID_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNull(request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_multipleParams() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
    }

    @Test
    public void formParams_getParameter_queryStringPrecendence() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void dateHeader_noDate_returnNegativeOne() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(-1L, request.getDateHeader(HttpHeaders.DATE));
    }

    @Test
    public void dateHeader_correctDate_parseToCorrectLong() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_WITH_DATE, null, null);
        assertNotNull(request);

        String instantString = AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE);
        assertEquals(Instant.from(AwsHttpServletRequest.dateFormatter.parse(instantString)).toEpochMilli(), request.getDateHeader(HttpHeaders.DATE));
        assertEquals(-1L, request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
    }

    @Test
    public void scheme_getScheme_https() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals("https", request.getScheme());
    }

    @Test
    public void scheme_getScheme_http() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals(REQUEST_SCHEME_HTTP, request.getScheme());
    }

    @Test
    public void cookie_getCookies_noCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void cookie_getCookies_singleCookie() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_SINGLE_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(1, request.getCookies().length);
        assertEquals(FORM_PARAM_NAME, request.getCookies()[0].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[0].getValue());
    }

    @Test
    public void cookie_getCookies_multipleCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MULTIPLE_COOKIES, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(2, request.getCookies().length);
        assertEquals(FORM_PARAM_NAME, request.getCookies()[0].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[0].getValue());
        assertEquals(FORM_PARAM_TEST, request.getCookies()[1].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[1].getValue());
    }

    @Test
    public void cookie_getCookies_emptyCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MALFORMED_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void queryParameters_getParameterMap_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_NULL_QUERY_STRING, null, null);
        assertNotNull(request);
        assertEquals(0, request.getParameterMap().size());
    }

    @Test
    public void queryParameters_getParameterMap_nonNull() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_QUERY, null, null);
        assertNotNull(request);
        assertEquals(1, request.getParameterMap().size());
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameterMap().get(FORM_PARAM_NAME)[0]);
    }

    @Test
    public void queryParameters_getParameterNames_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_NULL_QUERY_STRING, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(0, parameterNames.size());
    }

    @Test
    public void queryParameters_getParameterNames_notNull() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_QUERY, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(1, parameterNames.size());
        assertTrue(parameterNames.contains(FORM_PARAM_NAME));
    }

    @Test
    public void queryParameter_getParameterMap_avoidDuplicationOnMultipleCalls() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MULTIPLE_FORM_AND_QUERY, null, null);

        Map<String, String[]> params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertNotNull(params.get(FORM_PARAM_NAME));
        assertEquals(1, params.get(FORM_PARAM_NAME).length);
        assertNotNull(params.get(FORM_PARAM_TEST));
        assertEquals(1, params.get(FORM_PARAM_TEST).length);

        params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertNotNull(params.get(FORM_PARAM_NAME));
        assertEquals(1, params.get(FORM_PARAM_NAME).length);
        assertNotNull(params.get(FORM_PARAM_TEST));
        assertEquals(1, params.get(FORM_PARAM_TEST).length);
    }

    @Test
    public void charEncoding_getEncoding_expectNoEncodingWithoutContentType() {
         HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_SINGLE_COOKIE, null, null);
         try {
             request.setCharacterEncoding(StandardCharsets.UTF_8.name());
             // we have not specified a content type so the encoding will not be set
             assertEquals(null, request.getCharacterEncoding());
             assertEquals(null, request.getContentType());
         } catch (UnsupportedEncodingException e) {
             fail("Unsupported encoding");
             e.printStackTrace();
         }
    }

    @Test
    public void charEncoding_getEncoding_expectContentTypeOnly() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        // we have not specified a content type so the encoding will not be set
        assertEquals(null, request.getCharacterEncoding());
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void charEncoding_addCharEncodingTwice_expectSingleMediaTypeAndEncoding() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        // we have not specified a content type so the encoding will not be set
        assertEquals(null, request.getCharacterEncoding());
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());

        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());


            request.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.ISO_8859_1.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.ISO_8859_1.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void contentType_lowerCaseHeaderKey_expectUpdatedMediaType() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_WITH_LOWERCASE_HEADER, null, null);
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());

        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void contentType_duplicateCase_expectSingleContentTypeHeader() {
        AwsProxyRequest proxyRequest = getRequestWithHeaders();
        HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);

        try {
            request.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault())));

            assertFalse(proxyRequest.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE) && proxyRequest.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault())));
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }



    }

    private AwsProxyRequest getRequestWithHeaders() {
        return new AwsProxyRequestBuilder("/hello", "GET")
                       .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                       .header(AwsProxyHttpServletRequest.CF_PROTOCOL_HEADER_NAME, REQUEST_SCHEME_HTTP)
                       .build();
    }
}
