import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.merit.hugedata.ByteUtil;
import com.merit.hugedata.DiskCacheArrayList;
import com.merit.hugedata.ElementArrayReaderWriter;

public class DiskCacheArrayListTest extends TestCase {
 int testMax = 1000;
 final int byteSize = 4;
 int iMemoryRecord = 100;
 DiskCacheArrayList arrayList;
 ElementArrayReaderWriter brw;
 private static Logger log = Logger.getLogger(DiskCacheArrayListTest.class);

 @Before
 public void setUp() throws Exception {
  brw = new ElementArrayReaderWriter() {
   @Override
   public void writeToBuffer(Integer value, ByteBuffer buffer) {
    buffer.putInt(value);
   }

   @Override
   public Integer readFromBuffer(ByteBuffer buffer) {
    buffer.flip();
    return buffer.getInt();
   }

   @Override
   public int elementByteSize() {
    return byteSize;
   }

   @Override
   public Integer readFromBuffer(byte[] buffer) {
    return ByteBuffer.wrap(buffer).getInt();
   }
  };
  arrayList = new DiskCacheArrayList(brw, iMemoryRecord);

 }

 @After
 public void tearDown() throws Exception {
  try {
   arrayList.finalize();
  } catch (Throwable e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }
 }

 @Test
 public void testAdd() {
  add();
  for (int i = 0; i < testMax; i++) {
   int vI = arrayList.get(i);
   assertEquals(vI, i);
  }
  File file = new File(arrayList.getDiskFileAbsolutePath());
  long diskFileByteSize = file.length();
  long byteSizeTotal = (long) testMax * (long) brw.elementByteSize();
  assertEquals(
    byteSizeTotal,
    diskFileByteSize + (long) arrayList.inMemElsSize()
      * (long) brw.elementByteSize());
 }

 private void add() {
  for (int i = 0; i < testMax; i++) {
   arrayList.add(i);
  }
 }

 @Test
 public void testRemove() {
  arrayList.clear();
  add();
  // 从内存缓冲区移除一个
  int indexInmeory = arrayList.inMemElsSize() / 2;
  arrayList.remove(indexInmeory);
  for (int i = indexInmeory; i < arrayList.size(); i++) {
   int real = (i + 1);
   assertEquals(real, arrayList.get(i).intValue());
  }

  arrayList.remove(indexInmeory);
  for (int i = indexInmeory; i < arrayList.size(); i++) {
   int real = (i + 2);
   assertEquals(real, arrayList.get(i).intValue());
  }
  // 从磁盘中移除一个
  arrayList.clear();
  add();
  int sizeImem = arrayList.inMemElsSize();
  arrayList.remove(sizeImem - 1);
  for (int i = arrayList.inMemElsSize(); i < arrayList.size(); i++) {
   int real = (i + 1);
   assertEquals(real, arrayList.get(i).intValue());
  }
  // 检验总数
  assertEquals(testMax, arrayList.size() + 1);

  arrayList.clear();
  add();
  int size = arrayList.size();
  int testRemoveSize = 4;
  // 移除前testRemoveSize条
  for (int i = 0; i < testRemoveSize; i++) {
   int vI = arrayList.remove(0);
   assertEquals(vI, i);
  }
  assertEquals(size, arrayList.size() + testRemoveSize);
  // 从后往前移除
  // int iBack=0;
  while (arrayList.size() > 0) {
   int vI = arrayList.remove(arrayList.size() - 1);
   int vC = arrayList.size() + testRemoveSize;
   assertEquals(vI, vC);
  }
  // 和ArrayList进行一一比对。
  arrayList.clear();
  add();
  ArrayList arrayForTest = new ArrayList();
  for (int i = 0; i < this.testMax; i++) {
   arrayForTest.add(i);
  }

  int random = 6;
  arrayForTest.remove(random);
  System.out.println(random);
  arrayList.remove(random);
  assertEquals(arrayList.size(), arrayForTest.size());
  for (int i = 0; i < arrayList.size(); i++) {
   assertEquals(arrayForTest.get(i), arrayList.get(i));
  }

  while (arrayForTest.size() > 1) {
   // 随机移除X项
   random = (int) (Math.random() * (double) arrayForTest.size());
   arrayForTest.remove(random);
   System.out.println(random);
   arrayList.remove(random);
   assertEquals(arrayList.size(), arrayForTest.size());
   for (int i = 0; i < arrayList.size(); i++) {
    assertEquals(arrayForTest.get(i), arrayList.get(i));
   }
  }
  assertEquals(arrayForTest.get(0), arrayList.get(0));
 }

 public void testSubList() {
  arrayList.clear();
  add();
  ArrayList arrayForTest = new ArrayList();
  for (int i = 0; i < this.testMax; i++) {
   arrayForTest.add(i);
  }
  // List listSubDisk = arrayList.subList(9914, 9983);
  // List listSubTest = arrayForTest.subList(9914, 9983);
  // for (int j = 0; j < listSubDisk.size(); j++) {
  // int actual = listSubDisk.get(j);
  // int expect = listSubTest.get(j);
  // assertEquals(expect, actual);
  // }
  for (int i = 0; i < arrayList.size() * 10; i++) {
   int random1 = (int) (Math.random() * (double) arrayList.size());
   int random2 = (int) (Math.random() * (double) arrayList.size());
   int min = Math.min(random1, random2);
   int max = Math.max(random1, random2);
   log.info("min=" + min);
   log.info("max=" + max);
   List listSubDisk = arrayList.subList(min, max);
   List listSubTest = arrayForTest.subList(min, max);
   for (int j = 0; j < listSubDisk.size(); j++) {
    int actual = listSubDisk.get(j);
    int expect = listSubTest.get(j);
    assertEquals(expect, actual);
   }
  }
 }

 @Test
 public void testSet() {
  arrayList.clear();
  add();

  for (int i = this.testMax - 1; i >= 0; i--) {
   arrayList.set(testMax - 1 - i, i);
  }

  for (int i = 0; i < this.testMax; i++) {
   assertEquals(testMax - 1 - i, arrayList.get(i).intValue());
  }
 }
}