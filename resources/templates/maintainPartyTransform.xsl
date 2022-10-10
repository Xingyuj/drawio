<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:mdm="http://www.ibm.com/mdm/schema"
	xmlns:cc="http://www.anz.com/cm/data/specs/CommercialContact/internal/00000001"
	exclude-result-prefixes="fo fn xs mdm cc">
	<xsl:output method="text" encoding="UTF-8" indent="yes" />
	<xsl:template match="/">
		<xsl:apply-templates select="mdm:TCRMService/mdm:TxResponse/mdm:ResponseObject/mdm:TCRMPersonBObj" mode="map"/>
		<xsl:apply-templates select="mdm:TCRMService/mdm:TxResponse/mdm:ResponseObject/mdm:TCRMOrganizationBObj" mode="map"/>
	</xsl:template>
							
	<xsl:template name="PersonES" match="mdm:TCRMPersonBObj" mode="map">
{
	"ocvId" : "<xsl:value-of select="mdm:TCRMPartyIdentificationBObj[mdm:IdentificationValue='One Customer ID']/mdm:IdentificationNumber"/>",
	"partyType": "P",
	<xsl:if test="mdm:BirthDate !=''">"dateOfBirth": "<xsl:value-of select="substring-before(mdm:BirthDate, ' ')" />",</xsl:if><xsl:if test="mdm:ClientStatusValue">
	"status": "<xsl:value-of select="mdm:ClientStatusValue"/>",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue"/>",</xsl:if><xsl:if test="mdm:EntityStatusValue">
	"entityStatus": "<xsl:value-of select="mdm:EntityStatusValue"/>",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue != '' or mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue != ''">
"kycDetails":	{<xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue">
	"status": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue"/>"</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue != '' and mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue != ''">,</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue">
	"verificationLevel": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue"/>"</xsl:if>
	},</xsl:if><xsl:if test="mdm:GenderType">
	"gender": "<xsl:value-of select="mdm:GenderType" />",</xsl:if><xsl:if test="mdm:MaritalStatusValue">
	"maritalStatus": "<xsl:value-of select="mdm:MaritalStatusValue" />",</xsl:if><xsl:if test="mdm:DeceasedDate !=''">
	"deceasedDate": "<xsl:value-of select="substring-before(mdm:DeceasedDate, ' ')" />",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployeeIndicator">
	"employeeIndicator": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployeeIndicator" />",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployerName">
	"employerName": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployerName" />",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XBankruptDate !=''">
	"bankruptDate": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XBankruptDate" />",</xsl:if><xsl:if test="mdm:SinceDate !=''">
    "sourceEstablishedDate": "<xsl:value-of select="substring-before(mdm:SinceDate, ' ' )" />",</xsl:if><xsl:if test="mdm:LeftDate !=''">
    "sourceClosedDate": "<xsl:value-of select="substring-before(mdm:LeftDate, ' ' )" />",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XOccupationValue">
"occupation":	{
	"code": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XOccupationValue"/>"
	},</xsl:if><xsl:if test="mdm:TCRMPartyAddressBObj">
"addresses":	[
	<xsl:apply-templates select="mdm:TCRMPartyAddressBObj" mode="map"/>
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']">
"phones":	[
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']" mode="phone" />
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']">
"emails":	[
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']" mode="email" />

],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Fax']">
"faxes":	[
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Fax']" mode="fax" />
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Social Media']">
"socialMedia":	[
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Social Media']" mode="socialMedia" />
],</xsl:if><xsl:if test="mdm:TCRMPartyIdentificationBObj">
"identifiers":	[
	<xsl:apply-templates select="mdm:TCRMPartyIdentificationBObj" mode="map"/>
],</xsl:if><xsl:if test="mdm:TCRMPersonNameBObj">
"names":	[
	<xsl:apply-templates select="mdm:TCRMPersonNameBObj" mode="name"/>
],</xsl:if><xsl:if test="mdm:TCRMPartyPrivPrefBObj">
"preferences":	[
	<xsl:apply-templates select="mdm:TCRMPartyPrivPrefBObj" mode="map"/>
],</xsl:if>
<xsl:if test="mdm:TCRMAdminContEquivBObj !=''">
"sourceSystems":	[
	<xsl:apply-templates select="mdm:TCRMAdminContEquivBObj" mode="name"/>
]</xsl:if>
<xsl:if test="../mdm:KeyValueBObj!=''">
		    ,
			"bankingRelationships": [
			<xsl:apply-templates select="../mdm:KeyValueBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if test="../mdm:KeyValueBObj!=''">
		    ,
			"bankingServices": [
			<xsl:apply-templates select="../mdm:KeyValueBObj" mode="map" />
			],
		</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	
    <xsl:template name="OrganizationES" match="mdm:TCRMOrganizationBObj" mode="map">
{
	"ocvId" : "<xsl:value-of select="mdm:TCRMPartyIdentificationBObj[mdm:IdentificationValue='One Customer ID']/mdm:IdentificationNumber"/>",
	"partyType": "O",
	<xsl:if test="mdm:EstablishedDate !=''">"establishmentDate": "<xsl:value-of select="substring-before(mdm:EstablishedDate, ' ')" />",</xsl:if>
	"organisationType": "<xsl:value-of select="mdm:OrganizationValue"/>",<xsl:if test="mdm:ClientStatusValue">
	"status": "<xsl:value-of select="mdm:ClientStatusValue" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:EntityStatusValue">
	"entityStatus": "<xsl:value-of select="mdm:EntityStatusValue"/>",</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue != '' or mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue != ''">
"kycDetails":	{<xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue">
	"status": "<xsl:value-of select="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue"/>"</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue != '' and mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue != ''">,</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue">
	"verificationLevel": "<xsl:value-of select="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue"/>"</xsl:if>
	},</xsl:if><xsl:if test="mdm:IndustryType != '' or mdm:IndustryValue != '' ">
"industry":	{<xsl:if test="mdm:IndustryType">
	"code": "<xsl:value-of select="mdm:IndustryType"/>"</xsl:if><xsl:if test="mdm:IndustryType != '' and mdm:IndustryValue != '' ">,</xsl:if><xsl:if test="mdm:IndustryValue">
	"description": "<xsl:value-of select="mdm:IndustryValue"/>"</xsl:if>
	},</xsl:if><xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XBankruptDate !=''">
	"bankruptDate": "<xsl:value-of select="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XBankruptDate" />",</xsl:if><xsl:if test="mdm:SinceDate !=''">
    "sourceEstablishedDate": "<xsl:value-of select="substring-before(mdm:SinceDate, ' ' )" />",</xsl:if><xsl:if test="mdm:LeftDate !=''">
    "sourceClosedDate": "<xsl:value-of select="substring-before(mdm:LeftDate, ' ' )" />",</xsl:if><xsl:if test="mdm:SinceDate !=''">
    "sourceEstablishedDate": "<xsl:value-of select="mdm:SinceDate" />",</xsl:if><xsl:if test="mdm:LeftDate !=''">
    "sourceClosedDate": "<xsl:value-of select="mdm:LeftDate" />",</xsl:if><xsl:if test="mdm:TCRMPartyAddressBObj">
"addresses": [
	<xsl:apply-templates select="mdm:TCRMPartyAddressBObj" mode="map"/>
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']">
"phones": [
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']" mode="phone" />
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']">
"emails": [
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']" mode="email" />
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Fax']">
"faxes":	[
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Fax']" mode="fax" />
],</xsl:if><xsl:if test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Social Media']">
"socialMedia": [
	<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Social Media']" mode="socialMedia" />
],</xsl:if><xsl:if test="mdm:TCRMPartyIdentificationBObj">
"identifiers": [
	<xsl:apply-templates select="mdm:TCRMPartyIdentificationBObj" mode="map"/>
],</xsl:if><xsl:if test="mdm:TCRMOrganizationNameBObj">
"names": [
	<xsl:apply-templates select="mdm:TCRMOrganizationNameBObj" mode="name"/>
],</xsl:if><xsl:if test="mdm:TCRMPartyPrivPrefBObj">
"preferences": [
	<xsl:apply-templates select="mdm:TCRMPartyPrivPrefBObj" mode="map"/>
],</xsl:if><xsl:if test="mdm:TCRMPartyDemographicsBObj">
"commercialContacts": [
	<xsl:apply-templates select="mdm:TCRMPartyDemographicsBObj" mode="map"/>
],</xsl:if>
<xsl:if test="mdm:TCRMAdminContEquivBObj !=''">
"sourceSystems": [
	<xsl:apply-templates select="mdm:TCRMAdminContEquivBObj" mode="name"/>
]</xsl:if>
<xsl:if test="../mdm:KeyValueBObj!=''">
		    ,
			"bankingRelationships": [
			<xsl:apply-templates select="../mdm:KeyValueBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if test="../mdm:KeyValueBObj!=''">
		    ,
			"bankingServices": [
			<xsl:apply-templates select="../mdm:KeyValueBObj" mode="map" />
			],
		</xsl:if>
}
<xsl:if test="position() &gt; 0 and position() != last()">,
</xsl:if>
	</xsl:template>
	
	
    <xsl:template name="AddressES" match="mdm:TCRMPartyAddressBObj" mode="map">
{   <xsl:if test="mdm:AddressUsageValue">
	"addressUsageType": "<xsl:value-of select="mdm:AddressUsageValue" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineOne">
	"addressLineOne": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineOne" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineTwo">
	"addressLineTwo": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineTwo" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineThree">
	"addressLineThree": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineThree" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFour">
	"addressLineFour": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFour" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFive">
	"addressLineFive": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFive" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineSix">
	"addressLineSix": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineSix" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:City">
	"city": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:City" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:ZipPostalCode">
	"postalCode": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ZipPostalCode" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:ResidenceNumber">
	"residenceNumber":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ResidenceNumber" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:ResidenceValue">
	"residenceType":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ResidenceValue" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:ProvinceStateValue">
	"state": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ProvinceStateValue" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:CountryValue">
	"country": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:CountryValue" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:BuildingName">
	"buildingName":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:BuildingName" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StreetNumber">
	"streetNumber":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StreetNumber" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StreetPrefix">
	"streetPrefix":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StreetPrefix" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StreetName">
	"streetName":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StreetName" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StreetSuffix">
	"streetSuffix":"<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StreetSuffix" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:Region">
	"region": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:Region" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:DelId">
	"deliveryId": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:DelId" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:CountyCode !=''">
	"countyCode": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:CountyCode" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:LatitudeDegrees !=''">
	"latitudeDegrees": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:LatitudeDegrees" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:LongitudeDegrees !=''">
	"longitudeDegrees": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:LongitudeDegrees" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:ZipPostalBarCode !=''">
	"postalBarCode": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ZipPostalBarCode" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:PreDirectional !=''">
	"preDirectional": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:PreDirectional" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:PostDirectional !=''">
	"postDirectional": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:PostDirectional" />",</xsl:if>	<xsl:if test="mdm:TCRMAddressBObj/mdm:BoxDesignator !=''">
	"boxDesignator": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:BoxDesignator" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:BoxId !=''">
	"boxId": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:BoxId" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StnInfo !=''">
	"stnInfo": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StnInfo" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:StnId !=''">
	"stnId": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:StnId" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:DelDesignator !=''">
	"delDesignator": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:DelDesignator" />",</xsl:if><xsl:if test="mdm:TCRMAddressBObj/mdm:DelInfo !=''">
	"delInfo": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:DelInfo" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>		
	</xsl:template>
	<xsl:template name="PartyPrivPreferenceES" match="mdm:TCRMPartyPrivPrefBObj" mode="map">
{<xsl:if test="mdm:PrivPrefValue">
	"preferenceUsageType": "<xsl:value-of select="mdm:PrivPrefValue" />",</xsl:if><xsl:if test="mdm:ValueString">
	"preferenceValue": "<xsl:value-of select="mdm:ValueString" />",</xsl:if><xsl:if test="mdm:PrivPrefReasonValue">
	"preferenceReason": "<xsl:value-of select="mdm:PrivPrefReasonValue" />",</xsl:if><xsl:if test="mdm:SourceIdentValue">
	"source": "<xsl:value-of select="mdm:SourceIdentValue" />",</xsl:if>
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />",<xsl:if test="mdm:EndDate">
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="IdentifierES" match="mdm:TCRMPartyIdentificationBObj" mode="map">
{<xsl:if test="mdm:IdentificationValue">
	"identifierUsageType": "<xsl:value-of select="mdm:IdentificationValue" />",</xsl:if><xsl:if test="mdm:IdentificationNumber">
	"identifier": "<xsl:value-of select="mdm:IdentificationNumber" />",</xsl:if><xsl:if test="mdm:IdentificationIssueLocation">
	"identificationIssueLocation": "<xsl:value-of select="mdm:IdentificationIssueLocation" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if><xsl:if test="mdm:IdentificationExpiryDate !=''">,
	"expiryDate": "<xsl:value-of select="substring-before(mdm:IdentificationExpiryDate, ' ')" />"</xsl:if>
	<xsl:if test="mdm:TCRMExtension/mdm:XPartyIdentificationBObjExt/mdm:XOccurenceNum !=''">,
	"sequence": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPartyIdentificationBObjExt/mdm:XOccurenceNum" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="CommercialContactES" match="mdm:TCRMPartyDemographicsBObj" mode="map">
{
	"name": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Name" />",
	"title": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Title" />",
	"phone": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Phone" />",
	"fax": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Fax" />",
	"mobile": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Mobile" />",
	"preferred": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:PreferredIndicator" />"
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="PhoneES" match="mdm:TCRMPartyContactMethodBObj" mode="phone">
{<xsl:if test="mdm:ContactMethodUsageValue">
	"phoneUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber">
	"phone": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",</xsl:if><xsl:if test="mdm:PreferredContactMethodIndicator">
	"preferred": "<xsl:value-of select="mdm:PreferredContactMethodIndicator" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue !=''">,
	"status": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="EmailES" match="mdm:TCRMPartyContactMethodBObj" mode="email">
{<xsl:if test="mdm:ContactMethodUsageValue">
	"emailUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber">
	"email": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",</xsl:if><xsl:if test="mdm:PreferredContactMethodIndicator">
	"preferred": "<xsl:value-of select="mdm:PreferredContactMethodIndicator" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue !=''">,
	"status": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue" />"</xsl:if>
	
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="FaxES" match="mdm:TCRMPartyContactMethodBObj" mode="fax">
{<xsl:if test="mdm:ContactMethodUsageValue">
	"faxUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber">
	"fax": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",</xsl:if><xsl:if test="mdm:PreferredContactMethodIndicator">
	"preferred": "<xsl:value-of select="mdm:PreferredContactMethodIndicator" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if><xsl:if test="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue !=''">,
	"status": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:TCRMExtension/mdm:XContactMethodBObjExt/mdm:XContactMethodStatusValue" />"</xsl:if>
	
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="SocialMediaES" match="mdm:TCRMPartyContactMethodBObj" mode="socialMedia">
{
    "socialMediaUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",
    "socialMedia": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",
    "source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
    <xsl:if test="mdm:StartDate !=''">"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,</xsl:if>
    <xsl:if test="mdm:EndDate !=''">"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="OCVId" match="mdm:TCRMPartyIdentificationBObj" mode="ocvId">
	"ocvId": "<xsl:value-of select="mdm:IdentificationNumber"/>",
	</xsl:template>
	<xsl:template name="PersonNameES" match="mdm:TCRMPersonNameBObj" mode="name">
{<xsl:if test="mdm:NameUsageValue">
	"nameUsageType": "<xsl:value-of select="mdm:NameUsageValue" />",</xsl:if><xsl:if test="mdm:PrefixValue">
	"prefix": "<xsl:value-of select="mdm:PrefixValue" />",></xsl:if><xsl:if test="mdm:PrefixDescription">
	"title": "<xsl:value-of select="mdm:PrefixDescription" />",</xsl:if><xsl:if test="mdm:GivenNameOne">
	"firstName": "<xsl:value-of select="mdm:GivenNameOne" />",</xsl:if><xsl:if test="mdm:GivenNameTwo">
	"middleName": "<xsl:value-of select="mdm:GivenNameTwo" />",</xsl:if><xsl:if test="mdm:LastName">
	"lastName": "<xsl:value-of select="mdm:LastName" />",</xsl:if><xsl:if test="mdm:Suffix">
	"suffix": "<xsl:value-of select="mdm:Suffix" />",</xsl:if><xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	<xsl:template name="OrgNameES" match="mdm:TCRMOrganizationNameBObj" mode="name">
{
<xsl:if test="mdm:NameUsageValue">
	"nameUsageType": "<xsl:value-of select="mdm:NameUsageValue" />",</xsl:if>
	"name": "<xsl:value-of select="mdm:OrganizationName" />",<xsl:if test="mdm:SourceIdentifierValue">
	"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",</xsl:if><xsl:if test="mdm:StartDate !=''">
	"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"</xsl:if><xsl:if test="mdm:EndDate !=''">,
	"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"</xsl:if>
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	
	<xsl:template name="sourceSystems" match="mdm:TCRMAdminContEquivBObj" mode="name">
{
	"sourceSystemId": "<xsl:value-of select="mdm:AdminPartyId"/>",
	"sourceSystemName": "<xsl:value-of select="mdm:AdminSystemValue"/>"
}<xsl:if test="position() &gt; 0 and position() != last()">,</xsl:if>
	</xsl:template>
	
	<xsl:template name="BankingES" match="mdm:KeyValueBObj" mode="map">
		{
		"key": "<xsl:value-of select="mdm:Key" />",
		"value": "<xsl:value-of select="mdm:Value" />"
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
				,
	</xsl:if>
	</xsl:template>
</xsl:stylesheet>