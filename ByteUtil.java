package mcparallelprocess.util;

import java.nio.ByteBuffer;

public class ByteUtil {
	// 浮点到字节转换
	public static byte[] doubleToByte(double d) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(d);
		return bytes;
	}

	// 字节到浮点转换
	public static double byteToDouble(byte[] b) {
		return ByteBuffer.wrap(b).getDouble();
	}

	// 整数到字节数组的转换
	public static byte[] intToByte(int number) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(number);
		return bytes;
	}

	// 字节数组到整数的转换
	public static int byteToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}
}
