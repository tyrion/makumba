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

package org.makumba.devel;
import org.makumba.*;
import org.makumba.abstr.*;
import java.io.*;
import javax.servlet.http.*;

public class mddViewer extends LineViewer
{
  MakumbaError err;

  public mddViewer(HttpServletRequest req) throws Exception
  {
    super(true);
    virtualPath=req.getPathInfo();
    contextPath=req.getContextPath();
    if(virtualPath==null)
      virtualPath="/";
    java.net.URL u= org.makumba.abstr.RecordParser.findDataDefinition(virtualPath, "mdd");
    if(u==null)
      u= org.makumba.abstr.RecordParser.findDataDefinition(virtualPath, "idd");
    readFromURL(u);
    virtualPath= virtualPath.substring(1);
  }

  String closeLine;
  
  void intro(PrintWriter w)
  {
    try{
      RecordInfo.getRecordInfo(virtualPath);
    }catch(DataDefinitionNotFoundError nf) { 
      // FIXME: this is probably an include, we ignore it alltogether
    }
    catch(MakumbaError pe){
      err=pe;
      w.print("<td align=\"center\"><font color=\"red\">errors!<br><a href=#errors>details</a></font></td>");
      
    }
    w.print("<td align=\"center\" bgcolor=\"darkblue\"><font color=\"lightblue\">mdd</font></td>");
    w.print("<td align=\"center\"><font color=\"darkblue\">data</font></td>");
  }

  void footer(PrintWriter pw) throws IOException 
  {
    if(err!=null)
      pw.println("<a name=errors><hr><pre>"+err.getMessage()+"</pre>");
  } 

  void parseLine(String s)
  {
    closeLine="";
    super.parseLine(s);
    highlighted.append(closeLine);
  }

  String getLineTag(String s)
  {
    String ln=s.trim();
    int eq;
    if(!ln.startsWith("#") && !ln.startsWith("!") && !ln.startsWith("=")&&
       (eq=ln.indexOf('='))!=-1)
      return ln.substring(0, eq).trim();
    return null;
  }

  void printLine(PrintWriter w, String s) throws IOException
  {
    if(err!=null)
      {
	// we go thru the error text, if we find this particular line, we display its error message
	// this is a hack, it should rather go thru the multiple exceptions
	LineNumberReader lr= new LineNumberReader(new StringReader(err.getMessage()));
	String e=null;
	String before=null;
	while(true)
	  {
	    before=e;
	    e=lr.readLine();
	    if(e==null)
	      break;
	    if(e.length()>0 && e.equals(s))
	      {
		w.print("<span style=\"background:#cc0000\">");
		super.printLine(w, s);
		w.print("</span>\t<font color=red>"+
			lr.readLine()+" "+before.substring(before.indexOf(':')+2)+"</font>\r\n");
		return;
	      }
	  }
      }
    super.printLine(w, s);
  }

  void treat()
  {
    switch(current)
      {
      case '=': 
	highlighted.append("=<font color=\"#000077\">");
	closeLine="</font>"+closeLine;
	break;
      case '#': 
	highlighted.append("<span style=\"background:#eeeeee\"><i>#");
	closeLine="</i></span>"+closeLine;
	break;
      case ';': 
	highlighted.append("<font color=\"green\">;");
	closeLine="</font>"+closeLine;
	break;
      default:
	super.treat();
      }
  }
}
