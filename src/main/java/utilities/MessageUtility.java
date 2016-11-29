package utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import models.BankResponse;
import models.XMLData;

public class MessageUtility {

    public Object deSerializeBody( byte[] body ) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream( body );
        ObjectInput in = new ObjectInputStream( bis );
        return in.readObject();
    }

    public byte[] serializeBody( Object obj ) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream( bos );
        out.writeObject( obj );
        byte[] res = bos.toByteArray();
        out.close();
        bos.close();
        return res;
    }
//
//    public BankResponse convertToLoanResponse( XMLData data ) {
//        int creditScore = data.getCreditScore();
//        double interestRate;
//        if ( creditScore <= 100 ) {
//            interestRate = 12.02301;
//        } else if ( creditScore <= 150 && creditScore > 100 ) {
//            interestRate = 10.20155;
//        } else if ( creditScore <= 200 && creditScore > 150 ) {
//            interestRate = 9.2335;
//        } else {
//            interestRate = 6.32265;
//        }
//        return new BankResponse( data.getSsn(), interestRate );
//    }
}
