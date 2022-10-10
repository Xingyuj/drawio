package com.anz.mdm.ocv.api.downsteamservices;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.MDMException;
import com.anz.mdm.ocv.api.util.LogRequestModel;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.ibm.mdm.schema.DWLError;
import com.ibm.mdm.schema.TCRMService;
import com.ibm.mdm.schema.TxResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableRetry
@Service
public class MDMRetryService {

    @Retryable(value = { MDMException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public HttpEntity<TCRMService> createOrUpdatePartyRetry(MaintainPartyService service, String requestMDMXml,
            String traceId, ValidationResult validationResult, LogRequestModel logAttributes)
            throws IOException, MDMException, Exception {
        
        LogUtil.debug(log, "createOrUpdatePartyRetry", traceId,
                "Entering: createOrUpdatePartyRetry method" + " in MDMRetryService");

        HttpEntity<TCRMService> xmlResponseRetry = service.invokeMdmBackend(requestMDMXml, traceId);
        String status = null;
        TCRMService responseBody = xmlResponseRetry.getBody();
        if (null != responseBody) {
            TxResult txResult = responseBody.getTxResponse().getTxResult();
            status = txResult.getResultCode();
            if (!("SUCCESS".equalsIgnoreCase(status) || "WARNING".equalsIgnoreCase(status))) {
                LogUtil.debug(log, "createOrUpdatePartyRetry", traceId, "Request has failed");
                DWLError error = txResult.getDWLError().get(0);
                String errorCode = error.getReasonCode();
                String errMessage = error.getErrorMessage();

                // Handle retry for Interaction-2 concurrency issue on CONNTEQUIV: END
                validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
                LogUtil.error(log, "createOrUpdatePartyRetry", traceId, "Response Validation failed",
                        errMessage, errorCode);
                LogUtil.logErrorMessage(errMessage, traceId, logAttributes.getChannel(), logAttributes.getPartyType());
                throw new MDMException(OCVConstants.MDM_EXCEPTION_ERROR_CODE, errMessage);
            } else {
                validationResult.setStatus(status);
            }
        }

        LogUtil.debug(log, "createOrUpdatePartyRetry", traceId,
                "Exiting: createOrUpdatePartyRetry method " + "in MDMRetryService");
        return xmlResponseRetry;

    }

    @Recover
    public boolean retryFromRecover(MDMException ex, MaintainPartyService service, String requestMDMXml, String traceId,
            ValidationResult validationResult, Logger log, LogRequestModel logAttributes)
            throws IOException, Exception {

        return false;
    }

    public HttpEntity<TCRMService> handleExceptions(MaintainPartyService service, String traceId,
            ValidationResult validationResult, String requestMDMXml, DWLError error, LogRequestModel logAttributes,
            MDMRetryService mdmRetryService) throws Exception {

        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "handleExceptions", traceId, "Entering: handleExceptions method");
        HttpEntity<TCRMService> xmlResponseReturn = null;
        String errorCode = error.getReasonCode();
        String errMessage = error.getErrorMessage();
        String errType = error.getErrorType();
        StringBuilder buildRequestMDMXml = new StringBuilder();

        // Handle retry for Interaction-2 concurrency issue on CONNTEQUIV:START
        if (OCVConstants.ERROR_TYPE_DUPLICATE.equalsIgnoreCase(errType)
                && OCVConstants.ERROR_REASON_CODE_DUPLICATE.equalsIgnoreCase(errorCode)
                && (error.getThrowable() != null
                && error.getThrowable().contains(OCVConstants.ERROR_CONTAINS_DUPLICATE_INDEX))
                && requestMDMXml.contains(OCVConstants.UPDATE_CUSTOMER_TAG)) {
            xmlResponseReturn = mdmRetryService.createOrUpdatePartyRetry(service, requestMDMXml, traceId,
                    validationResult, logAttributes);
        } else if (OCVConstants.ERROR_TYPE_DIERR.equalsIgnoreCase(errType)
                && OCVConstants.ERROR_REASON_CODE_DUPLICATE_PRIVPREF.equalsIgnoreCase(errorCode)
                && requestMDMXml.contains(OCVConstants.UPDATE_CUSTOMER_TAG)) {
            buildRequestMDMXml.append(requestMDMXml);
            buildRequestMDMXml.delete(requestMDMXml.indexOf("<TCRMPartyPrivPrefBObj>"),
                    requestMDMXml.lastIndexOf("</TCRMPartyPrivPrefBObj>") + "</TCRMPartyPrivPrefBObj>".length());
            xmlResponseReturn = mdmRetryService.createOrUpdatePartyRetry(service, buildRequestMDMXml.toString(),
                    traceId, validationResult, logAttributes);
        } else {
            // Handle retry for Interaction-2 concurrency issue on CONNTEQUIV: END
            validationResult.setErrorCode(OCVConstants.BACKEND_SERVICE_EXCEPTION_ERROR_CODE);
            LogUtil.error(log, "handleExceptions", traceId, "Response Validation failed",
                    errMessage, errorCode);
            LogUtil.logErrorMessage(errMessage, traceId, logAttributes.getChannel(), logAttributes.getPartyType());
            throw new MDMException(OCVConstants.MDM_EXCEPTION_ERROR_CODE, errMessage);
        }

        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "handleExceptions", traceId, "Exit: handleExceptions method",
                (endTime - startTime));
        return xmlResponseReturn;
    }
}
