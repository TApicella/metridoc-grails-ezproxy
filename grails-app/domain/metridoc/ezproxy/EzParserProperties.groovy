package metridoc.ezproxy

import org.apache.commons.lang.RandomStringUtils
import org.jasypt.util.text.BasicTextEncryptor

class EzParserProperties {

    String crossRefEncryptionKey = RandomStringUtils.randomAlphanumeric(100)
    String crossRefUserName
    String crossRefPassword
    Boolean storePatronId = true
    Boolean encryptPatronInfo = true
    String fileFilter = ".*"
    String directory = EzproxyUtils.DEFAULT_FILE_LOCATION
    Boolean jobActivated = false
    String sampleLog = EzproxyUtils.DEFAULT_LOG_DATA
    String sampleParser = EzproxyUtils.DEFAULT_PARSER
    String encoding = "utf-8"
    Boolean anonymizePatronInfo = false
    private static final EzParserProperties instance;

    static mapping = {

    }

    static constraints = {
        sampleParser(maxSize: Integer.MAX_VALUE, nullable: true)
        sampleLog(maxSize: Integer.MAX_VALUE, nullable: true)
        directory(nullable: true)
        fileFilter(nullable: true)
        crossRefPassword(nullable: true)
        crossRefUserName(nullable: true)
    }

    synchronized static EzParserProperties instance() {
        int count = EzParserProperties.count()
        if (count == 0) {
            initializeEzParserProperties()
        }
        assert 1 == EzParserProperties.count(): "there should only be one instance of EzParserProperties, but there were $count"
        EzParserProperties.list().get(0)
    }

    private static initializeEzParserProperties() {
        new EzParserProperties().save(flush: true, failOnError: true)
    }

    static void updatePassword(String password) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor()
        def instance = instance()
        encryptor.password = instance.crossRefEncryptionKey
        instance.crossRefPassword = encryptor.encrypt(password)
        instance.save(flush: true)
    }

    static String getDecryptedCrossRefPassword() {
        BasicTextEncryptor encryptor = new BasicTextEncryptor()
        def instance = instance()
        encryptor.password = instance.crossRefEncryptionKey
        return encryptor.decrypt(instance.crossRefPassword)
    }
}
