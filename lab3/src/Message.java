import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class Message {
    private byte flags;
    private UUID id;
    private String name;
    private byte[] data;

    private final static int HEADER_SIZE = Protocol.FLAGS_FIELD_SIZE + Protocol.ID_FIELD_SIZE;

    public Message(byte flags, UUID id, String name, byte[] data) {
        this.flags = flags;
        this.id = id;
        this.name = name;
        this.data = data;
    }

    public Message(byte flags, UUID id) {
        this.flags = flags;
        this.id = id;
    }

    private static byte[] uuidToBytes(UUID uuid){
        ByteBuffer bb = ByteBuffer.wrap(new byte[Protocol.ID_FIELD_SIZE]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID bytesToUuid(byte[] input){
        ByteBuffer bb = ByteBuffer.wrap(input);
        return new UUID(bb.getLong(), bb.getLong());
    }

    //message: <flags> <uuid> <name size> <name> <data>

    public byte[] getBytes(){
        byte[] output;
        if (isServiceMessage() && flags != Protocol.Flags.SET_DEPUTY){
            output = new byte[HEADER_SIZE];

            //fill header
            output[0] = flags;
            byte[] uuidBytes = uuidToBytes(id);
            System.arraycopy(uuidBytes, 0, output, Protocol.FLAGS_FIELD_SIZE, uuidBytes.length);
        }
        else {
            output = new byte[HEADER_SIZE + Protocol.NAME_LENGTH_FIELD_SIZE + name.length() + data.length];

            //fill header
            output[0] = flags;
            byte[] uuidBytes = uuidToBytes(id);
            System.arraycopy(uuidBytes, 0, output, Protocol.FLAGS_FIELD_SIZE, uuidBytes.length);

            //fill name
            System.arraycopy(name.getBytes(), 0, output, HEADER_SIZE + Protocol.NAME_LENGTH_FIELD_SIZE, name.length());
            output[HEADER_SIZE] = (byte)name.length();

            //fill data
            System.arraycopy(data, 0, output, HEADER_SIZE + Protocol.NAME_LENGTH_FIELD_SIZE + name.length(), data.length);
        }
        return output;
    }

    public static Message read(byte[] input, int length){
        byte flags = input[0];
        byte[] uuidBytes = Arrays.copyOfRange(input, Protocol.FLAGS_FIELD_SIZE, HEADER_SIZE);
        UUID id = bytesToUuid(uuidBytes);
        if(length == HEADER_SIZE){
            return new Message(flags, id);
        }
        else {
            int nameLength = input[HEADER_SIZE];
            byte[] name = new byte[nameLength];

            int dataSize = length - nameLength - HEADER_SIZE - Protocol.NAME_LENGTH_FIELD_SIZE;
            byte[] data = new byte[dataSize];

            System.arraycopy(input, HEADER_SIZE + Protocol.NAME_LENGTH_FIELD_SIZE, name, 0, nameLength);
            System.arraycopy(input, HEADER_SIZE + Protocol.NAME_LENGTH_FIELD_SIZE + nameLength, data, 0, dataSize);
            return new Message(flags, id, new String(name, 0, nameLength), data);
        }
    }

    public byte getFlags() {
        return flags;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isServiceMessage(){
        return flags != Protocol.Flags.NO_FLAGS;
    }
}
