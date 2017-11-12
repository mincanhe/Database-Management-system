package com.tamu.manipulation;

import com.tamu.Statement;
import com.tamu.Util;
import storageManager.*;

import java.util.ArrayList;
import java.util.List;

public class Create implements Manipulation {
    private final static String errorInfo = "Error in a CREATE statement! ";

    public boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements) {
        String relationName = null;
        List<Statement> fieldStatements = null;

        for (Statement statement : statements) {
            switch (statement.getAttribute()) {
                case "RELATION":
                    relationName = statement.getLeafAttribute();
                    break;

                case "CREATE_COL":
                    fieldStatements = statement.branches;
                    break;
            }
        }

        if (relationName == null) {
            Util.outputErrorLn(errorInfo + "Cannot find relation name.");
            return false;

        } else if (fieldStatements == null) {
            Util.outputErrorLn(errorInfo + "Cannot find fields.");
            return false;
        }

        if (schemaManager.relationExists(relationName)) {
            Util.outputErrorLn(errorInfo + "There is already a relation \"" + relationName + "\".");
            return false;
        }

        ArrayList<String> fieldNames = new ArrayList<>();
        ArrayList<FieldType> fieldTypes = new ArrayList<>();

        for (Statement fieldStatement : fieldStatements) {
            String fieldName = null;
            String fieldTypeStr = null;

            // "CREATE_COL_DETAIL"
            for (Statement field : fieldStatement.branches) {
                switch (field.getAttribute()) {
                    case "COL_ID":
                        fieldName = field.getLeafAttribute();
                        break;
                    case "TYPE":
                        fieldTypeStr = field.getLeafAttribute();
                        break;
                }
            }

            if (fieldName == null) {
                Util.outputErrorLn(errorInfo + "Cannot find one specific field name.");
                return false;
            } else if (fieldTypeStr == null) {
                Util.outputErrorLn(errorInfo + "Cannot find one specific field type.");
                return false;
            } else {
                fieldNames.add(fieldName);
                switch (fieldTypeStr) {
                    case "INT":
                        fieldTypes.add(FieldType.INT);
                        break;
                    case "STR20":
                        fieldTypes.add(FieldType.STR20);
                        break;
                    default:
                        Util.outputErrorLn(errorInfo + "Field type is neither INT nor STR20: \"" + fieldName + "\".");
                        return false;
                }
            }
        }

        Schema schema = new Schema(fieldNames, fieldTypes);
        schemaManager.createRelation(relationName, schema);
        Util.outputLn("Successfully executed CREATE relation \"" + relationName + "\".");
        return true;
    }
}
