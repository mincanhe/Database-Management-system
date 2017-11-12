package com.tamu;

import java.util.ArrayList;
import java.util.List;

public class Statement {
    private String attribute;
    public List<Statement> branches;

    public Statement() {
        this.branches = new ArrayList<>();
    }

    public Statement(String attribute) {
        this();
        this.attribute = attribute;
    }

    public String getAttribute() {
        return this.attribute;
    }

    public int getSize() {
        return this.branches.size();
    }

    public Statement getLeft() {
        return this.branches.get(0);
    }

    public String getLeftAttribute() {
        return this.getLeft().getAttribute();
    }

    public Statement getRight() {
        return this.branches.get(1);
    }

    public String getRightAttribute() {
        return this.getRight().getAttribute();
    }

    public Statement getLeaf() {
        return this.getLeft();
    }

    public String getLeafAttribute() {
        return this.getLeftAttribute();
    }
}
