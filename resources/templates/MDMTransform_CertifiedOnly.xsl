<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:mdm="http://www.ibm.com/mdm/schema"
	xmlns:cc="http://www.anz.com/cm/data/specs/CommercialContact/internal/00000001"
	exclude-result-prefixes="fo fn xs mdm cc">
	<xsl:output method="text" encoding="UTF-8" indent="yes" />
	<xsl:template match="/">
	{
		<xsl:apply-templates select="mdm:TCRMService/mdm:TxResponse/mdm:ResponseObject/mdm:SearchPartyWrapperBObj" mode="map" />
	}
	</xsl:template>	
	<xsl:template name="PartyRecords" match="mdm:SearchPartyWrapperBObj" mode="map">
	"certifiedResults" : [
     <xsl:for-each select="mdm:PartyWrapperBObj"><xsl:call-template name="CertifiedRecords"/></xsl:for-each>             	            						
	 ]
	
	</xsl:template>

	<xsl:template name="CertifiedRecords" match="mdm:PartyWrapperBObj" mode="map">
         			     			         			         			
	                      <xsl:if  test="mdm:TCRMPersonBObj/mdm:TCRMSuspectBObj/mdm:AdjustedMatchCategoryCode = 'Certified-A1'">
	                                <xsl:apply-templates select="mdm:TCRMPersonBObj" mode="map" />
	                                <xsl:if test="position() &gt; 0 and position() != last()">
                                       ,
                                    </xsl:if>
	                      </xsl:if>
	                      <xsl:if  test="mdm:TCRMOrganizationBObj/mdm:TCRMSuspectBObj/mdm:AdjustedMatchCategoryCode = 'Certified-A1'">
	                                <xsl:apply-templates select="mdm:TCRMOrganizationBObj"  mode="map" />
	                                <xsl:if test="position() &gt; 0 and position() != last()">
                                       ,
                                    </xsl:if>
	                      </xsl:if>                                    

    </xsl:template>
    
 
	<!-- PERSON -->
	<xsl:template name="PersonES" match="mdm:TCRMPersonBObj"
		mode="map">
		 {"ocvId" : "<xsl:value-of select="mdm:TCRMPartyIdentificationBObj[mdm:IdentificationValue='One Customer ID']/mdm:IdentificationNumber" />",
		<xsl:if test="mdm:TCRMSuspectBObj/mdm:Weight !=''">
			"matchingScore" : "<xsl:value-of select="mdm:TCRMSuspectBObj/mdm:Weight" />",
		</xsl:if>
		"partyType": "P",
		<xsl:if test="mdm:BirthDate !=''">
			"dateOfBirth": "<xsl:value-of select="substring-before(mdm:BirthDate, ' ')" />",
		</xsl:if>
		<xsl:if test="mdm:ClientStatusValue !=''">
			"status": "<xsl:value-of select="mdm:ClientStatusValue" />",
		</xsl:if>
		<xsl:if
			test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue !='' or mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue != ''">
			"kycDetails": {
			<xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue !=''">
				"status": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycStatusValue" />"
			</xsl:if>
			<xsl:if	test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue !=''">
				,
				"verificationLevel": "<xsl:value-of	select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XKycVerificationLevelValue" />"
			</xsl:if>
			},
		</xsl:if>
		<xsl:if test="mdm:GenderType !=''">
			"gender": "<xsl:value-of select="mdm:GenderType" />",
		</xsl:if>
		<xsl:if test="mdm:MaritalStatusValue !=''">
			"maritalStatus": "<xsl:value-of select="mdm:MaritalStatusValue" />",
		</xsl:if>
		<xsl:if test="mdm:DeceasedDate !=''">
			"deceasedDate": "<xsl:value-of select="substring-before(mdm:DeceasedDate, ' ')" />",
		</xsl:if>
		<xsl:if	test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployeeIndicator !=''">
			"employeeIndicator": "<xsl:value-of	select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployeeIndicator" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployerName !=''">
			"employerName": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XEmployerName" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XBankruptDate !=''">
	    "bankruptDate": "<xsl:value-of select="substring-before(mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XBankruptDate, ' ' )" />",
	   </xsl:if>
	  <xsl:if test="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XOccupationValue">
       "occupation":	{
       	   "code": "<xsl:value-of select="mdm:TCRMExtension/mdm:XPersonBObjExt/mdm:XOccupationValue"/>"
	   },</xsl:if>
		<xsl:if test="mdm:TCRMPartyAddressBObj !=''">
			"addresses": [
			<xsl:apply-templates select="mdm:TCRMPartyAddressBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if	test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number'] !=''">
			"phones": [
			<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']" mode="phone" />
			],
		</xsl:if>
		<xsl:if	test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address'] !=''">
			"emails": [
			<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']" mode="email" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMPartyIdentificationBObj !=''">
			"identifiers": [
			<xsl:apply-templates select="mdm:TCRMPartyIdentificationBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMPersonNameBObj!=''">
			"names": [
			<xsl:apply-templates select="mdm:TCRMPersonNameBObj" mode="name" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMPartyPrivPrefBObj!=''">
			"preferences": [
			<xsl:apply-templates select="mdm:TCRMPartyPrivPrefBObj"	mode="map" />
			],
		</xsl:if>
		"sourceSystems":[{
		"sourceSystemId": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminPartyId" />",
		"sourceSystemName": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue" />"
		}]
		<xsl:choose>
		<xsl:when test="../mdm:JurisdictionBObj!=''">
			,
			"jurisdictionDetails":
			[
				<xsl:apply-templates select="../mdm:JurisdictionBObj"	mode="map" />
			]
		</xsl:when>
		<xsl:otherwise>
			<xsl:if test="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue= 'CAP-CIS'">
			,
			"jurisdictionDetails":[
			{
				"jurisdictionName":"AU"
			}]
			</xsl:if>
		</xsl:otherwise>
		</xsl:choose>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="OrganizationES" match="mdm:TCRMOrganizationBObj" mode="map">
		{ "ocvId" : "<xsl:value-of select="mdm:TCRMPartyIdentificationBObj[mdm:IdentificationValue='One Customer ID']/mdm:IdentificationNumber" />",
		<xsl:if test="mdm:TCRMSuspectBObj/mdm:Weight !=''">
			"matchingScore" : "<xsl:value-of select="mdm:TCRMSuspectBObj/mdm:Weight" />",
		</xsl:if>
		"partyType": "O",
		<xsl:if test="mdm:EstablishedDate !=''">
			"establishmentDate": "<xsl:value-of select="substring-before(mdm:EstablishedDate, ' ')" />",
		</xsl:if>
		<xsl:if test="mdm:ClientStatusValue !=''">
			"status": "<xsl:value-of select="mdm:ClientStatusValue" />",
		</xsl:if>
		<xsl:if test="mdm:OrganizationValue !=''">
			"organisationType": "<xsl:value-of select="mdm:OrganizationValue" />",
		</xsl:if>
		"kycDetails":{
		<xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue !=''">
			"status": "<xsl:value-of select="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycStatusValue" />"
		</xsl:if>
		<xsl:if test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue !=''">
			,
			"verificationLevel": "<xsl:value-of	select="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XKycVerificationLevelValue" />"
		</xsl:if>
		},
		<xsl:if	test="mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XBankruptDate !=''">
			"bankruptDate": "<xsl:value-of select="substring-before(mdm:TCRMExtension/mdm:XOrganizationBObjExt/mdm:XBankruptDate, ' ' )" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMPartyAddressBObj !=''">
			"addresses": [
			<xsl:apply-templates select="mdm:TCRMPartyAddressBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if	test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number'] !=''">
			"phones": [
			<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Telephone Number']" mode="phone" />
			],
		</xsl:if>
		<xsl:if	test="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address'] !=''">
			"emails": [
			<xsl:apply-templates select="mdm:TCRMPartyContactMethodBObj[mdm:TCRMContactMethodBObj/mdm:ContactMethodValue='Email Address']"	mode="email" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMPartyIdentificationBObj !=''">
			"identifiers": [
			<xsl:apply-templates select="mdm:TCRMPartyIdentificationBObj" mode="map" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMOrganizationNameBObj !=''">
			"names": [
			<xsl:apply-templates select="mdm:TCRMOrganizationNameBObj"	mode="name" />
			],
		</xsl:if>
		<!-- "qualifiedNames": [ <xsl:apply-templates select="mdm:TCRMOrganizationNameBObj" 
			mode="qname"/> ], -->
		<xsl:if test="mdm:TCRMPartyPrivPrefBObj !=''">
			"preferences": [
			<xsl:apply-templates select="mdm:TCRMPartyPrivPrefBObj"	mode="map" />
			],
		</xsl:if>
		<xsl:if test="mdm:TCRMPartyDemographicsBObj !=''">
			"commercialContacts": [
			<xsl:apply-templates select="mdm:TCRMPartyDemographicsBObj"	mode="map" />
			],
		</xsl:if>
		<xsl:if	test="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue !='' and mdm:TCRMAdminContEquivBObj/mdm:AdminPartyId !=''">
			
		"sourceSystems": [{
		"sourceSystemId": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminPartyId" />",
		"sourceSystemName": "<xsl:value-of select="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue" />"
		}]
		<xsl:choose>
		<xsl:when test="../mdm:JurisdictionBObj!=''">
			,
			"jurisdictionDetails":
			[
				<xsl:apply-templates select="../mdm:JurisdictionBObj"	mode="map" />
			]
		</xsl:when>
		<xsl:otherwise>
			<xsl:if test="mdm:TCRMAdminContEquivBObj/mdm:AdminSystemValue= 'CAP-CIS'">
			,
			"jurisdictionDetails":[
			{
				"jurisdictionName":"AU"
			}]
			</xsl:if>
		</xsl:otherwise>
		</xsl:choose>
		}
		</xsl:if>
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="AddressES" match="mdm:TCRMPartyAddressBObj"	mode="map">
		{
		<xsl:if test="mdm:AddressUsageValue !=''">
			"addressUsageType": "<xsl:value-of select="mdm:AddressUsageValue" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineOne !=''">
			"addressLineOne": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineOne" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineTwo !=''">
			"addressLineTwo": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineTwo" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:AddressLineThree !=''">
			"addressLineThree": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:AddressLineThree" />",
		</xsl:if>
		<xsl:if	test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFour !=''">
			"addressLineFour": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFour" />",
		</xsl:if>
		<xsl:if	test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFive !=''">
			"addressLineFive": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineFive" />",
		</xsl:if>
		<xsl:if	test="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineSix !=''">
			"addressLineSix": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:TCRMExtension/mdm:XAddressBObjExt/mdm:XAddressLineSix" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:City !=''">
			"city": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:City" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:ZipPostalCode !=''">
			"postalCode": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ZipPostalCode" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:ProvinceStateValue !=''">
			"state": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:ProvinceStateValue" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:CountryValue !=''">
			"country": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:CountryValue" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:Region !=''">
			"region": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:Region" />",
		</xsl:if>
		<xsl:if test="mdm:TCRMAddressBObj/mdm:DelId !=''">
			"deliveryId": "<xsl:value-of select="mdm:TCRMAddressBObj/mdm:DelId" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentifierValue !=''">
			"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		</xsl:if>
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="PartyPrivPreferenceES" match="mdm:TCRMPartyPrivPrefBObj" mode="map">
		{
		<xsl:if test="mdm:PrivPrefValue!=''">
			"preferenceType": "<xsl:value-of select="mdm:PrivPrefValue" />",
		</xsl:if>
		<xsl:if test="mdm:ValueString!=''">
			"preferenceValue": "<xsl:value-of select="mdm:ValueString" />",
		</xsl:if>
		<xsl:if test="mdm:PrivPrefReasonValue !=''">
			"preferenceReason": "<xsl:value-of select="mdm:PrivPrefReasonValue" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentValue!=''">
			"source": "<xsl:value-of select="mdm:SourceIdentValue" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="IdentifierES" match="mdm:TCRMPartyIdentificationBObj" mode="map">
		{
		<xsl:if test="mdm:IdentificationValue!=''">
			"identifierUsageType": "<xsl:value-of select="mdm:IdentificationValue" />",
		</xsl:if>
		<xsl:if test="mdm:IdentificationNumber !=''">
			"identifier": "<xsl:value-of select="mdm:IdentificationNumber" />",
		</xsl:if>
		<xsl:if test="mdm:IdentificationIssueLocation!=''">
			"identificationIssueLocation": "<xsl:value-of select="mdm:IdentificationIssueLocation" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentifierValue !=''">
			"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		</xsl:if>
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="CommercialContactES" match="mdm:TCRMPartyDemographicsBObj"	mode="map">
		{
		"name": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Name" />",
		"title": "<xsl:value-of	select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Title" />",
		"phone": "<xsl:value-of	select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Phone" />",
		"fax": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Fax" />",
		"mobile": "<xsl:value-of select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:Mobile" />",
		"preferred": "<xsl:value-of	select="mdm:TCRMDemographicsSpecValueBObj/mdm:AttributeValueBObj/mdm:SpecValueXML/cc:CommercialContactList/cc:CommercialContact/cc:PreferredIndicator" />"
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="PhoneES" match="mdm:TCRMPartyContactMethodBObj"	mode="phone">
		{
		"phoneUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",
		"phone": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",

		<xsl:if test="mdm:PreferredContactMethodIndicator!=''">
			"preferred": "<xsl:value-of select="mdm:PreferredContactMethodIndicator" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentifierValue!=''">
			"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		</xsl:if>
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="EmailES" match="mdm:TCRMPartyContactMethodBObj"	mode="email">
		{
		"emailUsageType": "<xsl:value-of select="mdm:ContactMethodUsageValue" />",
		"email": "<xsl:value-of select="mdm:TCRMContactMethodBObj/mdm:ReferenceNumber" />",
		<xsl:if test="mdm:PreferredContactMethodIndicator !=''">
			"preferred": "<xsl:value-of select="mdm:PreferredContactMethodIndicator" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentifierValue!=''">
			"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		</xsl:if>
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="OCVId" match="mdm:TCRMPartyIdentificationBObj"	mode="ocvId">
		"ocvId": "<xsl:value-of select="mdm:IdentificationNumber" />",
	</xsl:template>
	<xsl:template name="PersonNameES" match="mdm:TCRMPersonNameBObj" mode="name">
		{
		<xsl:if test="mdm:NameUsageValue!=''">
			"nameUsageType": "<xsl:value-of select="mdm:NameUsageValue" />",
		</xsl:if>
		<xsl:if test="mdm:PrefixValue !=''">
			"prefix": "<xsl:value-of select="mdm:PrefixValue" />",
		</xsl:if>
		<xsl:if test="mdm:PrefixDescription !=''">
			"title": "<xsl:value-of select="mdm:PrefixDescription" />",
		</xsl:if>
		<xsl:if test="mdm:GivenNameOne !=''">
			"firstName": "<xsl:value-of select="mdm:GivenNameOne" />",
		</xsl:if>
		<xsl:if test="mdm:GivenNameTwo !=''">
			"middleName": "<xsl:value-of select="mdm:GivenNameTwo" />",
		</xsl:if>
		<xsl:if test="mdm:LastName !=''">
			"lastName": "<xsl:value-of select="mdm:LastName" />",
		</xsl:if>
		<xsl:if test="mdm:Suffix !=''">
			"suffix": "<xsl:value-of select="mdm:Suffix" />",
		</xsl:if>
		<xsl:if test="mdm:SourceIdentifierValue !=''">
			"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		</xsl:if>
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="OrgNameES" match="mdm:TCRMOrganizationNameBObj"	mode="name">
		{
		"nameUsageType": "<xsl:value-of select="mdm:NameUsageValue" />",
		"name": "<xsl:value-of select="mdm:OrganizationName" />",
		"source": "<xsl:value-of select="mdm:SourceIdentifierValue" />",
		<xsl:if test="mdm:StartDate !=''">
			"startDate": "<xsl:value-of select="substring-before(mdm:StartDate, ' ')" />"
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			,
		</xsl:if>
		<xsl:if test="mdm:EndDate !=''">
			"endDate": "<xsl:value-of select="substring-before(mdm:EndDate, ' ')" />"
		</xsl:if>
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>
	<xsl:template name="JurisdictionES" match="mdm:JurisdictionBObj" mode="map">
		{
		"jurisdictionName": "<xsl:value-of select="mdm:Name" />"
		}
		<xsl:if test="position() &gt; 0 and position() != last()">
			,
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>