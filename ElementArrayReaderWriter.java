import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 列表项的字节数组读写对象
 *
 * @author 郭联伟
 *
 * @param 
 */
public interface ElementArrayReaderWriter {

 /**
  * 将指定对象写入字节缓冲
  *
  * @param value
  *            需要字节话的对象
  * @param buffer
  *            写入目标
  * @throws FileNotFoundException
  */
 public void writeToBuffer(T value, ByteBuffer buffer) throws IOException;

 /**
  * 从buffer读出对象
  *
  * @param buffer
  * @return
  */
 public T readFromBuffer(ByteBuffer buffer);

 /**
  * 从array读出对象
  *
  * @param array
  * @return
  */
 public T readFromBuffer(byte[] array);

 public int elementByteSize();
}

 