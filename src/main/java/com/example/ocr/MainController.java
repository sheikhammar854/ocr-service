package com.example.ocr;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.bytedeco.javacpp.BytePointer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.bytedeco.javacpp.lept.*;
import static org.bytedeco.javacpp.tesseract.TessBaseAPI;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;


@Controller
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping(path="/files")
public class MainController {
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ConfigurationRepository configurationRepository;
    @Autowired
    private AttributeRepository attributeRepository;

    @PostMapping(path="")
    public @ResponseBody result uploadfile (@RequestParam MultipartFile file) throws IOException {
        String file_name = file.getOriginalFilename();
        File convFile = new File(file_name);
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();


        TessBaseAPI api = new TessBaseAPI();
        if (api.Init(".", "ENG") != 0) {
            System.err.println("Could not initialize tesseract.");
            System.exit(1);
        }
        if (file_name.endsWith(".pdf")){
            PDDocument document = PDDocument.load(new File(file_name));
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int num=document.getNumberOfPages();
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String fileName = "image-" + page + ".png";
                ImageIO.write(bim, "png", new File(fileName));
            }
            document.close();
            document doc=new document();
            doc.setName(file_name);
            BytePointer text;
            boolean text_not_extractable=false;
            boolean first_page=false;
            boolean amount_found=false;
            attributes data_attribute=new attributes();
            data_attribute.setValue("");
            for(int page=0;page<num;page++){
                String file_input="image-"+page+".png";
                PIX image = pixRead(file_input);
                api.SetImage(image);
                // Get OCR result.
                text=api.GetUTF8Text();
                if (text != null) {
                    String data = text.getString();
                    if (!amount_found) {
                        String lines[] = data.split("\n");
                        String amount = "";
                        for (String line : lines) {
                            String words[] = line.split(" ");
                            int i;
                            for (i=0;i<words.length;i++) {
                                if (amount_found) {
                                    if (words[i].charAt(0)=='$' || (words[i].charAt(0)>='0' && words[i].charAt(0)<='9'))
                                    amount = words[i];
                                    break;
                                }
                                if ((words[i].startsWith("Amount Due") || words[i].startsWith("Total")) && i!=words.length-1) {
//                                    if (i>0){
//                                        if (!words[i-1].startsWith("Sub")){
//                                            amount_found = true;
//                                        }
//                                    }
//                                    else{
//                                        amount_found = true;
//                                    }
                                    amount_found = true;
                                }
                            }
                            if (amount_found) {
                                break;
                            }
                        }
                        attributes amount_attribute=new attributes();
                        amount_attribute.setAttribute("Amount Due");
                        amount_attribute.setValue(amount);
                        amount_attribute.setValue(amount_attribute.getValue()+text.getString());
                        data_attribute.setAttribute("Data");
                        doc.addAttribute(amount_attribute);
                    }
                    data_attribute.setValue(data_attribute.getValue()+data);
                    //configuration part
//                    if (!first_page) {
//                        Iterable<configurations> configs = configurationRepository.findAll();
//                        boolean found = false;
//                        configurations matching_config= new configurations();
//                        for (configurations config : configs) {
//                            if (doc.getData().substring(0, 10).equals(config.getFrom_company())) {
//                                found = true;
//                                matching_config = config;
//                                break;
//                            }
//                        }
//                        if (found) {
//                            doc.setAmount_due(doc.getData().substring(matching_config.getAmount_due_index_start(), matching_config.getAmount_due_index_end()));
//                            first_page = true;
//                        } else {
//                            System.err.println("Please add configuration for this type of file");
//                            break;
//                        }
//                    }
                    System.out.println("Document data added in database");
                }
                else{
                    System.err.println("Error in reading text from image");
                    text_not_extractable=true;
                }
                pixDestroy(image);
                File del_file=new File(file_input);
                del_file.delete();
            }
            if (!text_not_extractable) {
                //if (first_page) {
                doc.addAttribute(data_attribute);
                Set<attributes> attr=doc.getAttributes();
                for (attributes a: attr){
                    a.setDoc(doc);
                }
                documentRepository.save(doc);
                //}
            }
            File del_file=new File(file_name);
            del_file.delete();
        }
        else {
            String file_input = file_name;
            if (file_name.endsWith(".TIF")) {
                BufferedImage tif = ImageIO.read(new File(file_name));
                ImageIO.write(tif, "png", new File("tif.png"));
                file_input = "tif.png";
            }
            PIX image = pixRead(file_input);
            api.SetImage(image);
            boolean amount_found=false;
            // Get OCR result.
            if (api.GetUTF8Text() !=null) {
                document doc = new document();
                String data=api.GetUTF8Text().getString();
                doc.setName(file.getOriginalFilename());
                if (!amount_found) {
                    String lines[] = data.split("\n");
                    String amount = "";
                    for (String line : lines) {
                        String words[] = line.split(" ");
                        int i;
                        for (i=0;i<words.length;i++) {
                            if (amount_found) {
                                if (words[i].charAt(0)=='$' || (words[i].charAt(0)>='0' && words[i].charAt(0)<='9'))
                                    amount = words[i];
                                break;
                            }
                            if ((words[i].startsWith("Amount Due") || words[i].startsWith("Total")) && i!=words.length-1) {
//                                    if (i>0){
//                                        if (!words[i-1].startsWith("Sub")){
//                                            amount_found = true;
//                                        }
//                                    }
//                                    else{
//                                        amount_found = true;
//                                    }
                                amount_found = true;
                            }
                        }
                        if (amount_found) {
                            break;
                        }
                    }
                    attributes amount_attribute=new attributes();
                    amount_attribute.setAttribute("Amount Due");
                    amount_attribute.setValue(amount);
                    //amount_attribute.setValue(amount_attribute.getValue()+text.getString());
                    attributes data_attribute=new attributes();
                    data_attribute.setAttribute("Data");
                    data_attribute.setValue(data_attribute.getValue()+data);
                    doc.addAttribute(amount_attribute);
                    doc.addAttribute(data_attribute);
                }
                //config part
//                Iterable<configurations> configs = configurationRepository.findAll();
//                boolean found = false;
//                configurations matching_config= new configurations();
//                for (configurations config : configs) {
//                    if (doc.getData().substring(0, 10).equals(config.getFrom_company())) {
//                        found = true;
//                        matching_config = config;
//                        break;
//                    }
//                }
//                if (found) {
//                    doc.setAmount_due(doc.getData().substring(matching_config.getAmount_due_index_start(), matching_config.getAmount_due_index_end()));
//                    documentRepository.save(doc);
//                } else {
//                    System.out.print(doc.getData());
//                    //System.err.println("Please add configuration for this type of file");
//                }
                Set<attributes> attr=doc.getAttributes();
                for (attributes a: attr){
                    a.setDoc(doc);
                }
                documentRepository.save(doc);
            } else {
                System.out.print("Error in reading text from image\n");
            }
            pixDestroy(image);
            File del_file=new File(file_name);
            del_file.delete();
            del_file=new File(file_input);
            del_file.delete();
        }
        // Destroy used object and release memory
        api.End();
        return new result("Uploaded");
    }

    @GetMapping(path="")
    public @ResponseBody Iterable<document> getAllDocuments(){
        return documentRepository.findAll();
    }

    @GetMapping(path="/{id}")
    public @ResponseBody Iterable<attributes> getDocumentAttributes(@PathVariable long id){
        return attributeRepository.findByDoc_Id(id);
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }

    @GetMapping(path = "/email")
    public @ResponseBody void getAttachmentsEmail(){
        Properties properties = new Properties();
        String host="pop.gmail.com";
        String port="995";
        String userName="ocrtesting854@gmail.com";
        String password="ubuntu12345";
        // server setting
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", port);

        // SSL setting
        properties.setProperty("mail.pop3.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.pop3.socketFactory.fallback", "false");
        properties.setProperty("mail.pop3.socketFactory.port",
                String.valueOf(port));

        Session session = Session.getDefaultInstance(properties);
        try {
            // connects to the message store
            Store store = session.getStore("pop3");
            store.connect(userName, password);

            // opens the inbox folder
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);

            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();
            if (arrayMessages.length==0){
                System.out.println("NO unread messages found!");
            }
            for (int i = 0; i < arrayMessages.length; i++) {
                Message message = arrayMessages[i];
                Address[] fromAddress = message.getFrom();
                String from = fromAddress[0].toString();
                String subject = message.getSubject();
                String sentDate = message.getSentDate().toString();

                String contentType = message.getContentType();
                String messageContent = "";

                // store attachment file name, separated by comma
                String attachFiles = "";

                if (contentType.contains("multipart")) {
                    // content may contain attachments
                    Multipart multiPart = (Multipart) message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            // this part is attachment
                            String fileName = part.getFileName();
                            attachFiles += fileName + ", ";
                            part.saveFile(fileName);
                        } else {
                            messageContent=getTextFromMimeMultipart((MimeMultipart) part.getContent());
                            boolean to_found=false;
                            boolean copying=false;
                            String messageContentLines[]=messageContent.split("\n");
                            for (int lines=0;lines<messageContentLines.length;lines++){
                                if (copying){
                                    messageContent=messageContent +"\n"+messageContentLines[lines];
                                }
                                else if(to_found){
                                    if (Character.isLetter(messageContentLines[lines].charAt(0))){
                                        messageContent=messageContentLines[lines];
                                        copying=true;
                                    }
                                }
                                else if (messageContentLines[lines].contains("To")){
                                    to_found=true;
                                }
                            }
                        }
                    }

                    if (attachFiles.length() > 1) {
                        attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                    }
                } else if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    Object content = message.getContent();
                    if (content != null) {
                        messageContent = content.toString();
                    }
                }

                // print out details of each message
                System.out.println("Message #" + (i + 1) + ":");
                System.out.println("\t From: " + from);
                System.out.println("\t Subject: " + subject);
                System.out.println("\t Sent Date: " + sentDate);
                System.out.println("\t Message: \n\n" + messageContent);
                System.out.println("\t Attachments: " + attachFiles);
            }

            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider for pop3.");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}