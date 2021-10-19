class MyClass {     // the java code used as input file for test of custom rule
    MyClass(MyClass mc) { }

    int     foo1() { return 0; }
    void    foo2(int value) { }
    int     foo3(int value) { return 0; }               // Noncompliant
    Object  foo4(int value) { return null; }
    MyClass foo5(MyClass value) { return null; }        // Noncompliant

    int     foo6(int value, String name) { return 0; }
    int     foo7(int ... values) { return 0; }
}

/*
    The test file now contains the following test cases:

    line 2: A constructor, to differentiate the case from a method;
    line 4: A method without parameter (foo1);
    line 5: A method returning void (foo2);
    line 6: A method returning the same type as its parameter (foo3), which will be noncompliant;
    line 7: A method with a single parameter, but a different return type (foo4);
    line 8: Another method with a single parameter and same return type, but with non-primitive types (foo5), therefore non compliant too;
    line 10: A method with more than 1 parameter (foo6);
    line 11: A method with a variable arity argument (foo7);
 */