package com.anz.mdm.ocv.api.downsteamservices;

import static com.anz.mdm.ocv.api.util.LogUtil.printObjectAsString;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
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

import com.anz.mdm.ocv.api.constants.CAPConstants;
import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.dto.CapErrorResponseDTO;
import com.anz.mdm.ocv.api.dto.CapResponseReferenceDTO;
import com.anz.mdm.ocv.api.dto.CapResponseReferenceDTOV2;
import com.anz.mdm.ocv.api.dto.ErrorReferenceDTO;
import com.anz.mdm.ocv.api.exception.AvailabilityServiceDownException;
import com.anz.mdm.ocv.api.exception.JwtValidationException;
import com.anz.mdm.ocv.api.exception.RestTemplateCapResponseErrorHandler;
import com.anz.mdm.ocv.api.model.CAPParty;
import com.anz.mdm.ocv.api.transform.CapTransformer;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.util.StreetSuffixConfig;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.cap.v1.CapProfile;
import com.anz.mdm.ocv.cap.v1.CapResponse;
import com.anz.mdm.ocv.common.v1.Identifier;
import com.anz.mdm.ocv.common.v1.SourceSystem;
import com.anz.mdm.ocv.party.v1.Party;
import com.anz.mdm.ocv.party.v1.SearchPartyResultWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is to transform the Party API request to CAP Profile API Request
 * to create Customer in CAP
 * 
 */
@Slf4j
@Service
public class CapTransformationService {

    public static final String OCV_DATETIME_FORMATTER = "yyyy-MM-dd HH:mm:ss.SS";
    public static final String CAP_DATETIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String INVOCATION_ERROR = "CAP Profile API invocation failed";
    private static final String OTHER_PROCESSING_ERROR = "CAP Profile API returned a failed response";

    private static final String METHOD_NAME = "CreateCAPProfile";

    @Value("${capProfileAPI.url}")
    private String uri;

    @Value("${capProfileAPI.ocvclientid}")
    private String clientId;

    @Autowired
    @Qualifier("CapProfileRestTemplate")
    private RestTemplate restTemplate;
    
    @Autowired
    private StreetSuffixConfig streetSuffixCfg;

    @Autowired
    private CapTransformer capTransformer;

    public CAPParty createCapProfile(final APIRequest<Party> apiRequest, final APIRequest<Party> validatedRequest,
            String traceId) throws Exception {
        CapProfile capProfile = capTransformer.transformAPIRequest(apiRequest, streetSuffixCfg);
        return createProfileInCap(capProfile, apiRequest, validatedRequest);
    }

    public CAPParty createCapProfileV2(final APIRequest<Party> apiRequest, final APIRequest<Party> validatedRequest,
            String traceId) throws Exception {
        com.anz.mdm.ocv.cap.v2.CapProfile capProfile = capTransformer.fromApiRequest(apiRequest, streetSuffixCfg);

        LogUtil.debug(log, "Generated com.anz.mdm.ocv.cap.v2.CapProfile from apiRequest: capProfile",
                printObjectAsString(capProfile));
        return createProfileInCapV2(capProfile, apiRequest, validatedRequest);
    }

