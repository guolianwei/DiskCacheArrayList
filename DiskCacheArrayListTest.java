import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
public class DiskCacheArrayListTest extends TestCase {
 int testMax = 13010;
 final int byteSize = 4;
 int iMemoryRecord = 3000;
 DiskCacheArrayList arrayList;
 ElementArrayReaderWriter brw;
 @Before
 public void setUp() throws Exception {
  brw = new ElementArrayReaderWriter() {
   @Override
   public void write(Integer value, ByteBuffer buffer) {
    buffer.put(ByteUtil.intToByte(value));
   }
   @Override
   public Integer read(ByteBuffer buffer) {
    return ByteUtil.byteToInt(buffer.array());
   }
   @Override
   public int elementByteSize() {
    return byteSize;
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
  assertEquals(byteSizeTotal, diskFileByteSize + (long) iMemoryRecord
    * (long) brw.elementByteSize());
 }
    private void add(){
     for (int i = 0; i < testMax; i++) {
   arrayList.add(i);
  }
    }
 @Test
 public void testRemove() {
  arrayList.clear();
  add();
  //���ڴ滺�����Ƴ�һ��
  int indexInmeory=arrayList.inMemoryElementsSize()/2;
  arrayList.remove(indexInmeory);
  for(int i=indexInmeory;i    int real=(i+1);
   assertEquals(real,arrayList.get(i).intValue());
  }
  
  arrayList.remove(indexInmeory);
  for(int i=indexInmeory;i    int real=(i+2);
   assertEquals(real,arrayList.get(i).intValue());
  }
  //�Ӵ������Ƴ�һ��
  arrayList.clear();
  add();
  arrayList.remove(arrayList.inMemoryElementsSize());
  for(int i=arrayList.inMemoryElementsSize();i    int real=(i+1);
   assertEquals(real,arrayList.get(i).intValue());
  }
  //��������
  assertEquals(testMax,arrayList.size()+1);
  
  
  arrayList.clear();
  add();
  int size = arrayList.size();
  int testRemoveSize = 4;
  // �Ƴ�ǰtestRemoveSize��
  for (int i = 0; i < testRemoveSize; i++) {
   int vI = arrayList.remove(0);
   assertEquals(vI, i);
  }
  assertEquals(size, arrayList.size() + testRemoveSize);
  assertEquals(iMemoryRecord, arrayList.inMemoryElementsSize());
  //�Ӻ���ǰ�Ƴ�
//  int iBack=0;
  while(arrayList.size()>0){
   int vI = arrayList.remove(arrayList.size()-1);
   int vC=arrayList.size()+testRemoveSize;
   assertEquals(vI,vC);
  }
  //��ArrayList����һһ�ȶԡ�
  arrayList.clear();
  add();
  ArrayList arrayForTest=new ArrayList();
  for(int i=0;i    arrayForTest.add(i);
  }
  
  int random=6;
  arrayForTest.remove(random);
  System.out.println(random);
  arrayList.remove(random);
  assertEquals(arrayList.size(),arrayForTest.size());
  for(int i=0;i    assertEquals(arrayForTest.get(i),arrayList.get(i));
  }
  
  while(arrayForTest.size()>1){
   //����Ƴ�X��
   random=(int)(Math.random()*(double)arrayForTest.size());
   arrayForTest.remove(random);
   System.out.println(random);
   arrayList.remove(random);
   assertEquals(arrayList.size(),arrayForTest.size());
   for(int i=0;i     assertEquals(arrayForTest.get(i),arrayList.get(i));
   }
  }
 }
 @Test
 public void testSet() {
  arrayList.clear();
  add();
  for(int i=this.testMax-1;i>=0;i--){
   arrayList.set(testMax-1-i, i);
  }
  
  for(int i=0;i    assertEquals(testMax-1-i,arrayList.get(i).intValue());
  }
  
 }
}