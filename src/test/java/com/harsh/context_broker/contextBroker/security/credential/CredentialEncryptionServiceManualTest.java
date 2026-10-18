package com.harsh.context_broker.contextBroker.security.credential;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialEncryptionServiceManualTest {

    @Test
    void encryptsGivenValueUsingTheAppEncryptionMethod() {
        String base64Key = System.getenv("CREDENTIAL_ENCRYPTION_KEY");

        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "CREDENTIAL_ENCRYPTION_KEY must be set before running this test"
            );
        }

        CredentialEncryptionService service =
            new CredentialEncryptionService(base64Key);

        String value = "4f3636d0fa753340460f285a1ae585f4";
        String encrypted = service.encrypt(value);

        assertThat(encrypted).isNotBlank();
        assertThat(service.decrypt(encrypted)).isEqualTo(value);

        System.out.println(encrypted);
    }
}
