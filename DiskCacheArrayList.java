import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
/**
 * ���ڴ��̵�ArrayList
 *
 * @author ����ΰ
 *
 * @param 
 *            ��������
 */
public class DiskCacheArrayList implements List {
 private RandomAccessFile diskCacheFile;
 /**
  * ���ڴ��д洢�����������
  */
 private int inMemoryElements;
 /**
  * �ڴ��б�
  */
 private ArrayList inMemroyList;
 
 /**
  * ����д�����ǰǰ�Ļ���
  */
 private ArrayList tailBuffer;
 
 /**
  * д�����ǰ����������������Ĭ��Ϊ���ڴ��д洢���������������1/3
  */
 private int tailBufferSize;
 
 /**
  * ��ǰ�б����������������
  */
 private int size = 0;
 /**
  * һ����¼���ֽ���
  */
 private int oneElementByteSize;
 /**
  * ������Ķ�д����
  */
 public ElementArrayReaderWriter elementArrayReaderWriter;
 /**
  * �������һ��UUID����Ϊ��ʱ�洢�ļ�
  */
 String ID;
 /**
  * �����ļ��洢·��
  */
 String tempFilePath;
 FileChannel fc;
 /**
  * ���ļ����ݲ���ʱ�Ļ�������¼��
  */
 public static int OprateBufferRecordsNumber = 1000;
 /**
  * ��֧�ֵ��м�¼������1M������
  *
  * @param bigArrayReaderWriter
  * @param inMemoryElements
  * @throws FileNotFoundException
  */
 public DiskCacheArrayList(ElementArrayReaderWriter bigArrayReaderWriter,
   int inMemoryElements) throws FileNotFoundException {
  elementArrayReaderWriter = bigArrayReaderWriter;
  if (elementArrayReaderWriter.elementByteSize() > (1024 * 1024)) {
   throw new IllegalArgumentException(
     "can't support 1M bytes per Record!");
  }
  this.inMemoryElements = inMemoryElements;
  inMemroyList = new ArrayList(inMemoryElements);
  tailBufferSize=inMemoryElements/3;
  tailBuffer = new ArrayList(tailBufferSize);
  oneElementByteSize = this.elementArrayReaderWriter.elementByteSize();
  ID = java.util.UUID.randomUUID().toString();
  tempFilePath = ID + ".array";
  createFile();
 }
 /**
  * Checks if the given index is in range. If not, throws an appropriate
  * runtime exception. This method does *not* check if the index is negative:
  * It is always used immediately prior to an array access, which throws an
  * ArrayIndexOutOfBoundsException if index is negative.
  */
 private void RangeCheck(int index) {
  if (index >= size)
   throw new IndexOutOfBoundsException("Index: " + index + ", Size: "
     + size);
 }
 @Override
 public int size() {
  return size;
 }
 @Override
 public boolean isEmpty() {
  // TODO Auto-generated method stub
  return false;
 }
 @Override
 public boolean contains(Object o) {
  return false;
 }
 @Override
 public Iterator iterator() {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public Object[] toArray() {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public T[] toArray(T[] a) {
  throw new IllegalArgumentException("Not supported");
 }
 /**
  * ׷��
  *
  * @see java.util.List#add(java.lang.Object)
  */
 @Override
 public boolean add(E e) {
  if (size < this.inMemoryElements) {
   inMemroyList.add(e);
   size = inMemroyList.size();
   return true;
  }
  try {
   ByteBuffer buf = ByteBuffer.allocate(oneElementByteSize);
   buf.clear();
   elementArrayReaderWriter.write(e, buf);
   buf.flip();
   if (buf.array().length > 0) {
    while (buf.hasRemaining()) {
     fc.write(buf);
    }
    size++;
   }
  } catch (IOException ex) {
   ex.printStackTrace();
   return false;
  }
  return true;
 }
    private void addToTailBuffer(E elment){
     tailBuffer.add(elment);
    }
 /**
  * ��֧�ָò���
  *
  * @see java.util.List#remove(java.lang.Object)
  */
 @Override
 public boolean remove(Object o) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public boolean containsAll(Collection c) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public boolean addAll(Collection c) {
  // TODO Auto-generated method stub
  return false;
 }
 @Override
 public boolean addAll(int index, Collection c) {
  // TODO Auto-generated method stub
  return false;
 }
 @Override
 public boolean removeAll(Collection c) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public boolean retainAll(Collection c) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public void clear() {
  size = 0;
  this.inMemroyList.clear();
  try {
   deleteFile();
   createFile();
  } catch (IOException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
 }
 @Override
 public E get(int index) {
  RangeCheck(index);
  if (index < this.inMemroyList.size()) {
   return this.inMemroyList.get(index);
  }
  try {
   long getPos = (long) (index - this.inMemroyList.size())
     * (long) this.elementArrayReaderWriter.elementByteSize();
   ByteBuffer buf = ByteBuffer.allocate(oneElementByteSize);
   fc.read(buf, getPos);
   return elementArrayReaderWriter.read(buf);
  } catch (IOException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
  return null;
 }
 @Override
 public E set(int index, E element) {
  RangeCheck(index);
  if (index < this.inMemroyList.size()) {
   return this.inMemroyList.set(index, element);
  }
  // ���ļ��ж�ȡ��ǰ��¼����Ҫ����Position����λ�ü��㡣
  long pos = (long) (index - this.inMemroyList.size())
    * (long) elementArrayReaderWriter.elementByteSize();
  ByteBuffer buf = ByteBuffer.allocate(oneElementByteSize);
  elementArrayReaderWriter.write(element, buf);
  try {
   buf.flip();
   fc.write(buf, pos);
  } catch (IOException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
   return null;
  }
  return element;
 }
 @Override
 public void add(int index, E element) {
 }
 @Override
 public E remove(int index) {
  RangeCheck(index);
  if (index < this.inMemroyList.size()) {
   return removeInMemory(index);
  } else {
   return removeInDisk(index);
  }
 }
 /**
  * �Ӵ������Ƴ�
  *
  * @param index
  * @return
  */
 private E removeInDisk(final int index) {
  E el = this.get(index);
  int indexInDisk = index - this.inMemroyList.size();
  try {
   // ɾ���������һ����¼��ֱ�ӽ�Size��1
   if (indexInDisk == (size - this.inMemroyList.size() - 1)) {
    size--;
    if (size == this.inMemroyList.size()) {
     // �������ݴ������ڴ��У��������ļ����
     this.initDiskfile();
    }
    return el;
   }
   moveDiskRecordToLeft(indexInDisk + 1,
     size - this.inMemroyList.size(), 1);
   size--;
   return el;
  } catch (IOException e) {
   e.printStackTrace();
  }
  return null;
 }
 /**
  * ���ڴ����Ƴ�
  *
  * @param index
  * @return
  */
 private E removeInMemory(int index) {
  E removedEl = this.inMemroyList.remove(index);
  size--;
  // ��������ļ��������ݣ��򽫴����еĵ�һ������Ǩ�Ƶ��ڴ�
  if (this.inMemroyList.size() < size) {
   E getedEl = this.get(this.inMemroyList.size());
   try {
    moveDiskRecordToLeft(1, size - this.inMemroyList.size(), 1);
    this.inMemroyList.add(getedEl);
   } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    return null;
   }
  }
  return removedEl;
 }
 /**
  * ���������ļ��еļ�¼�����ƶ������Ǹ����ļ�¼��
  *
  * @param beginIndex
  *            ��ʼ�ƶ���¼����
  * @param endIndex
  *            �����ƶ�������
  * @param recordsNoToRecover
  *            ��Ҫ���ǵļ�¼��
  * @throws IOException
  */
 private void moveDiskRecordToLeft(int beginIndex, int endIndex,
   int recordsNoToRecover) throws IOException {
  int bytesToLeftMove = recordsNoToRecover * oneElementByteSize;
  // ��Ҫ�ƶ��ļ�¼�ĵ�һ�����ļ��е���ʼ�ֽ�λ��
  long beginPos = beginIndex * oneElementByteSize;
  // ��λһ��OprateBufferRecordsNumber��Ԫ�ص�buf
  ByteBuffer buffer = ByteBuffer.allocate(oneElementByteSize
    * OprateBufferRecordsNumber);
  buffer.clear();
  // ���ļ��е��ֽ������ƶ�һ����¼���ֽ���
  fc.position(beginPos);
  int readedBytes = fc.read(buffer);
  long posS = beginPos - bytesToLeftMove;
  while (readedBytes != -1) {
   buffer.flip();
   int bytesWriten = 0;
   while (buffer.hasRemaining()) {
    bytesWriten = fc.write(buffer, posS);
    posS += bytesWriten;
   }
   buffer.clear();
   readedBytes = fc.read(buffer);
  }
 }
 /**
  * @see java.nio.channels.FileChannel#force
  * @param metaData
  * @throws IOException
  */
 public void force(boolean metaData) throws IOException {
  fc.force(metaData);
 }
 /**
  * ��ʼ�������ļ�
  */
 private void initDiskfile() {
  try {
   deleteFile();
   createFile();
  } catch (IOException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
 }
 @Override
 public int indexOf(Object o) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public int lastIndexOf(Object o) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public ListIterator listIterator() {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public ListIterator listIterator(int index) {
  throw new IllegalArgumentException("Not supported");
 }
 @Override
 public List subList(int fromIndex, int toIndex) {
  throw new IllegalArgumentException("Not supported");
 }
 /**
  * ɾ���б����ݵı����ļ�
  *
  * @throws IOException
  */
 private void deleteFile() throws IOException {
  fc.close();
  this.diskCacheFile.close();
  File f = new File(this.tempFilePath);
  f.delete();
 }
 /**
  * �������½����̻����ļ�
  *
  * @throws FileNotFoundException
  */
 private void createFile() throws FileNotFoundException {
  diskCacheFile = new RandomAccessFile(tempFilePath, "rw");
//  System.out.println(getDiskFileAbsolutePath());
  fc = diskCacheFile.getChannel();
 }
 public String getDiskFileAbsolutePath() {
  File file = new File(tempFilePath);
  return (file.getAbsolutePath());
 }
 public int inMemoryElementsSize() {
  return inMemroyList.size();
 }

 @Override
 public String toString() {
  StringBuffer sb=new StringBuffer("");
  for(int i=0;i    sb.append(this.get(i)+",");
  }
  return super.toString()+" | "+sb.toString();
 }
 @Override
 protected void finalize() throws Throwable {
  super.finalize();
  deleteFile();
 }
}
  