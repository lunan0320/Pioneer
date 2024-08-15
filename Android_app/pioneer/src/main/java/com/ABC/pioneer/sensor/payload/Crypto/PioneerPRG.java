package com.ABC.pioneer.sensor.payload.Crypto;

import java.security.SecureRandom;

public class PioneerPRG {
    Digest digest;
    RandomGenerator prg;

    public PioneerPRG()
    {
        digest = new SM3Digest();
        prg = new DigestRandomGenerator(digest);
    }

    public void generateRandomNumber(byte[] bytes)
    {
        generateRandomNumber(bytes,0,bytes.length);
    }

    public void generateRandomNumber(byte[] bytes, int start, int len)
    {
        final SecureRandom seedSr = new SecureRandom();
        final byte[] seed = seedSr.generateSeed(55);
        prg.addSeedMaterial(seed);
        prg.nextBytes(bytes,start,len);
    }

}
