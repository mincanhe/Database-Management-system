package com.tamu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

class Parser {
    Statement parse(String statement) {
        Util.outputLn("\nProcessing: " + statement + " ...");

        statement = statement.replaceAll(";", "").replaceAll(", *", ",").replaceAll(" +", " ").replaceAll(" \\(", "\\(");
        String words[] = statement.split("(,| |(?=.)(?=\\())");
        return parse(words, "INITIAL");
    }

    private Statement parse(String[] words, String type) {
        Statement statement = null;

        switch (type.toUpperCase()) {
            case "INITIAL":
                switch (words[0].toUpperCase()) {
                    case "SELECT":
                    case "CREATE":
                    case "INSERT":
                    case "DELETE":
                        statement = parse(words, words[0]);
                        break;
                    case "DROP":
                        statement = new Statement("DROP");
                        statement.branches.add(generateLeaf(words[2], "RELATION"));
                        break;

                }
                break;

            case "DELETE":
                statement = new Statement(type);
                String relation = words[2];
                statement.branches.add(generateLeaf(relation, "RELATION"));
                if (words.length > 3 && words[3].equalsIgnoreCase("WHERE")) {
                    statement.branches.add(parse(Arrays.copyOfRange(words, 4, words.length), "WHERE"));
                }
                break;

            case "INSERT":
                // INSERT INTO course(sid, ...) VALUES(1, ...)
                int value = -1;
                // INSERT INTO course(sid, ...) SELECT * FROM course
                int select = -1;

                for (int i = 0; i != words.length; i++) {
                    if (words[i].equals("VALUES")) {
                        value = i;
                    } else if (words[i].equals("SELECT")) {
                        select = i;
                    }
                }

                if (value == -1 && select == -1) {
                    Util.outputErrorLn("Error in an INSERT statement! Cannot find VALUES or SELECT.");
                } else if (value != -1 && select != -1) {
                    Util.outputErrorLn("Error in an INSERT statement! Found both VALUES and SELECT.");
                } else {
                    int position = value == -1 ? select : value;
                    statement = new Statement(type);
                    statement.branches.add(generateLeaf(words[2], "RELATION"));
                    statement.branches.add(parse(Arrays.copyOfRange(words, 3, position), "COL"));

                    if (value != -1) {
                        statement.branches.add(parse(Arrays.copyOfRange(words, value + 1, words.length), "VALUES"));
                    } else {
                        statement.branches.add(parse(Arrays.copyOfRange(words, select, words.length), "VALUES"));
                    }
                }
                break;

            case "VALUES":
                if (words[0].equalsIgnoreCase("SELECT")) {
                    statement = parse(words, "SELECT");
                } else {
                    statement = new Statement(type);
                    for (String word : words) {
                        word = Util.trim(word);
                        statement.branches.add(generateLeaf(word, "VALUE"));
                    }
                }
                break;

            case "DROP":
                statement = new Statement(type);
                statement.branches.add(generateLeaf(words[2], "RELATION"));
                break;

            case "SELECT":
                statement = new Statement(type);
                int from = -1, where = -1, order = -1;
                for (int i = 1; i < words.length; i++) {
                    if (words[i].equalsIgnoreCase("FROM")) {
                        from = i;
                    } else if (words[i].equalsIgnoreCase("WHERE")) {
                        where = i;
                    } else if (words[i].equalsIgnoreCase("ORDER")) {
                        order = i;
                    }
                }

                if (from != -1) {
                    statement.branches.add(parse(Arrays.copyOfRange(words, 1, from), "COL"));
                }

                if (where != -1) {
                    statement.branches.add(parse(Arrays.copyOfRange(words, from + 1, where), "FROM"));
                    statement.branches.add(parse(Arrays.copyOfRange(words, where + 1, order > 0 ? order : words.length), "WHERE"));
                } else {
                    statement.branches.add(parse(Arrays.copyOfRange(words, from + 1, order > 0 ? order : words.length), "FROM"));
                }

                if (order != -1) {
                    statement.branches.add(parse(Arrays.copyOfRange(words, order + 2, words.length), "ORDER"));
                }
                break;

            case "ORDER":
                statement = new Statement(type);
                statement.branches.add(generateLeaf(words[0], "COL_ID"));
                break;

            case "COL":
                statement = new Statement(type);
                if (words[0].equalsIgnoreCase("DISTINCT")) {
                    statement.branches.add(new Statement("DISTINCT"));
                    Statement col = statement.getLeaf();
                    for (int i = 1; i < words.length; i++) {
                        String s = words[i];
                        if (s.length() > 0) {
                            col.branches.add(generateLeaf(s.charAt(s.length() - 1) == ',' ? s.substring(0, s.length() - 1) : s, "COL_ID"));
                        }
                    }
                } else {
                    for (String word : words) {
                        word = word.replaceAll("[()]", "");
                        statement.branches.add(generateLeaf(word, "COL_ID"));
                    }
                }
                break;

            case "FROM":
                statement = new Statement(type);
                for (String word : words) {
                    statement.branches.add(generateLeaf(word, "RELATION"));
                }
                break;

            case "WHERE":
                statement = new Statement("EXPRESSION");
                statement.branches.add(generateExpression(words));
                break;

            case "CREATE":
                statement = new Statement(type);
                statement.branches.add(generateLeaf(words[2], "RELATION"));
                statement.branches.add(parse(Arrays.copyOfRange(words, 3, words.length), "CREATE_COL"));
                break;

            case "CREATE_COL":
                statement = new Statement(type);
                for (int i = 0; i < words.length / 2; i++) {
                    statement.branches.add(parse(Arrays.copyOfRange(words, 2 * i, 2 * i + 2), "CREATE_COL_DETAIL"));
                }
                break;

            case "CREATE_COL_DETAIL":
                statement = new Statement(type);
                String fieldName = words[0].replaceAll("\\(", "");
                String fieldType = words[1].replaceAll("\\)", "");

                statement.branches.add(generateLeaf(fieldName, "COL_ID"));
                statement.branches.add(generateLeaf(fieldType, "TYPE"));
                break;

            default:
                break;
        }

        return statement;
    }

