
package newpackage;

/**
 *
 * @author Carlos Frias
 */
public class MainTeste{
    
    public static void main(String args[]) throws InterruptedException{
        PeerThread p1_control= new PeerThread(new Peer(), 1);
        p1_control.start();
        PeerThread p1_dados= new PeerThread(new Peer(), 3);
        p1_dados.start();
        Peer p2 = new Peer();
        p2.enviaProcura("pdf");
        Thread.sleep(1000);
        System.out.println(p2.foudsEncontrados);
        if(!p2.foudsEncontrados.isEmpty()) p2.enviaGET(0);
        
    }
}
