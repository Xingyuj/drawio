package com.anz.mdm.ocv.api.downsteamservices;

import com.anz.mdm.ocv.party.v1.Party;

public class StandardisedParty {

    private Party party;
    private boolean result;

    public StandardisedParty(final Party party, final boolean result) {
        this.party = party;
        this.result = result;
    }

    public boolean isPartyStandardised() {
        return result;
    }

    public Party getParty() {
        return party;
    }
}
