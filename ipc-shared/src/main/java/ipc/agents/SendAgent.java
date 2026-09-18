package ipc.agents;

import io.aeron.Publication;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class SendAgent implements Agent {
    private final Publication publication;
    private final int sendCount;
    private final UnsafeBuffer unsafeBuffer;
    private int currentCountItem = 1;

    /**
     * Creates a sender that publishes incrementing counter values.
     *
     * @param publication the Aeron publication used to send messages
     * @param sendCount the total number of messages to attempt to send
     */
    public SendAgent(final Publication publication, int sendCount) {
        this.publication = publication;
        this.sendCount = sendCount;
        this.unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
        unsafeBuffer.putInt(0, currentCountItem);
    }

    /**
     * Attempts to publish the next counter value when the publication is connected.
     *
     * @return always returns {@code 0} because no work count is tracked
     */
    @Override
    public int doWork() {
        if (currentCountItem > sendCount) {
            return 0;
        }

        if (publication.isConnected()) {
            if (publication.offer(unsafeBuffer) > 0) {
                currentCountItem += 1;
                unsafeBuffer.putInt(0, currentCountItem);
            }
        }
        return 0;
    }

    /**
     * Returns the logical name of this agent.
     *
     * @return the sender role name
     */
    @Override
    public String roleName() {
        return "sender";
    }
}
