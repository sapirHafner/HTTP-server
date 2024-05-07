package httpServer;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    public final String HTTP_NEW_LINE = "\n";

    private String method;
    private String requestedPage;
    private String contentType;
    private boolean isImage;
    private int contentLength;
    private String referer;
    private String userAgent;
    private Map<String, String> parameters;
    private String requestBody;
    private boolean chunked;


    public HTTPRequest(String httpRequestString) throws Exception {
        this.parameters = new HashMap<>();      // Initialize the parameters map

        chunked = false;
        isImage = false;

        parseRequest(httpRequestString);
    }

    private void parseRequest(String httpRequestString) throws Exception {
        // Split the request into lines
        String[] lines = httpRequestString.split(HTTP_NEW_LINE);
        String firstLine = lines[0];

        // Parse the first line to get method and requested page
        String[] requestParts = firstLine.split("\\s+");
        final int HTTP_REQUEST_NUM_OF_ARGS = 3;
        if (requestParts.length != HTTP_REQUEST_NUM_OF_ARGS) {
            throw new IllegalArgumentException("Invalid HTTP request format");
        }

        this.method = requestParts[0];
        if (requestParts[1].contains("?")){                                      //if the requesthas parameters
            String[] containsParameters = requestParts[1].split("\\?");     //saperate the parametres from the filename
            this.requestedPage = containsParameters[0];
            this.parameters = parseParameters(containsParameters[1]);  
        } 
        else {
            this.requestedPage = requestParts[1];                                 //with no prameters
        }

        this.contentType = extractFileExtension(requestedPage);
        this.isImage = checkIfImage(requestedPage);

        // Parse other headers
        int i = 1;
        while (i<lines.length) {
            String headerLine = lines[i];

            if (headerLine == "")
                break;

            if (headerLine.toLowerCase().startsWith("content-length:")) {
                this.contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
            } else if (headerLine.toLowerCase().startsWith("referer:")) {
                this.referer = headerLine.split(":")[1].trim();
            } else if (headerLine.toLowerCase().startsWith("user-agent:")) {
                this.userAgent = headerLine.split(":")[1].trim();
            } else if (headerLine.toLowerCase().startsWith("chunked:")) {
                this.chunked = headerLine.split(":")[1].trim().equals("yes");
            }
            i++;
        }

        StringBuilder bodyStringBuilder = new StringBuilder();
        for (int j=i+1; j<lines.length; j++)
            bodyStringBuilder.append(lines[j]);
        requestBody = bodyStringBuilder.toString();
    }
    
    private boolean checkIfImage(String page) {
        // This is a simple check based on file extensions, modify as needed
        String[] imageExtensions = {".jpg", ".jpeg", ".bmp", ".gif", ".png"};
        for (String extension : imageExtensions) {
            if (page.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private String extractFileExtension(String path) {
        if (path.equals("/")){
            return "text/html";
        }
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";                        // For JPEG images
        } else if (path.endsWith(".png")) {
            return "image/png";                         // For PNG images
        } else if (path.endsWith(".bmp")) {
            return "image/bmp";                         // For BMP images
        } else if (path.endsWith(".gif")) {
            return "image/gif";                         // For GIF images
        } else if (path.endsWith(".ico")) {
            return "image/x-icon";                      // For icon files
        }
        // Default binary content type
        return "application/octet-stream";
    }

    private Map<String, String> parseParameters(String queryString) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8.name());
                value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());

                params.put(key, value);
            }
        }

        return params;
    }


    // Getters for various components
    public String getMethod() {
        return method;
    }

    public String getRequestedPage() {
        return requestedPage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getBodyAsParams() throws Exception
    {
        return parseParameters(requestBody);
    }

    public boolean getChunked()
    {
        return chunked;
    }
}









