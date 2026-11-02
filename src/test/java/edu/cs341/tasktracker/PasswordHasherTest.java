package edu.cs341.tasktracker;

import edu.cs341.tasktracker.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void validPasswordVerifiesAndWrongPasswordFails() {
        PasswordHasher hasher=new PasswordHasher();
        String hash=hasher.hash("examplePass123".toCharArray());
        assertTrue(hasher.verify("examplePass123".toCharArray(),hash));
        assertFalse(hasher.verify("wrongPass123".toCharArray(),hash));
        assertFalse(hasher.verify("examplePass123".toCharArray(),"broken"));
    }
    @Test void randomSaltProducesDistinctHashes() {
        PasswordHasher hasher=new PasswordHasher();
        assertNotEquals(hasher.hash("samePassword".toCharArray()),hasher.hash("samePassword".toCharArray()));
    }
    @Test void malformedEncodingsCannotAuthenticate() {
        PasswordHasher hasher=new PasswordHasher();
        assertFalse(hasher.verify("password123".toCharArray(),null));
        assertFalse(hasher.verify("password123".toCharArray(),"pbkdf2-sha256$1$AA==$AA=="));
        assertFalse(hasher.verify("password123".toCharArray(),"pbkdf2-sha256$120000$invalid$$"));
    }

}
