/**
 * 
 */
package com.anz.mdm.ocv.api.downsteamservices;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.MDMRequestTemplates;
import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.converter.OrganizationObjectConverter;
import com.anz.mdm.ocv.api.converter.PersonObjectConverter;
import com.anz.mdm.ocv.api.dto.APIResponse;
import com.anz.mdm.ocv.api.exception.MDMException;
import com.anz.mdm.ocv.api.processor.DeletePartyServiceProcessor;
import com.anz.mdm.ocv.api.transform.Context;
import com.anz.mdm.ocv.api.transform.XMLParser;
import com.anz.mdm.ocv.api.util.ExceptionUtil;
import com.anz.mdm.ocv.api.util.LogRequestModel;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.util.RequestTransfomerUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.anz.mdm.ocv.party.v1.Party;
import com.ibm.mdm.schema.DWLError;
import com.ibm.mdm.schema.TCRMDeletedPartyBObjType;
import com.ibm.mdm.schema.TCRMDeletedPartyWithHistoryBObjType;
import com.ibm.mdm.schema.TCRMOrganizationBObjType;
import com.ibm.mdm.schema.TCRMPersonBObjType;
import com.ibm.mdm.schema.TCRMService;
import com.ibm.mdm.schema.TxResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeletePartyService extends AbstractMdmservice {

    @Value("${mdmEndpoint.url}")
    private String url;

    @Autowired
    @Qualifier("DeletePartyServiceMDMRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private LogRequestModel logAttributes;

    @Autowired
    private PersonObjectConverter personObjectConverter;

    @Autowired
    private OrganizationObjectConverter organizationObjectConverter;

    @Override
    public ValidationResult validateResponse(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponse", traceId, "Entering: validateResponse method in PurgePartyService");
        ValidationResult validationResult = new ValidationResult();
        String status = checkResponseStatus(xmlResponse, traceId);
        if (!("SUCCESS".equalsIgnoreCase(status))) {
            LogUtil.debug(log, "validateResponse", traceId, "MDM Response has failed");
            validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
        } else {
            LogUtil.debug(log, "validateResponse", traceId, "MDM Response has Success");
            validationResult.setStatus(status);
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponse", traceId, "Exit: validateResponse method in PurgePartyService",
                (endTime - startTime));
        return validationResult;
    }

    @Override
    public ValidationResult validateMdmResponse(HttpEntity<TCRMService> xmlResponse, String traceId,
            ValidationResult validationResult, String xml) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateMdmResponse", traceId, "Entering: validateResponse method in MaintainPartyService");
        validateResponseBody(xmlResponse, traceId, validationResult);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateMdmResponse", traceId, "Exit: validateResponse method in MaintainPartyService",
                (endTime - startTime));
        return validationResult;
    }

    private String validateResponseBody(HttpEntity<TCRMService> xmlResponse, String traceId,
            ValidationResult validationResult) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponseBody", traceId,
                "Entering: validateResponseBody method in MaintainPartyService");
        String status = null;
        TCRMService responseBody = xmlResponse.getBody();
        if (null != responseBody) {
            TxResult txResult = responseBody.getTxResponse().getTxResult();
            status = txResult.getResultCode();
            if (!("SUCCESS".equalsIgnoreCase(status) || "WARNING".equalsIgnoreCase(status))) {
                LogUtil.debug(log, "validateResponseBody", traceId, "Request has failed");
                DWLError error = txResult.getDWLError().get(0);
                String errorCode = error.getReasonCode();
                String errMessage = error.getErrorMessage();
                validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
                LogUtil.error(log, "maintainParty - validateResponseBody", traceId, "Response Validation failed",
                        errMessage, errorCode);
                LogUtil.logErrorMessage(errMessage, traceId, logAttributes.getChannel(), logAttributes.getPartyType());
                throw new MDMException(OCVConstants.MDM_EXCEPTION_ERROR_CODE, errMessage);
            } else {
                validationResult.setStatus(status);
            }
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "validateResponseBody", traceId, "Exit: validateResponseBody method in MaintainPartyService",
                (endTime - startTime));
        return status;
    }

    public String transformDeletePartyRequest(APIRequest<Party> apiRequest) throws Exception {
        LogUtil.debug(log, "transformPartyRequest", apiRequest.getTraceId(),
                "Entering: transformPartyRequest method in PurgePartyService");
        String requestxml = null;
        requestxml = createDeletePartyXML(apiRequest);
        LogUtil.debug(log, "transformDeletePartyRequest", apiRequest.getTraceId(),
                "Exiting: transformDeletePartyRequest method in DeletePartyService");
        return requestxml;
    }

    public HttpEntity<String> invokeBackend(String requestMDMXml, String traceId) {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Entering: invokeBackend method in PurgePartyService");
        HttpHeaders headers = prepareHeadersForBackened(traceId);
        HttpEntity<String> request = new HttpEntity<String>(requestMDMXml, headers);
        HttpEntity<String> xmlResponse = invokeDownStreamService(request, url, restTemplate, HttpMethod.DELETE,
                traceId);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Exit: invokeBackend method in PurgePartyService",
                (endTime - startTime));
        return xmlResponse;
    }

    public HttpEntity<TCRMService> invokeMdmBackend(String requestMDMXml, String traceId) {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Entering: invokeBackend method in MaintainPartyService");
        HttpHeaders headers = prepareHeadersForBackened(traceId);
        HttpEntity<String> request = new HttpEntity<String>(requestMDMXml, headers);
        HttpEntity<TCRMService> xmlResponse = invokeMdmDownStreamService(request, url, restTemplate, HttpMethod.PUT,
                traceId);
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "invokeBackend", traceId, "Exit: invokeBackend method in MaintainPartyService",
                (endTime - startTime));
        return xmlResponse;
    }

    public String processDeleteParty(APIRequest<Party> apiRequest, String requestTime, String traceId)
            throws IOException, Exception {
        LogUtil.debug(log, "processDeleteParty", traceId, "Entering: processDeleteParty method n DeletePartyService");
        String requestMDMXml = transformDeletePartyRequest(apiRequest);
        String response = null;
        HttpEntity<String> xmlResponse = invokeBackend(requestMDMXml, traceId);
        LogUtil.debug(log, "processDeleteParty", traceId, "xmlResponse--->" + xmlResponse);
        ValidationResult validationResult = validateResponse(xmlResponse, traceId);
        if (!validationResult.isValid()) {
            LogUtil.error(log, "processParty", traceId, "MDM Response Validation failed",
                    validationResult.getErrorCode(), logAttributes.getChannel(), logAttributes.getPartyType(),
                    OCVConstants.DELETEPARTY_SERVICE);
            String errorMessage = null;
            if (xmlResponse != null) {
                errorMessage = ExceptionUtil.getErrorMessage(xmlResponse.getBody());
            }

            // Added a error message NULL check as certian PurgePatry MDM
            // Response
            // are returned with no <TxResult> tags, Hence Error message is
            // unable to be extracted
            // failing with NULL pointer Exception.

            if (null != errorMessage) {
                LogUtil.logErrorMessage(errorMessage, traceId, logAttributes.getChannel(),
                        logAttributes.getPartyType());
                LogUtil.logErrorMessage(xmlResponse.getBody(), traceId, logAttributes.getChannel(),
                        logAttributes.getPartyType());
                throw new MDMException(validationResult.getErrorCode(), errorMessage);
            } else {
                errorMessage = "PurgeParty Failed. MDM Response returned with no <ErrorMessage> Tag.";
                LogUtil.logErrorMessage(errorMessage, traceId, logAttributes.getChannel(),
                        logAttributes.getPartyType());
                LogUtil.logErrorMessage(xmlResponse.getBody(), traceId, logAttributes.getChannel(),
                        logAttributes.getPartyType());
                throw new MDMException(validationResult.getErrorCode(), errorMessage);
            }

        } else {
            response = transformMDMResponse(xmlResponse, traceId);
            LogUtil.debug(log, "processDeleteParty", traceId, "response--->" + response);
        }
        LogUtil.debug(log, "processParty", traceId, "Exiting: processPurgeParty method in DeletePartyService");
        return response;
    }

    public APIResponse deleteParty(APIRequest<Party> apiRequest, String requestTime, String traceId)
            throws IOException, Exception {
        LogUtil.debug(log, "deleteParty", traceId, "Entering: deleteParty method n DeletePartyService");
        String requestMDMXml = transformDeletePartyRequest(apiRequest);
        APIResponse apiResponse = null;
        HttpEntity<TCRMService> xmlResponse = invokeMdmBackend(requestMDMXml, traceId);
        LogUtil.debug(log, "deleteParty", traceId, "xmlResponse--->" + xmlResponse);
        ValidationResult validationResult = new ValidationResult();
        TCRMService tcrmService = xmlResponse.getBody();
        if (ObjectUtils.isNotEmpty(tcrmService)) {
            apiResponse = transformMdmResponse(traceId, tcrmService, validationResult);
        }
        LogUtil.debug(log, "deleteParty", traceId, "Exiting: deleteParty method in DeletePartyService");
        return apiResponse;
    }

    private APIResponse transformMdmResponse(String traceId, TCRMService tcrmService, ValidationResult validationResult)
            throws Exception {
        APIResponse apiResponse = null;
        Object object = tcrmService.getTxResponse().getResponseObject().getCommonBObj().get(0).getValue();
        if (object instanceof TCRMDeletedPartyWithHistoryBObjType) {
            TCRMDeletedPartyWithHistoryBObjType tcrmDeletedPartyWithHistoryBObj = 
                    (TCRMDeletedPartyWithHistoryBObjType) object;
            TCRMDeletedPartyBObjType tcrmDeletedPartyBObj = tcrmDeletedPartyWithHistoryBObj.getTCRMDeletedPartyBObj();
            TCRMPersonBObjType tcrmPersonObject = tcrmDeletedPartyBObj.getTCRMPersonBObj();
            TCRMOrganizationBObjType tcrmOrgObject = tcrmDeletedPartyBObj.getTCRMOrganizationBObj();
            if (ObjectUtils.isNotEmpty(tcrmPersonObject)) {
                apiResponse = personObjectConverter.convertPersonDetails(tcrmPersonObject, traceId);
            } else if (ObjectUtils.isNotEmpty(tcrmOrgObject)) {
                apiResponse = organizationObjectConverter.convertOrgDetails(tcrmOrgObject, traceId);
            }
        }
        return apiResponse;
    }

    public String transformMDMResponse(HttpEntity<String> xmlResponse, String traceId) {
        LogUtil.debug(log, "transformMDMResponse", traceId,
                "Entering: transformMDMResponse method in deletePartyService");
        String jsonResponse = transformXmlResponseToJson(xmlResponse, Context.DELETEPARTYAPI, traceId);
        LogUtil.debug(log, "transformMDMResponse", traceId, "Exit: transformMDMResponse method in DeletePartyService");
        return jsonResponse;
    }

    public String checkResponseStatus(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "checkResponseStatus", traceId, "Entering: checkResponseStatus method in PurgePartyService");
        String status = null;
        XMLParser xmlParser = null;
        if (null != xmlResponse) {
            String response = xmlResponse.getBody();
            /*
             * if (null != response) { xmlParser = new XMLParser(response); }
             * String statusExpression =
             * "/TCRMService/TxResponse/TxResult/ResultCode"; status =
             * retriveValueFromXpath(xmlParser, statusExpression, traceId);
             */
            if (null != response && response.contains(MDMRequestTemplates.SUCCESS_RESULT_CODE)
                    && !response.contains(MDMRequestTemplates.WARNING_WRAPPER_BOBJ)) {
                status = "SUCCESS";
            } else {
                status = "FATAL";
            }
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "checkResponseStatus", traceId, "Exit: checkResponseStatus method in DeletePartyService",
                (endTime - startTime));
        return status;
    }

    public String createDeletePartyXML(APIRequest<Party> apiRequest) throws IOException {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "createdeletePartyRequestXML", apiRequest.getTraceId(),
                "Entering: createdeletePartyRequestXML method in DeletePartyService");
        String xmlFilePath = MDMRequestTemplates.MDM_DELETE_PARTY_REQUEST_XML;
        String requestXml = RequestTransfomerUtil.fetchTemplate(xmlFilePath, apiRequest.getTraceId());
        LogUtil.debug(log, "createDeletePartyXML", apiRequest.getTraceId(), "requestXml" + requestXml);

        StringBuilder request = new StringBuilder();
        try {
            request.append(requestXml);
            if (null != apiRequest.getRequestBody()) {
                DeletePartyServiceProcessor deleteServiceProcessor = new DeletePartyServiceProcessor();
                request = deleteServiceProcessor.prepareDeletePartyRequest(request, apiRequest);

            }
        } catch (Exception ex) {
            throw ex;
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "createMDMPersonRequestXML", apiRequest.getTraceId(),
                "Exit: createMDMPersonRequestXML method in MaintainPartyService", (endTime - startTime));
        return request.toString();
    }

    public void setUrl(String url) {
        this.url = url;
    }
}