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

package org.makumba.util;
import java.util.*;

/** An instance of this class holds a cache of resources. If a resource is requested but is not present, it is produced using the associated NamedResourceFactory, in a thread-safe way 
 *@see org.makumba.util.NamedResourceFactory
*/
public class NamedResources implements java.io.Serializable 
{
  static boolean soft_static_caches;
  static
  {
    String soft=null;
    try{ 
      soft= System.getProperty("makumba.soft-static-caches");      
    }catch(SecurityException se) {} // for applets
    if(soft!=null)
      soft=soft.trim();
    soft_static_caches="true".equals(soft);
  }

  NamedResourceFactory f;
    String name;
  Map values= new HashMap();

  static List staticCaches= new ArrayList();

    static Vector allCaches= new Vector();

    static void cleanup()
    {
	for(int i=0;i<staticCaches.size(); i++)
	    ((NamedResources)staticCaches.get(i)).close();
	staticCaches.clear();
	for(int i=0;i<allCaches.size(); i++)
	    ((java.lang.ref.WeakReference)allCaches.elementAt(i)).clear();
	allCaches.clear();

    }
  public synchronized static int makeStaticCache(String name,
						 NamedResourceFactory fact)
  {
    staticCaches.add(soft_static_caches?new SoftNamedResources(name, fact):
		     new NamedResources(name, fact));
    return staticCaches.size()-1;
  }

  public static NamedResources getStaticCache(int n)
  {
    return (NamedResources)staticCaches.get(n);
  }

    public static Map getCacheInfo()
    {
	Map m= new HashMap();
	for(int i=0; i<allCaches.size(); i++)
	    {
		NamedResources nr=(NamedResources)
		    ((java.lang.ref.WeakReference)allCaches.elementAt(i)).get();
		if(nr==null)
		    continue;
		Integer n=(Integer)m.get(nr.name);
		if(n==null)
		    m.put(nr.name, new Integer(nr.values.size()));
		else
		    m.put(nr.name, new Integer(nr.values.size()+n.intValue()));
	    }
	return m;
    }

  /** initialize, using the given factory */
  public NamedResources(String name, NamedResourceFactory f) 
    {
	this.name=name; this.f= f;
	allCaches.addElement(new java.lang.ref.WeakReference(this));
    }

  public boolean knowResource(Object name)
  {
    try{
      return values.get(f.getHashObject(name))!=null;
    }catch(Throwable t)
      {
	throw new RuntimeWrappedException(t);
      }
  }
 
  /** whatever supplementary stuff the factory wants to keep */
  public Object getSupplementary(){ return f.supplementary; }

  /** return the resource, if it doesn't exist, call the NamedResourceFactory to produce it */
  public Object getResource(Object name)
  {
    NameValue nv= null;

    synchronized(this)
      {
	Object hash=null;
	try{
	  hash= f.getHashObject(name);
	}catch(Throwable t)
	  {
	    throw new RuntimeWrappedException(t);
	  }
	nv=getNameValue(name, hash);
      }
    return nv.getResource();
  }

  protected NameValue getNameValue(Object name, Object hash)
  {
    NameValue nv= (NameValue)values.get(hash);
    if(nv==null)
      values.put(hash, nv=new NameValue(name, hash, f));
    return nv;
  }
  
  /** close each contained object by calling its close() method, if any */
  protected void close(){
    for(Iterator i= values.keySet().iterator(); i.hasNext(); )
      {
	NameValue nv= (NameValue)values.get(i.next());
	Object o= nv.getResource();

	java.lang.reflect.Method m= null;
	if(o==null)
	  continue;
	try{
  	  m=o.getClass().getMethod("close", null);
	}catch(NoSuchMethodException e){ 
	  // we assume homogenous caches
	  return; 
	}
	try{
	  m.invoke(o, null);
	}catch(Throwable t){ t.printStackTrace(); }
      }
    values.clear();
  }
}



