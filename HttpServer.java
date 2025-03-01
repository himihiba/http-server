import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HttpServer {
    private static final int PORT = 4221;
    private static final String WEB_ROOT = "public";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(() -> handleClient(clientSocket));
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            System.out.println("Request: " + requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0]; 
            String path = parts[1]; 

            if ("/".equals(path)) {
                sendResponse(out, 200, "text/html", "<h1>Welcome to My Java Server</h1>");
            } else {
                File file = new File(WEB_ROOT + path);
                if (file.exists() && !file.isDirectory()) {
                    sendFile(out, file);
                } else {
                    sendResponse(out, 404, "text/html", "<h1>404 Not Found</h1>");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void sendResponse(OutputStream out, int status, String contentType, String body) throws IOException {
        String response = "HTTP/1.1 " + status + " OK\r\n" +
                          "Content-Type: " + contentType + "\r\n" +
                          "Content-Length: " + body.length() + "\r\n" +
                          "\r\n" +
                          body;
        out.write(response.getBytes());
        out.flush();
    }

    private static void sendFile(OutputStream out, File file) throws IOException {
        String contentType = guessContentType(file.getName());
        byte[] fileBytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(fileBytes);
        }

        String header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + fileBytes.length + "\r\n" +
                        "\r\n";
        out.write(header.getBytes());
        out.write(fileBytes);
        out.flush();
    }

    private static String guessContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg")) return "image/jpeg";
        return "text/plain";
    }
}
