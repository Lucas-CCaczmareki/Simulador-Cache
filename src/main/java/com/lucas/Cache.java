package com.lucas;

import java.nio.ByteBuffer;
import java.util.Random;

/*
ok, basicamente então, a cache recebe seus parâmetros de configuração no construtor (nsets, bsize e assoc). 
Na hora do acesso recebe o endereço, divide entre bits offset, index e oq sobrar pra tag

A cache em si vai ser uma matriz de bits de validade e tag, que é só oq eu preciso pra simular hit/miss.
Se eu precisar expandir pra colocar algum dado eu posso. Vou fazer tipo uma struct usando uma inner class
*/
public class Cache {    
    // Inner class
    private class CacheLine {
        // Atributos da inner class
        boolean flag;           // bit de validade. Diz se oq tá na tag vale de algo ou é lixo
        int tag;                // compara com um pedaço do endereço e determina hit/miss
        byte[] data;            // vou simular o bloco da cache como um array de bytes

        public CacheLine(int bsize) {
            this.flag   = false;
            this.tag    = 0;
            this.data   = new byte[bsize];   //aloca espaço pros dados
        }
    }

    // Atributos de configuração
    private int nsets;  // conjuntos da cache. Vão ser as linhas da matriz
    private int assoc;  // número de espaços (blocos) num conjunto. São as colunas da matriz
    private int bsize;  // tamanho de cada bloco do conjunto
    
    // Atributos de controle
    private int bitsIndex;
    private int bitsOffset;
    // private int bitsTag;    // tag é oq sobra, não necessariamente precisa da variável

    // Atributos de dados
    private String name;
    private int accesses;
    private int hits;
    private int totalMisses;

    private int compulsoryMiss;
    private int conflictMiss;
    private int capacityMiss;

    private int cacheSize;          // nsets * bsize * assoc

    // O espaço da cache em si
    private CacheLine[][] cache;    //Criamos a cache com validade e tag.

    // OBSERVAÇÕES
    // "Política de substitução deve ser sempre randômica"
    
    // Construtor padrão (configuração default)
    public Cache() {
        this.name = "L1";

        // Mapeamento direto, bsize = 4, 1024 conjuntos
        this.nsets = 1024;
        this.assoc = 1;
        this.bsize = 4;

        // Bits offset  = log2(bsize)
        this.bitsOffset = (int) (Math.log(bsize) / Math.log(2));

        // Bits indice  = log2(nsets)
        this.bitsIndex  = (int) (Math.log(nsets) / Math.log(2));

        // Bits tag     = 32 - offset - indice - 1 (bit de validade)
        // this.bitsTag    = 32 - bitsOffset - bitsIndex;

        // Zera  os atributos de dado
        this.accesses       = 0;
        this.hits           = 0;
        this.totalMisses    = 0;
        this.compulsoryMiss = 0;
        this.conflictMiss   = 0;
        this.capacityMiss   = 0;

        // Calcula o tamanho da cache
        this.cacheSize = nsets * bsize * assoc;

        // Cria a estrutura da cache em si
        // Lembrando, o índice seleciona o conjunto, então, cada conjunto tem n bloco.
        // Cada bloco tem uma tag e um bit de validade. Pra saber o certo, comparamos força bruta com todos.
        cache = new CacheLine[nsets][assoc];

        // Inicializa com tudo zerado
        for (int i = 0; i < nsets; i++) {
            for (int j = 0; j < assoc; j++) {
                cache[i][j] = new CacheLine(bsize);
            }
        }

    }

