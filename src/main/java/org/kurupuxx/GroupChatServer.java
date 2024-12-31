package org.kurupuxx;

import java.io.*;
import java.net.*;
import java.util.*;

public class GroupChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Server is running on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("FILE:")) {
                        handleFile(message.substring(5));
                    } else {
                        broadcast(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Client disconnected: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }

        private void handleFile(String fileName) {
            try {
                byte[] buffer = new byte[4096];
                InputStream fileIn = socket.getInputStream();
                FileOutputStream fileOut = new FileOutputStream("uploads/" + fileName);
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) > 0) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.close();
                broadcast("File received: " + fileName);
            } catch (IOException e) {
                System.err.println("Error handling file: " + e.getMessage());
            }
        }
    }
}