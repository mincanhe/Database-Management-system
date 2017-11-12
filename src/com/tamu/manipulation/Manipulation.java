package com.tamu.manipulation;

import com.tamu.Statement;
import storageManager.Disk;
import storageManager.MainMemory;
import storageManager.SchemaManager;

import java.util.List;

public interface Manipulation {
    boolean execute(Disk disk, MainMemory memory, SchemaManager schemaManager, List<Statement> statements);
}
