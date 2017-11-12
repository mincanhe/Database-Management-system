package com.tamu;

import storageManager.FieldType;
import storageManager.Schema;
import storageManager.Tuple;

public class Expression {
    private final static String errorInfo = "Error in a expression! ";

    public static boolean evaluateBoolean(Statement statement, Tuple tuple) {
        String type = statement.getAttribute();
        if (type.equalsIgnoreCase("EXPRESSION")) {
            return Expression.evaluateBoolean(statement.getLeaf(), tuple);
        } else {
            Statement left = statement.getLeft();
            Statement right = statement.getRight();
            switch (type) {
                case "OR":
                    return evaluateBoolean(left, tuple) || evaluateBoolean(right, tuple);
                case "AND":
                    return evaluateBoolean(left, tuple) && evaluateBoolean(right, tuple);
                case "=":
                    return evaluateTuple(left, tuple).equals(evaluateTuple(right, tuple));
                case ">":
                    return evaluateInt(left, tuple) > evaluateInt(right, tuple);
                case "<":
                    return evaluateInt(left, tuple) < evaluateInt(right, tuple);
                default:
                    Util.outputLn("Unknown Expression!");
                    return false;
            }
        }
    }

    private static int evaluateInt(Statement statement, Tuple tuple) {
        String type = statement.getAttribute();
        switch (type) {
            case "COL_ID":
                StringBuilder fieldName = new StringBuilder();
                for (Statement name : statement.branches) {
                    fieldName.append(name.getAttribute()).append(".");
                }
                fieldName.deleteCharAt(fieldName.length() - 1);
                String name = fieldName.toString();
                return tuple.getField(name).integer;
            case "INT":
                return Integer.parseInt(statement.getLeafAttribute());
            default:
                Statement left = statement.getLeft();
                Statement right = statement.getRight();
                switch (type) {
                    case "+":
                        return Expression.evaluateInt(left, tuple) + Expression.evaluateInt(right, tuple);
                    case "-":
                        return Expression.evaluateInt(left, tuple) - Expression.evaluateInt(right, tuple);
                    case "*":
                        return Expression.evaluateInt(left, tuple) * Expression.evaluateInt(right, tuple);
                    default:
                        return 0;
                }
        }
    }

    private static TupleValue evaluateTuple(Statement statement, Tuple tuple) {
        TupleValue value = new TupleValue();
        String type = statement.getAttribute();
        switch (type) {
            case "STR20":
                value.fieldType = FieldType.STR20;
                value.str = statement.getLeafAttribute();
                break;

            case "INT":
                value.fieldType = FieldType.INT;
                value.integer = Integer.parseInt(statement.getLeafAttribute());
                break;

            case "COL_ID":
                Schema schema = tuple.getSchema();

                String fieldName;
                if (statement.getSize() == 1) {
                    // exam = 100
                    fieldName = statement.getLeafAttribute();

                } else if (statement.getSize() == 2) {
                    // course.exam = 100
                    String relationName = statement.getLeftAttribute();
                    fieldName = statement.getRightAttribute();
                    if (schema.getFieldOffset(fieldName) == -1) {
                        Util.outputErrorLn(errorInfo + "Relation \"" + relationName + "\" doesn't contain a column \"" + fieldName + "\".");
                        break;
                    }

                } else {
                    Util.outputErrorLn(errorInfo + "Unrecognized expression.");
                    break;
                }

                FieldType fieldType = schema.getFieldType(fieldName);
                value.fieldType = fieldType;

                if (fieldType == FieldType.INT) {
                    value.integer = tuple.getField(fieldName).integer;
                } else {
                    value.str = tuple.getField(fieldName).str;
                }
                break;

            default:
                // arithmetic operations: + - *
                value.fieldType = FieldType.INT;
                value.integer = evaluateInt(statement, tuple);
                break;
        }
        return value;
    }
}
