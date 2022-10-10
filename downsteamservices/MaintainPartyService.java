package com.anz.mdm.ocv.api.downsteamservices;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.MDMRequestTemplates;
import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.converter.OrganizationObjectConverter;
import com.anz.mdm.ocv.api.converter.PersonObjectConverter;
import com.anz.mdm.ocv.api.dto.APIResponse;
import com.anz.mdm.ocv.api.exception.MDMException;
import com.anz.mdm.ocv.api.processor.MaintainPartyServiceProcessor;
import com.anz.mdm.ocv.api.transform.Context;
import com.anz.mdm.ocv.api.util.ExceptionUtil;
import com.anz.mdm.ocv.api.util.IdempotencyConfigUtil;
import com.anz.mdm.ocv.api.util.LogRequestModel;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.util.RequestTransfomerUtil;
import com.anz.mdm.ocv.api.util.StreetSuffixConfig;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.anz.mdm.ocv.common.v1.CodeDescription;
import com.anz.mdm.ocv.common.v1.Identifier;
import com.anz.mdm.ocv.party.v1.Party;
import com.ibm.mdm.schema.ControlExtensionProperty;
import com.ibm.mdm.schema.DWLError;
import com.ibm.mdm.schema.PartyWrapperBObjType;
import com.ibm.mdm.schema.TCRMOrganizationBObjType;
import com.ibm.mdm.schema.TCRMPersonBObjType;
import com.ibm.mdm.schema.TCRMService;
import com.ibm.mdm.schema.TxResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Service class for create/update a party
 */

@Slf4j
@Service
public class MaintainPartyService extends AbstractMdmservice {

    @Value("${mdmEndpoint.url}")
    private String url;
    @Autowired
    @Qualifier("MaintainPartyServiceMDMRestTemplate")
    private RestTemplate restTemplate;
    @Autowired
    @Qualifier("MaintainPartyMDMRestTemplate")
    private RestTemplate maintainPartyMDMRestTemplate;
    @Autowired
    private LogRequestModel logAttributes;
    @Autowired
    private PersonObjectConverter personObjectConverter;
    @Autowired
    private OrganizationObjectConverter organizationObjectConverter;
    @Value("${anzx.address.channels}")
    private String anzxAddressChannel;
    @Autowired
    private StreetSuffixConfig streetSuffixCfg;
    @Autowired
    private MDMRetryService mdmRetryService;
    @Autowired
    private IdempotencyConfigUtil idempotencyUtil;
    public IdempotencyConfigUtil getIdempotencyUtil() {
        return idempotencyUtil;
    }

    @Override
    public ValidationResult validateResponse(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponse", traceId, "Entering: validateResponse method in MaintainPartyService");
        ValidationResult validationResult = new ValidationResult();
        String status = checkResponseStatus(xmlResponse, traceId);
        if (!("SUCCESS".equalsIgnoreCase(status) || "WARNING".equalsIgnoreCase(status))) {
            LogUtil.debug(log, "validateResponse", traceId, "Request has failed");
            validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
        } else {
            validationResult.setStatus(status);
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponse", traceId, "Exit: validateResponse method in MaintainPartyService",
                (endTime - startTime));
        return validationResult;
    }

