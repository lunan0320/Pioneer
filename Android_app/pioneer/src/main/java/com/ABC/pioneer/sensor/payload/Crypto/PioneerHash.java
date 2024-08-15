package com.ABC.pioneer.sensor.payload.Crypto;

public class PioneerHash {
    Digest digest;

    public PioneerHash()
    {
        digest = new SM3Digest();
    }

    public void generateHash(byte[] message,byte[] hash)
    {
        generateHash(message,0,message.length,hash,0);
    }

    public void generateHash(byte[] message,int inoff,int len,byte[] hash,int outoff)
    {
        digest.update(message,inoff,len);
        digest.doFinal(hash,outoff);
    }
}
