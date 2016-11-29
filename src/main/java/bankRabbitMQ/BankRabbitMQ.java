package bankRabbitMQ;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import connector.BankRabbitMQConnector;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebParam;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import models.BankResponse;
import models.Data;

public class BankRabbitMQ { //to fix line 56

    private static final BankRabbitMQConnector connector = new BankRabbitMQConnector();
    private static Channel channel;
    private static String queueName;
    private static final String BANKEXCHANGENAME = "cphbusiness.bankRabbitMQ";
    private static final String ROUTING_KEY = "rabbitMQ";
    
    public static void getTranslatorRequest() throws IOException, TimeoutException, InterruptedException, ClassNotFoundException {
        channel = connector.getChannel();
        channel.exchangeDeclare( BANKEXCHANGENAME, "direct" );
        queueName = channel.queueDeclare().getQueue();
        channel.queueBind( queueName, BANKEXCHANGENAME, ROUTING_KEY );

        QueueingConsumer consumer = new QueueingConsumer( channel );
        channel.basicConsume( queueName, true, consumer );
        while ( true ) {
            try {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                byte[] message = delivery.getBody();
                String stringMessage = removeBom( new String( message ) );
                Data inputMessage = unmarshal( stringMessage );
                String replyTo = delivery.getProperties().getReplyTo();
                String corrId = delivery.getProperties().getCorrelationId();
                String bankName = delivery.getProperties().getHeaders().get( "bankName" ).toString();
                int total = ( int ) delivery.getProperties().getHeaders().get( "total" );
                int messageNo = ( int ) delivery.getProperties().getHeaders().get( "messageNo" );
                System.out.println( " [x] Received from the translator '" + inputMessage.toString() + "'" );
                sendInterestRate( inputMessage.getSsn(), inputMessage.getCreditScore(), replyTo, corrId,
                                  bankName, total, messageNo );
            } catch ( JAXBException ex ) {
                Logger.getLogger( BankRabbitMQ.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
    }

    //remove unnecessary characters before xml declaration 
    private static Data unmarshal( String bodyString ) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance( Data.class );
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        StringReader reader = new StringReader( bodyString );
        return ( Data ) unmarshaller.unmarshal( reader );
    }

    private static String removeBom( String bodyString ) {
        String res = bodyString.trim();
        int substringIndex = res.indexOf( "<?xml" );
        if ( substringIndex < 0 ) {
            return res;
        }
        return res.substring( res.indexOf( "<?xml" ) );
    }

    public static String sendInterestRate( String ssn, int creditScore, String replyTo, String corrId, String bankName, int total, int messageNo ) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost( "datdb.cphbusiness.dk" );
            factory.setUsername( "what" );
            factory.setPassword( "what" );
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            double reply = 0;
            if ( creditScore <= 100 ) {
                reply = 12.02301;
            } else if ( creditScore <= 150 && creditScore > 100 ) {
                reply = 10.20155;
            } else if ( creditScore <= 200 && creditScore > 150 ) {
                reply = 9.2335;
            } else {
                reply = 6.32265;
            }

            Map<String, Object> headers = new HashMap();
            headers.put( "bankName", bankName );
            headers.put( "total", total );
            headers.put( "messageNo", messageNo );
            AMQP.BasicProperties prop = propBuilder( corrId, headers );
            BankResponse res = new BankResponse();
            res.setInterestRate( reply );
            res.setSsn( ssn );
            JAXBContext jc = JAXBContext.newInstance( BankResponse.class );
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
            JAXBElement<BankResponse> je2 = new JAXBElement( new QName( "LoanResponse" ), BankResponse.class, res );
            StringWriter sw = new StringWriter();
            marshaller.marshal( je2, sw );
            String xmlString = sw.toString();
            channel.queueDeclare( replyTo, false, false, false, null );
            channel.basicPublish( "", replyTo, prop, xmlString.getBytes() );
            System.out.println( "xml" + xmlString );
            return xmlString;
        } catch ( IOException | JAXBException ex ) {
            Logger.getLogger( BankRabbitMQ.class.getName() ).log( Level.SEVERE, null, ex );
            return (ex.getMessage());
        }
    }

    private static AMQP.BasicProperties propBuilder( String corrId, Map<String, Object> headers ) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.correlationId( corrId );
        builder.headers( headers );
        AMQP.BasicProperties prop = builder.build();
        return prop;
    }
}
