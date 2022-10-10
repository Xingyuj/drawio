package com.anz.mdm.ocv.api.downsteamservices;

import com.anz.mdm.ocv.api.exception.ServiceUnavailableException;
import com.anz.mdm.ocv.api.exception.VaultBatchInputException;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.party.v1.Party;

public interface TokenService {

    void decryptSensitiveAttributes(APIRequest<Party> apiRequest)
            throws ServiceUnavailableException, VaultBatchInputException;

    boolean checkForSensitiveConsumer(APIRequest<Party> apiRequest);

}