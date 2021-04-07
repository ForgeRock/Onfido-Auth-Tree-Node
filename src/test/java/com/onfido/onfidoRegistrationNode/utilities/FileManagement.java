package com.onfido.onfidoRegistrationNode.utilities;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileManagement {

    public static String readFile(String path) throws IOException {
        InputStream inputStream = null;

        try {
            FileInputStream fis = new FileInputStream(path);
            return IOUtils.toString(fis, "UTF-8");
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
    public static String readFile(Files file) throws IOException {
        return readFile(file.getPath());
    }
}
