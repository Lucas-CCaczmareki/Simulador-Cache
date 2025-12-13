package com.lucas;

import java.io.*;
import java.util.Random;

public class FileManager {
    // Atributos
    // static faz com que essa variável seja comum a todas instâncias (ou seja, 1 só)
    // file name é simbólico. Isso na real é o nome do endereço do arquivo no meu pc num objeto java
        // Talvez seja necessário incluir os diretórios.
    private static String dataFile;
    private String addressFile;
    private boolean isSplitted;

    private int dataCache_bsize;
    private int instrCache_bsize;

    private int n;          //número de blocos q vai gerar e endereços

    //Construtor padrão
    public FileManager() {
        dataFile = "data.bin";
        addressFile = null;
        this.isSplitted = false;

        this.dataCache_bsize = 4;
        this.instrCache_bsize = 4;

        this.n = 10000;          //altera quantos blocos de memória vão ser criados (no data.bin)
    }

    // //Construtor
    public FileManager(String addressFile, boolean isSplitted) {
        
        if (addressFile.equals("null")) { 
            this.addressFile = null;
        } else {
            this.addressFile = addressFile;
        }

        this.isSplitted = isSplitted;
        dataFile = "data.bin";

        this.dataCache_bsize = 4;
        this.instrCache_bsize = 4;

        this.n = 10000;          //altera quantos blocos de memória vão ser criados (no data.bin)
    }

