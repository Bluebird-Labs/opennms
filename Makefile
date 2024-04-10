##
# Makefile to build OpenNMS from source
##
.DEFAULT_GOAL := compile

SHELL                 := /bin/bash -o nounset -o pipefail -o errexit
WORKING_DIRECTORY     := $(shell pwd)
SITE_FILE             := antora-playbook-local.yml
ARTIFACTS_DIR         := target/artifacts
MAVEN_BIN             := maven/bin/mvn
MAVEN_ARGS            := --batch-mode -DupdatePolicy=never -Djava.awt.headless=true -Daether.connector.resumeDownloads=false -Daether.connector.basic.threads=1 -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -DvaadinJavaMaxMemory=2g -DmaxCpus=8
export MAVEN_OPTS     := -Xms8g -Xmx8g -XX:ReservedCodeCacheSize=1g -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-UseGCOverheadLimit -XX:-MaxFDLimit -Djdk.util.zip.disableZip64ExtraFieldValidation=true -Dmaven.wagon.http.retryHandler.count=3

GIT_BRANCH            := $(shell git branch | grep \* | cut -d' ' -f2)
OPENNMS_HOME          := /opt/opennms
OPENNMS_VERSION       := $(shell .circleci/scripts/pom2version.sh pom.xml)
VERSION               := $(shell echo ${OPENNMS_VERSION} | sed -e 's,-SNAPSHOT,,')
RELEASE_BUILD_KEY     := onms
RELEASE_BRANCH        := $(shell echo ${GIT_BRANCH} | sed -e 's,/,-,g')
ifndef CIRCLE_BUILD_NUM
override RELEASE_BUILD_NUM = 0
endif
RELEASE_BUILD_NUM     ?= ${CIRCLE_BUILD_NUM}
RELEASE_BUILDNAME     := ${RELEASE_BRANCH}
RELEASE_COMMIT        := $(shell git rev-parse --short HEAD)
RELEASE_MINOR_VERSION := $(shell git log --pretty="format:%cd" --date=short -1 | sed -e "s,^Date: *,," -e "s,-,,g" )
RELEASE_MICRO_VERSION := ${RELEASE_BUILD_KEY}.${RELEASE_BUILDNAME}.${RELEASE_BUILD_NUM}
INSTALL_VERSION       := ${VERSION}-0.${RELEASE_MINOR_VERSION}.${RELEASE_MICRO_VERSION}.${RELEASE_COMMIT}
DOCKER_ARCH           := linux/amd64
OCI_REGISTRY          ?= quay.io
OCI_REGISTRY_USER     ?= changeme
OCI_REGISTRY_PASSWORD ?= changeme
OCI_REGISTRY_ORG      ?= changeme

