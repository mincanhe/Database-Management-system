package com.tamu;

import storageManager.Field;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Util {
    private static FileWriter writer;

    static void createOutputFile(String outputFileName) {
        try {
            writer = new FileWriter(new File(outputFileName));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void output(String content) {
        System.out.print(content);
        outputToFile(content);
    }

    public static void outputLn(String content) {
        System.out.println(content);
        outputToFile(content + "\n");
    }

    public static void outputErrorLn(String content) {
        System.err.print(content);
        outputToFile(content + "\n");
    }

    private static void outputToFile(String content) {
        try {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void output(List<String> fieldNames, List<Map<String, Field>> tuples) {
        StringBuilder titleLineSB = new StringBuilder();
        for (String fieldName : fieldNames) {
            titleLineSB.append(fieldName).append("\t");
        }
        String titleLine = titleLineSB.toString();

        StringBuilder lineSB = new StringBuilder();
        for (int i = 0; i != titleLine.length(); i++) {
            lineSB.append('=');
        }
        String line = lineSB.toString();

        outputLn(line);
        outputLn(titleLine);

        for (Map<String, Field> tuple : tuples) {
            for (String fieldName : fieldNames) {
                output(tuple.get(fieldName) + "\t");
            }
            outputLn("");
        }

        outputLn(line);
    }

    static Boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static String trim(String s) {
        if (s.length() == 0) {
            return null;
        }

        String str = s;
        while (str.charAt(0) == '('
                || str.charAt(0) == '"') {
            str = str.substring(1);
        }
        while (str.charAt(str.length() - 1) == ')'
                || str.charAt(str.length() - 1) == ','
                || str.charAt(str.length() - 1) == '"') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    static List<String> readFile(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            for (String line; (line = reader.readLine()) != null; ) {
                lines.add(line);
            }
            reader.close();
            fileReader.close();
        } catch (IOException e) {
            output("File doesn't exist!\n");
        }
        return lines;
    }
}