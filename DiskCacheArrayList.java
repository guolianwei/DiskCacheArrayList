import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

/**
 * ���ڴ��̵�ArrayList
 *
 * @author guolw
 *
 */
public class DiskCacheArrayList implements List {
 private RandomAccessFile diskCacheFile;
 private static Logger log = Logger.getLogger(DiskCacheArrayList.class);
 /**
  * ���ڴ��д洢�����������
  */
 private int inMemoryElements;
 /**
  * ͷ���ڴ�����б�
  */
 private ArrayList inMemHeadList;



 /**
  * ����д�����ǰ���б���
  */
 private ArrayList inMemTailList;

 /**
  * д�����ǰ����������������Ĭ��Ϊ���ڴ��д洢���������������1/3
  */
 private int tailArraySize;

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
 public static int OprateBufferRecordsNumber = 10000;

 /**
  * ��֧�ֵ��м�¼������1M������
  *
  * @param bigArrayReaderWriter
  * @param inMemoryElementsP
  * @throws FileNotFoundException
  */
 public DiskCacheArrayList(ElementArrayReaderWriter bigArrayReaderWriter,
   int inMemoryElementsP) throws FileNotFoundException {
  elementArrayReaderWriter = bigArrayReaderWriter;
  if (elementArrayReaderWriter.elementByteSize() > (1024 * 1024)) {
   throw new IllegalArgumentException(
     "can't support 1M bytes per Record!");
  }
  tailArraySize = inMemoryElementsP / 3;
  inMemoryElements = inMemoryElementsP - tailArraySize;
  inMemHeadList = new ArrayList(inMemoryElements);
  inMemTailList = new ArrayList(tailArraySize);
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
   inMemHeadList.add(e);
   size = inMemHeadList.size();
   return true;
  }
  if (inMemTailList.size() >= this.tailArraySize) {
   try {
    flushTail();
   } catch (IOException e1) {
    log.error("IOException", e1);
    return false;
   }
  }
  inMemTailList.add(e);
  size++;

