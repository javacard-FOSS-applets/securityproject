package be.project.middleware;

import be.msec.client.connection.Connection;

import be.msec.client.connection.IConnection;
import be.msec.client.connection.SimulatedConnection;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.smartcardio.*;

import org.eclipse.jetty.websocket.common.frames.PingFrame;

import spark.Spark;

public class Main {


	interface PinOperation { void operate(byte[] pin); }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Commands cm = new Commands();
		cm.init();
		
		askForPin((pin) -> {
			cm.sendPIN(pin);
		});
	}
	
	public static void askForPin(final PinOperation cont) {
		Spark.get("/pin", (req, res) -> ("<form method=\"POST\">" +
				"    PIN:<input type=\"text\" name=\"pin\" pattern=\"[0-9]{4}\" maxlength=\"4\">" +
				"    <input type=\"submit\" value=\"Validate\">" +
				"</form>"));
		Spark.post("/pin", (req, res) -> {
			System.out.println("test");
			String pin = req.queryParams("pin");
			String[] pp = pin.split("");
			System.out.println("pin");
			int i = 0;
			byte[] pinarr = new byte[4];
			for(String num : pp) {
				pinarr[i] = Byte.parseByte(num);
				i++;
			}
			Spark.stop();
			
			cont.operate(pinarr);
			return true;
		});
	}


}
