package com.ABC.pioneer.sensor.payload.Crypto;

public interface Memoable
{
    /**
     * copy this Memoable object to generate a new Memoable object
     * @return the new Memoable object
     */
    Memoable copy();


    /**
     * reset this Memoable object according to the other Memoable object's state
     * @param other the provided Memoable object
     */
    void reset(Memoable other);
}