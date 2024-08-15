package com.ABC.pioneer.sensor.payload.Crypto;

public class PioneerHMac {
    Digest digest;
    Mac hmac;

    public PioneerHMac(byte[] key)
    {
        digest = new SM3Digest();
        hmac = new HMac(digest);
        hmac.init(new KeyParameter(key));
    }

    public void generateHMac(byte[] message,byte[] mac)
    {
        generateHMac(message,0,message.length,mac,0);
    }

    public void generateHMac(byte[] message,int inoff,int len,byte[] mac,int outoff)
    {
        hmac.update(message,inoff,len);
        hmac.doFinal(mac,outoff);
    }
}
