package com.anz.mdm.ocv.api.downsteamservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import com.anz.mdm.ocv.api.util.APIServiceUtil;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.common.v1.Address;
import com.anz.mdm.ocv.common.v1.SourceSystem;
import com.anz.mdm.ocv.party.v1.Party;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import lombok.extern.slf4j.Slf4j;

/**
 * This class takes care of getting the data standardised through Data
 * Standardisation service. In case of failures, it logs appropriately
 *
 * @author Ravi Batra
 */
@Slf4j
@Service
public class DataStandardisationService {

    private static final String COMMA = ",";

    // Kibana (or any other dashboard) should monitor for below keyword in logs
    // and
    // raise an appropriate alert
    private static final String DATA_STANDARDISATION_FAILED = "PARTY_DATA_STANDARDISATION_FAILED";

    private static final String INVOCATION_ERROR = "Data Standardisation library invocation failed";
    private static final String OTHER_PROCESSING_ERROR = "Data Standardisation library returned a failed response";

    private static final String METHOD_NAME = "standardiseParty";

    @Autowired
    private RedhatRuleEngineService redhatRuleEngineService;

    @Value("${dataStandardisation.url}")
    private String uri;

    @Autowired
    @Qualifier("DataStandardisationRestTemplate")
    private RestTemplate restTemplate;

    @Value("${anzx.address.channels}")
    private String anzxAddressChannel;

