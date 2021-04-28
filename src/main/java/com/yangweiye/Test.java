package com.yangweiye;

import java.util.Objects;

public class Test {
    public static void main(String[] args) {
        BTree<User> userBTree = new BTree<>(2);
        User user;
        for (int i = 1; i <= 22; i += 1) {
            user = new User(i, "i am " + i, i % 7);
            userBTree.put(user.getId(), user);
        }

        for (int i = 8; i < 20; i++) {
            userBTree.delete(i);
        }

        userBTree.search(8);
    }
}

class User {
    private Integer id;
    private String name;
    private Integer age;

    public User() {
    }

    public User(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(name, user.name) && Objects.equals(age, user.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}