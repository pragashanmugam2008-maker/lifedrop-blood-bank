package com.lifedrop.service;

import com.lifedrop.enums.BloodGroup;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BloodCompatibilityService {

    /**
     * Compatibility Map: Key is the DONOR blood group, Value is the set of RECIPIENT blood groups they CAN DONATE TO.
     *
     * Donor   | Can Donate To
     * --------|---------------------------------
     * O-      | O-, O+, A-, A+, B-, B+, AB-, AB+
     * O+      | O+, A+, B+, AB+
     * A-      | A-, A+, AB-, AB+
     * A+      | A+, AB+
     * B-      | B-, B+, AB-, AB+
     * B+      | B+, AB+
     * AB-     | AB-, AB+
     * AB+     | AB+
     */
    private static final Map<BloodGroup, Set<BloodGroup>> DONOR_TO_RECIPIENT_MAP = new EnumMap<>(BloodGroup.class);

    static {
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.O_NEG, EnumSet.allOf(BloodGroup.class));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.O_POS, EnumSet.of(BloodGroup.O_POS, BloodGroup.A_POS, BloodGroup.B_POS, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.A_NEG, EnumSet.of(BloodGroup.A_NEG, BloodGroup.A_POS, BloodGroup.AB_NEG, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.A_POS, EnumSet.of(BloodGroup.A_POS, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.B_NEG, EnumSet.of(BloodGroup.B_NEG, BloodGroup.B_POS, BloodGroup.AB_NEG, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.B_POS, EnumSet.of(BloodGroup.B_POS, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.AB_NEG, EnumSet.of(BloodGroup.AB_NEG, BloodGroup.AB_POS));
        DONOR_TO_RECIPIENT_MAP.put(BloodGroup.AB_POS, EnumSet.of(BloodGroup.AB_POS));
    }

    /**
     * Checks if a donor with donorBloodGroup can donate to a patient/recipient with recipientBloodGroup.
     * First parameter is always the DONOR's blood group.
     * Second parameter is always the RECIPIENT's blood group.
     *
     * @param donorBloodGroup the donor's blood group
     * @param recipientBloodGroup the recipient's blood group
     * @return true if compatible, false otherwise
     */
    public boolean canDonateTo(BloodGroup donorBloodGroup, BloodGroup recipientBloodGroup) {
        if (donorBloodGroup == null || recipientBloodGroup == null) {
            return false;
        }
        Set<BloodGroup> compatibleRecipients = DONOR_TO_RECIPIENT_MAP.get(donorBloodGroup);
        return compatibleRecipients != null && compatibleRecipients.contains(recipientBloodGroup);
    }

    /**
     * Returns the list of donor blood groups that CAN DONATE TO the given recipient.
     *
     * @param recipientBloodGroup recipient's blood group
     * @return list of compatible donor blood groups
     */
    public List<BloodGroup> getCompatibleDonorsFor(BloodGroup recipientBloodGroup) {
        if (recipientBloodGroup == null) {
            return Collections.emptyList();
        }
        List<BloodGroup> compatibleDonors = new ArrayList<>();
        for (BloodGroup candidateDonor : BloodGroup.values()) {
            if (canDonateTo(candidateDonor, recipientBloodGroup)) {
                compatibleDonors.add(candidateDonor);
            }
        }
        return compatibleDonors;
    }

    /**
     * Returns the list of recipient blood groups that CAN RECEIVE FROM the given donor.
     *
     * @param donorBloodGroup donor's blood group
     * @return list of compatible recipient blood groups
     */
    public List<BloodGroup> getCompatibleRecipientsFor(BloodGroup donorBloodGroup) {
        if (donorBloodGroup == null) {
            return Collections.emptyList();
        }
        Set<BloodGroup> recipients = DONOR_TO_RECIPIENT_MAP.get(donorBloodGroup);
        return recipients != null ? new ArrayList<>(recipients) : Collections.emptyList();
    }
}
