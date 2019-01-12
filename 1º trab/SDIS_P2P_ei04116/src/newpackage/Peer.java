package newpackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos Frias
 */
public class Peer{
    private static final int DefaultPortControl = 8967, DefaultPortData = 8966; 
    private static final String DefaltMulticastChannel = "224.0.2.10";
    private static String HashAlgorithm = "SHA-256";
    public static String dirTransferencias = "d://p2p/Transferencias";
    
    private static InetAddress enderecoMulticast;
    private static int portoControlo = DefaultPortControl, portoDados = DefaultPortData;
    private static String Mchannel = DefaltMulticastChannel;
    private static MulticastSocket MsocketControl, MsocketData;
    
    //variável global para guardar os id's das procuras enviadas
    public static Vector idsProcuras = new Vector();
    //variável global para guardar informação vinda nos FOUNDS recebidos 
    public static Vector foudsEncontrados = new Vector();//FoundInfo
    public static Vector downloadsAtivos = new Vector();//FoundInfo
    
    //variáveis contendo os cógidos hash (SHA) dos ficheiros trasnferíveis na pasta de partilha
    private Vector shaEmDisco = new Vector();//STRINGS
    /**
     * Construtor por defeito da classs Peer
     */
    public Peer(){
        this(portoControlo, portoDados, DefaltMulticastChannel);
    }
    /**
     * Construtor da classe Peer dados o porto de controlo e de dados
     * @param portControl porto de controlo
     * @param portData porto de dados
     */
    public Peer(int portControl, int portData){
        this(portControl, portData, DefaltMulticastChannel);
    }
    /**
     * Construtor da classe Peer dados o porto de controlo e de dados e o endereço IP de multicast
     * @param portControl porto de controlo
     * @param portData porto de dados
     * @param multicastChannel endereço IP de multicast
     */
    public Peer(int portControl, int portData, String multicastChannel){
        this.portoControlo = portControl;
        this.portoDados = portData;
        this.Mchannel = multicastChannel;
        try {
            enderecoMulticast = InetAddress.getByName(Mchannel);
            MsocketControl = new MulticastSocket(portoControlo);
            MsocketData = new MulticastSocket(portoDados);
            MsocketControl.joinGroup(enderecoMulticast);
            MsocketData.joinGroup(enderecoMulticast);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Método para lançar um request via multicast às máquinas do grupo para procurar um ficheira
     * @param KeywordList Palavras chave separadas por espaços
     */
    public void enviaProcura(String KeywordList){
        String msgProcura = "SEARCH id";
        
        Random generator = new Random();
        int search_id = generator.nextInt(999999999);
        msgProcura += Integer.toString(search_id) + " " + KeywordList + "\n";
        
        System.out.println("Request a enviar: " + msgProcura);
        
        DatagramPacket dp = new DatagramPacket(msgProcura.getBytes(), msgProcura.getBytes().length, enderecoMulticast, portoControlo);
        
        try {
            MsocketControl.send(dp);
            this.idsProcuras.add(search_id);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Procedimento que envia um pacote de FOUND de um ficheiro
     */
    private void enviaFound(String searchId, Vector v){
        String msgFound;
        for(int i = 0; i < v.size(); i++){
            msgFound =  "FOUND " + searchId 
                    + " " + getSHA(((File) v.elementAt(i)).getPath()) 
                    + " " + ((File) v.elementAt(i)).length() 
                    + " " + ((File) v.elementAt(i)).getName() + "\n";
            DatagramPacket dp = new DatagramPacket(msgFound.getBytes(), msgFound.getBytes().length, enderecoMulticast, portoControlo);

            try {
                MsocketControl.send(dp);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /**
     * Procedimento que envia um pacote de GET para um ficheiro
     */
    public void enviaGET(int indice){
        //Mensagem GET
        //GET
        //sha
        //chunks
        //TODO pensar no pormenor dos chunks (Quais pedir?? de que forma???)
        //Mandar transferir tudo, para já...
        int chunkMax = ((FoundInfo) foudsEncontrados.elementAt(indice)).getNumberOfChunks();
        String msgGET = "GET " 
                + ((FoundInfo)this.foudsEncontrados.elementAt(indice)).getSHA() + " " 
                + "0-" + chunkMax + "\n";
        DatagramPacket dp = new DatagramPacket(msgGET.getBytes(), msgGET.getBytes().length, enderecoMulticast, portoControlo);
       
        try {
            MsocketControl.send(dp);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Acrescentar o ficheiro aos downloadsAtivos
        this.downloadsAtivos.add(((FoundInfo) foudsEncontrados.elementAt(indice)));
    }
    /**
     * Função que devolve a percentagem de tranferência realizada do ficheiro
     * dado o seu índice na lista
     * @param indice
     * @return 
     */
    public double getPercentagem(int indice){
        return ((FoundInfo) foudsEncontrados.elementAt(indice)).percentagemTransferido();
    }
    /**
     * Procedimento que envia um pacote de dados
     * @param sha
     * @param chunk_number
     * @param dados 
     */
    public void enviaDados(String sha, int chunk_number, byte[] dados){
        byte[] send_data = new byte[1024 + 64];
        byte[] send_sha = this.get32ByteSHA(this.getFileNameFromSHA(sha));
        byte[] send_chunk_no = new byte[8];
        LittleEndian.putInt(send_chunk_no, chunk_number);
        byte[] send_reserved = new byte[24];
        int offset = 0;
        System.arraycopy(send_sha, 0, send_data, offset, send_sha.length);
        offset += send_sha.length;
        System.arraycopy(send_chunk_no, 0, send_data, offset, send_chunk_no.length);
        offset += send_chunk_no.length;
        System.arraycopy(send_reserved, 0, send_data, offset, send_reserved.length);
        offset += send_reserved.length;
        System.arraycopy(dados, 0, send_data, offset, dados.length);

        DatagramPacket send_packet = new DatagramPacket(send_data, send_data.length, this.enderecoMulticast, this.portoDados);
        try {
            this.MsocketData.send(send_packet);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Procedimento para que o peer fique à escuta de procuras
     * Strings recebidas pelo socket de controlo
     */
    public void escutaMensagens(){
        while(true){
            byte buffer[] = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            try {
                MsocketControl.receive(dp);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }

            String str = new String(dp.getData(), 0, dp.getLength());
            System.out.println("Mensagem recebida: " + str);
            processaMensagem(str);
        }
    }
    /**
     * Procedimento reponsável pelo tratamento das mensagens recebidas no porto de dados
     */
    public void escutaDados(){
        while(true){
            byte buffer[] = new byte[32 + 8 + 24 + 1024];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            try {
                this.MsocketData.receive(dp);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }

            String str = new String(dp.getData(), 0, dp.getLength());
            processaDadosRecebidos(dp.getData());
        }
    }
    /**
     * Procedimento para processar a mensagem recebida no porto de controlo
     * @param str mensagem recebida
     */
    private void processaMensagem(String str){
        StringTokenizer stoken = new StringTokenizer(str);
        Vector palavras = new Vector();
        while(stoken.hasMoreTokens()){
            palavras.add(stoken.nextToken());
        }
        if(palavras.elementAt(0).equals("SEARCH")){
            //Mensagem SEARCH
            //SEARCH
            //search_id
            //keywordList
            String searchId = (String) palavras.elementAt(1);
            for(int i = 2; i < palavras.size(); i++){
                if(existeFile((String) palavras.elementAt(i))){
                    enviaFound(searchId, getFile((String) palavras.elementAt(i)));
                }
            }
        }
        else if(palavras.elementAt(0).equals("FOUND")){
            //Se for FOUND devem ser agrupadas mensagens usando o valor SHA
            //e mostradas ao utilizador
            
            //Mensagem FOUND
            //FOUND         0
            //search_id     1
            //sha           2
            //size          3
            //filename      4
            System.out.println("FOUND recebido: " + str.toString());
            int search_id = -1;
            long size = -1;
            try{
                search_id = Integer.parseInt(((String)palavras.elementAt(1)).substring(2));
                size = Long.parseLong((String) palavras.elementAt(3));
            }catch(NumberFormatException e){
                System.err.println("Erro no formato do id de procura");
            }
            if(idsProcuras.contains(search_id) && search_id != -1 && size != -1){
                String filename = "";
                for(int i = 4; i < palavras.size(); i++) {
                    filename += (String) palavras.elementAt(i) + " ";
                }
                
                foudsEncontrados.add(new FoundInfo((String)palavras.elementAt(2),
                        filename, search_id, size));
            }
            
        }else if(((String)palavras.elementAt(0)).equals("GET")){
            //Recebe um GET
            String sha = (String) palavras.elementAt(1);
            //ver se existe esse ficheiro em disco
            //se existir splitar esse ficheiro (se ainda não tiver sido feito)
            updateSHAs();
            if(shaEmDisco.contains(sha)){
                FileManager fm = new FileManager();
                String filename = this.getFileNameFromSHA(sha);
                
                if(filename != null) 
                try {
                    fm.splitFile(filename);
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            String chunks_numbers_string = (String) palavras.elementAt(2);
            Vector RequestedChunks = parseChunkNumbers(chunks_numbers_string);
            System.out.println("Pedi estes chunks: " + RequestedChunks);
                                  
            //Action 2:
            PeerThread pAction2 = new PeerThread(this, 5);
            pAction2.start();
            
            //Action 1:
            action1(RequestedChunks, sha);
            
        }
    }
    /**
     * Função que averigua a existência de um ficheiro na pasta de partilha 
     * que contenha no seu nome a palavra chave
     * @param keyword palavra chave
     * @return true se o ficheiro existe, false no caso contrário
     */
    private boolean existeFile(String keyword){
        if(keyword.equals("")) return false;
        File diretorio = new File(dirTransferencias);
        String[] ficheiros = diretorio.list();
        for ( int i = 0; i < ficheiros.length; i++ ){
            if(ficheiros[i].contains(keyword)) return true;
        }
        return false;
    }
    /**
     * Função que retorna os ficheiros existentes na pasta de transferencias 
     * cujo nome inclui a palavra chave
     * @param keyword palavra chave
     * @return vector de ficheiro
     */
    private Vector getFile(String keyword){
        Vector v = new Vector();
        File diretorio = new File(dirTransferencias);
        File fList[] = diretorio.listFiles();
        
        for ( int i = 0; i < fList.length; i++ ){
            if(fList[i].getName().contains(keyword) && !fList[i].isDirectory()){
                v.add(fList[i]);
            }
        }
        return v;
    }
    /**
     * Função que devolve o path do ficheiro conhecido o seu SHA
     * @param sha
     * @return 
     */
    private String getFileNameFromSHA(String sha){
        File diretorio = new File(dirTransferencias);
        File fList[] = diretorio.listFiles();
        for(int i = 0; i < fList.length; i++){
            if(!fList[i].isDirectory())
                if(this.getSHA(fList[i].getPath()).equals(sha))
                    return fList[i].getPath();
        }
        return null;
    }
    /**
     * Função que devolve o valor do SHA de um ficheiro sabendo 
     * a sua localização no sistema de ficheiro
     * @param filepath caminho para o ficheiro
     * @return a String contento o valor do SHA do ficheiro
     */
    public static String getSHA(String filepath){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HashAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filepath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] dataBytes = new byte[1024];
        int nread = 0;
        try {
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] mdbytes = md.digest();
        //convert the byte to hex format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
    /**
     * Função que retorna o array de 32 Bytes do SHA
     * @param filepath
     * @return 
     */
    public static byte[] get32ByteSHA(String filepath){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HashAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filepath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] dataBytes = new byte[1024];
        int nread = 0;
        try {
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] mdbytes = md.digest();
        return mdbytes;
    }
    /**
     * Função para reconhecer quais são os chunks pretendidos quando se recebe um get
     * @param chunks_numbers_string expressões do tipo 8 ou 12,3,23,55 ou 13-45
     * @return array de inteiros com os chunks pedidos
     */
    private Vector parseChunkNumbers(String chunks_numbers_string) {
        Vector chunks = new Vector();
        if(chunks_numbers_string.contains("-")){
            int val1 = Integer.parseInt(chunks_numbers_string.substring(0, chunks_numbers_string.indexOf('-')));
            int val2 = Integer.parseInt(chunks_numbers_string.substring(chunks_numbers_string.indexOf('-') + 1));
            for(int i = val1; i < val2 + 1; i++){
                chunks.add(i);
            }
        }else if(chunks_numbers_string.contains(",")){
            String[] numeros = chunks_numbers_string.split(",");
            for(int i = 0; i < numeros.length; i++){
                chunks.add(Integer.parseInt(numeros[i]));
            }
        }else chunks.add(Integer.parseInt(chunks_numbers_string));
        return chunks;
    }
    /**
     * Procedimento que trata da acção 1, descrita na especificação, quando se recebe um GET
     * @param RequestedChunks
     * @param sha 
     */
    private void action1(Vector RequestedChunks, String sha) {
        //Action 1:
        //while RequestedChunks
        //pick at random a chunkNumber from RequestedChunks
        //get the chunk with number chunkNumber from LocalStoredChunk
        //sent chunk to data port
        //remove chunkNumber from RequestedChunks
        //Actualiza a lista de SHAs em disco
        while(!RequestedChunks.isEmpty()){
            //se o ficheiro pedido no get existe em disco, entao escolher aleatoriamente um dos chunks pedido e enviar
            if(shaEmDisco.contains(sha)){
                try {
                    Collections.shuffle(RequestedChunks);
                    int chunkPiked = (Integer) RequestedChunks.elementAt(0);
                    byte[] dados = new byte[1024];
                    //colocar os dados a enviar em dados...
                    String filename = this.getFileNameFromSHA(sha);
                    File f1 = new File(filename);
                    filename = f1.getParentFile().getPath() + "/partes/" + sha + ".p" + chunkPiked;
                    FileInputStream fis = new FileInputStream(filename);
                    File file = new File(filename);
                    long length = file.length();
                    fis.read(dados, 0, (int)length);
                    //a enviar dados...
                    enviaDados(sha, chunkPiked, dados);
                    //A decrementar o número de chunks a enviar
                    RequestedChunks.remove((Integer) chunkPiked);
                    
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
    }
    /**
     * Actualiza a inforação dos SHAs dos ficheiros existentes na pasta de partilha
     */
    private void updateSHAs(){
        File diretorio = new File(dirTransferencias);
        File fList[] = diretorio.listFiles();
        
        for ( int i = 0; i < fList.length; i++ ){
                if(!fList[i].isDirectory()) shaEmDisco.add(getSHA(fList[i].getPath()));
        }
    }
    /**
     * Procedimento responsável por tratar dos pacotes de dados recebidos
     * pelo porto de dados
     * @param str mensagem recebida
     */
    private void processaDadosRecebidos(byte[] data) {
        try {
            //parse ao pacote
            byte[] sha = new byte[32];
            byte[] chunk_number = new byte[8];
            byte[] reserved = new byte[24];
            byte[] chunkdados = new byte[1024];
            System.arraycopy(data, 0, sha, 0, 32);
            System.arraycopy(data, 32, chunk_number, 0, 8);
            System.arraycopy(data, 40, reserved, 0, 24);
            System.arraycopy(data, 64, chunkdados, 0, 1024);

            int chunk_number_int = LittleEndian.getInt(chunk_number);

            //guardar os dados recebidos em pequenos ficheiros separados...
            File dir = new File(this.dirTransferencias + "/partes recebidas");  
            dir.mkdir();
            //Criar dentro desta pasta novos ficheiros contendo as partes recebidas
            //quando todas as partes tiverem sido recebidas, juntar os ficheiros.


            //convert the byte to hex format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sha.length; i++) {
                sb.append(Integer.toString((sha[i] & 0xff) + 0x100, 16).substring(1));
            }

            FileOutputStream fos;
            fos = new FileOutputStream(dir.getPath() + "/" + sb.toString() + ".p" + chunk_number_int);
            fos.write(chunkdados, 0, chunkdados.length);
            fos.flush();
            fos.close();
            //nos downloadsAtivos colocar a flag a true (chunk_recebido)
            for(int i = 0; i < downloadsAtivos.size(); i++){
                if (((FoundInfo) downloadsAtivos.elementAt(i)).getSHA().equals(sb.toString())){
                    ((FoundInfo) downloadsAtivos.elementAt(i)).setRecebido(chunk_number_int);
                    System.out.println("Transferido a: " + ((FoundInfo) downloadsAtivos.elementAt(i)).percentagemTransferido() + "%");
                    //Se a tranferência está completa então há que juntar as partes do ficheiro
                    if(((FoundInfo) downloadsAtivos.elementAt(i)).isTransfered()){
                        int total_chunks = ((FoundInfo) downloadsAtivos.elementAt(i)).getNumberOfChunks();
                        File f = new File(getFileNameFromSHA(sb.toString()));
                        String nomeParte = f.getParentFile().getPath() + "/partes recebidas/" + sb.toString();
                        File dir1 = new File(this.dirTransferencias + "/Transferencias Completas");
                        dir1.mkdir();
                        String nomeDestino = dir1.getPath() + "/" + ((FoundInfo) downloadsAtivos.elementAt(i)).getName();
                        FileManager.joinFile(nomeParte , nomeDestino, total_chunks);
                        System.out.println("Transferência completa: " + ((FoundInfo) downloadsAtivos.elementAt(i)).getName());
                    }
                    break;
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Procedimento para implementar a ação 2 descrita na especificaçao do trabalho
     */
    public void action2() {
        //TODO implementar a acção 2
        //while true
        //receive a chunk from data port
        //if the chunk belongs to any (even partial) local stored file remove it from correspondent RequestedChunks
        //this.escutaDados();
    }
}
