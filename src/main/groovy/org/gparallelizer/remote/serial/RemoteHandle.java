package org.gparallelizer.remote.serial;

import org.gparallelizer.remote.RemoteHost;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.WriteAbortedException;
import java.util.UUID;

/**
* @author Alex Tkachman
*/
public class RemoteHandle implements Serializable {
    private final UUID serialId;

    private final Class klazz;

    public RemoteHandle(UUID id, Class klazz) {
        this.serialId = id;
        this.klazz = klazz;
    }

    protected Object readResolve () throws ObjectStreamException {
        try {
            WithSerialId obj = (WithSerialId) klazz.newInstance();
            obj.initDeserial(serialId);
            return obj;
        } catch (Exception t) {
            throw new WriteAbortedException(t.getMessage(), t);
        }
    }
}
