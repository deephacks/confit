package org.deephacks.confit.test.schema;

import org.deephacks.confit.model.AbortRuntimeException;
import org.deephacks.confit.model.Events;
import org.deephacks.confit.model.Schema;
import org.deephacks.confit.model.Schema.SchemaId;
import org.deephacks.confit.model.Schema.SchemaProperty;
import org.deephacks.confit.model.Schema.SchemaPropertyList;
import org.deephacks.confit.spi.SchemaManager;
import org.deephacks.confit.test.FeatureTestsRunner;
import org.deephacks.confit.test.LookupProxy;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(FeatureTestsRunner.class)
public final class SchemaManagerTests {
    protected SchemaManager manager = LookupProxy.lookup(SchemaManager.class);

    @Test
    public void test_add_get_schema() {
        Set<Schema> schemas = generateSchemas(10, 10);
        for (Schema schema : schemas) {
            manager.registerSchema(schema);
            Schema response = manager.getSchema(schema.getName()).get();
            assertThat(schema, equalTo(response));
        }

    }

    @Test
    public void test_get_schemas() {
        Set<Schema> schemas = generateSchemas(10, 10);
        for (Schema schema : schemas) {
            manager.registerSchema(schema);
            Schema response = manager.getSchema(schema.getName()).get();
            assertThat(schema, equalTo(response));
        }

        Map<String, Schema> schemaNames = manager.getSchemas();
        for (Schema s : schemas) {
            assertTrue(schemaNames.containsKey(s.getName()));
        }
    }

    @Test
    public void test_remove_schema() {
        Set<Schema> schemas = generateSchemas(2, 2);
        for (Schema schema : schemas) {
            manager.registerSchema(schema);
            Schema response = manager.getSchema(schema.getName()).get();
            assertThat(schema, equalTo(response));
        }
        Schema s = schemas.iterator().next();
        assertThat(manager.getSchema(s.getName()).get(), is(s));
        manager.removeSchema(s.getName());
        try {
            s = manager.getSchema(s.getName()).get();
        } catch (AbortRuntimeException e) {
            assertThat(e.getEvent().getCode(), is(Events.CFG101));
        }
    }

    public Set<Schema> generateSchemas(int numBeans, int numProps) {
        HashSet<Schema> schemas = new HashSet<Schema>();
        for (int i = 0; i < numBeans; i++) {
            String type = "configType" + i;
            String name = "configName" + i;
            String desc = "configDesc" + i;
            SchemaId id = SchemaId.create("configId" + i, "configDesc" + i, false);
            Schema schema = Schema.create(id, type, name, desc);
            for (int j = 0; j < numProps; j++) {
                String _name = "propName" + j;
                String _fieldName = "propFieldName" + j;
                String _classType = Integer.class.getName();
                String _desc = "propDesc" + j;
                String _defaultValue = "" + j;
                SchemaProperty prop = SchemaProperty.create(_name, _fieldName, _classType, _desc,
                        true, new ArrayList<String>(), _defaultValue, false);
                schema.add(prop);
                _name = "collPropName" + j;
                _fieldName = "collpropFieldName" + j;
                _classType = String.class.getName();
                _desc = "collpropDesc" + j;
                _defaultValue = "collpropDefaultValue" + j;
                List<String> _colDefault = new ArrayList<>();
                _colDefault.add("simple1");
                _colDefault.add("simple2");
                SchemaPropertyList col = SchemaPropertyList.create(_name, _fieldName, _classType,
                        _desc, true, new ArrayList<String>(), _colDefault, _colDefault.getClass()
                                .getName(), false);
                schema.add(col);

            }

            schemas.add(schema);
        }
        return schemas;
    }
}