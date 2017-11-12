package storageManager;

import java.io.Serializable;
import java.util.ArrayList;

/* A tuple equals a record/row in a relation/table. 
 * A tuple contains at most MAX_NUM_OF_FIELDS_IN_RELATION=8 fields. 
 * Each field in a tuple has offset 0,1,2,... respectively, 
 *   according to the defined schema. 
 * You can access a field by its offset or its field name.
 * Usage: Most of cases you access the tuples in main memory,
 *          either through the MainMemory class,
 *          or through both the MainMemory and the Block class.
 *        You can access or change fields of a tuple through here.
 *        If you need to delete a tuple inside a memory block, 
 *          "invalidate" the tuple
 *          by using Tuple::invalidate() or Block::invalidateTuple() .
 *        You are able to get schema of a particular tuple through here.
 */

public class Tuple implements Serializable {
    SchemaManager schemaManager;
    int schemaIndex; // points to the schema of the
    // relation which the tuple belongs to
    private ArrayList<Field> fields; // stores int and string fields

    // DO NOT use the constructor here.
    // Create an empty tuple only through Schema

    // for internal use: returns an invalid tuple
    protected Tuple() {
        this.schemaManager = null;
        this.schemaIndex = -1;
        this.fields = new ArrayList<>();
    }

    protected Tuple(Tuple t) {
        schemaManager = t.schemaManager;
        schemaIndex = t.schemaIndex;
        // fields=(ArrayList<Field>) DeepCopy.copy(t.fields);
        this.fields = new ArrayList<>();
        for (Field field : t.fields) {
            this.fields.add(new Field(field));
        }
    }

    protected Tuple(SchemaManager schemaManager, int schemaIndex) {
        this.schemaManager = schemaManager;
        this.schemaIndex = schemaIndex;
        this.fields = new ArrayList<>();
        if (this.schemaManager != null) {
            Schema schema = schemaManager.schemas[schemaIndex];
            int numberOfFields = schema.getNumOfFields();
            for (int i = 0; i < numberOfFields; i++) {
                Field f = new Field();
                f.type = schema.getFieldType(i);
                fields.add(f);
            }
        }
    }

    // returns true if the tuple is invalid
    public boolean isNull() {
        return fields.size() == 0;
    }

    // returns the schema of the tuple
    public Schema getSchema() {
        return new Schema(schemaManager.schemas[schemaIndex]);
    }

    // returns the number of fields in the tuple
    public int getNumOfFields() {
        Schema schema = schemaManager.schemas[schemaIndex];
        return schema.getNumOfFields();
    }

    // returns the number: tuples per block
    public int getTuplesPerBlock() {
        Schema schema = schemaManager.schemas[schemaIndex];
        return schema.getTuplesPerBlock();
    }

    // invalidates the tuple
    void invalidate() {
        fields.clear();
    }

    // returns false if the type is wrong or out of bound
    public boolean setField(int offset, String s) {
        Schema schema = schemaManager.schemas[schemaIndex];
        if (offset >= schema.getNumOfFields() || offset < 0) {
            System.err.print("setField ERROR: offset " + offset + " is out of bound!\n");
            return false;
        } else if (schema.getFieldType(offset) != FieldType.STR20) {
            System.err.print("setField ERROR: field type not FieldType.STR20!\n");
            return false;
        } else {
            fields.get(offset).str = s;
        }
        return true;
    }

    // returns false if the type is wrong or out of bound
    public boolean setField(int offset, int i) {
        Schema schema = schemaManager.schemas[schemaIndex];
        if (offset >= schema.getNumOfFields() || offset < 0) {
            System.err.print("setField ERROR: offset " + offset + " is out of bound!\n");
            return false;
        } else if (schema.getFieldType(offset) != FieldType.INT) {
            System.err.print("setField ERROR: field type not FieldType.INT!\n");
            return false;
        } else {
            fields.get(offset).integer = i;
        }
        return true;
    }

    // returns false if the type is wrong or the name is not found
    public boolean setField(String field_name, String s) {
        Schema schema = schemaManager.schemas[schemaIndex];
        if (!schema.fieldNameExists(field_name)) {
            System.err.print("setField ERROR: field name " + field_name + " not found\n");
            return false;
        }
        int offset = schema.getFieldOffset(field_name);
        if (schema.getFieldType(offset) != FieldType.STR20) {
            System.err.print("setField ERROR: field type not FieldType.STR20!\n");
            return false;
        } else {
            fields.get(offset).str = s;
        }
        return true;
    }

    // returns false if the type is wrong or the name is not found
    public boolean setField(String field_name, int i) {
        Schema schema = schemaManager.schemas[schemaIndex];
        if (!schema.fieldNameExists(field_name)) {
            System.err.print("setField ERROR: field name " + field_name + " not found\n");
            return false;
        }
        int offset = schema.getFieldOffset(field_name);
        if (schema.getFieldType(offset) != FieldType.INT) {
            System.err.print("setField ERROR: field type not FieldType.INT!\n");
            return false;
        } else {
            fields.get(offset).integer = i;
        }
        return true;
    }

    // returns default field if out of bound
    public Field getField(int offset) {
        if (offset < fields.size() && offset >= 0) {
            return new Field(fields.get(offset));
        } else {
            System.err.print("getField ERROR: offset " + offset + " is out of bound!\n");
            return new Field();
        }
    }

    // returns default field if out of bound
    public Field getField(String field_name) {
        Schema schema = schemaManager.schemas[schemaIndex];
        int offset = schema.getFieldOffset(field_name);
        if (offset < fields.size() && offset >= 0) {
            return new Field(fields.get(offset));
        } else {
            System.err.print("getField ERROR: offset " + offset + " is out of bound!\n");
            return new Field();
        }
    }

    public String toString(boolean print_field_names) {
        StringBuilder sb = new StringBuilder();
        Schema schema = schemaManager.schemas[schemaIndex];
        if (print_field_names) {
            sb.append(schema.fieldNamesToString()).append("\n");
        }
        for (Field field : fields) {
            sb.append(field).append("\t");
        }
        return sb.toString();
    }

    public String toString() {
        return toString(false);
    }
}
