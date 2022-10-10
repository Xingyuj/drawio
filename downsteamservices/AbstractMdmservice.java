package com.anz.mdm.ocv.api.downsteamservices;

import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.constants.OCVConstants;
import com.anz.mdm.ocv.api.exception.ServerException;
import com.anz.mdm.ocv.api.exception.ServiceUnavailableException;
import com.anz.mdm.ocv.api.exception.UnAuthorizedMDMException;
import com.anz.mdm.ocv.api.transform.Context;
import com.anz.mdm.ocv.api.transform.DataTransformer;
import com.anz.mdm.ocv.api.transform.DataTransformerFactory;
import com.anz.mdm.ocv.api.util.LogRequestModel;
import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.ibm.mdm.schema.TCRMService;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides default implementation for the methods required for services using
 * MDM as backend.
 * 
 */
@Slf4j
public abstract class AbstractMdmservice implements DownstreamService {
    @Autowired
    private DataTransformer dataTransformer;

    @Autowired
    private LogRequestModel logAttributes;

    @Override
    public abstract ValidationResult validateResponse(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception;

    @Override
    public HttpEntity<String> invokeDownStreamService(HttpEntity<String> request, String url, RestTemplate restemplate,
            HttpMethod method, String traceId) throws ResourceAccessException, HttpServerErrorException {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "Entering : invokeDownStreamService", traceId, "call backend MDM service : " + url);
        HttpEntity<String> response = null;
        try {
            response = restemplate.exchange(url, HttpMethod.PUT, request, String.class);
        } catch (ResourceAccessException ex) {
            if (ex.getMessage().contains("SocketTimeoutException")) {
                LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(), "5005",
                        logAttributes.getChannel(), logAttributes.getPartyType(), logAttributes.getServiceName());
                throw new ServiceUnavailableException("5005");
            } else if (ex.getMessage().contains("ConnectTimeoutException")) {
                LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(), "5004",
                        logAttributes.getChannel(), logAttributes.getPartyType(), logAttributes.getServiceName());
                throw new ServiceUnavailableException("5004");
            } else {
                throw new ServiceUnavailableException("5002");
            }
        } catch (HttpServerErrorException ex) {
            throw new ServiceUnavailableException("5002");
        } catch (Unauthorized ex) {
            LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(), "5006", logAttributes.getChannel(),
                    logAttributes.getPartyType(), logAttributes.getServiceName());
            throw new UnAuthorizedMDMException("5006", OCVConstants.UNAUTHORIZED_MDM_EXCEPTION);
        }
        LogUtil.debug(log, "Exit : invokeDownStreamService", traceId, "call backend MDM service is complete",
                (System.currentTimeMillis() - startTime));
        return response;
    }

    @Override
    public HttpEntity<TCRMService> invokeMdmDownStreamService(HttpEntity<String> request, String url,
            RestTemplate restemplate, HttpMethod method, String traceId)
            throws ResourceAccessException, HttpServerErrorException {
        Long startTime = System.currentTimeMillis();
        LogUtil.debug(log, "Entering : invokeMdmDownStreamService", traceId, "call backend MDM service : " + url);
        HttpEntity<TCRMService> response = null;
        try {
            response = restemplate.exchange(url, HttpMethod.PUT, request, TCRMService.class);
        } catch (ResourceAccessException ex) {
            if (ex.getMessage().contains("SocketTimeoutException")) {
                LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(),
                        OCVConstants.READ_TIMEOUT_EXCEPTION, logAttributes.getChannel(), logAttributes.getPartyType(),
                        logAttributes.getServiceName());
                throw new ServiceUnavailableException(OCVConstants.READ_TIMEOUT_EXCEPTION);
            } else if (ex.getMessage().contains("ConnectTimeoutException")) {
                LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(),
                        OCVConstants.CONNECT_TIMEOUT_EXCEPTION, logAttributes.getChannel(),
                        logAttributes.getPartyType(), logAttributes.getServiceName());
                throw new ServiceUnavailableException(OCVConstants.CONNECT_TIMEOUT_EXCEPTION);
            } else {
                throw new ServiceUnavailableException(OCVConstants.SERVICE_NOT_AVAILABLE);
            }
        } catch (HttpServerErrorException ex) {
            throw new ServiceUnavailableException(OCVConstants.SERVICE_NOT_AVAILABLE);
        } catch (Unauthorized ex) {
            LogUtil.error(log, "invokeDownStreamService ", traceId, ex.getMessage(),
                    OCVConstants.UNAUTHORIZED_EXCEPTION, logAttributes.getChannel(), logAttributes.getPartyType(),
                    logAttributes.getServiceName());
            throw new UnAuthorizedMDMException("5006", OCVConstants.UNAUTHORIZED_MDM_EXCEPTION);
        }
        LogUtil.debug(log, "Exit : invokeMdmDownStreamService", traceId, "call backend MDM service is complete",
                (System.currentTimeMillis() - startTime));
        return response;
    }

    public String transformXmlResponseToJson(HttpEntity<String> xmlResponse, Context context, String traceId) {
        String jsonResponse = null;
        try {
            LogUtil.debug(log, "transformXmlResponseToJson", traceId, "entering transformXmlResponseToJson");
            Long startTime1 = System.currentTimeMillis();
            DataTransformerFactory factory = DataTransformerFactory.getInstance();
            Transformer transformerInstance = factory.getTransformer(context, traceId);
            jsonResponse = dataTransformer.transformData(xmlResponse, transformerInstance, traceId, context);

            LogUtil.debug(log, "transformXmlResponseToJson using transformer", traceId,
                    "Exit transformXmlResponseToJson", (System.currentTimeMillis() - startTime1));
        } catch (IOException e) {
            LogUtil.debug(log, "transformXmlResponseToJson", traceId, "FileNotFoundException : throw ServerException");
            throw new ServerException(e);
        } catch (TransformerConfigurationException e) {
            LogUtil.debug(log, "transformXmlResponseToJson", traceId,
                    "TransformerConfigurationException : throw ServerException");
            throw new ServerException(e);
        }
        return jsonResponse;
    }

    public HttpHeaders prepareHeadersForBackened(String traceId) {
        LogUtil.debug(log, "prepareHeadersForBackend", traceId, "entering prepareHeadersForBackend");
        HttpHeaders headers = new HttpHeaders();
        headers.set(OCVConstants.ACCEPT_HEADER, OCVConstants.APPLICATION_XML);
        headers.add(OCVConstants.TARGETAPPLICATION, OCVConstants.TCRM);
        headers.add(OCVConstants.REQUESTTYPE, OCVConstants.STANDARD);
        headers.add(OCVConstants.PARSER, OCVConstants.TCRMSERVICE);
        headers.add(OCVConstants.RESPONSETYPE, OCVConstants.STANDARD);
        headers.add(OCVConstants.CONSTRUCTOR, OCVConstants.TCRMSERVICE);
        headers.add(OCVConstants.OPERATIONTYPE, OCVConstants.ALL);
        return headers;
    }

}
