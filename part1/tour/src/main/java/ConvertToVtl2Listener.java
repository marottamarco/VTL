/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 ***/


import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.NotNull;
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
import java.util.List;

public class ConvertToVtl2Listener extends VtlBaseListener {
    TokenStream tokens;
    TokenStreamRewriter rewriter;
    
    private Logger logger = Logger.getLogger(ConvertToVtl2Listener.class);
    
    private String currentClause="";
    
    public ConvertToVtl2Listener(TokenStream tokens) {
        this.tokens = tokens;
        rewriter = new TokenStreamRewriter(tokens);
    }
    
    @Override public void enterCalcClauseItem(@NotNull VtlParser.CalcClauseItemContext ctx) {
    	System.out.println("ConvertToVtl2Listener.enterCalcClauseItem");
    	
    	List<ParseTree> childrens = ctx.children;
    	for( ParseTree item : childrens ){
    		if(item instanceof CalcExprContext){
    			CalcExprContext a = (CalcExprContext) item;
    			
    		}else if( item instanceof TerminalNodeImpl ){
    			TerminalNodeImpl a = (TerminalNodeImpl)item;
    			if( a.symbol.getText().equals(getTokenName(AS)) ){
    				rewriter.replace(a.symbol, "");
    			}
    		}else if( item instanceof StringCContext ){
    			StringCContext a = (StringCContext)item;
    			   			
    			rewriter.insertAfter(ctx.getParent().start, String.format(" %s ", a.getText().replace("\"", "")));
    			rewriter.replace(a.start, "");
    		}else if( item instanceof RoleIDContext ){
    			RoleIDContext a = (RoleIDContext)item;
    			
    			rewriter.replace(a.start, "");
    		}
    	}
    	
    	rewriter.insertAfter(ctx.getParent().start, String.format(" %s ", getV2TokenName(Vtl2Parser.MEASURE)));
    	if( null != ctx.ROLE() ){
    		rewriter.replace(ctx.ROLE().getSymbol(), "");
    	}
    	rewriter.insertBefore(ctx.start, String.format(" %s ", getV2TokenName(Vtl2Parser.ASSIGN)));
    	
    }
    
	@Override public void enterRenameClause(@NotNull VtlParser.RenameClauseContext ctx) { 
		System.out.println("ConvertToVtl2Listener.enterRenameClauseItem");
		List<ParseTree> childrens = ctx.children;
		iterateTree(childrens);
		System.out.println("ConvertToVtl2Listener.exitRenameClauseItem");
	}
    
    @Override
    public void enterPutExpr(PutExprContext ctx) {
    	Token stop=null;
    	Token start=null;
    	TerminalNodeImpl comma=null;
    	
    	System.out.println("ConvertToVtl2Listener.enterPutExpr");
    	
    	// remove assignment variable
    	rewriter.replace(tokens.get(1), " ");
    	// remove assignment op and surrounding spaces
    	rewriter.replace(tokens.get(2), " ");
    	rewriter.replace(tokens.get(3), " ");
    	rewriter.replace(tokens.get(4), " ");

    	rewriter.replace(ctx.getStart(), " ");
    	    	
    	for (ParseTree item : ctx.children) {
			if (item instanceof TerminalNodeImpl) {
				TerminalNodeImpl a = (TerminalNodeImpl) item;
				if (a.symbol.getText().equals("put") || a.symbol.getText().equals("(") || a.symbol.getText().equals(")")) {
					rewriter.replace(a.symbol, "");
				}
			} else if (item instanceof VtlParser.PutInputParametersContext) {
				comma= (TerminalNodeImpl) ((VtlParser.PutInputParametersContext) item).children.get(1);
				rewriter.replace(comma.symbol, "");
				start=((VtlParser.PutInputParametersContext) item).start;
				stop=((VtlParser.PutInputParametersContext) item).stop;
				rewriter.insertAfter(start,
		    			" <- ");
				rewriter.replace(stop,
		    			stop.getText().replace("\"", ""));
			} 
		}
    	
    	System.out.println("ConvertToVtl2Listener.exitPutExpr");
	}

	@Override public void exitCalcClauseItem(@NotNull VtlParser.CalcClauseItemContext ctx) {
		System.out.println("ConvertToVtl2Listener.exitCalcClauseItem");
		System.out.println("");
	}
    
    @Override public void enterElseIfExpr(@NotNull VtlParser.ElseIfExprContext ctx) {
    	rewriter.replace(ctx.start, "else if");
    }
    
    @Override public void exitElseIfExpr(@NotNull VtlParser.ElseIfExprContext ctx) {
    	logger.info("Entrato:"+ctx.ELSEIF().getText());
    }

