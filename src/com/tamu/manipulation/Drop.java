package com.tamu.manipulation;

import com.tamu.Statement;
import com.tamu.Util;
import storageManager.Disk;
import storageManager.MainMemory;
import storageManager.Relation;
import storageManager.SchemaManager;

import java.util.List;

public class Drop implements Manipulation {
    private final static String errorInfo = "Error in a DROP statement! ";

    public boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements) {
        String relationName = null;

        for (Statement statement : statements) {
            switch (statement.getAttribute()) {
                case "RELATION":
                    relationName = statement.getLeafAttribute();
                    break;
            }
        }

        if (relationName == null) {
            Util.outputErrorLn(errorInfo + "Cannot find relation name.");
            return false;
        }

        return execute(schemaManager, relationName);
    }

    boolean execute(SchemaManager schemaManager, String relationName) {
        Relation relation = schemaManager.getRelation(relationName);
        if (relation == null) {
            Util.outputErrorLn(errorInfo + "Cannot find such relation: \"" + relationName + "\".");
            return false;

        } else {
            relation.deleteBlocks(0);
            schemaManager.deleteRelation(relationName);
            Util.outputLn("Successfully executed DROP relation \"" + relationName + "\".");
            return true;
        }
    }
}
