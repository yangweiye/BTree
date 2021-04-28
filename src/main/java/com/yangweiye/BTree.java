package com.yangweiye;

public class BTree<V> implements BalanceTree<V> {
    static int STAGE = 2;

    private Node<V> root;
    private final int stage;

    public BTree() {
        this(STAGE);
    }

    public BTree(Integer stage) {
        this.stage = stage;
        this.root = new Node<>(stage);
    }

    public void put(Comparable key, V value) {
        insert(root, key, value);
    }

    private void insert(Node<V> root, Comparable key, V value) {
        Entry<V> entry = new Entry<>(key, value);
        Node<V> position = insertPosition(root, key);
        position.addEntry(entry);
    }

    private Node<V> insertPosition(Node<V> node, Comparable key) {
        if (node.leaf)
            return node;
        Entry<V>[] entries = node.entries;
        int index = node.searchEntryIndex(key);

        if (entries[index].getKey().compareTo(key) < 0)
            return insertPosition(node.children[index + 1], key);
        else
            return insertPosition(node.children[index], key);
    }

    public V search(Comparable key) {
        SearchResult<V> result = search(root, key);
        if (!result.isExist) {
            return null;
        }

        return result.node.entries[result.index].getValue();
    }

    //因为搜索结果要返回node 和 index
    static class SearchResult<V> {
        boolean isExist;
        Node<V> node;
        int index;
    }

    private SearchResult<V> search(Node<V> node, Comparable key) {
        if (node.entriesSize <= 0) {
            SearchResult<V> searchResult = new SearchResult<>();
            searchResult.isExist = false;
            return searchResult;
        }
        Entry<V>[] entries = node.entries;
        int index = node.searchEntryIndex(key);
        if (node.leaf && entries[index].getKey().compareTo(key) != 0) {
            SearchResult<V> searchResult = new SearchResult<>();
            searchResult.isExist = false;
            return searchResult;
        }

        if (entries[index].getKey().compareTo(key) == 0) {
            SearchResult<V> searchResult = new SearchResult<>();
            searchResult.isExist = true;
            searchResult.node = node;
            searchResult.index = index;
            return searchResult;
        } else if (entries[index].getKey().compareTo(key) > 0) {
            return search(node.children[index], key);
        } else {
            return search(node.children[index + 1], key);
        }
    }

    public void delete(Comparable key) {
        delete(root, key);
    }

    private void delete(Node<V> node, Comparable key) {

        int index = node.searchEntryIndex(key);
        if (node.entries[index].getKey().compareTo(key) == 0) {
            //找到了要删除的 entry
            if (node.leaf) {
                node.delete(index);
            } else {
                Node<V> children = null;
                Entry<V> replacer = null;
                if (node.children[index].entriesSize >= this.stage) {
                    children = node.children[index];
                    replacer = getPreToEntry(children);
                } else if (node.children[index + 1].entriesSize >= this.stage) {
                    children = node.children[index + 1];
                    replacer = getNextToEntry(children);
                }

                if (null == children) {
                    children = mergeNode(node, index);
                    if (node.entriesSize == 0) {
                        this.root = children;
                    }

                    delete(children, key);
                    return;
                }

                delete(children, replacer.getKey());
                node.entries[index] = replacer;
            }
        } else {
            // 找不到
            Node<V> children;
            boolean isLeft;
            if (node.entries[index].getKey().compareTo(key) > 0) {
                children = node.children[index];
                isLeft = true;
            } else {
                children = node.children[index + 1];
                isLeft = false;
            }

            //子节点 如果 entries 少于 stage 为避免自下而上的合并 特殊处理一下
            if (children.entriesSize >= this.stage) {
                //子节点删除也不会产生合并 无需处理
            } else {
                // 只有 stage - 1 个key
                Node<V> other;
                if (isLeft) {
                    //左子树 other 是右子树
                    other = node.children[index + 1];
                } else {
                    //右子树 other 是左子树
                    other = node.children[index];
                }

                //兄弟是否有余粮
                if (other.entriesSize >= this.stage) {
                    //借一个
                    if (isLeft) {
                        //向右边借一个
                        children.insert(children.entriesSize, node.entries[index]);
                        if (!children.leaf) {
                            children.insertChildren(children.childrenSize, other.children[0]);
                            other.deleteChildren(0);
                        }

                        node.entries[index] = other.entries[0];
                        other.delete(0);
                    } else {
                        //向左边借一个
                        children.insert(0, node.entries[index]);
                        if (!children.leaf) {
                            children.insertChildren(0, other.children[other.childrenSize - 1]);
                            other.deleteChildren(other.childrenSize - 1);
                        }

                        node.entries[index] = other.entries[other.entriesSize - 1];
                        other.delete(other.entriesSize - 1);
                    }
                } else {
                    //合并节点

                    //将 左子节点 当前entry 右子树合并
                    children = mergeNode(node, index);
                    if (node.entriesSize == 0) {
                        //只有根节点会出现这种情况
                        this.root = children;
                    }
                }
            }

            //递归的删除
            delete(children, key);
        }
        return;
    }

