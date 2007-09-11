package org.makumba.util;

/** An ordered set of chooser choices */
public class ChoiceSet extends java.util.ArrayList{
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
public static final String PARAMNAME="org.makumba.ChoiceSet";

  public class Choice{
    Object value;
    String title;
    boolean forceSelection;
    boolean forceDeselection;
    
    public Object getValue(){return value; }
    public String getTitle(){return title; }
  }

  java.util.Map m= new java.util.HashMap();

  public void add(Object value, String title, boolean forceSelection, boolean forceDeselection){
    // only Pointer.Null values can be repeated, rest are ignored
    // null values are not a choice, they are simply a text between choices
    if(value!=org.makumba.Pointer.Null && value!=null && m.get(value)!=null)
      return;
    Choice c= new Choice();
    c.value=value;
    c.title=title;
    c.forceSelection=forceSelection;
    c.forceDeselection=forceDeselection;
    if(value!=null)
        m.put(value, c);
    add(c);
  }
}
