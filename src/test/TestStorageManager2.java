package test;

import storageManager.*;

import java.util.ArrayList;

public class TestStorageManager2 {
    private static void testStorageManager() {
        MainMemory memory = new MainMemory();
        Disk disk = new Disk();
        SchemaManager schemaManager = new SchemaManager(memory, disk);

        // create a relation: students(id:int, name:string)
        ArrayList<String> fieldNames = new ArrayList<>();
        fieldNames.add("id");
        fieldNames.add("name");

        ArrayList<FieldType> fieldTypes = new ArrayList<>(2);
        fieldTypes.add(FieldType.INT);
        fieldTypes.add(FieldType.STR20);

        Schema studentsSchema = new Schema(fieldNames, fieldTypes);
        Relation students = schemaManager.createRelation("students",
                studentsSchema);

        int memoryBlockIndex = 7;
        Block storeBlock = memory.getBlock(memoryBlockIndex);

        // create a tuple
        Tuple tuple1 = students.createTuple();
        tuple1.setField("id", 1);
        tuple1.setField("name", "zheng kai");
        storeBlock.appendTuple(tuple1);

        Tuple tuple2 = students.createTuple();
        tuple2.setField("id", 2);
        tuple2.setField("name", "zhou ling");
        storeBlock.appendTuple(tuple2);

        // store a tuple
        int relationBlockIndex = 0;
        students.setBlock(relationBlockIndex, memoryBlockIndex);

        // browse a relation
        students.getBlock(relationBlockIndex, memoryBlockIndex);
        Block browseBlock = memory.getBlock(memoryBlockIndex);
        ArrayList<Tuple> browseTuples = browseBlock.getTuples();
        for (Tuple tuple : browseTuples) {
            System.out.println("Tuple begins:");
            System.out.println(tuple.getField("id"));
            System.out.println(tuple.getField("name"));
            System.out.println("Tuple ends.");
        }
    }
}
