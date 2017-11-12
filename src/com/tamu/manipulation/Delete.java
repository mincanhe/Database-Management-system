package com.tamu.manipulation;

import com.tamu.Expression;
import com.tamu.Statement;
import com.tamu.Util;
import storageManager.*;

import java.util.ArrayList;
import java.util.List;

public class Delete implements Manipulation {
    private final static String errorInfo = "Error in a DELETE statement! ";

    public boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements) {
        String relationName = null;
        Statement expression = null;

        for (Statement statement : statements) {
            switch (statement.getAttribute()) {
                case "RELATION":
                    relationName = statement.getLeafAttribute();
                    break;

                case "EXPRESSION":
                    expression = statement;
                    break;
            }
        }

        if (relationName == null) {
            Util.outputErrorLn(errorInfo + "Cannot find relation name.");
            return false;
        }

        Relation relation = schemaManager.getRelation(relationName);
        if (relation == null) {
            Util.outputErrorLn(errorInfo + "Cannot find such relation: \"" + relationName + "\".");
            return false;
        }

        int blockNumber = relation.getNumOfBlocks();
        for (int i = 0; i < blockNumber; i++) {
            boolean blockToDelete = false;

            relation.getBlock(i, 0);
            Block block = memory.getBlock(0);
            ArrayList<Tuple> tuplesInBlock = block.getTuples();

            for (int j = 0; j < tuplesInBlock.size(); j++) {
                Tuple tuple = tuplesInBlock.get(j);
                if (!tuple.isNull() && (expression == null || Expression.evaluateBoolean(expression, tuple))) {
                    block.invalidateTuple(j);
                    blockToDelete = true;
                }
            }

            if (blockToDelete) {
                relation.setBlock(i, 0);
            }
        }

        eliminateHoles(relation, memory);

        Util.outputLn("Successfully executed DELETE from relation \"" + relation.getRelationName() + "\".");
        return true;
    }

    private void eliminateHoles(Relation relation, MainMemory memory) {
        int blockNumber = relation.getNumOfBlocks();
        int capacity = relation.getSchema().getTuplesPerBlock();

        int holeI = 0;
        int fillI = blockNumber - 1;

        relation.getBlock(holeI, 0);
        Block holeBlock = memory.getBlock(0);
        relation.getBlock(fillI, 1);
        Block fillBlock = memory.getBlock(1);

        int holeTupleI = 0;
        int fillTupleI = capacity - 1;

        boolean finishSearch = false;

        while (true) {
            // find next hole
            Tuple holeTuple = null;
            while (true) {
                for (; holeTupleI < capacity; holeTupleI++) {
                    Tuple tuple = holeBlock.getTuple(holeTupleI);
                    if (tuple.isNull()) {
                        holeTuple = tuple;
                        break;
                    }
                }
                if (holeTuple == null) {
                    holeTupleI = 0;
                    holeI++;
                    if (holeI > fillI || holeI > blockNumber || (holeI == fillI && holeTupleI > fillTupleI)) {
                        finishSearch = true;
                        break;
                    }
                    relation.getBlock(holeI, 0);
                    holeBlock = memory.getBlock(0);
                } else {
                    break;
                }
            }

            if (finishSearch) {
                break;
            }

            // find next fill
            Tuple fillTuple = null;
            while (true) {
                for (; -1 < fillTupleI; fillTupleI--) {
                    Tuple tuple = fillBlock.getTuple(fillTupleI);
                    if (!tuple.isNull()) {
                        fillTuple = tuple;
                        break;
                    }
                }
                if (fillTuple == null) {
                    fillTupleI = capacity - 1;
                    fillI--;
                    if (holeI > fillI || fillI < 0 || (holeI == fillI && holeTupleI > fillTupleI)) {
                        finishSearch = true;
                        break;
                    }
                    relation.getBlock(fillI, 1);
                    fillBlock = memory.getBlock(1);
                } else {
                    break;
                }
            }

            if (finishSearch) {
                break;
            }

            if (holeI == fillI) {
                if (holeTupleI > fillTupleI) {
                    for (; holeTupleI < capacity; holeTupleI++) {
                        holeBlock.invalidateTuple(holeTupleI);
                    }
                    break;
                } else {
                    // when holeI == fillI, holeBlock and fillBlock are the same block
                    holeBlock.setTuple(holeTupleI, fillTuple);
                    holeBlock.invalidateTuple(fillTupleI);
                    relation.setBlock(holeI, 0);

                    relation.getBlock(holeI, 0);
                    holeBlock = memory.getBlock(0);
                    relation.getBlock(fillI, 1);
                    fillBlock = memory.getBlock(1);
                }

            } else {
                holeBlock.setTuple(holeTupleI, fillTuple);
                relation.setBlock(holeI, 0);
                fillBlock.invalidateTuple(fillTupleI);
                relation.setBlock(fillI, 1);
            }
        }

        // clear those blocks that are all holes or empties (getNumTuple() == 0)
        // to free space
        for (int i = 0; i < blockNumber; i++) {
            relation.getBlock(i, 0);
            Block block = memory.getBlock(0);
            if (block.getNumTuples() == 0) {
                relation.deleteBlocks(i);
                return;
            }
        }
    }
}