    private static HashMap<String, Integer> priority;

    static {
        priority = new HashMap<>();
        priority.put("OR", 0);
        priority.put("AND", 1);
        priority.put("=", 2);
        priority.put(">", 2);
        priority.put("<", 2);
        priority.put("+", 3);
        priority.put("-", 3);
        priority.put("*", 4);
    }

    private Statement generateExpression(String[] tokens) {
        Stack<Statement> stack = new Stack<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (priority.containsKey(token)) {
                if (stack.size() >= 3) {
                    Statement last = stack.pop();
                    if (priority.get(token) >= priority.get(stack.peek().getAttribute())) {
                        stack.push(last);
                        stack.push(new Statement(token));
                    } else {
                        while (stack.size() > 0 && priority.get(stack.peek().getAttribute()) > priority.get(token)) {
                            Statement operator = stack.pop();
                            Statement anotherOperand = stack.pop();
                            operator.branches.add(anotherOperand);
                            operator.branches.add(last);
                            last = operator;
                        }
                        stack.push(last);
                        stack.push(new Statement(token));
                    }
                } else {
                    stack.push(new Statement(token));
                }
            } else if (Util.isInteger(token)) {
                stack.push(generateLeaf(token, "INT"));

            } else if (token.charAt(0) == '"') {
                stack.push(generateLeaf(token.substring(1, token.length() - 1), "STR20"));

            } else if (token.charAt(0) == '(') {
                int start = i;

                for (int bracketCount = 0; i < tokens.length; i++) {
                    String t = tokens[i];
                    if (t.charAt(0) == '(') {
                        bracketCount++;
                    }
                    if (t.charAt(t.length() - 1) == ')') {
                        bracketCount--;
                        if (bracketCount == 0) {
                            break;
                        }
                    }
                }

                String[] tokensInClause = Arrays.copyOfRange(tokens, start, i + 1);

                // eliminate first '(' and last ')'
                tokensInClause[0] = tokensInClause[0].substring(1);
                tokensInClause[tokensInClause.length - 1] = tokensInClause[tokensInClause.length - 1].substring(0, tokensInClause[tokensInClause.length - 1].length() - 1);

                stack.push(generateExpression(tokensInClause));
            } else {
                stack.push(generateLeaf(token, "COL_ID"));
            }
        }

        if (stack.size() >= 3) {
            Statement operand;
            for (operand = stack.pop(); stack.size() >= 2; ) {
                Statement operator = stack.pop();
                operator.branches.add(stack.pop());
                operator.branches.add(operand);
                operand = operator;
            }
            return operand;
        } else {
            return stack.peek();
        }
    }

    private static Statement generateLeaf(String word, String type) {
        Statement statement = new Statement(type);
        String[] parts = word.split("\\.");
        for (String part : parts) {
            statement.branches.add(new Statement(part));
        }
        return statement;
    }
}
