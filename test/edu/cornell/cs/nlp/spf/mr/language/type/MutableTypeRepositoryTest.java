package edu.cornell.cs.nlp.spf.mr.language.type;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;

public class MutableTypeRepositoryTest {

  MutableTypeRepository types;

  @Before
  public void setUp() throws Exception {
    types = new MutableTypeRepository();
  }

  @Test
  public final void testGetTypeCreateIfNeededString() {
    types.addTermType("p", types.getType("e"));
    types.addMacroType("s", "<p,t>");
    types.addMacroType("np", "<p,t>");
    types.addMacroType("vp", "<np,s>");
    types.getTypeCreateIfNeeded("<vp*,np>");
    
    // Not working from spf-2.0. Needs further research.
    // types.getTypeCreateIfNeeded("vp[]");

    assertEquals("<<e,t>,<e,t>>", types.unfoldType(types.getType("vp")));
    assertEquals("<<<e,t>,<e,t>>*,<e,t>>",
        types.unfoldType(types.getType("<vp*,np>")));
  }
}
