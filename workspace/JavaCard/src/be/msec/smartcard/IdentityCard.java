package be.msec.smartcard;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.Key;
import javacard.security.KeyBuilder;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;

public class IdentityCard extends Applet {
	private final static byte IDENTITY_CARD_CLA =(byte)0x80;
	
	private static final byte VALIDATE_PIN_INS = 0x22;
	private static final byte GET_SERIAL_INS = 0x24;
	private static final byte GIVE_TIME = 0x25;
	private static final byte TIME_UPDATE = 0x26;
	private static final byte AUTHENTICATE_SP = 0x27;
	
	
	private final static byte PIN_TRY_LIMIT =(byte)0x03;
	private final static byte PIN_SIZE =(byte)0x04;
	
	private final static short SW_VERIFICATION_FAILED = 0x6300;
	private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
	private final static short SW_TIME_UPDATE_REQUIRED = 0x6302;
	private final static short SW_TIME_VERIFY_FAILED = 0x6303;
	
	private final static byte[] REQUEST_TIME = new byte[] {0x00, 0x01};
	
	private byte[] serial = new byte[]{0x30, 0x35, 0x37, 0x36, 0x39, 0x30, 0x31, 0x05};
	private OwnerPIN pin;
	private byte[] time = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	private final static byte[] PUBLIC_KEY_G = new byte[] {0x00};
	
	private IdentityCard() {
		/*
		 * During instantiation of the applet, all objects are created.
		 * In this example, this is the 'pin' object.
		 */
		pin = new OwnerPIN(PIN_TRY_LIMIT,PIN_SIZE);
		pin.update(new byte[]{0x01,0x02,0x03,0x04},(short) 0, PIN_SIZE);
		
		/*
		 * This method registers the applet with the JCRE on the card.
		 */
		register();
	}

