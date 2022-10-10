/* included as part of JWT 17031 feature */

package com.anz.mdm.ocv.api.downsteamservices;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.stereotype.Service;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.JwtValidationException;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.jwt.exception.EndpointTimeoutException;
import com.anz.mdm.ocv.jwt.exception.MalformedBodyException;
import com.anz.mdm.ocv.jwt.exception.MalformedHeaderException;
import com.anz.mdm.ocv.jwt.exception.MalformedSignatureException;
import com.anz.mdm.ocv.jwt.util.LogUtil;
import com.anz.mdm.ocv.jwt.validator.impl.JWTSignatureValidatorServiceImpl;
import com.anz.mdm.ocv.party.v1.Party;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Service
@Slf4j
public class JWTService {

    @Autowired
    private JWTSignatureValidatorServiceImpl jwtSignatureValidatorServiceImpl;

    public JWTService(JWTSignatureValidatorServiceImpl jwtSignatureValidatorServiceImpl) {
        super();
        this.jwtSignatureValidatorServiceImpl = jwtSignatureValidatorServiceImpl;
    }

    public boolean getSignatureStatus(String jwtToken, String endPointUrl, APIRequest<Party> apiRequest,
            String keysFromConfigMap)
            throws ParseException, BadJOSEException, JOSEException, IOException, ExhaustedRetryException {
        boolean signatureValidationStatus = false;
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "Entering : Call Signature validation API", apiRequest.getTraceId(), "");
        try {
            signatureValidationStatus = jwtSignatureValidatorServiceImpl.validateisSignedToken(jwtToken, endPointUrl,
                    apiRequest.getTraceId(), keysFromConfigMap);
        } catch (MalformedHeaderException e) {
            LogUtil.debug(log, "SignatureStatus MalformedHeaderException MaintainPartyApi", apiRequest.getTraceId(),
                    "Header tampered causing signature validation failure. Pls try with a valid JWT",
                    System.currentTimeMillis() - startTime);
            LogUtil.error(log, "SignatureStatus MalformedHeaderException MaintainPartyApi", apiRequest.getTraceId(), "",
                    "Header tampered causing signature validation failure. Pls try with a valid JWT",
                    OCVConstants.INVALID_JWT_HEADER, OCVConstants.INVALID_JWT_HEADER_CODE);
            throw new JwtValidationException(OCVConstants.INVALID_JWT_HEADER_CODE, OCVConstants.INVALID_JWT_HEADER);
        } catch (MalformedSignatureException e) { // Invalid Signature
            LogUtil.debug(log, "SignatureStatus MalformedSignatureException MaintainPartyApi", apiRequest.getTraceId(),
                    "Header and Signature tampered causing signature validation failure. Pls try with a valid JWT",
                    System.currentTimeMillis() - startTime);
            LogUtil.error(log, "SignatureStatus MalformedSignatureException MaintainPartyApi", apiRequest.getTraceId(),
                    "", "Header and Signature tampered causing signature validation failure. Pls try with a valid JWT",
                    OCVConstants.INVALID_JWT_SIGNATURE, OCVConstants.INVALID_JWT_SIGNATURE_CODE);
            throw new JwtValidationException(OCVConstants.INVALID_JWT_SIGNATURE_CODE,
                    OCVConstants.INVALID_JWT_SIGNATURE);
        } catch (com.anz.mdm.ocv.jwt.exception.SecurityException e) { // Expired JWT
            LogUtil.debug(log, "SignatureStatus SecurityException MaintainPartyApi", apiRequest.getTraceId(),
                    "JWT Token Expired. Pls try with a valid JWT", System.currentTimeMillis() - startTime);
            LogUtil.error(log, "SignatureStatus SecurityException MaintainPartyApi", apiRequest.getTraceId(), "",
                    "JWT Token Expired. Pls try with a valid JWT", OCVConstants.INVALID_JWT_EXPIRED,
                    OCVConstants.INVALID_JWT_EXPIRED_CODE);
            throw new JwtValidationException(OCVConstants.INVALID_JWT_EXPIRED_CODE, OCVConstants.INVALID_JWT_EXPIRED);
        } catch (EndpointTimeoutException e) { // Connection / Read time out in endpoint.
            LogUtil.debug(log, "SignatureStatus EndpointTimeoutException MaintainPartyApi", apiRequest.getTraceId(),
                    "Connection / Read timeout in endpoint", System.currentTimeMillis() - startTime);
            LogUtil.error(log, "SignatureStatus EndpointTimeoutException MaintainPartyApi", apiRequest.getTraceId(), "",
                    "Connection / Read timeout in endpoint", OCVConstants.INVALID_JWT_TIMEOUT,
                    OCVConstants.INVALID_JWT_TIMEOUT_CODE);
            throw new JwtValidationException(OCVConstants.INVALID_JWT_TIMEOUT_CODE, OCVConstants.INVALID_JWT_TIMEOUT);
        } catch (MalformedBodyException e) { // Connection / Read time out in endpoint.
            LogUtil.debug(log, "SignatureStatus MalformedBodyException MaintainPartyApi", apiRequest.getTraceId(),
                    "Invalid JWT Body", System.currentTimeMillis() - startTime);
            LogUtil.error(log, "SignatureStatus MalformedBodyException MaintainPartyApi", apiRequest.getTraceId(), "",
                    "Invalid JWT Body", OCVConstants.INVALID_JWT_BODY, OCVConstants.INVALID_JWT_BODY_CODE);
            throw new JwtValidationException(OCVConstants.INVALID_JWT_BODY_CODE, OCVConstants.INVALID_JWT_BODY);
        }

        LogUtil.info(log, "SignatureStatus", apiRequest.getTraceId(), "",
                "JWT Header and Signature validation Completed Successfully : " + signatureValidationStatus,
                System.currentTimeMillis() - startTime);

        return signatureValidationStatus;
    }
}
