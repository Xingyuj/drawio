package com.anz.mdm.ocv.api.downsteamservices;


import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;

import javax.naming.directory.InvalidAttributesException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.dto.APIResponse;
import com.anz.mdm.ocv.api.model.CAPParty;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.anz.mdm.ocv.common.v1.SourceSystem;
import com.anz.mdm.ocv.party.v1.Party;
import com.anz.mdm.ocv.party.v1.SearchPartyResultWrapper;
import com.ibm.mdm.schema.TCRMService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@DependsOn({"dataValidationService", "dataStandardisationService",
        "retrievePartyService", "capTransformationService", "maintainPartyService"})
public class OnboardService {

    private final DataValidationService dataValidationService;
    private final DataStandardisationService dataStandardisationService;
    private final RetrievePartyService retrievePartyService;
    private final CapTransformationService capTransformationService;
    private final MaintainPartyService maintainPartyService;

    @Autowired
    public OnboardService(DataValidationService dataValidationService,
                          DataStandardisationService dataStandardisationService,
                          RetrievePartyService retrievePartyService,
                          CapTransformationService capTransformationService,
                          MaintainPartyService maintainPartyService) {
        this.dataValidationService = dataValidationService;
        this.dataStandardisationService = dataStandardisationService;
        this.retrievePartyService = retrievePartyService;
        this.capTransformationService = capTransformationService;
        this.maintainPartyService = maintainPartyService;
    }

    public Boolean retrieveOrGenerateCapCisId(APIRequest<Party> apiRequest, APIRequest<Party> validatedRequest,
                                              String traceId) throws Exception {
        SourceSystem capSourceSys = new SourceSystem();
        capSourceSys.setSourceSystemName(OCVConstants.CAP_CIS_SOURCE);
        apiRequest.getRequestBody().getSourceSystems().add(capSourceSys);
        // find the lowest CAP CIS ID
        String capCisId;
        SearchPartyResultWrapper serchResult = retrievePartyService
                .getResponse(capTransformationService.getRetrieveRequest(validatedRequest), traceId);
        Map<String, Party> idMap = capTransformationService.getIdMap(serchResult);
        Party party = idMap.get(OCVConstants.CAP_CIS_SOURCE);

        if (party == null || party.getSourceSystems().isEmpty() || party.getSourceSystems().get(0) == null
                || StringUtils.isBlank(party.getSourceSystems().get(0).getSourceSystemId())) {
            // create cap cis id
            log.info("Retrieve lowest cap-cis id not available in OCV ID, going to generate a new one");
            CAPParty capParty = capTransformationService.createCapProfileV2(apiRequest, validatedRequest, traceId);
            Optional<SourceSystem> sourceSystem = capParty.getPartyApiReq().getRequestBody()
                    .getSourceSystems().stream().findFirst();
            capCisId = sourceSystem.map(SourceSystem::getSourceSystemId).orElse(null);
            if (StringUtils.isBlank(capCisId)) {
                throw new InvalidAttributesException("cap cis id creation failed");
            }
            capSourceSys.setSourceSystemId(capCisId);
            return true;
        }
        // there should be only one entity and its lowest
        capCisId = party.getSourceSystems().get(0).getSourceSystemId();
        log.info("received lowest cap-cis id: " + capCisId);
        capSourceSys.setSourceSystemId(capCisId);
        return false;
    }


    public APIRequest<Party> getPartyAPIRequestWithValidatedParty(Map<String, String> headers,
                                                                  Map<String, String> queryParameters,
                                                                  APIRequest<Party> apiRequest,
                                                                  Long dlStartTime) throws Exception {
        ValidatedParty validatedParty = getValidatedParty(headers, queryParameters,
                headers.get(OCVConstants.TRACE_ID_HEADER), apiRequest, dlStartTime);
        return new APIRequest<>(headers, queryParameters, validatedParty.getParty());
    }

    public ValidatedParty getValidatedParty(Map<String, String> headers, Map<String, String> queryParameters,
                                            String traceId,
                                            APIRequest<Party> apiRequest, Long dlStartTime) throws Exception {
        final StandardisedParty standardisedParty;
        final APIRequest<Party> standardisedApiRequest;
        LogUtil.debug(log, "maintainParty", traceId, "Invoking data standardisation service");
        standardisedParty = dataStandardisationService.standardiseParty(apiRequest);
        LogUtil.debug(log, "maintainParty", traceId, "Data standardisation completed",
                System.currentTimeMillis() - dlStartTime);
        standardisedApiRequest = new APIRequest<>(headers, queryParameters, standardisedParty.getParty());
        LogUtil.debug(log, "validateParty", traceId, "Invoking data validation service");
        // Change start for https://jira.service.anz/browse/OCT-25769
        ValidatedParty validatedParty = dataValidationService.validateParty(standardisedApiRequest);
        LogUtil.debug(log, "maintainParty", traceId, "Invoking RedHatRuleEngineService");
        validatedParty = dataStandardisationService.standardisePhoneWithRHDM(validatedParty, headers, queryParameters);
        // Change end for https://jira.service.anz/browse/OCT-25769
        return validatedParty;
    }

    public APIResponse createOrUpdateProspectParty(APIRequest<Party> apiRequest, String traceId,
                                                   Boolean createCustomerModeWithCAPCISID,
                                                   boolean isGoldenProfile) throws Exception {
        LogUtil.debug(log, "createOrUpdateProspectParty", traceId, "Entering: processParty method in "
                + "MaintainPartyService");
        String requestMDMXml = maintainPartyService.transformPartyRequest(apiRequest, isGoldenProfile,
                createCustomerModeWithCAPCISID);

        if (createCustomerModeWithCAPCISID != null && createCustomerModeWithCAPCISID) {
            requestMDMXml = requestMDMXml.replace(requestMDMXml.substring(requestMDMXml.indexOf("<requestTime"
                                    + ">") + "<requestTime>".length(),
                            requestMDMXml.indexOf("</requestTime>")),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss").format(new java.util.Date()));
        }

        LogUtil.debug(log, "maintain Party - createOrUpdateProspectParty", traceId,
                "Transformed RequestXML" + requestMDMXml + "Channel :" + apiRequest.getChannel(),
                System.currentTimeMillis());

        APIResponse apiResponse = invokeAndParseMdmResponse(traceId, requestMDMXml);
        LogUtil.debug(log, "createOrUpdateProspectParty", traceId,
                "Exiting: createOrUpdateProspectParty method in MaintainPartyService");
        return apiResponse;
    }
    
    private APIResponse invokeAndParseMdmResponse(String traceId, String requestMDMXml) throws Exception {
        APIResponse apiResponse = null;
        HttpEntity<TCRMService> xmlResponse = maintainPartyService.invokeMdmBackend(requestMDMXml, traceId);
        ValidationResult validationResult = new ValidationResult();
        maintainPartyService.validateMdmResponse(xmlResponse, traceId, validationResult, requestMDMXml);

        if (validationResult.getXmlResponse() != null) {
            xmlResponse = validationResult.getXmlResponse();
        }
        TCRMService tcrmService = xmlResponse.getBody();
        if (ObjectUtils.isNotEmpty(tcrmService)) {
            apiResponse = maintainPartyService.transformMdmResponse(traceId, tcrmService, validationResult);
        }
        return apiResponse;
    }

}