    @HystrixCommand(fallbackMethod = "fallbackStandardiseParty")
    public StandardisedParty standardiseParty(final APIRequest<Party> apiRequest) throws Exception {
        Long startTime = System.currentTimeMillis();
        final HttpEntity<Party> request = new HttpEntity<>(apiRequest.getRequestBody(),
                prepareHeadersForBackend(apiRequest.getTraceId()));

        ResponseEntity<Party> response = null;
        try {
            // OCT-21623:Region fix for ANZx
            transformRegionForAnzx(apiRequest);
            response = restTemplate.exchange(uri, HttpMethod.POST, request, Party.class);
        } catch (Exception e) {
            logError(INVOCATION_ERROR, "Data Standardization failed. TraceId" + apiRequest.getTraceId(), e.getMessage(),
                    apiRequest, METHOD_NAME);
            throw e;
        }

        // Generally, the 4xx/5xx are handled in exception cases, still writing
        // below
        // block to take only 200 as success
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception(String.format("Received %s code. Expected 200", response.getStatusCode()));
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(), "Data Standardisation successful",
                (endTime - startTime));
        return new StandardisedParty(response.getBody(), true);
    }


    //Added for https://jira.service.anz/browse/OCT-25769
    public Object invokeRedHatRuleEngine(final APIRequest<Party> apiRequest) throws Exception {
        return redhatRuleEngineService.invokeRedHatRuleEngine(apiRequest);
    }


    /**
     * Added for https://jira.service.anz/browse/OCT-25769
     * This method makes call to RHDM if phone exists.
     */
    public ValidatedParty standardisePhoneWithRHDM(ValidatedParty validatedPartyParam, Map<String, String> headers,
                                                    Map<String, String> queryParameters) throws Exception {
        Long startTime = System.currentTimeMillis();
        ValidatedParty validatedPartyResponse = null;
        APIRequest<Party> apiRequestParty = new APIRequest<Party>(headers, queryParameters,
                validatedPartyParam.getParty());
        LogUtil.debug(log, "Entered standardisePhoneWithRHDM", apiRequestParty.getTraceId());
        if (new APIServiceUtil().isPhoneExists(validatedPartyParam)) {
            LogUtil.debug(log, "maintainParty Api", apiRequestParty.getTraceId(), "Phone Exits in request",
                    System.currentTimeMillis() - startTime);
            Object rhdmResponse = this.invokeRedHatRuleEngine(apiRequestParty);
            if (rhdmResponse instanceof StandardisedParty) {
                validatedPartyResponse = new ValidatedParty(((StandardisedParty) rhdmResponse).getParty(),
                        validatedPartyParam.isPartyvalidated());
            }
            LogUtil.debug(log, "maintainParty Api", apiRequestParty.getTraceId(),
                    "Returned from RedHatRuleEngineService", System.currentTimeMillis() - startTime);
        } else {
            LogUtil.debug(log, "maintainParty Api", apiRequestParty.getTraceId(),
                    "Phone does not Exits in request, returning without Standardisation from "
                            + "standardisePhoneWithRHDM...",
                    System.currentTimeMillis() - startTime);
            validatedPartyResponse = validatedPartyParam;
        }

        return validatedPartyResponse;
    }

    /**
     * This method is will populate raw state value to region. This is for all
     * ANZX Channels
     */
    private void transformRegionForAnzx(final APIRequest<Party> apiRequest) {
        Long startTime = System.currentTimeMillis();
        String traceId = apiRequest.getTraceId();
        LogUtil.debug(log, "transformRegionForAnzx", traceId,
                "Entering: transformRegionForAnzx method in StandardisedParty");
        String channel = apiRequest.getChannel();
        List<Address> addressList = apiRequest.getRequestBody().getAddresses();
        for (Address address : addressList) {
            String region = address.getRegion();
            if (StringUtils.isNotBlank(anzxAddressChannel)) {
                List<String> channelsList = new ArrayList<String>(Arrays.asList(anzxAddressChannel.split(",")));
                if (channelsList.contains(channel) && StringUtils.isBlank(region)) {
                    LogUtil.debug(log, "transformRegionForAnzx", traceId,
                            "Channel from header is an " + "ANZx channel.Replace region with state value");
                    region = address.getState();
                    address.setRegion(region);
                    LogUtil.debug(log, "transformRegionForAnzx", "Replaced region value is ", region);
                }
            }
        }
        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, "transformRegionForAnzx", traceId,
                "Exit: transformRegionForAnzx method in StandardisedParty", (endTime - startTime));
    }

    private StandardisedParty fallbackStandardiseParty(final APIRequest<Party> apiRequest, final Throwable throwable) {

        if (null != throwable) {
            logError(INVOCATION_ERROR, throwable.getMessage(), throwable.toString(), apiRequest, METHOD_NAME);
        } else {
            logError(OTHER_PROCESSING_ERROR, "Timeout or other error", null, apiRequest, METHOD_NAME);
        }
        return new StandardisedParty(apiRequest.getRequestBody(), false);
    }

    private void logError(final String errorString, final String reason, final String reasonCode,
                          final APIRequest<Party> apiRequest, final String method) {

        final String requestTime = apiRequest.getRequestTimestamp();
        final List<SourceSystem> sourceSystems = apiRequest.getRequestBody().getSourceSystems();

        /*
         * Generally, there would be single Source System ID and Name. But as it
         * is defined as a List, we need to take care of this and take it as a
         * comma separated string
         */
        final String commaSeparatedSourceSystemIds = (sourceSystems == null) ? ""
                : sourceSystems.stream().map(s -> s.getSourceSystemId()).collect(Collectors.joining(COMMA));

        final String commaSeparatedSourceSystemNames = (sourceSystems == null) ? ""
                : sourceSystems.stream().map(s -> s.getSourceSystemName()).collect(Collectors.joining(COMMA));

        // Excluding traceId here as it gets logged in the wrapper LogUtil
        // function
        final String logMessage = String.format(
                "%s: %s. Request Time: %s, Source System IDs: [ %s ], Source Names: [ %s ]",
                DATA_STANDARDISATION_FAILED, errorString, requestTime, commaSeparatedSourceSystemIds,
                commaSeparatedSourceSystemNames);
        LogUtil.error(log, method, apiRequest.getTraceId(), logMessage, reason, reasonCode);
        LogUtil.error(log, method, apiRequest.getTraceId(), reason, reasonCode, apiRequest.getChannel(),
                apiRequest.getRequestBody().getPartyType(), OCVConstants.MAINTAINPARTY_SERVICE);

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