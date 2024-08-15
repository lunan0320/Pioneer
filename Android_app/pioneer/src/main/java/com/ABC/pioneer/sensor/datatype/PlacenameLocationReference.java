

package com.ABC.pioneer.sensor.datatype;

public class PlacenameLocationReference implements LocationReference {
    public final String name;

    public PlacenameLocationReference(String name) {
        this.name = name;
    }

    public String description() {
        return "PLACE(name=" + name + ")";
    }
}
