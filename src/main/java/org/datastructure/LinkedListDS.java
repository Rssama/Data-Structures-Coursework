package org.datastructure;

/**
 * 单链表实现
 */
public class LinkedListDS implements DataStructure {

    // ================== 节点类 ==================
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
            this.next = null;
        }
    }

    private Node head; // 链表头节点

    public LinkedListDS() {
        head = null;
    }

    // ================== DataStructure 接口实现 ==================

    @Override
    public void insert(int value) {
        // 插入到链表尾部
        Node newNode = new Node(value);
        if (head == null) {
            head = newNode;
        } else {
            Node current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
    }

    @Override
    public void delete(int value) {
        if (head == null) return;

        // 如果要删的是头节点
        if (head.value == value) {
            head = head.next;
            return;
        }

        // 遍历查找要删除的节点
        Node current = head;
        while (current.next != null) {
            if (current.next.value == value) {
                current.next = current.next.next;
                return;
            }
            current = current.next;
        }
    }

    @Override
    public boolean search(int value) {
        Node current = head;
        while (current != null) {
            if (current.value == value) return true;
            current = current.next;
        }
        return false;
    }

    // ================== 扩展方法 ==================

    /** 打印链表内容 */
    public void printList() {
        Node current = head;
        while (current != null) {
            System.out.print(current.value + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }

    /** 获取链表长度 */
    public int size() {
        int count = 0;
        Node current = head;
        while (current != null) {
            count++;
            current = current.next;
        }
        return count;
    }

    /** 获取头节点（方便可视化使用） */
    public Node getHead() {
        return head;
    }
}

