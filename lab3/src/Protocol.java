public class Protocol {
    public static final int FLAGS_FIELD_SIZE = 1;
    public static final int ID_FIELD_SIZE = 16;
    public static final int NAME_LENGTH_FIELD_SIZE = 1;

    public static class Flags{
        public static final byte NO_FLAGS = 0;
        public static final byte ACK = 1;
        public static final byte SYN = 1 << 1;
    }
}