    // Construtor configurável
    public Cache(String name, int nsets, int bsize, int assoc) {
        this.name = name;
        
        // Mapeamento direto, bsize = 4, 1024 conjuntos
        this.nsets = nsets;
        this.assoc = assoc;
        this.bsize = bsize;

        // VALIDAÇÕES DE SEGURANÇA
        // Tamanho do bloco precisa ser pelo menos 4. 
        // (Assumindo que estamos trabalhando apenas com inteiros, ou seja 4 bytes a palavra)
        if (bsize < 4) {
            throw new IllegalArgumentException("Erro: bsize deve ser no mínimo 4 bytes!");
        }

        // Associatividade não pode ser < 1.
        if (assoc < 1) {
            throw new IllegalArgumentException("Erro: assoc deve ser pelo menos 1!");
        }

        // Verifica se bsize, nsets e assoc é potência de 2
        if ((bsize & (bsize - 1)) != 0) {
            throw new IllegalArgumentException("Erro: bsize (" + bsize + ") DEVE ser uma potência de 2!");
        }
        if ((nsets & (nsets - 1)) != 0) {
            throw new IllegalArgumentException("Erro: nsets (" + nsets + ") DEVE ser uma potência de 2!");
        }
        if ((assoc & (assoc - 1)) != 0) {
            throw new IllegalArgumentException("Erro: assoc (" + assoc + ") DEVE ser uma potência de 2!");
        }

        // Bits offset  = log2(bsize)
        this.bitsOffset = (int) (Math.log(bsize) / Math.log(2));

        // Bits indice  = log2(nsets)
        this.bitsIndex  = (int) (Math.log(nsets) / Math.log(2));

        // Bits tag     = 32 - offset - indice - 1 (bit de validade)
        // this.bitsTag    = 32 - bitsOffset - bitsIndex;

        // Zera  os atributos de dado
        this.accesses       = 0;
        this.hits           = 0;
        this.totalMisses    = 0;
        this.compulsoryMiss = 0;
        this.conflictMiss   = 0;
        this.capacityMiss   = 0;

        // Calcula o tamanho da cache
        this.cacheSize = nsets * bsize * assoc;

        // Cria a estrutura da cache em si
        // Lembrando, o índice seleciona o conjunto, então, cada conjunto tem n bloco.
        // Cada bloco tem uma tag e um bit de validade. Pra saber o certo, comparamos força bruta com todos.
        cache = new CacheLine[nsets][assoc];

        // Inicializa com tudo zerado
        for (int i = 0; i < nsets; i++) {
            for (int j = 0; j < assoc; j++) {
                cache[i][j] = new CacheLine(bsize);
            }
        }
    }

    // Métodos
    /* SEARCH
    Recebe o endereço, trata os dados binário (separa em offset, index, etc)
    Vê se tá na cache, se tá, retorna o "dado", se não tá, insere.
    
    */
    public int search(int address) {
        accesses++;

        //cria uma máscara de n(bitsOffset) bits se o offset for != 0
        int offsetMask = (bitsOffset == 0) ? 0 : ((1 << bitsOffset) - 1); 
        int offset = (bitsOffset == 0) ? 0 : (address & offsetMask);    //se o offset for != 0, aplica a mask e retira os bits

        // index (próximos bitsIndex bits)
        int index;
        if (bitsIndex == 0) {
            index = 0;
        } else {
            int indexMask = (1 << bitsIndex) - 1;           //cria a máscara de n(bitsIndex) bits
            index = (address >>> bitsOffset) & indexMask;   //descarta bits offset e aplica a máscara
        }

        // tag (restante dos bits à esquerda)
        int tag = address >>> (bitsOffset + bitsIndex);

        /* Lógica antiga (falhava em casos de borda (totalmente associativa))
        // // Num caso de borda onde bitsOffset = 0 esse código pode dar problema
        // // Separando os bits do offset
        // offset = address << (32 - bitsOffset);  //move todos os bits de offset pro extremo esquerdo, "apagando os outros"
        
        // // O >>> (unsigned shift) preenche com 0 sempre
        // offset = offset >>> (32 - bitsOffset);  //move com o unsigned shift os bits do offset pra direita de novo.

        // // Separando os bits do index (que são os n bitsIndex após os bits do offset);
        // index = address >>> (bitsOffset);       // descarta os bits do offset (bits a direita do index)
        // index = index << (32 - bitsIndex);      // descarta os outros bits (à esquerda, o tag)
        // index = index >>> (32 - bitsIndex);     // retorna os bits do index pra direita

        // // Separando os bits do tag
        // // Como o tag é todo o resto que sobrou, só precisamos descartar os bits do offset e index
        // tag = address >>> (bitsOffset + bitsIndex);     // descarta os bits do offset e index
        */

        // RELEMBRANDO
            // O index me diz em qual conjunto da cache eu devo ir
            // O tag confirma, junto do bit de validade, se o bloco tem dado/instrução que eu quero.
            // O offset me diz qual parte do bloco tem o dado/deve ser armazenado o dado.

        // se assoc = 1 tamo numa diretamente mapeada, então vou só comparar flag e tag
        if (assoc == 1) {
            if(!cache[index][0].flag) { // se o bit de validade é 0
                compulsoryMiss++;
                return -1;          // retorna um valor sentinela pra avisar que não encontrou o dado
            
            // Se o espaço é válido
            } else {
                if(cache[index][0].tag == tag) { // se a tag é igual, temos um hit
                    hits++;
                    
                    // Ambos arquivo e buffer operam de big endian. Então isso não deve corromper
                    // Aqui esse buffer vai ler os 4 bytes consecutivos começando no offset e montar meu inteiro
                    // é a mesma coisa que fazer na mão, mas assim é mais simples e confunde menos.
                    ByteBuffer bb = ByteBuffer.wrap(cache[index][0].data);
                    return bb.getInt(offset);
                
                // Caso onde a validade tá ok, mas o dado não é oq a gente procura (tag diferente)
                } else {
                    conflictMiss++;
                    return -1;      // arrumar/modificar o valor sentinela depois
                }
            } // Na mapeada diretamente não temos capacity miss, todos entram na classe de conflict misses (pelo q eu entendi)

        // Se ela for totalmente associativa ou parcialmente associativa, acho que é a mesma lógica
        } else {
            // O que eu quero? 1. Achar o número
            // 1a coisa. O bloco com o dado q eu quero tá lá? Isso é tudo que eu quero saber aqui
            
            for (int i = 0; i < assoc; i++) {   //olha todos os blocos
                if(cache[index][i].flag) { //se o bloco for válido
                    // Compara pra ver se a tag bate
                    if(cache[index][i].tag == tag) {
                        hits++;
                        // Se bateu, monta o inteiro e retorna
                        ByteBuffer bb = ByteBuffer.wrap(cache[index][i].data);
                        return bb.getInt(offset);
                    }
                } else {
                    compulsoryMiss++;
                    return -1;  //se encontrou um bloco vazio, logicamente todos depois dele tão vazio, já da um break e retorna -1
                }
            }
            // Se percorreu todos blocos do conjunto e a tag não bateu, então não temos esse dado lá
            conflictMiss++;
            return -1;
        }
    }

