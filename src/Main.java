
public static void main(String[] args){
    TCPLogServer tcp = new TCPLogServer();
//    Logging log = new Logging("log.txt");


    System.out.println("Starting UI");
    javax.swing.SwingUtilities.invokeLater(() -> {
        LogViewer gui = new LogViewer();
        gui.setLocationRelativeTo(null);
        gui.setVisible(true);
    });

    //log.addLog("Hej");
}

