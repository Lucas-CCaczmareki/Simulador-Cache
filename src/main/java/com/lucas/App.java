package com.lucas;

/*
Emulador de cache configurável
*/
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );


        // Cria a cache antes de criar o arquivo 
        FileManager fp = new FileManager("teste.bin", 4);
        
        fp.writeBinFile();
        fp.readBinFile();
    }

}
