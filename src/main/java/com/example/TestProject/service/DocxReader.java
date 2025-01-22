package com.example.TestProject.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DocxReader {
    public static String readDocx(Path filePath) throws IOException {
        StringBuilder content = new StringBuilder();

        // Чтение файла .docx
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(filePath))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            if (paragraphs.isEmpty()) {
                return "Файл не содержит текста.";
            }

            for (XWPFParagraph paragraph : paragraphs) {
                content.append(paragraph.getText()).append("\n");
            }
        } catch (Exception e) {
            throw new IOException("Ошибка чтения файла DOCX: " + e.getMessage(), e);
        }

        return content.toString();
    }
}
