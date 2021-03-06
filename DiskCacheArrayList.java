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
 * 基于磁盘的ArrayList
 *
 * @author guolw
 *
 */
public class DiskCacheArrayList implements List {
 private RandomAccessFile diskCacheFile;
 private static Logger log = Logger.getLogger(DiskCacheArrayList.class);
 /**
  * 在内存中存储的数据项个数
  */
 private int inMemoryElements;
 /**
  * 头部内存对象列表
  */
 private ArrayList inMemHeadList;



 /**
  * 数据写入磁盘前的列表缓存
  */
 private ArrayList inMemTailList;

 /**
  * 写入磁盘前缓存的数据项个数，默认为“内存中存储的数据项个数”的1/3
  */
 private int tailArraySize;

 /**
  * 当前列表包含的数据项总数
  */
 private int size = 0;
 /**
  * 一条记录的字节数
  */
 private int oneElementByteSize;
 /**
  * 增加项的读写对象
  */
 public ElementArrayReaderWriter elementArrayReaderWriter;
 /**
  * 随机生成一个UUID，最为临时存储文件
  */
 String ID;
 /**
  * 本地文件存储路径
  */
 String tempFilePath;
 FileChannel fc;
 /**
  * 在文件数据操作时的缓冲区记录数
  */
 public static int OprateBufferRecordsNumber = 10000;

 /**
  * 不支持单行记录数超过1M的数据
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
  * 追加
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
  * 不支持该操作
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
  // 从文件中读取当前记录，需要根据Position进行位置计算。
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
  * 从磁盘中移除
  *
  * @param index
  * @return
  */
 private E removeInDisk(final int index) {
  E el = this.get(index);
  int indexInDisk = index - this.inMemHeadList.size();
  try {
   // 删除的是磁盘中的最后一个记录，直接将Size减1
   if (indexInDisk == (size - this.inMemElsSize() - 1)) {
    size--;
    if (size == this.inMemElsSize()) {
     // 所有数据存在于内存中，将磁盘文件清空
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
  * 从头部列表中移除
  *
  * @param index
  * @return
  */
 private E removeInHeadList(int index) {
  E removedEl = this.inMemHeadList.remove(index);
  size--;
  // 如果磁盘文件中有数据，则将磁盘中的第一条数据迁移到内存
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
  * 将二进制文件中的记录向左移动，覆盖给定的记录数
  *
  * @param beginIndex
  *            开始移动记录索引
  * @param endIndex
  *            结束移动的索引
  * @param recordsNoToRecover
  *            需要覆盖的记录数
  * @throws IOException
  */
 private void moveDiskRecordToLeft(int beginIndex, int endIndex,
   int recordsNoToRecover) throws IOException {
  int bytesToLeftMove = recordsNoToRecover * oneElementByteSize;
  // 需要移动的记录的第一条在文件中的起始字节位置
  long beginPos = beginIndex * oneElementByteSize;
  // 定位一个OprateBufferRecordsNumber个元素的buf
  ByteBuffer buffer = ByteBuffer.allocate(oneElementByteSize
    * OprateBufferRecordsNumber);
  buffer.clear();
  // 将文件中的字节向左移动一个记录的字节数
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
  * 初始化磁盘文件
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
  * 返回ArrayList
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
  * 从磁盘文件中获得子列表
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
  * 从磁盘数据中读取记录，并增加到给定List
  *
  * @param rArrayDisk
  *            增加目标
  * @param diskBeginIndex
  *            开始索引，包括此条记录
  * @param diskEndIndex
  *            结束索引，不包括此条记录
  * @throws IOException
  */
 private void readFromDisk(List rArrayDisk, int diskBeginIndex,
   int diskEndIndex) throws IOException {
  long posBegin = (long) this.oneElementByteSize * (long) diskBeginIndex;
  long posEnd = (long) this.oneElementByteSize * (long) diskEndIndex;
  ByteBuffer bufferAll = ByteBuffer.allocate((int) (posEnd - posBegin));
  this.fc.read(bufferAll, posBegin);
  // 将磁盘中的数据一次拷贝到ArrayList
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
  * 删除列表数据的本地文件
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
  * 给本地新建磁盘缓存文件
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
  // TODO:磁盘文件的trim
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