.PHONY: help
help:
	@echo ""
	@echo "Makefile to build artifacts for OpenNMS"
	@echo ""
	@echo "Requirements to build:"
	@echo "  * OpenJDK 17 Development Kit"
	@echo "  * Maven 3.6.1, WARNING: Don't run with latest Maven, it throws errors. 3.6.1 is shipped with the git repo."
	@echo "  * NodeJS 18 with npm"
	@echo "  * Global install of yarn: npm install --global yarn"
	@echo "  * Global install of node-gyp to build the UI: yarn global add node-gyp"
	@echo "  * Antora"
	@echo "We are using the command tool to test for the requirements in your search path."
	@echo ""
	@echo "Targets:"
	@echo "  help:                  Show this help"
	@echo "  validate:              Fail quickly by checking project structure with mvn:clean"
	@echo "  maven-structure-graph: Generate a JSON file with the Maven structure used to generate test class list"
	@echo "  test-lists:            Generate a list with all JUnit and Integration Test class names for splitting jobs"
	@echo "  compile:               Compile OpenNMS from source code with runs expensive tasks doing"
	@echo "  assemble:              Assemble the build artifacts with expensive tasks for a production build"
	@echo "  quick-compile:         Quick compile to get fast feedback for development"
	@echo "  quick-assemble:        Quick assemble to run on a build local system"
	@echo "  core-oci:              Build container image for Horizon Core, tag: opennms/horizon:latest"
	@echo "  minion-oci:            Build container image for Minion, tag opennms/minion:latest"
	@echo "  sentinel-oci:          Build container image for Sentiel, tag opennms/sentinel:latest"
	@echo "  show-core-oci:         Analyze the OCI image using dive, tag opennms/horizon:latest"
	@echo "  show-minion-oci:       Analyze the OCI image using dive, tag opennms/minion:latest"
	@echo "  show-sentinel-oci:     Analyze the OCI image using dive, tag opennms/sentinel:latest"
	@echo "  core-oci-sbom:         Create software bill of material for the Core container image"
	@echo "  minion-oci-sbom:       Create software bill of material for the Minion container image"
	@echo "  sentinel-oci-sbom:     Create software bill of material for the Sentinel container image"
	@echo "  core-oci-sec-scan:     Create security scan report for the Core container image"
	@echo "  minion-oci-sec-scan:   Create security scan report for the Core container image"
	@echo "  sentinel-oci-sec-scan: Create security scan report for the Core container image"
	@echo "  quick-smoke:           Simple smoke test to verify the application can be started using the MenuHeaderIT,SinglePortFlowsIT"
	@echo "  core-smoke:            Run full smoke test suite against the Core components"
	@echo "  minion-smoke:          Run full smoke test suite against the Minion components"
	@echo "  sentinel-smoke:        Run full smoke test suite against the Sentinel components"
	@echo "  unit-tests:            Run full unit test suite, you can run specific tests in a projects with:"
	@echo "                           U_TESTS=org.opennms.netmgt.provision.detector.dhcp.DhcpDetectorTest TEST_PROJECTS=org.opennms:opennms-detector-dhcp"
	@echo "  integration-tests:     Run full integration test suit, you can run specific integration tests in a project with:"
	@echo "                           I_TESTS=org.opennms.netmgt.snmpinterfacepoller.SnmpPollerIT TEST_PROJECTS=org.opennms:opennms-services"
	@echo "  code-coverage:         Test code coverage with SonarScanner CLI"
	@echo "  javadocs:              Generate Java docs"
	@echo "  docs:                  Build Antora docs with a local install Antora, default target"
	@echo "  clean-all:             Clean git repository with untracked files, docs, M2 opennms artifacts and build assemblies"
	@echo "  clean-git:             DELETE *all* untracked files from local git repository"
	@echo "  clean-m2:              Remove just OpenNMS build artifacts from Maven local repository"
	@echo "  clean-assembly:        Run mvn clean on assemblies, equivalent to clean.pl"
	@echo "  clean-docs:            Clean all docs build artifacts"
	@echo "  collect-artifacts:     Fetch and collect build artifacts in $(ARTIFACTS_DIR)"
	@echo "  collect-testresults:   Fetch test results from tests in $(ARTIFACTS_DIR)/tests"
	@echo "  spinup-postgres:       Spinup a PostgreSQL container to run integration tests used by integration tests"
	@echo "  destroy-postgres:      Shutdown and destroy the PostgreSQL container"
	@echo ""
	@echo "Arguments: "
	@echo "  SITE_FILE:           Antora site.yml file to build the site"
	@echo ""
	@echo ""

.PHONY: deps-build
deps-build:
	@echo "Check build dependencies: Java JDK, NodeJS, NPM, paste, python3 and yarn with node-gyp"
	@command -v $(MAVEN_BIN)
	@command -v java
	@command -v javac
	@command -v npm
	@command -v paste
	@command -v python3
	@command -v yarn
	@yarn global list | grep "^info \"node-gyp.*has binaries:"
	mkdir -p $(ARTIFACTS_DIR)

.PHONY: deps-docs
deps-docs:
	@echo "Check documentation build dependency: antora"
	@command -v antora

.PHONY: deps-oci
deps-oci:
	@echo "Check OCI build dependency: docker"
	@command -v docker

