package starter;

import bankRabbitMQ.BankRabbitMQ;

public class Starter {

    public static void main( String[] argv ) throws Exception {
        BankRabbitMQ bankRabbitMQ = new BankRabbitMQ();
        bankRabbitMQ.getTranslatorRequest();
    }
}
