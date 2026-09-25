package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class CredentialEncryptionServiceTest {

    @Autowired
    private CredentialEncryptionService encryptionService;

    @Test
    void shouldEncryptAndDecryptCredential() {

        String original = "test-slack-secret-123";

        String encrypted =
                encryptionService.encrypt(original);

        String decrypted =
                encryptionService.decrypt(encrypted);

        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }
}
