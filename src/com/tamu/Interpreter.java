package com.tamu;

import com.tamu.manipulation.*;
import storageManager.Disk;
import storageManager.MainMemory;
import storageManager.SchemaManager;

import java.io.File;
import java.util.List;

class Interpreter {
    private Disk disk;
    private MainMemory memory;
    private SchemaManager schemaManager;
    private Parser parser;

    // for benchmark
    private double DBMSTime;
    private long DBMSIO;
    private long computerTime;

    Interpreter() {
        this.disk = new Disk();
        this.memory = new MainMemory();
        this.schemaManager = new SchemaManager(memory, disk);
        this.parser = new Parser();
    }

    private boolean execute(String statement) {
        Statement statementTree = parser.parse(statement);
        if (statementTree == null) {
            return false;
        }
        String manipulationType = statementTree.getAttribute();

        Manipulation manipulation;
        switch (manipulationType) {
            case "CREATE":
                manipulation = new Create();
                break;
            case "INSERT":
                manipulation = new Insert();
                break;
            case "SELECT":
                manipulation = new Select();
                break;
            case "DELETE":
                manipulation = new Delete();
                break;
            case "DROP":
                manipulation = new Drop();
                break;
            case "INITIAL":
            default:
                // if everything works fine, this line will be never reached
                manipulation = null;
                Util.outputLn("Unknown manipulation type.");
                System.exit(1);
                break;
        }
        return manipulation.execute(disk, memory, schemaManager, statementTree.branches);
    }

    boolean executeStatement(String statement) {
        benchmarkBegin();
        boolean result = execute(statement);
        if (result) {
            benchmarkEnd();
        }

        return result;
    }

    boolean executeFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            List<String> statements = Util.readFile(fileName);

            benchmarkBegin();
            for (String statement : statements) {
                execute(statement);
            }
            benchmarkEnd();

            return true;
        } else {
            return false;
        }
    }

    private void benchmarkBegin() {
        DBMSTime = disk.getDiskTimer();
        DBMSIO = disk.getDiskIOs();
        computerTime = System.currentTimeMillis();
    }

    private void benchmarkEnd() {
        Util.outputLn("DBMS time = " + String.format("%.2f", (disk.getDiskTimer() - DBMSTime)) + " ms");
        Util.outputLn("DBMS Disk I/Os = " + (disk.getDiskIOs() - DBMSIO));
        Util.outputLn("Computer time = " + (System.currentTimeMillis() - computerTime) + " ms");
    }
}
