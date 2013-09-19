package org.deephacks.confit.internal.berkeley;

import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import org.deephacks.confit.model.Bean.BeanId;
import org.deephacks.confit.spi.serialization.UniqueIds;

@Persistent
public class BBeanId {
    @KeyField(1) int schemaName;
    @KeyField(2) long instanceId;

    private BBeanId() {
    }

    public BBeanId(BeanId id, UniqueIds uniqueIds) {
        this.schemaName = (int) uniqueIds.getSchemaIds().getId(id.getSchemaName());
        this.instanceId = (int) uniqueIds.getInstanceIds().getId(id.getInstanceId());
    }

    public BBeanId(String schemaName, String instanceId, UniqueIds uniqueIds) {
        this.schemaName = (int) uniqueIds.getSchemaIds().getId(schemaName);
        this.instanceId = (int) uniqueIds.getInstanceIds().getId(instanceId);
    }

    public BBeanId(int schemaName, long instanceId) {
        this.schemaName = schemaName;
        this.instanceId = instanceId;
    }

    public int getSchemaName() {
        return schemaName;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
