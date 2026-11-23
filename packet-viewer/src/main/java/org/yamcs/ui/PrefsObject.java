package org.yamcs.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class PrefsObject {

    public static void putObject(Preferences prefs, String key, Object o) {
        byte[] raw;
        raw = object2Bytes(o);
        prefs.putByteArray(key, raw);
    }

    public static Object getObject(Preferences prefs, String key) {
        byte[] raw = prefs.getByteArray(key, null);
        if (raw == null) {
            return null;
        }
        Object o = null;
        try {
            o = bytes2Object(raw);
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to decode data for " + key + ": " + e.getMessage());
        }
        return o;
    }

    private static byte[] object2Bytes(Object o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Object bytes2Object(byte[] raw) throws ClassNotFoundException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(raw);
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.setObjectInputFilter(info -> {
                if (info.depth() > 3 || info.references() > raw.length || info.arrayLength() > raw.length) {
                    return ObjectInputFilter.Status.REJECTED;
                }
                Class<?> type = info.serialClass();
                if (type == null) {
                    return ObjectInputFilter.Status.UNDECIDED;
                }
                // ArrayList requests an Object[] allocation while reading its elements.
                return (type == ArrayList.class || type == String.class || type == String[].class
                        || type == Object[].class) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            return ois.readObject();
        } catch (IOException e) {
            return null;
        }
    }
}
