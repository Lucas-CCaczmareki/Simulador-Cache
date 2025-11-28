package com.lucas;

import java.io.*;

/**
 * Hello world!
 *
 */
public class App 
{
    // Atributos
    // static faz com que essa variável seja comum a todas instâncias (ou seja, 1 só)
    // file name é simbólico. Isso na real é o nome do endereço do arquivo no meu pc num objeto java
        // Talvez seja necessário incluir os diretórios.
    private static String FILE_NAME = "teste.bin";


    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        writeBinFile();
        readBinFile();
    }

    /*
     * Escreve um arquivo binário pros nossos testes com a cache
    */
    private static File writeBinFile() {
        System.out.println(": Escrevendo arquivo binário...");

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
            //Padrão dados: [Tipo byte 1 byte][Endereço int 4 bytes][Dado int 4 bytes]
                // 0 - Instrução
                // 1 - Leitura de dado
                // 2 - Escrita de dado
        
            // Posteriormente desenvolver isso aqui melhor, pra criar um arquivo
            // com endereços aleatórios. Acessos aleatórios, etc
        
            // Acesso 1: Instrução no endereço 100
            fp.writeByte(0);
            fp.writeInt(100);   // 4 bytes = 32 bits. Endereço de 32 bits
            fp.writeInt(1);     // escrevendo uns dados aleatórios

            // Acesso 2: Leitura de dado no endereço 2048
            fp.writeByte(1);
            fp.writeInt(2048);
            fp.writeInt(2);

            // Acesso 3: Escrita de dado no endereço 4096
            fp.writeByte(2);
            fp.writeInt(4096);
            fp.writeInt(3);
            

            System.out.println("| OK! | Arquivo de teste criado com sucesso!");

        // Se não conseguioiu abrir o arquivo
        } catch (IOException e) {
            System.err.println("X - Falha ao escrever no arquivo! " + e.getMessage());
        }
        System.out.println();
        return file;
   }

   /*
    * Lê os dados que foram salvos estruturadamente dentro do arquivo binário
    */
    private static void readBinFile() {
        System.out.println(": Lendo arquivo binário...");

        //Usamos o tipo file aqui a fim de fazer uma verificação de segurança
        //garantido que o arquivo existe, e se não existir, criando ele.
        File file = new File(FILE_NAME);    //busca o endereço do arquivo

        if(!file.exists()) {
            System.err.println("Arquivo não encontrado! Erro ao abrir");
            System.out.println("Criando arquivo...");
            file = writeBinFile();
        }

        // Eu provavelmente deveria usar a convenção
        // dos = data output stream
        // dis = data input stream
        // mas vou usar o fp por enquanto que é mais próximo do C, que to acostumado

        try (DataInputStream fp = new DataInputStream(new FileInputStream(FILE_NAME))) {
            // o .available retorna um número estimado de bytes para leitura
            while(fp.available() > 0) {
                // Lemos de volta na mesma ordem que inserimos
                
                // Lê o tipo do endereço (instrução ou leitura/escrita
                byte type = fp.readByte();

                // Lê os 32 bits (4 bytes) do endereço
                int address = fp.readInt();

                // Lê o dado (4 bytes de um int) do endereço
                int data = fp.readInt();

                

                processAccess(address, type, data); //temporario só pra printar

            }

        } catch (EOFException e) {
            System.out.println("Fim do arquivo. Terminando leitura...");
        } catch (IOException e) {
            System.err.println("Falha ao ler arquivo: " + e.getMessage());
        }
    }

    // Método auxiliar só pra printar aqui
    private static void processAccess(int address, byte type, int data) {
        String typeStr;
        
        switch(type) {
            case 0:
                typeStr = "INSTRUÇÃO (i-Cache)";
                break;
            case 1:
                typeStr = "LEITURA DADO (d-Cache)";
                break;
            case 2:
                typeStr = "ESCRITA DE DADO (d-Cache)";
                break;
            default:
                typeStr = "DESCONHECIDO";
                break;
        }

        // Segundo o gemini o Java n tem unsigned int. Então usamos uma função auxiliar pra converter pra unsigned.
        System.out.println(" [Lido] Endereço: " + Integer.toUnsignedString(address) + " | Tipo: " + typeStr + " | Dado: " + data);
    }

}
