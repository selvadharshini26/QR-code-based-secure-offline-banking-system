package com.bank.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for QR payloads.
 * In production: load key from environment / KMS.
 */
public class CryptoService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;
    private static final int    GCM_TAG    = 128;

    // 32-byte key for AES-256 (demo: fixed; production: load from env)
    private static final byte[] KEY_BYTES = "SecureBankQR256BitKey12345678901".getBytes();

    private final SecretKey secretKey;
    private final SecureRandom rng = new SecureRandom();

    public CryptoService() {
        this.secretKey = new SecretKeySpec(KEY_BYTES, "AES");
    }

    /** Encrypt plaintext → Base64(IV || ciphertext) */
    public String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LEN];
        rng.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));

        byte[] ct  = cipher.doFinal(plaintext.getBytes("UTF-8"));
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);

        return Base64.getEncoder().encodeToString(out);
    }

    /** Decrypt Base64(IV || ciphertext) → plaintext */
    public String decrypt(String encoded) throws Exception {
        byte[] data = Base64.getDecoder().decode(encoded);
        byte[] iv   = new byte[GCM_IV_LEN];
        byte[] ct   = new byte[data.length - GCM_IV_LEN];
        System.arraycopy(data, 0, iv, 0, GCM_IV_LEN);
        System.arraycopy(data, GCM_IV_LEN, ct, 0, ct.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));
        return new String(cipher.doFinal(ct), "UTF-8");
    }
}
