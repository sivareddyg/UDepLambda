package edu.uw.cs.lil.tiny.mr.language.type;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MutableTypeRepositoryTest {

  MutableTypeRepository types;

  @Before
  public void setUp() throws Exception {
    types = new MutableTypeRepository();
  }

  @Test
  public final void testGetTypeCreateIfNeededString() {
    types.getTypeCreateIfNeeded("(p e)");
    types.getTypeCreateIfNeeded("{s <p,t>}");
    types.getTypeCreateIfNeeded("{np <p,t>}");
    types.getTypeCreateIfNeeded("{vp <np,s>}");
    types.getTypeCreateIfNeeded("<vp*,np>");
    types.getTypeCreateIfNeeded("vp[]");

    assertEquals("<<e,t>,<e,t>>", types.unfoldType(types.getType("vp")));
    assertEquals("<<<e,t>,<e,t>>*,<e,t>>",
        types.unfoldType(types.getType("<vp*,np>")));
    assertEquals("<<e,t>,<e,t>>[]", types.unfoldType(types.getType("vp[]")));
  }
}
