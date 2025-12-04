package com.lucas;

import java.util.Scanner;

/*
Emulador de cache configurável
*/
public class App 
{
    public static void main( String[] args )
    {   
        // Padrão de entrada do usuário:
        // cache_simulator <nsets_L1>: <bsize_L1>: <assoc_L1> arquivo_de_entrada
        // cache_simulator <nsets_iL1>: <bsize_iL1>: <assoc_iL1> <nsets_dL1>:<bsize_dL1>:<assoc_dL1> arquivo_de_entrada
        // L1 1024:4:16 "address.bin"
        // iL1 1024:4:16 dL1 1024:4:16 "address.bin"

        // Ok, então se eu só inserir 1 é uma cache pra dados e instrução, se eu inserir duas, é splitted entre dados e instrução
        // A moral é que eu vou ter que pegar essa string de entrada e dividir...
        
        boolean isSplitted;
        Scanner terminal = new Scanner(System.in); //lê inputs do terminal
        
        System.out.println("Config: ");
        String in = terminal.nextLine(); // Lê a linha inteira digitada
        terminal.close();

        // não vou fazer verificações de segurança por enquanto

        String[] partes = in.split(" ");    //divide a string nos espaços
        
        //Com base no padrão de string esperado, se sairem 3 elementos, é normal, se sairem 5 elementos é splitted
        if(partes.length == 5) {
            isSplitted = true;
        } else {
            isSplitted = false;
        }

        if (isSplitted) {
            String nameDataCache    = partes[0],
                   nameInstrCache   = partes[2],
                   filename         = partes[4]; 

            // Separa as strings de dados
            String[] configDataCache  = partes[1].split(":");
            String[] configInstrCache = partes[3].split(":");

            // Converte as strings para inteiros
            int data_nsets  = Integer.parseInt(configDataCache[0]),
                data_assoc  = Integer.parseInt(configDataCache[1]),
                data_bsize  = Integer.parseInt(configDataCache[2]),
                instr_nsets = Integer.parseInt(configInstrCache[0]),
                instr_assoc = Integer.parseInt(configInstrCache[1]),
                instr_bsize = Integer.parseInt(configInstrCache[2]);

            FileManager fp = new FileManager(filename, isSplitted);
            
            // Cria a cache de dados
            Cache dL1 = new Cache(nameDataCache, data_nsets, data_assoc, data_bsize);
            Cache iL1 = new Cache(nameInstrCache, instr_nsets, instr_assoc, instr_bsize);

            fp.runSimulation(dL1, iL1);

        // Se for só uma cache, assuma que é uma de dados e executa
        } else {
            // nome = partes[0], arquivo = partes[2]
            String nameCache = partes[0],
                   filename  = partes[2];

            String[] config = partes[1].split(":"); //divide a string nos :
            // config[0] = nsets, config[1] = assoc, config[2] = bsize

            int nsets = Integer.parseInt(config[0]),
                assoc = Integer.parseInt(config[1]),
                bsize = Integer.parseInt(config[2]);


            // Cria a cache antes de criar o arquivo 
            FileManager fp = new FileManager(filename, isSplitted);
            Cache dL1 = new Cache(nameCache, nsets, assoc, bsize);
            fp.runSimulation(dL1, null);
        }
        
    }

}
