.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (c) 2017-2018 AT&T Intellectual Property.  All rights reserved.

Release Notes
=============

Version: 3.0.3
--------------

:Release Date: 2018-11-13

**New Features**

The Casablanca release is the third release of the Control Loop Automation Management Platform (CLAMP).

The main goal of the Casablanca release was to:

    - Enhance Platform maturity by improving CLAMP maturity matrix see `Wiki <https://wiki.onap.org/display/DW/Casablanca+Release+Platform+Maturity>`_.
    - CLAMP Dashboard improvements for the monitoring of active Closed Loops
    - CLAMP logs alignment on the ONAP platform.
    - CLAMP is now integrated with AAF for authentication and permissions retrieval (AAF server is pre-loaded by default with the required permissions)
    - CLAMP improvement for configuring the policies (support of Scale Out use case)
    - CLAMP main Core/UI have been reworked, removal of security issues reported by Nexus IQ on JAVA/JAVASCRIPT code (Libraries upgrade or removal/replacement when possible)
    - As a POC, the javascript coverage can now be enabled in SONAR (Disabled for now)

**Bug Fixes**

	- The full list of implemented user stories and epics is available on `CASABLANCA RELEASE <https://jira.onap.org/projects/CLAMP/versions/10408>`_
	  This includes the list of bugs that were fixed during the course of this release.

**Known Issues**

    - None

**Security Notes**

CLAMP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and actions to be taken in future release. 
The CLAMP open Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=42598587>`_.

Quick Links:
 	- `CLAMP project page <https://wiki.onap.org/display/DW/CLAMP+Project>`_

 	- `Passing Badge information for CLAMP <https://bestpractices.coreinfrastructure.org/en/projects/1197>`_

 	- `Project Vulnerability Review Table for CLAMP <https://wiki.onap.org/pages/viewpage.action?pageId=42598587>`_

**Upgrade Notes**

    New Docker Containers are available, an ELK stack is also now part of CLAMP deployments.

**Deprecation Notes**

    The CLAMP Designer Menu (in CLAMP UI) is deprecated since Beijing, the design time is being onboarded into SDC - DCAE D.

**Other**

    CLAMP Dashboard is now implemented, allows to monitor Closed Loops that are running by retrieving CL events on DMAAP.

**How to - Videos**

    https://wiki.onap.org/display/DW/CLAMP+videos

Version: 2.0.2
--------------

:Release Date: 2018-06-07

**New Features**

The Beijing release is the second release of the Control Loop Automation Management Platform (CLAMP).

The main goal of the Beijing release was to:

    - Enhance Platform maturity by improving CLAMP maturity matrix see `Wiki <https://wiki.onap.org/display/DW/Beijing+Release+Platform+Maturity>`_.
    - Focus CLAMP on Closed loop runtime operations and control - this is reflected by the move of the design part to DCAE-D.
    - Introduce CLAMP Dashboard for monitoring of active Closed Loops.
    - CLAMP is integrated with MSB.
    - CLAMP has integrated SWAGGER.
    - CLAMP main Core has been reworked for improved flexibility.

**Bug Fixes**

	- The full list of implemented user stories and epics is available on `BEIJING RELEASE <https://jira.onap.org/projects/CLAMP/versions/10314>`_
	  This includes the list of bugs that were fixed during the course of this release.

**Known Issues**

    - `CLAMP-69 <https://jira.onap.org/browse/CLAMP-69>`_ Deploy action does not always work.

        The "Deploy" action does not work directly after submitting it.

        Workaround:

        You have to close the CL and reopen it again. In that case the Deploy action will do something.

**Security Notes**

CLAMP code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and determined to be false positive. The CLAMP open Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=25440749>`_.

Quick Links:
 	- `CLAMP project page <https://wiki.onap.org/display/DW/CLAMP+Project>`_
 	
 	- `Passing Badge information for CLAMP <https://bestpractices.coreinfrastructure.org/en/projects/1197>`_
 	
 	- `Project Vulnerability Review Table for CLAMP <https://wiki.onap.org/pages/viewpage.action?pageId=25440749>`_

**Upgrade Notes**

    New Docker Containers are avaialble, an ELK stack is also now part of CLAMP deployments.

**Deprecation Notes**

    The CLAMP Designer UI is now deprecated and unavailable, the design time is being onboarded into SDC - DCAE D.

**Other**

    CLAMP Dashboard is now implemented, allows to monitor Closed Loops that are running by retrieving CL events on DMAAP.

Version: 1.1.0
--------------

:Release Date: 2017-11-16

**New Features**

The Amsterdam release is the first release of the Control Loop Automation Management Platform (CLAMP).

The main goal of the Amsterdam release was to:

    - Support the automation of provisionning for the Closed loops of the vFW, vDNW and vCPE through TCA.
    - Support the automation of provisionning for the Closed loops of VVolte (Holmes)
    - Demonstrate complete interaction with Policy, DCAE, SDC and Holmes.

**Bug Fixes**

	- The full list of implemented user stories and epics is available on `AMSTERDAM RELEASE <https://jira.onap.org/projects/CLAMP/versions/10313>`_
	  This is technically the first release of CLAMP, previous release was the seed code contribution.
	  As such, the defects fixed in this release were raised during the course of the release.
	  Anything not closed is captured below under Known Issues. If you want to review the defects fixed in the Amsterdam release, refer to Jira link above.

**Known Issues**
	- `CLAMP-68 <https://jira.onap.org/browse/CLAMP-68>`_ ResourceVF not always provisioned.

        In Closed Loop -> Properties CL: When opening the popup window, the first service in the list does not show Resource-VF even though in SDC there is a resource instance in the service.

        Workaround:

        If you have multiple service available (if not create a dummy one on SDC), just click on another one and then click back on the first one in the list. The ResourceVF should be provisioned now.

    - `CLAMP-69 <https://jira.onap.org/browse/CLAMP-69>`_ Deploy action does not always work.

        The "Deploy" action does not work directly after submitting it.

        Workaround:

        You have to close the CL and reopen it again. In that case the Deploy action will do something


**Security Issues**
	CLAMP is following the CII Best Practices Badge Program, results including security assesment can be found on the
	`project page <https://bestpractices.coreinfrastructure.org/projects/1197>`_


**Upgrade Notes**

    N/A

**Deprecation Notes**

    N/A

**Other**



===========

End of Release Notes
