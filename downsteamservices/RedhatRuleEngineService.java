package com.anz.mdm.ocv.api.downsteamservices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.ServiceUnavailableException;
import com.anz.mdm.ocv.api.exception.UnauthorizedException;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.util.RedhatRuleEngineUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.party.v1.Party;

import lombok.extern.slf4j.Slf4j;

@Service
@EnableRetry
@Slf4j
public class RedhatRuleEngineService {

    private static final String METHOD_NAME = "invokeRedHatRuleEngine";
    
    @Autowired
    @Qualifier("RedhatRuleEngineServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${RedhatRuleEngineService.url}")
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Retryable(value = { ServiceUnavailableException.class },
            maxAttemptsExpression = "#{${rhdmMaintainParty.maxRetryCount}}",
            backoff = @Backoff(delayExpression = "#{${rhdmMaintainParty.delay}}",
                    multiplierExpression = "#{${rhdmMaintainParty.multiplier}}",
                    maxDelayExpression = "#{${rhdmMaintainParty.maxDelay}}"))
    public Object invokeRedHatRuleEngine(APIRequest<Party> apiRequest) {
        Object response = null;
        ResponseEntity<Party> rhdmResponse = null;
        String traceId = apiRequest.getTraceId();
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(),
                "Entering RedhatRuleEngineService : invokeRedHatRuleEngine");

        HttpEntity<Party> request = RedhatRuleEngineUtil.prepareInput(apiRequest, startTime);
        StandardisedParty reqStan = new StandardisedParty(apiRequest.getRequestBody(), false);
        try {
            LogUtil.debug(log, METHOD_NAME, traceId, "invokeRedHatRuleEngine:" + url);
            if (null != request) {
                rhdmResponse = restTemplate.exchange(url, HttpMethod.POST, request, Party.class);
                StandardisedParty resStan = new StandardisedParty(rhdmResponse.getBody(), true);
                response = RedhatRuleEngineUtil.prepareSuccessOutput(reqStan, resStan, apiRequest, startTime);
            }
        } catch (ResourceAccessException ex) {
            if (ex.getMessage().contains("SocketTimeoutException")) {
                throw new ServiceUnavailableException("5005");
            } else if (ex.getMessage().contains("ConnectTimeoutException")) {
                throw new ServiceUnavailableException("5004");
            } else {
                throw new ServiceUnavailableException("5002");
            }
        } catch (HttpServerErrorException ex) {
            throw new ServiceUnavailableException("5002");
        } catch (Unauthorized ex) {
            throw new UnauthorizedException("5006", OCVConstants.UNAUTHORIZED_RHDM_EXCEPTION);
        } catch (Exception ex) {
            throw ex;
        }

        Long endTime = System.currentTimeMillis();
        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(),
                "Exiting RedhatRuleEngineService : invokeRedHatRuleEngine", (endTime - startTime));
        return response;
    }

    /**
     * This method will trigger for failure recovery from RHDM call.
     */
    @Recover
    public Object retryFromRecover(final Throwable throwable, final APIRequest<Party> apiRequest) throws Exception {

        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(), "Entering retryFromRecover");

        Object response = RedhatRuleEngineUtil.prepareErrorOutput(throwable, apiRequest);

        LogUtil.debug(log, METHOD_NAME, apiRequest.getTraceId(), "Exiting retryFromRecover");

        return response;
    }

}
