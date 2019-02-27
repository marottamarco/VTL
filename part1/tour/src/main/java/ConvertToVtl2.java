/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 ***/


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.log4j.Logger;
import org.sqtds.antlr4.vtl.VtlLexer;
import org.sqtds.antlr4.vtl.VtlParser;
import org.sqtds.antlr4.vtl2.Vtl2Lexer;
import org.sqtds.antlr4.vtl2.Vtl2Parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ConvertToVtl2 {
    static class ConversionResult {
        // id della trasformazione originale
        String id;
        // expression originale
        String original;
        // expression tradotta
        String converted;
        // esito ed eventuale exception
        boolean success;
        String exceptionMsg;
        

        public ConversionResult(String id, String original, String converted, boolean success, String exceptionMsg) {
            this.id = id;
            this.original = original;
            this.converted = converted;
            this.success = success;
            this.exceptionMsg = exceptionMsg;
        }
    }

    static class ParseResult {
        // id della trasformazione originale
        String id;
        // expression originale
        String original;
        // expression parsata
        String parsed;
        // esito ed eventuale exception
        boolean success;
        String exceptionMsg;

        public ParseResult(String id, String original, String parsed, boolean success, String exceptionMsg) {
            this.id = id;
            this.original = original;
            this.parsed = parsed;
            this.success = success;
            this.exceptionMsg = exceptionMsg;
        }
    }
    private static Logger logger = Logger.getLogger(ConvertToVtl2.class);

    public static void main(String[] args) throws Exception {
        ConversionResult convertedExpr = null;
        ParseResult parsedV2Expr = null;
        boolean test=false;

        // singola espressione di test da convertire
        String expression;

//        expression = "out := dd1 [filter(y in (\"4\", \"5\"))];";

//        expression = "SCRTSTNS_SRVCRS_E := TRNSCTNS_CNTRPRTS [filter(TYP_TRNSCTN in (\"4\", \"5\") and CNTRPRTY_RL in (\"3\"))];";

        expression = "create function D_ACCMLTD_NGTV_CHNGS_FV_CR (ACCMLTD_CHNGS_FV_CR) {\n" +
                "returns \n" +
                "if ACCMLTD_CHNGS_FV_CR < 0 then ACCMLTD_CHNGS_FV_CR else 0\n" +
                "as integer\n" +
                "}";

//        expression = "/*Application of hierarchies and derivation of combinations for the given input dataset*/create function APPLY_HIERARCHIES_AND_AGGREGATION(INPT_CB) {\n" +
//                "returns \n" +
//                "hierarchiesAndAggregation(INPT_CB)\n" +
//                "as dataset\n" +
//                "}";

//        expression = "BT_SCRTS_DBTRS_PRTCTNS_FINREP := DBT_SCRTS_DBTRS_PRTCTNS_FINREP [keep(ACCNTNG_CLSSFCTN role Identifier, DT_RFRNC role Identifier, INSTTTNL_SCTR role Identifier, IS_HFS role Identifier, OBSRVD_AGNT_INTRNL_ID role Identifier, PRFRMNG_STTS role Identifier, PRJCT_FNNC_LN role Identifier, PRPS role Identifier, PRSPCTV_ID role Identifier, RPYMNT_RGHTS role Identifier, TM_PST_DU role Identifier, TYP_ACCNTNG_ITM role Identifier, TYP_CLLTRL_GRNT_GVN role Identifier, TYP_INSTRMNT role Identifier, CRRYNG_AMNT role Measure)]";

        /*
        // converte
        convertedExpr = convertExpressionToVTL2("", expression, false);
        // verifica se il parser VTL2 è in grado di gestire correttamente l'espressione
        parsedV2Expr = parseVTL2Expression("", convertedExpr.converted, false);

        if (convertedExpr != null && parsedV2Expr != null) {
            System.out.println(String.format("ORIGINAL : \n%s\n\nTRANSLATED : \n%s\n\nAST V2 (no whitespaces) --->\n%s"
                    , expression, convertedExpr.converted, parsedV2Expr.parsed));
        } else {
            System.out.println(String.format("ERROR : \n%s\n%s\n"
                    , expression, convertedExpr.converted));
        }

        // se è un test singolo, evitiamo il loop su tutte le altre
        if (true)
            return;
		*/
        // legge il file .json contenente tutte le transformations estratte dal BIRD database (ignora conversioni di tipo)
        ObjectMapper objectMapper = new ObjectMapper();
        List<Translation> listTrans = objectMapper.readValue(new File("E:\\Data\\Temp\\transformations2.json"), new TypeReference<List<Translation>>() {});
        HashMap<String, String> customTransformation = new HashMap<>();
        HashMap<String, String> errors = new HashMap<>();
        HashMap<String, List<String>> success = new HashMap<String, List<String>>();
        
        fillCustomTransformation(customTransformation);
        
        for (Translation translation : listTrans) {
        	logger.info("--------------------------------------");
        	System.out.println("--------------------------------------");
            convertedExpr = null;
            parsedV2Expr = null;
            
			if (test == true) {
				convertedExpr = new ConversionResult(translation.getTransformationId(), translation.getExpression(),
						translation.getExpression(), true, null);
			} else {
				if (customTransformation.containsKey(translation.getTransformationId())) {
					convertedExpr = new ConversionResult(translation.getTransformationId(), translation.getExpression(),
							"", true, null);
				} else {
					// converte
					convertedExpr = convertExpressionToVTL2(translation.getTransformationId(),
							translation.getExpression(), false);
				}
			}
            System.out.println("convertedExpr.converted:"+convertedExpr.converted);
            System.out.println("Chiamo vtl2 Parser");
            // verifica se il parser VTL2 � in grado di gestire correttamente l'espressione
            parsedV2Expr = parseVTL2Expression(translation.getTransformationId(), convertedExpr.converted, false);
            
            //parsedV2Expr = parseVTL2Expression(translation.getTransformationId(), translation.getExpression(), false);
            

            //TODO: gestire output ed errori in modo appropriato
            if (convertedExpr != null && convertedExpr.success && parsedV2Expr != null && parsedV2Expr.success) {
            	logger.info(String.format("ORIGINAL : \n%s\n\nTRANSLATED : \n%s\n\nAST V2 (no whitespaces) --->\n%s"
                        , translation.getExpression(), convertedExpr.converted, parsedV2Expr.parsed));
            	List<String> valori = new ArrayList<String>();
            	valori.add("TRANSFORMATION_SCHEME_ID:"+translation.getTransformationId().substring(0,translation.getTransformationId().lastIndexOf("_")));
            	valori.add("ORIGINAL:"+translation.getExpression().replaceAll("[\r\n]",""));
            	valori.add("TRANSLATED:"+convertedExpr.converted);
            	success.put(translation.getTransformationId(), valori);
            } else {
                errors.put(translation.getTransformationId(), translation.getExpression());
                System.out.println("Error:ID:"+translation.getTransformationId());
                System.out.println("Converted in VTL1:"+convertedExpr.converted);
                
                //logger.error(String.format("ERROR : %s\n%s\n%s\n"
                        //, "ID:"+translation.getTransformationId(), "ORIGINAL:"+translation.getExpression(), "Converted in VTL1"+convertedExpr.converted));
            }
        }

        System.out.println("=============================");
        System.out.println(String.format("Errors %d/%d", errors.size(), listTrans.size()));
        System.out.println(String.format("Successes %d/%d", success.size(), listTrans.size()));
        
        Iterator<String> keys = errors.keySet().iterator();
        while( keys.hasNext() ){
        	logger.info("++++++++++++++");
        	String key = keys.next();
        	logger.info("ID:"+key);
        	String values = errors.get(key);
        	/*for( String item : values ){
        		logger.info(item);
        	}*/
        	logger.info(values);
        }
        
        logger.info("/n++++++++++++++ CSV ++++++++++++++/n");
        
		for (HashMap.Entry<String, List<String>> entry : success.entrySet()) {
			logger.info(String.format("%s;%s;%s;%s", entry.getKey()+"@", entry.getValue().get(0).replace("TRANSFORMATION_SCHEME_ID:", "")+"@", entry.getValue().get(1).replace("ORIGINAL:", "").replace("\n", "")+"@",
					entry.getValue().get(2).replace("TRANSLATED:", "")));
		}
        
        
    }

	private static void fillCustomTransformation(HashMap<String, String> customTransformation) {
		customTransformation.put("P_EXTRCTN_RGSTRY_TBL_SCRTS_1_0", "SHR_CPTL_INVST_CNTRPRTY_FINREP := RGSTRY_TBL_SCRTS [filter(TYP_INSTRMNT in TYP_INSTRMNT_11)];");
        customTransformation.put("G_SHSG_GNRL_1_2", "OWND_SCRTS_E := OWND_SCRTS_E [filter(OBSRVD_AGNT_INTRNL_ID in GRP_SHSG)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_11", "EQTY_INSTRMNTS_GRP_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(OBSRVD_AGNT_INTRNL_ID not in CNSLDTD_ENTTS_PRDTNL_SUB_LST)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_4", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID in JNT_VNTRS_SBSDRS_ENTTS_FINREP_LST)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_3", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID not in CNSTNT_INSTTTN_ID)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_9", "EQTY_INSTRMNTS_GRP_INDVDL_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(OBSRVD_AGNT_INTRNL_ID in CNSLDTD_ENTTS_PRDTNL_SUB_LST)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_1", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID in CNSLDTD_ENTTS_ACCNTNG_LST)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_0", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_DBTRS_FINREP [filter(OBSRVD_AGNT_INTRNL_ID in CNSLDTD_ENTTS_ACCNTNG_LST)];");
        customTransformation.put("G_IS_CMPNNT_LGL_ENTTY_1_3", "CNTRPRTS_NT_CMPNNTS := full_join (ALL_CNTRPRTS, CNTRPRTY_CMPNNTS using CNTRPRTY_ID);");
        customTransformation.put("P_PRSPCTV_IDS_1_0", "PRDNTL_NT_FLL_PRPTNL_CNSLDT_PRSPCTV_IDS := PRSPCTV_INF [filter(CNTRPRTY_ID in CNSLDTD_ENTTS_ACCNTNG_LST and ACCNTNG_FRMWRK = CNSTNT_ACCNTNG_FRMWRK_CNS)];");
        customTransformation.put("G_SHSG4_GROUP_REF_1_5", "SHSG4_GROUP_REF := SHSG4_GROUP_REF [rename GRP_ID to CRDTR_GRP_ID , GRP_TYP to GROUP_ID_TYPE, LEI to LEI_CRDTR_GRP, GROUP_TYPE to TYP_CRDTR_GRP, GROUP_SECTOR to INSTTTNL_SCTR_CRDTR_GRP, NM_ENTTY to NM_CRDTR_GRP];");
        customTransformation.put("G_SCRTY_MSTR_DT_1_7", "CNTRPRTS_E_SUB := CNTRPRTS_E [filter (PRSPCTV_ID = INSTTTN_ID)] [keep OBSRVD_AGNT_ID, CNTRPRTY_ID, PD];");
        customTransformation.put("G_NT_RL_ET_PRTCTN_RCVD_AC_1_0", "PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_E [filter (IS_PRMRY_PRTCTN_PRVDR = T)] [keep PRTCTN_ID, CNTRPRTY_ID]; PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_SUB [rename CNTRPRTY_ID to PRTCTN_PRVD_ID]");
        customTransformation.put("G_RL_ESTT_PRTCTN_RCVD_AC_1_1", "PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_E [filter (IS_PRMRY_PRTCTN_PRVDR = T)] [keep PRTCTN_ID, CNTRPRTY_ID]; PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_SUB [rename CNTRPRTY_ID to PRTCTN_PRVD_ID];");
        customTransformation.put("G_ANCRDT_ENTTY_RSK_C_1_4", "ANCRDT_ENTTY_RSK_C := inner_join (ANCRDT_ENTTY_RSK_C as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID] as B  using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.LEI); ANCRDT_ENTTY_RSK_C := ANCRDT_ENTTY_RSK_C [rename LEI to OBSRVD_AGNT_CD];");
        customTransformation.put("P_DRVTVS_FINREP_1_4", "DRVTVS_TRS := inner_join (DRVTVS_TRS as A, DRVTVS_TRS_FV as B using INSTRMNT_ID keep A.ALL_VARIABLES, B.FV);");
        customTransformation.put("P_CNTRPRTS_E_1_2", "CNTRPRTS_E := inner_join (CNTRPRTS_E as A, EXTRNL_CRDT_ASSSSMNT as B using EXTRNL_CRDT_ASSSSMNT keep A.ALL_VARIABLES, B.CRDT_QLTY_STP, B.ECAI_ECA, B.IS_SHRT_TRM_CRDT_ASSSSMNT); CNTRPRTS_E := CNTRPRTS_E [rename CRDT_QLTY_STP to  CRDT_QLTY_STP_CNTRPRTY, ECAI_ECA to ECAI_ECA_CNTRPRTY, IS_SHRT_TRM_CRDT_ASSSSMNT to IS_SHRT_TRM_CRDT_ASSSSMNT_CNTRPRTY];");
        customTransformation.put("G_PRP_LNS_1_2", "LNS_AC := inner_join (LNS_AC as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID] as B using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.LEI); LNS_AC := LNS_AC [rename LEI to OBSRVD_AGNT_CD];");
        customTransformation.put("P_OWND_SCRTS_E_1_2", "OWND_SCRTS_E := inner_join (OWND_SCRTS as A, D_CRRYNG_AMNT_SCRTS as B using SCRTY_ID keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
        customTransformation.put("G_ANCRDT_ENTTY_DFLT_C_1_7", "ANCRDT_ENTTY_DFLT_C := inner_join (ANCRDT_ENTTY_DFLT_C as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID] as B using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.LEI); ANCRDT_ENTTY_DFLT_C := ANCRDT_ENTTY_DFLT_C [rename LEI to OBSRVD_AGNT_CD ];");
        customTransformation.put("P_DRVTVS_FINREP_1_10", "DRVTVS_MSTR_NTTNG_CRRNG_AMNT := left_join (DRVTVS_MSTR_NTTNG_CRRNG_AMNT as A, MSTR_NTTNG_AGRMNT_FINREP [rename MSTR_NTTNG_AGRMNT_ID to NTTNG_AGRMNT_ID] as B using NTTNG_AGRMNT_ID keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
        customTransformation.put("P_PRTCTNS_RCVD_FINREP_1_6", "PRTCTNS_RCVD_FINREP := inner_join (PRTCTNS_RCVD_FINREP as A, PRTCTNS_RCVD_FINREP_SUB as B using PRTCTN_ID keep A.PRTCTN_ID, A.FNNCL_GRNT_CNSDRD_MX_AMNT, A.TYP_PRTCTN, B.IS_RSTNTL_CLLTRL_FNNCL_GRNT, B.IS_CMMRCL_CLLTRL_FNNCL_GRNT);");
        customTransformation.put("P_LNS_ADVNCS_E_1_2", "LNS_ADVNCS_E := inner_join (LNS_ADVNCS_E as A, D_CRRYNG_AMNT_LNS_ADVNCS as B using INSTRMNT_UNQ_ID  keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
        customTransformation.put("P_CMMTMNTS_GVN_CSTMRS_FINREP_1_0", "CMMTMNTS_GVN_CSTMRS_FINREP := inner_join (CMMTMNTS_GVN_FINREP as A, CMMTMNTS_GVN_CSTMRS_RLTNSHP_E as B using CMMTMNT_UNQ_ID keep A.ALL_VARIABLES, B.CNTPRTY_ID);");
        customTransformation.put("D_CNTRY_FINREP_1_0", "/* map: (Institutional sector, International organisation) &rArr; Institutional sector*/ define operator D_CNTRY_FINREP(CNTRY IDENTIFIER, INTRNTNL_ORGNSTN_CD IDENTIFIER)  returns string is if (INTRNTNL_ORGNSTN_CD in MLTLTRL_DVLPMNT_BNKS_FINREP or INTRNTNL_TRGNSTN_CD in INTRNTNL_ORGNSTNS_FINREP) then \\\"_X\\\" else CNTRY  end operator");
        customTransformation.put("D_IS_CSH_DBT_INSTRMNT_ISSD_1_0", "define operator D_IS_CSH_DBT_INSTRMNT_ISSD(TYP_PRTCTN IDENTIFIER, RPRTNG_AGNT IDENTIFIER, GRP_INFRMTN IDENTIFIER)  returns string is if (TYP_PRTCTN in {\\\"12\\\", \\\"15\\\"} and RPRTNG_AGNT in GRP_INFRMTN) then \\\"T\\\" else \\\"F\\\"  end operator");
        customTransformation.put("D_TYP_INSTRMNT_CMMTMNTS_FINREP_1_0", "define operator D_TYP_INSTRMNT_CMMTMNTS_FINREP(TYP_CMMTMNT IDENTIFIER)  returns string is if (TYP_CMMTMNT in LN_CMMTMNTS_GVN) then \\\"910\\\" else if (TYP_CMMTMNT in OTHR_CMMTMNTS_GVN) then \\\"930\\\" else \\\"0\\\"  end operator");
        customTransformation.put("G_TYP_INSTRMNT_AC_1_0", "/*map: (Type of instrument, Is revolving loan, Is credit line other than revolving credit) &rArr; Type of instrument (AnaCredit)*/ define operator G_TYP_INSTRMNT_AC (TYP_INSTRMNT IDENTIFIER, IS_RVLVNG_LN IDENTIFIER, IS_CRDT_LN_OTHR_RV_CRDT IDENTIFIER, INSTTTNL_SCTR IDENTIFIER)     returns string is /*Current accounts*/if (TYP_INSTRMNT in {\\\"260\\\"}) then    /*Current accounts with MFIs are considered Deposits in AnaCredit*/if (INSTTTNL_SCTR in Sectors03) then \\\"1000\\\"    /*otherwise such current accounts are considered as Overdrafts*/else \\\"20\\\"  /*Factoring and Other trade receivables*/else if (TYP_INSTRMNT in {\\\"1020\\\", \\\"1023\\\"}) then     (if (IS_RVLVNG_LN = \\\"T\\\") then \\\"1001\\\"     else if (IS_CRDT_LN_OTHR_RV_CRDT = \\\"T\\\") then \\\"1002\\\"     else \\\"71\\\")     /*Other loans*/else if (TYP_INSTRMNT = \\\"1022\\\") then     (if (IS_RVLVNG_LN = \\\"T\\\") then \\\"1001\\\"     else if (IS_CRDT_LN_OTHR_RV_CRDT = \\\"T\\\") then \\\"1002\\\"     else \\\"1004\\\")    /*for other types of instruments the input classification is equal to the classification required in AnaCredit*/else TYP_INSTRMNT  end operator");
        customTransformation.put("D_INSTTTNL_SCTR_FINREP_1_0", "/* map: (Institutional sector, International organisation) &rArr; Institutional sector*/ define operator D_INSTTTNL_SCTR_FINREP(INSTTTNL_SCTR IDENTIFIER, INTRNTNL_ORGNSTN_CD IDENTIFIER)  returns string is if (INSTTTNL_SCTR in {\\\"S122_A\\\"} or INTRNTNL_ORGNSTN_CD in MLTLTRL_DVLPMNT_BNKS_FINREP) then \\\"S1_MDB\\\" else if (INSTTTNL_SCTR in Sectors04 or INTRNTNL_TRGNSTN_CD in INTRNTNL_ORGNSTNS_FINREP) then \\\"S13_IO\\\" else INSTTTNL_SCTR  end operator");        
	}

    /**
     * Converte una expression (di una transformation) da sintassi VTL1 in una equivalente in VTL2
     * @param id id della transformation originale (nel BIRD database)
     * @param expression expression originale in VTL1
     * @param printAst stampa per debug l'AST
     * @return DTO con l'esito della conversione
     */
    public static ConversionResult convertExpressionToVTL2(String id, String expression, boolean printAst) {
        ANTLRInputStream input = new ANTLRInputStream(expression.replace("\n", " "));

        VtlLexer lexer = new VtlLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VtlParser parser = new VtlParser(tokens);
        ParseTree tree = parser.start(); // parse

        if(printAst)
            printAST(tree);

        ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        ConvertToVtl2Listener extractor = new ConvertToVtl2Listener(tokens);
        walker.walk(extractor, tree); // initiate walk of tree with listener
        
        // print back ALTERED stream
        String output = extractor.rewriter.getText().replaceAll("  ", " ");
        RecognitionException excp = ((VtlParser.StartContext) tree).exception;

        return new ConversionResult(id, expression, output, excp == null, excp != null ? excp.getMessage() : null);
    }

    /**
     * Esegue il parse di una expression scritta in VTL2
     * @param id id della transformation originale (nel BIRD database)
     * @param expression espressione in VTL2
     * @param printAst stampa per debug l'AST
     * @return DTO con l'esito del parse
     */

    public static ParseResult parseVTL2Expression(String id, String expression, boolean printAst) {
        ANTLRInputStream input = new ANTLRInputStream(expression.replace("\n", " "));

        Vtl2Lexer lexer = new Vtl2Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Vtl2Parser parser = new Vtl2Parser(tokens);
        
       
        ParseTree tree = parser.start(); // parse

        if (((Vtl2Parser.StartContext) tree).exception != null)
            return null;

        if(printAst)
            printAST(tree);

        String output = tree.getText();
        //ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        //MyVTL2Listener extractor = new MyVTL2Listener(tokens);
        //walker.walk(extractor, tree); // initiate walk of tree with listener
        
        // print back ALTERED stream
        //String output = extractor.rewriter.getText().replaceAll("  ", " ").replaceAll("  ", " ");
        RecognitionException excp = ((Vtl2Parser.StartContext) tree).exception;
        return new ParseResult(id, expression, output, excp == null, excp != null ? excp.getMessage() : null);
    }

    private static void printAST(ParseTree tree) {
        AST ast = new AST(tree);
        System.out.println("* Tree structure of the abstract syntax tree:");
        System.out.println(ast.toString());
        System.out.println("----------------------------------");
    }
}
