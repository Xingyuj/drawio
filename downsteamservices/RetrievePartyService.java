package com.anz.mdm.ocv.api.downsteamservices;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.BackendServiceException;
import com.anz.mdm.ocv.api.exception.BadRequestException;
import com.anz.mdm.ocv.api.exception.DuplicateRecordException;
import com.anz.mdm.ocv.api.exception.ErrorResponseDto;
import com.anz.mdm.ocv.api.exception.RecordNotFoundException;
import com.anz.mdm.ocv.api.exception.RestTemplateResponseErrorHandler;
import com.anz.mdm.ocv.api.exception.ServiceUnavailableException;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.party.v1.Party;
import com.anz.mdm.ocv.party.v1.SearchPartyResultWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/*
 * This class will act like service class for retrieve party api
 */

@Service
@Slf4j
public class RetrievePartyService extends AbstractFuzzyService {

    @Autowired
    @Qualifier("ApiRestTemplate")
    private RestTemplate restemplate;

    @Value("${retrieve-api.url}")
    private String url;

    @Value("${retrieve-api.good-status}")
    private String goodStatuses;

    /*
     * This method will act like fascade for the methods inside service
     *
     * @param request
     *
     * @param traceId
     *
     * @return ResponseEntity<Object>
     *
     */
    public SearchPartyResultWrapper getResponse(APIRequest<Party> request, String traceId) throws IOException {

        // LogUtil.debug(log, "getResponse", request.getTraceId(), "Entering
        // retrieve
        // api get response");
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "getResponse", traceId, "Entering getResponse in Retrieve Party API: ", startTime);

        ResponseEntity<Object> apiResponse = invokeDownStreamService(request, traceId);
        LogUtil.debug(log, "getResponse", traceId, "Exiting getResponse in Retrieve Party API: ",
                System.currentTimeMillis() - startTime);

