package com.tamu;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class Main {
    private static FileWriter writer;

    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss");
        String outputFileName = "Database_Result_" + sdf.format(Calendar.getInstance().getTime()) + ".log";
        Util.createOutputFile(outputFileName);

        Interpreter interpreter = new Interpreter();
//        interpreter.executeFile("multiple relations join.txt");
//        interpreter.executeFile("holes.txt");
//        interpreter.executeFile("select1.txt");
//        interpreter.executeFile("3.txt");
//        interpreter.executeFile("all.txt");
//        interpreter.executeFile("TinySQL_windows_updated.txt");
        mainLoop(interpreter);
    }

    private static void mainLoop(Interpreter interpreter) {
        System.out.println("===== Database Interpreter =====");
        System.out.println("NOTE: All the results will be logged to a file in this directory, using the current time as file name.");


        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("Please select:\n"
                    + "1: Test with file\n"
                    + "2: Test with text input\n"
                    + "Q: Quit\n"
                    + "> ");
            String input = in.nextLine().trim().toUpperCase();

            switch (input) {
                case "1":
                    while (true) {
                        System.out.print("Please input file name (input \"R\" to return): ");
                        String inputFileName = in.nextLine().trim();
                        if (inputFileName.equalsIgnoreCase("R")) {
                            break;
                        } else {
                            if (interpreter.executeFile(inputFileName)) {
                                System.out.println("File executed successfully!");
                            } else {
                                System.out.println("Invalid file name.");
                            }
                        }
                    }
                    break;

                case "2":
                    while (true) {
                        System.out.print("Please input SQL statement (input \"R\" to return): ");
                        String statement = in.nextLine().trim();
                        if (statement.equalsIgnoreCase("R")) {
                            break;
                        } else {
                            if (interpreter.executeStatement(statement)) {
                                System.out.println("SQL statement executed successfully!");
                            } else {
                                System.out.println("Invalid SQL statement.");
                            }
                        }
                    }
                    break;

                case "Q":
                    in.close();
                    try {
                        if (writer != null) {
                            writer.flush();
                            writer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Bye.");
                    return;

                default:
                    System.out.println("Invalid command.");
                    break;
            }
        }
    }
}