    @Override
    public void enterDefFunction(VtlParser.DefFunctionContext ctx) {
        // create function -> define operator
        rewriter.replace(ctx.start, "define operator ");
        if (ctx.RETURNS() != null) {
            Token retType = findNextNotWhitespace(tokens, ctx.AS().getSymbol().getTokenIndex() + 1);
            rewriter.insertBefore(ctx.expr().start, String.format("%s %s %s ", ctx.RETURNS().getText(), retType.getText(), getTokenName(IS)));
            rewriter.delete(ctx.RETURNS().getSymbol());
            rewriter.delete(ctx.AS().getSymbol());
            rewriter.delete(retType);
        } else {
            rewriter.insertBefore(ctx.expr().start, String.format(" %s ", getTokenName(IS)));
        }

        for (VtlParser.ArgContext arg : ctx.argList().arg()) {
            // TODO: verificare eventuale lettura tipi dal dominio invece di indentifier
            rewriter.insertAfter(arg.stop, String.format(" %s", getTokenName(IDENTIFIER)));
        }

        Token openBracket = findNextNotWhitespace(tokens, ctx.argList().getStop().getTokenIndex() + 2);
        if (openBracket != null)
            rewriter.delete(openBracket);
        rewriter.replace(ctx.stop, String.format(" %s %s", getTokenName(END), getV2TokenName(Vtl2Parser.OPERATOR)));

        rewriter.delete(ctx.AS().getSymbol());
    }

    @Override
    public void enterSetMemberList(VtlParser.SetMemberListContext ctx) {
        // in () -> in {}
        // elimina gli eventuali spazi andando a ritroso finché non trova il token "IN"
        Token in = findPreviousNotWhitespace(tokens, ctx.getStart().getTokenIndex() - 1, "(");
        if (in != null && VtlParser.IN == in.getType() || VtlParser.NOT_IN == in.getType()) {
            // elimina gli eventuali spazi andando in avanti finché non trova la parentesi
            Token openBrk = findNextNotWhitespace(tokens, in.getTokenIndex() + 1);
            if ("(".equals(openBrk.getText())) {
                rewriter.replace(openBrk, "{");
//                Token closeBrk = tokens.get(ctx.getStop().getTokenIndex()+1);
                Token closeBrk = findNextNotWhitespace(tokens, ctx.getStop().getTokenIndex() + 1);
                if (")".equals(closeBrk.getText())) {
                    rewriter.replace(closeBrk, "}");
                }
            }
        }
    }

    @Override
    public void enterKeepClause(KeepClauseContext ctx) {
        // keep(a, b... -> keep a, b
        Token openBrk = findNextNotWhitespace(tokens, ctx.getStart().getTokenIndex() + 1);
        Token currentToken = null;
        
        if (openBrk != null && "(".equals(openBrk.getText())) {
            rewriter.replace(openBrk, " ");
            rewriter.replace(ctx.getStop(), " ");
        }
        
        //remove double quotes, as
		for (int count = 1; count < tokens.size(); count++) {
			if (null != tokens.get(count)) {
				if (tokens.get(count).getText().equals("as")) {
					rewriter.replace(tokens.get(count), ", ");
				}
				if (tokens.get(count).getText().startsWith("\"") || tokens.get(count).getText().endsWith("\"")) {
					rewriter.replace(tokens.get(count), tokens.get(count).getText().replaceAll("^\"|\"$", ""));
				}
			}
		}

        // remove role
        for (int i = 0; ctx.setMemberListAlias() != null && i < ctx.setMemberListAlias().getChildCount(); ++i) {
            if (ctx.setMemberListAlias().getChild(i) instanceof TerminalNodeImpl) {
            	currentToken=((TerminalNodeImpl) ctx.setMemberListAlias().getChild(i)).symbol;
                if ((currentToken.getType() == VtlParser.ROLE)) {
                    rewriter.delete(currentToken);
                }
            }
            if (ctx.setMemberListAlias().getChild(i) instanceof VtlParser.RoleIDContext) {
                rewriter.delete(((RoleIDContext) ctx.setMemberListAlias().getChild(i)).start);
            }
        }
    }

    @Override
    public void enterKeepClauseItem(KeepClauseItemContext ctx) {
        super.enterKeepClauseItem(ctx);
    }

    private String getTokenName(int tok) {
        return VtlParser.tokenNames[tok].replaceAll("'", "");
    }

    private String getV2TokenName(int tok) {
        return Vtl2Parser.tokenNames[tok].replaceAll("'", "");
    }

    private Token findPreviousNotWhitespace(TokenStream tokens, int startIdx) {
        return findPreviousNotWhitespace(tokens, startIdx, null);
    }

    private Token findPreviousNotWhitespace(TokenStream tokens, int startIdx, String excludeAlso) {
        while (startIdx >= 0) {
            if (!" ".equals(tokens.get(startIdx).getText())
                    && (excludeAlso == null || !excludeAlso.equals(tokens.get(startIdx).getText()))) {
                return tokens.get(startIdx);
            }
            startIdx--;
        }
        return null;
    }

    private Token findNextNotWhitespace(TokenStream tokens, int startIdx) {
        while (startIdx < tokens.size()) {
            if (!" ".equals(tokens.get(startIdx).getText())) {
                return tokens.get(startIdx);
            }
            startIdx++;
        }
        return null;
    }
    
