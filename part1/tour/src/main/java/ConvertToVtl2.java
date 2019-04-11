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
        boolean testVTL2=true; // se true le espressioni vengono valutate direttamente con il parser VTL2

        // legge il file .json contenente tutte le transformations estratte dal BIRD database (ignora conversioni di tipo)
        ObjectMapper objectMapper = new ObjectMapper();
        List<Translation> listTrans = objectMapper.readValue(new File("E:\\Data\\Temp\\transformations2.json"), new TypeReference<List<Translation>>() {});
        HashMap<String, String> customTransformation = new HashMap<>();
        HashMap<String, String> errors = new HashMap<>();
        HashMap<String, String> errorsVTL = new HashMap<>();
        HashMap<String, List<String>> success = new HashMap<String, List<String>>();
        
        fillCustomTransformation(customTransformation);
        
        for (Translation translation : listTrans) {
        	logger.info("--------------------------------------");
        	System.out.println("--------------------------------------");
            convertedExpr = null;
            parsedV2Expr = null;
            
            if (translation.getTransformationId().contains("comment")) {
            	continue;
            }
            
			if (testVTL2 == true) {
				convertedExpr = new ConversionResult(translation.getTransformationId(), translation.getExpression(),
						translation.getExpression(), true, null);
			} else {
				if (customTransformation.containsKey(translation.getTransformationId())) {
					convertedExpr = new ConversionResult(translation.getTransformationId(), translation.getExpression(),
							customTransformation.get(translation.getTransformationId()), true, null);
				} else {
					// converte
					convertedExpr = convertExpressionToVTL2(translation.getTransformationId(),
							translation.getExpression(), false);
				}
			}
			System.out.println("id: "+convertedExpr.id);
            System.out.println("convertedExpr.converted:"+convertedExpr.converted);
            System.out.println("Chiamo vtl2 Parser");
            // verifica se il parser VTL2 è in grado di gestire correttamente l'espressione
            parsedV2Expr = parseVTL2Expression(translation.getTransformationId(), convertedExpr.converted, false);

            if (convertedExpr != null && convertedExpr.success && parsedV2Expr != null && parsedV2Expr.success) {
            	logger.info(String.format("ID : \n%s\n\nORIGINAL : \n%s\n\nTRANSLATED : \n%s\n\nAST V2 (no whitespaces) --->\n%s"
                        , translation.getTransformationId(), translation.getExpression(), convertedExpr.converted, parsedV2Expr.parsed));
            	List<String> valori = new ArrayList<String>();
            	valori.add("TRANSFORMATION_SCHEME_ID:"+translation.getTransformationId().substring(0,translation.getTransformationId().lastIndexOf("_")));
            	valori.add("ORIGINAL:"+translation.getExpression().replaceAll("[\r\n]",""));
            	valori.add("TRANSLATED:"+convertedExpr.converted);
            	success.put(translation.getTransformationId(), valori);
            } else {
            	if (convertedExpr!=null && convertedExpr.converted!=null && convertedExpr.converted.contains("VTL1 ERROR")) {
            		errorsVTL.put(translation.getTransformationId(), translation.getExpression());
				}
            	errors.put(translation.getTransformationId(), translation.getExpression());
                System.out.println("Error:ID:"+translation.getTransformationId());
                System.out.println("Converted in VTL1:"+convertedExpr.converted);
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
        	String value = errors.get(key);
        	logger.info(value);
        }
        
        logger.info("");
        logger.info("++++++++++++++ VTL1 ERROR ++++++++++++++");
        
        for (HashMap.Entry<String, String> entry : errorsVTL.entrySet()) {
        	logger.info("");
        	logger.info("++++++++++++++");
        	logger.info("ID:"+entry.getKey());
        	logger.info(entry.getValue());
		}
        
        logger.info("\n\n++++++++++++++ CSV ++++++++++++++\n");
        
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
        customTransformation.put("G_IS_ELGBL_INSTRMNT_AC_1_0", "/*Returns 'True (T)' if the instrument is eligible w.r.t. AnaCredit reporting requirements, 'False (F)' otherwise*/ define operator G_IS_ELGBL_INSTRMNT_AC (TYP_INSTRMNT IDENTIFIER, OFF_BLNC_SHT_AMNT IDENTIFIER, OTSTNDNG_NMNL_AMNT IDENTIFIER, ACCMLTD_WRTFFS IDENTIFIER) returns string is (if (TYP_INSTRMNT in INSTRMNTS_ELGBL_AC and (OFF_BLNC_SHT_AMNT > 0 or OTSTNDNG_NMNL_AMNT > 0 or ACCMLTD_WRTFFS > 0)) then \\\"T\\\" else \\\"F\\\")  end operator");        
        customTransformation.put("D_TYP_CLLTRL_GRNT_RCVD1_1_0", "define operator D_TYP_CLLTRL_GRNT_RCVD1(TYP_PRTCTN IDENTIFIER, ISSR_ID IDENTIFIER)  returns string is if (CNSTNL_SL_CNLDT = \\\"T\\\" and TYP_PRTCTN in {\\\"12\\\"} and ISSR_ID in CONST_GRP) then \\\"1161\\\" else if (CNSTNT_SL_CNLDT = \\\"F\\\" and TYP_PRTCTN in {\\\"23\\\"}) then \\\"1161\\\" else if (TYP_PRTCTN in {\\\"21\\\"}) then \\\"1161\\\" else if (TYP_PRTCTN in {\\\"8\\\"}) then \\\"8\\\" else if (TYP_PRTCTN in {\\\"9\\\", \\\"10\\\"}) then \\\"10\\\" else if (TYP_PRTCTN in {\\\"5\\\", \\\"41\\\"}) then \\\"960\\\" else \\\"20\\\"  end operator");        
        customTransformation.put("D_CRRYNG_AMNT_1_0", "/*map: (Accounting classification, Fair value, Gross carrying amount excluding accrued interest, Accrued interest, Fair value changes due to hedge accounting, Accumulated impairment) &rArr; Carrying amount*/ define operator D_CRRYNG_AMNT(ACCNTNG_CLSSFCTN IDENTIFIER, FV IDENTIFIER, GRSS_CRRYNG_AMNT_E_INTRST IDENTIFIER, ACCRD_INTRST IDENTIFIER, FV_CHNG_HDG_ACCNTNG IDENTIFIER, ACCMLTD_IMPRMNT IDENTIFIER, CRRYNG_AMNT IDENTIFIER, IS_CRRYNG_AMNT_DRVD IDENTIFIER)   returns integer is if (IS_CRRYNG_AMNT_DRVD = \\\"T\\\") then  if (ACCNTNG_CLSSFCTN in FV_ACCNTNG_CLSSFCTNS) then FV  else (GRSS_CRRYNG_AMNT_E_INTRST + ACCRD_INTRST - ACCMLTD_IMPRMNT + FV_CHNG_HDG_ACCNTNG) else CRRYNG_AMNT  end operator");        
        customTransformation.put("D_ASSGN_EXPSR_CLSS1_1_0", "/*map: (Is exposure class applicable, Exposure class (input), Relationship with securitisation or credit transfer, Type of risk transfer, Is equity with 250% risk weight, Institutional sector, Credit quality status, International organisations, Security rank level, LEI code, Type of instrument, Is particular high risk, Is covered bond, Characteristics for credit risk, Is short-term credit assessment ) &rArr; Exposure class*/ define operator D_ASSGN_EXPSR_CLSS1 (IS_EXPSR_CLSS_APPLCBL IDENTIFIER, EXPSR_CLSS IDENTIFIER, RLTNSHP_SCRTSTN_CRDT_TRNSFR IDENTIFIER, TYP_RSK_TRNSFR IDENTIFIER, TYP_RSK_TRNSFR_ORGNL IDENTIFIER, IS_EQTY_250_RSK_WGHT IDENTIFIER, INSTTTNL_SCTR IDENTIFIER, CHRCTRSTCS_CRDT_RSK IDENTIFIER, IS_PRTCLR_HGH_RSK IDENTIFIER, TYP_INSTRMNT IDENTIFIER, SCRTY_RNK_LVL IDENTIFIER, CRDT_QLTY_STTS IDENTIFIER, INTRNTNL_ORGNSTN IDENTIFIER, LEI IDENTIFIER, IS_CVRD_BND IDENTIFIER,IS_SHRT_TRM_CRDT_ASSSSMNT IDENTIFIER)  returns string is if IS_EXPRSR_CLSS_APPLCBL = \\\"T\\\" then (/*for exposures for which the IRB approach is followed the exposure class and the risk weight are equal to the variables provided in the input layer*/ if APPRCH_PRDNTL_PRPSS not_in {\\\"66\\\", \\\"67\\\"} then EXPSR_CLSS /*The exposures to a securitisation are classified as “items representing securitisation positions”*/ else if (RLTNSHP_SCRTSTN_CRDT_TRNSFR = \\\"2\\\" and TYP_RSK_TRNSFR in {\\\"1\\\", \\\"2\\\"}) then \\\"15\\\" else if (RLTNSHP_SCRTSTN_CRDT_TRNSFR = \\\"4\\\" and TYP_RSK_TRNSFR_ORGNL in {\\\"1\\\", \\\"2\\\"}) then \\\"15\\\" /*investments in equity attracting a 250% risk weight are classified as “exposures to institutions”*/ else if (IS_EQTY_250_RSK_WGHT = \\\"T\\\" and INSTTTNL_SCTR in {\\\"S122_A\\\", \\\"S122_B1\\\", \\\"S122_B2\\\", \\\"S125_B\\\", \\\"S125_C\\\", \\\"S125_D\\\", \\\"S125_E\\\", \\\"S125_I\\\", \\\"S126\\\", \\\"S127\\\"} and CHRCTRSTCS_CRDT_RSK = \\\"1\\\") then \\\"9\\\" /*items associated with particular high risk are classified in the specific exposure class*/ else if IS_PRTCLR_HGH_RSK = \\\"T\\\" then \\\"14\\\" /*equity and subordinated debt securities are classified as “equity exposures”*/ else if TYP_INSTRMNT in {\\\"220\\\", \\\"230\\\"} or (TYP_INSTRMNT = \\\"210\\\" and SCRTY_RNK_LVL in {\\\"1\\\", \\\"2\\\", \\\"3\\\", \\\"5\\\", \\\"6\\\"}) then \\\"1\\\" /*if the issuer is defaulted then the exposure is classified in default*/ else if CRDT_QLTY_STTS in {\\\"18\\\", \\\"19\\\", \\\"20\\\"} then \\\"2\\\" /*if the issuer is an international organisation listed in Article 118 CRR, the exposure is classified in the specific class*/ else if (INTRNTNL_ORGNSTN in {\\\"1C\\\", \\\"5B\\\", \\\"4S\\\", \\\"4D\\\", \\\"4J1\\\", \\\"4J2\\\", \\\"4J3\\\", \\\"4J4\\\", \\\"4J5\\\", \\\"4J6\\\", \\\"4J7\\\", \\\"4J8\\\"} or LEI = \\\"222100OW6UHQXNHKN143\\\") then \\\"10\\\" /*if the issuer is a multilateral development bank listed in Article 117 CRR, the exposure is classified in the specific class*/ else if INTRNTNL_ORGNSTN in {\\\"5G\\\", \\\"7G\\\", \\\"5T\\\", \\\"5U\\\", \\\"1E\\\", \\\"1M\\\", \\\"5C\\\", \\\"5E\\\", \\\"5D\\\", \\\"7E\\\", \\\"5H\\\", \\\"5L\\\", \\\"5F\\\", \\\"4C\\\", \\\"4G\\\", \\\"1N\\\", \\\"7L\\\", \\\"7B\\\"} then \\\"11\\\" /*if the issuer’s sector is central bank or it is central government neither to be considered as public sector entity nor corporate, the exposure is classified as to central governments or central bank*/ else if (INSTTTNL_SCTR = \\\"S121\\\" or (INSTTTNL_SCTR = \\\"S1311\\\" and CHRCTRSTCS_CRDT_RSK = \\\"0\\\")) then \\\"6\\\" /*if the issuer’s sector is state or local government neither to be considered as public sector entity nor corporate, the exposure is classified as to regional governments or local authorities*/ else if (INSTTTNL_SCTR in {\\\"S1312\\\", \\\"S1313\\\"} and CHRCTRSTCS_CRDT_RSK = \\\"0\\\") then \\\"13\\\" /*if the issuer’s sector is social security funds or it is credit institution, central, state or local government to be considered as public sector entity, the exposure is classified as to public sector entities*/ else if "+
        "(INSTTTNL_SCTR = \\\"S1314\\\" or (INSTTTNL_SCTR in {\\\"S122_A\\\", \\\"S1311\\\", \\\"S1312\\\", \\\"S1313\\\"} and CHRCTRSTCS_CRDT_RSK = \\\"2\\\")) then \\\"12\\\" /*if the issuer’s sector is financial corporations, except some subsectors, and the entity is supervised, the exposure is classified as to institutions*/ else if (INSTTTNL_SCTR = \\\"S122_A\\\" and CHRCTRSTCS_CRDT_RSK = \\\"1\\\") then   (if IS_CVRD_BND = \\\"T\\\" then \\\"3\\\"  else if IS_SHRT_TRM_CRDT_ASSSSMNT = \\\"T\\\" then \\\"8\\\"  else \\\"9\\\") else if INSTTTNL_SCTR in {\\\"S122_B1\\\", \\\"S122_B2\\\", \\\"S125_B\\\", \\\"S125_C\\\", \\\"S125_D\\\", \\\"S125_E\\\", \\\"S125_I\\\", \\\"S126\\\", \\\"S127\\\"} then   (if not isnull(EXTRNL_CRDT_ASSSSMNT) and IS_SHRT_TRM_CRDT_ASSSSMNT = \\\"T\\\" then \\\"8\\\"   else \\\"9\\\") /*if the issuer’s sector is non-financial corporations, some subsectors of financial corporations, households or non-profit institutions serving households or it is financial corporations, except some subsectors, and the entity is not supervised or it is central, state or local government and the entity is considered as corporate, the exposure is classified as to corporates*/ else if (INSTTTNL_SCTR in {\\\"S11\\\", \\\"S125_A\\\", \\\"S128\\\", \\\"S129\\\", \\\"S14_A\\\", \\\"S14_B\\\", \\\"S15\\\"} or (INSTTTNL_SCTR in {\\\"S122_A\\\", \\\"S122_B1\\\", \\\"S122_B2\\\", \\\"S125_B\\\", \\\"S125_C\\\", \\\"S125_D\\\", \\\"S125_E\\\", \\\"S125_I\\\", \\\"S126\\\", \\\"S127\\\"} and CHRCTRSTCS_CRDT_RSK = \\\"0\\\") or (INSTTTNL_SCTR in {\\\"S1311\\\", \\\"S1312\\\", \\\"S1313\\\"} and CHRCTRSTCS_CRDT_RSK = \\\"3\\\")) then   (if (not isnull (EXTRNL_CRDT_ASSSSMNT) and IS_SHRT_TRM_CRDT_ASSSSMNT = \\\"T\\\") then \\\"8\\\"   else \\\"7\\\") /*if the issuer’s sector is MMFs or non-MMF investment funds, the exposure is classified as in the form of units or shares in CIUs*/ else if INSTTTNL_SCTR in {\\\"S123\\\", \\\"S124\\\"} then \\\"4\\\" /*in case none of the above conditions are met the exposure is classified as “other items” (in practice it should never happen)*/ else \\\"16\\\") /*in any other case the exposure class is not applicable*/else \\\"0\\\"   end operator");        
        customTransformation.put("D_IS_EXPSR_CLSS_APPLCBL1_1_0", "/*map: (Capital calculation approach for prudential purposes, Exposure class (input), Prudential portfolio, Derogation for small trading book business, Is short position, Issuer is part of the Group (prudential scope), Relationship with securitisation or credit transfer, Treatment of securitised/transferred assets in balance sheet, Significant risk transfer, Treatment of securitised/transferred assets in balance sheet of the originator, Significant risk transfer of the originator) &rArr; Is exposure class applicaple*/ define operator D_IS_EXPSR_CLSS_APPLCBL1 (APPRCH_PRDNTL_PRPSS IDENTIFIER, EXPSR_CLSS IDENTIFIER, PRDNTL_PRTFL IDENTIFIER, DRGTN_SMLL_TRDNG_BK_BSNSS IDENTIFIER, IS_SHRT_PSTN IDENTIFIER, INTRA_GROUP_PRUD_SCOPE IDENTIFIER, RLTNSHP_SCRTSTN_CRDT_TRNSFR IDENTIFIER, TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT IDENTIFIER, SGNFCNT_RSK_TRNSFR IDENTIFIER, TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT_ORGNL IDENTIFIER, SGNFCNT_RSK_TRNSFR_ORGNL IDENTIFIER)  returns string is /*for exposures for which the IRB approach is followed the exposure class and the risk weight are equal to the variables provided in the input layer, therefore the exposure class is applicable*/ if APPRCH_PRDNTL_PRPSS not_in {\\\"66\\\", \\\"67\\\"} then (/*in case the security is in the trading book and the bank does not calculate the requirement for credit risk the exposure class and the risk weight are not applicable*/ if (PRDNTL_PRTFL = \\\"1\\\" and DRGTN_SMLL_TRDNG_BK_BSNSS = \\\"F\\\") then \\\"F\\\" /*for short positions the exposure class and the risk weight are not applicable*/ else if IS_SHRT_PSTN = \\\"T\\\" then \\\"F\\\" /*for intra-group holdings the exposure class and the risk weight are not applicable*/ else if INTRA_GROUP_PRUD_SCOPE = \\\"T\\\" then \\\"F\\\" /*for securitisation positions where the originator has not transferred significant credit risk the exposure class and the risk weight are not applicable*/ else if RLTNSHP_SCRTSTN_CRDT_TRNSFR = \\\"2\\\" and TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\\\"1\\\", \\\"2\\\"} and SGNFCNT_RSK_TRNSFR <> \\\"T\\\" then \\\"F\\\" else if RLTNSHP_SCRTSTN_CRDT_TRNSFR = \\\"4\\\" and TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT_ORGNL in {\\\"1\\\", \\\"2\\\"} and SGNFCNT_RSK_TRNSFR_ORGNL <> \\\"T\\\" then \\\"F\\\" /*for securitised/transferred assets that are not recognised in balance sheet or, for traditional securitisations, where the originator has transferred significant credit risk the exposure class and the risk weight are not applicable*/ else if RLTNSHP_SCRTSTN_CRDT_TRNSFR in {\\\"1\\\", \\\"2\\\", \\\"3\\\"} and (TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\\\"0\\\", \\\"3\\\"} or (TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\\\"1\\\", \\\"2\\\"} and TYP_RSK_TRNSFR = \\\"1\\\" and SGNFCNT_RSK_TRNSFR = \\\"T\\\"))then \\\"F\\\" else \\\"T\\\") else \\\"T\\\"  end operator");        
        customTransformation.put("GT_CB_VLD_AT_1_0", "/*map: (CB_CD, TM) &rArr; data set\\nPlease note that this function, from a pure technical perspective, retrieves the record from the table CUBE in the BIRD data base that matches the input parameter CB_CD and is valid at the time TM.\\n*/define operator GT_CB_VLD_AT(CB_CD IDENTIFIER, TM IDENTIFIER)  returns dataset is CUBE [filter(CUBE.FRAMEWORK_ID = \\\"BIRD\\\" and CUBE.CODE = CB_CD and CUBE.VALID_FROM <= TM and TM <= CUBE.VALID_TO)]  end operator");
        customTransformation.put("G_CRRNT_ACCNTS_LBLTS_AC_1_0", "/*Generate the cube CRRNT_ACCNTS_LBLTS_AC  for current accounts that are liabilities for the reporting requirements of AnaCredit\\njoin Deposits that are liabilities - enriched (DPSTS_THT_LBLTS_E)  with the related credit facility (UNQ_CRDT_FCLTS_INSTRMNTS) variables transferred from the related credit facility: Annualised agreed rate (ANNLSD_AGRD_RT), Assessment approach for credit quality status (APPRCH_CRDT_QLTY_STTS), Credit quality status (CRDT_QLTY_STTS), Is debt financing (IS_DBT_FNNCNG), Date of default status (DT_DFLT_STTS), Date of performing status (DT_PRFRMNG_STTS), Next interest rate reset date (DT_NXT_INTRST_RT_RST), Forbearance and renegotiation status (FRBRNC_STTS), Project finance loan (PRJCT_FNNC_LN), Purpose (PRPS), Recourse (RCRS), Is retail exposure (IS_RTL_EXPSR), Is serviced by the observed agent (IS_SRVCD_OBSRVD_AGNT), Syndicated contract identifier (SYNDCTD_CNTRCT_ID), Type of amortisation (TYP_AMRTSTN), Inception date (DT_INCPTN), Interest rate cap (INTRST_RT_CP), Interest rate floor (INTRST_RT_FLR), Interest rate reset frequency (INTRST_RT_RST_FRQNCY), Interest rate spread/margin (INTRST_RT_SPRD), Type of interest rate (TYP_INTRST_RT), Commitment amount at inception (CMMTMNT_INCPTN), Payment frequency (PYMNT_FRQNCY), Reference rate (RFRNC_RT) */CRRNT_ACCNTS_LBLTS_AC := inner_join (UNQ_CRDT_FCLTS_INSTRMNTS as A, DPSTS_THT_LBLTS_E [filter(TYP_INSTRMNT = 260)] as B using INSTRMNT_UNQ_ID keep B.INSTRMNT_UNQ_ID, B.PRSPCTV_ID, B.TYP_INSTRMNT, B.ACCMLTD_CHNGS_FV, B.ACCMLTD_CHNGS_FV_CR,  B.ACCNTNG_CLSSFCTN, B.CRRNCY_DNMNTN, B.CRRYNG_AMNT, B.DT_LGL_FNL_MTRTY, B.DT_RFRNC,  B.DT_STTLMNT, B.FV, B.FV_CHNG_HDG_ACCNTNG, B.FV_HRRCHY, B.FVO_DSGNTN,  B.GRSS_CRRYNG_AMNT_E_INTRST, B.IS_HFS, B.OBSRVD_AGNT_INTRNL_ID, B.OTSTNDNG_NMNL_AMNT, B.SBRDNTD_DBT, A.ANNLSD_AGRD_RT, A.APPRCH_CRDT_QLTY_STTS, A.CMMTMNT_INCPTN, A.CRDT_QLTY_STTS,  A.DT_DFLT_STTS, A.IS_DBT_FNNCNG, A.DT_PRFRMNG_STTS, A.DT_NXT_INTRST_RT_RST, A.FRBRNC_STTS, A.PRJCT_FNNC_LN, A.PRPS, A.RCRS, A.IS_RTL_EXPSR,  A.IS_SRVCD_OBSRVD_AGNT, A.SYNDCTD_CNTRCT_ID, A.TYP_AMRTSTN, A.DT_INCPTN, A.INTRST_RT_CP, A.INTRST_RT_FLR, A.INTRST_RT_RST_FRQNCY, A.INTRST_RT_SPRD, A.TYP_INTRST_RT, A.PYMNT_FRQNCY, A.RFRNC_RT)");
        customTransformation.put("G_ANCRDT_JNT_LBLTS_C_1_2", "/*join CNTRPRTS_CDS through CNTRPRTY_ID to \\\"translate\\\" OBSRVD_AGNT_ID and CNTRPRTY_ID in RIAD code (CNTRPRTY_EXTRNL_ID) as OBSRVD_AGNT_CD and ENTTY_RIAD_CD*/ANCRDT_JNT_LBLTS_C := inner_join( ANCRDT_JNT_LBLTS_C as A, CNTRPRTS_CDS[filter(TYP_CNTRPRTY_EXTRNL_ID = \\\"RIAD code\\\")][rename CNTRPRTY_ID to OBSRVD_AGNT_ID] as B using OBSRVD_AGNT_ID keep A.CNTRCT_ID , A.DT_RFRNC , A.ENTTY_ID , A.INSTRMNT_ID , A.JNT_LBLTY_AMNT, B.CNTRPRTY_EXTRNL_ID   ); ANCRDT_JNT_LBLTS_C := ANCRDT_JNT_LBLTS_C [rename CNTRPRTY_EXTRNL_ID to OBSRVD_AGNT_CD ];");
        customTransformation.put("G_SHSG4_GROUP_REF_1_2", "SHSG4_GROUP_REF := SHSG4_GROUP_REF [calc measure ACCNTNG_FRMWRK_CNS  := CNSTNT_ACCNTNG_FRMWRK_CNS ];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_11", "EQTY_INSTRMNTS_GRP_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(OBSRVD_AGNT_INTRNL_ID not_in CNSLDTD_ENTTS_PRDTNL_SUB_LST)];");
        customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_3", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID not_in CNSTNT_INSTTTN_ID)];");
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
        boolean errorFound=false;

        VtlLexer lexer = new VtlLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VtlParser parser = new VtlParser(tokens);
        VTLErrorListener vtlErrorListener=new VTLErrorListener();
        ParseTree tree=null;
        parser.removeErrorListeners();
        parser.addErrorListener(vtlErrorListener);
		tree = parser.start();

        if(printAst)
            printAST(tree);

        ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        ConvertToVtl2Listener extractor = new ConvertToVtl2Listener(tokens);
        walker.walk(extractor, tree); // initiate walk of tree with listener
        
        // print back ALTERED stream
        String output = extractor.rewriter.getText().replaceAll("  ", " ");
        
        //RecognitionException excp = ((VtlParser.StartContext) tree).exception;
		
        /*if (vtlErrorListener.get_s().equals("error")) {
			errorFound = true;
			output=output+" VTL1 ERROR";
		}*/
        return new ConversionResult(id, expression, output, !errorFound, errorFound != false ? vtlErrorListener.get_s() : null);
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