    // 合并
    private Node<V> mergeNode(Node<V> node, int index) {
        int stage = node.stage;
        Entry<V> entry = node.entries[index];
        node.delete(index);
        Node<V> leftChildren = node.children[index];
        Node<V> rightChildren = node.children[index + 1];

        leftChildren.entries[stage - 1] = entry;
        leftChildren.entriesSize++;

        System.arraycopy(rightChildren.entries, 0, leftChildren.entries, stage, rightChildren.entriesSize);
        System.arraycopy(rightChildren.children, 0, leftChildren.children, leftChildren.childrenSize, rightChildren.childrenSize);
        //恢复 childrenIndex
        for (int i = leftChildren.childrenSize; i < leftChildren.childrenSize + rightChildren.childrenSize; i++) {
            leftChildren.children[i].childrenIndex = i;
        }
        leftChildren.entriesSize += rightChildren.entriesSize;
        leftChildren.childrenSize += rightChildren.childrenSize;
        node.deleteChildren(index + 1);

        return leftChildren;
    }

    private Entry<V> getPreToEntry(Node<V> node) {
        if (node.leaf)
            return node.entries[node.entriesSize - 1];

        return getPreToEntry(node.children[node.childrenSize - 1]);
    }

    private Entry<V> getNextToEntry(Node<V> node) {
        if (node.leaf)
            return node.entries[0];

        return getNextToEntry(node.children[0]);
    }


    private static class Entry<V> {
        private final Comparable key;
        private final V value;

        public Entry(Comparable key, V value) {
            this.key = key;
            this.value = value;
        }

        public Comparable getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

    private static class Node<V> {
        private final int stage;
        private final int maxEntry;
        private final int maxChildren;

        //节点内数据 有序
        private Entry<V>[] entries;
        private int entriesSize;

        //子节点
        private Node<V>[] children;
        private int childrenSize;

        //是否为叶子节点
        private boolean leaf;

        private Node<V> parentNode;
        private int childrenIndex;

        public Node(int stage) {
            this.stage = stage;
            this.maxEntry = stage * 2 - 1;
            this.maxChildren = stage * 2;
            // 这里 entries 容量增加了一个 方便分裂
            this.entries = new Entry[this.maxEntry + 1];
            this.entriesSize = 0;
            // +1 同样是方便分裂
            this.children = new Node[this.maxChildren + 1];
            this.childrenSize = 0;
            this.leaf = true;
            this.parentNode = null;
            this.childrenIndex = 0;
        }

        private void insert(int index, Entry<V> entry) {
            if (entriesSize - index >= 0) System.arraycopy(entries, index, entries, index + 1, entriesSize - index);

            entries[index] = entry;
            entriesSize++;
        }

