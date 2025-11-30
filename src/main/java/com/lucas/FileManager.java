package com.lucas;

import java.io.*;
import java.util.Random;

public class FileManager {
    // Atributos
    // static faz com que essa variável seja comum a todas instâncias (ou seja, 1 só)
    // file name é simbólico. Isso na real é o nome do endereço do arquivo no meu pc num objeto java
        // Talvez seja necessário incluir os diretórios.
    private static String FILE_NAME;
    private int bsize;
    private int minData;
    private int n;          //número de blocos q vai gerar e endereços

    //Construtor padrão
    public FileManager() {
        FILE_NAME = "data.bin";
        this.bsize = 4;
        this.minData = bsize/4;
        this.n = 1000;          //altera quantos blocos de memória vão ser criados (no data.bin)
    }

    // //Construtor
    // public FileManager(String name, int bsize) {
    //     FILE_NAME = name;
    //     this.bsize = bsize;
    //     this.minData = bsize/4; // Como bsize é em bytes, ao dividir por 4 temos quantos inteiros cabem num bloco
    // }

    public void runSimulation(Cache cache) {
        this.bsize = cache.getBsize();
        this.minData = bsize / 4;

        // E aqui eu gerencio
        writeBinFile(); //a partir daqui eu tenho data.bin e address.bin

        // Ler um endereço e tenta fazer um acesso na cache.

        try (DataInputStream fp = new DataInputStream(new FileInputStream("address.bin"))) {
            
            // ele entra aqui
            // System.out.println("entro aqui?");



            // Lê todos os endereços no arquivo e faz as buscas na cache
            while (true) {
                try {
                    int address = fp.readInt();
                    int x = cache.search(address);
                    
                    // Cache retorna -1 em caso de miss
                    if (x != -1) {
                        System.out.println("Hit! '" + x + "' encontrado!");
                    } else {
                        System.out.println("Miss! Inserindo bloco na cache...");
                        
                        // Acessamos o arquivo de dados no modo aleatório pra poder acessar qualquer parte
                        try (RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r")) {
                            //como é um inteiro, vai truncar a parte decimal, arredondando pra baixo, como eu disse antes
                            int blockNumber = address / bsize; 
                            int blockAddress = blockNumber * bsize; //endereço do bloco no arquivo em byte

                            raf.seek(blockAddress); //move o ponteiro até o byte do bloco
                            
                            byte[] block = new byte[bsize]; // cria o bloco

                            //read fully lê byte a byte até encher o vetor
                            raf.readFully(block);   //pega todos os dados do arquivo e bota no bloco
                            cache.insert(address, block);

                        } catch (FileNotFoundException e) {
                            System.err.println("X -> Arquivo de endereços não encontrado: " + e.getMessage());
                        } catch (IOException e) {
                            System.err.println("X -> Erro de I/O ao abrir address.bin: " + e.getMessage());
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

        cache.printLog();

    }


    /*
    * Cria o arquivo de dados com pelo menos 1 bloco completo. Então se bsize = 4, 1 inteiro por bloco.
    * to considerando que só vai ter inteiros no campo de dado, ent o tamanho da palavra é sempre 4 bytes.
    * Como a cache já foi criada antes de criarmos o arquivo, bsize teoricamente é uma potência de 2 e >= 4.
    */
    private File writeBinFile() {
        System.out.println("Escrevendo arquivo binário...");

        // Procura o endereço do arquivo 
        File file = new File(FILE_NAME);

        // Só pra garantir
        if(!file.exists()) {
            System.err.println("X -> Erro ao criar arquivo binário!");
        }

        // Abre um fluxo de dados de um arquivo.
            // Devido ao FileOutputStream, se o arquivo não existir, ele é criado
            // (lembrando que dessa maneira ele é zerado toda vez que iniciamos o programa)
        try (DataOutputStream fp = new DataOutputStream(new FileOutputStream(file))) {
            // Random rand = new Random(); //cria um gerador de números aleatórios

            for(int i = 0; i < (this.n*minData); i++) {
                fp.writeInt(i); //escreve n blocos de inteiros sequenciais no arquivo (posso deixar aleatório dps)
            }
            System.out.println("Arquivo de teste criado com sucesso!");

        // Se não conseguioiu abrir o arquivo
        } catch (IOException e) {
            System.err.println("X - Falha ao escrever no arquivo! " + e.getMessage());
        }
        writeAddressFile();
        
        return file;
    }

    /*
    Gera um arquivo com endereços que vão de 0 até o máximo de inteiros do arquivo de memória

    // ATENÇÃO: // Como eu também to emulando a cache de uma maneira simples, apenas com inteiros
                // vou garantir que os endereços sejam sempre múltiplos de 4, por que assim o offset vai estar sempre alinhado
                // e eu não vou ter um inteiro com um pedaço dos bytes num bloco e o resto em outro.
            
                // Para que um número seja múltiplico de 4 digitalmente, seus dois últimos bits devem ser 0
                // Vou fazer no seguinte formato, o número gerado é sempre arredondado pro múltiplico de 4 mais abaixo dele.
                // Exemplo: 21, 22 e 23 são arredondados pra 20. Ambos vão representar o mesmo endereço.
    */
    private void writeAddressFile() {
        Random rand = new Random(); //gerador de números aleatórios

        //total de inteiros no data.bin
        int totalWords = this.n * (bsize / 4);
        
        // Garante um número mínimo ou grande de acessos
        int numAddresses = Math.max(10, totalWords * 2); //Math.max escolhe o maior entre eles

        try (DataOutputStream fp = new DataOutputStream(new FileOutputStream("address.bin"))) {
            for (int i = 0; i < numAddresses; i++) {
                //gera um inteiro aleatório entre 0 e o máximo de inteiros do arquivo
                int wordIndex = (rand.nextInt(totalWords));  //ou seja, escolhe algum dos inteiros do arquivo. Como um índice
                
                // EX: wordIndex = 1, representa a palavra no byte 4 do arquivo.
                int wordAddress = wordIndex * 4;             //converte pra formato bináriio
                fp.writeInt(wordAddress);                   //escreve o número no arquivo
            }
        } catch (IOException e) {
            System.err.println("X - Falha ao escrever arquivo de endereços! " + e.getMessage());;
        }

        System.out.println("Arquivo de endereços criado com sucesso!");
        System.out.println();
    }
    //     System.out.println(" [Lido] Endereço: " + Integer.toUnsignedString(address) + " | Tipo: " + typeStr + " | Dado: " + data);
    // }
}
