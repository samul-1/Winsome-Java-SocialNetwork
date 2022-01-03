package client;

import services.Wallet;
import services.Wallet.WalletTransaction;

public class WalletRenderer implements IRenderer<Wallet> {
    @Override
    public String render(Wallet data) {
        String ret = "Balance: " + data.getBalance() + " wincoin\nTransactions:\n";
        for (WalletTransaction transaction : data.getTransactions()) {
            ret += "[" + transaction.timestamp.toString() + "] " + (transaction.delta >= 0 ? "+"
                    : "") + transaction.delta + "\n";
        }
        if (data.getTransactions().size() == 0) {
            ret += "There are no transactions.";
        }
        return ret;
    }

    @Override
    public String render(Wallet[] data) {
        return null;
    }
}
