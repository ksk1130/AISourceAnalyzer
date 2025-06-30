package org.example;

/**
 * ParentClassを継承し、MyInterfaceを実装したクラス。
 * さまざまなメソッドのサンプル実装を含みます。
 */
public class ChildClass extends ParentClass implements MyInterface {
    /**
     * doSomethingメソッドの実装例。
     * ヘルパーメソッドの呼び出しや例外のスローなどを行います。
     */
    @Override
    public void doSomething() {
        System.out.println("doSomething() called in ChildClass");
        String msg = callHelper("Test");
        System.out.println(msg);

        if (true && shouldCallExtra()) {
            extraMethod();
            throw new RuntimeException("This is a runtime exception in ChildClass");
        }
        // クラス生成のサンプル
        HelperObject obj = new HelperObject();
        obj.help();
    }

    /**
     * extraMethodを呼び出すかどうかを判定します。
     * @return 常にtrueを返します。
     */
    private boolean shouldCallExtra() {
        System.out.println("shouldCallExtra() called in ChildClass");
        return true;
    }

    /**
     * 追加の処理を行うメソッド。
     */
    private void extraMethod() {
        System.out.println("extraMethod() called in ChildClass");
    }

    /**
     * ヘルパーメソッドを呼び出します。
     * @param message メッセージ文字列
     * @return 呼び出し内容を示す文字列
     */
    private String callHelper(String message) {
        return "callHelper(" + message + ") called in ChildClass";
    }

    /**
     * ChildClass独自のメソッド。
     */
    public void childMethod() {
        System.out.println("This is a method in ChildClass");
    }
}

/**
 * ChildClass内で利用されるヘルパークラス。
 */
class HelperObject {
    /**
     * ヘルパー処理を実行します。
     */
    void help() {
        System.out.println("HelperObject.help() called");
    }
}