        private void insertChildren(int index, Node<V> node) {
            if (childrenSize - index >= 0) System.arraycopy(children, index, children, index + 1, childrenSize - index);
            children[index] = node;
            node.childrenIndex = index;
            node.parentNode = this;
            childrenSize++;
        }

        private void delete(int index) {
            entriesSize--;
            if (entriesSize - index >= 0) System.arraycopy(entries, index + 1, entries, index, entriesSize - index);
            entries[entriesSize] = null;
        }

        private void deleteChildren(int index) {
            childrenSize--;
            for (int i = index; i < childrenSize; i++) {
                children[i] = children[i + 1];
                children[i].childrenIndex = i;
            }
            children[childrenSize] = null;
        }


        public int searchEntryIndex(Comparable key) {
            int low = 0;
            int high = entriesSize - 1;
            int mid = 0;

            while (low <= high) {
                mid = low + (high - low >> 1);
                if (entries[mid].getKey().compareTo(key) > 0) {
                    // key小
                    high = mid - 1;
                } else if (entries[mid].getKey().compareTo(key) < 0) {
                    low = mid + 1;
                } else {
                    break;
                }
            }

            return mid;
        }

        public void addEntry(Entry<V> entry) {
            int index = searchEntryIndex(entry.getKey());
            if (entries[index] != null && entries[index].getKey().compareTo(entry.getKey()) < 0)
                index++;
            insert(index, entry);
            nodeSplit(this);
        }

        // 1 根分裂
        // 2 叶子分裂
        // 3 中间节点分裂
        public void nodeSplit(Node<V> node) {
            if (node.entriesSize <= this.maxEntry)
                return;
            int lastIndex = node.entriesSize - 1;
            int mid = lastIndex / 2;

            Node<V> leftNode = new Node<>(this.stage);
            Node<V> rightNode = new Node<>(this.stage);

            System.arraycopy(node.entries, 0, leftNode.entries, 0, mid);
            leftNode.entriesSize = mid;
            System.arraycopy(node.entries, mid + 1, rightNode.entries, 0, lastIndex - mid);
            rightNode.entriesSize = lastIndex - mid;

            if (node.childrenSize > 0) {
                //有子节点

                System.arraycopy(node.children, 0, leftNode.children, 0, mid + 1);
                leftNode.childrenSize = mid + 1;
                leftNode.leaf = false;
                // 处理copy后子节点关系
                for (int i = 0; i < leftNode.childrenSize; i++) {
                    leftNode.children[i].parentNode = leftNode;
                    leftNode.children[i].childrenIndex = i;
                }

                System.arraycopy(node.children, mid + 1, rightNode.children, 0, node.childrenSize - mid - 1);
                rightNode.childrenSize = node.childrenSize - mid - 1;
                rightNode.leaf = false;
                for (int i = 0; i < rightNode.childrenSize; i++) {
                    rightNode.children[i].parentNode = rightNode;
                    rightNode.children[i].childrenIndex = i;
                }
            }

            if (null == node.parentNode) {
                //根节点特殊处理
                Entry<V>[] newEntries = new Entry[this.maxEntry + 1];
                newEntries[0] = node.entries[mid];
                node.entries = newEntries;
                node.entriesSize = 1;

                Node<V>[] newChildren = new Node[this.maxChildren + 1];

                newChildren[0] = leftNode;
                leftNode.childrenIndex = 0;
                leftNode.parentNode = node;

                newChildren[1] = rightNode;
                rightNode.childrenIndex = 1;
                rightNode.parentNode = node;

                node.children = newChildren;
                node.childrenSize = 2;
                node.leaf = false;
                return;
            }

            node.parentNode.insert(node.childrenIndex, node.entries[mid]);
            node.parentNode.deleteChildren(node.childrenIndex);
            node.parentNode.insertChildren(node.childrenIndex, leftNode);
            node.parentNode.insertChildren(node.childrenIndex + 1, rightNode);

            nodeSplit(node.parentNode);

        }
    }
}
