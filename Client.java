
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client{

    JFrame f = new JFrame("Client");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JButton describeButton = new JButton("Describe");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp; //UDP packet received from the server
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    static int RTP_RCV_PORT; // = 25000; //port where the client will receive the RTP packets

    Timer timer; //timer used to receive data from the UDP socket
    byte[] buf; //buffer used to store data received from the server

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    static int state; //RTSP state == INIT or READY or PLAYING
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file to request to the server
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
    int RTSPid = 0; //ID of the RTSP session (given by the RTSP Server)

    final static String CRLF = "\n";

    //Video constants:
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

    public Client() {

        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(describeButton);
        buttonPanel.add(tearButton);
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());
        describeButton.addActionListener(new describeButtonListener());

       


        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(110,50,380,280);
        buttonPanel.setBounds(10,400,580,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(600,600));
    
        f.setVisible(true);


        //init timer
        //--------------------------
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[15000];
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String[] argv) throws Exception
    {
        //Create a Client object
        Client theClient = new Client();

        //command: python ClientLauncher.py server_host server_port RTP_port video_file

        //get server RTSP port and IP address from the command line
        String ServerHost = argv[0];
        InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);
        int RTSP_server_port = Integer.parseInt(argv[1]);
        RTP_RCV_PORT = Integer.parseInt(argv[2]);
        //get video filename to request:
        VideoFileName = argv[3];

        //Establish a TCP connection with the server to exchange RTSP messages
        //------------------
        theClient.RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

        //Open input and output stream at client
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()) );
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()) );

        //init RTSP state:
        state = INIT;
    }


    //------------------------------------
    //Handler for buttons
    //Need to complete
    //------------------------------------

    class setupButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if (state == INIT)
            {
                try{
                    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                    RTPsocket.setSoTimeout(5*100);
                }
                catch (SocketException se)
                {
                    System.out.println("Socket exception: "+se);
                    System.exit(0);
                }
                RTSPSeqNb = 1;
                send_RTSP_request("SETUP");
                if (parse_server_response(0) != 200)
                    System.out.println("Invalid Server Response");
                else
                {
                    state = READY;
                    System.out.println("New RTSP state: READY");
                }
            }
        }
    }

    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            if (state == READY)
            {
                RTSPSeqNb++;
                send_RTSP_request("PLAY");
//                System.out.println("break_playfunction");
                if (parse_server_response(0) != 200)
                    System.out.println("Invalid Server Response");
                else
                {
                    state = PLAYING;
                    System.out.println("New RTSP state: PLAYING");
                    timer.start();
                }
            }
        }
    }

    class pauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            if (state == PLAYING)
            {
                RTSPSeqNb++;
                send_RTSP_request("PAUSE");
                if (parse_server_response(0) != 200)
                    System.out.println("Invalid Server Response");
                else
                {
                    state = READY;
                    System.out.println("New RTSP state: READY");
                    timer.stop();
                }
            }
        }
    }

    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            RTSPSeqNb++;
            send_RTSP_request("TEARDOWN");

            if (parse_server_response(0) != 200)
                System.out.println("Invalid Server Response");
            else
            {
                state = INIT;
                System.out.println("New RTSP state: INIT");
                timer.stop();
                System.exit(0);
            }
        }
    }
    
    class describeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            RTSPSeqNb++;
            send_RTSP_request("DESCRIBE");

            if (parse_server_response(1) != 200)
                System.out.println("Invalid Server Response");

            if (state == PLAYING){
                RTSPSeqNb++;
                send_RTSP_request("PAUSE");
                if (parse_server_response(0) != 200)
                    System.out.println("Invalid Server Response");
                else
                {
                    state = READY;
                    System.out.println("New RTSP state: READY");
                    timer.stop();
                }
            }
            timer.stop();
        }
    }


    //------------------------------------
    //Handler for timer
    //------------------------------------

    class timerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try{
                //receive the DP from the socket:
                RTPsocket.receive(rcvdp);

                //create an RtpPacket object from the DP
                RtpPacket rtp_packet = new RtpPacket(rcvdp.getData(), rcvdp.getLength());

                //print important header fields of the RTP packet received:
                // System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
                System.out.println("Current Seq Num: " + rtp_packet.getsequencenumber());
                //print header bitstream:
                // rtp_packet.printheader();

                //get the payload bitstream from the RtpPacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){
                //System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }

    //------------------------------------
    //Parse Server Response
    //------------------------------------
    private int parse_server_response(int i)
    {
        int reply_code = 0;
        try{
        //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            //System.out.println("RTSP Client - Received from Server:");
            System.out.println(StatusLine);
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());
            if (reply_code == 200)
            {
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);
                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);
                tokens = new StringTokenizer(SessionLine);
                tokens.nextToken(); //skip over the Session:
                RTSPid = Integer.parseInt(tokens.nextToken());
                if (i != 0) {
                    String line = null;
                    while (RTSPBufferedReader.ready()){
                        System.out.println(RTSPBufferedReader.readLine());
                    }                    
                }
            }
            else{
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);
                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);
                JOptionPane.showMessageDialog(f, "File not found");
                System.exit(0);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
            

        return reply_code;
    }

    //------------------------------------
    //Send RTSP Request to Server through connection
    //Need to implement (complete)
    //------------------------------------

    private void send_RTSP_request(String request_type)
    {
        try{
            String requestLine = request_type + " " + VideoFileName  + " RTSP/1.0" + CRLF;
            RTSPBufferedWriter.write(requestLine);

            String cseq = "CSeq: " + RTSPSeqNb + CRLF;
            RTSPBufferedWriter.write(cseq);

            if (request_type.equals("SETUP")) {
                String transport = "Transport: RTP/UDP; client_port= " + RTP_RCV_PORT;
                RTSPBufferedWriter.write(transport);
            }
            else {
                String session = "Session: " + RTSPid;
                RTSPBufferedWriter.write(session);
            }

            RTSPBufferedWriter.flush();
        }
        catch(Exception ex)
        {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }

}
