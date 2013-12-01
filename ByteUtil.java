import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.merit.hugedata.ByteUtil;

public class ByteUtilTest {

 @BeforeClass
 public static void setUpBeforeClass() throws Exception {
 }

 @Before
 public void setUp() throws Exception {
 }

 @Test
 public void unsingedByteToIntTest() {
  for (int i = 0; i < 255; i++) {
   int v = ByteUtil.unsingedByteToInt(ByteUtil.intToUnsingedByte(i));
   assertEquals(v, i);
  }
 }
}