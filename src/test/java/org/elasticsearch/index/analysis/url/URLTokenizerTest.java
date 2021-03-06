package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.analysis.url.IsTokenizerWithTokenAndPosition.hasTokenAtOffset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

/**
 * Joe Linn
 * 7/30/2015
 */
public class URLTokenizerTest extends BaseTokenStreamTestCase {
    public static final String TEST_HTTP_URL = "http://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";
    public static final String TEST_HTTPS_URL = "https://www.foo.bar.com:9200/index_name/type_name/_search.html?foo=bar&baz=bat#tag";


    @Test
    public void testTokenizeProtocol() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(tokenizer, "http");

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PROTOCOL);
        assertThat(tokenizer, hasTokenAtOffset("http", 0, 4));

        tokenizer = createTokenizer(TEST_HTTPS_URL, URLPart.PROTOCOL);
        assertTokenStreamContents(tokenizer, "https");
    }


    @Test
    public void testTokenizeHost() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertTokenStreamContents(tokenizer, stringArray("www.foo.bar.com", "foo.bar.com", "bar.com", "com"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("www.foo.bar.com", 7, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("foo.bar.com", 11, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("bar.com", 15, 22));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.HOST);
        assertThat(tokenizer, hasTokenAtOffset("com", 19, 22));
    }


    @Test
    public void testTokenizePort() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PORT);
        assertThat(tokenizer, hasTokenAtOffset("9200", 23, 27));

        tokenizer = createTokenizer("http://foo.bar.com", URLPart.PORT);
        assertThat(tokenizer, hasTokenAtOffset("80", 0, 0));
    }


    @Test
    public void testTokenizePath() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertTokenStreamContents(tokenizer, stringArray("/index_name", "/index_name/type_name", "/index_name/type_name/_search.html"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name", 27, 38));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name/type_name", 27, 48));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.PATH);
        assertThat(tokenizer, hasTokenAtOffset("/index_name/type_name/_search.html", 27, 61));

        tokenizer.reset();
        tokenizer.setReader(new StringReader(TEST_HTTPS_URL));
        tokenizer.setTokenizePath(false);

        assertTokenStreamContents(tokenizer, stringArray("/index_name/type_name/_search.html"));
    }


    @Test
    public void testTokenizeQuery() throws IOException {
        URLTokenizer tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertTokenStreamContents(tokenizer, stringArray("foo=bar", "baz=bat"));

        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertThat(tokenizer, hasTokenAtOffset("foo=bar", 62, 69));
        tokenizer = createTokenizer(TEST_HTTP_URL, URLPart.QUERY);
        assertThat(tokenizer, hasTokenAtOffset("baz=bat", 70, 77));
    }


    @Test
    public void testTokenizeRef() throws IOException {
        URLTokenizer tokenizer = createTokenizer("http://foo.com#baz", URLPart.REF);
        assertThat(tokenizer, hasTokenAtOffset("baz", 15, 18));
    }


    @Test
    public void testAll() throws IOException {
        URLTokenizer tokenizer = new URLTokenizer();
        tokenizer.setReader(new StringReader(TEST_HTTPS_URL));
        CharTermAttribute termAttribute = tokenizer.getAttribute(CharTermAttribute.class);
        tokenizer.reset();
        tokenizer.clearAttributes();
        List<String> tokens = new ArrayList<>();
        while(tokenizer.incrementToken()){
            tokens.add(termAttribute.toString());
        }

        assertThat(tokens, hasItem(equalTo("https")));
        assertThat(tokens, hasItem(equalTo("foo.bar.com")));
        assertThat(tokens, hasItem(equalTo("www.foo.bar.com:9200")));
        assertThat(tokens, hasItem(equalTo("https://www.foo.bar.com")));

        tokenizer = createTokenizer("https://foo.com", null);
        assertThat(tokenizer, hasTokenAtOffset("https", 0, 5));
    }


    @Test(expected = IOException.class)
    public void testMalformed() throws IOException {
        URLTokenizer tokenizer = createTokenizer("://foo.com", URLPart.QUERY);
        assertTokenStreamContents(tokenizer, stringArray("foo=bar", "baz=bat"));
    }


    @Test
    public void testAllowMalformed() throws IOException {
        URLTokenizer tokenizer = createTokenizer("://foo.com", URLPart.QUERY);
        tokenizer.setAllowMalformed(true);
        assertTokenStreamContents(tokenizer, stringArray("://foo.com"));
    }


    private URLTokenizer createTokenizer(String input, URLPart part) throws IOException {
        URLTokenizer tokenizer = new URLTokenizer(part);
        tokenizer.setReader(new StringReader(input));
        return tokenizer;
    }


    private String[] stringArray(String... strings) {
        return strings;
    }


    private static void assertTokenStreamContents(TokenStream in, String output) throws IOException {
        assertTokenStreamContents(in, new String[]{output});
    }
}