package com.anz.mdm.ocv.api.downsteamservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.ServiceUnavailableException;
import com.anz.mdm.ocv.api.exception.VaultBatchInputException;
import com.anz.mdm.ocv.api.model.BatchInput;
import com.anz.mdm.ocv.api.model.BatchResult;
import com.anz.mdm.ocv.api.model.VaultRequest;
import com.anz.mdm.ocv.api.util.JWTScope;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.util.VaultUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.common.v1.Identifier;
import com.anz.mdm.ocv.common.v1.RelatedIdentifier;
import com.anz.mdm.ocv.party.v1.Party;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Value("${vault.decrypt-endpoint}")
    private String decryptEndpoint;

    @Value("${tokenise.anzxWriteScope}")
    private String anzxWriteScope;

    @Autowired
    private VaultUtil vaultUtil;

    @Autowired
    @Qualifier("tokenRestTemplate")
    private RestTemplate tokenRestTemplate;

    @Override
    public void decryptSensitiveAttributes(APIRequest<Party> apiRequest)
            throws ServiceUnavailableException, VaultBatchInputException {
        List<BatchInput> batchInputs = new ArrayList<>();
        checkSensitiveIdentifiers(apiRequest.getRequestBody(), batchInputs, apiRequest.getTraceId(),
                apiRequest.getChannel());
        if (!ObjectUtils.isEmpty(batchInputs)) {
            LogUtil.debug(log, "decryptSensitiveAttributes", apiRequest.getTraceId(),
                    "input token size: " + batchInputs.size());
            List<BatchResult> batchResults = hitOCVVaultService(batchInputs, apiRequest.getTraceId());
            replaceDeTokenValue(batchResults, apiRequest);
        }
    }

    @Override
    public boolean checkForSensitiveConsumer(APIRequest<Party> apiRequest) {
        JWTScope jwtScope = apiRequest.getJwtScope();
        List<String> requestedScopes = Arrays.asList(jwtScope.getScopes());
        return requestedScopes.contains(anzxWriteScope);
    }

    private void replaceDeTokenValue(List<BatchResult> batchResults, APIRequest<Party> apiRequest) {
        Party party = apiRequest.getRequestBody();
        if (!ObjectUtils.isEmpty(party.getIdentifiers())) {
            List<Identifier> identifierList = party.getIdentifiers();
            for (Identifier identifier : identifierList) {
                identifier.setIdentifier(isItemExists(batchResults, identifier, apiRequest.getTraceId(),
                        apiRequest.getChannel(), apiRequest.getRequestBody().getPartyType()));
                if (null != identifier.getRelatedIdentifiers() && identifier.getRelatedIdentifiers().size() > 0) {
                    for (RelatedIdentifier childId : identifier.getRelatedIdentifiers()) {
                        childId.setIdentifier(isChildIdentifier(batchResults, childId, apiRequest.getTraceId(),
                                apiRequest.getChannel(), apiRequest.getRequestBody().getPartyType()));
                    }
                }
            }
            //apiRequest.setRequestBody(party);
        }
    }

    private String isChildIdentifier(List<BatchResult> batchResults,
            RelatedIdentifier childId, String traceId, String channel, String partyType) {

        Long startTime = System.currentTimeMillis();
        LogUtil.info(log, "replaceDeTokenValue", traceId,
                "Getting into isChildIdentifier method for setting up the decoded value",
                (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
        BatchResult batchResultForRelId = batchResults.stream()
                .filter(p -> p.getActual_value().equals(childId.getIdentifier())).findAny().orElse(null);
        if (!ObjectUtils.isEmpty(batchResultForRelId)) {
            LogUtil.info(log, "replaceDeTokenValue in child Identifier ", traceId,
                    childId.getIdentifierUsageType() + " decrypted token size: "
                            + (StringUtils.isEmpty(batchResultForRelId.getDecoded_value()) ? 0
                            : batchResultForRelId.getDecoded_value().length()),
                    (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
            return batchResultForRelId.getDecoded_value();
        } else {
            LogUtil.info(log, "replaceDeTokenValue in child Identifier", traceId,
                    childId.getIdentifierUsageType() + " raw identifier token size: "
                            + (StringUtils.isEmpty(childId.getIdentifier()) ? 0
                            : childId.getIdentifier().length()),
                    (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
            return childId.getIdentifier();
        }


    }

    public List<BatchResult> hitOCVVaultService(List<BatchInput> batchInputs, String traceId)
            throws ServiceUnavailableException, VaultBatchInputException {
        LogUtil.debug(log, "hitOCVVaultService", traceId, "Entering: hitOCVVaultService");
        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-b3-traceId", traceId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        VaultRequest vaultRequest = new VaultRequest();
        vaultRequest.setBatch_input(batchInputs);

        HttpEntity<Object> entity = new HttpEntity<>(vaultRequest, headers);

        ResponseEntity<List<BatchResult>> tokenResponse;
        LogUtil.debug(log, "hitOCVVaultService", traceId,
                decryptEndpoint);
        try {
            tokenResponse = tokenRestTemplate.exchange(
                    decryptEndpoint,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<BatchResult>>() {
                    });
        } catch (HttpClientErrorException
                | ServiceUnavailableException
                | HttpServerErrorException e) {

            handleVaultBatchException(e, traceId);

            LogUtil.error(log, "hitOCVVaultService", traceId, "Exception at OCV-Vault service",
                    e.getMessage(), OCVConstants.OCV_VAULT_SERVICE_NOT_AVAILABLE_EXCEPTION);
            throw new ServiceUnavailableException(OCVConstants.OCV_VAULT_SERVICE_NOT_AVAILABLE_EXCEPTION,
                    OCVConstants.OCV_VAULT_SERVICE_UNAVAILABLE_MSG);
        }
        if (tokenResponse.hasBody() && tokenResponse.getBody() != null) {
            List<BatchResult> batchResultList = tokenResponse.getBody();
            LogUtil.debug(log, "hitOCVVaultService", traceId, "Success response ",
                    (System.currentTimeMillis() - startTime));
            return batchResultList;
        }
        LogUtil.error(log, "hitOCVVaultService ", traceId,
                OCVConstants.INVALID_OCV_VAULT_RESPONSE_MSG, "Invalid Response from OCV-Vault service",
                OCVConstants.OCV_VAULT_INVALID_FORMAT_EXCEPTION);
        throw new ServiceUnavailableException(OCVConstants.OCV_VAULT_INVALID_FORMAT_EXCEPTION,
                OCVConstants.INVALID_OCV_VAULT_RESPONSE_MSG);
    }

    private void handleVaultBatchException(Exception e, String traceId)
            throws VaultBatchInputException {

        if (e instanceof HttpClientErrorException) {
            LogUtil.error(log, "hitOCVVaultService", traceId,
                    OCVConstants.VAULT_SERVICE_BATCH_INPUT_EXCEPTION_MSG, e.getMessage(),
                    OCVConstants.VAULT_SERVICE_BATCH_INPUT_EXCEPTION);
            throw new VaultBatchInputException(OCVConstants.VAULT_SERVICE_BATCH_INPUT_EXCEPTION,
                    OCVConstants.VAULT_SERVICE_BATCH_INPUT_EXCEPTION_MSG);
        }
    }

    private void checkSensitiveIdentifiers(Party party, List<BatchInput> batchInputs, String traceId, String channel) {
        Long startTime = System.currentTimeMillis();
        if (!ObjectUtils.isEmpty(party.getIdentifiers())) {
            List<Identifier> identifierList = party.getIdentifiers();
            List<String> configIdentifiers = vaultUtil.getIdentifiers();
            for (Identifier identifier : identifierList) {
                if (vaultUtil.isIdentifiersExists(configIdentifiers, identifier.getIdentifierUsageType())) {
                    // OCT-25030: logging the length of the input sensitive identifier
                    LogUtil.info(log, "checkSensitiveIdentifiers", traceId,
                            identifier.getIdentifierUsageType() + " encrypted token size: "
                                    + (StringUtils.isEmpty(identifier.getIdentifier()) ? 0
                                            : identifier.getIdentifier().length()),
                            (System.currentTimeMillis() - startTime), channel, party.getPartyType(),
                            OCVConstants.MAINTAINPARTY_SERVICE);
                    
                    BatchInput batchInput = new BatchInput();
                    batchInput.setTransformation(OCVConstants.FPE_IDENTIFIER);
                    batchInput.setValue(identifier.getIdentifier());
                    batchInputs.add(batchInput);
                    checkForRelatedIdentifier(traceId,identifier,batchInputs,channel,party);
                }
            }
        }
    }

    private void checkForRelatedIdentifier(String traceId, Identifier identifier, List<BatchInput> batchInputs,
            String channel, Party party) {
        LogUtil.debug(log,"Inside checkForRelatedIdentifier",traceId);
        Long startTime = System.currentTimeMillis();
        if (null != identifier.getRelatedIdentifiers() && identifier.getRelatedIdentifiers().size() > 0) {
            for (RelatedIdentifier childIdentifier : identifier.getRelatedIdentifiers()) {
                LogUtil.info(log, "checkForRelatedIdentifier", traceId,
                        identifier.getIdentifierUsageType() + " encrypted token size: "
                                + (StringUtils.isEmpty(identifier.getIdentifier()) ? 0
                                : identifier.getIdentifier().length()),
                        (System.currentTimeMillis() - startTime), channel, party.getPartyType(),
                        OCVConstants.MAINTAINPARTY_SERVICE);
                BatchInput batchInput = new BatchInput();
                batchInput.setTransformation(OCVConstants.FPE_IDENTIFIER);
                batchInput.setValue(childIdentifier.getIdentifier());
                batchInputs.add(batchInput);
            }
        }
        LogUtil.debug(log,"Exiting checkForRelatedIdentifier",traceId);

    }

    private String isItemExists(List<BatchResult> batchResults, Identifier identifier, String traceId, String channel,
            String partyType) {
        Long startTime = System.currentTimeMillis();
        LogUtil.info(log, "replaceDeTokenValue", traceId,
                "Getting into isItemExists method for setting up the decoded value",
                (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
        BatchResult batchResult = batchResults.stream()
                .filter(p -> p.getActual_value().equals(identifier.getIdentifier())).findAny().orElse(null);
        if (!ObjectUtils.isEmpty(batchResult)) {
            // OCT-25030: logging the length of the decrypted sensitive identifier
            LogUtil.info(log, "replaceDeTokenValue", traceId,
                    identifier.getIdentifierUsageType() + " decrypted token size: "
                            + (StringUtils.isEmpty(batchResult.getDecoded_value()) ? 0
                                    : batchResult.getDecoded_value().length()),
                    (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
            return batchResult.getDecoded_value();
        } else {
            // OCT-25030: logging the length of the raw-identifier sensitive identifier
            LogUtil.info(log, "replaceDeTokenValue", traceId,
                    identifier.getIdentifierUsageType() + " raw identifier token size: "
                            + (StringUtils.isEmpty(identifier.getIdentifier()) ? 0
                                    : identifier.getIdentifier().length()),
                    (System.currentTimeMillis() - startTime), channel, partyType, OCVConstants.MAINTAINPARTY_SERVICE);
            return identifier.getIdentifier();
        }
    }

    public void setMockUtils(String vaultEndpoint1, VaultUtil vaultUtil1) {
        this.decryptEndpoint = vaultEndpoint1;
        this.vaultUtil = vaultUtil1;
    }
}