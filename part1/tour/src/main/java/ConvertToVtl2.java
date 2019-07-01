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
    private static final Logger LOGGER = Logger.getLogger(ConvertToVtl2.class);

    public static void main(String[] args) throws Exception {
        ConversionResult convertedExpr = null;
        ParseResult parsedV2Expr = null;
        
        // se true le espressioni vengono valutate direttamente con il parser VTL2
        boolean testVTL2=false;

        // legge il file .json contenente tutte le transformations estratte dal BIRD database (ignora conversioni di tipo)
        ObjectMapper objectMapper = new ObjectMapper();
        List<Translation> listTrans = objectMapper.readValue(new File("E:\\Data\\Backup\\Lavoro\\Aubay\\VTL\\Doc\\transformation\\transformations7.json"), new TypeReference<List<Translation>>() {});
        HashMap<String, String> customTransformation = new HashMap<>();
        HashMap<String, String> errors = new HashMap<>();
        HashMap<String, String> errorsVTL = new HashMap<>();
        HashMap<String, List<String>> success = new HashMap<String, List<String>>();
        
        fillCustomTransformation(customTransformation);
        
        for (Translation translation : listTrans) {
            convertedExpr = null;
            parsedV2Expr = null;
            
            if (translation.getTransformationId().contains("comment")) {
            	continue;
            }
            LOGGER.info("--------------------------------------");
            System.out.println("--------------------------------------");
        	System.out.println("Elaborazione trasformazione ID: "+translation.getTransformationId()+"\n");
            
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
            	LOGGER.info(String.format("ID : \n%s\n\nORIGINAL : \n%s\n\nTRANSLATED : \n%s\n\nAST V2 (no whitespaces) --->\n%s"
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
            	else {errors.put(translation.getTransformationId(), translation.getExpression());
                System.out.println("Error:ID:"+translation.getTransformationId());
                System.out.println("Converted in VTL1:"+convertedExpr.converted);}
            }
        }

        System.out.println("=============================");
        System.out.println(String.format("Errors VTL1 %d/%d", errorsVTL.size(), listTrans.size()));
        System.out.println(String.format("Errors VTL2 %d/%d", errors.size(), listTrans.size()));
        System.out.println(String.format("Total Errors %d/%d", errors.size()+errorsVTL.size(), listTrans.size()));
        System.out.println(String.format("Successes %d/%d", success.size(), listTrans.size()));
        
        Iterator<String> keys = errors.keySet().iterator();
        while( keys.hasNext() ){
        	LOGGER.info("++++++++++++++");
        	String key = keys.next();
        	LOGGER.info("ID:"+key);
        	String value = errors.get(key);
        	LOGGER.info(value);
        }
        
        LOGGER.info("");
        LOGGER.info("++++++++++++++ VTL1 ERROR ++++++++++++++");
        
        for (HashMap.Entry<String, String> entry : errorsVTL.entrySet()) {
        	LOGGER.info("");
        	LOGGER.info("++++++++++++++");
        	LOGGER.info("ID:"+entry.getKey());
        	LOGGER.info(entry.getValue());
		}
        
        LOGGER.info("\n\n++++++++++++++ CSV ++++++++++++++\n");
        
		for (HashMap.Entry<String, List<String>> entry : success.entrySet()) {
			LOGGER.info(String.format("%s;%s;%s;%s", entry.getKey()+"@", entry.getValue().get(0).replace("TRANSFORMATION_SCHEME_ID:", "")+"@", entry.getValue().get(1).replace("ORIGINAL:", "").replace("\n", "")+"@",
					entry.getValue().get(2).replace("TRANSLATED:", "")));
		}
    }

	private static void fillCustomTransformation(HashMap<String, String> customTransformation) {
	    customTransformation.put("P_EXTRCTN_RGSTRY_TBL_SCRTS_1_0", "SHR_CPTL_INVST_CNTRPRTY_FINREP := RGSTRY_TBL_SCRTS [filter(TYP_INSTRMNT in TYP_INSTRMNT_11)];");
            customTransformation.put("G_SHSG_GNRL_1_2", "OWND_SCRTS_E := OWND_SCRTS_E [filter(OBSRVD_AGNT_INTRNL_ID in GRP_SHSG)];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_4", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID in JNT_VNTRS_SBSDRS_ENTTS_FINREP_LST)];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_9", "EQTY_INSTRMNTS_GRP_INDVDL_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(OBSRVD_AGNT_INTRNL_ID in CNSLDTD_ENTTS_PRDTNL_SUB_LST)];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_1", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID in CNSLDTD_ENTTS_ACCNTNG_LST)];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_0", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_DBTRS_FINREP [filter(OBSRVD_AGNT_INTRNL_ID in CNSLDTD_ENTTS_ACCNTNG_LST)];");
            customTransformation.put("G_IS_CMPNNT_LGL_ENTTY_1_3", "CNTRPRTS_NT_CMPNNTS := full_join (ALL_CNTRPRTS, CNTRPRTY_CMPNNTS using CNTRPRTY_ID);");
            customTransformation.put("P_PRSPCTV_IDS_1_0", "PRDNTL_NT_FLL_PRPTNL_CNSLDT_PRSPCTV_IDS := PRSPCTV_INF [filter(CNTRPRTY_ID in CNSLDTD_ENTTS_ACCNTNG_LST and ACCNTNG_FRMWRK = CNSTNT_ACCNTNG_FRMWRK_CNS)];");
            customTransformation.put("G_SHSG4_GROUP_REF_1_5", "SHSG4_GROUP_REF := SHSG4_GROUP_REF [rename GRP_ID to CRDTR_GRP_ID , GRP_TYP to GROUP_ID_TYPE, LEI to LEI_CRDTR_GRP, GROUP_TYPE to TYP_CRDTR_GRP, GROUP_SECTOR to INSTTTNL_SCTR_CRDTR_GRP, NM_ENTTY to NM_CRDTR_GRP];");
            customTransformation.put("G_SCRTY_MSTR_DT_1_7", "CNTRPRTS_E_SUB := CNTRPRTS_E [filter (PRSPCTV_ID = INSTTTN_ID)][calc identifier OBSRVD_AGNT_ID := CNTRPRTS_E_SUB#OBSRVD_AGNT_ID, identifier CNTRPRTY_ID := CNTRPRTS_E_SUB#CNTRPRTY_ID, identifier PD := CNTRPRTS_E_SUB#PD, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(CNTRPRTS_E_SUB#TMP_AGGR_MSR )group by OBSRVD_AGNT_ID, CNTRPRTY_ID, PD][drop TMP_AGGR_MSR];");
            customTransformation.put("G_NT_RL_ET_PRTCTN_RCVD_AC_1_0", "/* extract a subset of the cube Protections-protection providers - enriched containing only the primary protection providers (i.e. the variable Is primary protection provider equals \"TRUE (T)\") */ PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_E [filter (IS_PRMRY_PRTCTN_PRVDR = \"T\")][calc identifier PRTCTN_ID := PRTCTNS_PRTCTN_PRVDRS_E#PRTCTN_ID, identifier CNTRPRTY_ID := PRTCTNS_PRTCTN_PRVDRS_E#CNTRPRTY_ID, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum( PRTCTNS_PRTCTN_PRVDRS_E#TMP_AGGR_MSR )group by PRTCTN_ID, CNTRPRTY_ID][rename CNTRPRTY_ID to PRTCTN_PRVD_ID][drop TMP_AGGR_MSR];");
            customTransformation.put("G_RL_ESTT_PRTCTN_RCVD_AC_1_1", "/*extract a subset of the cube Protections-protection providers - enriched containing only the primary protection providers (i.e. the variable Is primary protection provider equals \"TRUE (T)\") */PRTCTNS_PRTCTN_PRVDRS_SUB := PRTCTNS_PRTCTN_PRVDRS_E [filter (IS_PRMRY_PRTCTN_PRVDR = \"T\")][calc identifier PRTCTN_ID := PRTCTNS_PRTCTN_PRVDRS_E#PRTCTN_ID, identifier CNTRPRTY_ID := PRTCTNS_PRTCTN_PRVDRS_E#CNTRPRTY_ID, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(PRTCTNS_PRTCTN_PRVDRS_E#TMP_AGGR_MSR )group by PRTCTN_ID, CNTRPRTY_ID][rename CNTRPRTY_ID to PRTCTN_PRVD_ID][drop TMP_AGGR_MSR];");
            customTransformation.put("G_ANCRDT_ENTTY_RSK_C_1_4", "/*Add the LEI of the observed agent*/ANCRDT_ENTTY_RSK_C := inner_join (ANCRDT_ENTTY_RSK_C as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID, LEI to OBSRVD_AGNT_CD] as B  using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.OBSRVD_AGNT_CD);");
            customTransformation.put("P_DRVTVS_FINREP_1_4", "DRVTVS_TRS := inner_join (DRVTVS_TRS as A, DRVTVS_TRS_FV as B using INSTRMNT_ID keep A.ALL_VARIABLES, B.FV);");
            customTransformation.put("P_CNTRPRTS_E_1_2", "inner_join (CNTRPRTS_E as A, EXTRNL_CRDT_ASSSSMNT [rename CRDT_QLTY_STP to  CRDT_QLTY_STP_CNTRPRTY, ECAI_ECA to ECAI_ECA_CNTRPRTY, IS_SHRT_TRM_CRDT_ASSSSMNT to IS_SHRT_TRM_CRDT_ASSSSMNT_CNTRPRTY] as B using EXTRNL_CRDT_ASSSSMNT keep A.ALL_VARIABLES, B.CRDT_QLTY_STP_CNTRPRTY, B.ECAI_ECA_CNTRPRTY, B.IS_SHRT_TRM_CRDT_ASSSSMNT_CNTRPRTY);");
            customTransformation.put("G_PRP_LNS_1_2", "LNS_AC := inner_join (LNS_AC as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID, LEI to OBSRVD_AGNT_CD] as B using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.OBSRVD_AGNT_CD);");
            customTransformation.put("P_OWND_SCRTS_E_1_2", "OWND_SCRTS_E := inner_join (OWND_SCRTS as A, D_CRRYNG_AMNT_SCRTS as B using SCRTY_ID keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
            customTransformation.put("G_ANCRDT_ENTTY_DFLT_C_1_7", "ANCRDT_ENTTY_DFLT_C := inner_join (ANCRDT_ENTTY_DFLT_C as A, LEI_CDS [rename CNTRPRTY_ID to OBSRVD_AGNT_ID, LEI to OBSRVD_AGNT_CD] as B using OBSRVD_AGNT_ID keep A.ALL_VARIABLES, B.OBSRVD_AGNT_CD);");
            customTransformation.put("P_DRVTVS_FINREP_1_10", "DRVTVS_MSTR_NTTNG_CRRNG_AMNT := left_join (DRVTVS_MSTR_NTTNG_CRRNG_AMNT as A, MSTR_NTTNG_AGRMNT_FINREP [rename MSTR_NTTNG_AGRMNT_ID to NTTNG_AGRMNT_ID] as B using NTTNG_AGRMNT_ID keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
            customTransformation.put("P_PRTCTNS_RCVD_FINREP_1_6", "PRTCTNS_RCVD_FINREP := inner_join (PRTCTNS_RCVD_FINREP as A, PRTCTNS_RCVD_FINREP_SUB as B using PRTCTN_ID keep A.PRTCTN_ID, A.FNNCL_GRNT_CNSDRD_MX_AMNT, A.TYP_PRTCTN, B.IS_RSTNTL_CLLTRL_FNNCL_GRNT, B.IS_CMMRCL_CLLTRL_FNNCL_GRNT);");
            customTransformation.put("P_LNS_ADVNCS_E_1_2", "LNS_ADVNCS_E := inner_join (LNS_ADVNCS_E as A, D_CRRYNG_AMNT_LNS_ADVNCS as B using INSTRMNT_UNQ_ID  keep A.ALL_VARIABLES, B.CRRYNG_AMNT);");
            customTransformation.put("P_CMMTMNTS_GVN_CSTMRS_FINREP_1_0", "CMMTMNTS_GVN_CSTMRS_FINREP := inner_join (CMMTMNTS_GVN_FINREP as A, CMMTMNTS_GVN_CSTMRS_RLTNSHP_E as B using CMMTMNT_UNQ_ID keep A.ALL_VARIABLES, B.CNTPRTY_ID);");
            customTransformation.put("D_CNTRY_FINREP_1_0", "/* map: (Institutional sector, International organisation) &rArr; Institutional sector*/ define operator D_CNTRY_FINREP(CNTRY scalar, INTRNTNL_ORGNSTN_CD scalar)  returns string is if (INTRNTNL_ORGNSTN_CD in MLTLTRL_DVLPMNT_BNKS_FINREP or INTRNTNL_TRGNSTN_CD in INTRNTNL_ORGNSTNS_FINREP) then \"_X\" else CNTRY  end operator");
            customTransformation.put("D_IS_CSH_DBT_INSTRMNT_ISSD_1_0", "define operator D_IS_CSH_DBT_INSTRMNT_ISSD(TYP_PRTCTN scalar, RPRTNG_AGNT scalar, GRP_INFRMTN scalar)  returns string is if (TYP_PRTCTN in {\"12\", \"15\"} and RPRTNG_AGNT in GRP_INFRMTN) then \"T\" else \"F\"  end operator");
            customTransformation.put("D_TYP_INSTRMNT_CMMTMNTS_FINREP_1_0", "define operator D_TYP_INSTRMNT_CMMTMNTS_FINREP(TYP_CMMTMNT scalar)  returns string is if (TYP_CMMTMNT in LN_CMMTMNTS_GVN) then \"910\" else if (TYP_CMMTMNT in OTHR_CMMTMNTS_GVN) then \"930\" else \"0\"  end operator");
            customTransformation.put("G_TYP_INSTRMNT_AC_1_0", "/*map: (Type of instrument, Is revolving loan, Is credit line other than revolving credit) &rArr; Type of instrument (AnaCredit)*/ define operator G_TYP_INSTRMNT_AC (TYP_INSTRMNT scalar, IS_RVLVNG_LN scalar, IS_CRDT_LN_OTHR_RV_CRDT scalar, INSTTTNL_SCTR scalar)     returns string is /*Current accounts*/if (TYP_INSTRMNT in {\"260\"}) then    /*Current accounts with MFIs are considered Deposits in AnaCredit*/if (INSTTTNL_SCTR in Sectors03) then \"1000\"    /*otherwise such current accounts are considered as Overdrafts*/else \"20\"  /*Factoring and Other trade receivables*/else if (TYP_INSTRMNT in {\"1020\", \"1023\"}) then     (if (IS_RVLVNG_LN = \"T\") then \"1001\"     else if (IS_CRDT_LN_OTHR_RV_CRDT = \"T\") then \"1002\"     else \"71\")     /*Other loans*/else if (TYP_INSTRMNT = \"1022\") then     (if (IS_RVLVNG_LN = \"T\") then \"1001\"     else if (IS_CRDT_LN_OTHR_RV_CRDT = \"T\") then \"1002\"     else \"1004\")    /*for other types of instruments the input classification is equal to the classification required in AnaCredit*/else TYP_INSTRMNT  end operator");
            customTransformation.put("D_INSTTTNL_SCTR_FINREP_1_0", "/* map: (Institutional sector, International organisation) &rArr; Institutional sector*/ define operator D_INSTTTNL_SCTR_FINREP(INSTTTNL_SCTR scalar, INTRNTNL_ORGNSTN_CD scalar)  returns string is if (INSTTTNL_SCTR in {\"S122_A\"} or INTRNTNL_ORGNSTN_CD in MLTLTRL_DVLPMNT_BNKS_FINREP) then \"S1_MDB\" else if (INSTTTNL_SCTR in Sectors04 or INTRNTNL_TRGNSTN_CD in INTRNTNL_ORGNSTNS_FINREP) then \"S13_IO\" else INSTTTNL_SCTR  end operator");
            customTransformation.put("G_IS_ELGBL_INSTRMNT_AC_1_0", "/*Returns 'True (T)' if the instrument is eligible w.r.t. AnaCredit reporting requirements, 'False (F)' otherwise*/ define operator G_IS_ELGBL_INSTRMNT_AC (TYP_INSTRMNT scalar, OFF_BLNC_SHT_AMNT scalar, OTSTNDNG_NMNL_AMNT scalar, ACCMLTD_WRTFFS scalar) returns string is (if (TYP_INSTRMNT in INSTRMNTS_ELGBL_AC and (OFF_BLNC_SHT_AMNT > 0 or OTSTNDNG_NMNL_AMNT > 0 or ACCMLTD_WRTFFS > 0)) then \"T\" else \"F\")  end operator");        
            customTransformation.put("D_TYP_CLLTRL_GRNT_RCVD1_1_0", "define operator D_TYP_CLLTRL_GRNT_RCVD1(TYP_PRTCTN scalar, ISSR_ID scalar)  returns string is if (CNSTNL_SL_CNLDT = \"T\" and TYP_PRTCTN in {\"12\"} and ISSR_ID in CONST_GRP) then \"1161\" else if (CNSTNT_SL_CNLDT = \"F\" and TYP_PRTCTN in {\"23\"}) then \"1161\" else if (TYP_PRTCTN in {\"21\"}) then \"1161\" else if (TYP_PRTCTN in {\"8\"}) then \"8\" else if (TYP_PRTCTN in {\"9\", \"10\"}) then \"10\" else if (TYP_PRTCTN in {\"5\", \"41\"}) then \"960\" else \"20\"  end operator");        
            customTransformation.put("D_CRRYNG_AMNT_1_0", "/*map: (Accounting classification, Fair value, Gross carrying amount excluding accrued interest, Accrued interest, Fair value changes due to hedge accounting, Accumulated impairment) &rArr; Carrying amount*/ define operator D_CRRYNG_AMNT(ACCNTNG_CLSSFCTN scalar, FV scalar, GRSS_CRRYNG_AMNT_E_INTRST scalar, ACCRD_INTRST scalar, FV_CHNG_HDG_ACCNTNG scalar, ACCMLTD_IMPRMNT scalar, CRRYNG_AMNT scalar, IS_CRRYNG_AMNT_DRVD scalar)   returns integer is if (IS_CRRYNG_AMNT_DRVD = \"T\") then  if (ACCNTNG_CLSSFCTN in FV_ACCNTNG_CLSSFCTNS) then FV  else (GRSS_CRRYNG_AMNT_E_INTRST + ACCRD_INTRST - ACCMLTD_IMPRMNT + FV_CHNG_HDG_ACCNTNG) else CRRYNG_AMNT  end operator");        
            customTransformation.put("D_ASSGN_EXPSR_CLSS1_1_0", "/*map: (Is exposure class applicable, Exposure class (input), Relationship with securitisation or credit transfer, Type of risk transfer, Is equity with 250% risk weight, Institutional sector, Credit quality status, International organisations, Security rank level, LEI code, Type of instrument, Is particular high risk, Is covered bond, Characteristics for credit risk, Is short-term credit assessment ) &rArr; Exposure class*/ define operator D_ASSGN_EXPSR_CLSS1 (IS_EXPSR_CLSS_APPLCBL scalar, EXPSR_CLSS scalar, RLTNSHP_SCRTSTN_CRDT_TRNSFR scalar, TYP_RSK_TRNSFR scalar, TYP_RSK_TRNSFR_ORGNL scalar, IS_EQTY_250_RSK_WGHT scalar, INSTTTNL_SCTR scalar, CHRCTRSTCS_CRDT_RSK scalar, IS_PRTCLR_HGH_RSK scalar, TYP_INSTRMNT scalar, SCRTY_RNK_LVL scalar, CRDT_QLTY_STTS scalar, INTRNTNL_ORGNSTN scalar, LEI scalar, IS_CVRD_BND scalar,IS_SHRT_TRM_CRDT_ASSSSMNT scalar)  returns string is if IS_EXPRSR_CLSS_APPLCBL = \"T\" then (/*for exposures for which the IRB approach is followed the exposure class and the risk weight are equal to the variables provided in the input layer*/ if APPRCH_PRDNTL_PRPSS not_in {\"66\", \"67\"} then EXPSR_CLSS /*The exposures to a securitisation are classified as “items representing securitisation positions”*/ else if (RLTNSHP_SCRTSTN_CRDT_TRNSFR = \"2\" and TYP_RSK_TRNSFR in {\"1\", \"2\"}) then \"15\" else if (RLTNSHP_SCRTSTN_CRDT_TRNSFR = \"4\" and TYP_RSK_TRNSFR_ORGNL in {\"1\", \"2\"}) then \"15\" /*investments in equity attracting a 250% risk weight are classified as “exposures to institutions”*/ else if (IS_EQTY_250_RSK_WGHT = \"T\" and INSTTTNL_SCTR in {\"S122_A\", \"S122_B1\", \"S122_B2\", \"S125_B\", \"S125_C\", \"S125_D\", \"S125_E\", \"S125_I\", \"S126\", \"S127\"} and CHRCTRSTCS_CRDT_RSK = \"1\") then \"9\" /*items associated with particular high risk are classified in the specific exposure class*/ else if IS_PRTCLR_HGH_RSK = \"T\" then \"14\" /*equity and subordinated debt securities are classified as “equity exposures”*/ else if TYP_INSTRMNT in {\"220\", \"230\"} or (TYP_INSTRMNT = \"210\" and SCRTY_RNK_LVL in {\"1\", \"2\", \"3\", \"5\", \"6\"}) then \"1\" /*if the issuer is defaulted then the exposure is classified in default*/ else if CRDT_QLTY_STTS in {\"18\", \"19\", \"20\"} then \"2\" /*if the issuer is an international organisation listed in Article 118 CRR, the exposure is classified in the specific class*/ else if (INTRNTNL_ORGNSTN in {\"1C\", \"5B\", \"4S\", \"4D\", \"4J1\", \"4J2\", \"4J3\", \"4J4\", \"4J5\", \"4J6\", \"4J7\", \"4J8\"} or LEI = \"222100OW6UHQXNHKN143\") then \"10\" /*if the issuer is a multilateral development bank listed in Article 117 CRR, the exposure is classified in the specific class*/ else if INTRNTNL_ORGNSTN in {\"5G\", \"7G\", \"5T\", \"5U\", \"1E\", \"1M\", \"5C\", \"5E\", \"5D\", \"7E\", \"5H\", \"5L\", \"5F\", \"4C\", \"4G\", \"1N\", \"7L\", \"7B\"} then \"11\" /*if the issuer’s sector is central bank or it is central government neither to be considered as public sector entity nor corporate, the exposure is classified as to central governments or central bank*/ else if (INSTTTNL_SCTR = \"S121\" or (INSTTTNL_SCTR = \"S1311\" and CHRCTRSTCS_CRDT_RSK = \"0\")) then \"6\" /*if the issuer’s sector is state or local government neither to be considered as public sector entity nor corporate, the exposure is classified as to regional governments or local authorities*/ else if (INSTTTNL_SCTR in {\"S1312\", \"S1313\"} and CHRCTRSTCS_CRDT_RSK = \"0\") then \"13\" /*if the issuer’s sector is social security funds or it is credit institution, central, state or local government to be considered as public sector entity, the exposure is classified as to public sector entities*/ else if "+
            "(INSTTTNL_SCTR = \"S1314\" or (INSTTTNL_SCTR in {\"S122_A\", \"S1311\", \"S1312\", \"S1313\"} and CHRCTRSTCS_CRDT_RSK = \"2\")) then \"12\" /*if the issuer’s sector is financial corporations, except some subsectors, and the entity is supervised, the exposure is classified as to institutions*/ else if (INSTTTNL_SCTR = \"S122_A\" and CHRCTRSTCS_CRDT_RSK = \"1\") then   (if IS_CVRD_BND = \"T\" then \"3\"  else if IS_SHRT_TRM_CRDT_ASSSSMNT = \"T\" then \"8\"  else \"9\") else if INSTTTNL_SCTR in {\"S122_B1\", \"S122_B2\", \"S125_B\", \"S125_C\", \"S125_D\", \"S125_E\", \"S125_I\", \"S126\", \"S127\"} then   (if not isnull(EXTRNL_CRDT_ASSSSMNT) and IS_SHRT_TRM_CRDT_ASSSSMNT = \"T\" then \"8\"   else \"9\") /*if the issuer’s sector is non-financial corporations, some subsectors of financial corporations, households or non-profit institutions serving households or it is financial corporations, except some subsectors, and the entity is not supervised or it is central, state or local government and the entity is considered as corporate, the exposure is classified as to corporates*/ else if (INSTTTNL_SCTR in {\"S11\", \"S125_A\", \"S128\", \"S129\", \"S14_A\", \"S14_B\", \"S15\"} or (INSTTTNL_SCTR in {\"S122_A\", \"S122_B1\", \"S122_B2\", \"S125_B\", \"S125_C\", \"S125_D\", \"S125_E\", \"S125_I\", \"S126\", \"S127\"} and CHRCTRSTCS_CRDT_RSK = \"0\") or (INSTTTNL_SCTR in {\"S1311\", \"S1312\", \"S1313\"} and CHRCTRSTCS_CRDT_RSK = \"3\")) then   (if (not isnull (EXTRNL_CRDT_ASSSSMNT) and IS_SHRT_TRM_CRDT_ASSSSMNT = \"T\") then \"8\"   else \"7\") /*if the issuer’s sector is MMFs or non-MMF investment funds, the exposure is classified as in the form of units or shares in CIUs*/ else if INSTTTNL_SCTR in {\"S123\", \"S124\"} then \"4\" /*in case none of the above conditions are met the exposure is classified as “other items” (in practice it should never happen)*/ else \"16\") /*in any other case the exposure class is not applicable*/else \"0\"   end operator");        
            customTransformation.put("D_IS_EXPSR_CLSS_APPLCBL1_1_0", "/*map: (Capital calculation approach for prudential purposes, Exposure class (input), Prudential portfolio, Derogation for small trading book business, Is short position, Issuer is part of the Group (prudential scope), Relationship with securitisation or credit transfer, Treatment of securitised/transferred assets in balance sheet, Significant risk transfer, Treatment of securitised/transferred assets in balance sheet of the originator, Significant risk transfer of the originator) &rArr; Is exposure class applicaple*/ define operator D_IS_EXPSR_CLSS_APPLCBL1 (APPRCH_PRDNTL_PRPSS scalar, EXPSR_CLSS scalar, PRDNTL_PRTFL scalar, DRGTN_SMLL_TRDNG_BK_BSNSS scalar, IS_SHRT_PSTN scalar, INTRA_GROUP_PRUD_SCOPE scalar, RLTNSHP_SCRTSTN_CRDT_TRNSFR scalar, TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT scalar, SGNFCNT_RSK_TRNSFR scalar, TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT_ORGNL scalar, SGNFCNT_RSK_TRNSFR_ORGNL scalar)  returns string is /*for exposures for which the IRB approach is followed the exposure class and the risk weight are equal to the variables provided in the input layer, therefore the exposure class is applicable*/ if APPRCH_PRDNTL_PRPSS not_in {\"66\", \"67\"} then (/*in case the security is in the trading book and the bank does not calculate the requirement for credit risk the exposure class and the risk weight are not applicable*/ if (PRDNTL_PRTFL = \"1\" and DRGTN_SMLL_TRDNG_BK_BSNSS = \"F\") then \"F\" /*for short positions the exposure class and the risk weight are not applicable*/ else if IS_SHRT_PSTN = \"T\" then \"F\" /*for intra-group holdings the exposure class and the risk weight are not applicable*/ else if INTRA_GROUP_PRUD_SCOPE = \"T\" then \"F\" /*for securitisation positions where the originator has not transferred significant credit risk the exposure class and the risk weight are not applicable*/ else if RLTNSHP_SCRTSTN_CRDT_TRNSFR = \"2\" and TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\"1\", \"2\"} and SGNFCNT_RSK_TRNSFR <> \"T\" then \"F\" else if RLTNSHP_SCRTSTN_CRDT_TRNSFR = \"4\" and TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT_ORGNL in {\"1\", \"2\"} and SGNFCNT_RSK_TRNSFR_ORGNL <> \"T\" then \"F\" /*for securitised/transferred assets that are not recognised in balance sheet or, for traditional securitisations, where the originator has transferred significant credit risk the exposure class and the risk weight are not applicable*/ else if RLTNSHP_SCRTSTN_CRDT_TRNSFR in {\"1\", \"2\", \"3\"} and (TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\"0\", \"3\"} or (TRTMNT_TRNSFRRD_ASSTS_BLNC_SHT in {\"1\", \"2\"} and TYP_RSK_TRNSFR = \"1\" and SGNFCNT_RSK_TRNSFR = \"T\"))then \"F\" else \"T\") else \"T\"  end operator");        
            customTransformation.put("GT_CB_VLD_AT_1_0", "/*map: (CB_CD, TM) &rArr; data set\\nPlease note that this function, from a pure technical perspective, retrieves the record from the table CUBE in the BIRD data base that matches the input parameter CB_CD and is valid at the time TM.\\n*/define operator GT_CB_VLD_AT(CB_CD scalar, TM scalar)  returns dataset is CUBE [filter(CUBE.FRAMEWORK_ID = \"BIRD\" and CUBE.CODE = CB_CD and CUBE.VALID_FROM <= TM and TM <= CUBE.VALID_TO)]  end operator");
            customTransformation.put("G_CRRNT_ACCNTS_LBLTS_AC_1_0", "/*Generate the cube CRRNT_ACCNTS_LBLTS_AC  for current accounts that are liabilities for the reporting requirements of AnaCredit\\njoin Deposits that are liabilities - enriched (DPSTS_THT_LBLTS_E)  with the related credit facility (UNQ_CRDT_FCLTS_INSTRMNTS) variables transferred from the related credit facility: Annualised agreed rate (ANNLSD_AGRD_RT), Assessment approach for credit quality status (APPRCH_CRDT_QLTY_STTS), Credit quality status (CRDT_QLTY_STTS), Is debt financing (IS_DBT_FNNCNG), Date of default status (DT_DFLT_STTS), Date of performing status (DT_PRFRMNG_STTS), Next interest rate reset date (DT_NXT_INTRST_RT_RST), Forbearance and renegotiation status (FRBRNC_STTS), Project finance loan (PRJCT_FNNC_LN), Purpose (PRPS), Recourse (RCRS), Is retail exposure (IS_RTL_EXPSR), Is serviced by the observed agent (IS_SRVCD_OBSRVD_AGNT), Syndicated contract scalar (SYNDCTD_CNTRCT_ID), Type of amortisation (TYP_AMRTSTN), Inception date (DT_INCPTN), Interest rate cap (INTRST_RT_CP), Interest rate floor (INTRST_RT_FLR), Interest rate reset frequency (INTRST_RT_RST_FRQNCY), Interest rate spread/margin (INTRST_RT_SPRD), Type of interest rate (TYP_INTRST_RT), Commitment amount at inception (CMMTMNT_INCPTN), Payment frequency (PYMNT_FRQNCY), Reference rate (RFRNC_RT) */CRRNT_ACCNTS_LBLTS_AC := inner_join (UNQ_CRDT_FCLTS_INSTRMNTS as A, DPSTS_THT_LBLTS_E [filter(TYP_INSTRMNT = 260)] as B using INSTRMNT_UNQ_ID keep B.INSTRMNT_UNQ_ID, B.PRSPCTV_ID, B.TYP_INSTRMNT, B.ACCMLTD_CHNGS_FV, B.ACCMLTD_CHNGS_FV_CR,  B.ACCNTNG_CLSSFCTN, B.CRRNCY_DNMNTN, B.CRRYNG_AMNT, B.DT_LGL_FNL_MTRTY, B.DT_RFRNC,  B.DT_STTLMNT, B.FV, B.FV_CHNG_HDG_ACCNTNG, B.FV_HRRCHY, B.FVO_DSGNTN,  B.GRSS_CRRYNG_AMNT_E_INTRST, B.IS_HFS, B.OBSRVD_AGNT_INTRNL_ID, B.OTSTNDNG_NMNL_AMNT, B.SBRDNTD_DBT, A.ANNLSD_AGRD_RT, A.APPRCH_CRDT_QLTY_STTS, A.CMMTMNT_INCPTN, A.CRDT_QLTY_STTS,  A.DT_DFLT_STTS, A.IS_DBT_FNNCNG, A.DT_PRFRMNG_STTS, A.DT_NXT_INTRST_RT_RST, A.FRBRNC_STTS, A.PRJCT_FNNC_LN, A.PRPS, A.RCRS, A.IS_RTL_EXPSR,  A.IS_SRVCD_OBSRVD_AGNT, A.SYNDCTD_CNTRCT_ID, A.TYP_AMRTSTN, A.DT_INCPTN, A.INTRST_RT_CP, A.INTRST_RT_FLR, A.INTRST_RT_RST_FRQNCY, A.INTRST_RT_SPRD, A.TYP_INTRST_RT, A.PYMNT_FRQNCY, A.RFRNC_RT)");
            customTransformation.put("G_ANCRDT_JNT_LBLTS_C_1_2", "/*join CNTRPRTS_CDS through CNTRPRTY_ID to \"translate\" OBSRVD_AGNT_ID and CNTRPRTY_ID in RIAD code (CNTRPRTY_EXTRNL_ID) as OBSRVD_AGNT_CD and ENTTY_RIAD_CD*/ANCRDT_JNT_LBLTS_C := inner_join( ANCRDT_JNT_LBLTS_C as A, CNTRPRTS_CDS[filter(TYP_CNTRPRTY_EXTRNL_ID = \"RIAD code\")][rename CNTRPRTY_ID to OBSRVD_AGNT_ID, CNTRPRTY_EXTRNL_ID to OBSRVD_AGNT_CD] as B using OBSRVD_AGNT_ID keep A.CNTRCT_ID , A.DT_RFRNC , A.ENTTY_ID , A.INSTRMNT_ID , A.JNT_LBLTY_AMNT, B.OBSRVD_AGNT_CD   );");
            customTransformation.put("G_SHSG4_GROUP_REF_1_2", "SHSG4_GROUP_REF := SHSG4_GROUP_REF [calc ACCNTNG_FRMWRK_CNS  := CNSTNT_ACCNTNG_FRMWRK_CNS ];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_11", "EQTY_INSTRMNTS_GRP_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(OBSRVD_AGNT_INTRNL_ID not_in CNSLDTD_ENTTS_PRDTNL_SUB_LST)];");
            customTransformation.put("G_F_40_02_REF_UNFLDD_FINREP_1_3", "EQTY_INSTRMNTS_GRP_FINREP := EQTY_INSTRMNTS_GRP_FINREP [filter(ISSR_ID not_in CNSTNT_INSTTTN_ID)];");
            customTransformation.put("G_IS_CMPNNT_LGL_ENTTY_1_1", "/*Extraction of all Counterparty identifiers (CNTRPRTY_ID) that are considered as components of the legal entity*/CNTRPRTY_CMPNNTS := CNTRPRTY_CMPNNTS [calc identifier CMPNNT_ID := CNTRPRTY_CMPNNTS#CMPNNT_ID, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(CNTRPRTY_CMPNNTS#TMP_AGGR_MSR )group by CMPNNT_ID][rename CMPNNT_ID to CNTRPRTY_ID][drop TMP_AGGR_MSR];");
            customTransformation.put("FOLD_F_04_09_REF_UNFLDD_F_04_09_REF_1_0", "F_04_09_REF := F_04_09_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_09_01_1_REF_UNFLDD_F_09_01_1_REF_1_0", "F_09_01_1_REF := F_09_01_1_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_18_00_c_REF_UNFLDD_F_18_00_c_REF_1_0", "F_18_00_c_REF := F_18_00_c_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_10_00_REF_UNFLDD_F_10_00_REF_1_0", "F_10_00_REF := F_10_00_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_19_00_b_REF_UNFLDD_F_19_00_b_REF_1_0", "F_19_00_b_REF := F_19_00_b_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_01_REF_UNFLDD_F_20_01_REF_1_0", "F_20_01_REF := F_20_01_REF_UNFLDD[unpivot MTRCS, BSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_07_REF_UNFLDD_F_04_07_REF_1_0", "F_04_07_REF := F_04_07_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_02_2_REF_UNFLDD_F_04_02_2_REF_1_0", "F_04_02_2_REF := F_04_02_2_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_10_REF_UNFLDD_F_04_10_REF_1_0", "F_04_10_REF := F_04_10_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_13_03_REF_UNFLDD_F_13_03_REF_1_0", "F_13_03_REF := F_13_03_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_01_02_REF_UNFLDD_F_01_02_REF_1_0", "F_01_02_REF := F_01_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_19_00_c_REF_UNFLDD_F_19_00_c_REF_1_0", "F_19_00_c_REF := F_19_00_c_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_06_REF_UNFLDD_F_04_06_REF_1_0", "F_04_06_REF := F_04_06_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_15_00_a_REF_UNFLDD_F_15_00_a_REF_1_0", "F_15_00_a_REF := F_15_00_a_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_13_02_REF_UNFLDD_F_13_02_REF_1_0", "F_13_02_REF := F_13_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_01_01_REF_UNFLDD_F_01_01_REF_1_0", "F_01_01_REF := F_01_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_07_1_REF_UNFLDD_F_20_07_1_REF_1_0", "F_20_07_1_REF := F_20_07_1_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_05_REF_UNFLDD_F_04_05_REF_1_0", "F_04_05_REF := F_04_05_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_40_02_REF_UNFLDD_F_40_02_REF_1_0", "F_40_02_REF := F_40_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_13_01_REF_UNFLDD_F_13_01_REF_1_0", "F_13_01_REF := F_13_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_11_02_REF_UNFLDD_F_11_02_REF_1_0", "F_11_02_REF := F_11_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_19_00_d_REF_UNFLDD_F_19_00_d_REF_1_0", "F_19_00_d_REF := F_19_00_d_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_15_00_b_REF_UNFLDD_F_15_00_b_REF_1_0", "F_15_00_b_REF := F_15_00_b_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_08_02_REF_UNFLDD_F_08_02_REF_1_0", "F_08_02_REF := F_08_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_42_00_REF_UNFLDD_F_42_00_REF_1_0", "F_42_00_REF := F_42_00_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_40_01_REF_UNFLDD_F_40_01_REF_1_0", "F_40_01_REF := F_40_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_11_01_REF_UNFLDD_F_11_01_REF_1_0", "F_11_01_REF := F_11_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_03_1_REF_UNFLDD_F_04_03_1_REF_1_0", "F_04_03_1_REF := F_04_03_1_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_19_00_a_REF_UNFLDD_F_19_00_a_REF_1_0", "F_19_00_a_REF := F_19_00_a_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_07_01_REF_UNFLDD_F_07_01_REF_1_0", "F_07_01_REF := F_07_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_02_1_REF_UNFLDD_F_04_02_1_REF_1_0", "F_04_02_1_REF := F_04_02_1_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_05_b_REF_UNFLDD_F_20_05_b_REF_1_0", "F_20_05_b_REF := F_20_05_b_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_02_REF_UNFLDD_F_20_02_REF_1_0", "F_20_02_REF := F_20_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_06_01_REF_UNFLDD_F_06_01_REF_1_0", "F_06_01_REF := F_06_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_08_REF_UNFLDD_F_04_08_REF_1_0", "F_04_08_REF := F_04_08_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_05_01_REF_UNFLDD_F_05_01_REF_1_0", "F_05_01_REF := F_05_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_01_REF_UNFLDD_F_04_01_REF_1_0", "F_04_01_REF := F_04_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_21_00_REF_UNFLDD_F_21_00_REF_1_0", "F_21_00_REF := F_21_00_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_06_REF_UNFLDD_F_20_06_REF_1_0", "F_20_06_REF := F_20_06_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_08_01_a_REF_UNFLDD_F_08_01_a_REF_1_0", "F_08_01_a_REF := F_08_01_a_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_31_01_REF_UNFLDD_F_31_01_REF_1_0", "F_31_01_REF := F_31_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_18_00_a_REF_UNFLDD_F_18_00_a_REF_1_0", "F_18_00_a_REF := F_18_00_a_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_41_02_REF_UNFLDD_F_41_02_REF_1_0", "F_41_02_REF := F_41_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_09_02_REF_UNFLDD_F_09_02_REF_1_0", "F_09_02_REF := F_09_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_04_04_1_REF_UNFLDD_F_04_04_1_REF_1_0", "F_04_04_1_REF := F_04_04_1_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_08_01_b_REF_UNFLDD_F_08_01_b_REF_1_0", "F_08_01_b_REF := F_08_01_b_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_41_01_REF_UNFLDD_F_41_01_REF_1_0", "F_41_01_REF := F_41_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_14_00_REF_UNFLDD_F_14_00_REF_1_0", "F_14_00_REF := F_14_00_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_04_REF_UNFLDD_F_20_04_REF_1_0", "F_20_04_REF := F_20_04_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_20_05_a_REF_UNFLDD_F_20_05_a_REF_1_0", "F_20_05_a_REF := F_20_05_a_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_18_00_b_REF_UNFLDD_F_18_00_b_REF_1_0", "F_18_00_b_REF := F_18_00_b_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_07_02_REF_UNFLDD_F_07_02_REF_1_0", "F_07_02_REF := F_07_02_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("FOLD_F_09_01_REF_UNFLDD_F_09_01_REF_1_0", "F_09_01_REF := F_09_01_REF_UNFLDD[unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_1_3", "/* Add Group info to cube DPSTS_THT_LBLTS_RESL */   DPSTS_THT_LBLTS_RESL := left_join( DPSTS_THT_LBLTS_RESL as A, GRP_E [rename LGL_ENTTY_ID to CNTPRTS_ID] as B using CNTPRTS_ID filter(A.DT_RFRNC = B.DT_RFRNC) keep A.ALL_VARIABLES, B.GRP_STRCTR_RLTNSHP, B.CNSLDTN_SCP, B.GRP_ID);");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_1_6", "/* Add MTRCS indicator */DPSTS_THT_LBLTS_RESL := DPSTS_THT_LBLTS_RESL [calc TYP_MRKT := \"0\"];");
            customTransformation.put("P_SCRTS_RESL_E_INTR_GRP_1_2", "SCRTS_RESL_E_INTR_GRP_ISSR_ENTT:= inner_join( SCRTS_RESL_E_INTR_GRP as A, GRP_RESL_INTR_GRP [filter A.DT_RFRNC = B.DT_RFRNC][rename LGL_ENTTY_ID to OBSRVD_AGNT_INTRNL_ID, NM_ENTTY to NM_ENTTY_ISSR, LEI to ENTTY_CD_ISSR_GRNTD] as B using OBSRVD_AGNT_INTRNL_ID  keep A.DT_RFRNC, A.OBSRVD_AGNT_INTRNL_ID, A.TYP_RSLTN_ITM, A.GVRND_EU_LW, A.MREL_ELGBL, A.OBSRVTN_VL, B.NM_ENTTY_ISSR , B.ENTTY_CD_ISSR_GRNTD, B.PRSPCTV_ID);");
            customTransformation.put("P_SCRTS_RESL_E_INTR_GRP_1_5", "SCRTS_RESL_E_INTR_GRP_ISSR := SCRTS_RESL_E_INTR_GRP_IISR [calc TYP_FNNCL_INTRCNNCTN := D_TYP_FNNCL_INTRCNNCTN(TYP_RSLTN_ITM)];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_2", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR_ENTT:= inner_join( OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP as A, GRP_RESL_INTR_GRP[filter A.DT_RFRNC = B.DT_RFRNC][rename LGL_ENTTY_ID to OBSRVD_AGNT_INTRNL_ID, NM_ENTTY to NM_ENTTY_ISSR, LEI to ENTTY_CD_ISSR_GRNTD] as B using OBSRVD_AGNT_INTRNL_ID keep A.DT_RFRNC, A.OBSRVD_AGNT_INTRNL_ID, A.TYP_RSLTN_ITM, A.GVRND_EU_LW, A.MREL_ELGBL, A.OBSRVTN_VL, NM_ENTTY_ISSR, ENTTY_CD_ISSR_GRNTD, B.PRSPCTV_ID);");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_3", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR_GRP:= inner_join(OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP as A, GRP_RESL_INTR_GRP [filter A.DT_RFRNC = B.DT_RFRNC][rename GRP_ID to OBSRVD_AGNT_INTRNL_ID, NM_ENTTY to NM_ENTTY_ISSR, LEI to ENTTY_CD_ISSR_GRNTD] as B using OBSRVD_AGNT_INTRNL_ID keep A.DT_RFRNC, A.OBSRVD_AGNT_INTRNL_ID, A.TYP_RSLTN_ITM, A.GVRND_EU_LW, A.MREL_ELGBL, A.OBSRVTN_VL, B.NM_ENTTY_ISSR , B.ENTTY_CD_ISSR_GRNTD, B.PRSPCTV_ID);");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_5", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR := OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_IISR [calc TYP_FNNCL_INTRCNNCTN :=  (D_TYP_FNNCL_INTRCNNCTN(TYP_RSLTN_ITM)) ];");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_6", "SCRTS_ISSD_RESL :=SCRTS_ISSD_RESL [calc TYP_MRKT := if IS_LSTD = \"T\" then \"31\" else \"10\"];");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_8", "SCRTS_ISSD_RESL_SBRDNTD := SCRTS_ISSD_RESL_SBRDNTD [calc TYP_RSLTN_ITM := \"24\" ];");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_10", "SCRTS_ISSD_RESL_SBRDNTD :=SCRTS_ISSD_RESL_SBRDNTD [calc OBSERVATION_VALUE := OTSTNDNG_NMNL_AMNT + ACCRD_INTRST];");
            customTransformation.put("P_OWND_SCRTS_RESL_1_0", "/* Create cube OWND_SCRTS_RESL from the cube 'Own Securities - enriched' */   OWND_SCRTS_RESL := OWND_SCRTS_E[calc STRCTRD_NT:=0][filter (IS_SHRT_PSTN = \"1\")][keep DT_RFRNC, OBSRVD_AGNT_INTRNL_ID, SCRTY_ID, PRSPCTV_ID, OTSTNDNG_NMNL_AMNT, ACCRD_INTRST, IS_OWND_FND, SBRDNTD_DBT, OTSTNDNG_NMNL_AMNT_SCRD, GVRND_EU_LW, STRCTRD_NT, DT_MTRTY, TYP_INSTRMNT, ISSR_ID, IS_LSTD, PRMRY_ASST_CLSSFCTN, SCRTY_RNK_LVL];");
            customTransformation.put("P_OWND_SCRTS_RESL_E_1_6", "OWND_SCRTS_RESL_E :=OWND_SCRTS_RESL_E [calc TYP_MRKT := if IS_LSTD = \"T\" then \"31\" else \"10\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_2", "/* Additional information of the group structure relationship, scope of consolidation and group identifier */   DRVTS_RESL := left_join(DRVTS_RESL as A, GRP_E[rename LGL_ENTTY_ID to CNTPRTS_ID] as B using CNTPRTS_ID filter (A.DT_RFRNC = B.DT_RFRNC) keep A.ALL_VARIABLES, B.GRP_STRCTR_RLTNSHP, B.CNSLDTN_SCP, B.GRP_ID);");
            customTransformation.put("P_DRVTS_RESL_E_1_10", "DRVTS_RESL := DRVTS_RESL [calc TYP_MRKT := if TYP_MRKT = \"2\" then \"31\" else \"10\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_13", "/* Set TYP_RSLTN_ITM to identify the row - TYP_RSLTN_ITM_15 - row 330 */  DRVTS_RESL_LBLTS_NT_MSTR_NTTNG_AGRMNT := DRVTS_RESL_LBLTS_NT_MSTR_NTTNG_AGRMNT [calc TYP_RSLTN_ITM := \"15\"  ];");
            customTransformation.put("P_DRVTS_RESL_E_1_19", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS [calc OBSERVATION_VALUE := CARRYNG_AMNT];");
            customTransformation.put("P_DRVTS_RESL_E_1_20", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS [calc TYP_RSLTN_ITM := \"15\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_24", "/* Calc Collateralised Amount with Close up */\\nDRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT := DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT [calc CLLTRLSD_AMNT_WTH_CLS := (FV_OF_MSTR_NTTNG_AGRMNT_TTL - CLLTRLSD_AMNT - CLS_OT_MSTR_NTTNG_AGRMNT_TTL) ];");
            customTransformation.put("P_DRVTS_RESL_E_1_25", "/* Determinens the dataset DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR with Derivates that have a Master Netting Agreement - after mark-to-market adjustments, prior to collateral offset */DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR := inner_join(DRVTS_RESL_MSTR_NTTNG_AGRMNT as A, DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT[filter (FV_OF_MSTR_NTTNG_AGRMNT_TTL < 0)] as B using MSTR_NTTNG_AGRMNT_ID keep A.ALL_VARIABLES);");
            customTransformation.put("P_DRVTS_RESL_E_1_26", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR [calc OBSERVATION_VALUE := FV ];");
            customTransformation.put("P_DRVTS_RESL_E_1_27", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PRR [calc TYP_RSLTN_ITM := \"16\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_28", "/* Create dataset DRVTVS_DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST for the derivative instruments that have a Master Netting Agreement - after mark-to-market adjustments, post collateral offset*/   DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST := inner_join(DRVTS_RESL_MSTR_NTTNG_AGRMNT as A, DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT[filter (CLLTRLSD_AMNT < 0)][rename CLLTRLSD_AMNT to OBSERVATION_VALUE] as B using MSTR_NTTNG_AGRMNT_ID keep A.ALL_VARIABLES, B.OBSERVATION_VALUE);");
            customTransformation.put("P_DRVTS_RESL_E_1_29", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST [calc  TYP_RSLTN_ITM := \"17\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_30", "/* Create dataset DRVTVS_DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST_CLS for the derivative instruments that have a Master Netting Agreement - after mark-to-market adjustments, post collateral offset, incorporating estimated close-out amounts*/   DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST_CLS := inner_join(DRVTS_RESL as A, DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT[filter (B.CLLTRLSD_AMNT_WTH_CLS < 0)][rename CLLTRLSD_AMNT_WTH_CLS to OBSERVATION_VALUE] as B  using MSTR_NTTNG_AGRMNT_ID keep A.ALL_VARIABLES, B.OBSERVATION_VALUE);");
            customTransformation.put("P_DRVTS_RESL_E_1_31", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST_CLS := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PST_CLS [calc TYP_RSLTN_ITM := \"18\"];");
            customTransformation.put("P_DRVTS_RESL_E_1_34", "/* Create dataset DRVTVS_DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR for the derivative instruments that have a Master Netting Agreement -taking into account prudential netting rules*/   DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR := inner_join( DRVTS_RESL_MSTR_NTTNG_AGRMNT as A, DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY2_TT as B using MSTR_NTTNG_AGRMNT_ID filter(B.FV_OF_MSTR_NTTNG_AGRMN_TTL < 0 and NMB_TYP_RSK = 1)  keep A.ALL_VARIABLES);");
            customTransformation.put("P_DRVTS_RESL_E_1_35", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR [calc OBSERVATION_VALUE := FV];");
            customTransformation.put("P_DRVTS_RESL_E_1_36", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR := DRVTS_RESL_MSTR_NTTNG_AGRMNT_THT_LBLTS_PNR [calc TYP_RSLTN_ITM := \"19\"];");
            customTransformation.put("P_NN_FNNCL_LBLTS_RESL_E_1_9", "NN_FNNCL_LBLTS_RESL_E := NN_FNNCL_LBLTS_RESL_E [calc OBSERVATION_VALUE := CRRYNG_AMNT];");
            customTransformation.put("P_NN_FNNCL_LBLTS_RESL_E_1_10", "NN_FNNCL_LBLTS_RESL_E := NN_FNNCL_LBLTS_RESL_E [calc TYP_RSLTN_ITM := \"26\" ];");
            customTransformation.put("OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_1_10", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E := OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E [calc OBSERVATION_VALUE := OTSTNDNG_NMNL_AMNT + ACCRD_INTRST];");
            customTransformation.put("G_Z_06_00_REF_UNFLDD_RESL_1_0", "GRP_RESL := GRP_E [filter (RLVNT_LE = \"1\" )][calc identifier DT_RFRNC := GRP_E#DT_RFRNC, identifier GRP_ID := GRP_E#GRP_ID, identifier LGL_ENTTY_ID := GRP_E#LGL_ENTTY_ID, identifier NM_DGS := GRP_E#NM_DGS, identifier NM_IPS := GRP_E#NM_IPS, identifier PRSPCTV_ID := GRP_E#PRSPCTV_ID, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(GRP_E#TMP_AGGR_MSR )group by DT_RFRNC, GRP_ID, LGL_ENTTY_ID, NM_DGS, NM_IPS, PRSPCTV_ID][drop TMP_AGGR_MSR];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_E_NT_INTR_GRP_4", "DPSTS_THT_LBLTS_RESL_E_NT_INTR_GRP := DPSTS_THT_LBLTS_RESL_E_NT_INTR_GRP [calc GRP_TYP := if DGT_CTP_CNNCTD = \"T\" then \"5\" else \"6\"];");
            customTransformation.put("P_SCRTS_RESL_E_NT_INTR_GRP_4", "SCRTS_RESL_E_NT_INTR_GRP := SCRTS_RESL_E_NT_INTR_GRP [calc GRP_TYP := if DGT_CTP_CNNCTD = \"T\" then \"5\" else \"6\"];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP_1_4", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP := OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP [calc GRP_TYP := if DGT_CTP_CNNCTD = \"T\" then \"5\" else \"6\"];");
            customTransformation.put("P_DRVTS_RESL_E_NT_INTR_GRP_1_4", "DRVTS_RESL_E_NT_INTR_GRP := DRVTS_RESL_E_NT_INTR_GRP [calc GRP_TYP := if DGT_CTP_CNNCTD = \"T\" then \"5\" else \"6\"];");
            customTransformation.put("FOLD_Z_05_01_REF_UNFLDD_Z_05_01_REF_1_0", "Z_05_01_REF := Z_05_01_REF_UNFLDD [unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("P_PSTS_THT_LBLTS_RESL_E_INTR_GRP_1_3", "DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_ENTT:= inner_join( DPSTS_THT_LBLTS_RESL_E_INTR_GRP as A, GRP_RESL_INTR_GRP [filter (A.DT_RFRNC = B.DT_RFRNC)][rename LGL_ENTTY_ID to OBSRVD_AGNT_INTRNL_ID, NM_ENTTY to NM_ENTTY_ISSR, LEI to ENTTY_CD_ISSR_GRNTD] as B using OBSRVD_AGNT_INTRNL_ID keep A.DT_RFRNC, A.OBSRVD_AGNT_INTRNL_ID, A.TYP_RSLTN_ITM, A.GVRND_EU_LW, A.MREL_ELGBL, A.OBSRVTN_VL, B.NM_ENTTY_ISSR, B.ENTTY_CD_ISSR_GRNTD, B.PRSPCTV_ID);");
            customTransformation.put("P_PSTS_THT_LBLTS_RESL_E_INTR_GRP_1_6", "DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR := DPSTS_THT_LBLTS_RESL_E_INTR_GRP_IISR [calc TYP_FNNCL_INTRCNNCTN := D_TYP_FNNCL_INTRCNNCTN(TYP_RSLTN_ITM)];");
            customTransformation.put("FOLD_Z_05_02_REF_UNFLDD_Z_05_02_REF_1_0", "Z_05_02_REF := Z_05_02_REF_UNFLDD [unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("P_DRVTS_RESL_E_INTR_GRP_1_2", "DRVTS_RESL_E_INTR_GRP_ISSR_ENTT:= inner_join(DRVTS_RESL_E_INTR_GRP as A, GRP_RESL_INTR_GRP [filter (A.DT_RFRNC = B.DT_RFRNC) ][rename LGL_ENTTY_ID to OBSRVD_AGNT_INTRNL_ID, NM_ENTTY to NM_ENTTY_ISSR, LEI to ENTTY_CD_ISSR_GRNTD ] as B using OBSRVD_AGNT_INTRNL_ID  keep A.DT_RFRNC, A.OBSRVD_AGNT_INTRNL_ID, A.TYP_RSLTN_ITM, A.GVRND_EU_LW, A.MREL_ELGBL, A.OBSRVTN_VL, B.NM_ENTTY_ISSR, B.ENTTY_CD_ISSR_GRNTD, B.PRSPCTV_ID);");
            customTransformation.put("P_DRVTS_RESL_E_INTR_GRP_1_5", "DRVTS_RESL_E_INTR_GRP_ISSR := DRVTS_RESL_E_INTR_GRP_IISR [calc TYP_FNNCL_INTRCNNCTN := D_TYP_FNNCL_INTRCNNCTN(TYP_RSLTN_ITM)];");
            customTransformation.put("FOLD_Z_04_00_REF_UNFLDD_Z_04_00_REF_1_0", "Z_04_00_REF := Z_04_00_REF_UNFLDD [unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("P_FNNCL_GRNTS_RCVD_RESL_1_4", "FNNCL_GRNTS_RCVD_RESL := FNNCL_GRNTS_RCVD_RESL [calc INTR_GRP_PSTN := D_CNTRPS_INTRGRP(GRP_STRCTR_RLTNSHP, CNSLDTN_SCP)];");
            customTransformation.put("P_FNNCL_GRNTS_RCVD_RESL_1_9", "FNNCL_GRNTS_RCVD_RESL := FNNCL_GRNTS_RCVD_RESL [calc GRP_TYP := if DGT_CTP_CNNCTD = \"T\" then \"5\" else \"6\"];");
            customTransformation.put("P_PSTS_THT_LBLTS_RESL_E_INTR_GRP_1_0", "GRP_RESL_INTR_GRP := GRP_E[filter (RLVNT_LE = \"1\")][filter (GRP_ID = OBSRVD_AGNT_INTRNL_ID)][calc identifier DT_RFRNC := GRP_E#DT_RFRNC, identifier GRP_ID := GRP_E#GRP_ID, identifier LGL_ENTTY_ID := GRP_E#LGL_ENTTY_ID, identifier PRSPCTV_ID := GRP_E#PRSPCTV_ID, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(GRP_E#TMP_AGGR_MSR )group by DT_RFRNC, GRP_ID, LGL_ENTTY_ID, PRSPCTV_ID][drop TMP_AGGR_MSR];");
            customTransformation.put("FOLD_Z_06_00_REF_UNFLDD_Z_06_00_REF_1_0", "Z_06_00_REF := Z_06_00_REF_UNFLDD [unpivot MTRCS, OBSERVATION_VALUE];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_1_0", "/* Create a dataset containing all Deposit Liabilities*/  DPSTS_THT_LBLTS_RESL:= DPSTS_THT_LBLTS_E [filter (TYP_INSTRMNT <> \"300\")][calc identifier DT_RFRNC := DPSTS_THT_LBLTS_E#DT_RFRNC, identifier OBSRVD_AGNT_INTRNL_ID := DPSTS_THT_LBLTS_E#OBSRVD_AGNT_INTRNL_ID, identifier INSTRMNT_UNQ_ID := DPSTS_THT_LBLTS_E#INSTRMNT_UNQ_ID, identifier PRSPCTV_ID := DPSTS_THT_LBLTS_E#PRSPCTV_ID, identifier SBRDNTD_DBT := DPSTS_THT_LBLTS_E#SBRDNTD_DBT, identifier EXCLDD_FRM_BL_IN := DPSTS_THT_LBLTS_E#EXCLDD_FRM_BL_IN, identifier DGS_ELGBL := DPSTS_THT_LBLTS_E#DGS_ELGBL, identifier DGS_DPST := DPSTS_THT_LBLTS_E#DGS_DPST, identifier CRRNCY_DNMNTN := DPSTS_THT_LBLTS_E#CRRNCY_DNMNTN, identifier OTSTNDNG_NMNL_AMNT := DPSTS_THT_LBLTS_E#OTSTNDNG_NMNL_AMNT, identifier OTSTNDNG_NMNL_AMNT_CVRD := DPSTS_THT_LBLTS_E#OTSTNDNG_NMNL_AMNT_CVRD, identifier OTSTNDNG_NMNL_AMNT_SCRD := DPSTS_THT_LBLTS_E#OTSTNDNG_NMNL_AMNT_SCRD, identifier ERLY_RDMPTN_DT := DPSTS_THT_LBLTS_E#ERLY_RDMPTN_DT, identifier PRCNTG_RDMPTN := DPSTS_THT_LBLTS_E#PRCNTG_RDMPTN, identifier DT_LGL_FNL_MTRTY := DPSTS_THT_LBLTS_E#DT_LGL_FNL_MTRTY, identifier GVRND_EU_LW := DPSTS_THT_LBLTS_E#GVRND_EU_LW, identifier RDMPTN_NTC_PRD := DPSTS_THT_LBLTS_E#RDMPTN_NTC_PRD, identifier ADDTNL_PRTCTN := DPSTS_THT_LBLTS_E#ADDTNL_PRTCTN, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(DPSTS_THT_LBLTS_E#TMP_AGGR_MSR )group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID, INSTRMNT_UNQ_ID, PRSPCTV_ID, SBRDNTD_DBT, EXCLDD_FRM_BL_IN, DGS_ELGBL, DGS_DPST, CRRNCY_DNMNTN, OTSTNDNG_NMNL_AMNT, OTSTNDNG_NMNL_AMNT_CVRD, OTSTNDNG_NMNL_AMNT_SCRD, ERLY_RDMPTN_DT, PRCNTG_RDMPTN, DT_LGL_FNL_MTRTY, GVRND_EU_LW, RDMPTN_NTC_PRD, ADDTNL_PRTCTN][drop TMP_AGGR_MSR];");
            customTransformation.put("P_OWND_SCRTS_RESL_1_0", "/* Create cube OWND_SCRTS_RESL from the cube 'Own Securities - enriched' */ OWND_SCRTS_RESL :=  OWND_SCRTS_E[filter (IS_SHRT_PSTN = \"1\")][ keep DT_RFRNC,  OBSRVD_AGNT_INTRNL_ID, SCRTY_ID, PRSPCTV_ID, OTSTNDNG_NMNL_AMNT, ACCRD_INTRST, IS_OWND_FND, SBRDNTD_DBT, OTSTNDNG_NMNL_AMNT_SCRD, GVRND_EU_LW, DT_MTRTY,  TYP_INSTRMNT, ISSR_ID, IS_LSTD, PRMRY_ASST_CLSSFCTN, SCRTY_RNK_LVL][calc STRCTRD_NT := \"0\"];");
            customTransformation.put("P_PRTCTNS_RCVD_RESL_1_1", "PRTCTNS_RL_ESTT_RESL := PRTCTNS_RL_ESTT_E [calc P1:=null][calc identifier PRTCTN_ID := PRTCTNS_RL_ESTT_E#PRTCTN_ID, identifier TYP_PRTCTN := PRTCTNS_RL_ESTT_E#TYP_PRTCTN, identifier P1 := PRTCTNS_RL_ESTT_E#P1, measure TMP_AGGR_MSR := 1 ][aggr TMP_AGGR_MSR := sum(PRTCTNS_RL_ESTT_E#TMP_AGGR_MSR )group by PRTCTN_ID, TYP_PRTCTN, P1][rename P1 to FNNCL_GRNT_CNSDRD_MX_AMNT][drop TMP_AGGR_MSR];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_32", "/* Create dataset for not subordinated Deposit not subordinated, elegible for DGS, not totally covered, not totally secured and not excluded from bail-in - residual other (other then Hauseholds,SME) */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_ERL := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD [filter (IS_EXCLDD_FRM_BL_IN = \"0\")][filter INSTTTNL_SCTR not_in (\"S1_A\",\"S11\")];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_53", "/* set the variable RSDL_MTRTY_INTRVL to identify the subrow - row from 341 to 344 */ DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_OTH [calc P1:=0][calc RSDL_MTRTY_INTRVL:= D_RSDL_MTRTY_D(P1, ERLY_RDMPTN_DT, DT_LGL_FNL_MTRTY, DT_RFRNC)];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_1_2", "/* Add INSTTTNL_SCTR and ENTRPRS_SZ to cube DPSTS_THT_LBLTS_RESL */   DPSTS_THT_LBLTS_RESL:= inner_join( DPSTS_THT_LBLTS_RESL as A, CNTRPRTS_E [filter (A.DT_RFRNC = B.DT_RFRNC)][calc INSTTTNL_SCTR:=G_INSTTTNL_SCTR_RSL(B.INSTTTNL_SCTR)][calc ENTRPRS_SZ:= G_ENTRPRS_SZ_RESL(B.ENTRPRS_SZ)] as B using CNTRPRTY_ID   keep A.ALL_VARIABLES, INSTTTNL_SCTR, ENTRPRS_SZ);");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_28", "DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_ERL := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_ERL [calc P1:=(OTSTNDNG_NMNL_AMNT - OTSTNDNG_NMNL_AMNT_CVRD - OTSTNDNG_NMNL_AMNT_SCRD)] [calc OBSERVATION_VALUE:= D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1) ];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_30", "/* set the variable RSDL_MTRTY_INTRVL to identify the subrow - row from 311 to 314 */ DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_OTH [calc P1:=0][calc RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, ERLY_RDMPTN_DT, DT_LGL_FNL_MTRTY, DT_RFRNC)];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_35", "/* set the OBSERVATION_VALUE */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_ERL := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_ERL[calc P1:= (OTSTNDNG_NMNL_AMNT - OTSTNDNG_NMNL_AMNT_CVRD - OTSTNDNG_NMNL_AMNT_SCRD)] [calc OBSERVATION_VALUE:=D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1)];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_66", "/* set the variable RSDL_MTRTY_INTRVL to identify the subrow - row from 321 to 324 */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_NT_SCRD_NT_EXC_DGS_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_NT_SCRD_NT_EXC_DGS_OTH [calc P1:=0] [calc RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, ERLY_RDMPTN_DT, DT_LGL_FNL_MTRTY, DT_RFRNC)]; ");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_73", "/* set the variable RSDL_MTRTY_INTRVL to identify the subrow - row from 361 to 364 */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_NT_SCRD_NT_EXC_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_NT_SCRD_NT_EXC_OTH [calc P1:=0][calc RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, ERLY_RDMPTN_DT, DT_LGL_FNL_MTRTY, DT_RFRNC)];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_37", "/* set the variable RSDL_MTRTY_INTRVL to identify the subrow - row from 321 to 324 */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RE_OTH [calc P1:=0] [calc RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, ERLY_RDMPTN_DT, DT_LGL_FNL_MTRTY, DT_RFRNC)]; ");
            customTransformation.put("P_SCRTS_ISSD_RESL_1_1", "/* Set the Institutional sector (INSTTTNL_SCTR) and Enterprise size (ENTRPRS_SZ) */   SCRTS_ISSD_RESL:= inner_join (SCRTS_ISSD_RESL as A, CNTRPRTS_E [rename CNTRPRTY_ID to ISSR_ID][calc ENTRPRS_SZ:= G_ENTRPRS_SZ_RESL(B.ENTRPRS_SZ), INSTTTNL_SCTR:=G_INSTTTNL_SCTR_RESL(B.INSTTTNL_SCTR)] as B using ISSR_ID  keep A.ALL_VARIABLES, INSTTTNL_SCTR, ENTRPRS_SZ);");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_20", "/* Set the variable RSDL_MTRTY_INTRVL */   SCRTS_ISSD_RESL_NT_SBRDNTD_NT_SCRD := SCRTS_ISSD_RESL_NT_SBRDNTD_NT_SCRD[calc P1:=0, P2:=null, RSDL_MTRTY_INTRVL:=(D_RSDL_MTRTY_D(P1, P2, DT_MTRTY, DT_RFRNC))];");
            customTransformation.put("P_OWND_SCRTS_RESL_1_1", "/* Set the Institutional sector (INSTTTNL_SCTR) and Enterprise size (ENTRPRS_SZ) */   OWND_SCRTS_RESL := inner_join( OWND_SCRTS_RESL as A, CNTRPRTS_E[rename CNTRPRTY_ID to ISSR_ID] [calc INSTTTNL_SCTR:=G_INSTTTNL_SCTR_RESL(B.INSTTTNL_SCTR), ENTRPRS_SZ:=G_ENTRPRS_SZ_RESL(B.ENTRPRS_SZ)]as B using ISSR_ID  keep A.ALL_VARIABLES, INSTTTNL_SCTR, ENTRPRS_SZ);");
            customTransformation.put("OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_1_2", "/* Add the institutional sector and enterprize size */   OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL:= inner_join( OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL as A, CNTRPRTS_E [filter (A.DT_RFRNC = B.DT_RFRNC) ] [calc INSTTTNL_SCTR:=G_INSTTTNL_SCTR_RESL(B.INSTTTNL_SCTR), ENTRPRS_SZ:= G_ENTRPRS_SZ_RESL(B.ENTRPRS_SZ)] as B using CNTRPRTY_ID  keep A.ALL_VARIABLES, INSTTTNL_SCTR, ENTRPRS_SZ);");
            customTransformation.put("P_CMMTMNT_RCVD_NT_FNNCL_GRNTS_RESL_1_3", "CMMTMNT_RCVD_NT_FNNCL_GRNTS_RESL := inner_join( CMMTMNT_RCVD_NT_FNNCL_GRNTS_RESL as A, CNTRPRTS_E [calc INSTTTNL_SCTR:= G_INSTTTNL_SCTR_RESL(B.INSTTTNL_SCTR)] as B using CNTRPRTY_ID  keep A.ALL_VARIABLES, INSTTTNL_SCTR);");
            customTransformation.put("P_FNNCL_GRNTS_RCVD_RESL_1_2", "FNNCL_GRNTS_RCVD_RESL := inner_join (FNNCL_GRNTS_RCVD_RESL as A, CNTRPRTS_E [calc  INSTTTNL_SCTR := G_INSTTTNL_SCTR_RESL(B.INSTTTNL_SCTR)] as B using CNTRPRTY_ID  keep A.ALL_VARIABLES, INSTTTNL_SCTR);");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_16", "/* Set the variable RSDL_MTRTY_INTRVL */   SCRTS_ISSD_RESL_NT_SBRDNTD_SCRD_USP := SCRTS_ISSD_RESL_NT_SBRDNTD_SCRD_USP [calc P1:=0, P2:=null, RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, P2, DT_MTRTY, DT_RFRNC)];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP_1_3", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP := OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_NT_INTR_GRP [calc INSTTTNL_SCTR := if DGT_CTP_CNNCTD = \"T\" then \"-4\" else INSTTTNL_SCTR];");
            customTransformation.put("P_SCRTS_RESL_E_NT_INTR_GRP_3", "SCRTS_RESL_E_NT_INTR_GRP := SCRTS_RESL_E_NT_INTR_GRP [calc INSTTTNL_SCTR:= if DGT_CTP_CNNCTD = \"T\" then \"-4\" else INSTTTNL_SCTR];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_51", "/* set the OBSERVATION_VALUE */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_ERL := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_ERL[calc P1:=(OTSTNDNG_NMNL_AMNT - OTSTNDNG_NMNL_AMNT_SCRD), OBSERVATION_VALUE:=D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1)];");
            customTransformation.put("D_CNTRPS_INTRGRP_1_0", "define operator D_CNTRPS_INTRGRP (GRP_STRCTR_RLTNSHP scalar, CNSLDTN_SCP scalar) /*map: ( relaztionship grpup, consolidation scope) &rArr; Digit counterparties intragroup */  returns string is if GRP_STRCTR_RLTNSHP = \"3\" and CNSLDTN_SCP = \"2\" then \"6\"  else \"5\"    end operator");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_54", "/* set the OBSERVATION_VALUE */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_NT_ELGBL_PR_SCRD_NT_EXC_OTH [calc P1:=(OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_SCRD)] [calc OBSERVATION_VALUE:=(OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_SCRD) - D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1)];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_31", "/* set the OBSERVATION_VALUE */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_PR_OTH [calc P1:=(OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_CVRD-OTSTNDNG_NMNL_AMNT_SCRD)] [calc OBSERVATION_VALUE:= (OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_CVRD-OTSTNDNG_NMNL_AMNT_SCRD) - D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1)];");
            customTransformation.put("P_DPSTS_LBLTS_RESL_E_1_38", "/* set the OBSERVATION_VALUE */   DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_OTH := DPSTS_THT_LBLTS_RESL_NT_SBRDNTD_ELGBL_NT_CVRD_NT_SCRD_RS_OTH [calc P1:=(OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_CVRD-OTSTNDNG_NMNL_AMNT_SCRD)] [calc OBSERVATION_VALUE:= (OTSTNDNG_NMNL_AMNT-OTSTNDNG_NMNL_AMNT_CVRD-OTSTNDNG_NMNL_AMNT_SCRD) - D_RSL_ERL_AMNT(DT_RFRNC, DT_LGL_FNL_MTRTY, RDMPTN_NTC_PRD, PRCNTG_RDMPTN, P1)];");
            customTransformation.put("G_EXCLDD_FRM_BL_IN_RW_1_0", "define operator G_EXCLDD_FRM_BL_IN_ROW (EXCLDD_FRM_BL_IN scalar) /* return TYP_RSLTN_ITM value for identify the row for instruments excluded from bail-in (EXCLDD_FRM_BL_IN <> \"0\" */  returns string is if EXCLDD_FRM_BL_IN = \"1\" then \"4\"  else if EXCLDD_FRM_BL_IN = \"2\" then \"5\"  else if EXCLDD_FRM_BL_IN = \"3\" then \"7\"    else if EXCLDD_FRM_BL_IN = \"4\" then \"8\"    else if EXCLDD_FRM_BL_IN = \"5\" then \"9\"    else if EXCLDD_FRM_BL_IN = \"6\" then \"11\" else if EXCLDD_FRM_BL_IN = \"7\" then \"10\"  else null   end operator");
            customTransformation.put("D_TYP_FNNCL_INTRCNNCTN_1_0", "/* return TYP_FNNCL_INTRCNNCTN value for identify the financial interconnections */define operator D_TYP_FNNCL_INTRCNNCTN (TYP_RSLTN_ITM scalar) returns string is if TYP_RSLTN_ITM in {2, 3, 4, 5, 6, 7, 8, 9, 10, 11} then \"1\"   else if TYP_RSLTN_ITM = \"13\" then \"2\"    else if TYP_RSLTN_ITM = \"14\" then \"3\"    else if TYP_RSLTN =\"15\" then \"4\"    else if TYP_RSLTN_ITM = \"20\" then \"5\"    else if TYP_RSLTN_ITM = \"21\" then \"6\"    else if TYP_RSLTN_ITM = \"22\" then \"7\"    else if TYP_RSLTN_ITM = \"23\" then \"8\"    else if TYP_RSLTN_ITM = \"24\" then \"9\"    else if TYP_RSLTN_ITM = \"25\" then \"10\"    else if TYP_RSLTN_ITM = \"26\" then \"11\"    else if TYP_RSLTN_ITM = \"27\" then \"12\"    else \"0\"    end operator");
            customTransformation.put("D_TYP_FNNCL_INTRCNNCTN_1_1", "/* return TYP_FNNCL_INTRCNNCTN value for identify the financial interconnections */define operator D_TYP_FNNCL_INTRCNNCTN_1 (TYP_RSLTN_ITM scalar) returns string is if TYP_RSLTN_ITM in {2, 3, 4, 5, 6, 7, 8, 9, 10 ,11} then \"1\"  else if TYP_RSLTN_ITM = \"13\" then \"2\"    else if TYP_RSLTN_ITM = \"14\" then \"3\"    else if TYP_RSLTN =\"15\" then \"4\"    else if TYP_RSLTN_ITM = \"20\" then \"5\"    else if TYP_RSLTN_ITM = \"21\" then \"6\"    else if TYP_RSLTN_ITM = \"22\" then \"7\"    else if TYP_RSLTN_ITM = \"23\" then \"8\"    else if TYP_RSLTN_ITM = \"24\" then \"9\"    else if TYP_RSLTN_ITM = \"25\" then \"10\"    else if TYP_RSLTN_ITM = \"26\" then \"11\" else if TYP_RSLTN_ITM = \"27\" then \"12\"  else \"0\"    end operator");
            customTransformation.put("G_INSTTTNL_SCTR_RESL_1_0", "/*map: Institutional sector &rArr; Institutional sector for Z02.00 RESL*/  define operator G_INSTTTNL_SCTR_RESL(INSTTTNL_SCTR scalar)    returns string is if INSTTTNL_SCTR in {\"S122_B1\", \"S122_B2\" ,\"S123\",\"S124_A\",\"S124_B\", \"S125_A\",\"S125_B\", \"S125_C\", \"S125_D\", \"S125_E\", \"S125_I\", \"S125_J\", \"S125_W\", \"S126_A\", \"S126_B\", \"S126_C\", \"S126_D\", \"S127_A\", \"S127_B\", \"S128_A\", \"S128_B\", \"S128_C\", \"S128_D\", \"S128_E\", \"S129\"}  then \"S12_D\" else if INSTTTNL_SCTR in (\"S1311\", \"S1312\", \"S1313\", \"S1314\")    then \"S1_H\"    else if INSTTTNL_SCTR in (\"S14_A\",\"S14_B\", \"S15\")    then \"S1_A\"    else if INSTTTNL_SCTR in (\"S122_A\")    then \"S1_MDB\"    else if INSTTTNL_SCTR in (\"-99\", \"-1\", \"-4\")    then \"-4\" else if INSTTTNL_SCTR = \"0\"  then \"0\"  else INSTTTNL_SCTR  end operator");
            customTransformation.put("P_SCRTS_ISSD_RESL_E_1_9", "/* Set the variable RSDL_MTRTY_INTRVL */   SCRTS_ISSD_RESL_SBRDNTD := SCRTS_ISSD_RESL_SBRDNTD[calc P1:=0, P2:=null][calc RSDL_MTRTY_INTRVL:=D_RSDL_MTRTY_D(P1, P2, DT_MTRTY, DT_RFRNC)];");
            
            customTransformation.put("P_DRVTS_RESL_E_1_16", "/* Determines the derivative instruments  that have sum of Carryng Amount < 0 at master netting agreement level. Create dataset DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY1_TT for sum of of Carryng Amount  for each master netting agreement id */DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY1_TT := DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY1 [aggr FV_OF_MSTR_NTTNG_AGRMNT_TTL := sum(CRRYNG_AMNT) group by MSTR_NTTNG_AGRMNT_ID];");
            customTransformation.put("P_DRVTS_RESL_E_1_22", "DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3_TT := DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY3 [aggr FV_OF_MSTR_NTTNG_AGRMNT_TTL := sum(FV),  CLS_OT_MSTR_NTTNG_AGRMNT_TTL := sum(CLS_OUT_AMNT) group by MSTR_NTTNG_AGRMNT_ID];");
            customTransformation.put("P_DRVTS_RESL_E_1_33", "/* Determines the derivative instruments  that have sum of Netting FV < 0 at master netting agreement level. Create dataset DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY2_TT for sum of Netting FV  for each master netting agreement id */DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY2_TT := DRVTS_RESL_MSTR_NTTNG_AGRMNT_APPLCBLTY2 [aggr FV_OF_MSTR_NTTNG_AGRMN_TTL := sum(FV),  NMB_TYP_RSK := count(TYP_RSK) group by MSTR_NTTNG_AGRMNT_ID];");
            customTransformation.put("G_Z_02_00_REF_1_6", "/*create structure {} where the observed value is the sum for each identifier of row and column of template */Z_02_00_REF := Z_02_00_REF [aggr OBSERVATION_VALUE := sum(OBSERVATION_VALUE) group by OBSRVD_AGNT_INTRNL_ID, MTRCS, TYP_OWN_FNDS, TYP_PRDNTL_ITM, TYP_RSLTN_ITM, DT_RFRNC, ENTRPRS_SZ, INSTTTNL_SCTR, RSDL_MTRTY_INTRVL, TYP_MRKT, INTR_GRP_PSTN, GVRND_EU_LW, EBA_Unit];");
            customTransformation.put("G_Z_06_00_REF_UNFLDD_RESL_1_3", "DPSTS_CVRD_AMNT_RESL := DPSTS_CVRD_RESL [aggr OTSNDNG_AMNT := sum(OBSERVATION_VALUE) group by OBSRVD_AGNT_INTRNL_ID, DT_RFRNC];");
            customTransformation.put("G_Z_06_00_REF_UNFLDD_RESL_1_6", "DPSTS_CVRD_ADD_RESL := DPSTS_CVRD_RESL.ADDTNL_PRTCTN [aggr OTSNDNG_AMNT := sum(ADDTNL_PRTCTN) group by OBSRVD_AGNT_INTRNL_ID, DT_RFRNC];");
            customTransformation.put("G_Z_05_01_REF_UNFLDD_RESL_1_1", "Z_05_01_REF_UNFLDD_RNK :=  Z_05_01_REF_UNFLDD [aggr OBSERVATION_VALUE := sum(OBSERVATION_VALUE) group by DT_RFRNC, ENTTY_CD];");
            customTransformation.put("G_Z_05_01_REF_UNFLDD_RESL_1_7", "Z_05_01_REF_UNFLDD :=  Z_05_01_REF_UNFLDD [aggr OBSERVATION_VALUE := sum(OBSERVATION_VALUE)   group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID, ENTTY_CD, NM_ENTTY, INSTTTNL_SCTR, CNTRY, GRP_TYP , TYP_FNNCL_INTRCNNCTN];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_E_INTR_GRP_1_8", "DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_TTL :=  DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR [aggr OTSTNDNG_AMNT := sum(OBSERVATION_VALUES) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_E_INTR_GRP_1_14", "DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW_TTL :=  DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, GVRND_EU_LW];");
            customTransformation.put("P_DPSTS_THT_LBLTS_RESL_E_INTR_GRP_1_19", "DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL_TTL :=  DPSTS_THT_LBLTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, MREL_ELGBL];");
            customTransformation.put("P_SCRTS_RESL_E_INTR_GRP_1_7", "SCRTS_RESL_E_INTR_GRP_ISSR_TTL :=  SCRTS_RESL_E_INTR_GRP_ISSR [aggr OTSTNDNG_AMNT := sum(OBSERVATION_VALUES) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID];");
            customTransformation.put("P_SCRTS_RESL_E_INTR_GRP_1_13", "SCRTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW_TTL :=  SCRTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, GVRND_EU_LW];");
            customTransformation.put("P_SCRTS_RESL_E_INTR_GRP_1_18", "SCRTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL_TTL :=  SCRTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, MREL_ELGBL];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_7", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR_TTL :=  OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR [aggr OTSTNDNG_AMNT := sum(OBSERVATION_VALUES) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_13", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW_TTL :=  OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, GVRND_EU_LW];");
            customTransformation.put("P_OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_1_18", "OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E_INTR_GRP_ISSR_MREL_ELGBL_TTL :=  OTHR_FNNCL_LBLTS_NT_FNNCL_GRNTS_LN_CMMTMNTS_GVN_RESL_E [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, MREL_ELGBL];");
            customTransformation.put("P_DRVTS_RESL_E_INTR_GRP_1_7", "DRVTS_RESL_E_INTR_GRP_ISSR_TTL :=  DRVTS_RESL_E_INTR_GRP_ISSR [aggr OTSTNDNG_AMNT := sum(OBSERVATION_VALUES) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID];");
            customTransformation.put("P_DRVTS_RESL_E_INTR_GRP_1_13", "DRVTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW_TTL :=  DRVTS_RESL_E_INTR_GRP_ISSR_UNDR_THRD_CNTR_LW [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, GVRND_EU_LW];");
            customTransformation.put("P_DRVTS_RESL_E_INTR_GRP_1_18", "DRVTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL_TTL :=  DRVTS_RESL_E_INTR_GRP_ISSR_MREL_ELGBL [aggr OTSTNDNG_AMNT := sum(OBSRVTN_VL) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD ,NM_ENTTY_ISSR, NM_ENTTY_CRDTR, TYP_FNNCL_INTRCNNCTN, PRSPCTV_ID, MREL_ELGBL];");
            customTransformation.put("G_Z_04_00_REF_UNFLDD_RESL_1_1", "Z_04_00_REF_UNFLDD := Z_04_00_REF_UNFLDD [aggr OTSTNDNG_AMNT := sum(OTSTNDNG_AMNT) group by DT_RFRNC, OBSRVD_AGNT_INTRNL_ID,  ENTTY_CD_CRDTR_HLDR_GRNTR, ENTTY_CD_ISSR_GRNTD, NM_ENTTY_CRDTR_HLDR_GRNTR, NM_ENTTY_ISSR_GRNTD, GVRND_EU_LW, MREL_ELGBL , RL_CNTRCT_ISSR, RL_CNTRCT_CRDTR, TYP_FNNCL_INTRCNNCTN , PRSPCTV_ID];");
            customTransformation.put("G_Z_05_02_REF_UNFLDD_RESL_1_3", "Z_05_02_REF_UNFLDD := Z_05_02_REF_UNFLDD [aggr OTSTNDNG_AMNT := sum(OTSTNDNG_AMNT) group by DT_RFRNC, INSTTTNL_SCTR, OBSRVD_AGNT_INTRNL_ID, TYP_ACCNTNG_ITM, TYP_INSTRMNT, ENTTY_CD, NM_ENTTY , GRP_TYP, TYP_FNNCL_INTRCNNCTN, CNTRY];");
            customTransformation.put("G_Z_05_02_REF_UNFLDD_RESL_1_4", "Z_05_02_REF_UNFLDD_RNK  :=  Z_05_02_REF_UNFLDD [aggr OBSERVATION_VALUE := sum(OTSTNDNG_AMNT) group by DT_RFRNC, ENTTY_CD, TYP_FNNCL_INTRCNNCTN];");
            customTransformation.put("G_Z_05_01_REF_UNFLDD_RESL_1_2", "Z_05_01_REF_UNFLDD_RNK := Z_05_01_REF_UNFLDD_RNK [ calc OBSERVATION_VALUE := rank (over (partition by  DT_RFRNC, ENTTY_CD order by DT_RFRNC))];");
            customTransformation.put("G_Z_05_02_REF_UNFLDD_RESL_1_5", "Z_05_02_REF_UNFLDD_RNK := Z_05_02_REF_UNFLDD_RNK [calc OBSERVATION_VALUE := rank( over (partition by  DT_RFRNC, ENTTY_CD, TYP_FNNCL_INTRCNNCTN order by DT_RFRNC))];");
            customTransformation.put("G_Z_05_01_REF_UNFLDD_RESL_1_3", "Z_05_01_REF_UNFLDD_RNK_MX := Z_05_01_REF_UNFLDD_RNK [ aggr MX_RNK := max(OBSERVATION_VALUE) group by DT_RFRNC];");
            customTransformation.put("G_Z_05_02_REF_UNFLDD_RESL_1_6", "Z_05_02_REF_UNFLDD_RNK_MX := Z_05_02_REF_UNFLDD_RNK [aggr MX_RNK := max(OBSERVATION_VALUE) group by DT_RFRNC];");
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
		
        if (vtlErrorListener.get_s().equals("error")) {
			errorFound = true;
			output=output+" VTL1 ERROR";
		}
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
