package com.tamu;

import storageManager.FieldType;

public class TupleValue {
    FieldType fieldType;
    String str;
    int integer;

    public boolean equals(TupleValue value) {
        if (this.fieldType == value.fieldType) {
            if (this.fieldType == FieldType.INT) {
                return this.integer == value.integer;
            } else {
                return this.str.equals(value.str);
            }
        } else {
            return false;
        }
    }
}
