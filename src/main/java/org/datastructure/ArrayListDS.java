package org.datastructure;

import java.util.ArrayList;

/**
 * 顺序表（基于 ArrayList 实现）
 */
public class ArrayListDS implements DataStructure {
    private ArrayList<Integer> list;

    public ArrayListDS() {
        list = new ArrayList<>();
    }

    @Override
    public void insert(int value) {
        // 插入到末尾
        list.add(value);
    }

    @Override
    public void delete(int value) {
        // 删除第一个匹配的值
        list.remove((Integer) value);
    }

    @Override
    public boolean search(int value) {
        return list.contains(value);
    }

    // ================== 扩展方法 ==================

    /** 获取指定下标元素 */
    public int get(int index) {
        return list.get(index);
    }

    /** 在指定位置插入 */
    public void insertAt(int index, int value) {
        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException("下标越界");
        }
        list.add(index, value);
    }

    /** 获取当前大小 */
    public int size() {
        return list.size();
    }

    /** 清空顺序表 */
    public void clear() {
        list.clear();
    }

    /** 打印顺序表内容（调试用） */
    public void printList() {
        System.out.println(list);
    }
}
