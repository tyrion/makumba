/*
* generated by Xtext
*/
package org.makumba.devel.eclipse.mdd.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class MDDAntlrTokenFileProvider implements IAntlrTokenFileProvider {
	
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
    	return classLoader.getResourceAsStream("org/makumba/devel/eclipse/mdd/parser/antlr/internal/InternalMDD.tokens");
	}
}
