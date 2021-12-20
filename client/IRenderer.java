package client;

public interface IRenderer<E> {
    public String render(E data);

    public String render(E[] data);
}