    public void runSimulation(Cache dataCache, Cache instrCache) {
        
        if (isSplitted) {
            this.dataCache_bsize = dataCache.getBsize();
            this.instrCache_bsize = instrCache.getBsize();

            writeDataFile(); //a partir daqui eu tenho data.bin e o address.bin (se não foi identificado antes)
            
            // Vou considerar que o padrão é
                //byte endereço
                // 0 = dado, 1 = instrução

            System.out.println("\n=== INICIANDO SIMULAÇÃO COM CACHE SPLITTED ===\n");

            try (DataInputStream fp = new DataInputStream(new FileInputStream(addressFile))) {
                
                // Lê todos os endereços no arquivo e faz as buscas na cache
                while (true) {
                    try {
                        int address = fp.readInt();
                        int type = fp.readInt();                        

                        //Endereço de dado
                        if (type == 0) {
                            int data = dataCache.search(address);

                            if (data != -1) {
                                // System.out.println("(data cache) Hit: Dado " + data);
                            } else {
                                try (RandomAccessFile raf = new RandomAccessFile(dataFile, "r")) {
                                    
                                    //como é um inteiro, vai truncar a parte decimal, arredondando pra baixo, como eu disse antes
                                    int blockNumber = address / dataCache_bsize; 
                                    int blockAddress = blockNumber * dataCache_bsize; //endereço do bloco no arquivo em byte

                                    raf.seek(blockAddress); //move o ponteiro até o byte do bloco
                                    
                                    byte[] block = new byte[dataCache_bsize]; // cria o bloco

                                    //read fully lê byte a byte até encher o vetor
                                    raf.readFully(block);   //pega todos os dados do arquivo e bota no bloco
                                    dataCache.insert(address, block);

                                 } catch (FileNotFoundException e) {
                                    System.err.println("X -> Arquivo de dados não encontrado: " + e.getMessage());
                                 } catch (IOException e) {
                                    System.err.println("X -> Erro de I/O ao abrir address.bin: " + e.getMessage());
                                }
                            }

                        //Endereço de instrução
                        } else if (type == 1) {
                            int data = instrCache.search(address);

                            if (data != -1) {
                                // System.out.println("(instr cache) Hit! Dado: " + data);
                            } else {
                                try (RandomAccessFile raf = new RandomAccessFile(dataFile, "r")) {
                                    
                                    //como é um inteiro, vai truncar a parte decimal, arredondando pra baixo, como eu disse antes
                                    int blockNumber = address / instrCache_bsize; 
                                    int blockAddress = blockNumber * instrCache_bsize; //endereço do bloco no arquivo em byte

                                    raf.seek(blockAddress); //move o ponteiro até o byte do bloco
                                    
                                    byte[] block = new byte[instrCache_bsize]; // cria o bloco

                                    //read fully lê byte a byte até encher o vetor
                                    raf.readFully(block);   //pega todos os dados do arquivo e bota no bloco
                                    instrCache.insert(address, block);

                                 } catch (FileNotFoundException e) {
                                    System.err.println("X -> Arquivo de dados não encontrado: " + e.getMessage());
                                 } catch (IOException e) {
                                    System.err.println("X -> Erro de I/O ao abrir address.bin: " + e.getMessage());
                                }
                            }
                        }
                    } catch (EOFException e) {
                        break; //fim do arquivo de endereço
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("X -> Arquivo de endereços não encontrado: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("X - > Erro ao ler arquivo de endereços: " + e.getMessage());
            }

            dataCache.printLog();
            instrCache.printLog();

        // Caso seja um cache normal
        } else {
            this.dataCache_bsize = dataCache.getBsize();

            // E aqui eu gerencio
            writeDataFile(); //a partir daqui eu tenho data.bin e address.bin

            System.out.println("\n=== INICIANDO SIMULAÇÃO COM CACHE UNIFICADA ===\n");

            // Ler um endereço e tenta fazer um acesso na cache.
            try (DataInputStream fp = new DataInputStream(new FileInputStream(addressFile))) {
                // Lê todos os endereços no arquivo e faz as buscas na cache
                while (true) {
                    try {
                        int address = fp.readInt();
                        // byte type = 
                        fp.readInt(); //só pra avançar o ponteiro
                        
                        // A cache unificada deve tratar dados e instruções (se eu n to louco)
                        // to usando o nome de dataCache pra facilitar minha vida aqui
                        // já que se ela é unificada a instrCache = null e a dataCache = cache unificada
                        int data = dataCache.search(address);
                        


                        // Cache retorna -1 em caso de miss
                        if (data != -1) {
                            // System.out.println("Hit! '" + data + "' encontrado!");
                        } else {
                            // System.out.println("Miss! Inserindo bloco na cache...");
                            
                            // Acessamos o arquivo de dados no modo aleatório pra poder acessar qualquer parte
                            try (RandomAccessFile raf = new RandomAccessFile(dataFile, "r")) {
                                //como é um inteiro, vai truncar a parte decimal, arredondando pra baixo, como eu disse antes
                                int blockNumber = address / dataCache_bsize; 
                                int blockAddress = blockNumber * dataCache_bsize; //endereço do bloco no arquivo em byte

                                raf.seek(blockAddress); //move o ponteiro até o byte do bloco
                                
                                byte[] block = new byte[dataCache_bsize]; // cria o bloco

                                //read fully lê byte a byte até encher o vetor
                                raf.readFully(block);   //pega todos os dados do arquivo e bota no bloco
                                dataCache.insert(address, block);

                            } catch (FileNotFoundException e) {
                                System.err.println("X -> Arquivo de dados não encontrado: " + e.getMessage());
                            } catch (IOException e) {
                                System.err.println("X -> Erro de I/O ao abrir data.bin: " + e.getMessage());
                            }
                        }
    
                    } catch (EOFException e) {
                        break; //fim do arquivo de endereço
                    }
                }

            } catch (FileNotFoundException e) {
                System.err.println("X -> Arquivo de endereços não encontrado: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("X - > Erro ao ler arquivo de endereços: " + e.getMessage());
            }

            dataCache.printLog();
        }
        
    }


    /*
    * Cria o arquivo de dados com pelo menos 1 bloco completo. Então se bsize = 4, 1 inteiro por bloco.
    * to considerando que só vai ter inteiros no campo de dado, ent o tamanho da palavra é sempre 4 bytes.
    * Como a cache já foi criada antes de criarmos o arquivo, bsize teoricamente é uma potência de 2 e >= 4.
    * 
    * Se eu tiver saco, eu separo os arquivos de dados entre instruções e dados depois
    * 
    */
    private File writeDataFile() {
        System.out.println("Escrevendo arquivo de dados binário...");

        // Primeiro: descobrir o maior endereço no address.bin
        int maxAddress = 0;

        if(addressFile == null) {
            writeAddressFile();
        }

        try (DataInputStream fp = new DataInputStream(new FileInputStream(addressFile))) {
            while (true) {
                try {
                    int address = fp.readInt();
                    // byte type = 
                    fp.readInt(); // ignorado
                    
                    if (address > maxAddress) maxAddress = address;
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("X -> Erro ao ler address.bin para descobrir tamanho da memória: " + e.getMessage());
        }

        // calcula quantos blocos são necessários no data.bin
        int maxBlockNumber = maxAddress / dataCache_bsize;
        int totalWords = (maxBlockNumber + 1) * (dataCache_bsize / 4);

        // Procura o endereço do arquivo 
        File file = new File(dataFile);

        // Só pra garantir
        if(!file.exists()) {
            System.err.println("X -> Erro ao criar arquivo binário!");
        }

        // Abre um fluxo de dados de um arquivo.
            // Devido ao FileOutputStream, se o arquivo não existir, ele é criado
            // (lembrando que dessa maneira ele é zerado toda vez que iniciamos o programa)
        try (DataOutputStream fp = new DataOutputStream(new FileOutputStream(file))) {

            for(int i = 0; i < totalWords; i++) {
                fp.writeInt(i); //escreve n blocos de inteiros sequenciais no arquivo (posso deixar aleatório dps)
            }
            System.out.println("Arquivo de teste criado com sucesso!");

        // Se não conseguioiu abrir o arquivo
        } catch (IOException e) {
            System.err.println("X - Falha ao escrever no arquivo! " + e.getMessage());
        }

        return file;
        
    }

    /*
    Gera um arquivo com endereços que vão de 0 até o máximo de inteiros do arquivo de memória

    // ATENÇÃO: // Como eu também to emulando a cache de uma maneira simples, apenas com inteiros
                // vou garantir que os endereços sejam sempre múltiplos de 4, por que assim o offset vai estar sempre alinhado
                // e eu não vou ter um inteiro com um pedaço dos bytes num bloco e o resto em outro.
    */
    private void writeAddressFile() {
        Random rand = new Random(); //gerador de números aleatórios

        //total de inteiros no data.bin
        int totalWords = this.n * (dataCache_bsize / 4);
        
        // Garante um número mínimo ou grande de acessos
        // int numAddresses = Math.max(10, totalWords * 2); //Math.max escolhe o maior entre eles
        int numAddresses = Math.max(1000, totalWords);

        try (DataOutputStream fp = new DataOutputStream(new FileOutputStream("address.bin"))) {
            
            for (int i = 0; i < numAddresses; i++) {
                //gera um inteiro aleatório entre 0 e o máximo de inteiros do arquivo
                int wordIndex = (rand.nextInt(totalWords));  //ou seja, escolhe algum dos inteiros do arquivo. Como um índice
                
                // EX: wordIndex = 1, representa a palavra no byte 4 do arquivo.
                int wordAddress = wordIndex * 4;             //converte pra formato bináriio

                // Em tese, é só colocar um byte 0 ou 1 aqui pra ver se é instrução ou dado.
                //vou escrever 0(dado) se for par e 1(instr) se for impar, simples por enquanto
                fp.writeInt(wordAddress);                    //escreve o número no arquivo
                fp.writeInt(i%2);  
            }
        } catch (IOException e) {
            System.err.println("X - Falha ao escrever arquivo de endereços! " + e.getMessage());;
        }

        System.out.println("Arquivo de endereços criado com sucesso!");
        System.out.println();
        this.addressFile = "address.bin";
    }
    //     System.out.println(" [Lido] Endereço: " + Integer.toUnsignedString(address) + " | Tipo: " + typeStr + " | Dado: " + data);
    // }
}
