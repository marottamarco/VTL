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
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.log4j.Logger;
import org.sqtds.antlr4.vtl.VtlBaseListener;
import org.sqtds.antlr4.vtl.VtlParser;
import org.sqtds.antlr4.vtl.VtlParser.PutExprContext;
import org.sqtds.antlr4.vtl.VtlParser.SetMemberContext;
import org.sqtds.antlr4.vtl.VtlParser.SetMemberListAliasContext;
import org.sqtds.antlr4.vtl2.Vtl2Parser;

import static org.sqtds.antlr4.vtl.VtlParser.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    	String role="";
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
    			   			
    			rewriter.insertAfter(ctx.getParent().start, String.format(" %s ", a.getText().replace("\"", "").replace(" ","")));
    			rewriter.replace(a.start, "");
    		}else if( item instanceof RoleIDContext ){
    			RoleIDContext a = (RoleIDContext)item;
    			role=a.getText().toLowerCase();
    			rewriter.replace(a.start, "");
    		}
    	}
    	
    	if( null != ctx.ROLE() ){
    		rewriter.replace(ctx.ROLE().getSymbol(), "");
    		rewriter.insertAfter(ctx.getParent().start, String.format(" %s ", role));
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
    	//rewriter.replace(ctx.start, "else if");
    }
    
    /**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterConstant(@NotNull VtlParser.ConstantContext ctx) {
		System.out.println("");
	}
    
    @Override public void exitElseIfExpr(@NotNull VtlParser.ElseIfExprContext ctx) {
    	logger.info("Entrato:"+ctx.ELSEIF().getText());
    }
    
    @Override public void enterDefProcedure(@NotNull VtlParser.DefProcedureContext ctx) {
    	_functionOrProcedure(ctx);
    }

    @Override
    public void enterDefFunction(VtlParser.DefFunctionContext ctx) {
        // create function -> define operator
        _functionOrProcedure(ctx);
    }

    @Override
    public void enterSetMemberList(VtlParser.SetMemberListContext ctx) {
    	
        // in () -> in {}
        // elimina gli eventuali spazi andando a ritroso finchè non trova il token "IN"
        Token in = findPreviousNotWhitespace(tokens, ctx.getStart().getTokenIndex() - 1, "(");
        if (in != null && VtlParser.IN == in.getType() || VtlParser.NOT_IN == in.getType()) {
            // elimina gli eventuali spazi andando in avanti finchè non trova la parentesi
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
        Token             openBrk        = findNextNotWhitespace(tokens, ctx.getStart().getTokenIndex() + 1);
        Token             currentToken   = null;
        Token             terminalToken  = null;
        ParserRuleContext keepItemList   = null;
        ParserRuleContext keepItem       = null;
        List<ParseTree[]> listRename     = new ArrayList<>();
        ParseTree[]       listRenameItem = null;
        List<String>      listCalc       = new ArrayList<>();
        List<String>      listMeasure    = new ArrayList<>();
        List<String>      listGroupby    = new ArrayList<>();
        String            comment        = "";
        String            dataSetName    = "";
        String            itemClause     = ""; // identificatore dell'oggetto con cui costruire
                                               // le clausole
        String            role           = "";
        String            renameClause   = "";
        String            calcClause     = "";
        String            aggrClause     = "";
        String            removeClause   = "";
        String            groupByClause  = "";
        String            finalClause     = "";

        comment = this.tokens.get(0)
                             .getText()
                             .contains("/*")
                                     ? this.tokens.get(0)
                                                  .getText()
                                     : "";
        for (int childCount = this.tokens.size(), count = 1; count < childCount; count++) {
            String nextToken = this.tokens.get(count + 1)
                                          .getText()
                                          .trim();
            if (this.tokens.get(count)
                           .getText()
                           .trim()
                           .equals(":=")) {
                if (nextToken.contains("/*")
                        ||
                        nextToken.isEmpty()) {
                    dataSetName = this.tokens.get(count + 2)
                                             .getText()
                                             .trim();
                } else {
                    dataSetName = nextToken;
                }
                break;
            }
        }
            /*
            dataSetName  = currentToken.getInputStream()
                                       .toString()
                                       .substring(
                                               0,
                                               currentToken.getInputStream()
                                                           .toString()
                                                           .indexOf(":="))
                                       .trim();*/

        // cancellazione delle parentesi
        if (openBrk != null && "(".equals(openBrk.getText())) {
            rewriter.replace(openBrk, " ");
            rewriter.replace(ctx.getStop(), " ");
        }

        if (ctx.setMemberListAlias() != null) {
            keepItemList = ctx.setMemberListAlias();
        } else {
            keepItemList = ctx.setMemberList();
        }

        for (int childCount = keepItemList.getChildCount(), count = 0; keepItemList != null
                && count < childCount; count++) {
            if (keepItemList.getChild(count) instanceof SetMemberContext) {
                keepItem   = (SetMemberContext) keepItemList.getChild(count);
                itemClause = keepItem.start.getText();
                
                // controllo il nodo successivo per capire come trattare il token corrente oppure se è l'ultimo
                if (keepItemList.getChild(count + 1) instanceof TerminalNodeImpl) {
                    terminalToken = ((TerminalNodeImpl) keepItemList.getChild(count + 1)).symbol;
                    if ((terminalToken.getType() == VtlParser.ROLE)) {
                        role = ((RoleIDContext) keepItemList.getChild(count + 2)).start.getText();
                        if (role.equalsIgnoreCase("identifier")) {
                            listCalc.add("identifier " + itemClause + " := " + dataSetName + "#" + itemClause);
                            listGroupby.add(itemClause);
                        }
                        if (role.equalsIgnoreCase("measure")) {
                            listCalc.add("measure " + itemClause + " := " + dataSetName + "#" + itemClause);
                            listMeasure.add(itemClause);
                        }
                    } else if ((terminalToken.getType() == VtlParser.AS)) {
                        listRenameItem    = new ParseTree[2];
                        listRenameItem[0] = keepItem;
                        listRenameItem[1] = keepItemList.getChild(count + 2);
                        listRename.add(listRenameItem);
                        listCalc.add("identifier " + itemClause + " := " + dataSetName + "#" + itemClause);
                        listGroupby.add(itemClause);
                    } else if ((terminalToken.getType() == 70) || keepItemList.getChild(count + 1) == null) {
                        listCalc.add("identifier " + itemClause + " := " + dataSetName + "#" + itemClause);
                        listGroupby.add(itemClause);
                    }

                } else if (keepItemList.getChild(count + 1) == null) {
                    listCalc.add("identifier " + itemClause + " := " + dataSetName + "#" + itemClause);
                    listGroupby.add(itemClause);
                }
            }
        }

        if (listMeasure.isEmpty()) {
            itemClause = "measure TMP_AGGR_MSR := 1";
            listCalc.add(itemClause);
            listMeasure.add("TMP_AGGR_MSR");
            removeClause = "[drop TMP_AGGR_MSR]";
        }

        calcClause = "calc ";
        for (String s : listCalc) {
            calcClause += s + ", ";
        }
        calcClause = calcClause.substring(0, calcClause.length() - 2) + " ]";

        aggrClause = "[aggr ";
        for (String s : listMeasure) {
            aggrClause += s + " := sum(" + dataSetName + "#" + s + "),";
        }
        aggrClause = aggrClause.substring(0, aggrClause.length() - 2) + " )";

        if (!listGroupby.isEmpty()) {
            groupByClause = "group by ";
            for (String s : listGroupby) {
                groupByClause += s + ", ";
            }
            groupByClause = groupByClause.substring(0, groupByClause.length() - 2) + "]";
        }

        if (!listRename.isEmpty()) {
            renameClause = "[rename ";
            for (ParseTree[] item : listRename) {

                /*
                 * la clausola "Rename" viene costruita con i due componenti che circondavano il
                 * token "AS", ciascuno dei quali va gestito due volte perchè possono essere di
                 * due tipi diversi
                 */
                if (item[0] instanceof VtlParser.StringCContext) {
                    renameClause += ((VtlParser.StringCContext) item[0]).start.getText()
                                                                              .replace("\"", "")
                            + " to ";
                }
                if (item[0] instanceof VtlParser.SetMemberContext) {
                    renameClause += ((VtlParser.SetMemberContext) item[0]).start.getText() + " to ";
                }
                if (item[1] instanceof VtlParser.StringCContext) {
                    renameClause += ((VtlParser.StringCContext) item[1]).start.getText()
                                                                              .replace("\"", "")
                            + ", ";
                }
                if (item[1] instanceof VtlParser.SetMemberContext) {
                    renameClause += ((VtlParser.SetMemberContext) item[1]).start.getText() + ", ";
                }
            }
            renameClause = renameClause.substring(0, renameClause.length() - 2) + "]";
        }

        finalClause = comment + calcClause + aggrClause + groupByClause + renameClause + removeClause;

        if (!finalClause.isEmpty()) {
            currentToken = ctx.KEEP()
                              .getSymbol();
            rewriter.replace(
                    ctx.getStart()
                       .getTokenIndex(),
                    ctx.getStop()
                       .getTokenIndex(),
                    finalClause.substring(0, finalClause.length() - 1));
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
		
		if (nodeImpl.symbol.getText().equals("elseif")) {
			rewriter.replace(nodeImpl.symbol, "else if");
		}
		
		if (nodeImpl.symbol.getText().equals("define procedure")) {
			rewriter.replace(nodeImpl.symbol, "define operator ");
		}
		
		switch (this.currentClause) {
		case "enterFunctionCall":
			rewriter.replace(nodeImpl.symbol, nodeImpl.symbol.getText().replaceAll("\"", ""));
			break;
		case "enterJoinAtom":
			if (nodeImpl.symbol.getText().equals("[") || nodeImpl.symbol.getText().equals("]")) {
				rewriter.replace(nodeImpl.symbol, "");
			}
			break;
		case "enterJoinClause":
			if (nodeImpl.symbol.getText().equals("left")){
				rewriter.replace(nodeImpl.symbol, "");
			}
			if (nodeImpl.symbol.getText().equals("inner")){
				rewriter.replace(nodeImpl.symbol, "");
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
		int ctxLength=(null!=ctx.setMemberListAlias())?ctx.setMemberListAlias().getChildCount():0;
		Object currentTk=null;
		TerminalNodeImpl currentNodeImpl=null;
		
		for (int i = 0; i < ctxLength; i++) {
			currentTk=ctx.setMemberListAlias().getChild(i);
			if (currentTk instanceof VtlParser.RoleIDContext) {
				rewriter.delete(((VtlParser.RoleIDContext) currentTk).start);
			}
			else if (currentTk instanceof TerminalNodeImpl) {
				currentNodeImpl= (TerminalNodeImpl) currentTk;
				if (currentNodeImpl.symbol.getType()==VtlParser.AS || currentNodeImpl.symbol.getType()==VtlParser.ROLE) {
					rewriter.delete(currentNodeImpl.symbol);
					if (ctx.setMemberListAlias().getChild(i+1) instanceof VtlParser.StringCContext)
                    {
						currentTk=ctx.setMemberListAlias().getChild(i+1);
						rewriter.delete(((VtlParser.StringCContext) currentTk).start);
                    }
					
				}
			}
		}
		
		for (ParseTree item : ctx.children) {
		List<Token> tokens=getFlatTokenList(item);
		for (Token currentToken : tokens) {
			//rewriter.replace(currentToken, currentToken.getText().replace("\"", ""));
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
	@Override public void enterJoinExpr(@NotNull VtlParser.JoinExprContext ctx) { 
		System.out.println(" >> enterJoinExpr");
		this.currentClause="enterJoinExpr";
		TerminalNodeImpl a=null;
		
		for (ParseTree item : ctx.children) {
			if (item instanceof TerminalNodeImpl) {
				
				 if (((TerminalNodeImpl) item).symbol.getText().equals("["));
				 {
					 a= (TerminalNodeImpl) item;
				 }
				
			}
			if (a!=null && item instanceof VtlParser.JoinClauseContext) {
				
				for (ParseTree i : ((VtlParser.JoinClauseContext) item).children) {
					if (i instanceof TerminalNodeImpl) {
					 if (((TerminalNodeImpl) i).symbol.getText().equals("inner"))
					 {
						 rewriter.replace(a.symbol, "inner_join( " );
					 }
					 else if (((TerminalNodeImpl) i).symbol.getText().equals("left")) {
						 rewriter.replace(a.symbol, "left_join( " );
					}}
					else if(i instanceof VtlParser.DatasetAliasContext) {
						rewriter.replace(a.symbol, "inner_join( " );
					}
				}
			}
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
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override
	public void enterFunctionCall(@NotNull VtlParser.FunctionCallContext ctx) {
		System.out.println(" >> enterFunctionCall");
		this.currentClause="enterFunctionCall";
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override
	public void exitFunctionCall(@NotNull VtlParser.FunctionCallContext ctx) {
		System.out.println(" >> exitFunctionCall");
		this.currentClause="";
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterProcedureCall(@NotNull VtlParser.ProcedureCallContext ctx) {
		System.out.println(" >> enterProcedureCall");
		rewriter.replace(ctx.start, "");
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterStringC(@NotNull VtlParser.StringCContext ctx) {
		System.out.println(" >> enterStringC");
		
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterSumExprAtom(@NotNull VtlParser.SumExprAtomContext ctx) {
		System.out.println(" >> enterSumExprAtom");
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterExprMember(@NotNull VtlParser.ExprMemberContext ctx) { 
		System.out.println(" >> enterExprMember");
		boolean checkAggregateFunction=false;
		
		for (ParseTree item : ctx.children) {
			if ((item instanceof VtlParser.SumExprAtomContext) || (item instanceof VtlParser.MaxAtomContext)) {
				checkAggregateFunction=true;
				if (item instanceof VtlParser.SumExprAtomContext) {
					rewriter.replace(((VtlParser.SumExprAtomContext) item).stop, "");
				}else if (item instanceof VtlParser.MaxAtomContext) {
					rewriter.replace(((VtlParser.MaxAtomContext) item).stop, "");
				}
			}
			if ((checkAggregateFunction) && (item instanceof ErrorNodeImpl)) {
				ErrorNodeImpl a = (ErrorNodeImpl) item;
				if (a.symbol.getText().equals("(")) {rewriter.replace(a.symbol, "");}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	@Override public void enterDropClause(@NotNull VtlParser.DropClauseContext ctx) {
		System.out.println(" >> enterDropClause");

		for (ParseTree item : ctx.children) {
			if (item instanceof TerminalNodeImpl) {
				TerminalNodeImpl a = (TerminalNodeImpl) item;
				if (a.symbol.getText().equals("(")||a.symbol.getText().equals(")")) {
					rewriter.replace(a.symbol, " ");
				}
			}
		}
		
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
    
    private void _functionOrProcedure(ParserRuleContext ctx) {
    	VtlParser.DefFunctionContext ctxFun=null;
    	VtlParser.DefProcedureContext ctxProc=null;
    	Token openBracket = null;
    	Token semiComma = null;
    	
    	rewriter.replace(ctx.start, "define operator ");
    	
    	if (ctx instanceof VtlParser.DefFunctionContext) {
    		ctxFun = (VtlParser.DefFunctionContext)ctx;
    		
    		if (ctxFun.RETURNS() != null) {
                Token retType = findNextNotWhitespace(tokens, ctxFun.AS().getSymbol().getTokenIndex() + 1);
                rewriter.insertBefore(ctxFun.expr().start, String.format("%s %s %s ", ctxFun.RETURNS().getText(), retType.getText(), getTokenName(IS)));
                rewriter.delete(ctxFun.RETURNS().getSymbol());
                rewriter.delete(ctxFun.AS().getSymbol());
                rewriter.delete(retType);
            } else {
                rewriter.insertBefore(ctxFun.expr().start, String.format(" %s ", getTokenName(IS)));
            }
    		for (VtlParser.ArgContext arg : ctxFun.argList().arg()) {
                // TODO: verificare eventuale lettura tipi dal dominio invece di indentifier
                rewriter.insertAfter(arg.stop, String.format(" %s", "scalar"));
            }
    		openBracket = findNextNotWhitespace(tokens, ctxFun.argList().getStop().getTokenIndex() + 2);
    		rewriter.delete(ctxFun.AS().getSymbol());
    	}
    	else if (ctx instanceof VtlParser.DefProcedureContext) {
    		ctxProc= (VtlParser.DefProcedureContext)ctx;
    		
    			rewriter.insertBefore(ctxProc.procedureBody().start, String.format(" %s ", getTokenName(IS)));
                
                for (VtlParser.ProcedureArgContext arg : ctxProc.procedureArgList().procedureArg()) {
                    // TODO: verificare eventuale lettura tipi dal dominio invece di indentifier
                    //rewriter.insertAfter(arg.stop, String.format(" %s", getTokenName(IDENTIFIER)));
                    if (arg.INPUT() != null) {
                    	rewriter.delete(arg.INPUT().getSymbol());
					}
                    rewriter.delete(arg.AS().getSymbol());
                    if (arg.OUTPUT() != null) {
                    	rewriter.delete(arg.OUTPUT().getSymbol());
					}
                }
                openBracket = findNextNotWhitespace(tokens, ctxProc.procedureArgList().getStop().getTokenIndex() + 2);
                semiComma = findPreviousNotWhitespace(tokens, ctx.getStop().getTokenIndex()-1);
                if (semiComma.getText().equals(";")) {
                	rewriter.delete(semiComma);					
				}
		}
    	
        
        if (openBracket != null)
            {rewriter.delete(openBracket);}
        rewriter.replace(ctx.stop, String.format(" %s %s", getTokenName(END), getV2TokenName(Vtl2Parser.OPERATOR)));
    }
    
}
