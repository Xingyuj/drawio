<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:mdm="http://www.ibm.com/mdm/schema"
	xmlns:cc="http://www.anz.com/cm/data/specs/CommercialContact/internal/00000001"
	exclude-result-prefixes="fo fn xs mdm cc">
	<xsl:output method="text" encoding="UTF-8" omit-xml-declaration="yes" indent="yes" />
<xsl:template match="/">
	<xsl:apply-templates
		select="mdm:TCRMService/mdm:TxResponse/mdm:ResponseObject/mdm:TCRMDeletedPartyWithHistoryBObj/mdm:TCRMDeletedPartyBObj/mdm:TCRMPersonBObj"
		mode="map" />
		<xsl:apply-templates
		select="mdm:TCRMService/mdm:TxResponse/mdm:ResponseObject/mdm:TCRMDeletedPartyWithHistoryBObj/mdm:TCRMDeletedPartyBObj/mdm:TCRMOrganizationBObj"
		mode="map" />
</xsl:template>
<xsl:template name="PersonES"
	match="mdm:TCRMPersonBObj" mode="map">
	{
		"partyType" : "<xsl:value-of select="mdm:PartyType" />",
		"sourceSystems": [{
			"sourceSystemId": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminPartyId" />",
			"sourceSystemName": "<xsl:value-of 	select="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue" />"
		}]
	}
</xsl:template>
<xsl:template name="OrganizationES" match="mdm:TCRMOrganizationBObj" mode="map">
	{	
		"partyType" : "<xsl:value-of select="mdm:PartyType" />",
		"sourceSystems": [{
			"sourceSystemId": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminPartyId" />",
			"sourceSystemName": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue" />"
		}]
	}
</xsl:template>

</xsl:stylesheet>