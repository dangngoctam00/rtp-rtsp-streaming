
public class RtpPacket {

    // Size of the RTP header:
    static int HEADER_SIZE = 12;

    // Fields that compose the RTP header
    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public int Ssrc;

    // Bitstream of the RTP header
    public byte[] header;

    // Size of the RTP payload
    public int payload_size;

    // Bitstream of the RTP payload
    public byte[] payload;

    // --------------------------
    // Constructor of an RTPpacket object from header fields and payload bitstream
    // --------------------------

    // --------------------------
    // Constructor of an RTPpacket object from the packet bistream
    // --------------------------
    public RtpPacket(byte[] packet, int packet_size) {

        // Fill default fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        // Check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE) {

            // Get the header bitsream:
            header = new byte[HEADER_SIZE];

            for (int i=0; i < HEADER_SIZE; i++) {
                header[i] = packet[i];
            }

            // Get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];

            for (int i=HEADER_SIZE; i < packet_size; i++) {
                payload[i-HEADER_SIZE] = packet[i];
            }

            // Interpret the changing fields of the header:
            PayloadType = header[1] & 127;
            SequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
            TimeStamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
        }
    }

    // --------------------------
    // getpayload: return the payload bistream of the RTPpacket and its size
    // --------------------------
    public int getpayload(byte[] data) {

        for (int i=0; i < payload_size; i++) {
            data[i] = payload[i];
        }

        return(payload_size);
    }

    // --------------------------
    // getpayload_length: return the length of the payload
    // --------------------------
    public int getpayload_length() {

        return(payload_size);
    }

    // --------------------------
    // getlength: return the total length of the RTP packet
    // --------------------------
    public int getlength() {

        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    // Get packet: returns the packet bitstream and its length
    //--------------------------
    public int getpacket(byte[] packet) {

        // Construct the packet = header + payload
        for (int i=0; i < HEADER_SIZE; i++) {
            packet[i] = header[i];
        }

        for (int i=0; i < payload_size; i++) {
            packet[i+HEADER_SIZE] = payload[i];
        }

        // Return total size of the packet
        return(payload_size + HEADER_SIZE);
    }

    // --------------------------
    // gettimestamp: returns timestamp
    // --------------------------
    public int gettimestamp() {
        return(TimeStamp);
    }

    // --------------------------
    // getsequencenumber: returns sequence number
    // --------------------------
    public int getsequencenumber() {
        return(SequenceNumber);
    }

    // --------------------------
    // getpayloadtype: returns payload type
    // --------------------------
    public int getpayloadtype() {
        return(PayloadType);
    }

    // --------------------------
    // printheader: Prints headers without the SSRC
    // --------------------------
    public void printheader() {

        for (int i=0; i < (HEADER_SIZE-4); i++) {
            for (int j = 7; j>=0 ; j--) {
                if (((1<<j) & header[i] ) != 0) {
                    System.out.print("1");
                }
                else {
                    System.out.print("0");
                }
                System.out.print(" ");
            }
        }
        System.out.println();
    }

    // Return the unsigned value of 8-bit integer nb
    static int unsigned_int(int nb) {

        if (nb >= 0) {
            return(nb);
        }
        else {
            return(256+nb);
        }
    }
}