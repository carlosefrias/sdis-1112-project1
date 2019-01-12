
package newpackage;

/**
 *
 * @author Carlos Frias
 */
public class PeerThread extends Thread{
    public Peer peer;
    public int tipo;
    /**
     * tipo = 1 - receber mensagens
     * tipo = 2 - enviar mensagens
     * tipo = 3 - receber dados
     * tipo = 4 - enviar dados
     * tipo = 5 - escutar dados e fazer a Action2
     */
    public PeerThread(Peer p, int tipo){
        this.peer = p;
        this.tipo = tipo;
    }
    @Override
    public void run() {
        switch (tipo) {
            case 1: peer.escutaMensagens();break;
            case 2: break;
            case 3: peer.escutaDados();break;
            case 4: break;
            case 5: peer.action2(); break;
            default: break;
        }
    }
    
}
