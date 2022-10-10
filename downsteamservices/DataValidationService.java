package com.anz.mdm.ocv.api.downsteamservices;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.common.v1.SourceSystem;
import com.anz.mdm.ocv.party.v1.Party;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import lombok.extern.slf4j.Slf4j;

/**
 * This class takes care of getting the data validated through Data validation
 * service. In case of failures, it logs appropriately
 * 
 * @author surendrn
 *
 */
@Slf4j
@Service
public class DataValidationService {

    private static final String COMMA = ",";

    // Kibana (or any other dashboard) should monitor for below keyword in logs and
    // raise an appropriate alert
    private static final String DATA_VALIDATION_FAILED = "PARTY_DATA_VALIDATION_FAILED";

    private static final String INVOCATION_ERROR = "Data validation library invocation failed";
    private static final String OTHER_PROCESSING_ERROR = "Data validation library returned a failed response";

    private static final String METHOD_NAME = "validateLibrary";

    @Value("${dataValidation.url}")
    private String uri;

    @Autowired
    @Qualifier("DataValidationRestTemplate")
    private RestTemplate restTemplate;
    
    @HystrixCommand(fallbackMethod = "fallbackValidatedParty")
    public ValidatedParty validateParty(final APIRequest<Party> apiRequest) throws Exception {
        LogUtil.debug(log, "validateParty", apiRequest.getTraceId(),
                "Invoking data validation service for validating mobile and email");
        Long startTime = System.currentTimeMillis();
        final HttpEntity<Party> request = new HttpEntity<>(apiRequest.getRequestBody(),
                prepareHeadersForBackend(apiRequest.getTraceId()));

        ResponseEntity<Party> response = null;
        try {
            response = restTemplate.exchange(uri, HttpMethod.POST, request, Party.class);
            LogUtil.debug(log, "Validation status", apiRequest.getTraceId(),
                    response.getStatusCode().getReasonPhrase());
        } catch (Exception e) {
            logError(INVOCATION_ERROR, "Data Validation failed. TraceId" + apiRequest.getTraceId(), e.getMessage(),
                    apiRequest, METHOD_NAME);
            throw e;
        }

        // Generally, the 4xx/5xx are handled in exception cases, still writing below
        // block to take only 200 as success
        if (response.getStatusCode() != HttpStatus.OK
                && response.getStatusCode() != HttpStatus.NON_AUTHORITATIVE_INFORMATION) {
            throw new Exception(String.format("Received %s code. Expected 200", response.getStatusCode()));
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(), "Data Validation successful", (endTime - startTime));
        return new ValidatedParty(response.getBody(), true);
    }

    private ValidatedParty fallbackValidatedParty(final APIRequest<Party> apiRequest, final Throwable throwable) {
        LogUtil.debug(log, "validateParty", apiRequest.getTraceId(),
                "Invoking fallbackValidatedParty of data validation");
        if (null != throwable) {
            logError(INVOCATION_ERROR, throwable.getMessage(), throwable.toString(), apiRequest, METHOD_NAME);
        } else {
            logError(OTHER_PROCESSING_ERROR, "Timeout or other error", null, apiRequest, METHOD_NAME);
        }
        return new ValidatedParty(apiRequest.getRequestBody(), false);
    }

    private void logError(final String errorString, final String reason, final String reasonCode,
            final APIRequest<Party> apiRequest, final String method) {

        final String requestTime = apiRequest.getRequestTimestamp();
        final List<SourceSystem> sourceSystems = apiRequest.getRequestBody().getSourceSystems();

        /*
         * Generally, there would be single Source System ID and Name. But as it is
         * defined as a List, we need to take care of this and take it as a comma
         * separated string
         */
        final String commaSeparatedSourceSystemIds = (sourceSystems == null) ? ""
                : sourceSystems.stream().map(s -> s.getSourceSystemId()).collect(Collectors.joining(COMMA));

        final String commaSeparatedSourceSystemNames = (sourceSystems == null) ? ""
                : sourceSystems.stream().map(s -> s.getSourceSystemName()).collect(Collectors.joining(COMMA));

        // Excluding traceId here as it gets logged in the wrapper LogUtil function
        final String logMessage = String.format(
                "%s: %s. Request Time: %s, Source System IDs: [ %s ], Source Names: [ %s ]", DATA_VALIDATION_FAILED,
                errorString, requestTime, commaSeparatedSourceSystemIds, commaSeparatedSourceSystemNames);

        LogUtil.error(log, method, apiRequest.getTraceId(),  logMessage, reasonCode, reason);

    }

    private HttpHeaders prepareHeadersForBackend(final String traceId) {
        LogUtil.debug(log, "prepareHeadersForBackend", traceId, "entering prepareHeadersForBackend");
        final HttpHeaders headers = new HttpHeaders();
        headers.set(OCVConstants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.TRACE_ID_HEADER, traceId);
        return headers;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

}