	/*
	 * This method is called by the JCRE when installing the applet on the card.
	 */
	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new IdentityCard();
	}
	
	/*
	 * If no tries are remaining, the applet refuses selection.
	 * The card can, therefore, no longer be used for identification.
	 */
	public boolean select() {
		if (pin.getTriesRemaining()==0)
			return false;
		return true;
	}

	/*
	 * This method is called when the applet is selected and an APDU arrives.
	 */
	public void process(APDU apdu) throws ISOException {
		//A reference to the buffer, where the APDU data is stored, is retrieved.
		byte[] buffer = apdu.getBuffer();
		
		//If the APDU selects the applet, no further processing is required.
		if(this.selectingApplet())
			return;
		
		//Check whether the indicated class of instructions is compatible with this applet.
		if (buffer[ISO7816.OFFSET_CLA] != IDENTITY_CARD_CLA)ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		//A switch statement is used to select a method depending on the instruction
		switch(buffer[ISO7816.OFFSET_INS]){
		case VALIDATE_PIN_INS:
			validatePIN(apdu);
			break;
		case GET_SERIAL_INS:
			getSerial(apdu);
			break;
		case GIVE_TIME:
			giveTime(apdu);
			break;
			
		case TIME_UPDATE:
			timeUpdate(apdu);
			break;
			
		case AUTHENTICATE_SP:
			authenticateServiceProvider(apdu);
			break;
		//If no matching instructions are found it is indicated in the status word of the response.
		//This can be done by using this method. As an argument a short is given that indicates
		//the type of warning. There are several predefined warnings in the 'ISO7816' class.
		default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	/*
	 * This method is used to authenticate the owner of the card using a PIN code.
	 */
	private void validatePIN(APDU apdu){
		byte[] buffer = apdu.getBuffer();
		//The input data needs to be of length 'PIN_SIZE'.
		//Note that the byte values in the Lc and Le fields represent values between
		//0 and 255. Therefore, if a short representation is required, the following
		//code needs to be used: short Lc = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		if(buffer[ISO7816.OFFSET_LC]==PIN_SIZE){
			//This method is used to copy the incoming data in the APDU buffer.
			apdu.setIncomingAndReceive();
			//Note that the incoming APDU data size may be bigger than the APDU buffer 
			//size and may, therefore, need to be read in portions by the applet. 
			//Most recent smart cards, however, have buffers that can contain the maximum
			//data size. This can be found in the smart card specifications.
			//If the buffer is not large enough, the following method can be used:
			//
			//byte[] buffer = apdu.getBuffer();
			//short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
			//Util.arrayCopy(buffer, START, storage, START, (short)5);
			//short readCount = apdu.setIncomingAndReceive();
			//short i = ISO7816.OFFSET_CDATA;
			//while ( bytesLeft > 0){
			//	Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, storage, i, readCount);
			//	bytesLeft -= readCount;
			//	i+=readCount;
			//	readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
			//}
			if (pin.check(buffer, ISO7816.OFFSET_CDATA,PIN_SIZE)==false)
				ISOException.throwIt(SW_VERIFICATION_FAILED);
			else {
				apdu.setOutgoing();
				apdu.setOutgoingLength((short) 1);
				apdu.sendBytesLong(new byte[]{0x00}, (short)0, (short)1);
			}
		}else ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	}
	
	/*
	 * This method checks whether the user is authenticated and sends
	 * the identity file.
	 */
	private void getSerial(APDU apdu){
		//If the pin is not validated, a response APDU with the
		//'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if(!pin.isValidated())ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else{
			//This sequence of three methods sends the data contained in
			//'identityFile' with offset '0' and length 'identityFile.length'
			//to the host application.
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)serial.length);
			apdu.sendBytesLong(serial,(short)0,(short)serial.length);
		}
	}
	
	// Hello function
	private void giveTime(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		// 86 400 000 -> 24 hour in miliseconds
		if(Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, time, (short) 0, (short) 8) == 1) {
			// Request time update
			 ISOException.throwIt(SW_TIME_UPDATE_REQUIRED);
		}
	}
	
	// If time is outdated
	private void timeUpdate(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		RSAPublicKey PKg = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
		PKg.setExponent(new byte[]{}, (short)0, KeyBuilder.LENGTH_RSA_512);
		PKg.setModulus(new byte[]{}, (short)0, KeyBuilder.LENGTH_RSA_512);
		Signature signature = Signature.getInstance(Signature.ALG_RSA_SHA_ISO9796, false);
		signature.init(PKg, Signature.MODE_VERIFY);
		boolean verify = signature.verify(buffer, ISO7816.OFFSET_CDATA, (short) 8,// time is a long so 8 size
				buffer, (short) (ISO7816.OFFSET_CDATA + 8), (short) 20); // SHA1 results in 160 bit = 20 bytes
		if(verify) {
			// Time is correct, add 24 hours
			add(buffer, (byte) 0x05, 
					new byte[] {0x00,0x00,0x00,0x00,0x05,0x26,0x5C,0x00}, (byte) 0x00,
					time, (byte) 0x00, (byte) 0x08);
		} else {
			ISOException.throwIt(SW_TIME_VERIFY_FAILED);
		}
		
	}
	
	// If time is outdated
	private void authenticateServiceProvider(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
	}
	
	// See https://stackoverflow.com/questions/36518553/javacard-applet-to-subtract-two-hexadecimal-array-of-byte
   public static boolean add(byte[] A, byte AOff, byte[] B, byte BOff, byte[] C, byte COff, byte len) {
        short result = 0;

        for (len = (byte) (len - 1); len >= 0; len--) {
            // add two unsigned bytes and the carry from the
            // previous byte computation.
            result = (short) (getUnsignedByte(A, AOff, len) + getUnsignedByte(B, BOff, len) + result);
            // store the result in byte array C
            C[(byte) (len + COff)] = (byte) result;
            // has a carry?
            if (result > 0x00FF) {
                result = 1;
                result = (short) (result + 0x100);
            } else {
                result = 0;
            }
        }
        //produce overflow in the sum.
        if (result == 1) {
            return false;
        }
        return true;
    }
   
   private static short getUnsignedByte(byte[] A, byte AOff, byte count) {
       return (short) (A[(short) (count + AOff)] & 0x00FF);
   }

   
}
