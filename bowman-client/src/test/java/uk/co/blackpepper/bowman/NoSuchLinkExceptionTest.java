package uk.co.blackpepper.bowman;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NoSuchLinkExceptionTest {

  @Test
  public void constructorSetsProperties() {
    NoSuchLinkException exception = new NoSuchLinkException("linked");

    assertThat(exception.getLinkName(), is("linked"));
    assertThat(exception.getMessage(), is("Link 'linked' could not be found!"));
  }
}
