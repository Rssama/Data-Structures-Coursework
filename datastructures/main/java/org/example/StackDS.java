package org.example;

import java.util.ArrayList;

public class StackDS implements DataStructure {
    private ArrayList<Integer> stack = new ArrayList<>();

    @Override
    public void insert(int value) {
        push(value);
    }

    @Override
    public void delete(int value) {
        pop();
    }

    @Override
    public boolean search(int value) {
        return stack.contains(value);
    }

    public void push(int value) {
        stack.add(value);
    }

    public int pop() {
        if (stack.isEmpty()) {
            throw new RuntimeException("栈为空");
        }
        return stack.remove(stack.size() - 1);
    }
}

