/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.sqtds.antlr4.java.JavaLexer;
import org.sqtds.antlr4.java.JavaParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InsertSerialID {
    public static void main(String[] args) throws Exception {
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        String str = null;
        while ((str=bufferedReader.readLine()) != null) {
            InputStream is = new FileInputStream(str);
            ANTLRInputStream input = new ANTLRInputStream(is);

            JavaLexer lexer = new JavaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
            ParseTree tree = parser.compilationUnit(); // parse

            ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
            InsertSerialIDListener extractor = new InsertSerialIDListener(tokens);
            walker.walk(extractor, tree); // initiate walk of tree with listener

            // print back ALTERED stream
            System.out.println(extractor.rewriter.getText());
        }
    }
}
