package be.project.middleware;

import java.nio.ByteBuffer;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import be.msec.client.connection.Connection;
import be.msec.client.connection.IConnection;
import be.msec.client.connection.SimulatedConnection;

public class Commands {
	
	private final static boolean simulation = true;

	private final static byte IDENTITY_CARD_CLA =(byte)0x80;
	private static final byte VALIDATE_PIN_INS = 0x22;
	private static final byte GET_SERIAL_INS = 0x24;
	private static final byte GIVE_TIME = 0x25;
	private static final byte AUTHENTICATE_SP = 0x27;
	private final static short SW_VERIFICATION_FAILED = 0x6300;
	private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
	private final static short SW_TIME_UPDATE_REQUIRED = 0x6302;
	
	private IConnection c;
	
	public Commands() {
		
	}

	public void init() {


		if (simulation) {
			//Simulation:
			c = new SimulatedConnection();
		} else {
			//Real Card:
			c = new Connection();
			((Connection)c).setTerminal(0); //depending on which cardreader you use
		}
		
		try {
			
			c.connect();
			
			/*
			 * For more info on the use of CommandAPDU and ResponseAPDU:
			 * See http://java.sun.com/javase/6/docs/jre/api/security/smartcardio/spec/index.html
			 */
			
			CommandAPDU a;
			ResponseAPDU r;
			
			if (simulation) {
				//0. create applet (only for simulator!!!)
				a = new CommandAPDU(0x00, 0xa4, 0x04, 0x00,new byte[]{(byte) 0xa0, 0x00, 0x00, 0x00, 0x62, 0x03, 0x01, 0x08, 0x01}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("select installer applet failed");
				
				a = new CommandAPDU(0x80, 0xB8, 0x00, 0x00,new byte[]{0xb, 0x01,0x02,0x03,0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x00, 0x00, 0x00}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("Applet creation failed");
				
				//1. Select applet  (not required on a real card, applet is selected by default)
				a = new CommandAPDU(0x00, 0xa4, 0x04, 0x00,new byte[]{0x01,0x02,0x03,0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x00, 0x00}, 0x7f);
				r = c.transmit(a);
				System.out.println(r);
				if (r.getSW()!=0x9000) throw new Exception("Applet selection failed");
			}
		} catch (Exception e) {
			System.out.println("Something went wrong in init");
		}

	}
	
	public boolean sendPIN(byte[] pin) {
		CommandAPDU a = new CommandAPDU(IDENTITY_CARD_CLA, VALIDATE_PIN_INS, 0x00, 0x00, pin);
		try {
			System.out.println("trying pin");
			printBA(pin);
			ResponseAPDU r = c.transmit(a);
			if(r.getSW() == 0x9000) {
				System.out.println("Pin succesful");
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
	
	
	public void sendTime() {
		long time = System.currentTimeMillis();
		CommandAPDU a = new CommandAPDU(IDENTITY_CARD_CLA, GIVE_TIME, 0x00, 0x00, longToBytes(time));
		try {
			ResponseAPDU r = c.transmit(a);
			if(r.getSW() == SW_TIME_UPDATE_REQUIRED) {
				// Do update
				
			}			
		} catch (Exception e) {
			System.out.println("Error sending time: " + e.getMessage());
		}
	}
	
	public void close() {
		try {
			c.close();
		} catch(Exception e) {
			System.out.println("Could not close: " +  e.getMessage());
		}
	}
	
	
	
	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}
	
	public static long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
	public static void printBA(byte[] i) {
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(i));
	}
	
	
}