    @Override
    public ValidationResult validateMdmResponse(HttpEntity<TCRMService> xmlResponse, String traceId,
                                                ValidationResult validationResult,
                                                String requestMDMXml) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateMdmResponse", traceId, "Entering: validateResponse method in MaintainPartyService");
        validateResponseBody(xmlResponse, traceId, validationResult, requestMDMXml);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateMdmResponse", traceId, "Exit: validateResponse method in MaintainPartyService",
                (endTime - startTime));
        return validationResult;
    }

    private String validateResponseBody(HttpEntity<TCRMService> xmlResponse, String traceId,
                                        ValidationResult validationResult, String requestMDMXml) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponseBody", traceId,
                "Entering: validateResponseBody method in MaintainPartyService");
        String status = null;
        HttpEntity<TCRMService> xmlResponseReturn = null;
        TCRMService responseBody = xmlResponse.getBody();
        if (null != responseBody) {
            TxResult txResult = responseBody.getTxResponse().getTxResult();
            status = txResult.getResultCode();
            if (!("SUCCESS".equalsIgnoreCase(status) || "WARNING".equalsIgnoreCase(status))) {
                LogUtil.debug(log, "validateResponseBody", traceId, "Request has failed");
                DWLError error = txResult.getDWLError().get(0);
                xmlResponseReturn = mdmRetryService.handleExceptions(this, traceId, validationResult, requestMDMXml,
                        error, logAttributes, mdmRetryService);
            } else {
                validationResult.setStatus(status);
            }
        }
        validationResult.setXmlResponse(xmlResponseReturn);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponseBody", traceId, "Exit: validateResponseBody method in MaintainPartyService",
                (endTime - startTime));
        return status;
    }

    private String getPartyType(APIRequest<Party> apiRequest) {
        String partyType = null;
        if (null != apiRequest.getRequestBody()) {
            partyType = apiRequest.getRequestBody().getPartyType();
        }
        return partyType;
    }

    public String transformPartyRequest(APIRequest<Party> apiRequest, String fenergoSourceId, boolean isGoldenProfile)
            throws Exception {
        return transformPartyRequest(apiRequest, fenergoSourceId, null, isGoldenProfile, false);
    }

    public String transformPartyRequest(APIRequest<Party> apiRequest, String fenergoSourceId,
                                        Boolean createCustomerModeWithCAPCISID, boolean isGoldenProfile,
                                        boolean crmParty)
            throws Exception {
        LogUtil.debug(log, "transformPartyRequest", apiRequest.getTraceId(),
                "Entering: transformPartyRequest method in MaintainPartyService");
        String requestxml = null;
        String partyType = getPartyType(apiRequest);
        if (partyType.equalsIgnoreCase(OCVConstants.NON_INDIVIDUAL_PARTY_TYPE)) {
            requestxml = createMDMOrgRequestXML(apiRequest);
        } else {
            LogUtil.debug(log, "transformPartyRequest", apiRequest.getTraceId(), "Individual Request");
            requestxml = crmParty
                    ? createMDMPersonRequestXML(apiRequest, isGoldenProfile, createCustomerModeWithCAPCISID) :
                    createMDMPersonRequestXML(apiRequest, fenergoSourceId, isGoldenProfile);
        }
        LogUtil.debug(log, "transformPartyRequest", apiRequest.getTraceId(),
                "Exiting: transformPartyRequest method in MaintainPartyService");
        return requestxml;
    }

    public String transformPartyRequest(APIRequest<Party> apiRequest, boolean isGoldenProfile,
                                        Boolean createCustomerModeWithCAPCISID) throws Exception {
        return transformPartyRequest(apiRequest, null, createCustomerModeWithCAPCISID, isGoldenProfile, true);
    }

    public HttpEntity<String> invokeBackend(String requestMDMXml, String traceId) {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Entering: invokeBackend method in MaintainPartyService");
        HttpHeaders headers = prepareHeadersForBackened(traceId);
        HttpEntity<String> request = new HttpEntity<String>(requestMDMXml, headers);
        HttpEntity<String> xmlResponse = invokeDownStreamService(request, url, restTemplate, HttpMethod.PUT, traceId);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Exit: invokeBackend method in MaintainPartyService",
                (endTime - startTime));
        return xmlResponse;
    }

    public HttpEntity<TCRMService> invokeMdmBackend(String requestMDMXml, String traceId) {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Entering: invokeBackend method in MaintainPartyService");
        HttpHeaders headers = prepareHeadersForBackened(traceId);
        HttpEntity<String> request = new HttpEntity<String>(requestMDMXml, headers);
        HttpEntity<TCRMService> xmlResponse = invokeMdmDownStreamService(request, url, maintainPartyMDMRestTemplate,
                HttpMethod.PUT, traceId);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Exit: invokeBackend method in MaintainPartyService",
                (endTime - startTime));
        return xmlResponse;
    }

    public String processParty(APIRequest<Party> apiRequest, String requestTime, String traceId,
                               boolean isGoldenProfile) throws IOException, Exception {
        LogUtil.debug(log, "processParty", traceId, "Entering: processParty method in MaintainPartyService");
        // Remove ocv id in interaction 3 for both Fenergo and Cap profile
        // update in mdm. Also check for delta(tax, occupation) update for updating channel
        filterRequestForAnzxInteraction3(apiRequest);
        String requestMDMXml = transformPartyRequest(apiRequest, null, isGoldenProfile);

        /*
         * LogUtil.info(log, "maintain Party - processParty", traceId,
         * "Transformed RequestXML", (System.currentTimeMillis()),
         * apiRequest.getChannel(), requestMDMXml,
         * OCVConstants.MAINTAINPARTY_SERVICE);
         */
        LogUtil.debug(log, "maintain Party - processParty", traceId,
                "Transformed RequestXML" + requestMDMXml + "Channel :" + apiRequest.getChannel(),
                System.currentTimeMillis());

        String response = null;
        HttpEntity<String> xmlResponse = invokeBackend(requestMDMXml, traceId);
        ValidationResult validationResult = validateResponse(xmlResponse, traceId);
        if (!validationResult.isValid()) {
            LogUtil.error(log, "processParty", traceId, "Response Validation failed", validationResult.getErrorCode(),
                    logAttributes.getChannel(), logAttributes.getPartyType(), logAttributes.getServiceName());
            String errorMessage = null;
            if (xmlResponse != null) {
                errorMessage = ExceptionUtil.getErrorMessage(xmlResponse.getBody());
            }
            LogUtil.logErrorMessage(errorMessage, traceId, logAttributes.getChannel(), logAttributes.getPartyType());
            LogUtil.logErrorMessage(xmlResponse.getBody(), traceId, logAttributes.getChannel(),
                    logAttributes.getPartyType());
            throw new MDMException(validationResult.getErrorCode(), errorMessage);
        } else if ("WARNING".equalsIgnoreCase(validationResult.getStatus())) {
            response = "{\"message\":\"Data submitted already exist, no updates applied\"}";
        } else {
            response = transformMDMResponse(xmlResponse, traceId);
        }

        LogUtil.debug(log, "processParty", traceId, "Exiting: processParty method in MaintainPartyService");
        return response;
    }

    public APIResponse createOrUpdateParty(APIRequest<Party> apiRequest, String requestTime, String traceId,
                                           String fenergoSourceId, boolean isGoldenProfile) throws IOException,
            Exception {
        LogUtil.debug(log, "processParty", traceId, "Entering: processParty method in MaintainPartyService");
        // Remove ocv id in interaction 3 for both Fenergo and Cap profile
        // update in mdm. Also check for delta(tax, occupation) update for
        // updating channel
        filterRequestForAnzxInteraction3(apiRequest);
        String requestMDMXml = transformPartyRequest(apiRequest, fenergoSourceId, isGoldenProfile);

        // LogUtil.info(log, "maintain Party - processParty", traceId,
        // "Transformed RequestXML",
        // (System.currentTimeMillis()), apiRequest.getChannel(), requestMDMXml,
        // OCVConstants.MAINTAINPARTY_SERVICE);
        LogUtil.debug(log, "maintain Party - createOrUpdateParty", traceId,
                "Transformed RequestXML" + requestMDMXml + "Channel :" + apiRequest.getChannel(),
                System.currentTimeMillis());

        APIResponse apiResponse = invokeAndParseMdmResponse(traceId, requestMDMXml);
        LogUtil.debug(log, "processParty", traceId, "Exiting: processParty method in MaintainPartyService");
        return apiResponse;
    }

    private APIResponse invokeAndParseMdmResponse(String traceId, String requestMDMXml) throws Exception {
        APIResponse apiResponse = null;
        HttpEntity<TCRMService> xmlResponse = invokeMdmBackend(requestMDMXml, traceId);
        ValidationResult validationResult = new ValidationResult();
        validateMdmResponse(xmlResponse, traceId, validationResult, requestMDMXml);

        if (validationResult.getXmlResponse() != null) {
            xmlResponse = validationResult.getXmlResponse();
        }
        TCRMService tcrmService = xmlResponse.getBody();
        if (ObjectUtils.isNotEmpty(tcrmService)) {
            apiResponse = transformMdmResponse(traceId, tcrmService, validationResult);
        }
        return apiResponse;
    }

    public APIResponse transformMdmResponse(String traceId, TCRMService tcrmService, ValidationResult validationResult)
            throws Exception {
        APIResponse apiResponse = null;
        Object object = tcrmService.getTxResponse().getResponseObject().getCommonBObj().get(0).getValue();

        if (object instanceof TCRMPersonBObjType) {
            TCRMPersonBObjType tcrmPersonObject = (TCRMPersonBObjType) object;
            apiResponse = personObjectConverter.convertPersonDetails(tcrmPersonObject, traceId);
        } else if (object instanceof TCRMOrganizationBObjType) {
            TCRMOrganizationBObjType tcrmOrgObject = (TCRMOrganizationBObjType) object;
            apiResponse = organizationObjectConverter.convertOrgDetails(tcrmOrgObject, traceId);
        } else if (object instanceof PartyWrapperBObjType) {
            PartyWrapperBObjType partyWrapperBObjType = (PartyWrapperBObjType) object;
            List<DWLError> dwlErrorList = null != partyWrapperBObjType.getDWLStatus()
                    ? partyWrapperBObjType.getDWLStatus().getDWLError() : null;
            if (null != dwlErrorList) {
                DWLError error = dwlErrorList.get(0);
                String errorCode = error.getReasonCode();
                String errMessage = error.getErrorMessage();
                validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
                LogUtil.error(log, "maintainParty - validateResponseBody", traceId, "Response Validation failed",
                        errMessage, errorCode);
                LogUtil.logErrorMessage(errMessage, traceId, logAttributes.getChannel(), logAttributes.getPartyType());
                throw new MDMException(OCVConstants.MDM_EXCEPTION_ERROR_CODE, errMessage);
            } else {
                apiResponse = transformPartyWrapperObj(traceId, partyWrapperBObjType);
            }
        }
        checkIsResponseFromIdempotency(tcrmService, apiResponse);
        removeProfileIdForOtherResponses(tcrmService, apiResponse);
        return apiResponse;
    }

    private void checkIsResponseFromIdempotency(TCRMService tcrmService, APIResponse apiResponse) {
        if (null != tcrmService.getResponseControl() && null != tcrmService.getResponseControl().getDWLControl()) {
            List<ControlExtensionProperty> controlExtensions = tcrmService.getResponseControl().getDWLControl()
                    .getControlExtensionProperty();

            ControlExtensionProperty controlExtension = controlExtensions.stream()
                    .filter(control -> OCVConstants.RESPONSE_FROM_IDEMPOTENCY.equalsIgnoreCase(control.getName()))
                    .findAny().orElse(null);
            if (null != controlExtension) {
                apiResponse.setResponseFromIdempotency(Boolean.valueOf(controlExtension.getValue()));
                LogUtil.debug(log, "maintainPartyService", logAttributes.getTraceId(),
                        "Response from Idempotency: " + Boolean.valueOf(controlExtension.getValue()));
            }

        }

    }

    private APIResponse transformPartyWrapperObj(String traceId, PartyWrapperBObjType partyWrapperBObjType) {
        APIResponse apiResponse = null;
        TCRMPersonBObjType personsObject = partyWrapperBObjType.getTCRMPersonBObj();
        TCRMOrganizationBObjType orgObject = partyWrapperBObjType.getTCRMOrganizationBObj();
        if (ObjectUtils.isNotEmpty(personsObject)) {
            apiResponse = personObjectConverter.convertPersonDetails(personsObject, traceId);
        } else if (ObjectUtils.isNotEmpty(orgObject)) {
            apiResponse = organizationObjectConverter.convertOrgDetails(orgObject, traceId);
        }
        return apiResponse;
    }

    private void filterRequestForAnzxInteraction3(APIRequest<Party> apiRequest) {
        String requestMode = apiRequest.getRequestMode();
        if (OCVConstants.REQUEST_MODE_UPDATE_ANZX.equalsIgnoreCase(requestMode)) {
            List<Identifier> identifiers = apiRequest.getRequestBody().getIdentifiers();
            String selfCertTaxFlag = apiRequest.getRequestBody().getSelfCertTaxFlag();
            String selfCertTaxDate = apiRequest.getRequestBody().getSelfCertTaxDate();
            String sourceOccupationCode = null;
            CodeDescription occupation = apiRequest.getRequestBody().getOccupation();
            if (ObjectUtils.isNotEmpty(occupation)) {
                sourceOccupationCode = occupation.getSourceOccupationCode();
            }
            removeOcvId(identifiers);
            if (StringUtils.isNotEmpty(selfCertTaxFlag) || StringUtils.isNotEmpty(selfCertTaxDate)
                    || StringUtils.isNotEmpty(sourceOccupationCode)) {
                apiRequest.setChannel(OCVConstants.FENERGO_SOURCE_DELTA);
            }
        }
    }

    private void removeOcvId(List<Identifier> identifiers) {
        if (!CollectionUtils.isEmpty(identifiers)) {
            for (Iterator<Identifier> iterator = identifiers.iterator(); iterator.hasNext(); ) {
                Identifier identifier = iterator.next();
                if (OCVConstants.ONE_CUSTOMER_ID.equalsIgnoreCase(identifier.getIdentifierUsageType())) {
                    iterator.remove();
                }
            }
        }
    }

    public String transformMDMResponse(HttpEntity<String> xmlResponse, String traceId) {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "transformMDMResponse", traceId,
                "Entering: transformMDMResponse method in MaintainPartyService");
        String jsonResponse = transformXmlResponseToJson(xmlResponse, Context.PARTYAPI, traceId);
        LogUtil.debug(log, "transformMDMResponse", traceId, "Exit: transformMDMResponse method in MaintainPartyService",
                System.currentTimeMillis() - startTime);
        return jsonResponse;
    }

    public String checkResponseStatus(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "checkResponseStatus", traceId,
                "Entering: checkResponseStatus method in MaintainPartyService");
        String status = null;
        String response = "";
        if (null != xmlResponse.getBody()) {
            response = xmlResponse.getBody();
            if (null != response && response.contains(MDMRequestTemplates.SUCCESS_RESULT_CODE)
                    && response.contains(MDMRequestTemplates.WARNING_COMP_CODE)
                    && response.contains(MDMRequestTemplates.WARNING_WRAPPER_BOBJ)
                    && response.contains(MDMRequestTemplates.WARNING_MESSAGE)) {
                status = "WARNING";
            } else if (null != response && response.contains(MDMRequestTemplates.SUCCESS_RESULT_CODE)
                    && !response.contains(MDMRequestTemplates.WARNING_WRAPPER_BOBJ)) {
                status = "SUCCESS";
            } else {
                status = "FATAL";
            }
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "checkResponseStatus", traceId, "Exit: checkResponseStatus method in MaintainPartyService",
                (endTime - startTime));
        return status;
    }

    public String createMDMPersonRequestXML(APIRequest<Party> apiRequest, boolean isGoldenProfile,
                                            Boolean createCustomerModeWithCAPCISID) throws IOException {
        StringBuilder request = new StringBuilder(createMDMPersonRequestXML(apiRequest, null, isGoldenProfile));
        if (createCustomerModeWithCAPCISID != null) {
            RequestTransfomerUtil.addNewLineToTemplate(request, "DWLControl", "ControlExtensionProperty",
                    "CreateCustomerModeWithCAPCISID", createCustomerModeWithCAPCISID);
        }
        return request.toString();
    }

    public String createMDMPersonRequestXML(APIRequest<Party> apiRequest, String fenergoSourceId,
                                            boolean isGoldenProfile) throws IOException {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "createMDMPersonRequestXML", apiRequest.getTraceId(),
                "Entering: createMDMPersonRequestXML method in MaintainPartyService");
        String xmlFilePath = MDMRequestTemplates.MDM_MAINTAIN_PARTY_REQUEST_XML_PERSON;
        String requestXml = RequestTransfomerUtil.fetchTemplate(xmlFilePath, apiRequest.getTraceId());
        StringBuilder request = new StringBuilder();
        try {
            request.append(requestXml);
            if (null != apiRequest.getRequestBody()) {
                MaintainPartyServiceProcessor maintainServiceProcessor = new MaintainPartyServiceProcessor();
                request = maintainServiceProcessor.preparePersonMDMRequest(request, apiRequest, anzxAddressChannel,
                        streetSuffixCfg, fenergoSourceId, getIdempotencyUtil(), isGoldenProfile);
            }
        } catch (Exception ex) {
            throw ex;
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "createMDMPersonRequestXML", apiRequest.getTraceId(),
                "Exit: createMDMPersonRequestXML method in MaintainPartyService", (endTime - startTime));
        return request.toString();
    }

    public String createMDMOrgRequestXML(APIRequest<Party> apiRequest) throws IOException {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "createMDMOrgRequestXML", apiRequest.getTraceId(),
                "Entering: createMDMOrgRequestXML method in MaintainPartyService");
        String xmlFilePath = MDMRequestTemplates.MDM_MAINTAIN_PARTY_REQUEST_XML_ORG;
        String requestXml = RequestTransfomerUtil.fetchTemplate(xmlFilePath, apiRequest.getTraceId());
        StringBuilder request = new StringBuilder();
        try {
            request.append(requestXml);

            if (null != apiRequest.getRequestBody()) {
                MaintainPartyServiceProcessor maintainServiceProcessor = new MaintainPartyServiceProcessor();
                request = maintainServiceProcessor.prepareOrgMDMRequest(request, apiRequest, anzxAddressChannel,
                        streetSuffixCfg);
            }
        } catch (Exception ex) {
            throw ex;
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "createMDMOrgRequestXML", apiRequest.getTraceId(),
                "Exit: createMDMOrgRequestXML method in MaintainPartyService", (endTime - startTime));
        return request.toString();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private void removeProfileIdForOtherResponses(TCRMService tcrmService, APIResponse apiResponse) {
        if (null != tcrmService.getResponseControl() && null != tcrmService.getResponseControl().getDWLControl()) {
            String clientSystemName = tcrmService.getResponseControl().getDWLControl()
                    .getClientSystemName();

            if (null == clientSystemName || !OCVConstants.CRM.equals(clientSystemName)) {
                apiResponse.setProfileIds(null);
                LogUtil.debug(log, "removeProfileIdForOtherResponses", logAttributes.getTraceId(),
                        "removed Profile Ids for other Source Systems");
            }
        }

    }
}