package com.lucas;

/*
Emulador de cache configurável
*/
public class App 
{
    public static void main( String[] args )
    {
        // Cria a cache antes de criar o arquivo 
        FileManager fp = new FileManager();
        Cache dL1 = new Cache();
        // Cache iL1 = new Cache();

        fp.runSimulation(dL1);
        // fp.runSimulation(iL1);
    }

}
