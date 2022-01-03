package client;

import services.Wallet;

public class WalletRenderer implements IRenderer<Wallet> {
    @Override
    public String render(Wallet data) {
        return "Balance: " + data.getBalance() + "\n";
    }

    @Override
    public String render(Wallet[] data) {
        return null;
    }
}
