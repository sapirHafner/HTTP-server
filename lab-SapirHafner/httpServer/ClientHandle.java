package httpServer;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

public class ClientHandle extends Thread{
    private Socket socket;
    private String defaultPage;
    private String root;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String requestHeader;
    public final String HTTP_NEW_LINE = "\n";

    public ClientHandle(Socket socket, String defaultPage, String root) {
        this.socket = socket;
        this.defaultPage = defaultPage;
        this.root = root;
    }
    
    public HTTPRequest readHttpRequest(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder httpRequest = new StringBuilder();

        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        httpRequest.append(line).append(HTTP_NEW_LINE);            // Append the first line to the StringBuilder

        while ((line = reader.readLine()) != null) {            // Read the remaining HTTP request headers
            httpRequest.append(line).append(HTTP_NEW_LINE);

            if (line.isEmpty()) {                               // Check if it's the end of the headers
                break;
            }
        }

        requestHeader = httpRequest.toString().trim();

        // Check if it's a POST request with a body
        if ("POST".equalsIgnoreCase(httpRequest.toString().split("\\s+")[0])) {
            int contentLength = getContentLength(httpRequest.toString());
            char[] body = new char[contentLength];
            reader.read(body, 0, contentLength);
            httpRequest.append(body);
        }

        String httpRequestString = httpRequest.toString().trim();

        // Attempt to create an instance of the HTTPRequest class
        try {
            return new HTTPRequest(httpRequestString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int getContentLength(String httpRequest) {
        String[] lines = httpRequest.split(HTTP_NEW_LINE);
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(line.split(":")[1].trim());
            }
        }
        return 0;
    }

    public static String sanitizePath(String requestedPath) {
        return requestedPath.replace("\\..\\", "\\")
            .replace("\\..", "\\")
            .replace("..\\", "\\")
            .replace("/../", "/")
            .replace("/..", "/")
            .replace("../", "/");
    }

    @Override
    public void run() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            HTTPRequest httpRequest = readHttpRequest(inputStream);
            if (httpRequest == null) {
                handleErrorResponse("400 Bad Request");
                return;
            }
            String method = httpRequest.getMethod();

            switch (method) {
                case "GET":
                    handleGet(httpRequest);
                    break;
                case "POST":
                    handlePost(httpRequest);
                    break;
                case "HEAD":
                    handleHead(httpRequest);
                    break;
                case "TRACE":
                    handleTrace(httpRequest);
                    break;
                default:
                    handleErrorResponse("501 Not Implemented");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error while handling http request: " + e.getMessage());
            e.printStackTrace();
            handleErrorResponse("500 Internal Server Error");
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close resources. " + e.getMessage());
            e.printStackTrace();
        }
    }

    
/*---------------------------------------------HANDLE REQUESTS------------------------------------------------ */ 


    public void handleGet(HTTPRequest httpRequest) throws Exception {
        System.out.println("Request headers: "+ HTTP_NEW_LINE + requestHeader + HTTP_NEW_LINE);
        String resource = sanitizePath(httpRequest.getRequestedPage());
        String path = this.root + resource;
        if (resource.equals("/")) {
            path = this.root + "/" + this.defaultPage;
        }
        if(resource.equals("/params_info.html")){
            handlePost(httpRequest);
            return;
        }
        File file = new File(path);
        if (file.exists() && file.isFile()) { // return 200
            byte[] fileContent = Files.readAllBytes(Paths.get(file.getPath()));
            String contentType = httpRequest.getContentType();
            if (httpRequest.getChunked()) {
                sendChunkedResponse(contentType, fileContent);
            }
            else {
                sendOKResponse200(contentType, fileContent);
            }
            
        } else {
            System.err.println("file not found: " + path);
            handleErrorResponse("404 Not Found");
        }
    }

    
    public void handlePost(HTTPRequest httpRequest) throws Exception {  
        try {
            System.out.println("Request headers: "+ HTTP_NEW_LINE + requestHeader + HTTP_NEW_LINE);
            if ("/params_info.html".equals(httpRequest.getRequestedPage())) {
                String filePath = this.root + "/params_info.html";
                File file = new File(filePath);

                Map<String, String> URLParameters = httpRequest.getParameters();
                Map<String, String> parameters = httpRequest.getBodyAsParams();
                Map<String, String> unionParameters = new HashMap<>();

                for (Map.Entry<String, String> entry : parameters.entrySet() ) {
                    unionParameters.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, String> entry : URLParameters.entrySet() ) {
                    unionParameters.put(entry.getKey(), entry.getValue());
                }

                // Read the HTML template from a file
                String htmlTemplate = new String(Files.readAllBytes(Paths.get(file.getPath())));
                String htmlContent;

                if(!unionParameters.isEmpty()){
                    StringBuilder tableRows = new StringBuilder();
                    for (Map.Entry<String, String> entry : unionParameters.entrySet()) {
                        String paramName = entry.getKey();
                        String paramValue = entry.getValue();
    
                        // Append a new table row with parameter name and value
                        tableRows.append("<tr><td>").append(paramName).append("</td><td>").append(paramValue).append("</td></tr>\n");

                    }
                    htmlContent = htmlTemplate.replace("<!--PARAM_ROWS-->", tableRows.toString());
                } else {
                    htmlContent = htmlTemplate.replace("<!--PARAM_ROWS-->", "<tr><td colspan=\"2\">No parameters submitted</td></tr>");
                }
                
                byte[] fileContent = htmlContent.getBytes();

                if (httpRequest.getChunked()) {
                    sendChunkedResponse("text/html", fileContent);
                }
                else {
                    sendOKResponse200("text/html", fileContent);
                }

            } 
            else {
                handleGet(httpRequest);
            }
        } catch (IOException e) {
            handleErrorResponse("500 Internal Server Error");
            System.err.println("Error while handling post request. " + e.getMessage());
            e.printStackTrace();
        }finally {
            closeResources();
        }
    }


