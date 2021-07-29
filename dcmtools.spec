%undefine _disable_source_fetch
Name: dcmimport
Version: 0.0.1
Release: 0
License: Apache-2.0
Summary: A utility for importing certificates into Digital Certificate Manager
(DCM) for IBM i.
Url: https://github.com/ThePrez/dcmimport

BuildRequires: maven
BuildRequires: openjdk-11
BuildRequires: coreutils-gnu
BuildRequires: make-gnu
Requires: bash
Requires: coreutils-gnu
Requires: /QOpenSys/pkgs/bin/trust

Source0: https://github.com/ThePrez/dcmimport/archive/v%{version}.tar.gz


%description
A utility for importing certificates into Digital Certificate Manager
(DCM) for IBM i.
%prep
%setup

%build
gmake all


%install
INSTALL_ROOT=%{buildroot} gmake -e install

%files
%defattr(-, qsys, *none)

%{_bindir}/dcmimport
%{_libdir}/dcmimport

%changelog
* Wed Jul 28 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.0.1
- initial RPM release