    /* INSERT
    
    */
    public void insert(int address, byte[] block) {
        // int offsetMask = (bitsOffset == 0) ? 0 : ((1 << bitsOffset) - 1); //cria a máscara pra ficar só com os bits do offset
        // int offset = (bitsOffset == 0) ? 0 : (address & offsetMask);      //se o offset for válido, aplica a máscara.

        // index (próximos bitsIndex bits)
        int index;
        if (bitsIndex == 0) {
            index = 0;
        } else {
            int indexMask = (1 << bitsIndex) - 1;         //cria uma mascara com n (bitsIndex) bits
            index = (address >>> bitsOffset) & indexMask; //descarta os bits do offset e aplica a máscara.
        }

        int tag = address >>> (bitsOffset + bitsIndex);   //descarta os bits do offset e do index

        // Se for mapeada diretamente, cada conjunto é 1 bloco só.
        if (assoc == 1) {
            // vai lá e substitui oq ser que esteja lá
            cache[index][0].flag = true;
            cache[index][0].tag = tag;
            cache[index][0].data = block;
            return;
        
        // Se tiver associatividade
        } else {
            // Procura o primeiro bloco livre naquele conjunto
            for (int i = 0; i < assoc; i++) {
                if(!cache[index][i].flag) { //bloco livre
                    // Carrega o bloco pra lá
                    cache[index][i].flag = true;
                    cache[index][i].tag = tag;
                    cache[index][i].data = block;
                    return;
                }
            }
            // SE não tiver nenhum, escolhe 1 aleatoriamente e substitui
            Random rand = new Random();
            int target = rand.nextInt(assoc); // escolhe um número aleatório entre 0 e assoc (que é = número de blocos)

            //Carrega o bloco pra lá
            cache[index][target].flag = true;
            cache[index][target].tag = tag;
            cache[index][target].data = block;
            return;
        }
    }

    public void printLog() {
        totalMisses = compulsoryMiss + conflictMiss + capacityMiss;
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("============= RELATÓRIO CACHE " + this.name + " =============");
        System.out.println();
        System.out.println("------------ Configuração da cache ------------");
        
        System.out.println("Words de 4 bytes (inteiros)");
        System.out.println("Endereço de 32 bits");

        if (assoc == 1) {
            System.out.println("Mapeamento direto");
        } else {
            if(nsets == 1) {
                System.out.println("Totalmente associativa");
            } else {
                System.out.println("Associativa por conjunto " + assoc + "-way");
            }
        }

        System.out.println();
        System.out.println("Tamanho da cache: \t" + cacheSize + " bytes");
        System.out.println("Conjuntos:\t\t" + this.nsets);
        System.out.println("Tamanho do bloco:\t" + this.bsize);
        
        System.out.println();
        System.out.println("===============================================");
        System.out.println("Acessos:\t\t" + this.accesses);
        System.out.println("Misses totais:\t\t" + this.totalMisses);
        System.out.println("Hits totais:\t\t" + this.hits);;
        System.out.println("===============================================");
        System.out.println("Misses compulsórios:\t" + this.compulsoryMiss);
        System.out.println("Misses de conflito:\t" + this.conflictMiss);
        System.out.println("Misses de capacidade:\t" + this.capacityMiss);
        System.out.println("===============================================");
        
        double hit_ratio, miss_ratio;
        hit_ratio = (double) hits / accesses;
        miss_ratio = (double) totalMisses / accesses;

        System.out.println("Taxa de Hit:\t" + hit_ratio);
        System.out.println("Taxa de Miss:\t" + miss_ratio);
        System.out.println("===============================================");
    }

    //Getters e setters
    public int getBsize() {
        return bsize;
    }

}
