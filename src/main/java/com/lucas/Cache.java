package com.lucas;


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
        boolean flag;   // bit de validade. Diz se oq tá na tag vale de algo ou é lixo
        int tag;        // compara com um pedaço do endereço e determina hit/miss
        int data;       // usando um int pra representar um dado qualquer

        public CacheLine() {
            this.flag   = false;
            this.tag    = 0;
            this.data   = -1;   //vamos supor que só pode ter valores positivos pra dado. Um negativo indica que tá vazio.
        }
    }

    // Atributos de configuração
    private int nsets;  // conjuntos da cache. Vão ser as linhas da matriz
    private int assoc;  // número de espaços (blocos) num conjunto. São as colunas da matriz
    private int bsize;  // tamanho de cada bloco do conjunto
    
    // Atributos de controle
    private int bitsIndex;
    private int bitsOffset;
    private int bitsTag;    // tag é oq sobra, não necessariamente precisa da variável

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
        this.bitsTag    = 32 - bitsOffset - bitsIndex;

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
                cache[i][j] = new CacheLine();
            }
        }

    }

    // Construtor configurável
    public Cache(int nsets, int assoc, int bsize) {
        // Mapeamento direto, bsize = 4, 1024 conjuntos
        this.nsets = nsets;
        this.assoc = assoc;
        this.bsize = bsize;

        // Bits offset  = log2(bsize)
        this.bitsOffset = (int) (Math.log(bsize) / Math.log(2));

        // Bits indice  = log2(nsets)
        this.bitsIndex  = (int) (Math.log(nsets) / Math.log(2));

        // Bits tag     = 32 - offset - indice - 1 (bit de validade)
        this.bitsTag    = 32 - bitsOffset - bitsIndex;

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
                cache[i][j] = new CacheLine();
            }
        }
    }

    // Métodos

    /* SEARCH
    Recebe o endereço, trata os dados binário (separa em offset, index, etc)
    Vê se tá na cache, se tá, retorna o "dado", se não tá, insere.
    
    */
    public int search(int address, int data) {
        


        return 0;
    }

    public void insert() {

    }

    public void printLog() {
        
    }
}
