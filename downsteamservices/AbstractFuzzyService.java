package com.anz.mdm.ocv.api.downsteamservices;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.anz.mdm.ocv.api.constants.OCVConstants;

import com.anz.mdm.ocv.api.util.LogUtil;
import com.anz.mdm.ocv.api.validator.APIRequest;
import com.anz.mdm.ocv.api.validator.SearchResponseFilter;
import com.anz.mdm.ocv.party.v1.Party;
import com.anz.mdm.ocv.party.v1.SearchPartyResultWrapper;

import lombok.extern.slf4j.Slf4j;

/*
 * This abstract service class for fuzzy search.
 * @author Deepika Handa
 */
@Slf4j
public abstract class AbstractFuzzyService {

    @Autowired
    private SearchResponseFilter filter;

    public abstract SearchPartyResultWrapper getResponse(APIRequest<Party> request, String traceId) throws IOException;

    public List<Party> filterResponse(APIRequest<Party> request, List<Party> parties) {
        String partyType = null;
        String orgType = null;
        String traceId = null;
        String channel = null;
        List<Party> filteredParties = null;
        if (!isEmpty(request) && !isEmpty(request.getRequestBody())) {
            partyType = request.getRequestBody().getPartyType();
            orgType = request.getRequestBody().getOrganisationType();
            traceId = request.getTraceId();
            channel = request.getChannel();

            // filter based on party type
            filteredParties = filter.filterRecordsBasedOnPartyType(parties, partyType, traceId);
            LogUtil.debug(log, "filterResponse", request.getTraceId(), "Entering filterResponse");

            // filter based on organisationType
            if (OCVConstants.NON_INDIVIDUAL_PARTY_TYPE.equalsIgnoreCase(partyType) && !isEmpty(orgType)) {
                filteredParties = filter.filterRecordsBasedOnOrgType(orgType, parties, traceId);
            }

            // filter non CLG parties MMLO specific scenario
            if (OCVConstants.NON_INDIVIDUAL_PARTY_TYPE.equalsIgnoreCase(partyType) && isEmpty(orgType)
                    && OCVConstants.MMLO_CHANNEL.equalsIgnoreCase(channel)) {
                filteredParties = filter.filterOrgRecordsNotClg(parties, traceId);
            }
        }
        return filteredParties;
    }

    public void sortPartiesBasedonSearchScore(List<Party> parties) {
        Comparator<Party> compareBySearchScore = Comparator.comparing(Party::getSearchScore);
        Collections.sort(parties, compareBySearchScore.reversed());
    }
}
