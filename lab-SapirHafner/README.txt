README
Submitted by: Sapir Hafner 209373620, Adi Shimony 322718024
The provided compile.sh and run.sh scripts simplify the compilation and execution processes. 

Short description of the project classes:

HttpServer:
The HttpServer is a Java program designed to create and run a simple HTTP server based on user-defined configurations. 
The server reads its settings from a config.ini file, specifying details such as the listening port, root directory for web content, default page, and the maximum number of concurrent threads. 
Users can customize the configuration to tailor the server to their needs, and the server handles exceptions by printing informative error messages to the console for easy troubleshooting.

ServerListeningThread:
The ServerListeningThread class is a crucial component of the HTTP server, responsible for managing incoming client connections.
It initializes a server socket, sets up a shared thread pool, and continuously listens for client requests. 
Upon accepting a connection, it utilizes the thread pool to delegate the request handling to instances of the ClientHandle class, promoting concurrent processing. 
The design enhances server scalability and resource utilization, ensuring efficient management of multiple simultaneous client interactions.
Additionally, the class incorporates a parameter for specifying the maximum number of threads in the thread pool, enabling flexible configuration of server concurrency based on system requirements.
The class features error handling to address potential exceptions during the server's operation, contributing to the overall reliability of the HTTP server.

ClientHandle:
The ClientHandle class is responsible for handling individual client connections.
It extends the Thread class to enable concurrent processing of multiple client requests.
The class manages input and output streams for communication with clients, reads HTTP requests, and dynamically responds based on the request method (GET, POST, HEAD, TRACE). 
It incorporates robust error handling and response mechanisms, including handling the following HTTP Response code:
o 200 OK
o 404 Not Found
o 501 Not Implemented
o 400 Bad Request
o 500 Internal Server Error 
 
Additionally, the class supports the serving of static files, such as HTML content and images, ensuring a comprehensive and versatile functionality within the HTTP server architecture.


HTTPRequest:
The HTTPRequest class serving as a parser for incoming HTTP requests that are read by the ClientHandle class.
It extracts essential information from the request, such as the request method, requested page, content type, content length, and additional headers like referer and user agent. 
The class intelligently identifies image types based on file extensions and incorporates chunked encoding support. 
Furthermore, it offers convenient methods for retrieving parsed data, including request parameters and the request body. 
This class significantly contributes to the server's ability to interpret and respond effectively to various client requests, ensuring a comprehensive and adaptable functionality within the HTTP server architecture.