    @HystrixCommand(fallbackMethod = "fallbackCreateCAPProfile")
    private CAPParty createProfileInCap(final CapProfile capProfileRequest, APIRequest<Party> apiRequest,
            final APIRequest<Party> validatedRequest) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "CapProfile is -", printObjectAsString(capProfileRequest),
                OCVConstants.CAPTRANSFORMATION_SERVICE);
        final HttpEntity<CapProfile> request = new HttpEntity<>(capProfileRequest,
                prepareHeadersForBackend(validatedRequest));
        ResponseEntity<Object> response = null;
        addCustomErrorHandler();
        try {
            response = restTemplate.exchange(uri, HttpMethod.POST, request, Object.class);
        } catch (Exception e) {
            LogUtil.error(log, "Cap Profile Create failed. TraceId" + validatedRequest.getTraceId(), e.getMessage(),
                    capProfileRequest.toString());
            throw e;
        }
        // Generally, the 4xx/5xx are handled in exception cases, still writing
        // below
        // block to take only 200 as success
        if (response.getStatusCode() != HttpStatus.OK) {

            handleCapException(response, validatedRequest);
            throw new Exception(String.format("Received %s code. Expected 200 from CAP", response.getStatusCode()));
        }

        CapResponse capResponse = new ObjectMapper().convertValue(response.getBody(), new CapResponseReferenceDTO());
        Long endTime = System.currentTimeMillis();
        LogUtil.info(log, "createCapProfile", validatedRequest.getTraceId(), "Cap Profile Create Request successful",
                (endTime - startTime), validatedRequest.getChannel(), validatedRequest.getRequestBody().getPartyType(),
                OCVConstants.MAINTAINPARTY_SERVICE);
        SourceSystem srcSys = new SourceSystem();
        if (null != capResponse) {
            srcSys.setSourceSystemId(capResponse.getCustomerID());
        }
        srcSys.setSourceSystemName(OCVConstants.CAP_CIS_SOURCE);
        validatedRequest.getRequestBody().getSourceSystems().clear();
        validatedRequest.getRequestBody().getSourceSystems().add(srcSys);
        return new CAPParty(validatedRequest, true);
    }

    @HystrixCommand(fallbackMethod = "fallbackCreateCAPProfile")
    public CAPParty createProfileInCapV2(final com.anz.mdm.ocv.cap.v2.CapProfile capProfileRequest,
            APIRequest<Party> apiRequest, final APIRequest<Party> validatedRequest) throws Exception {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "CapProfile is -", printObjectAsString(capProfileRequest),
                OCVConstants.CAPTRANSFORMATION_SERVICE);
        final HttpEntity<com.anz.mdm.ocv.cap.v2.CapProfile> request = new HttpEntity<>(capProfileRequest,
                prepareHeadersForCapV2Backend(validatedRequest));
        ResponseEntity<Object> response = null;
        addCustomErrorHandler();
        LogUtil.debug(log, "Requesting Cap Customer API - URI: ", uri);
        LogUtil.debug(log, "Requesting Cap Customer API - Request: ", printObjectAsString(request));
        try {
            response = restTemplate.exchange(uri, HttpMethod.POST, request, Object.class);
        } catch (Exception e) {
            LogUtil.error(log, "Cap Profile Create failed. TraceId" + validatedRequest.getTraceId(), e.getMessage(),
                    capProfileRequest.toString());
            throw e;
        }
        LogUtil.debug(log, "Requesting Cap Customer API - Response: ", printObjectAsString(response));

        // Generally, the 4xx/5xx are handled in exception cases, still writing
        // below
        // block to take only 200 as success
        if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
            log.debug("not 200/201 response: " + printObjectAsString(request));
            handleCapException(response, validatedRequest);
            throw new Exception(
                    String.format("Received %s code. Expected 200 or 201 from CAP", response.getStatusCode()));
        }

        com.anz.mdm.ocv.cap.v2.CapResponse capResponse = new ObjectMapper().convertValue(response.getBody(),
                new CapResponseReferenceDTOV2());
        Long endTime = System.currentTimeMillis();
        LogUtil.info(log, "createCapProfile", validatedRequest.getTraceId(), "Cap Profile Create Request successful",
                (endTime - startTime), validatedRequest.getChannel(), validatedRequest.getRequestBody().getPartyType(),
                OCVConstants.MAINTAINPARTY_SERVICE);
        SourceSystem srcSys = new SourceSystem();
        if (null != capResponse) {
            srcSys.setSourceSystemId(capResponse.getCapcisId());
        }
        srcSys.setSourceSystemName(OCVConstants.CAP_CIS_SOURCE);
        validatedRequest.getRequestBody().getSourceSystems().clear();
        validatedRequest.getRequestBody().getSourceSystems().add(srcSys);
        return new CAPParty(validatedRequest, true);
    }

    private CAPParty fallbackCreateCAPProfile(final APIRequest<Party> apiRequest, final Throwable throwable) {

        if (null != throwable) {
            LogUtil.error(log, METHOD_NAME, apiRequest.getTraceId(), INVOCATION_ERROR, throwable.getMessage(),
                    throwable.toString());
        } else {
            LogUtil.error(log, METHOD_NAME, apiRequest.getTraceId(), OTHER_PROCESSING_ERROR, "Timeout or other error",
                    null);
        }
        return new CAPParty(apiRequest, false);
    }

    private HttpHeaders prepareHeadersForBackend(final APIRequest<Party> apiRequest) {
        LogUtil.debug(log, "prepareHeadersForBackend", apiRequest.getTraceId(), "entering prepareHeadersForBackend");
        final HttpHeaders headers = new HttpHeaders();
        headers.set(OCVConstants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.TRACE_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.AUTHORIZATION_HEADER, apiRequest.getJwtToken());
        headers.set(OCVConstants.SPAN_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.PARENT_SPAN_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.IBM_CLIENT_ID_HEADER, getClientId());
        headers.set(OCVConstants.ORIG_APP_HEADER, OCVConstants.OCV_STRING);
        return headers;
    }

    private HttpHeaders prepareHeadersForCapV2Backend(final APIRequest<Party> apiRequest) {
        LogUtil.debug(log, "prepareHeadersForBackend", apiRequest.getTraceId(), "entering prepareHeadersForBackend");
        final HttpHeaders headers = new HttpHeaders();
        headers.set(OCVConstants.ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(OCVConstants.TRACE_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.AUTHORIZATION_HEADER, apiRequest.getJwtToken());
        headers.set(OCVConstants.SPAN_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.PARENT_SPAN_ID_HEADER, apiRequest.getTraceId());
        headers.set(OCVConstants.IBM_CLIENT_ID_HEADER, getClientId());
        headers.set(OCVConstants.ORIG_APP_HEADER, OCVConstants.OCV_STRING);
        headers.set(CAPConstants.CAP_X_OPERATOR_ID, OCVConstants.OCV_STRING);
        headers.set(CAPConstants.CAP_X_OPERATOR_BRANCH, OCVConstants.BRANCH_ID);
        headers.set(CAPConstants.CAP_X_WORKSTATION, OCVConstants.X_WORKSTATION);
        headers.set(CAPConstants.CAP_X_IDEMPOTENCY_KEY, getOrGenerateUUIDV4(apiRequest));
        headers.set(CAPConstants.CAP_X_IDEMPOTENCY_FIRST_SENT,
                getOrGenerateInstantNow(apiRequest.getHeaders().get(OCVConstants.IDEMPOTENCY_FIRST_SENT)));

        return headers;
    }

    public String getOrGenerateUUIDV4(final APIRequest<Party> apiRequest) {
        String idempotencyKey = apiRequest.getHeaders().get(OCVConstants.IDEMPOTENCY_KEY);
        try {
            UUID uuid = UUID.fromString(idempotencyKey);
            if (uuid.version() != 4) {
                log.info("invalid UUID version, expected v4");
                throw new IllegalArgumentException("UUID Version is NOT 4");
            }
            idempotencyKey = uuid.toString();
        } catch (IllegalArgumentException e) {
            log.info("invalid UUID input for IDEMPOTENCY_KEY, ");
            UUID uuid = Generators.randomBasedGenerator().generate();
            log.info("UUID v4 generated: " + uuid);
            idempotencyKey = uuid.toString();
        } catch (NullPointerException e) {
            log.info("missing IDEMPOTENCY_KEY, received null");
            UUID uuid = Generators.randomBasedGenerator().generate();
            log.info("UUID v4 generated: " + uuid);
            idempotencyKey = uuid.toString();
        }
        return idempotencyKey;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public Map<String, Party> getIdMap(SearchPartyResultWrapper serchResult) {
        Map<String, Party> idMap = new HashMap<String, Party>();
        Optional.ofNullable(serchResult.getCertifiedResults())
                .ifPresent(certPartyList -> getIdMapFromResults(certPartyList, idMap));
        return idMap;

    }

    public Map<String, Party> getIdMapFromResults(List<Party> partyList, Map<String, Party> idMap) {
        partyList.forEach(partyObject -> {
            Optional.ofNullable(partyObject).ifPresent(availablePartyObject -> {
                getPartySrcId(availablePartyObject, idMap);
            });
        });
        return idMap;
    }

    public void getPartySrcId(Party partyObject, Map<String, Party> idMap) {
        if (null != partyObject.getSourceSystems() && partyObject.getSourceSystems().size() > 0) {
            partyObject.getSourceSystems().forEach(srcSysObject -> {
                getPartyBySrc(srcSysObject, idMap, partyObject);
            });
        } else {
            idMap.put(OCVConstants.FENERGO_SOURCE, partyObject);
        }
    }

    public void getPartyBySrc(SourceSystem srcSysObject, Map<String, Party> idMap, Party partyObject) {
        if (null != srcSysObject && StringUtils.isNotBlank(srcSysObject.getSourceSystemName())) {
            if (srcSysObject.getSourceSystemName().equalsIgnoreCase(OCVConstants.CAP_CIS_SOURCE)) {
                idMap.put(OCVConstants.CAP_CIS_SOURCE, partyObject);
            } else if (srcSysObject.getSourceSystemName().equalsIgnoreCase(OCVConstants.FENERGO_SOURCE)) {
                idMap.put(OCVConstants.FENERGO_SOURCE, partyObject);
            }
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public APIRequest<Party> getRetrieveRequest(final APIRequest<Party> validatedAPIRequest)
            throws IllegalAccessException, InvocationTargetException {

        APIRequest<Party> retireveRequest = new APIRequest<Party>(
                new HashMap<String, String>(validatedAPIRequest.getHeaders()),
                new HashMap<String, String>(validatedAPIRequest.getQueryParameters()), new Party());
        BeanUtils.copyProperties(retireveRequest.getRequestBody(), validatedAPIRequest.getRequestBody());
        List<Identifier> identifierList = getSetIdentifierList(validatedAPIRequest.getRequestBody().getIdentifiers());
        retireveRequest.getRequestBody().getSourceSystems().clear();
        retireveRequest.getRequestBody().getIdentifiers().clear();
        retireveRequest.getRequestBody().getIdentifiers().addAll(identifierList);
        return retireveRequest;
    }

    private List<Identifier> getSetIdentifierList(List<Identifier> validatedRequestIden) {
        List<Identifier> identifierList = new ArrayList<Identifier>();
        validatedRequestIden.forEach(identifier -> {
            Optional.ofNullable(identifier).ifPresent(identifierObject -> {
                if (identifierObject.getIdentifierUsageType().equals(OCVConstants.OCV_ID_IDENTIFIER_TYPE)) {
                    identifierList.add(identifierObject);
                }
            });
        });
        return identifierList;
    }

    public void handleCapException(ResponseEntity<Object> response, APIRequest<Party> validatedRequest)
            throws AvailabilityServiceDownException {
        ObjectMapper mapper = new ObjectMapper();

        CapErrorResponseDTO errResponse = mapper.convertValue(response.getBody(), new ErrorReferenceDTO());
        if (errResponse == null) {
            return;
        }
        if (errResponse.getErrors() == null) {
            handleAPIcException(errResponse, validatedRequest);
        } else if (errResponse.getErrors().size() > 0) {
            String code = errResponse.getErrors().get(0).getCode();
            String description = errResponse.getErrors().get(0).getDescription();
            String message = errResponse.getErrors().get(0).getMessage();

            if (StringUtils.isNoneBlank(code) && StringUtils.isNoneBlank(description)
                    && OCVConstants.CAP_UNAVAILABLE_CODE.equalsIgnoreCase(code)
                    && description.contains(OCVConstants.CAP_UNAVAILABLE_DESCRIPTION)) {
                LogUtil.error(log, "createProfileInCap ", validatedRequest.getTraceId(),
                        OCVConstants.CAP_SERVICE_UNAVAILABLE, OCVConstants.CAP_UNAVAILABLE_ERROR_CODE,
                        validatedRequest.getChannel(), "", OCVConstants.CAPTRANSFORMATION_SERVICE);
                throw new AvailabilityServiceDownException(OCVConstants.CAP_SERVICE_UNAVAILABLE,
                        OCVConstants.CAP_UNAVAILABLE_ERROR_CODE);
            }

            LogUtil.error(log, "Cap Profile Create failed.", validatedRequest.getTraceId(), description, message, code);
        }
    }

    public void handleAPIcException(CapErrorResponseDTO errResponse, APIRequest<Party> validatedRequest) {
        String code = Integer.toString(errResponse.getHttpCode());
        String description = errResponse.getMoreInformation();
        String message = errResponse.getHttpMessage();
        if (StringUtils.isNoneBlank(code) && StringUtils.isNoneBlank(description)) {
            LogUtil.error(log, "Cap Profile Create failed.", validatedRequest.getTraceId(), description, message, code);
            if (OCVConstants.APIC_INSUFFICIENT_SCOPE.equalsIgnoreCase(description)
                    && OCVConstants.APIC_FORBIDDEN_ERROR_CODE.equalsIgnoreCase(code)) {
                throw new JwtValidationException(OCVConstants.INVALID_APIC_CAP_SCOPE_ERROR_CODE,
                        OCVConstants.APIC_INSUFFICIENT_SCOPE);
            }
        }
    }

    private RestTemplate addCustomErrorHandler() {
        restTemplate.setErrorHandler(new RestTemplateCapResponseErrorHandler("BAD_REQUEST"));
        return restTemplate;
    }

    public String getOrGenerateInstantNow(String datetime) {
        DateTimeFormatter ocvDateTimeFormatter = DateTimeFormatter.ofPattern(OCV_DATETIME_FORMATTER);
        DateTimeFormatter capDateTimeFormatter = DateTimeFormatter.ofPattern(CAP_DATETIME_FORMATTER);

        LocalDateTime localDateTime = getLocalDateTimeIfValid(ocvDateTimeFormatter, datetime);
        if (localDateTime == null) {
            localDateTime = getLocalDateTimeIfValid(capDateTimeFormatter, datetime);
            if (localDateTime == null) {
                localDateTime = LocalDateTime.now();
            }
        }
        return getSecondsLevelInstant(localDateTime).toString();
    }

    public LocalDateTime getLocalDateTimeIfValid(DateTimeFormatter formatter, String datetime) {
        try {
            return LocalDateTime.parse(datetime, formatter);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private Instant getSecondsLevelInstant(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.SECONDS).toInstant(ZoneOffset.UTC);
    }

}
