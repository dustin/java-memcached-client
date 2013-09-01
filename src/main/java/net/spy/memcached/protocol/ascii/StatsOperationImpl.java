import net.spy.memcached.protocol.ascii.OperationImpl;

/**
 * Operation to retrieve statistics from a memcached server.
 */
final class StatsOperationImpl extends OperationImpl implements StatsOperation {

  private static final OperationStatus END = new OperationStatus(true, "END");
  private static final OperationStatus RESET = new OperationStatus(true, "RESET");

  private static final byte[] MSG = "stats\r\n".getBytes();

  private final byte[] msg;
  private final StatsOperation.Callback cb;

  public StatsOperationImpl(String arg, StatsOperation.Callback c) {
    super(c);
    cb = c;
    if (arg == null) {
      msg = MSG;
    } else {
      msg = ("stats " + arg + "\r\n").getBytes();
    }
  }

  @Override
  public void handleLine(String line) {
    if (line.equals("END")) {
      cb.receivedStatus(END);
      transitionState(OperationState.COMPLETE);
    } else if (line.equals("RESET")) {
      // The server responds to "stats reset" with "RESET"
      cb.receivedStatus(RESET);
      transitionState(OperationState.COMPLETE);
    } else {
      String[] parts = line.split(" ", 3);
      assert parts.length == 3;
      cb.gotStat(parts[1], parts[2]);
    }
  }

  @Override
  public void initialize() {
    setBuffer(ByteBuffer.wrap(msg));
  }

  @Override
  protected void wasCancelled() {
    cb.receivedStatus(CANCELLED);
  }

  @Override
  public String toString() {
    return "Cmd: " + Arrays.toString(msg);
  }
}