    public void handleHead(HTTPRequest httpRequest){ 
        try {
            System.out.println("Request headers: "+ HTTP_NEW_LINE + requestHeader + HTTP_NEW_LINE);
            String resource = httpRequest.getRequestedPage();
            String path = this.root + resource;
            if (resource.equals("/")) {
                path = this.root + "/" + this.defaultPage;
            }
            File file = new File(path);
            if (file.exists() && file.isFile()) {           //return 200
                String contentType = httpRequest.getContentType();
                byte[] fileContent = Files.readAllBytes(Paths.get(file.getPath()));
                String header = "HTTP/1.1 200 OK" + HTTP_NEW_LINE +
                        "Content-Type: " + contentType + HTTP_NEW_LINE +
                        "Content-Length: " + fileContent.length + HTTP_NEW_LINE + HTTP_NEW_LINE;

                System.out.println("Response headers: " + HTTP_NEW_LINE + header);
                try {
                    outputStream.write(header.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    System.err.println("Failed to send header data, " + e.getMessage());
                    e.printStackTrace();
                }    
            } else {
                handleErrorResponse("404 Not Found");
            }
        } catch (IOException e) {
            System.err.println("Failed to send header data. " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }
    
    public void handleTrace(HTTPRequest httpRequest){ 
        try {

            String responseBody = "Version: 1.0"+ HTTP_NEW_LINE +
                "THreadPool threads count: " + activeCount();
            String response = "HTTP/1.1 200 OK"+ HTTP_NEW_LINE +
                    "Content-Type: message/http"+ HTTP_NEW_LINE +
                    "Content-Length: " + responseBody.length() + HTTP_NEW_LINE + HTTP_NEW_LINE; 
    
            System.out.println(response);
            outputStream.write(response.getBytes());
            outputStream.write(responseBody.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Failed handle trace request. " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    } 


  /*---------------------------------------------END HANDLE REQUESTS------------------------------------------------ */
    
      
  /*-----------------------------------------------SENDING RESPONSES------------------------------------------------ */     
     
    private void sendOKResponse200(String contentType, byte[] fileContent){
        String header = "HTTP/1.1 200 OK" + HTTP_NEW_LINE +
                        "Content-Type: " + contentType + HTTP_NEW_LINE +
                        "Content-Length: " + fileContent.length + 
                        HTTP_NEW_LINE + HTTP_NEW_LINE;

        BufferedOutputStream output = new BufferedOutputStream(outputStream);
        System.out.println("Response headers: " + HTTP_NEW_LINE + header);
        try {
            output.write(header.getBytes());
            output.write(fileContent);
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to send 200OK. " + e.getMessage());
            e.printStackTrace();
        }finally {
            closeResources();
        }

    }
     
    private void sendChunkedResponse(String contentType, byte[] fileContent){
        String header = "HTTP/1.1 200 OK" + HTTP_NEW_LINE +
                        "Content-Type: " + contentType + HTTP_NEW_LINE +
                        "Transfer-Encoding: chunked" + 
                        HTTP_NEW_LINE + HTTP_NEW_LINE;

        BufferedOutputStream output = new BufferedOutputStream(outputStream);
        System.out.println(header);
        try {
            output.write(header.getBytes());
            ByteBuffer buffer = ByteBuffer.wrap(fileContent);

            int remainingData = fileContent.length;
            int currentIndex = 0;
            while (remainingData > 0) {
                final int CHUNK_SIZE = 1024;

                int currentLength = 0;
                if (remainingData >= CHUNK_SIZE) {
                    currentLength = CHUNK_SIZE;
                }
                else {
                    currentLength = remainingData;
                }

                output.write(Integer.toHexString(currentLength).getBytes());
                output.write(HTTP_NEW_LINE.getBytes());

                byte[] chunck = new byte[currentLength];
                buffer.get(currentIndex,chunck);
                output.write(chunck);
                output.write(HTTP_NEW_LINE.getBytes());

                remainingData -= currentLength;
                currentIndex += currentLength;
            }

            output.write(("0" + HTTP_NEW_LINE + HTTP_NEW_LINE).getBytes());
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to send chunked response. " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    private void handleErrorResponse(String errorResponse) { 
        try {
            if(socket.isClosed()) {
                return;
            }
            String htmlMessage = "<html><head><title>Error</title></head><body style='text-align:center;font-size:20px;'>"
                + "<h1>Error: " + errorResponse + "</h1></body></html>";

            String response = "HTTP/1.1 " + errorResponse + HTTP_NEW_LINE +
                "Content-Type: text/html" + HTTP_NEW_LINE +
                "Content-Length: " + htmlMessage.length() + HTTP_NEW_LINE + HTTP_NEW_LINE;
            
            System.out.println(response);
            outputStream.write((response + htmlMessage).getBytes());
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Failed to send " + errorResponse + ". " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }   

  /*---------------------------------------------END SENDING RESPONSES------------------------------------------------ */    
}