    /**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void visitTerminal(@NotNull TerminalNode node) {
		System.out.println(" >> visitTerminal");
		TerminalNodeImpl nodeImpl = (TerminalNodeImpl) node;
		
		switch (this.currentClause) {
		case "enterJoinAtom":
			if (nodeImpl.symbol.getText().equals("[") || nodeImpl.symbol.getText().equals("]")) {
				rewriter.replace(nodeImpl.symbol, "");
			}
			break;
		case "enterJoinClause":
			if (nodeImpl.symbol.getText().equals("left")){
				rewriter.replace(nodeImpl.symbol, "left_join (");
			}
			if (nodeImpl.symbol.getText().equals("inner")){
				rewriter.replace(nodeImpl.symbol, "inner_join (");
			}
			if (nodeImpl.symbol.getText().equals("]")){
				rewriter.replace(nodeImpl.symbol, "");
			}
			if (nodeImpl.symbol.getText().equals("{")){
				rewriter.replace(nodeImpl.symbol, "");
			}
			if (nodeImpl.symbol.getText().contains("\"")){
				rewriter.replace(nodeImpl.symbol, nodeImpl.symbol.getText().replaceAll("\"", ""));
			}
			break;
		case "enterJoinKeepClause":
			if (nodeImpl.symbol.getText().equals("(") || nodeImpl.symbol.getText().equals(")")) {
				rewriter.replace(nodeImpl.symbol, "");
			}
			if (nodeImpl.symbol.getText().equals("}")){
				rewriter.replace(nodeImpl.symbol, "");
			}
			break;
		}
				
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJoinKeepClause(@NotNull VtlParser.JoinKeepClauseContext ctx) { 
		System.out.println(" >> enterJoinKeepClause");
		this.currentClause="enterJoinKeepClause";
		for (ParseTree item : ctx.children) {
		List<Token> tokens=getFlatTokenList(item);
		for (Token currentToken : tokens) {
			rewriter.replace(currentToken, currentToken.getText().replace("\"", ""));
		}}
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override
	public void enterJoinClause(@NotNull VtlParser.JoinClauseContext ctx) {
		System.out.println(" >> enterJoinClause");
		this.currentClause = "enterJoinClause";
		VtlParser.ExprOrExprContext leftOnClause = null;
		String s = "";

		ParseTree itemCurrent = null;
		for (ParseTree item : ctx.children) {
			if ((itemCurrent instanceof TerminalNodeImpl)) {
				TerminalNodeImpl a = (TerminalNodeImpl) itemCurrent;
				if (a.symbol.getText().equals("on")) {
					if (item instanceof VtlParser.ExprOrExprContext) {
						leftOnClause = (VtlParser.ExprOrExprContext) item;
						s = leftOnClause.getStop().getText();
						List<Token> tokens=getFlatTokenList(item);
						for (Token currentToken : tokens) {
							rewriter.replace(currentToken, "");
						}
						
					}
					rewriter.replace(a.symbol, "using " + s);
				}
			}
			itemCurrent = item;
		}
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterJoinAtom(@NotNull VtlParser.JoinAtomContext ctx) { 
		System.out.println(" >> enterJoinAtom");
		this.currentClause="enterJoinAtom";
		
		rewriter.insertAfter(((VtlParser.JoinAtomContext) ctx.getPayload()).getStop(),")");
	}
	
	private static List<Token> getFlatTokenList(ParseTree tree) {
	    List<Token> tokens = new ArrayList<Token>();
	    inOrderTraversal(tokens, tree);
	    return tokens;
	}
    
	private static void inOrderTraversal(List<Token> tokens, ParseTree parent) {

	    // Iterate over all child nodes of `parent`.
	    for (int i = 0; i < parent.getChildCount(); i++) {

	        // Get the i-th child node of `parent`.
	        ParseTree child = parent.getChild(i);

	        if (child instanceof TerminalNode) {
	            // We found a leaf/terminal, add its Token to our list.
	            TerminalNode node = (TerminalNode) child;
	            tokens.add(node.getSymbol());
	        }
	        else {
	            // No leaf/terminal node, recursively call this method.
	            inOrderTraversal(tokens, child);
	        }
	    }
	}
	
    private void iterateTree(List<ParseTree> childrens) {
		Token stop=null;
		
		for (ParseTree item : childrens) {
			if (item instanceof TerminalNodeImpl) {
				TerminalNodeImpl a = (TerminalNodeImpl) item;
				if (a.symbol.getText().equals(getTokenName(AS))) {
					rewriter.replace(a.symbol, "to");
				} else if (a.symbol.getText().equals("(") || a.symbol.getText().equals(")")) {
					rewriter.replace(a.symbol, "");
				}}
			else if (item instanceof VtlParser.RenameArgListContext) {
				stop=((VtlParser.RenameArgListContext) item).stop;
				rewriter.replace(stop,
						stop.getText().replace("\"", ""));
				iterateTree(((VtlParser.RenameArgListContext) item).children);
			} else if (item instanceof VtlParser.RenameArgContext) {
				iterateTree(((VtlParser.RenameArgContext) item).children);
			} else if (item instanceof VtlParser.RulesetArgContext) {
				iterateTree(((VtlParser.RulesetArgContext) item).children);
			}
		}
	}
    
}
