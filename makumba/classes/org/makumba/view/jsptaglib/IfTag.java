///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.view.jsptaglib;
import org.makumba.LogicException;

import org.makumba.util.MultipleKey;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspException;


/**
 * If tag will accept test="..." similar to value tag, and will show body only if OQL expression evaluates to true (integer 1).
 * @author fred
 * @since makumba-0.5.9.11
 */

public class IfTag extends MakumbaTag implements BodyTag
{
  String testExpr;
  private static final Integer TRUE_INT = new Integer(1);
  
  
  public void setTest(String s) { this.testExpr = s; }

  // these 2 are required to implement BodyTag, no action needed inside.
  public void setBodyContent(BodyContent bc) { }        
  public void doInitBody() { }                          
  
  /** cleanup the state to make this reusable */
  public void release()
  {
    super.release();
    testExpr = null;
  }
  
  /** Set tagKey to uniquely identify this tag. Called at analysis time before doStartAnalyze() and at runtime before doMakumbaStartTag() */
  public void setTagKey()
  {
    addToParentListKey(testExpr.trim());
  }

  /** determine the ValueComputer and associate it with the tagKey */
  public void doStartAnalyze()
  {
    pageCache.valueComputers.put(tagKey, ValueComputer.getValueComputer(this, testExpr));
  }
  
  /** tell the ValueComputer to finish analysis, and set the types for var and printVar */
  public void doEndAnalyze()
  {
    ValueComputer vc= (ValueComputer)pageCache.valueComputers.get(tagKey);
    vc.doEndAnalyze(this);
  }
  
  /** ask the ValueComputer to calculate the expression, and SKIP_BODY if false */
  public int doMakumbaStartTag() throws JspException, org.makumba.LogicException
  {
    Object exprvalue = ((ValueComputer)getPageCache(pageContext).valueComputers.get(tagKey)).getValue(this);


    if ( exprvalue instanceof Integer) {
       int i= ((Integer) exprvalue).intValue();
       if (i == 1) { 
           return EVAL_BODY_INCLUDE;
       } else if (i == 0){
           return SKIP_BODY;
       } 

       // integer value other than 1/0
       throw new MakumbaJspException(this, "test expression in mak:if should result in 0 or 1; result is " + exprvalue);
    }

    // comparison with null, will return null, equivalent to false 
    if (exprvalue == null) return SKIP_BODY;

    // return value is another type
    throw new MakumbaJspException(this, "test expression in mak:if should result in an Integer, result is "+ exprvalue);
    
  }

  /* FIXME: what should this output? */
  public String toString() { 
    return "IF test="+testExpr+ 
      " parameters: "+ params; 
  }
}
