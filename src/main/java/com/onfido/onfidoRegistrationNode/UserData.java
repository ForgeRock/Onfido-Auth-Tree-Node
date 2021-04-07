package com.onfido.onfidoRegistrationNode;

import com.iplanet.sso.SSOException;
import com.onfido.models.ExtractedData;
import com.onfido.models.Extraction;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import lombok.Builder;
import lombok.Data;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import java.time.LocalDate;
import java.util.Map;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

@Data
@Builder
public class UserData {
    String documentNumber;
    LocalDate dateOfExpiry;

    // Consider Create/Using another class for this Information
    String firstName;
    String lastName;
    String middleName;
    String fullName;
    LocalDate dateOfBirth;
    String nationality;
    String gender;

    // Consider Creating/Using another class for MRZ
    String mrzLine1;
    String mrzLine2;
    String mrzLine3;

    // Consider Creating/Using another class for Address
    String addressLine1;
    String addressLine2;
    String addressLine3;
    String addressLine4;
    String addressLine5;
    String issuingState;
    String issuingCountry;

    public JsonValue asJsonValueWith(Map<String, String> forgerockToOnfidoKeys) throws NodeProcessException {
        JsonValue attributes = json(object());

        for (Map.Entry<String, String> entry : forgerockToOnfidoKeys.entrySet()) {
            String forgerockKey = entry.getKey();
            String onfidoKey = entry.getValue();

            if (getFieldValue(onfidoKey) != null) {
                attributes.put(forgerockKey, getFieldValue(onfidoKey));
            }
        }

        return attributes;
    }

    private String getFieldValue(String onfidoKey) throws NodeProcessException {
        switch (onfidoKey) {
            case "document_number": return getDocumentNumber();
            case "date_of_expiry": return getDateOfExpiry().toString();
            case "first_name": return getFirstName();
            case "last_name": return getLastName();
            case "middle_name": return getMiddleName();
            case "full_name": return getFullName();
            case "date_of_birth": return getDateOfBirth().toString();
            case "gender": return getGender();
            case "mrz_line1": return getMrzLine1();
            case "mrz_line2": return getMrzLine2();
            case "mrz_line3": return getMrzLine3();
            case "address_line_1": return getAddressLine1();
            case "address_line_2": return getAddressLine2();
            case "address_line_3": return getAddressLine3();
            case "address_line_4": return getAddressLine4();
            case "address_line_5": return getAddressLine5();
            case "issuing_state": return getIssuingState();
            case "issuing_country": return getIssuingCountry();
        }

        throw new NodeProcessException("Could not map onfido json key to data field");
    }

    public static UserData fromOcrExtraction(Extraction extraction) {
        ExtractedData data = extraction.getExtractedData();

        return UserData.builder()
                       .documentNumber(data.getDocumentNumber())
                       .dateOfExpiry(data.getDateOfExpiry())
                       .firstName(data.getFirstName())
                       .middleName(data.getMiddleName())
                       .lastName(data.getLastName())
                       .fullName(data.getFullName())
                       .dateOfBirth(data.getDateOfBirth())
                       .gender(data.getGender())
                       .mrzLine1(data.getMrzLine1())
                       .mrzLine2(data.getMrzLine2())
                       .mrzLine3(data.getMrzLine3())
                       .addressLine1(data.getAddressLine1())
                       .addressLine2(data.getAddressLine2())
                       .addressLine3(data.getAddressLine3())
                       .addressLine4(data.getAddressLine4())
                       .addressLine5(data.getAddressLine5())
                       .issuingState(extraction.getDocumentClassification().getIssuingState())
                       .issuingCountry(extraction.getDocumentClassification().getIssuingCountry())
                       .build();
    }

    public static UserData fromAmIdentityWith(AMIdentity amIdentity, Map<String, String> forgerockToOnfidoKeys) throws NodeProcessException {
        UserData userData;

        try {
            String forgeRockFirstNameAttribute = "cn";
            String forgeRockLastNameAttribute = "sn";

            for (Map.Entry<String, String> entry : forgerockToOnfidoKeys.entrySet()) {
                String onfidoKey = entry.getValue();
                String forgeRockKey = entry.getKey();

                switch(onfidoKey) {
                    case onfidoConstants.FIRST_NAME: forgeRockFirstNameAttribute = forgeRockKey; break;
                    case onfidoConstants.LAST_NAME: forgeRockLastNameAttribute = forgeRockKey; break;
                    default: // Do Nothing
                }
            }

            userData = UserData.builder()
                               .firstName(amIdentity.getAttribute(forgeRockFirstNameAttribute).iterator().next())
                               .lastName(amIdentity.getAttribute(forgeRockLastNameAttribute).iterator().next())
                               .build();

        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException(e);
        }

        return userData;
    }
}
