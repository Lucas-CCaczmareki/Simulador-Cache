package com.lucas;

import java.nio.ByteBuffer;

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
    public Cache(int nsets, int assoc, int bsize) {
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
            throw new IllegalArgumentException("Erro: nsets (" + ") DEVE ser uma potência de 2!");
        }
        if ((assoc & (assoc - 1)) != 0) {
            throw new IllegalArgumentException("Erro: assoc (" + ") DEVE ser uma potência de 2!");
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
        int offset, 
            index, 
            tag;

        accesses++;

        // Num caso de borda onde bitsOffset = 0 esse código pode dar problema
        // Separando os bits do offset
        offset = address << (32 - bitsOffset);  //move todos os bits de offset pro extremo esquerdo, "apagando os outros"
        
        // O >>> (unsigned shift) preenche com 0 sempre
        offset = offset >>> (32 - bitsOffset);  //move com o unsigned shift os bits do offset pra direita de novo.

        // Separando os bits do index (que são os n bitsIndex após os bits do offset);
        index = address >>> (bitsOffset);       // descarta os bits do offset (bits a direita do index)
        index = index << (32 - bitsIndex);      // descarta os outros bits (à esquerda, o tag)
        index = index >>> (32 - bitsIndex);     // retorna os bits do index pra direita

        // Separando os bits do tag
        // Como o tag é todo o resto que sobrou, só precisamos descartar os bits do offset e index
        tag = address >>> (bitsOffset + bitsIndex);     // descarta os bits do offset e index

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
            }
        // Na mapeada diretamente não temos capacity miss, todos entram na classe de conflict misses (pelo q eu entendi)

        // Se ela for totalmente associativa ou parcialmente associativa, acho que é a mesma lógica
        } else {
            //Aqui a gente vai no conjunto apontado por index
            // dentro do conjunto a gente confere todos os n blocos
                // se todos tiverem bit de válidade ok e não bater nenhuma tag: capacity miss, usa política de substituição    
                // se achar um bit de validade 0 (vazio) e não tiver batido uma tag antes: compulsory miss
                // se achar um bit de valid

        }



        for (int i = 0; i < assoc; i++) {
            if (!cache[index][i].flag) {    //se o bit de validade é 0
                compulsoryMiss++;
                return -1; //dado não encontrado
            
            // Se o que tá ali é um dado válido
            } else {
                // Compara a tag pra ver se é o dado que a gente busca
                if(cache[index][i].tag == tag) {
                    // se é o que a gente busca, manda bala e retorna o número
                    // vou ter que fazer a lógica pra montar o inteiro
                

                // ESSE ELSE TÁ ERRADO
                // Caso a tag seja diferente

                // preciso comparar todas as tags, e 
                } else {
                    conflictMiss++;
                    return -1; //dado não encontrado
                }
            }
        }

        //Se saiu do for e não achou o dado;
        // capacityMiss;
        
        

        return 0;
    }

    // colocar byte[] data nos parâmetros
    public void insert(byte[] block) {

    }

    public void printLog() {
        
    }


    public int getBsize() {
        return bsize;
    }

}
