package com.tamu.manipulation;

import com.tamu.Expression;
import com.tamu.Statement;
import com.tamu.Util;
import storageManager.*;

import java.util.*;

public class Select implements Manipulation {
    public List<Map<String, Field>> tuples;
    List<Tuple> originTuples;

    private final static String errorInfo = "Error in a SELECT statement! ";

    public boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements) {
        List<Statement> columns = null;
        Statement from = null, expression = null, order = null;
        boolean distinct = false;

        boolean singleRelation = true;

        for (Statement statement : statements) {
            switch (statement.getAttribute()) {
                case "COL":
                    if (statement.getLeafAttribute().equals("DISTINCT")) {
                        distinct = true;
                        columns = statement.getLeaf().branches;
                    } else {
                        columns = statement.branches;
                    }
                    break;

                case "FROM":
                    from = statement;
                    singleRelation = from.getSize() == 1;
                    break;

                case "EXPRESSION":
                    expression = statement;
                    break;

                case "ORDER":
                    order = statement;
                    break;
            }
        }

        if (columns == null) {
            Util.outputErrorLn(errorInfo + "Cannot find columns.");
            return false;

        } else if (from == null) {
            Util.outputErrorLn(errorInfo + "Cannot find relation name.");
            return false;
        }

        this.tuples = new ArrayList<>();
        this.originTuples = new ArrayList<>();

        ArrayList<String> relationNames = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        if (from.getSize() == 1) {
            // Select from SINGLE relation.

            // get relation
            String relationName = from.getLeaf().getLeafAttribute();
            Relation relation = schemaManager.getRelation(relationName);
            if (relation == null) {
                Util.outputErrorLn(errorInfo + "No such relation \"" + relationName + "\".");
                return false;
            }

            relationNames.add(relationName);

            Schema schema = schemaManager.getSchema(relationName);

            // get columns
            if (columns.get(0).getLeafAttribute().equals("*")) {
                // SELECT * ...
                for (String fieldName : schema.getFieldNames()) {
                    fieldNames.add(fieldName);
                }

            } else {
                // SELECT sid, homework, ...
                for (Statement column : columns) {
                    String fieldNameInColumn;
                    if (column.getSize() == 1) {
                        // SELECT sid ...
                        fieldNameInColumn = column.getLeafAttribute();

                    } else if (column.getSize() == 2) {
                        // SELECT course.sid ...
                        String relationNameInColumn = column.getLeftAttribute();
                        if (relationNameInColumn.equals(relationName)) {
                            fieldNameInColumn = column.getRightAttribute();
                        } else {
                            Util.outputErrorLn(errorInfo + "Relation name in COL \"" + relationNameInColumn + "\" and relation name in FROM \"" + relationNameInColumn + "\" are not identical.");
                            return false;
                        }

                    } else {
                        Util.outputErrorLn(errorInfo + "Unrecognized COL name.");
                        return false;
                    }

                    if (schema.getFieldOffset(fieldNameInColumn) == -1) {
                        Util.outputErrorLn(errorInfo + "No such column \"" + fieldNameInColumn + "\" in relation \"" + relationName + "\".");
                        return false;
                    } else {

                        fieldNames.add(fieldNameInColumn);
                    }
                }
            }

            int blockNumber = relation.getNumOfBlocks();
            for (int i = 0; i < blockNumber; i++) {
                if (relation.getBlock(i, 0)) {
                    Block block = memory.getBlock(0);
                    ArrayList<Tuple> tuplesInBlock = block.getTuples();

                    for (Tuple tuple : tuplesInBlock) {
                        // deal with WHERE
                        if (!tuple.isNull() && (expression == null || Expression.evaluateBoolean(expression, tuple))) {
                            Map<String, Field> tupleStringMap = new HashMap<>();
                            for (String fieldName : fieldNames) {
                                tupleStringMap.put(fieldName, tuple.getField(fieldName));
                            }
                            this.tuples.add(tupleStringMap);
                            this.originTuples.add(tuple);
                        }
                    }
                }
            }
        } else {
            // Select from MULTIPLE relations.

            // get relations
            for (Statement relationStatement : from.branches) {
                String relationName = relationStatement.getLeafAttribute();
                Relation relation = schemaManager.getRelation(relationName);
                if (relation == null) {
                    Util.outputErrorLn(errorInfo + "No such relation \"" + relationName + "\".");
                    return false;
                }

                relationNames.add(relationName);
            }

            // get columns
            if (columns.get(0).getLeafAttribute().equals("*")) {
                // SELECT * ...
                for (String relationName : relationNames) {
                    Schema schema = schemaManager.getSchema(relationName);
                    List<String> fieldNamesInSchema = schema.getFieldNames();

                    for (String fieldName : fieldNamesInSchema) {
                        fieldNames.add(relationName + "." + fieldName);
                    }
                }

            } else {
                // SELECT sid, course.homework, course2.project ...
                for (Statement column : columns) {
                    String relationNameInColumn;
                    String fieldNameInColumn;
                    if (column.getSize() == 1) {
                        // SELECT sid ...
                        fieldNameInColumn = column.getLeafAttribute();
                        boolean foundColumn = false;
                        for (String relationName : relationNames) {
                            Schema schema = schemaManager.getSchema(relationName);

                            if (schema.getFieldOffset(fieldNameInColumn) != -1) {
                                if (foundColumn) {
                                    Util.outputErrorLn(errorInfo + "There are at least two columns \"" + fieldNameInColumn + "\" in these relations. Please specify relation name for the column.");
                                    return false;

                                } else {
                                    foundColumn = true;
                                    fieldNames.add(relationName + "." + fieldNameInColumn);
                                }
                            }
                        }

                        if (!foundColumn) {
                            Util.outputErrorLn(errorInfo + "Cannot find such column: \"" + fieldNameInColumn + "\" in any relation.");
                            return false;
                        }

                    } else if (column.getSize() == 2) {
                        // SELECT course.sid ...
                        relationNameInColumn = column.getLeftAttribute();

                        boolean foundRelation = false;
                        for (String relationName : relationNames) {
                            if (relationNameInColumn.equals(relationName)) {
                                foundRelation = true;
                                fieldNameInColumn = column.getRightAttribute();

                                if (schemaManager.getSchema(relationName).getFieldOffset(fieldNameInColumn) == -1) {
                                    Util.outputErrorLn(errorInfo + "No such column \"" + fieldNameInColumn + "\" in relation \"" + relationName + "\".");
                                    return false;

                                } else {
                                    fieldNames.add(relationName + "." + fieldNameInColumn);
                                    break;
                                }
                            }
                        }

                        if (!foundRelation) {
                            Util.outputErrorLn(errorInfo + "Cannot find such relation: \"" + relationNameInColumn + "\".");
                            return false;
                        }

                    } else {
                        Util.outputErrorLn(errorInfo + "Unrecognized COL name.");
                        return false;
                    }


                }
            }

            if (relationNames.size() == 2) {
                crossJoin(schemaManager, memory, relationNames);
            } else if (relationNames.size() == 3) {
                // deal with this condition later
                return true;
            } else {
                multipleJoin(schemaManager, memory, relationNames);
            }
        }

        // deal with DISTINCT
        if (distinct) {
            List<Map<String, Field>> distinctTuples = new ArrayList<>();
            List<Tuple> distinctOriginalTuples = new ArrayList<>();
            Set<Integer> distinctTupleSet = new HashSet<>();

            for (int i = 0; i != tuples.size(); i++) {
                Map<String, Field> tuple = tuples.get(i);

                int hashCode = 1;
                for (String entireFieldName : tuple.keySet()) {
                    FieldType fieldType;
                    if (singleRelation) {
                        Schema schema = schemaManager.getSchema(relationNames.get(0));
                        fieldType = schema.getFieldType(entireFieldName);

                    } else {
                        fieldType = getFieldType(schemaManager, entireFieldName);
                    }

                    // pick an arbitrary primitive number to calculate hashcode
                    // here, we choose 10007
                    if (fieldType == FieldType.INT) {
                        hashCode = 10007 * hashCode + tuple.get(entireFieldName).integer;
                    } else {
                        hashCode = 10007 * hashCode + tuple.get(entireFieldName).str.hashCode();
                    }
                }

                if (!distinctTupleSet.contains(hashCode)) {
                    distinctTuples.add(tuple);
                    distinctOriginalTuples.add(originTuples.get(i));
                    distinctTupleSet.add(hashCode);
                }
            }
            this.tuples = distinctTuples;
            this.originTuples = distinctOriginalTuples;
        }

        // deal with ORDER BY
        if (order != null) {
            String orderRel = null;
            String orderCol = null;

            Statement orderColumnInfo = order.getLeaf();
            if (orderColumnInfo.getSize() == 1) {
                // ORDER BY sid ...
                orderRel = null;
                orderCol = orderColumnInfo.getLeafAttribute();

            } else if (orderColumnInfo.getSize() == 2) {
                // ORDER BY course.sid ...
                boolean foundColumn = false;
                for (String relationName : relationNames) {
                    orderRel = orderColumnInfo.getLeafAttribute();

                    if (orderRel.equals(relationName)) {
                        orderCol = orderColumnInfo.getRightAttribute();

                        if (foundColumn) {
                            Util.outputErrorLn(errorInfo + "There are at least two columns \"" + orderCol + "\" in these relations. Please specify relation name for the column.");
                            return false;

                        } else {
                            foundColumn = true;
                        }
                    }
                }

            } else {
                Util.outputErrorLn(errorInfo + "Unrecognized ORDER BY column.");
                return false;
            }

            if (orderCol == null) {
                Util.outputErrorLn(errorInfo + "Unrecognized ORDER BY column.");
                return false;
            }

            FieldType fieldType;

            if (singleRelation) {
                Schema schema = schemaManager.getSchema(relationNames.get(0));
                fieldType = schema.getFieldType(orderCol);

            } else {
                // give it a default value
                fieldType = FieldType.INT;

                boolean foundColumn = false;
                if (orderRel == null) {
                    for (String relationName : relationNames) {
                        if (isColumnInRelation(orderCol, relationName, schemaManager)) {
                            if (foundColumn) {
                                Util.outputErrorLn(errorInfo + "There are at least two columns \"" + orderCol + "\" in these relations. Please specify relation name for the column.");
                                return false;

                            } else {
                                foundColumn = true;
                                fieldType = schemaManager.getSchema(relationName).getFieldType(orderCol);
                            }
                        }
                    }
                } else {
                    if (isColumnInRelation(orderCol, orderRel, schemaManager)) {
                        foundColumn = true;
                        fieldType = schemaManager.getSchema(orderRel).getFieldType(orderCol);
                    }
                }

                if (!foundColumn) {
                    Util.outputErrorLn(errorInfo + "Cannot find such column: \"" + orderCol + "\" in any relation.");
                    return false;
                }
            }

            final String orderColumn;
            if (orderRel == null) {
                orderColumn = orderCol;
            } else {
                orderColumn = orderRel + "." + orderCol;
            }

            // sort using lambda expressions
            if (fieldType == FieldType.INT) {
                this.tuples.sort((tupleMap1, tupleMap2) -> new Integer(tupleMap1.get(orderColumn).integer).compareTo(tupleMap2.get(orderColumn).integer));
                this.originTuples.sort((tuple1, tuple2) ->
                        new Integer(tuple1.getField(orderColumn).integer).compareTo(tuple2.getField(orderColumn).integer));
            } else {
                this.tuples.sort((tupleMap1, tupleMap2) -> tupleMap1.get(orderColumn).str.compareTo(tupleMap2.get(orderColumn).str));
                this.originTuples.sort((tuple1, tuple2) ->
                        tuple1.getField(orderColumn).str.compareTo(tuple2.getField(orderColumn).str));
            }
        }

        Util.output(fieldNames, tuples);
        return true;
    }

    private boolean isColumnInRelation(String column, String relationName, SchemaManager schemaManager) {
        Schema schema = schemaManager.getSchema(relationName);
        return schema.getFieldOffset(column) != -1;
    }

    private FieldType getFieldType(SchemaManager schemaManager, String fieldInfo) {
        String[] parts = fieldInfo.split("\\.");
        String relationName = parts[0];
        String fieldName = parts[1];
        Schema schema = schemaManager.getSchema(relationName);
        return schema.getFieldType(fieldName);
    }

    private void crossJoin(SchemaManager schemaManager, MainMemory memory, ArrayList<String> relationNames) {
        if (relationNames.size() == 2) {
            // two relations join
            String name0 = relationNames.get(0);
            String name1 = relationNames.get(1);

            Relation relation0 = schemaManager.getRelation(name0);
            Relation relation1 = schemaManager.getRelation(name1);

            ArrayList<String> newRelationNames = new ArrayList<>();
            Relation smallerRelation;
            if (relation0.getNumOfBlocks() <= relation1.getNumOfBlocks()) {
                newRelationNames.add(name0);
                newRelationNames.add(name1);
                smallerRelation = relation0;
            } else {
                newRelationNames.add(name1);
                newRelationNames.add(name0);
                smallerRelation = relation1;
            }

            if (smallerRelation.getNumOfBlocks() < memory.getMemorySize() - 1) {
                // the main memory is sufficiently large
                // to hold the smaller relation
                // One-pass
                onePassJoin(schemaManager, memory, newRelationNames);
            } else {
                // the main memory is NOT sufficiently large
                // to hold the smaller relation
                // Two-pass
                nestedJoin(schemaManager, memory, newRelationNames);
            }
        } else {
            onePassJoin(schemaManager, memory, relationNames);
        }
    }

    private void multipleJoin(SchemaManager schemaManager, MainMemory memory, ArrayList<String> relationNames) {
        String tempRelationName = "temp_multiple_join";

        // if the temp relation already exists, drop it
        if (schemaManager.relationExists(tempRelationName)) {
            Drop drop = new Drop();
            drop.execute(schemaManager, tempRelationName);
        }

        // create a temp relation to store join result
        Schema tempRelationSchema = generateSchema(schemaManager, relationNames);
        Relation tempRelation = schemaManager.createRelation(tempRelationName, tempRelationSchema);

        List<Tuple> resultTuples = new ArrayList<>();
        generateNewTupleList(resultTuples, null, 0, relationNames, tempRelation, schemaManager, memory);
        addTupleListToResults(tempRelationSchema, resultTuples);
    }

    // recursive way to deal with cross join of any number of relations
    private void generateNewTupleList(List<Tuple> resultTuples, List<Tuple> newTupleList, int relationOffset, ArrayList<String> relationNames, Relation tempRelation, SchemaManager schemaManager, MainMemory memory) {
        String relationName = relationNames.get(relationOffset);
        Relation relation = schemaManager.getRelation(relationName);

        for (int i = 0; i != relation.getNumOfBlocks(); i++) {
            // put n-th relation's block into n-th block in memory
            relation.getBlock(i, relationOffset);
            Block block = memory.getBlock(relationOffset);

            // it should be like this,
            // but due to the design of storage manager,
            // this will produce a lot of "error"s
            // so let's work around!
//            for (int j = 0; j != numOfTuplesPerBlock; j++) {
//                Tuple tuple = block.getTuple(j);
//                if (!tuple.isNull()) {

            for (int j = 0; j != block.getNumTuples(); j++) {
                Tuple tuple = block.getTuple(j);

                List<Tuple> newTupleListCopy = new ArrayList<>();

                if (relationOffset != 0) {
                    for (Tuple tupleInList : newTupleList) {
                        newTupleListCopy.add(tupleInList);
                    }
                }

                newTupleListCopy.add(tuple);

                if (relationOffset == relationNames.size() - 1) {
                    resultTuples.add(mergeTuples(tempRelation, newTupleListCopy));
                } else {
                    generateNewTupleList(resultTuples, newTupleListCopy, relationOffset + 1, relationNames, tempRelation, schemaManager, memory);
                }
            }
        }
    }

    private void onePassJoin(SchemaManager schemaManager, MainMemory memory, ArrayList<String> relationNames) {
        String smaller = relationNames.get(0);
        String larger = relationNames.get(1);

        Relation smallerRelation = schemaManager.getRelation(smaller);
        Relation largerRelation = schemaManager.getRelation(larger);

        String tempRelationName = "temp_" + smaller + "_cross_" + larger;

        // if the temp relation already exists, drop it
        if (schemaManager.relationExists(tempRelationName)) {
            Drop drop = new Drop();
            drop.execute(schemaManager, tempRelationName);
        }

        // create a temp relation to store join result
        Schema tempRelationSchema = generateSchema(schemaManager, relationNames);
        Relation tempRelation = schemaManager.createRelation(tempRelationName, tempRelationSchema);

        // get all blocks of smaller relation,
        // because the main memory can hold them
        int numOfBlocksInSmallerRelation = smallerRelation.getNumOfBlocks();
        smallerRelation.getBlocks(0, 0, numOfBlocksInSmallerRelation);
        List<Tuple> tuplesInSmallerRelation = memory.getTuples(0, numOfBlocksInSmallerRelation);

        int lastBlockInMemory = memory.getMemorySize() - 1;

        List<Tuple> resultTuples = new ArrayList<>();

        for (int i = 0; i < largerRelation.getNumOfBlocks(); i++) {
            // use last block of memory to hold block in larger relation
            largerRelation.getBlock(i, lastBlockInMemory);
            Block blockL = memory.getBlock(lastBlockInMemory);

            for (Tuple tupleS : tuplesInSmallerRelation) {
                for (Tuple tupleL : blockL.getTuples()) {
                    if (!tupleS.isNull() && !tupleL.isNull()) {
                        addTuplesToList(resultTuples, tempRelation, tupleS, tupleL);
                    }
                }
            }
        }

        addTupleListToResults(tempRelationSchema, resultTuples);
    }

    private void nestedJoin(SchemaManager schemaManager, MainMemory memory, ArrayList<String> relationNames) {
        String rName = relationNames.get(0);
        String sName = relationNames.get(1);

        Relation r = schemaManager.getRelation(rName);
        Relation s = schemaManager.getRelation(sName);

        String tempRelationName = "temp_" + rName + "_cross_" + sName;

        // if the temp relation already exists, drop it
        if (schemaManager.relationExists(tempRelationName)) {
            Drop drop = new Drop();
            drop.execute(schemaManager, tempRelationName);
        }

        // create a temp relation to store join result
        Schema tempRelationSchema = generateSchema(schemaManager, relationNames);
        Relation tempRelation = schemaManager.createRelation(tempRelationName, tempRelationSchema);

        List<Tuple> resultTuples = new ArrayList<>();
        for (int i = 0; i < r.getNumOfBlocks(); i++) {
            r.getBlock(i, 0);
            Block blockR = memory.getBlock(0);

            for (int j = 0; j < s.getNumOfBlocks(); j++) {
                s.getBlock(j, 1);
                Block blockS = memory.getBlock(1);

                for (Tuple tupleR : blockR.getTuples()) {
                    for (Tuple tupleS : blockS.getTuples()) {
                        if (!tupleR.isNull() && !tupleS.isNull()) {
                            addTuplesToList(resultTuples, tempRelation, tupleR, tupleS);
                        }
                    }
                }
            }
        }

        addTupleListToResults(tempRelationSchema, resultTuples);
    }

    private void addTuplesToList(List<Tuple> resultTuples, Relation relation, Tuple tuple1, Tuple tuple2) {
        List<Tuple> newTupleList = new ArrayList<>();
        newTupleList.add(tuple1);
        newTupleList.add(tuple2);
        resultTuples.add(mergeTuples(relation, newTupleList));
    }

    private void addTupleListToResults(Schema schema, List<Tuple> resultTuples) {
        List<String> fieldNames = schema.getFieldNames();

        for (Tuple tuple : resultTuples) {
            Map<String, Field> tupleStringMap = new HashMap<>();
            for (String fieldName : fieldNames) {
                tupleStringMap.put(fieldName, tuple.getField(fieldName));
            }
            this.tuples.add(tupleStringMap);
            this.originTuples.add(tuple);
        }
    }

    private static Schema generateSchema(SchemaManager schemaManager, List<String> relationNames) {
        ArrayList<String> newFieldNames = new ArrayList<>();
        ArrayList<FieldType> newFieldTypes = new ArrayList<>();

        for (String relationName : relationNames) {
            Relation relation = schemaManager.getRelation(relationName);
            Schema schema = relation.getSchema();

            for (String fieldName : schema.getFieldNames()) {
                newFieldNames.add(relationName + '.' + fieldName);
                newFieldTypes.add(schema.getFieldType(fieldName));
            }
        }

        return new Schema(newFieldNames, newFieldTypes);
    }

    private static Tuple mergeTuples(Relation relation, List<Tuple> tupleList) {
        Tuple newTuple = relation.createTuple();
        int newTupleFieldOffset = 0;

        for (Tuple tuple : tupleList) {
            for (int i = 0; i != tuple.getNumOfFields(); i++, newTupleFieldOffset++) {
                Field field = tuple.getField(i);
                if (field.type == FieldType.INT) {
                    newTuple.setField(newTupleFieldOffset, field.integer);
                } else {
                    newTuple.setField(newTupleFieldOffset, field.str);
                }
            }
        }

        return newTuple;
    }
}
