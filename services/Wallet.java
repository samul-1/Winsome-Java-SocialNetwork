package services;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class Wallet {
    public static class WalletTransaction {
        public double delta;
        public Date timestamp;

        @JsonCreator
        public WalletTransaction() {
        }

        public WalletTransaction(double delta) {
            this(delta, new Date());
        }

        public WalletTransaction(double delta, Date timestamp) {
            this.delta = delta;
            this.timestamp = timestamp;
        }
    }

    public Wallet() {
    }

    private final List<WalletTransaction> transactions = new LinkedList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public double getBalance() {
        return this.transactions
                .stream()
                .map(transaction -> transaction.delta)
                .reduce(0.0,
                        (acc, el) -> acc + el);
    }

    public void addTransaction(double delta) {
        this.transactions.add(new WalletTransaction(delta));
    }

    public List<WalletTransaction> getTransactions() {
        return this.transactions;
    }
}
