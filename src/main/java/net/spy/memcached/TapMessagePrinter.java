package net.spy.memcached;

import java.io.PrintWriter;

import net.spy.memcached.tapmessage.BaseMessage;

public class TapMessagePrinter {

	/**
	 * Prints the message in byte form in a pretty way. This function is mainly used for
	 * debugging.\ purposes.
	 */
	public static void printMessage(BaseMessage message, PrintWriter p) {
		int colNum = 0;
		byte[] mbytes = message.getBytes().array();
		p.printf("   %5s%5s%5s%5s\n", "0", "1", "2", "3");
		p.print("   ----------------------");
		for (int i = 0; i < mbytes.length; i++) {
			if ((i % 4) == 0) {
				p.printf("\n%3d|", colNum);
				colNum += 4;
			}
			int field = mbytes[i];
			if (field < 0)
				field = field + 256;
			p.printf("%5x", field);
		}
		p.print("\n\n");
		p.flush();
	}
}
