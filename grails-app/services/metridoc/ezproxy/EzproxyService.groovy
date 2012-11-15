package metridoc.ezproxy

import static org.apache.commons.lang.SystemUtils.*
import grails.util.Holders

/**
 * base ezproxy service that handles maintaining / storing parsers and raw data
 */
class EzproxyService {

    def grailsApplication
    def parserText
    def parserObject
    def parserException
    static final PARSER_PROPERTY = "metridoc.ezproxy.parser"
    static final RAW_DATA_PROPERTY = "metridoc.ezproxy.rawData"
    static final EZPROXY_DIRECTORY_PROPERTY = "metridoc.ezproxy.directory"
    static final DEFAULT_EZPROXY_DIRECTORY = "${USER_HOME}${FILE_SEPARATOR}.metridoc${FILE_SEPARATOR}files${FILE_SEPARATOR}ezproxy"
    static final CHARSET = "utf-8"
    static final DEFAULT_FILE_FILTER = /ezproxy\.log\.\d{8}\.gz/
    static final FILE_FILTER_PROPERTY = "metridoc.ezproxy.fileFilter"
    static datasource

    static {
        def grailsApplication = Holders.grailsApplication

        if (grailsApplication) {
            if(grailsApplication.mergedConfig.dataSource_ezproxy) {
                datasource = 'ezproxy'
            }
        }
    }

    def getRawSampleData() {
        def propertyDomain = EzProperties.find {
            propertyName == RAW_DATA_PROPERTY
        }

        if(propertyDomain) {
            return propertyDomain.propertyValue
        }

        return grailsApplication.mergedConfig.metridoc.ezproxy.sampleLog
    }

    def getRawParser() {
        def propertyDomain = EzProperties.find {
            propertyName == PARSER_PROPERTY
        }

        if(propertyDomain) {
            return propertyDomain.propertyValue
        }

        return grailsApplication.mergedConfig.metridoc.ezproxy.sampleParser
    }

    def updateParser(parserText) {
        def propertyDomain = EzProperties.find {
            propertyName == PARSER_PROPERTY
        }

        if(propertyDomain) {
            propertyDomain.propertyValue = parserText
        } else {
            new EzProperties(propertyName: PARSER_PROPERTY, propertyValue: parserText).save()
        }
    }

    //TODO: use dry to clean this up
    def updateSampleData(rawData) {
        def propertyDomain = EzProperties.find {
            propertyName == RAW_DATA_PROPERTY
        }

        if(propertyDomain) {
            propertyDomain.propertyValue = rawData
        } else {
            new EzProperties(propertyName: RAW_DATA_PROPERTY, propertyValue: rawData).save()
        }
    }

    def hasParser() {
        EzProperties.find {
            propertyName == PARSER_PROPERTY
        } ? true : false
    }

    def hasData() {
        EzProperties.find {
            propertyName == RAW_DATA_PROPERTY
        } ? true : false
    }

    def getParsedData() {

        def parserObject = getParserObject()
        def results = [:]
        results.rows = []
        results.headers = []

        getRawSampleData().eachLine {line, lineNumber ->
            def row = parserObject.parse(line, lineNumber+1, "ezproxy.file") as Map
            if (!results.headers) {
                results.headers = row.keySet()
            }
            results.rows << row.values()
        }

        return results
    }

    def getParserObject() {
        parserException = null
        def storedText = EzProperties.find {
            propertyName == PARSER_PROPERTY
        }.propertyValue

        if(shouldRebuildParser(storedText)) {
            parserText = storedText
            try {
                rebuildParser(storedText)
            } catch (Throwable t) {
                parserText = null
                throw t
            }
        }

        return parserObject
    }

    def rebuildParser(String parserText) {
        def parserTemplate = grailsApplication.mergedConfig.metridoc.ezproxy.ezproxyParserTemplate
        parserObject = buildParser(parserText, parserTemplate)
    }

    def buildParser(String parserText, Closure parserTemplate) {
        def code = parserTemplate.call(parserText)
        def classLoader = Thread.currentThread().contextClassLoader
        new GroovyClassLoader(classLoader).parseClass(code).newInstance()
    }

    def shouldRebuildParser(String storedparser) {
        parserText != storedparser
    }

    def getEzproxyFiles() {
        def result = []

        new File(getEzproxyDirectory()).listFiles().each {
            if(it.isFile()) {
                def filter = getEzproxyFileFilter()
                if(it.name ==~ filter) {
                    def logLine = EzproxyEvent.findAllByFileName(it.name, [max:1])
                    def itemToAdd = [file:it]
                    if(logLine) {
                        itemToAdd.done = true
                    } else {
                        itemToAdd.done = false
                    }
                    result << itemToAdd
                }
            }
        }

        return result
    }

    def getEzproxyFileFilter() {
        getEzproxyProperty(FILE_FILTER_PROPERTY, DEFAULT_FILE_FILTER)
    }

    def getEzproxyDirectory() {
        getEzproxyProperty(EZPROXY_DIRECTORY_PROPERTY, DEFAULT_EZPROXY_DIRECTORY)
    }

    def getEzproxyProperty(String propertyName, String defaultValue) {
        return getEzproxyPropertyObject(propertyName, defaultValue).propertyValue
    }

    def getEzproxyPropertyObject(String propertyName, String defaultValue) {
        def property = EzProperties.findByPropertyName(propertyName)
        if(property) {
            return property
        } else {
            property = new EzProperties(propertyName: propertyName, propertyValue: defaultValue)
            property.save(flush: true)
        }

        return property
    }

    def updateFileFilter(String newFileFilter) {
        log.info "updating file filter to $newFileFilter"
        getEzproxyPropertyObject(FILE_FILTER_PROPERTY, DEFAULT_FILE_FILTER).propertyValue = newFileFilter
    }

    def updateDirectory(String newEzproxyDirectory) {
        log.info "updating ezproxy directory to $newEzproxyDirectory"
        getEzproxyPropertyObject(EZPROXY_DIRECTORY_PROPERTY, DEFAULT_EZPROXY_DIRECTORY).propertyValue = newEzproxyDirectory
    }
}