  return true;
 }

 private void flushTail() throws IOException {
  ByteBuffer buf = ByteBuffer.allocate(inMemTailList.size()
    * oneElementByteSize);
  for (int i = 0; i < inMemTailList.size(); i++) {
   elementArrayReaderWriter.writeToBuffer(inMemTailList.get(i), buf);
  }
  buf.flip();
  if (buf.array().length > 0) {
   while (buf.hasRemaining()) {
    fc.write(buf);
   }
  }
  inMemTailList.clear();
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
  this.inMemHeadList.clear();
  this.inMemTailList.clear();
  try {
   deleteFile();
   createFile();
  } catch (IOException e) {
   log.error("IOException", e);
  }
 }

 @Override
 public E get(int index) {
  RangeCheck(index);
  if (index < this.inMemHeadList.size()) {
   return this.inMemHeadList.get(index);
  } else if (index >= (size - this.inMemTailList.size())) {
   int indexTail = index - (size - this.inMemTailList.size());
   return this.inMemTailList.get(indexTail);
  }
  try {
   long getPos = (long) (index - this.inMemHeadList.size())
     * (long) this.elementArrayReaderWriter.elementByteSize();
   ByteBuffer buf = ByteBuffer.allocate(oneElementByteSize);
   fc.read(buf, getPos);
   return elementArrayReaderWriter.readFromBuffer(buf);
  } catch (IOException e) {
   log.error("IOException", e);
  }
  return null;
 }

 @Override
 public E set(int index, E element) {
  RangeCheck(index);
  if (index < this.inMemHeadList.size()) {
   return this.inMemHeadList.set(index, element);
  } else if (index >= size - this.inMemTailList.size()) {
   int indexTail = index - (size - this.inMemTailList.size());
   return this.inMemTailList.set(indexTail, element);
  }
  // ���ļ��ж�ȡ��ǰ��¼����Ҫ����Position����λ�ü��㡣
  long pos = (long) (index - this.inMemHeadList.size())
    * (long) elementArrayReaderWriter.elementByteSize();
  ByteBuffer buf = ByteBuffer.allocate(oneElementByteSize);
  try {
   elementArrayReaderWriter.writeToBuffer(element, buf);
   buf.flip();
   fc.write(buf, pos);
  } catch (IOException e) {
   log.error("IOException", e);
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
  if (index < this.inMemHeadList.size()) {
   return removeInHeadList(index);
  } else if (index >= (size - this.inMemTailList.size())) {
   int tailIndex = index - (size - this.inMemTailList.size());
   size--;
   return this.inMemTailList.remove(tailIndex);

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
  int indexInDisk = index - this.inMemHeadList.size();
  try {
   // ɾ�����Ǵ����е����һ����¼��ֱ�ӽ�Size��1
   if (indexInDisk == (size - this.inMemElsSize() - 1)) {
    size--;
    if (size == this.inMemElsSize()) {
     // �������ݴ������ڴ��У��������ļ����
     this.initDiskfile();
    }
    return el;
   }
   moveDiskRecordToLeft(indexInDisk + 1,
     size - this.inMemHeadList.size(), 1);
   size--;
   return el;
  } catch (IOException e) {
   log.error("IOException", e);
  }
  return null;
 }

 /**
  * ��ͷ���б����Ƴ�
  *
  * @param index
  * @return
  */
 private E removeInHeadList(int index) {
  E removedEl = this.inMemHeadList.remove(index);
  size--;
  // ��������ļ��������ݣ��򽫴����еĵ�һ������Ǩ�Ƶ��ڴ�
  if ((this.inMemElsSize()) < size) {
   E getedEl = this.get(this.inMemHeadList.size());
   try {
    moveDiskRecordToLeft(1, size - this.inMemHeadList.size(), 1);
    this.inMemHeadList.add(getedEl);
   } catch (IOException e) {
    log.error("IOException", e);
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
  *
  * @throws IOException
  */
 private void initDiskfile() throws IOException {
  deleteFile();
  createFile();
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

 /**
  * ����ArrayList
  *
  * @see java.util.List#subList(int, int)
  */
 @Override
 public List subList(int fromIndex, int toIndex) {
  RangeCheck(fromIndex);
  RangeCheck(toIndex);
  if (fromIndex > toIndex) {
   throw new IllegalArgumentException("fromIndex(" + fromIndex
     + ") > toIndex(" + toIndex + ")");
  }
  int offsetTail = size - this.inMemTailList.size();
  int menHeadSize = this.inMemHeadList.size();
  List listReturn = new ArrayList(toIndex - fromIndex);
  int indexHeadEnd = Math.min(toIndex, menHeadSize);
  for (int i = fromIndex; i < indexHeadEnd; i++) {
   listReturn.add(inMemHeadList.get(i));
  }
  if (toIndex <= menHeadSize) {
   return listReturn;
  }

  int diskStart = this.inMemHeadList.size();
  int diskEnd = offsetTail - 1;

  if ((diskEnd <= toIndex && diskEnd >= fromIndex)
    || (diskStart <= toIndex && diskStart >= fromIndex)) {
   try {
    subListInDiskElments(fromIndex, toIndex, offsetTail, listReturn);
   } catch (IOException e) {
    log.error("IOException", e);
    return null;
   }
  }
  if (toIndex > offsetTail) {
   int fromTail = fromIndex - offsetTail;
   fromTail = fromTail < 0 ? 0 : fromTail;
   for (int i = fromTail; i < (toIndex - offsetTail); i++) {
    listReturn.add(inMemTailList.get(i));
   }
  }
  return listReturn;
 }

 /**
  * �Ӵ����ļ��л�����б�
  *
  * @param fromIndex
  * @param toIndex
  * @param offsetTail
  * @return
  * @throws IOException
  */
 private void subListInDiskElments(int fromIndex, int toIndex,
   int offsetTail, List rArrayDisk) throws IOException {
  int offsetHead = this.inMemHeadList.size();
  int diskBeginIndex = fromIndex - offsetHead;
  diskBeginIndex = diskBeginIndex < 0 ? 0 : diskBeginIndex;
  int diskEndIndex = Math.min(toIndex - offsetHead, offsetTail
    - offsetHead);
  readFromDisk(rArrayDisk, diskBeginIndex, diskEndIndex);
 }

 /**
  * �Ӵ��������ж�ȡ��¼�������ӵ�����List
  *
  * @param rArrayDisk
  *            ����Ŀ��
  * @param diskBeginIndex
  *            ��ʼ����������������¼
  * @param diskEndIndex
  *            ����������������������¼
  * @throws IOException
  */
 private void readFromDisk(List rArrayDisk, int diskBeginIndex,
   int diskEndIndex) throws IOException {
  long posBegin = (long) this.oneElementByteSize * (long) diskBeginIndex;
  long posEnd = (long) this.oneElementByteSize * (long) diskEndIndex;
  ByteBuffer bufferAll = ByteBuffer.allocate((int) (posEnd - posBegin));
  this.fc.read(bufferAll, posBegin);
  // �������е�����һ�ο�����ArrayList
  int diskSize = diskEndIndex - diskBeginIndex;
  byte[] buffEl = new byte[oneElementByteSize];
  bufferAll.flip();
  for (int i = 0; i < diskSize; i++) {
   bufferAll.get(buffEl);
   E el = this.elementArrayReaderWriter.readFromBuffer(buffEl);
   rArrayDisk.add(el);
  }
 }

 /**
  * ɾ���б����ݵı����ļ�
  *
  * @throws IOException
  */
 protected void deleteFile() throws IOException {
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
  // System.out.println(getDiskFileAbsolutePath());
  fc = diskCacheFile.getChannel();
 }

 public String getDiskFileAbsolutePath() {
  File file = new File(tempFilePath);
  return (file.getAbsolutePath());
 }

 public int inMemElsSize() {
  return inMemHeadList.size() + inMemTailList.size();
 }

 @Override
 public String toString() {
  StringBuffer sb = new StringBuffer("");
  for (int i = 0; i < size; i++) {
   sb.append(this.get(i) + ",");
  }
  return super.toString() + " | " + sb.toString();
 }

 @Override
 public void finalize() throws Throwable {
  super.finalize();
  deleteFile();
 }

 /**
  * Trims the capacity of this ArrayList instance to be the list's
  * current size. An application can use this operation to minimize the
  * storage of an ArrayList instance.
  */
 public void trimToSize() {
  this.inMemHeadList.trimToSize();
  this.inMemTailList.trimToSize();
  // TODO:�����ļ���trim
  // int oldCapacity = elementData.length;
  // if (size < oldCapacity) {
  // elementData = Arrays.copyOf(elementData, size);
 }

 @Override
 public ListIterator listIterator(int index) {
  // TODO Auto-generated method stub
  return null;
 }
}