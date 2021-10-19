class JavaScope {
    public static void main(String[] args) {
        // Scope에 대해 알아봅시다
        int number = 1;         // hi
        int ITblog = 1;

        System.out.println(number + ITblog);

        add();
    }

    public static void add() {
        int number = 3;
        int ITblog = 5;
        System.out.println(number + ITblog);
    }
}