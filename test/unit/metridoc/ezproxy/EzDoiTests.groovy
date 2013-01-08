package metridoc.ezproxy

import grails.test.mixin.TestFor
import org.apache.commons.lang.StringUtils
import org.junit.Test

@TestFor(EzDoi)
class EzDoiTests {

    @Test
    void "if the record has a url then has url is true, otherwise false"() {
        hasUrlTest([:], false)
        hasUrlTest([url: StringUtils.EMPTY], false)
        hasUrlTest([url: "   \n  "], false)
        hasUrlTest([url: "-"], false)
        hasUrlTest([url: "   -  "], false)
        hasUrlTest([url: "http://foo.com10."], true)
    }

    @Test
    void "has doi returns true if a doi exists in url, false otherwise, input is a valid non empty string"() {
        hasDoiTest([url: "http://foo.com"], false)
        hasDoiTest([url: "http://foo.com10."], true)
    }

    @Test
    void "is a url returns true if the url is actually a url, it is assumed that url is a string that contains the doi pattern"() {
        urlIsAUrlTest([url: "http://foo.com10."], true)
        urlIsAUrlTest([url: "foo.com10."], false)
    }

    @Test
    void "invalid record should be valid"() {
        def invalidRecord = new EzDoi().createDefaultInvalidRecord()
        invalidRecord.validate()
        assert invalidRecord.errors.allErrors.size() == 0
    }

    @Test
    void "test doi extractions"() {
        assert '10.1021/jo0601009' == EzDoi.extractDoi('http://pubs.acs.org:80/doi/full/10.1021/jo0601009')
        assert '10.1002/(ISSN)1531-4995' == EzDoi.extractDoi('http://onlinelibrary.wiley.com:80?doi=10.1002%2F%28ISSN%291531-4995&simpleSearchError=Please+remove')
        assert null == EzDoi.extractDoi('http://foo.com')
        //odd ball cases
        assert '10.' == EzDoi.extractDoi('http://foo.com10.')
        assert '10.' == EzDoi.extractDoi('http://foo.com?doi=10.&stuff')
    }

    static void hasUrlTest(Map record, boolean expected) {
        doTest("hasUrl", record, expected)
    }

    static void hasDoiTest(Map record, boolean expected) {
        doTest("hasDoi", record, expected)
    }

    static void urlIsAUrlTest(Map record, boolean expected) {
        doTest("urlIsAUrl", record, expected)
        assert expected == new EzDoi().accept(record)
    }

    static void doTest(String method, Map record, boolean expected) {
        assert expected == EzDoi."$method"(record)
    }
}