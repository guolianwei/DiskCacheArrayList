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
 * 基于磁盘的ArrayList
 *
 * @author 郭联伟
 *
 * @param 
 *            待增加项
 */
public class DiskCacheArrayList implements List {
 private RandomAccessFile diskCacheFile;
 /**
  * 在内存中存储的数据项个数
  */
 private int inMemoryElements;
 /**
  * 内存列表
  */
 private ArrayList inMemroyList;
 
 /**
  * 数据写入磁盘前前的缓存
  */
 private ArrayList tailBuffer;
 
 /**
  * 写入磁盘前缓存的数据项个数，默认为“内存中存储的数据项个数”的1/3
  */
 private int tailBufferSize;
 
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
 public static int OprateBufferRecordsNumber = 1000;
 /**
  * 不支持单行记录数超过1M的数据
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
  * 追加
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
  // 从文件中读取当前记录，需要根据Position进行位置计算。
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
  * 从磁盘中移除
  *
  * @param index
  * @return
  */
 private E removeInDisk(final int index) {
  E el = this.get(index);
  int indexInDisk = index - this.inMemroyList.size();
  try {
   // 删除的是最后一个记录，直接将Size减1
   if (indexInDisk == (size - this.inMemroyList.size() - 1)) {
    size--;
    if (size == this.inMemroyList.size()) {
     // 所有数据存在于内存中，将磁盘文件清空
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
  * 从内存中移除
  *
  * @param index
  * @return
  */
 private E removeInMemory(int index) {
  E removedEl = this.inMemroyList.remove(index);
  size--;
  // 如果磁盘文件中有数据，则将磁盘中的第一条数据迁移到内存
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
  * 删除列表数据的本地文件
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
  * 给本地新建磁盘缓存文件
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
  