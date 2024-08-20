package com.dao;

import java.security.SecureRandom;
public class RandomGenerator {
    private static String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static int candidateLength = candidateChars.length();
    public static String GeneratePsuRandomString(int length)
    {
        byte [] randomBytes = GeneratePsuRandomBytes(length);
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<length;i++)
            sb.append(candidateChars.charAt((randomBytes[i] & 0xff) % candidateLength));
        return sb.toString();
    }

    public static byte [] GeneratePsuRandomBytes(int length)
    {
        final SecureRandom secureRandom = getSecureRandom();
        final byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public final static SecureRandom getSecureRandom() {
        try{
            final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            // Generate a secure seed
            final SecureRandom seedSr = new SecureRandom();
            // We need a 440 bit seed - see NIST SP800-90A
            final byte[] seed = seedSr.generateSeed(55);
            sr.setSeed(seed); // seed with random number
            // Securely generate bytes
            sr.nextBytes(new byte[256 + sr.nextInt(1024)]); // start from random position
            return sr;
        }
        catch(Exception e)
        {
            return new SecureRandom();
        }
    }
}