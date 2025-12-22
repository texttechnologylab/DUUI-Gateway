package org.texttechnologylab.duui.api.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.xml.sax.SAXException;
import org.apache.uima.cas.impl.XmiCasSerializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;

import org.texttechnologylab.annotation.type.Image;
import org.texttechnologylab.annotation.type.Video;
import org.texttechnologylab.annotation.type.Audio;
import org.texttechnologylab.type.llm.prompt.Prompt;
import org.texttechnologylab.type.llm.prompt.Message;

public final class FileUploadUtils {

    private FileUploadUtils() {}

    public static String readFileAsBase64(String filePath) throws IOException {
        return readFileAsBase64(new File(filePath));
    }

    public static String readFileAsBase64(File file) throws IOException {
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public static String convertFileToBase64(String filePath) throws FileNotFoundException, IOException {
        return convertFileToBase64(new File(filePath));
    }

    public static String convertFileToBase64(File file) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = fis.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    public static String convertPngToBase64(String imagePath) throws IOException {
        return convertPngToBase64(new File(imagePath));
    }

    public static String convertPngToBase64(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);

        // Convert the image to byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Encode the byte array to Base64
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static void saveBase64ToPng(String base64String, String outputPath) throws IOException {
        // Decode the Base64 string into a byte array
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);

        // Create an image from the byte array
        InputStream inputStream = new ByteArrayInputStream(decodedBytes);
        BufferedImage image = ImageIO.read(inputStream);

        // Save the image to the specified output file
        File outputFile = new File(outputPath);
        ImageIO.write(image, "png", outputFile);
    }


    public static void saveBase64ToVideo(String base64String, String outputPath) throws IOException {
        // Decode the Base64 string into a byte array
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);

        // Write the byte array to the specified output MP4 file
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            outputStream.write(decodedBytes);
        }
    }

	public static void addPrompts(JCas cas, String language, String prompt) {
        addPrompts(cas, language, List.of(prompt));
    }

	public static void addPrompts(JCas cas, String language, List<String> prompts) {
        addPrompts(cas, language, prompts, "user");
    }

	public static void addPrompts(JCas cas, String language, List<String> prompts, String role) {
        cas.setDocumentLanguage(language);
        StringBuilder sb = new StringBuilder();

        for (String messageText : prompts) {
            Prompt prompt = new Prompt(cas);

            // Set required `args` field as an empty JSON string
            prompt.setArgs("{}");

            // Create a Message object
            Message message = new Message(cas);
            message.setRole(role); // Or whatever default role
            message.setContent(messageText);
            message.addToIndexes();

            // Link message to prompt
            prompt.setMessages(new FSArray<Message>(cas, 1));
            prompt.setMessages(0, message);
            prompt.addToIndexes();

            sb.append(messageText).append(" ");
        }

        cas.setDocumentText(sb.toString().trim());
    }

    public static void addImages(JCas cas, List<String> base64Images) {
        for (String image : base64Images) {
            Image img = new Image(cas);
            img.setSrc(image);
            img.addToIndexes();
        }
    }

    public static void addImages(JCas cas, String... base64Images) {
        for (String image : base64Images) {
            Image img = new Image(cas);
            img.setSrc(image);
            img.addToIndexes();
        }
    }

	public static void addAudios(JCas cas, List<String> base64Wavs) {
        for (String wav : base64Wavs) {
            Audio audio = new Audio(cas);
            audio.setSrc(wav);
            audio.setMimetype("audio/wav");
            audio.addToIndexes();
        }
    }

	public static void addAudios(JCas cas, String... base64Wavs) {
        for (String wav : base64Wavs) {
            Audio audio = new Audio(cas);
            audio.setSrc(wav);
            audio.setMimetype("audio/wav");
            audio.addToIndexes();
        }
    }

    public static void addVideo(JCas cas, String base64Mp4) {
        if (StringUtils.isBlank(base64Mp4)) return;

        Video videoWrapper = new Video(cas);
        videoWrapper.setMimetype("video/mp4");
        videoWrapper.setSrc(base64Mp4);
        videoWrapper.addToIndexes();
    }

    public static JCas createCas(Collection<Part> parts) throws UIMAException, IOException {
        return createCas(parts, "en");
    }

    public static JCas createCas(Collection<Part> parts, String language) throws UIMAException, IOException {
        List<String> texts = new ArrayList<>();
        List<String> imagesB64 = new ArrayList<>();
        List<String> audiosB64 = new ArrayList<>();
        String videoB64 = null;

        for (Part part : parts) {
            String filename = part.getSubmittedFileName();
            String lower = filename == null ? "" : filename.toLowerCase();

            // prompt texts are files: promt_<i>.txt
            if (lower.startsWith("promt_") && lower.endsWith(".txt")) {
                try (InputStream is = part.getInputStream()) {
                    String txt = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (!txt.isEmpty()) texts.add(txt);
                }
                continue;
            }

            try (InputStream is = part.getInputStream()) {
                String b64 = Base64.getEncoder().encodeToString(is.readAllBytes());

                String ct = part.getContentType() == null ? "" : part.getContentType();
                if (lower.endsWith(".png") || ct.startsWith("image/")) { imagesB64.add(b64); }
                else if (lower.endsWith(".wav") || ct.startsWith("audio/")) { audiosB64.add(b64); } 
                else if (lower.endsWith(".mp4") || ct.startsWith("video/")) { videoB64 = b64; }
            }
        }

        JCas cas = JCasFactory.createJCas();
        FileUploadUtils.addPrompts(cas, language, texts);
        FileUploadUtils.addImages(cas, imagesB64);
        FileUploadUtils.addAudios(cas, audiosB64);
        FileUploadUtils.addVideo(cas, videoB64);
        return cas;
    }

    public static void writeCasAsXmi(JCas cas, Path root, String name) throws IOException, SAXException {
        writeCasAsXmi(cas, Paths.get(root.toString(), name + ".xmi"));
    }

    public static void writeCasAsXmi(JCas cas, Path outputFile) throws IOException, SAXException {
        Path parent = outputFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            XmiCasSerializer.serialize(cas.getCas(), out);
        }
    }
}
