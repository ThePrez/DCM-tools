


target/dcmimport.jar: FORCE /QOpenSys/pkgs/bin/mvn /QOpenSys/pkgs/bin/trust /QOpenSys/pkgs/lib/jvm/openjdk-11/bin/java
	JAVA_HOME=/QOpenSys/pkgs/lib/jvm/openjdk-11 /QOpenSys/pkgs/bin/mvn package
	cp target/*-with-dependencies.jar target/dcmimport.jar

FORCE:

all: target/dcmimport.jar

uninstall: clean
	rm -r ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmimport ${INSTALL_ROOT}/QOpenSys/pkgs/bin/dcmimport

clean:
	rm -r target


/QOpenSys/pkgs/bin/mvn:
	/QOpenSys/pkgs/bin/yum install maven

/QOpenSys/pkgs/lib/jvm/openjdk-11/bin/java:
	/QOpenSys/pkgs/bin/yum install openjdk=11

/QOpenSys/pkgs/bin/trust:
	/QOpenSys/pkgs/bin/yum install /QOpenSys/pkgs/bin/trust

install: dcmimport.bin target/dcmimport.jar 
	install -m 755 -o qsys -D -d ${INSTALL_ROOT}/QOpenSys/pkgs/bin ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmimport
	install -m 555 -o qsys dcmimport.bin ${INSTALL_ROOT}/QOpenSys/pkgs/bin/dcmimport
	install -m 444 -o qsys target/dcmimport.jar ${INSTALL_ROOT}/QOpenSys/pkgs/lib/dcmimport/dcmimport.jar