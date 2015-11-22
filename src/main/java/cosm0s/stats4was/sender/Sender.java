package cosm0s.stats4was.sender;

import cosm0s.stats4was.domain.Statistic;

public interface Sender {

    public boolean isConnected();

    public void send(Statistic statistic);

}
