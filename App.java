import error.CompileError;

import java.io.*;
import java.util.List;
import java.util.Scanner;

public class App{
    public static void main(String[] args) throws CompileError, IOException {
        InputStream input = new FileInputStream(args[0]);
        //InputStream input = new FileInputStream("input.txt");
        Scanner scanner;
        scanner = new Scanner(input);
        StringIter iter = new StringIter(scanner);
        Tokenizer tokenizer = new Tokenizer(iter);
        Analyser analyzer = new Analyser(tokenizer);
        analyzer.analyse();


        System.out.println("全局符号表大小："+analyzer.globalmap.size());
        System.out.println("全局符号表：");
        for (Global global : analyzer.globalmap) {
            System.out.println(global);
        }
        System.out.println("函数：");
        for (Function function : analyzer.functionmap) {
            System.out.println(function);
        }

        //输出格式转换
        Target target = new Target(analyzer.globalmap, analyzer.functionmap);
        List<Byte> result= target.out();
        byte[] chu = new byte[result.size()];
        for (int i = 0; i < result.size(); ++i) chu[i] = result.get(i);


        //输出
        DataOutputStream output = new DataOutputStream(new FileOutputStream(new File(args[1])));
        output.write(chu);
    }

}