        return buildResponse(request, apiResponse);

    }

    public ResponseEntity<Object> invokeDownStreamService(APIRequest<Party> request, String traceId) {
        ResponseEntity<Object> apiResponse = null;
        LogUtil.debug(log, "invokeDownStreamService", request.getTraceId(),
                "Entering retrieve api invokeDownStreamService");
        addCustomErrorHandler();
        if (!isEmpty(request)) {
            HttpHeaders headers = prepareHeadersForApi(request, traceId);
            HttpEntity<Party> apiRequest = new HttpEntity<Party>(request.getRequestBody(), headers);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam(OCVConstants.ROLES_REQUIRED, "true");
            if (OCVConstants.FENERGOANZX_CHANNEL.equalsIgnoreCase(request.getChannel())
                    && OCVConstants.REQUEST_MODE_UPDATEKYC.equalsIgnoreCase(request.getRequestMode())) {
                uriBuilder.queryParam(OCVConstants.INCLUDE_ACCOUNTS, "true")
                        .queryParam(OCVConstants.ACCOUNT_PROD_TP, OCVConstants.PCB_ACCOUNT)
                        .queryParam(OCVConstants.UNDER_REVIEW, OCVConstants.ANZX_ONBOARDING);
            }
            apiResponse = restemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, apiRequest, Object.class);
        }
        return apiResponse;

    }

    private SearchPartyResultWrapper buildResponse(APIRequest<Party> request, ResponseEntity<Object> apiResponse) {
        LogUtil.debug(log, "buildResponse", request.getTraceId(), "Entering retrieve apibuildResponse");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("x-b3-traceid", request.getTraceId());
        responseHeaders.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains;");
        SearchPartyResultWrapper wrapper = new SearchPartyResultWrapper();
        ArrayList<Party> emptyParties = new ArrayList<Party>();
        if (!isEmpty(apiResponse) && !isEmpty(request) && !isEmpty(request.getRequestBody())) {
            handleResponse(request, apiResponse, wrapper, emptyParties);
        }
        return wrapper;
    }

    private void logHandleResponse(APIRequest<Party> request, ResponseEntity<Object> response) {
        LogUtil.debug(log, "handleResponse, request: ", LogUtil.printObjectAsString(request));
        LogUtil.debug(log, "handleResponse, response: ", LogUtil.printObjectAsString(response));
    }

    private void handleResponse(APIRequest<Party> request, ResponseEntity<Object> apiResponse,
                                SearchPartyResultWrapper wrapper, ArrayList<Party> emptyParties)
            throws IllegalArgumentException, RecordNotFoundException, BadRequestException, ServiceUnavailableException {
        int httpsStatus = apiResponse.getStatusCode().value();
        ObjectMapper mapper = new ObjectMapper();
        logHandleResponse(request, apiResponse);
        if (httpsStatus == 200) {
            wrapper.getCertifiedResults().addAll(filterResponse(request, mapper.convertValue(apiResponse.getBody(),
                    new PartyReference())));
            wrapper.getProbableResults().addAll(emptyParties);
        } else {
            handleErrorResponse(request, apiResponse, wrapper, emptyParties, httpsStatus, mapper);
        }
    }

    private void handleErrorResponse(APIRequest<Party> request, ResponseEntity<Object> apiResponse,
                                     SearchPartyResultWrapper wrapper, ArrayList<Party> emptyParties, int httpsStatus,
                                     ObjectMapper mapper) {
        ErrorResponseDto response = mapper.convertValue(apiResponse.getBody(), new ErrorReference());
        String statusCode = response.getErrors().get(0).getStatusCode();
        String statusMessage = response.getErrors().get(0).getMoreInformation();
        String partyType = request.getRequestBody().getPartyType();
        switch (httpsStatus) {
            case 404:
                wrapper.getCertifiedResults().addAll(emptyParties);
                wrapper.getProbableResults().addAll(emptyParties);
                if (StringUtils.isNoneBlank(statusCode) && OCVConstants.NO_CAP_CIS_IDS_IDENTIFIED.equals(statusCode)) {
                    LogUtil.info(log, "maintainParty", request.getTraceId(),
                            "No CAP-CIS id Associated for the OCV ID requested", 0, request.getChannel(),
                            request.getRequestBody().getPartyType(), OCVConstants.MAINTAINPARTY_SERVICE);
                    return;
                } else {
                    LogUtil.error(log, "buildResponse ", request.getTraceId(), statusMessage, statusCode,
                            request.getChannel(), partyType, OCVConstants.RETRIEVEPARTY_SERVICE);
                    throw new RecordNotFoundException(statusCode, statusMessage);
                }
            case 400:
                LogUtil.error(log, "buildResponse ", request.getTraceId(), statusMessage, statusCode,
                        request.getChannel(), partyType, OCVConstants.RETRIEVEPARTY_SERVICE);
                throw new BadRequestException(statusCode, statusMessage);
            case 500:
                statusCode = "5002";
                if (!isEmpty(response.getMessage())) {
                    throw new ServiceUnavailableException(statusCode);
                }
                if (!isEmpty(response.getErrors())) {
                    statusCode = response.getErrors().get(0).getStatusCode();
                }
                LogUtil.error(log, "buildResponse ", request.getTraceId(), "Internal server Error from retrieve",
                        statusCode, request.getChannel(), partyType, OCVConstants.RETRIEVEPARTY_SERVICE);
                throw new ServiceUnavailableException(statusCode);
            case 409:
                handleConflictDuplicateException(apiResponse, mapper, request, wrapper, emptyParties);
                break;
            default:
                throw new ServiceUnavailableException("5002");
        }
    }

    private void handleConflictDuplicateException(ResponseEntity<Object> apiResponse, ObjectMapper mapper,
                                                  APIRequest<Party> request, SearchPartyResultWrapper wrapper,
                                                  ArrayList<Party> emptyParties) {
        ErrorResponseDto response = mapper.convertValue(apiResponse.getBody(), new ErrorReference());
        String statusCode = response.getErrors().get(0).getStatusCode();
        String statusMessage = response.getErrors().get(0).getMoreInformation();

        /*
         * Split review OCV ID CAP Profiles should not be considered for On-boarding,
         * instead new CAP Profile should be created
         */
        if (StringUtils.isNoneBlank(statusCode) && OCVConstants.OCV_ID_UNDER_REVEIEW.equals(statusCode)) {
            wrapper.getCertifiedResults().addAll(emptyParties);
            wrapper.getProbableResults().addAll(emptyParties);
            LogUtil.info(log, "maintainParty", request.getTraceId(), "OCV ID Under review", 0, request.getChannel(),
                    request.getRequestBody().getPartyType(), OCVConstants.MAINTAINPARTY_SERVICE);
            return;
        } else if (StringUtils.isNoneBlank(statusCode) && OCVConstants.DUPLICATE_RECORD_FOUND_ERROR.equals(statusCode)
                && StringUtils.isNoneBlank(statusMessage) && OCVConstants.KYC_COMPLETED_MESSAGE.equals(statusMessage)) {
            /*
             * Idempotency exception handling implemented - Existing ANZx Customer
             */
            throw new DuplicateRecordException(OCVConstants.DUPLICATE_ANZX_CUSTOMER_FOUND_ERROR,
                    OCVConstants.KYC_COMPLETED_ANZX_CUSTOMER_EXISTS);
        } else if (StringUtils.isNoneBlank(statusCode)) {
            throw new BackendServiceException("5003");
        }
        LogUtil.error(log, "buildResponse ", request.getTraceId(), statusMessage, statusCode, request.getChannel(),
                request.getRequestBody().getPartyType(), OCVConstants.RETRIEVEPARTY_SERVICE);
        throw new DuplicateRecordException(statusCode, statusMessage);
    }

    public HttpHeaders prepareHeadersForApi(APIRequest<Party> request, String traceId) {
        LogUtil.debug(log, "prepareHeadersForApi", traceId, "entering prepareHeadersForBackend");
        HttpHeaders headers = new HttpHeaders();
        headers.set(OCVConstants.ACCEPT_HEADER, request.getAcceptHeader());
        headers.set(OCVConstants.CONTENT_TYPE, request.getAcceptHeader());
        headers.set(OCVConstants.REQUEST_MODE_HEADER, request.getRequestMode());
        headers.set(OCVConstants.TRACE_ID_HEADER, request.getTraceId());
        headers.set(OCVConstants.AUTHORIZATION_HEADER, request.getJwtToken());
        headers.set(OCVConstants.CHANNEL, request.getChannel());
        headers.set(OCVConstants.USER_ID_HEADER, request.getUserId());
        headers.set(OCVConstants.INVOKING_APPLICATION_HEADER, OCVConstants.OCV);

        return headers;
    }

    private RestTemplate addCustomErrorHandler() {
        restemplate.setErrorHandler(new RestTemplateResponseErrorHandler(goodStatuses));
        return restemplate;
    }

    static class PartyReference extends TypeReference<List<Party>> {

    }

    static class ErrorReference extends TypeReference<ErrorResponseDto> {

    }
}