.PHONY: deps-oci-sbom
deps-oci-sbom:
	@echo "Check OCI SBOM dependency: syft"
	@command -v syft

.PHONY: deps-oci-sec-scan
deps-oci-sec-scan:
	@echo "Check OCI security scan dependency: trivy"
	@command -v trivy

.PHONY: deps-sonar
deps-sonar:
	@echo "Check code coverage test dependency: sonar-scanner"
	@command -v sonar-scanner

.PHONY: deps-oci-layers
deps-oci-layers:
	@echo "Show OCI container layer usage: dive"
	@command -v dive

.PHONY: show-info
show-info:
	@echo "MAVEN_OPTS=\"$(MAVEN_OPTS)\""
	@echo "MAVEN_ARGS=\"$(MAVEN_ARGS)\""
	@$(MAVEN_BIN) --version

.PHONY: validate
validate: deps-build show-info
	$(MAVEN_BIN) clean
	$(MAVEN_BIN) clean --file opennms-full-assembly/pom.xml -Dbuild.profile=default

.PHONY: maven-structure-graph
maven-structure-graph: deps-build show-info
	${MAVEN_BIN} org.opennms.maven.plugins:structure-maven-plugin:1.0:structure $(MAVEN_ARGS) -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -s .circleci/structure-settings.xml --fail-at-end -Prun-expensive-tasks -Pbuild-bamboo

