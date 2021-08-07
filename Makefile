


target/dcmtools.jar: FORCE /QOpenSys/pkgs/bin/mvn /QOpenSys/pkgs/bin/trust /QOpenSys/pkgs/lib/jvm/openjdk-11/bin/java
	JAVA_HOME=/QOpenSys/pkgs/lib/jvm/openjdk-11 /QOpenSys/pkgs/bin/mvn package
	cp target/*-with-dependencies.jar target/dcmtools.jar

FORCE:

all: target/dcmtools.jar

uninstall: clean
	rm -r ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmtools ${INSTALL_ROOT}/QOpenSys/pkgs/bin/dcmimport ${INSTALL_ROOT}/QOpenSys/pkgs/bin/dcmexport

clean:
	rm -r target

/QOpenSys/pkgs/bin/mvn:
	/QOpenSys/pkgs/bin/yum install maven

/QOpenSys/pkgs/lib/jvm/openjdk-11/bin/java:
	/QOpenSys/pkgs/bin/yum install openjdk=11

/QOpenSys/pkgs/bin/trust:
	/QOpenSys/pkgs/bin/yum install /QOpenSys/pkgs/bin/trust

install: scripts/dcmimport scripts/dcmexport target/dcmtools.jar 
	install -m 755 -o qsys -D -d ${INSTALL_ROOT}/QOpenSys/pkgs/bin ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmtools
	install -m 555 -o qsys scripts/* ${INSTALL_ROOT}/QOpenSys/pkgs/bin/
	install -m 444 -o qsys target/dcmtools.jar ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmtools/dcmimport.jar