import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * �б�����ֽ������д����
 *
 * @author ����ΰ
 *
 * @param 
 */
public interface ElementArrayReaderWriter {

 /**
  * ��ָ������д���ֽڻ���
  *
  * @param value
  *            ��Ҫ�ֽڻ��Ķ���
  * @param buffer
  *            д��Ŀ��
  * @throws FileNotFoundException
  */
 public void writeToBuffer(T value, ByteBuffer buffer) throws IOException;

 /**
  * ��buffer��������
  *
  * @param buffer
  * @return
  */
 public T readFromBuffer(ByteBuffer buffer);

 /**
  * ��array��������
  *
  * @param array
  * @return
  */
 public T readFromBuffer(byte[] array);

 public int elementByteSize();
}

 