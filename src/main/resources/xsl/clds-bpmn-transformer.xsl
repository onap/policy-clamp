<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
  ============LICENSE_START=======================================================
  ONAP CLAMP
  ================================================================================
  Copyright (C) 2017 AT&T Intellectual Property. All rights
                              reserved.
  ================================================================================
  Licensed under the Apache License, Version 2.0 (the "License"); 
  you may not use this file except in compliance with the License. 
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software 
  distributed under the License is distributed on an "AS IS" BASIS, 
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
  See the License for the specific language governing permissions and 
  limitations under the License.
  ============LICENSE_END============================================
  ===================================================================
  ECOMP is a trademark and service mark of AT&T Intellectual Property.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
<!--
  ** clds-bpmn-transformer.xsl
  -->

    <xsl:output method="text" indent="no" omit-xml-declaration="yes" />
    <xsl:strip-space elements="*"/>

	<!-- by default copy all attributes and elements -->
	<xsl:template match="/bpmn2:definitions/bpmn2:process">

                <xsl:text>{"collector":[</xsl:text>
                <xsl:for-each select="bpmn2:collector" >
		        <xsl:call-template name="network-element" />
                </xsl:for-each>
                <xsl:text>],</xsl:text>

                <xsl:text>"stringMatch":[</xsl:text>
                <xsl:for-each select="bpmn2:stringMatch" >
		        <xsl:call-template name="network-element" />
                </xsl:for-each>
                <xsl:text>],</xsl:text>

                <xsl:text>"policy":[</xsl:text>
                <xsl:for-each select="bpmn2:policy" >
		        <xsl:call-template name="network-element" />
                </xsl:for-each>
                <xsl:text>],</xsl:text>
                
                <xsl:text>"tca":[</xsl:text>
                <xsl:for-each select="bpmn2:tCA" >
		        <xsl:call-template name="network-element" />
                </xsl:for-each>
                <xsl:text>]</xsl:text>
                
                <xsl:text>}</xsl:text>
	</xsl:template>

	<xsl:template name="network-element">
		<xsl:variable name="incoming" select="./bpmn2:incoming"/>

		<xsl:text>{"id":"</xsl:text>
		<xsl:value-of select="./@id"/>
		<xsl:text>", "from":"</xsl:text>
		<xsl:value-of select="../bpmn2:sequenceFlow[@id=$incoming]/@sourceRef"/>
		<xsl:text>"}</xsl:text>
		<xsl:if test="not(position()=last())">, </xsl:if>
	</xsl:template>

</xsl:stylesheet>
