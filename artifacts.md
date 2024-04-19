# Artifacts 

> Artifacts required to build OpenNMS without maven.opennms.org

* `org.opennms.maven.plugins:opennms-warmerge-plugin:jar:0.5` => https://github.com/Bluebird-Labs/maven-plugins
  * auschecken, richtiger tag => 0.5 warmerge
  * bauen `mvn clean install -DskipTests`
  * manuell depyloyen über `mvn install:install-file` oder über Pipeline/CircleCi Github Actions

* Could not resolve dependencies for project org.opennms.dependencies:jackson1-dependencies:pom:34.0.0-SNAPSHOT: 
The following artifacts could not be resolved: 
`org.codehaus.jackson:jackson-core-asl:jar:1.9.14-atlassian-6,
org.codehaus.jackson:jackson-mapper-asl:jar:1.9.14-atlassian-6 
org.codehaus.jackson:jackson-jaxrs:jar:1.9.14-atlassian-6 
org.codehaus.jackson:jackson-xc:jar:1.9.14-atlassian-6`
Failure to find org.codehaus.jackson:jackson-core-asl:jar:1.9.14-atlassian-6 in https://repo1.maven.org/maven2/ was cached in the local repository, resolution will not be reattempted until the update interval of central has elapsed or updates are forced
  * ==>  https://packages.atlassian.com/maven-3rdparty/

* Could not resolve dependencies for project org.opennms.dependencies:spring-web-dependencies:pom:34.0.0-SNAPSHOT: 
  Failure to find org.apache.servicemix.bundles:org.apache.servicemix.bundles.spring-web:jar:4.2.9.RELEASE_1.ONMS.1 in https://repo1.maven.org/maven2/ was cached in the local repository, resolution will not be reattempted until the update interval of central has elapsed or updates are forced 
  * ==> https://github.com/Bluebird-Labs/servicemix-bundles
  * Auschecken, richtiger Tag => 4.2.9.RELEASE_1
  * mvn clean install -DskipTests
  * manual deployment
    * TODO MVR must be verified if this below is correct 
    * `mvn install:install-file -Dfile=./spring-web-4.2.9.RELEASE/target/original-org.apache.servicemix.bundles.spring-web-4.2.9.RELEASE_1.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-web -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`
    * `mvn install:install-file -Dfile=./spring-aop-4.2.9.RELEASE/target/org.apache.servicemix.bundles.spring-aop-4.2.9.RELEASE_2-SNAPSHOT.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-aop -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`
    * `mvn install:install-file -Dfile=./spring-beans-4.2.9.RELEASE/target/org.apache.servicemix.bundles.spring-beans-4.2.9.RELEASE_2-SNAPSHOT.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-beans -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`
    * `mvn install:install-file -Dfile=./spring-messaging-4.2.9.RELEASE/target/org.apache.servicemix.bundles.spring-messaging-4.2.9.RELEASE_2-SNAPSHOT.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-messaging -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`
    * `mvn install:install-file -Dfile=./spring-expression-4.2.9.RELEASE/target/org.apache.servicemix.bundles.spring-expression-4.2.9.RELEASE_2-SNAPSHOT.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-expression -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`
    * `mvn install:install-file -Dfile=./spring-web-4.2.9.RELEASE/target/org.apache.servicemix.bundles.spring-web-4.2.9.RELEASE_1.jar -DgroupId=org.apache.servicemix.bundles -DartifactId=org.apache.servicemix.bundles.spring-web -Dversion=4.2.9.RELEASE_1.ONMS.1 -Dpackaging=jar -DgeneratePom=true`

* `com.lowagie:itext:jar:2.1.7.js4` => http://maven.icm.edu.pl/artifactory/repo/ => no longer active
  * manual installation or proxy it from opennms

* Could not resolve dependencies for project org.opennms.dependencies:hibernate-dependencies:pom:34.0.0-SNAPSHOT: Could not find artifact org.hibernate:hibernate-core:jar:3.6.11.ONMS_RELEASE_1 in central (https://repo1.maven.org/maven2/)
  * Kein Plan woher das kommt. Gibt nur ein `3.6.10.FINAL` bzw. ein `3.6.11-SNAPSHOT`, maybe copy them all over from OpenNMS and be done with it
  * the racoon found it for me: https://github.com/opennms-forge/hibernate-orm/tree/3.6
    * was not able to build it in 2024 so far :(
    * manually installed via file-install => hibernate-core-3.6.11.ONMS_RELEASE_1.jar



Third Party:
  * http://maven.icm.edu.pl/artifactory/repo/         => no longer active 401 :(
    * replacement? => https://jaspersoft.jfrog.io/ui/native/third-party-ce-artifacts/ => no broken
    * this seems to work: https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts
  * https://packages.atlassian.com/maven-3rdparty/