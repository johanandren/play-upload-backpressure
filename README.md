# File upload with backpressure

A small example of how file upload with back pressure could be implemented 
with play framework 2.3 and web sockets.


## How it works
When the user has selected a file and pressed upload a websocket connection
to the server is made. The websocket connection has a protocol that does
not allow the client to push a chunk of data unless the server has sent an
ack (the text string "next") that it accepts more data.

On the server side this is managed by an actor that changes between two
states/receive-blocks, it is either open and waiting for a chunk of data
or closed. If the client sends a chunk when it is closed the server will
close the connection.

This way the server has got control over at what rate the clients will
be allowed to push data into it.
