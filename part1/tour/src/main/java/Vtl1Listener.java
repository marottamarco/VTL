/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 ***/


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.log4j.Logger;
import org.sqtds.antlr4.vtl.VtlBaseListener;
import org.sqtds.antlr4.vtl.VtlParser;
import org.sqtds.antlr4.vtl.VtlParser.PutExprContext;
import org.sqtds.antlr4.vtl2.Vtl2Parser;

import static org.sqtds.antlr4.vtl.VtlParser.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Vtl1Listener extends VtlBaseListener {
    TokenStream tokens;
    TokenStreamRewriter rewriter;
    
    private Logger logger = Logger.getLogger(Vtl1Listener.class);
    
    private String currentClause="";
    
    public Vtl1Listener(TokenStream tokens) {
        this.tokens = tokens;
        rewriter = new TokenStreamRewriter(tokens);
    }
    
}
