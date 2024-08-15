package com.ABC.pioneer.sensor.payload.Crypto;

import java.util.Arrays;

public class HMACSHA256 {
    public static byte [] GenerateMAC(byte [] data, byte [] key) throws Exception
    {
        byte[] mac = new byte[32];
        PioneerHMac pmac = new PioneerHMac(key);
        pmac.generateHMac(data,mac);
        return mac;
    }

    public static boolean VerifyMAC(byte [] data, byte [] key,byte [] Mac_str) throws Exception
    {
        byte [] Gen_Mac_str = null;
        Gen_Mac_str = GenerateMAC(data,key);
        if(Arrays.equals(Mac_str,Gen_Mac_str))
            return true;
        else
            return false;
    }
}

