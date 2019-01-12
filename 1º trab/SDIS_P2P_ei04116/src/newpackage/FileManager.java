package newpackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class FileManager {
    public FileManager(){
    }
    /**
     * Construtor da classe FileManager
     * @param nome nome do ficheiro que se vai gerir
     */
    public FileManager(String nome){
        this.nomeficheiro = nome;
    }
    private String nomeficheiro;
    public static int splitFile(String nome) throws IOException{
        File f = new File(nome);
        System.out.println(f.getParentFile().getPath());
        String dir_name = f.getParentFile().getPath()  + "/partes";
        File dir = new File(dir_name);  
        dir.mkdir();
        return splitFile(nome, dir.getAbsolutePath()  + "/" + Peer.getSHA(nome),1024);
    }
    /**
     * Função que divide o ficheiro em partes com um número definido de Bytes
     * @param nomeOrigem ficheiro de origem
     * @param nomeDestino nome comum dos ficheiros de destino
     * @param size número máximo de Bytes para cada parte
     * @return número de partes
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static int splitFile(String nomeOrigem, String nomeDestino, int size)
            throws FileNotFoundException, IOException{
        FileInputStream fis = new FileInputStream(nomeOrigem);
        byte buffer[] = new byte[size];
        int count = 0;
        while(true) {
            int i = fis.read(buffer, 0, size);
            if (i == -1) break;
            String filename = nomeDestino + ".p" + count ;
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(buffer, 0, i);
            fos.flush();
            fos.close();
            ++count;
        }
        return count;
    }
    /**
     * Procedimento que efectua a junção das partes de um ficheiro
     * @param nomePartes nome comum de cada parte
     * @param nomeFinal nome a atribuir ao ficheiro completo
     * @param nPartes número de partes do ficheiro
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void joinFile(String nomePartes, String nomeFinal, int nPartes)
            throws FileNotFoundException, IOException{
        FileOutputStream fos = new FileOutputStream(nomeFinal);
        FileInputStream fis;
        int Max_size = 1024*1024;
        byte buffer[] = new byte[Max_size];
        for(int count = 0; count < nPartes; count++){
            String filename = nomePartes + ".p" + count;
            fis = new FileInputStream(filename);
            File file = new File(filename);
            long length = file.length();//nº de bytes da parte do ficheiro
            int i = fis.read(buffer, 0, (int)length);
            if (i == -1) break;
            fos.write(buffer, 0, i);
            fos.flush();
        }
        fos.close();
    }
    /**
     * Getter do nome do ficheiro
     * @return nome do ficheiro
     */
    public String getFileName(){
        return nomeficheiro;
    }
    /**
     * getter da extensão do ficheiro
     * @return extensão do ficheiro
     */
    public String getFileExtention(){
        return nomeficheiro.substring(nomeficheiro.indexOf("."));
    }
}