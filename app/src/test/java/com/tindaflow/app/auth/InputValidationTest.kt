package com.tindaflow.app.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InputValidationTest {
    @Test fun emailValidationRejectsMalformedAddressesAndAllowsTrimmedInput() {
        assertEquals(InputError.REQUIRED, InputValidation.email("  "))
        listOf("person", "a@b", "a@@b.com", "a b@example.com", "a@.com").forEach {
            assertEquals(it, InputError.EMAIL, InputValidation.email(it))
        }
        assertNull(InputValidation.email("  owner+shop@example.com  "))
    }

    @Test fun passwordsAreNotTrimmedAndConfirmationMustMatchExactly() {
        assertEquals(InputError.PASSWORD_LENGTH, InputValidation.password("short"))
        assertNull(InputValidation.password("a secure passphrase"))
        assertEquals(InputError.TOO_LONG, InputValidation.password("a".repeat(129)))
        assertEquals(InputError.PASSWORD_MISMATCH, InputValidation.confirmation("password ", "password"))
        assertEquals(InputError.REQUIRED, InputValidation.confirmation("password", ""))
        assertNull(InputValidation.confirmation("password ", "password "))
        // Existing credentials are not rejected by a new registration length policy.
        assertNull(InputValidation.loginPassword("short"))
        assertEquals(InputError.REQUIRED, InputValidation.loginPassword(""))
    }

    @Test fun businessNameRequiresMeaningfulBoundedInput() {
        assertEquals(InputError.REQUIRED, InputValidation.businessName(" \n "))
        assertEquals(InputError.TOO_LONG, InputValidation.businessName("a".repeat(101)))
        assertNull(InputValidation.businessName(" Tinda Mart "))
    }

    @Test fun absentBackendAlwaysReturnsSetupRequired() = runBlocking {
        assertEquals(AccountActionState.SETUP_REQUIRED, UnconfiguredAccountAvailability.check())
        assertEquals(AccountActionState.SETUP_REQUIRED, UnconfiguredAccountAvailability.check())
    }
}
