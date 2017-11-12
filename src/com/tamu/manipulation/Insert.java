package com.tamu.manipulation;

import com.tamu.Statement;
import com.tamu.Util;
import storageManager.*;

import java.util.ArrayList;
import java.util.List;

public class Insert implements Manipulation {
    private final static String errorInfo = "Error in a INSERT statement! ";

    public boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements) {
        String relationName = null;
        List<Statement> fieldStatements = null;
        List<Statement> values = null;
        List<Tuple> tuplesToInsert = null;

        for (Statement statement : statements) {
            switch (statement.getAttribute()) {
                case "RELATION":
                    relationName = statement.getLeafAttribute();
                    break;

                case "COL":
                    fieldStatements = statement.branches;
                    break;

                case "VALUES":
                    // INSERT INTO course(sid, ...) VALUES(1, ...)
                    values = statement.branches;
                    break;

                case "SELECT":
                    // INSERT INTO course(sid, ...) SELECT * FROM course
                    Select select = new Select();
                    select.execute(disk, memory, schemaManager, statement.branches);
                    tuplesToInsert = select.originTuples;
                    break;
            }
        }

        if (relationName == null) {
            Util.outputErrorLn(errorInfo + "Cannot find relation name.");
            return false;

        } else if (fieldStatements == null) {
            Util.outputErrorLn(errorInfo + "Cannot find fieldStatements.");
            return false;

        } else if (values == null && tuplesToInsert == null) {
            Util.outputErrorLn(errorInfo + "Cannot find values or tuples.");
            return false;

        } else if (values != null && tuplesToInsert != null) {
            Util.outputErrorLn(errorInfo + "Find both values and tuples. Please use only one of them.");
            return false;
        }

        Relation relation = schemaManager.getRelation(relationName);
        Schema schema = schemaManager.getSchema(relationName);

        if (values != null) {
            // INSERT INTO course(sid, ...) VALUES(1, ...)
            Tuple newTuple = relation.createTuple();
            for (int i = 0; i < fieldStatements.size(); i++) {
                // not using fieldNames because the order of input may be different
                String fieldName = fieldStatements.get(i).branches.get(0).getAttribute();
                FieldType fieldType = schema.getFieldType(fieldName);
                String value = values.get(i).branches.get(0).getAttribute();

                if (fieldType.equals(FieldType.INT)) {
                    if (value.equalsIgnoreCase("NULL")) {
                        newTuple.setField(fieldName, 0);
                    } else {
                        newTuple.setField(fieldName, Integer.parseInt(value));
                    }
                } else {
                    newTuple.setField(fieldName, value);
                }
            }
            insertTuple(relation, memory, newTuple);

        } else {
            // INSERT INTO course(sid, ...) SELECT * FROM course
            ArrayList<String> fieldNames = schema.getFieldNames();
            for (Tuple tuple : tuplesToInsert) {
                Tuple newTuple = relation.createTuple();

                for (String fieldName : fieldNames) {
                    Field field = tuple.getField(fieldName);
                    FieldType fieldType = schema.getFieldType(fieldName);
                    if (fieldType.equals(FieldType.INT)) {
                        newTuple.setField(fieldName, field.integer);
                    } else {
                        newTuple.setField(fieldName, field.str);
                    }
                }
                insertTuple(relation, memory, newTuple);
            }
        }

        Util.outputLn("Successfully executed INSERT into relation \"" + relation.getRelationName() + "\".");
        return true;
    }

    private void insertTuple(Relation relation, MainMemory memory, Tuple tupleToInsert) {
        int blockNumber = relation.getNumOfBlocks();
        if (blockNumber == 0) {
            // empty relation
            Block block = memory.getBlock(0);
            block.clear();
            block.appendTuple(tupleToInsert);
            relation.setBlock(0, 0);

        } else {
            int capacity = relation.getSchema().getTuplesPerBlock();

            // insert it to last block
            relation.getBlock(blockNumber - 1, 0);
            Block lastBlock = memory.getBlock(0);

            // deal with "hole"s in last block
            int insertPosition = 0;
            int validTuple = lastBlock.getNumTuples();
            if (validTuple == 0) {
                lastBlock.clear();
                lastBlock.appendTuple(tupleToInsert);
                insertPosition = blockNumber - 1;

            } else if (validTuple == capacity) {
                lastBlock.clear();
                lastBlock.appendTuple(tupleToInsert);
                insertPosition = blockNumber;

            } else {
                // it should be like this,
                // but storage manager will produce "error"
                // so let's work around.
//                for (int i = 0; i < capacity; i++) {
//                    Tuple tuple = lastBlock.getTuple(i);
//                    if (tuple.isNull()) {
//                        lastBlock.setTuple(i, tupleToInsert);
//                        insertPosition = blockNumber - 1;
//                        break;
//                    }
//                }

                lastBlock.appendTuple(tupleToInsert);
                insertPosition = blockNumber - 1;
            }
            memory.setBlock(0, lastBlock);
            relation.setBlock(insertPosition, 0);
        }
    }
}
