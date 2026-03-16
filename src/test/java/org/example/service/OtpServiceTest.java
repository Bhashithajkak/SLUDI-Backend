package org.example.service;

import org.example.dto.OTP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGenerateOTP_Success() {
        String did = "did:example:123";
        OTP otp = otpService.generateOTP(did);

        // Assert the returned OTP structure is correct
        assertNotNull(otp);
        assertNotNull(otp.getCode());
        assertEquals(6, otp.getCode().length());

        // Verify that Redis was called correctly to save the OTP
        verify(valueOperations, times(1)).set(
                eq("otp:did:" + did),
                contains(otp.getCode() + ":"),
                eq(5L),
                eq(TimeUnit.MINUTES));
    }

    @Test
    void testVerifyOTP_ValidCode() {
        String did = "did:example:123";
        String validCode = "123456";

        // Simulate future timestamp so it is not expired
        long futureTime = Instant.now().plusSeconds(60).toEpochMilli();
        String storedValue = validCode + ":" + futureTime;

        when(valueOperations.get("otp:did:" + did)).thenReturn(storedValue);

        boolean result = otpService.verifyOTP(did, validCode);

        // Assert verification was successful
        assertTrue(result);

        // Verify that whether successfully deleted the token after usage
        verify(redisTemplate, times(1)).delete("otp:did:" + did);
    }

    @Test
    void testVerifyOTP_ExpiredCode() {
        String did = "did:example:123";
        String validCode = "123456";

        // Simulate past timestamp so it is expired
        long pastTime = Instant.now().minusSeconds(60).toEpochMilli();
        String storedValue = validCode + ":" + pastTime;

        when(valueOperations.get("otp:did:" + did)).thenReturn(storedValue);

        boolean result = otpService.verifyOTP(did, validCode);

        // Assert verification was failed
        assertFalse(result);

        // Ensure invalid tokens are deleted from the system
        verify(redisTemplate, times(1)).delete("otp:did:" + did);
    }
}
