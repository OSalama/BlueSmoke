package com.bluesmoke.farm.util;

import java.util.ArrayList;
import java.util.Iterator;

public class FixedSizeStackArrayList<T>{

    private int maxSize = 10000;
    private ArrayList<ListElement<T>> list = new ArrayList<ListElement<T>>();

    private ListElement<T> head;
    private ListElement<T> tail;

    public FixedSizeStackArrayList(int maxSize)
    {
        this.maxSize = maxSize;
    }

    public void addToStack(T data)
    {
        ListElement<T> element = null;
        if(list.isEmpty())
        {
            element = new ListElement<T>(data, null, null);
        }
        else {
            element = new ListElement<T>(data, head, null);
        }
        list.add(0, element);

        if(list.size() > maxSize)
        {
            list.remove(maxSize);
            element.delete();
        }

        head = element.getHead();
        tail = element.getTail();
    }

    public void clear()
    {
        list.clear();
    }

    public int size()
    {
        return list.size();
    }

    public T getTailData()
    {
        return tail.getData();
    }

    public T getHeadData()
    {
        return head.getData();
    }

    public ListElement<T> getTail()
    {
        return tail;
    }

    public ListElement<T> getHead()
    {
        return head;
    }
}