.PHONY: test-lists
test-lists: maven-structure-graph
	mkdir -p $(ARTIFACTS_DIR)/tests
	python3 .circleci/scripts/find-tests/find-tests.py generate-test-lists --changes-only="false" --output-unit-test-classes="$(ARTIFACTS_DIR)/tests/unit_tests_classnames" --output-integration-test-classes="$(ARTIFACTS_DIR)/tests/integration_tests_classnames" .
	cat $(ARTIFACTS_DIR)/tests/*_tests_classnames | python3 .circleci/scripts/find-tests/find-tests.py generate-test-modules --output="$(ARTIFACTS_DIR)/tests/test_modules" .
	find smoke-test -type f -regex ".*\/src\/test\/java\/.*IT.*\.java" | sed -e 's#^.*src/test/java/\(.*\)\.java#\1#' | tr "/" "." > $(ARTIFACTS_DIR)/tests/smoke_tests_classnames

.PHONY: compile
compile: maven-structure-graph
	# The -Dbuild.sbom=true prevents using -T 1C and fails with compiling errors
	$(MAVEN_BIN) install $(MAVEN_ARGS) -DskipTests=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dbuild.skip.tarball=false -Prun-expensive-tasks -Psmoke -Dbuild.type=production -Dbuild.sbom=true 2>&1 | tee $(ARTIFACTS_DIR)/mvn.compile.log

.PHONY: compile-ui
compile-ui:
	cd ui && yarn install && yarn build && yarn test

.PHONY: assemble
assemble: deps-build show-info
	# TODO: Investigate why it's not possible to add the -Pbuild-bamboo here
	$(MAVEN_BIN) install $(MAVEN_ARGS) -DskipTests=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dopennms.home=$(OPENNMS_HOME) -Dinstall.version=$(INSTALL_VERSION) -Pbuild-bamboo -Prun-expensive-tasks -Dbuild.skip.tarball=false -Denable.license=true -Dbuild.type=production --file opennms-full-assembly/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.assemble.log

.PHONY: quick-compile
quick-compile: maven-structure-graph
	$(MAVEN_BIN) install $(MAVEN_ARGS) -T 1C -DskipTests=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dcyclonedx.skip=true 2>&1 | tee $(ARTIFACTS_DIR)/mvn.quick-compile.log

.PHONY: quick-assemble
quick-assemble: deps-build show-info
	$(MAVEN_BIN) install $(MAVEN_ARGS) -DskipTests=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dopennms.home=$(OPENNMS_HOME) -Dinstall.version=$(INSTALL_VERSION) --file opennms-full-assembly/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.quick-assemble.log

.PHONY: core-oci
core-oci:
ifeq (,$(wildcard ./opennms-full-assembly/target/*-core.tar.gz))
	@echo "Can't build the Core container image, the build artifact"
	@echo "./opennms-full-assembly/target/opennms-full-assembly-$(OPENNMS_VERSION)-core.tar.gz doesn't exist."
	@echo ""
	@echo "You can create the artifact with:"
	@echo ""
	@echo "  make quick-compile && make quick-assemble"
	@echo ""
	@exit 1
endif
	cd opennms-container/core && \
	make DOCKER_ARCH="$(DOCKER_ARCH)" \
         DOCKER_OCI="images/core.oci" \
         DOCKER_TAG="opennms/horizon" \
         BUILD_NUMBER="${CIRCLE_BUILD_NUM}" \
         BUILD_URL="${CIRCLE_BUILD_URL}" \
         BUILD_BRANCH="${CIRCLE_BRANCH}" \
         oci && \
    docker load -i images/core.oci # Need to run in the same context as the docker build

.PHONY: minion-oci
minion-oci:
ifeq (,$(wildcard ./opennms-assemblies/minion/target/org.opennms.assemblies.minion-*-minion.tar.gz))
	@echo "Can't build the Minion container image, the build artifact"
	@echo "./opennms-assemblies/minion/target/org.opennms.assemblies.minion-$(OPENNMS_VERSION)-minion.tar.gz doesn't exist."
	@echo ""
	@echo "You can create the artifact with:"
	@echo ""
	@echo "  make quick-compile && make quick-assemble"
	@echo ""
	@exit 1
endif
	cd opennms-container/minion && \
	make DOCKER_ARCH="$(DOCKER_ARCH)" \
         DOCKER_OCI="images/minion.oci" \
         DOCKER_TAG="opennms/minion" \
         BUILD_NUMBER="${CIRCLE_BUILD_NUM}" \
         BUILD_URL="${CIRCLE_BUILD_URL}" \
         BUILD_BRANCH="${CIRCLE_BRANCH}" \
         oci && \
    docker load -i images/minion.oci # Need to run in the same context as the docker build

.PHONY: sentinel-oci
sentinel-oci:
ifeq (,$(wildcard ./opennms-assemblies/sentinel/target/org.opennms.assemblies.sentinel-*-sentinel.tar.gz))
	@echo "Can't build the Sentinel container image, the build artifact"
	@echo "./opennms-assemblies/sentinel/target/org.opennms.assemblies.sentinel-$(OPENNMS_VERSION)-sentinel.tar.gz doesn't exist."
	@echo ""
	@echo "You can create the artifact with:"
	@echo ""
	@echo "  make quick-compile && make quick-assemble"
	@echo ""
	@exit 1
endif
	cd opennms-container/sentinel && \
	make DOCKER_ARCH="$(DOCKER_ARCH)" \
         DOCKER_OCI="images/sentinel.oci" \
         DOCKER_TAG="opennms/sentinel" \
         BUILD_NUMBER="${CIRCLE_BUILD_NUM}" \
         BUILD_URL="${CIRCLE_BUILD_URL}" \
         BUILD_BRANCH="${CIRCLE_BRANCH}" \
         oci && \
    docker load -i images/sentinel.oci # Need to run in the same context as the docker build

.PHONY: show-core-oci
show-core-oci: deps-oci-layers core-oci
	CI=true dive opennms/horizon:latest

.PHONY: show-minion-ociow
show-minion-oci: deps-oci-layers minion-oci
	CI=true dive opennms/minion:latest

.PHONY: show-sentinel-oci
show-sentinel-oci: deps-oci-layers sentinel-oci
	CI=true dive opennms/sentinel:latest

.PHONY: core-oci-sbom
core-oci-sbom: deps-oci-sbom core-oci
	syft scan ./opennms-container/core/images/core.oci -o cyclonedx=$(ARTIFACTS_DIR)/oci/core-oci-sbom.xml --quiet

.PHONY: minion-oci-sbom
minion-oci-sbom: deps-oci-sbom minion-oci
	syft scan ./opennms-container/minion/images/minion.oci -o cyclonedx=$(ARTIFACTS_DIR)/oci/minion-oci-sbom.xml --quiet

.PHONY: sentinel-oci-sbom
sentinel-oci-sbom: deps-oci-sbom sentinel-oci
	syft scan ./opennms-container/sentinel/images/sentinel.oci -o cyclonedx=$(ARTIFACTS_DIR)/oci/sentinel-oci-sbom.xml --quiet

.PHONY: core-oci-sec-scan
core-oci-sec-scan: deps-oci-sec-scan core-oci
	trivy image --input opennms-container/core/images/core.oci --timeout 30m --format json -o $(ARTIFACTS_DIR)/oci/core-trivy-report.json

.PHONY: minion-oci-sec-scan
minion-oci-sec-scan: deps-oci-sec-scan minion-oci
	trivy image --input opennms-container/minion/images/minion.oci --timeout 30m --format json -o $(ARTIFACTS_DIR)/oci/minion-trivy-report.json

.PHONY: sentinel-oci-sec-scan
sentinel-oci-sec-scan: deps-oci-sec-scan sentinel-oci
	trivy image --input opennms-container/sentinel/images/sentinel.oci --timeout 30m --format json -o $(ARTIFACTS_DIR)/oci/sentinel-trivy-report.json

# Run just the a very limited set of integration tests to verify the application comes up and we have something we can
# at least work with.
.PHONY: quick-smoke
quick-smoke: deps-oci core-oci minion-oci sentinel-oci test-lists
	$(MAVEN_BIN) install $(MAVEN_ARGS) -N -DskipTests=false -DskipITs=false -DfailIfNoTests=false -Dtest.fork.count=1 -Dit.test="MenuHeaderIT,SinglePortFlowsIT" --fail-fast -Dfailsafe.skipAfterFailureCount=1 -P!smoke.all -Psmoke.core --file smoke-test/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.smoke-quick.log

.PHONY: core-smoke
core-smoke: deps-oci test-lists core-oci minion-oci sentinel-oci
	$(MAVEN_BIN) install $(MAVEN_ARGS) -N -DskipTests=false -DskipITs=false -DfailIfNoTests=false -Dtest.fork.count=1 -Dit.test="$(shell cat $(ARTIFACTS_DIR)/tests/smoke_tests_classnames | paste -s -d, -)" --fail-fast -Dfailsafe.skipAfterFailureCount=1 -P!smoke.all -Psmoke.core --file smoke-test/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.core-smoke.log

.PHONY: minion-smoke
minion-smoke: deps-oci test-lists minion-oci sentinel-oci core-oci
	$(MAVEN_BIN) install $(MAVEN_ARGS) -N -DskipTests=false -DskipITs=false -DfailIfNoTests=false -Dtest.fork.count=1 -Dit.test="$(shell cat $(ARTIFACTS_DIR)/tests/smoke_tests_classnames | paste -s -d, -)" --fail-fast -Dfailsafe.skipAfterFailureCount=1 -P!smoke.all -Psmoke.minion --file smoke-test/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.minion-smoke.log

.PHONY: sentinel-smoke
sentinel-smoke: deps-oci test-lists sentinel-oci minion-oci core-oci
	$(MAVEN_BIN) install $(MAVEN_ARGS) -N -DskipTests=false -DskipITs=false -DfailIfNoTests=false -Dtest.fork.count=1 -Dit.test="$(shell cat $(ARTIFACTS_DIR)/tests/smoke_tests_classnames | paste -s -d, -)" --fail-fast -Dfailsafe.skipAfterFailureCount=1 -P!smoke.all -Psmoke.sentinel --file smoke-test/pom.xml 2>&1 | tee $(ARTIFACTS_DIR)/mvn.sentinel-smoke.log

# We allow users here to pass a specific unit tests and projects to run.
# Otherwise we run the full test suite
.PHONY: unit-tests
ifeq ($(origin U_TESTS), undefined)
# TODO: Workaround to skip specific unit tests which fail and need some additional investigation
U_TESTS = $(shell grep -Fxv -f ./.circleci/_skipTests.txt ./target/artifacts/tests/unit_tests_classnames | paste -s -d, -)
endif
ifeq ($(origin TESTS_PROJECTS), undefined)
TESTS_PROJECTS = $(shell cat ${ARTIFACTS_DIR}/tests/test_modules | paste -s -d, -)
endif
unit-tests: test-lists
	# Parallel compiling with -T 1C works, but it doesn't for tests
	$(MAVEN_BIN) install $(MAVEN_ARGS) -T 1C -DskipTests=true -DskipITs=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dfailsafe.skipAfterFailureCount=1 -P!checkstyle -P!production -Pbuild-bamboo -Dbuild.skip.tarball=true -Dmaven.test.skip.exec=true --fail-fast --also-make --projects "$(TESTS_PROJECTS)" 2>&1 | tee $(ARTIFACTS_DIR)/mvn.tests.compile.log
	$(MAVEN_BIN) install $(MAVEN_ARGS) -DskipTests=false -DskipITs=true -DskipSurefire=false -DskipFailsafe=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dfailsafe.skipAfterFailureCount=1 -P!checkstyle -P!production -Pbuild-bamboo -Pcoverage -Dbuild.skip.tarball=true -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false -DrunPingTests=false --fail-fast -Dorg.opennms.core.test-api.dbCreateThreads=1 -Dorg.opennms.core.test-api.snmp.useMockSnmpStrategy=false -Dtest="$(U_TESTS)" --projects "$(TESTS_PROJECTS)" 2>&1 | tee $(ARTIFACTS_DIR)/mvn.u_tests.log

.PHONY: integration-tests
ifeq ($(origin I_TESTS), undefined)
# TODO: Workaround to skip specific integrations tests which fail and need some additional investigation
I_TESTS = $(shell grep -Fxv -f ./.circleci/_skipIntegrationTests.txt ./target/artifacts/tests/integration_tests_classnames | paste -s -d, -)
endif
ifeq ($(origin TESTS_PROJECTS), undefined)
TESTS_PROJECTS = $(shell cat $(ARTIFACTS_DIR)/tests/test_modules | paste -s -d, -)
endif
integration-tests: test-lists spinup-postgres
	# Parallel compiling with -T 1C works, but it doesn't for tests
	$(MAVEN_BIN) install $(MAVEN_ARGS) -T 1C -DskipTests=true -DskipITs=true -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dfailsafe.skipAfterFailureCount=1 -P!checkstyle -P!production -Pbuild-bamboo -Dbuild.skip.tarball=true -Dmaven.test.skip.exec=true --fail-fast --also-make --projects "$(TESTS_PROJECTS)" 2>&1 | tee $(ARTIFACTS_DIR)/mvn.tests.compile.log
	$(MAVEN_BIN) install $(MAVEN_ARGS) -DskipTests=false -DskipITs=false -DskipSurefire=true -DskipFailsafe=false -Dbuild.profile=default -Droot.dir=$(WORKING_DIRECTORY) -Dfailsafe.skipAfterFailureCount=1 -P!checkstyle -P!production -Pbuild-bamboo -Pcoverage -Dbuild.skip.tarball=true -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false -DrunPingTests=false --fail-fast -Dorg.opennms.core.test-api.dbCreateThreads=1 -Dorg.opennms.core.test-api.snmp.useMockSnmpStrategy=false -Dtest="$(U_TESTS)" -Dit.test="$(I_TESTS)" --projects "$(TESTS_PROJECTS)" 2>&1 | tee $(ARTIFACTS_DIR)/mvn.i_tests.log

.PHONY: code-coverage
code-coverage: deps-sonar
	mkdir -p $(ARTIFACTS_DIR)/code-coverage
	# Generate a list with all Jacoco code coverage reports from compile phase
	find . -type f '!' -path './.git/*' -name jacoco.xml | sort -u > $(ARTIFACTS_DIR)/code-coverage/jacoco.xml

	# Get just the source folders from Java compiled targets and reverse engineer the main and assembly directory structure
	for src in $(shell find . -type d '!' -path './.git/*' -name target | sed -e 's,/target,/src,') ; do \
  		echo $$src/main ; \
  		echo $$src/assembly ; \
  	done \
  	| sort -u > $(ARTIFACTS_DIR)/code-coverage/source-folders.txt

	# Generate a list for all Junit report folders
	find . -type d '!' -path './.git/*' -a \( -name surefire-reports\* -o -name failsafe-reports\* \) | sort -u > $(ARTIFACTS_DIR)/code-coverage/junit-report-folders.txt

	# Get just the test folders from Java compiled targets and reverse engineer the test directory structure
	for src in $(shell find . -type d '!' -path './.git/*' -name target | sed -e 's,/target,/src,') ; do \
  		echo $$src/test ; \
  	done \
  	| sort -u > $(ARTIFACTS_DIR)/code-coverage/test-folders.txt

	# Get just test class folders from surefire or failsafe directories
	for test_classes_dir in $(shell cat target/artifacts/code-coverage/junit-report-folders.txt | sed -e 's,/surefire-reports,,' | sed -e 's,/failsafe-reports,,') ; do \
		find "$$test_classes_dir" -maxdepth 1 -type d -name test-classes ; \
	done \
	| sort -u > $(ARTIFACTS_DIR)/code-coverage/test-class-folders.txt

	# Get just class folders from surefire or failsafe directories
	for classes_dir in $(shell cat target/artifacts/code-coverage/junit-report-folders.txt | sed -e 's,/surefire-reports,,' | sed -e 's,/failsafe-reports,,') ; do \
		find "$$classes_dir" -maxdepth 1 -type d -name classes ; \
	done \
	| sort -u > $(ARTIFACTS_DIR)/code-coverage/class-folders.txt

	bash -c "sonar-scanner -Dsonar.host.url=\"https://sonarcloud.io\" \
                           -Djava.security.egd=file:/dev/./urandom \
                           -Dsonar.coverage.jacoco.xmlReportPaths=\"$(shell cat $(ARTIFACTS_DIR)/code-coverage/jacoco.xml | paste -s -d, -)\" \
                           -Dsonar.junit.reportPaths=\"$(shell cat $(ARTIFACTS_DIR)/code-coverage/junit-report-folders.txt | paste -s -d, -)\" \
                           -Dsonar.sources=\"$(shell cat $(ARTIFACTS_DIR)/code-coverage/source-folders.txt | paste -s -d, -)\" \
                           -Dsonar.tests=\"$(shell cat $(ARTIFACTS_DIR)/code-coverage/test-folders.txt | paste -s -d, -)\" \
                           -Dsonar.java.binaries=\"$(shell $(ARTIFACTS_DIR)/code-coverage/class-folders.txt | paste -s -d, -)\" \
                           -Dsonar.java.libraries=\"${HOME}/.m2/repository/**/*.jar,**/*.jar\" \
                           -Dsonar.java.test.binaries=\"$(shell cat $(ARTIFACTS_DIR)/code-coverage/test-class-folders.txt | paste -s -d, -)\" \
                           -Dsonar.java.test.libraries=\"${HOME}/.m2/repository/**/*.jar,**/*.jar\""

.PHONY: javadocs
javadocs: deps-build show-info
	$(MAVEN_BIN) javadoc:aggregate --batch-mode -Prun-expensive-tasks

.PHONY: docs
docs: deps-docs
	@echo "Build Antora docs..."
	antora --stacktrace $(SITE_FILE)

.PHONY: clean-all
clean-all: clean-docs clean-assembly clean-m2 clean-git

.PHONY: clean-git
clean-git:
	git clean -fdx

.PHONY: clean-m2
clean-m2:
	rm -rf ~/.m2/repository/org/opennms

.PHONY: clean-assembly
clean-assembly:
	$(MAVEN_BIN) -Passemblies clean

.PHONY: clean-docs
clean-docs:
	@echo "Delete build and public artifacts ..."
	@rm -rf build public
	@echo "Clean Antora cache for git repositories and UI components ..."
	@rm -rf .cache

.PHONY: collect-artifacts
# We use find with a regex, which exits gracefully when targets don't exist in case steps failed.
collect-artifacts:
	mkdir -p $(ARTIFACTS_DIR)/{archives,config-schema,oci}
	find . -type f -regex "^\.\/target\/opennms-.*\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives \; # Fetch -source and assembled archive
	find . -type f -regex "^\.\/opennms-assemblies\/minion\/target\/org.opennms.assemblies.minion-.*\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives/minion-${OPENNMS_VERSION}.tar.gz \;
	find . -type f -regex "^\.\/opennms-assemblies\/sentinel\/target\/org.opennms.assemblies.sentinel-.*\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives/sentinel-${OPENNMS_VERSION}.tar.gz \;
	find . -type f -regex "^\.\/opennms-assemblies\/xsds\/target\/.*-xsds\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives/opennms-${OPENNMS_VERSION}-xsds.tar.gz \;
	find . -type f -regex "^\.\/opennms-full-assembly\/target\/opennms-full-assembly-.*-core\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives/opennms-${OPENNMS_VERSION}-core.tar.gz \;
	find . -type f -regex "^\.\/opennms-full-assembly\/target\/opennms-full-assembly-.*-optional\.tar\.gz" -exec cp -v {} $(ARTIFACTS_DIR)/archives/opennms-${OPENNMS_VERSION}-optional.tar.gz \;
	find . -type f -regex "^\.\/opennms-full-assembly\/target\/THIRD-PARTY.txt" -exec cp -v {} $(ARTIFACTS_DIR) \;
	find . -type f -regex "^\.\/opennms-container\/.*\/images\/.*\.oci" -exec cp -v {} $(ARTIFACTS_DIR)/oci \;
	find . -type f -regex "^\.\/target\/bom.*" -exec cp -v {} $(ARTIFACTS_DIR) \;

.PHONY: collect-testresults
collect-testresults:
	mkdir -p $(ARTIFACTS_DIR)/{surefire-reports,failsafe-reports,recordings}
	find . -type f -regex ".*\/target\/.*\.mp4" -exec cp -v {} $(ARTIFACTS_DIR)/recordings \;
	find . -type f -regex ".*\/target\/surefire-reports\/.*\.xml" -exec cp -v {} $(ARTIFACTS_DIR)/surefire-reports/ \;
	find . -type f -regex ".*\/target\/failsafe-reports\/.*\.xml" -exec cp -v {} $(ARTIFACTS_DIR)/failsafe-reports/ \;
	find . -type d -regex "^\.\/target\/logs" -exec tar czf $(ARTIFACTS_DIR)/logs.tar.gz {} \;
	find . -type f -regex "^\.\/target\/structure-graph\.json" -exec cp -v {} $(ARTIFACTS_DIR) \;

.PHONY: spinup-postgres
spinup-postgres: deps-oci
	@echo "Spin-up PostgreSQL database for tests using Docker Compose on port 5432/tcp"
	docker compose -f .circleci/postgres-it-docker-compose.yaml up -d

.PHONY: destroy-postgres
destroy-postgres: deps-oci
	@echo "Shutdown and remove PostgreSQL database using Docker Compose"
	docker compose -f .circleci/postgres-it-docker-compose.yaml down -v

.PHONY: registry-login
registry-login: deps-oci
	@echo ${OCI_REGISTRY_PASSWORD} | docker login --username ${OCI_REGISTRY_USER} --password-stdin ${OCI_REGISTRY}
