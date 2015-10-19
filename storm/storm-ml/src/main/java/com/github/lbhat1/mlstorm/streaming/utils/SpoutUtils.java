package com.github.lbhat1.mlstorm.streaming.utils;

/**
 * User: lbhat <laksh85@gmail.com>
 * Date: 12/17/13
 * Time: 6:51 PM
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.*;

public class SpoutUtils {
    public static void listFilesForFolder(final File folder, Queue<String> listToLoadFiles) {
        assert (folder != null);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, listToLoadFiles);
            } else {
                listToLoadFiles.add(fileEntry.getAbsolutePath());
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        final File folder = new File("G:\\code\\storm\\mlstorm\\res");
        Queue<String> files = new ArrayDeque<String>();
        listFilesForFolder(folder, files);
        System.out.println(pythonDictToJava(files.remove()));
    }

    public static List<Map<String, List<Double>>> pythonDictToJava(String filename) throws FileNotFoundException {
        Type listOfMapOfStringObjectType = new TypeToken<List<Map<String, List<Double>>>>() {
        }.getType();
        Gson gson = new Gson();
        Scanner scanner = new Scanner(new FileInputStream(filename));
        return gson.fromJson(scanner.nextLine(), listOfMapOfStringObjectType);
    }
}
