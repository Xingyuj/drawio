package com.anz.mdm.ocv.api.downsteamservices;

import javax.xml.xpath.XPathExpressionException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestTemplate;

import com.anz.mdm.ocv.api.validator.ValidationResult;
import com.ibm.mdm.schema.TCRMService;

/**
 * Interface for all the downstream systems like MDM and Elastic Search
 * 
 */

public interface DownstreamService {
    HttpEntity<String> invokeDownStreamService(HttpEntity<String> request, String url, RestTemplate restemplate,
            HttpMethod method, String traceId);

    ValidationResult validateResponse(HttpEntity<String> xmlResponse, String traceId)
            throws XPathExpressionException, Exception;

    HttpEntity<TCRMService> invokeMdmDownStreamService(HttpEntity<String> request, String url, RestTemplate restemplate,
            HttpMethod method, String traceId) throws ResourceAccessException, HttpServerErrorException;

    ValidationResult validateMdmResponse(HttpEntity<TCRMService> xmlResponse, String traceId,
            ValidationResult validationResult, String requestMDMXml) throws XPathExpressionException, Exception;

}
