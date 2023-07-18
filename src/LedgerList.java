import java.util.ArrayList;
import java.io.Serializable;

/**
 *  The {@code LedgerList} class represents a wrapper for an ArrayList object that is
 *  used for storing blocks in the chain. The intuition behind this class is to limit
 *  access to the underlying ArrayList object through a tailored API.
 *
 *  It supports the following methods: size, getFirst, getLast, add, and findByIndex.
 */
public class LedgerList<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<T> list;

    // Constructor
    public LedgerList() {
        list = new ArrayList<T>();
    }

    // Return ledger size.
    public int size() {
        return this.list.size();
    }

    // Retrieve the first block in the ledger.
    public T getFirst() {
        return this.list.get(0);
    }

    // Retrieve the last block in the ledger.
    public T getLast() {
        return this.list.get(this.size() - 1);
    }

    // Add a block at the end of the ledger.
    public boolean add(T e) {
        return this.list.add(e);
    }

    // Retrieve block at specific index.
    public T findByIndex(int index) {
        return this.list.get(index);
    }
}