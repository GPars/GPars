package org.gparallelizer.issues

final class TestMessage {
    String value
}

final class Foo {}

if (new Foo() == new TestMessage(value : 'Value')) println 'Equal'
if (new TestMessage(value : 'Value') == new Foo()) println 'Equal'
