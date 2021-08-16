%undefine _disable_source_fetch
Name: dcmtools
Version: 0.0.1
Release: 0
License: Apache-2.0
Summary: Utilities for working with Digital Certificate Manager (DCM) for IBM i.
Url: https://github.com/ThePrez/dcmimport

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
* Wed Jul 28 2021 Jesse Gorzinski <jgorzins@us.ibm.com> - 0.0.1
- initial RPM release
