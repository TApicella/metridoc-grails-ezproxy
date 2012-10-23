package metridoc.test

import java.util.zip.GZIPInputStream
import metridoc.ezproxy.EzproxyLog
import groovy.sql.Sql
import org.slf4j.LoggerFactory

class EzproxyWorkflow extends Script {

    def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP
    def dataSource
    def ezproxyService
    static logger = LoggerFactory.getLogger(EzproxyWorkflow)

    def run() {
        target(runEzproxy: "main target to run ezproxy") {
            def sql = new Sql(dataSource)
            sql.execute("truncate table ezproxy_log")
            sql.execute("ALTER TABLE ezproxy_log AUTO_INCREMENT = 1")

            def file = new File("/Volumes/Scratch/ezproxy/logs/ezproxy.log.20120203.gz")
            def fileStream = new FileInputStream(file)
            def gzipFileStream = new GZIPInputStream(fileStream)


            profile("inserting ${file.name}") {
                def parser = ezproxyService.parserObject

                EzproxyLog.withNewSession {session ->

                    def batch = []
                    gzipFileStream.eachLine("utf-8") {String line, int index ->
                        def log = new EzproxyLog(parser.parse(line, index, file.name))

                        batch << log
                        if (index % 5000 == 0) {
                            storeBatch(batch, session)
                            EzproxyWorkflow.logger.info "$index items inserted" as String
                        }
                    }
                    storeBatch(batch, session)
                    cleanUpGorm(session)
                }
            }
        }
    }

    def storeBatch(batch, session) {
        EzproxyLog.withNewTransaction {
            batch.each {
                it.save(validate: false)
            }
            batch.clear()
            cleanUpGorm(session)
        }
    }

    def cleanUpGorm(session) {
        session.flush()
        session.clear()
        propertyInstanceMap.get().clear()
    }
}




