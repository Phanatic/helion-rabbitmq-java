/* ============================================================================
 (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to
use, copy, modify, merge,publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
============================================================================ */

package org.hp.samples;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

public class ProcessMessage extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(200);
        PrintWriter writer = response.getWriter();
        writer.println("Here's your message:");

        // Pull out the RABBITMQ_URL environment variable
        String uri = System.getenv("RABBITMQ_URL");

        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(uri);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Create the queue
        channel.queueDeclare("hello", false, false, false, null);

        String routingKey = "thekey";
        String exchangeName = "exchange";

        // Declare an exchange and bind it to the queue
        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueBind("hello", exchangeName, routingKey);

        // Grab the message from the HTML form and publish it to the queue
        String message = request.getParameter("message");
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
        writer.println(" Message sent to queue '" + message + "'");

        boolean autoAck = false;

        // Get the response message
        GetResponse responseMsg = channel.basicGet("hello", autoAck);

        if (responseMsg == null) {
            // No message retrieved.
        } else {
            byte[] body = responseMsg.getBody();
            // Since getBody() returns a byte array, convert to a string for
            // the user.
            String bodyString = new String(body);
            long deliveryTag = responseMsg.getEnvelope().getDeliveryTag();

            writer.println("Message received: " + bodyString);

            // Acknowledge that we received the message so that the queue
            // removes the message so that it's not sent to us again.
            channel.basicAck(deliveryTag, false);
        }

        writer.close();
    }
}
