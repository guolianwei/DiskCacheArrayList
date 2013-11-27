package mcparallelprocess.util;

import java.nio.ByteBuffer;

public class ByteUtil {
	// ���㵽�ֽ�ת��
	public static byte[] doubleToByte(double d) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(d);
		return bytes;
	}

	// �ֽڵ�����ת��
	public static double byteToDouble(byte[] b) {
		return ByteBuffer.wrap(b).getDouble();
	}

	// �������ֽ������ת��
	public static byte[] intToByte(int number) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(number);
		return bytes;
	}

	// �ֽ����鵽������ת��
	public static int byteToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}
}
