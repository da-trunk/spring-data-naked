package uk.co.blackpepper.bowman;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HalSupportTest {

  @Test
  public void toLinkNameWithIsMethodReturnsDecapitalizedMethodNameSubstring() {
    assertThat(HalSupport.toLinkName("isTheProperty"), is("theProperty"));
  }

  @Test
  public void toLinkNameWithGetMethodReturnsDecapitalizedMethodNameSubstring() {
    assertThat(HalSupport.toLinkName("getTheProperty"), is("theProperty"));
  }

  @Test
  public void toLinkNameWithOtherMethodReturnsMethodName() {
    assertThat(HalSupport.toLinkName("aMethod"), is("aMethod"));
  }
}
