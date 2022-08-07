%undefine _disable_source_fetch
Name: dcmtools
Version: 0.3.0
Release: 0
License: Apache-2.0
Summary: Utilities for working with Digital Certificate Manager (DCM) for IBM i.
Url: https://github.com/ThePrez/DCM-tools

BuildRequires: maven
BuildRequires: openjdk-11
BuildRequires: coreutils-gnu
BuildRequires: make-gnu
BuildRequires: p11-kit-trust
Requires: bash
Requires: coreutils-gnu
Requires: /QOpenSys/pkgs/bin/trust

Source0: https://github.com/ThePrez/DCM-tools/archive/refs/tags/v%{version}.tar.gz

%description
Utilities for working with Digital Certificate Manager (DCM) for IBM i.
%prep
%setup -n DCM-tools-%{version}

%build
gmake all


%install
INSTALL_ROOT=%{buildroot} gmake -e install

%files
%defattr(-, qsys, *none)

%{_bindir}/dcm*
%{_libdir}/%{name}

%changelog
* Sat Aug 06 2022 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.3.0
- bugfix: fix fetching of certificates with dcmimport --fetch-from
- feature: better user experience when missing root certs

* Mon Nov 15 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.2.0
- rework several commands to use IBM i JSSE provider

* Mon Nov 15 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.1.1
- bugfix: fix argument parsing when args contain spaces

* Sun Nov 14 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.1.0
- feature: new 'dcmcreate' command
- feature: new 'dcmview' command
- feature: new 'dcmchangepw' command
- feature: new 'dcmrenamecert' command
- feature: new 'dcmremovecert' command
- feature: provide binary install for IBM i 7.2
- bugfix: IBM i password is not hidden

* Wed Jul 28 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.0.1
- initial RPM release
