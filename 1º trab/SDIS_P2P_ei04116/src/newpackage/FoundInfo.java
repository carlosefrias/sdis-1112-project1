package newpackage;

/**
 *
 * @author Carlos Frias
 */
public class FoundInfo {
    private String sha_value;
    private String file_name;
    private int search_id;
    private long size;
    private int number_of_chunks;
    private boolean[] chunk_recebidos;// para controlar que chunks já foram recebidos
    /**
     * Construtor da classe FoundInfo
     * @param sha
     * @param name
     * @param id
     * @param size 
     */
    public FoundInfo(String sha, String name, int id, long size){
        this.sha_value = sha;
        this.file_name = name;
        this.search_id = id;
        this.size = size;
        this.number_of_chunks = (int)((size-1)/1024);
        chunk_recebidos = new boolean[number_of_chunks + 1];
        for(int i = 0; i < number_of_chunks + 1; i++){
            chunk_recebidos[i] = false;
        }
    }
    @Override
    public String toString(){
        return "" + search_id + " " + sha_value + " " + file_name;
    }
    /**
     * Getter do nome do ficheiro e do seu tamanho
     * @return 
     */
    public String getFileShowInfo(){
        if(size < 1024) return file_name + "      Tamanho: " + size + " bytes";
        else if(size >= 1024 && size <= 1024*1024)return file_name + "      Tamanho: " +  (size / 1024.0) + " Kbytes";
        else return file_name + "      Tamanho: " +  (double)(size / (1024.0*1024.0)) + " Mbytes";
    }
    /**
     * Função que devolve o SHA em formato Hexagimal
     * @return 
     */
    public String getSHA(){
        return this.sha_value;
    }
    /**
     * Função que devolve o SHA em array de bytes
     * @return 
     */
    public byte[] get32BytesSHA(){
        return Peer.get32ByteSHA(file_name);
    }
    /**
     * Getter do nome do ficheiro
     * @return 
     */
    public String getName(){
        return this.file_name;
    }
    /**
     * Getter do tamanho do ficheiro
     * @return 
     */
    public long getSize(){
        return size;
    }
    /**
     * Getter do número de chunks
     * @return 
     */
    public int getNumberOfChunks(){
        return this.number_of_chunks;
    }
    /**
     * Função que averigua um ficheiro está completo (transferido)
     * Se tiver todas as flags a true
     * @return 
     */
    public boolean isTransfered(){
        for(int i = 0; i < chunk_recebidos.length; i++){
            if(!chunk_recebidos[i]) return false;
        }
        return true;
    }

    /**
     * 
     * @param chunk_number_int
     */
    public void setRecebido(int chunk_number_int) {
        this.chunk_recebidos[chunk_number_int] = true;
    }
    /**
     * 
     * @return
     */
    public double percentagemTransferido(){
        int count = 0;
        for(int i = 0; i < this.chunk_recebidos.length; i++){
            if(this.chunk_recebidos[i]) count++;
        }
        return 100 * count / ((double)chunk_recebidos.length);
    }
    
    
}
